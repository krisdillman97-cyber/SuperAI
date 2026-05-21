package com.superai.app.ui.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.superai.app.storage.drive.DriveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StorageUiState(
    val isSyncing: Boolean = false,
    val lastSyncMessage: String = "Not synced yet",
    val error: String? = null
)

@HiltViewModel
class StorageViewModel @Inject constructor(
    private val driveRepo: DriveRepository
) : ViewModel() {
    private val _state = MutableStateFlow(StorageUiState())
    val state: StateFlow<StorageUiState> = _state.asStateFlow()

    fun sync() = viewModelScope.launch {
        _state.value = _state.value.copy(isSyncing = true, error = null)
        try {
            driveRepo.syncAll()
            _state.value = _state.value.copy(isSyncing = false, lastSyncMessage = "Synced successfully")
        } catch (e: Exception) {
            _state.value = _state.value.copy(isSyncing = false, error = e.message)
        }
    }

    fun upload() = viewModelScope.launch {
        _state.value = _state.value.copy(isSyncing = true)
        try {
            driveRepo.uploadPendingFiles()
            _state.value = _state.value.copy(isSyncing = false, lastSyncMessage = "Upload complete")
        } catch (e: Exception) {
            _state.value = _state.value.copy(isSyncing = false, error = e.message)
        }
    }
}
