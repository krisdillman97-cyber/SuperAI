package com.superai.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.superai.app.agent.core.AgentRepository
import com.superai.app.agent.profile.AgentProfile
import com.superai.app.agent.state.AgentStateMachine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repo: AgentRepository,
    val stateMachine: AgentStateMachine
) : ViewModel() {

    val profiles: StateFlow<List<AgentProfile>> = repo.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeProfile: StateFlow<AgentProfile?> = repo.getActiveProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun createAgent(name: String) = viewModelScope.launch {
        repo.save(AgentProfile(name = name))
    }

    fun activateAgent(id: String) = viewModelScope.launch {
        repo.setActive(id)
        stateMachine.setProfile(id)
    }

    fun deleteAgent(profile: AgentProfile) = viewModelScope.launch {
        repo.delete(profile)
    }
}
