package com.engfred.lockit.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.engfred.lockit.domain.usecase.ValidatePinUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LockViewModel @Inject constructor(
    private val validatePinUseCase: ValidatePinUseCase
) : ViewModel() {
    suspend fun validatePin(pin: String): Boolean {
        return validatePinUseCase(pin)
    }
}