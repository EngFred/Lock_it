package com.engfred.lockit.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple event bus for temporary unlock events.
 * emitUnlockedPackage is now non-suspending and uses tryEmit so UI can emit without needing a suspend scope.
 */
@Singleton
class UnlockEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val events = _events.asSharedFlow()

    /**
     * tryEmit will push the event immediately if buffer allows.
     * This avoids needing to call from a suspend context in the UI.
     */
    fun emitUnlockedPackage(packageName: String) {
        _events.tryEmit(packageName)
    }
}
