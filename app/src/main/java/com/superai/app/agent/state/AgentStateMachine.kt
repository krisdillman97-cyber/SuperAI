package com.superai.app.agent.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class AgentState {
    IDLE, LISTENING, PROCESSING, RESPONDING, ERROR, COMPILING, SUSPENDED
}

data class AgentContext(
    val state: AgentState = AgentState.IDLE,
    val activeProfileId: String? = null,
    val lastDirective: String = "",
    val errorMessage: String? = null,
    val contextWindow: List<String> = emptyList(),
    val maxContextTokens: Int = 4096
)

@Singleton
class AgentStateMachine @Inject constructor() {

    private val _context = MutableStateFlow(AgentContext())
    val context: StateFlow<AgentContext> = _context.asStateFlow()

    private val _windowBuffer = ArrayDeque<String>(MAX_WINDOW)

    fun transition(newState: AgentState, directive: String = "", error: String? = null) {
        val current = _context.value
        val allowed = validTransitions[current.state] ?: emptySet()
        if (newState !in allowed && newState != current.state) {
            _context.value = current.copy(state = AgentState.ERROR,
                errorMessage = "Invalid transition ${current.state} → $newState")
            return
        }
        _context.value = current.copy(
            state         = newState,
            lastDirective = directive.ifBlank { current.lastDirective },
            errorMessage  = error
        )
    }

    fun pushContext(entry: String) {
        val tokenEstimate = entry.length / 4
        pruneWindowIfNeeded(tokenEstimate)
        _windowBuffer.addLast(entry)
        _context.value = _context.value.copy(contextWindow = _windowBuffer.toList())
    }

    private fun pruneWindowIfNeeded(incomingTokens: Int) {
        var total = _windowBuffer.sumOf { it.length / 4 } + incomingTokens
        while (total > _context.value.maxContextTokens && _windowBuffer.isNotEmpty()) {
            val removed = _windowBuffer.removeFirst()
            total -= removed.length / 4
        }
    }

    fun clearContext() {
        _windowBuffer.clear()
        _context.value = _context.value.copy(contextWindow = emptyList())
    }

    fun reset() {
        _windowBuffer.clear()
        _context.value = AgentContext(activeProfileId = _context.value.activeProfileId)
    }

    fun setProfile(id: String) {
        _context.value = _context.value.copy(activeProfileId = id)
    }

    fun buildContextString(): String = _windowBuffer.joinToString("\n")

    private val validTransitions = mapOf(
        AgentState.IDLE        to setOf(AgentState.LISTENING, AgentState.COMPILING, AgentState.SUSPENDED),
        AgentState.LISTENING   to setOf(AgentState.PROCESSING, AgentState.IDLE, AgentState.ERROR),
        AgentState.PROCESSING  to setOf(AgentState.RESPONDING, AgentState.COMPILING, AgentState.ERROR, AgentState.IDLE),
        AgentState.RESPONDING  to setOf(AgentState.IDLE, AgentState.LISTENING, AgentState.ERROR),
        AgentState.COMPILING   to setOf(AgentState.IDLE, AgentState.ERROR),
        AgentState.ERROR       to setOf(AgentState.IDLE, AgentState.LISTENING),
        AgentState.SUSPENDED   to setOf(AgentState.IDLE)
    )

    companion object {
        private const val MAX_WINDOW = 100
    }
}
