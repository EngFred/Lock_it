package com.engfred.lockit.domain.usecase

import com.engfred.lockit.domain.model.AuthCredential
import com.engfred.lockit.domain.repository.AppRepository
import javax.inject.Inject

class GetAuthCredentialUseCase @Inject constructor(
    private val repository: AppRepository
) {
    suspend operator fun invoke(): AuthCredential? {
        return repository.getAuthCredential()
    }
}