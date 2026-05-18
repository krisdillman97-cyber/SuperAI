package com.superai.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.superai.app.agent.core.AgentRepository
import com.superai.app.agent.profile.AgentProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repo: AgentRepository
) : ViewModel() {

    val profiles: StateFlow<List<AgentProfile>> = repo.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeProfile: StateFlow<AgentProfile?> = repo.getActiveProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun createAgent(name: String, instructions: String, emoji: String) = viewModelScope.launch {
        repo.createProfile(AgentProfile(
            id                 = UUID.randomUUID().toString(),
            name               = name,
            systemInstructions = instructions,
            avatarEmoji        = emoji
        ))
    }

    fun deleteAgent(profile: AgentProfile) = viewModelScope.launch { repo.deleteProfile(profile) }

    fun activateAgent(id: String) = viewModelScope.launch { repo.activateProfile(id) }

    fun toggleOverlay(start: Boolean) = if (start) repo.startOverlay() else repo.stopOverlay()
}
