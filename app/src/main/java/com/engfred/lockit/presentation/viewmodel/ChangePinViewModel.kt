package com.engfred.lockit.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.engfred.lockit.domain.repository.AppRepository
import com.engfred.lockit.utils.toSha256
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ChangePinViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    suspend fun validateOldPin(oldPin: String): Boolean {
        return repository.validatePin(oldPin.toSha256())
    }

    suspend fun updatePin(newPin: String): Boolean {
        return repository.updatePin(newPin.toSha256())
    }
}
