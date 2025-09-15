package com.engfred.lockit.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engfred.lockit.domain.model.AppInfo
import com.engfred.lockit.domain.usecase.GetInstalledAppsUseCase
import com.engfred.lockit.domain.usecase.LockAppUseCase
import com.engfred.lockit.domain.usecase.UnlockAppUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppListViewModel @Inject constructor(
    getInstalledAppsUseCase: GetInstalledAppsUseCase,
    private val lockAppUseCase: LockAppUseCase,
    private val unlockAppUseCase: UnlockAppUseCase
) : ViewModel() {

    val apps = getInstalledAppsUseCase().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    suspend fun toggleLock(app: AppInfo) {
        if (app.isLocked) unlockAppUseCase(app.packageName) else lockAppUseCase(app.packageName)
    }
}