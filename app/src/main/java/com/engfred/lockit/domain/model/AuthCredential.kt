package com.engfred.lockit.domain.model

data class AuthCredential(
    val id: Int = 0,
    val hashedPin: String // SHA-256 hashed
)