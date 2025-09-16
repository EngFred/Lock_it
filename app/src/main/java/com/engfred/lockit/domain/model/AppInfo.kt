package com.engfred.lockit.domain.model

/**
 * icon: optional PNG byte array for the app icon (can be decoded to a Bitmap in UI).
 * installTime: package firstInstallTime (ms since epoch). Useful for sorting/filtering.
 * isLocked default stays false to avoid breaking callers.
 */
data class AppInfo(
    val packageName: String,
    val name: String,
    val icon: ByteArray? = null,
    val installTime: Long = 0L,
    val isLocked: Boolean = false
)
