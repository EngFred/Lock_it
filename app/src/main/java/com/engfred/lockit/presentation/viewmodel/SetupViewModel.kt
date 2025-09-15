package com.engfred.lockit.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.engfred.lockit.domain.usecase.SetPinUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val setPinUseCase: SetPinUseCase
) : ViewModel() {
    suspend fun setPin(pin: String): Boolean {
        return setPinUseCase(pin)
    }
}