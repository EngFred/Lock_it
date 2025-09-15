package com.engfred.lockit.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.util.Log

/**
 * Helpers to check and open Accessibility Settings
 */
object AccessibilityUtils {

    /**
     * Open the system accessibility settings so the user can enable the service.
     */
    fun openAccessibilitySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (ex: Exception) {
            Log.e("AccessibilityUtils", "Failed to open accessibility settings", ex)
        }
    }

    /**
     * Returns true if the accessibility service with the given serviceClass is enabled.
     */
    fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<*>): Boolean {
        val expected = ComponentName(context.packageName, serviceClass.name).flattenToString()
        return try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':').apply { setString(enabledServices) }
            while (colonSplitter.hasNext()) {
                val component = colonSplitter.next()
                if (component.equals(expected, ignoreCase = true)) {
                    val accessibilityEnabled = Settings.Secure.getInt(
                        context.contentResolver,
                        Settings.Secure.ACCESSIBILITY_ENABLED,
                        0
                    )
                    return accessibilityEnabled == 1
                }
            }
            false
        } catch (e: Exception) {
            Log.e("AccessibilityUtils", "Error checking accessibility enabled", e)
            false
        }
    }
}