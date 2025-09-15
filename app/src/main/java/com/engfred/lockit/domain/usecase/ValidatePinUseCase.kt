package com.engfred.lockit.domain.usecase

import com.engfred.lockit.domain.repository.AppRepository
import com.engfred.lockit.utils.toSha256
import javax.inject.Inject

class ValidatePinUseCase @Inject constructor(
    private val repository: AppRepository
) {
    suspend operator fun invoke(pin: String): Boolean {
        return repository.validatePin(pin.toSha256())
    }
}