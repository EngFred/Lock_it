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
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppInfo

        if (packageName != other.packageName) return false
        if (name != other.name) return false
        if (installTime != other.installTime) return false
        if (isLocked != other.isLocked) return false
        if (icon != null) {
            if (other.icon == null) return false
            if (!icon.contentEquals(other.icon)) return false
        } else if (other.icon != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + installTime.hashCode()
        result = 31 * result + isLocked.hashCode()
        result = 31 * result + (icon?.contentHashCode() ?: 0)
        return result
    }
}