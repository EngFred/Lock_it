package com.engfred.lockit.domain.usecase

import com.engfred.lockit.domain.model.AppInfo
import com.engfred.lockit.domain.repository.AppRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetInstalledAppsUseCase @Inject constructor(
    private val repository: AppRepository
) {
    operator fun invoke(): Flow<List<AppInfo>> = repository.getInstalledApps()
}