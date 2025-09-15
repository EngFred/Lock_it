package com.engfred.lockit.domain.usecase

import com.engfred.lockit.domain.repository.AppRepository
import javax.inject.Inject

class UnlockAppUseCase @Inject constructor(
    private val repository: AppRepository
) {
    suspend operator fun invoke(packageName: String) {
        repository.unlockApp(packageName)
    }
}