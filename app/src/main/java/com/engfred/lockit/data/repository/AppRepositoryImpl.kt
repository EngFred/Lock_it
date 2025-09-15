package com.engfred.lockit.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.engfred.lockit.data.local.AppDatabase
import com.engfred.lockit.data.local.AuthCredentialEntity
import com.engfred.lockit.data.local.LockedAppEntity
import com.engfred.lockit.domain.model.AppInfo
import com.engfred.lockit.domain.model.AuthCredential
import com.engfred.lockit.domain.repository.AppRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AppRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    @ApplicationContext private val context: Context
) : AppRepository {

    override fun getInstalledApps(): Flow<List<AppInfo>> {
        // Fetch from PackageManager, map to domain, check locked status
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .map { AppInfo(it.packageName, pm.getApplicationLabel(it).toString()) }
        return getLockedApps().map { locked ->
            apps.map { it.copy(isLocked = locked.contains(it.packageName)) }
        }
    }

    override suspend fun lockApp(packageName: String) {
        database.lockedAppDao().insert(LockedAppEntity(packageName))
    }

    override suspend fun unlockApp(packageName: String) {
        database.lockedAppDao().delete(LockedAppEntity(packageName))
    }

    override fun getLockedApps(): Flow<List<String>> = database.lockedAppDao().getAll()

    // In setPin we pass hashedPin from use case
    override suspend fun setPin(hashedPin: String): Boolean {
        database.authDao().insert(AuthCredentialEntity(hashedPin = hashedPin))
        return true
    }

    // In validatePin (hash input in use case, but here for repo)
    override suspend fun validatePin(hashedPin: String): Boolean {
        val stored = database.authDao().get()
        return stored?.hashedPin == hashedPin
    }

    // getAuthCredential
    override suspend fun getAuthCredential(): AuthCredential? {
        val entity = database.authDao().get()
        return entity?.let { AuthCredential(it.id, it.hashedPin) }
    }
}