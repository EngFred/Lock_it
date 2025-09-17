package com.engfred.lockit.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engfred.lockit.domain.model.AppInfo
import com.engfred.lockit.domain.usecase.GetInstalledAppsUseCase
import com.engfred.lockit.domain.usecase.LockAppUseCase
import com.engfred.lockit.domain.usecase.UnlockAppUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppListViewModel @Inject constructor(
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase,
    private val lockAppUseCase: LockAppUseCase,
    private val unlockAppUseCase: UnlockAppUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<AppInfo>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<AppInfo>>> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getInstalledAppsUseCase()
                .catch { e ->
                    _uiState.value = UiState.Error("Failed to load apps: ${e.message}")
                }
                .collect { apps ->
                    _uiState.value = UiState.Success(apps)
                }
        }
    }

    suspend fun toggleLock(app: AppInfo) {
        if (app.isLocked) unlockAppUseCase(app.packageName) else lockAppUseCase(app.packageName)
    }
}

// Sealed class for UI states
sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}