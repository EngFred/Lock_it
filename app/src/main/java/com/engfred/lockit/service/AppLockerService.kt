package com.engfred.lockit.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.app.NotificationCompat
import com.engfred.lockit.LockItApplication
import com.engfred.lockit.MainActivity
import com.engfred.lockit.presentation.ui.LockScreenActivity
import com.engfred.lockit.domain.usecase.GetLockedAppsUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@AndroidEntryPoint
class AppLockerService : AccessibilityService() {

    @Inject
    lateinit var getLockedAppsUseCase: GetLockedAppsUseCase

    @Inject
    lateinit var unlockEventBus: UnlockEventBus

    private val scope = CoroutineScope(Job() + Dispatchers.IO)
    private var lockedApps: List<String> = emptyList()

    // The package currently considered foreground (last processed "real" focused package)
    private var previousPackage: String? = null

    // Temporarily unlocked package (cleared when the user actually backgrounds the app)
    @Volatile
    private var lastUnlockedPackage: String? = null

    // throttle map: package -> last lock attempt timestamp (ms)
    private val lastLockAttempt = ConcurrentHashMap<String, Long>()

    // Packages to ignore (IME packages & system UI only)
    private val ignoredPackages = mutableSetOf<String>()

    override fun onCreate() {
        super.onCreate()

        // Build initial ignored packages: include system UI and IMEs (but NOT our own package)
        refreshIgnoredPackages()

        // Collect locked apps (flow)
        scope.launch {
            try {
                getLockedAppsUseCase().collectLatest {
                    lockedApps = it
                    Log.d(TAG, "Locked apps updated: ${lockedApps.size}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error collecting locked apps", e)
            }
        }

        // Subscribe to unlock events - non-blocking update of lastUnlockedPackage
        scope.launch {
            try {
                unlockEventBus.events.collectLatest { packageName ->
                    lastUnlockedPackage = packageName
                    Log.d(TAG, "Temporarily unlocked from event bus: $packageName")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error collecting unlock events", e)
            }
        }
    }

    /**
     * Refresh the set of packages we should ignore (keyboards / input methods + system UI).
     * NOTE: we DO NOT add our own package here to avoid missing important events.
     */
    private fun refreshIgnoredPackages() {
        try {
            ignoredPackages.clear()

            // Add common system UI package
            ignoredPackages.add("com.android.systemui")

            // Add enabled input method packages (keyboards)
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            val enabledImes: List<InputMethodInfo> = imm?.enabledInputMethodList ?: emptyList()
            for (imi in enabledImes) {
                try {
                    val pkg = imi.packageName
                    if (!pkg.isNullOrBlank()) ignoredPackages.add(pkg)
                } catch (_: Exception) { /* ignore per-IME failures */ }
            }

            Log.d(TAG, "Ignored packages (IME/system): $ignoredPackages")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh ignored packages", e)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.DEFAULT or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100L
        }
        serviceInfo = info

        try {
            startForeground(NOTIF_ID, createNotification())
            Log.d(TAG, "Service connected and started foreground")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground in onServiceConnected", e)
        }
    }

    /**
     * Compute the actual package owning the focused (top) application window.
     * This is more reliable than trusting event.packageName because many events come
     * from IMEs, overlays, or background windows.
     */
    private fun getTopFocusedPackage(): String? {
        return try {
            val winList: List<AccessibilityWindowInfo> = windows ?: return null
            // Prefer the focused application window
            for (w in winList) {
                try {
                    if (w.isFocused && w.type == AccessibilityWindowInfo.TYPE_APPLICATION) {
                        val root = w.root
                        val pkg = root?.packageName?.toString()
                        if (!pkg.isNullOrBlank()) return pkg
                    }
                } catch (_: Exception) { /* ignore individual window failures */ }
            }
            // Fallback: first application window with a package
            for (w in winList) {
                try {
                    if (w.type == AccessibilityWindowInfo.TYPE_APPLICATION) {
                        val root = w.root
                        val pkg = root?.packageName?.toString()
                        if (!pkg.isNullOrBlank()) return pkg
                    }
                } catch (_: Exception) { /* ignore */ }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error reading windows for top-focused package", e)
            null
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        // First try to get the real focused package from windows
        val focusedPackage = getTopFocusedPackage()
        val eventPkg = event.packageName?.toString()
        val className = event.className?.toString()

        // If className looks like an IME or overlay, ignore it early
        if (!className.isNullOrBlank() &&
            (className.contains("InputMethod", ignoreCase = true) || className.contains("IME", ignoreCase = true))
        ) {
            Log.d(TAG, "Ignoring event from IME-like class: $className (pkg: $eventPkg)")
            return
        }

        // If event package is an ignored package, ignore
        if (!eventPkg.isNullOrBlank() && ignoredPackages.contains(eventPkg)) {
            Log.d(TAG, "Ignoring event from ignored package: $eventPkg")
            return
        }

        // Decide which package we actually treat as the "current foreground package".
        // Prefer the focusedPackage (from windows). If it's null, fall back to the event package.
        val currentPkg = when {
            !focusedPackage.isNullOrBlank() -> focusedPackage
            !eventPkg.isNullOrBlank() -> eventPkg
            else -> null
        } ?: return

        // Special handling for our own package: ignore specific internal windows to prevent loops,
        // but do not globally ignore our package (we must react when user opens our app).
        if (currentPkg == packageName) {
            if (!className.isNullOrBlank()) {
                val lower = className.lowercase()
                if (lower.contains("lockscreen") || lower.contains("lockscreenactivity") || lower.contains("setup") || lower.contains("mainactivity")) {
                    Log.d(TAG, "Ignoring our internal UI window: $className")
                    return
                }
            }
            Log.d(TAG, "Received event from our package (will process): $className")
        }

        // If same package as previous, nothing to do (debounce frequent internal window changes).
        if (currentPkg == previousPackage) return

        // Detect "previous app moved to background" -> if the previous app was temporarily unlocked, clear unlock.
        val prev = previousPackage
        if (prev != null && lockedApps.contains(prev) && prev == lastUnlockedPackage && currentPkg != prev) {
            // User moved away from the unlocked app (to a real different app) -> clear temporary unlock (re-lock)
            lastUnlockedPackage = null
            Log.d(TAG, "Re-locking $prev after background (foreground now: $currentPkg)")
        }

        // If the newly foregrounded app is locked and it's not temporarily unlocked, show lock screen.
        if (lockedApps.contains(currentPkg) && currentPkg != lastUnlockedPackage) {
            val now = System.currentTimeMillis()
            val lastAttempt = lastLockAttempt[currentPkg] ?: 0L
            if (now - lastAttempt > LOCK_THROTTLE_MS) {
                lastLockAttempt[currentPkg] = now
                Log.d(TAG, "Locking app: $currentPkg")
                val intent = Intent(this, LockScreenActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("locked_package", currentPkg)
                }
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start LockScreenActivity for $currentPkg", e)
                }
            } else {
                Log.d(TAG, "Skipping lock attempt for $currentPkg (throttled)")
            }
        }

        // update previousPackage to the newly foregrounded package (only non-ignored packages reach here)
        previousPackage = currentPkg
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        else PendingIntent.FLAG_UPDATE_CURRENT

        val pending = PendingIntent.getActivity(this, 0, intent, pendingFlags)

        return NotificationCompat.Builder(this, LockItApplication.CHANNEL_ID)
            .setContentTitle(getString(com.engfred.lockit.R.string.app_name))
            .setContentText("Protecting your apps")
            .setSmallIcon(com.engfred.lockit.R.drawable.ic_lock)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val TAG = "AppLockerService"
        private const val NOTIF_ID = 1
        private const val LOCK_THROTTLE_MS = 800L // minimum time between lock attempts for same package
    }
}