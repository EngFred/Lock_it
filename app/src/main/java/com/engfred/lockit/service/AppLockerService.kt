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

    private var previousPackage: String? = null

    @Volatile
    private var lastUnlockedPackage: String? = null

    private val lastLockAttempt = ConcurrentHashMap<String, Long>()

    private val ignoredPackages = mutableSetOf<String>()

    override fun onCreate() {
        super.onCreate()

        refreshIgnoredPackages()

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

        scope.launch {
            try {
                unlockEventBus.events.collectLatest { packageName ->
                    lastUnlockedPackage = packageName
                    Log.d(TAG, "Temporarily unlocked: $packageName")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error collecting unlock events", e)
            }
        }
    }

    private fun refreshIgnoredPackages() {
        try {
            ignoredPackages.clear()
            ignoredPackages.add("com.android.systemui")

            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            val enabledImes: List<InputMethodInfo> = imm?.enabledInputMethodList ?: emptyList()
            for (imi in enabledImes) {
                val pkg = imi.packageName
                if (!pkg.isNullOrBlank()) ignoredPackages.add(pkg)
            }

            Log.d(TAG, "Ignored packages: $ignoredPackages")
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
            Log.d(TAG, "Service connected and foreground started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground", e)
        }
    }

    private fun getTopFocusedPackage(): String? {
        return try {
            val winList: List<AccessibilityWindowInfo> = windows ?: return null
            for (w in winList) {
                if (w.isFocused && w.type == AccessibilityWindowInfo.TYPE_APPLICATION) {
                    val root = w.root
                    val pkg = root?.packageName?.toString()
                    if (!pkg.isNullOrBlank()) return pkg
                }
            }
            for (w in winList) {
                if (w.type == AccessibilityWindowInfo.TYPE_APPLICATION) {
                    val root = w.root
                    val pkg = root?.packageName?.toString()
                    if (!pkg.isNullOrBlank()) return pkg
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting top-focused package", e)
            null
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val focusedPackage = getTopFocusedPackage()
        val eventPkg = event.packageName?.toString()
        val className = event.className?.toString()

        if (!className.isNullOrBlank() &&
            (className.contains("InputMethod", ignoreCase = true) || className.contains("IME", ignoreCase = true))
        ) {
            Log.v(TAG, "Ignoring IME-like class: $className (pkg: $eventPkg)")
            return
        }

        if (!eventPkg.isNullOrBlank() && ignoredPackages.contains(eventPkg)) {
            Log.v(TAG, "Ignoring ignored package: $eventPkg")
            return
        }

        val currentPkg = focusedPackage ?: eventPkg ?: return

        // For own package: Ignore only LockScreenActivity to prevent loops; process others (e.g., MainActivity) for self-locking
        if (currentPkg == packageName) {
            if (!className.isNullOrBlank() && className.lowercase().contains("lockscreen")) {
                Log.v(TAG, "Ignoring lock screen UI: $className")
                return
            }
            Log.d(TAG, "Processing own package event: $className")
        }

        if (currentPkg == previousPackage) return

        val prev = previousPackage
        if (prev != null && lockedApps.contains(prev) && prev == lastUnlockedPackage && currentPkg != prev) {
            lastUnlockedPackage = null
            Log.d(TAG, "Re-locking $prev after backgrounding (new foreground: $currentPkg)")
        }

        if (lockedApps.contains(currentPkg) && currentPkg != lastUnlockedPackage) {
            val now = System.currentTimeMillis()
            val lastAttempt = lastLockAttempt[currentPkg] ?: 0L
            if (now - lastAttempt > LOCK_THROTTLE_MS) {
                lastLockAttempt[currentPkg] = now
                Log.d(TAG, "Launching lock for: $currentPkg")
                val intent = Intent(this, LockScreenActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("locked_package", currentPkg)
                }
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to launch lock screen for $currentPkg", e)
                }
            } else {
                Log.v(TAG, "Throttled lock for $currentPkg")
            }
        }

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
        private const val LOCK_THROTTLE_MS = 800L

        private fun String.containsAny(vararg substrings: String): Boolean {
            return substrings.any { this.contains(it, ignoreCase = true) }
        }
    }
}