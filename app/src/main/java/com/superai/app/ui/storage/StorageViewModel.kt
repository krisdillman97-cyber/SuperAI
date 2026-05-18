package com.superai.app.ui.storage

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.superai.app.storage.drive.DriveFileInfo
import com.superai.app.storage.drive.DriveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StorageUiState(
    val isSignedIn: Boolean = false,
    val accountName: String? = null,
    val files: List<DriveFileInfo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class StorageViewModel @Inject constructor(
    private val driveRepo: DriveRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StorageUiState())
    val uiState: StateFlow<StorageUiState> = _uiState.asStateFlow()

    fun getSignInIntent(context: Context): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        return GoogleSignIn.getClient(context, gso).signInIntent
    }

    fun handleSignInResult(context: Context, data: Intent?) {
        val account = GoogleSignIn.getSignedInAccountFromIntent(data)
            .runCatching { result }.getOrNull() ?: return
        driveRepo.initWithAccount(account.email ?: return)
        _uiState.value = _uiState.value.copy(isSignedIn = true, accountName = account.email)
        refreshFiles()
    }

    fun refreshFiles() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        val files = driveRepo.listFiles()
        _uiState.value = _uiState.value.copy(isLoading = false, files = files)
    }

    fun deleteFile(fileId: String) = viewModelScope.launch {
        driveRepo.deleteFile(fileId)
        refreshFiles()
    }

    fun signOut(context: Context) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(context, gso).signOut()
        driveRepo.signOut()
        _uiState.value = StorageUiState()
    }
}
