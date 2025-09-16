package com.engfred.lockit.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.LruCache
import com.engfred.lockit.data.local.AppDatabase
import com.engfred.lockit.data.local.AuthCredentialEntity
import com.engfred.lockit.data.local.LockedAppEntity
import com.engfred.lockit.domain.model.AppInfo
import com.engfred.lockit.domain.model.AuthCredential
import com.engfred.lockit.domain.repository.AppRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

class AppRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    @ApplicationContext private val context: Context
) : AppRepository {

    private val TAG = "AppRepositoryImpl"

    // Simple in-memory LRU cache keyed by package name (stores PNG bytes).
    private val memoryCache: LruCache<String, ByteArray> by lazy {
        val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSizeKb = maxMemoryKb / 8 // use 1/8th of available memory
        object : LruCache<String, ByteArray>(cacheSizeKb) {
            override fun sizeOf(key: String, value: ByteArray): Int {
                val kb = value.size / 1024
                return if (kb <= 0) 1 else kb
            }
        }
    }

    // Disk cache folder under app's cache dir
    private val iconsCacheDir: File by lazy {
        File(context.cacheDir, "app_icons").also { if (!it.exists()) it.mkdirs() }
    }

    /**
     * Return launchable apps (apps with MAIN + LAUNCHER activities).
     * Heavy work (icon conversion & package info) runs on Dispatchers.IO.
     *
     * Icon lookup order:
     * 1) in-memory LRU cache
     * 2) disk cache (cacheDir/app_icons/<pkg>.png)
     * 3) generate from drawable -> store to disk & memory
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getInstalledApps(): Flow<List<AppInfo>> {
        val pm = context.packageManager

        return getLockedApps().mapLatest { lockedPackages ->
            withContext(Dispatchers.IO) {
                try {
                    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                    val resolveInfos = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)

                    val packageNames = resolveInfos
                        .mapNotNull { it.activityInfo?.packageName }
                        .distinct()

                    val apps = packageNames.mapNotNull { pkg ->
                        // skip our own package
                        if (pkg == context.packageName) return@mapNotNull null

                        try {
                            val ai: ApplicationInfo = pm.getApplicationInfo(pkg, 0)

                            // Only include enabled apps
                            if (!ai.enabled) return@mapNotNull null

                            val label = pm.getApplicationLabel(ai)?.toString() ?: pkg

                            // package info: firstInstallTime (guarded)
                            val installTime: Long = try {
                                val pi = pm.getPackageInfo(pkg, 0)
                                pi?.firstInstallTime ?: 0L
                            } catch (t: Throwable) {
                                0L
                            }

                            // Try memory cache
                            var iconBytes: ByteArray? = memoryCache.get(pkg)

                            // Try disk cache
                            if (iconBytes == null) {
                                val diskFile = File(iconsCacheDir, "$pkg.png")
                                if (diskFile.exists()) {
                                    try {
                                        iconBytes = diskFile.readBytes()
                                        // put into memory cache for faster future access
                                        memoryCache.put(pkg, iconBytes)
                                    } catch (t: Throwable) {
                                        // ignore read failures, we'll regenerate
                                        Log.w(TAG, "Failed reading disk cache for $pkg", t)
                                    }
                                }
                            }

                            // If still null, fetch drawable and convert (then persist)
                            if (iconBytes == null) {
                                val iconDrawable: Drawable? = try {
                                    pm.getApplicationIcon(pkg)
                                } catch (e: Exception) {
                                    null
                                }

                                iconBytes = iconDrawable?.let { drawableToPngBytes(it) }

                                // persist to disk & memory if we got bytes
                                if (iconBytes != null) {
                                    try {
                                        val diskFile = File(iconsCacheDir, "$pkg.png")
                                        diskFile.writeBytes(iconBytes)
                                        memoryCache.put(pkg, iconBytes)
                                    } catch (t: Throwable) {
                                        Log.w(TAG, "Failed writing icon cache for $pkg", t)
                                    }
                                }
                            }

                            AppInfo(
                                packageName = pkg,
                                name = label,
                                icon = iconBytes,
                                installTime = installTime
                            )
                        } catch (e: PackageManager.NameNotFoundException) {
                            null
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to build AppInfo for $pkg", e)
                            null
                        }
                    }

                    // sort by installTime (newest first), then apply locked mapping
                    apps.sortedByDescending { it.installTime }
                        .map { app -> app.copy(isLocked = lockedPackages.contains(app.packageName)) }

                } catch (t: Throwable) {
                    Log.w(TAG, "Error enumerating launcher apps", t)
                    emptyList()
                }
            }
        }
    }

    override suspend fun lockApp(packageName: String) {
        database.lockedAppDao().insert(LockedAppEntity(packageName))
    }

    override suspend fun unlockApp(packageName: String) {
        database.lockedAppDao().delete(LockedAppEntity(packageName))
    }

    override fun getLockedApps(): Flow<List<String>> = database.lockedAppDao().getAll()

    override suspend fun setPin(hashedPin: String): Boolean {
        database.authDao().insert(AuthCredentialEntity(hashedPin = hashedPin))
        return true
    }

    override suspend fun validatePin(hashedPin: String): Boolean {
        val stored = database.authDao().get()
        return stored?.hashedPin == hashedPin
    }

    override suspend fun getAuthCredential(): AuthCredential? {
        val entity = database.authDao().get()
        return entity?.let { AuthCredential(it.id, it.hashedPin) }
    }

    /**
     * Convert Drawable to PNG bytes. Uses a Canvas for non-BitmapDrawable Drawables.
     * Called on Dispatchers.IO.
     */
    private fun drawableToPngBytes(drawable: Drawable): ByteArray? {
        return try {
            val bitmap: Bitmap = when (drawable) {
                is BitmapDrawable -> drawable.bitmap
                else -> {
                    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 48
                    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 48
                    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    bmp
                }
            }

            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            baos.toByteArray()
        } catch (t: Throwable) {
            Log.w(TAG, "drawableToPngBytes failed", t)
            null
        }
    }
}
