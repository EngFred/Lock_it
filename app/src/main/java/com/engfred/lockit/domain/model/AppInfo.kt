package com.engfred.lockit.domain.model

data class AppInfo(
    val packageName: String,
    val name: String,
    val isLocked: Boolean = false
)