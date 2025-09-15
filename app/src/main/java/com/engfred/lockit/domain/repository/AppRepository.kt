package com.engfred.lockit.domain.repository

import com.engfred.lockit.domain.model.AppInfo
import com.engfred.lockit.domain.model.AuthCredential
import kotlinx.coroutines.flow.Flow

interface AppRepository {
    fun getInstalledApps(): Flow<List<AppInfo>>
    suspend fun lockApp(packageName: String)
    suspend fun unlockApp(packageName: String)
    fun getLockedApps(): Flow<List<String>> // Package names
    suspend fun setPin(hashedPin: String): Boolean
    suspend fun validatePin(hashedPin: String): Boolean
    suspend fun getAuthCredential(): AuthCredential?
}