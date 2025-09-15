package com.engfred.lockit.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "auth_credentials")
data class AuthCredentialEntity(
    @PrimaryKey val id: Int = 0,
    val hashedPin: String
)