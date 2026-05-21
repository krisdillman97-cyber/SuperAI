package com.superai.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.superai.app.agent.core.AgentRepository
import com.superai.app.agent.profile.AgentProfile  // used in StateFlow<AgentProfile?>
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

    // Use a StateFlow of the machine itself so flatMapLatest picks up the
    // machine only after it has been created by loadProfile().
    private val _machine = MutableStateFlow<AgentStateMachine?>(null)

    val agentState: StateFlow<AgentState> = _machine
        .filterNotNull()
        .flatMapLatest { it.state }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AgentState.Idle)

    val resultLog: StateFlow<List<DirectiveResult>> = _machine
        .filterNotNull()
        .flatMapLatest { it.resultLog }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun loadProfile(id: String) {
        _profileId.value = id
        viewModelScope.launch {
            val p = repo.getProfileById(id) ?: return@launch
            _machine.value = repo.getOrCreateMachine(p)
        }
    }

    fun submitDirective(text: String) {
        val machine = _machine.value ?: return
        val id = _profileId.value ?: return
        viewModelScope.launch {
            machine.transition(AgentEvent.SubmitDirective(text))
            val startMs = System.currentTimeMillis()
            // Simulate processing delay; replace with real LLM call as needed
            kotlinx.coroutines.delay(1200)
            val duration = System.currentTimeMillis() - startMs
            machine.transition(AgentEvent.Complete(
                directive  = text,
                output     = "Processed: $text",
                success    = true,
                durationMs = duration
            ))
            // Persist incremented directive count
            val p = repo.getProfileById(id) ?: return@launch
            repo.updateProfile(p.copy(
                totalDirectivesProcessed = p.totalDirectivesProcessed + 1,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    fun resetState() {
        _machine.value?.transition(AgentEvent.Reset)
    }
}
