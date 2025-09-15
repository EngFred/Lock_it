package com.engfred.lockit.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.engfred.lockit.domain.usecase.GetAuthCredentialUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getAuthCredentialUseCase: GetAuthCredentialUseCase
) : ViewModel() {

    suspend fun isSetupComplete(): Boolean {
        return getAuthCredentialUseCase() != null
    }
}