package com.engfred.lockit.domain.usecase

import com.engfred.lockit.domain.repository.AppRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLockedAppsUseCase @Inject constructor(
    private val repository: AppRepository
) {
    operator fun invoke(): Flow<List<String>> = repository.getLockedApps()
}