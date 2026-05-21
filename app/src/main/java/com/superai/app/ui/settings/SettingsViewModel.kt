package com.superai.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.superai.app.agent.core.AgentRepository
import com.superai.app.agent.profile.AgentProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: AgentRepository
) : ViewModel() {

    private val _profile = MutableStateFlow<AgentProfile?>(null)
    val profile: StateFlow<AgentProfile?> = _profile.asStateFlow()

    fun loadProfile(id: String) = viewModelScope.launch {
        _profile.value = repo.getById(id)
    }

    fun updateProfile(profile: AgentProfile) = viewModelScope.launch {
        repo.update(profile)
        _profile.value = profile
    }

    fun resetFilters() = viewModelScope.launch {
        val p = _profile.value ?: return@launch
        val reset = p.copy(
            safetyEnabled = true, safetyLevel = 0.7f,
            illicitFilterEnabled = true, illicitFilterLevel = 0.7f
        )
        repo.update(reset)
        _profile.value = reset
    }
}
