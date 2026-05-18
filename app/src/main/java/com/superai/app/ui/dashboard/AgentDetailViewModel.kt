package com.superai.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.superai.app.agent.core.AgentRepository
import com.superai.app.agent.profile.AgentProfile
import com.superai.app.agent.state.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AgentDetailViewModel @Inject constructor(
    private val repo: AgentRepository
) : ViewModel() {

    private val _profileId = MutableStateFlow<String?>(null)

    val profile: StateFlow<AgentProfile?> = _profileId
        .filterNotNull()
        .flatMapLatest { id ->
            repo.getAllProfiles().map { list -> list.firstOrNull { it.id == id } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val agentState: StateFlow<AgentState> = _profileId
        .filterNotNull()
        .flatMapLatest { id ->
            repo.getMachine(id)?.state ?: flowOf(AgentState.Idle)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AgentState.Idle)

    val resultLog: StateFlow<List<DirectiveResult>> = _profileId
        .filterNotNull()
        .flatMapLatest { id ->
            repo.getMachine(id)?.resultLog ?: flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun loadProfile(id: String) {
        _profileId.value = id
        viewModelScope.launch {
            val p = repo.getProfileById(id) ?: return@launch
            repo.getOrCreateMachine(p)
        }
    }

    fun submitDirective(text: String) {
        val id = _profileId.value ?: return
        viewModelScope.launch {
            val p = repo.getProfileById(id) ?: return@launch
            val machine = repo.getOrCreateMachine(p)
            machine.transition(AgentEvent.SubmitDirective(text))
            // Simulate processing + complete
            kotlinx.coroutines.delay(1200)
            machine.transition(AgentEvent.Complete(
                directive = text,
                output    = "Processed: $text",
                success   = true,
                durationMs = 1200
            ))
            repo.updateProfile(p.copy(
                totalDirectivesProcessed = p.totalDirectivesProcessed + 1,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    fun resetState() {
        val id = _profileId.value ?: return
        repo.getMachine(id)?.transition(AgentEvent.Reset)
    }
}
