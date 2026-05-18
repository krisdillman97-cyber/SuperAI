package com.superai.app.agent.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import java.util.LinkedList
import java.util.UUID

// ── State sealed class ────────────────────────────────────────────────────────
sealed class AgentState {
    object Idle : AgentState()
    data class Processing(val directive: String, val step: Int = 0) : AgentState()
    data class Building(val target: String, val progress: Int = 0) : AgentState()
    data class Executing(val command: String) : AgentState()
    data class Completed(val result: DirectiveResult) : AgentState()
    data class Error(val message: String, val recoverable: Boolean = true) : AgentState()
    object Paused : AgentState()
}

data class DirectiveResult(
    val id: String = UUID.randomUUID().toString(),
    val directive: String,
    val output: String,
    val success: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val durationMs: Long = 0L,
    val artifacts: List<String> = emptyList()
)

// ── Sliding-window context buffer ─────────────────────────────────────────────
data class ContextEntry(
    val role: String,       // "user" | "agent" | "system" | "result"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val tokenEstimate: Int = (content.length / 4).coerceAtLeast(1)
)

class ContextWindowBuffer(
    private val maxTokens: Int = 131_072,
    private val pruneToFraction: Float = 0.70f
) {
    private val _entries: LinkedList<ContextEntry> = LinkedList()
    private var _totalTokens: Int = 0

    val entries: List<ContextEntry> get() = _entries.toList()
    val totalTokens: Int get() = _totalTokens
    val size: Int get() = _entries.size

    fun push(entry: ContextEntry) {
        _entries.addLast(entry)
        _totalTokens += entry.tokenEstimate
        if (_totalTokens > maxTokens) prune()
    }

    private fun prune() {
        val target = (maxTokens * pruneToFraction).toInt()
        while (_totalTokens > target && _entries.isNotEmpty()) {
            val removed = _entries.removeFirst()
            _totalTokens -= removed.tokenEstimate
        }
        Timber.d("Context pruned: ${_entries.size} entries, ~$_totalTokens tokens")
    }

    fun buildContextBlock(systemInstructions: String, safetyLevel: Float): String = buildString {
        appendLine("=== SYSTEM ===")
        appendLine(systemInstructions.ifBlank { "General-purpose agent. Process all directives." })
        appendLine("[SAFETY: $safetyLevel | ${safetyLabel(safetyLevel)}]")
        appendLine()
        appendLine("=== CONTEXT (${_entries.size} entries, ~$_totalTokens tokens) ===")
        _entries.forEach { e -> appendLine("[${e.role.uppercase()}] ${e.content}") }
    }

    fun clear() { _entries.clear(); _totalTokens = 0 }

    private fun safetyLabel(v: Float) = when {
        v < 0.2f -> "UNRESTRICTED"
        v < 0.4f -> "LOW"
        v < 0.6f -> "MEDIUM"
        v < 0.8f -> "HIGH"
        else     -> "MAXIMUM"
    }
}

// ── State machine ─────────────────────────────────────────────────────────────
class AgentStateMachine(
    val agentId: String,
    val systemInstructions: String,
    initialSafetyLevel: Float = 0.7f
) {
    private val _state = MutableStateFlow<AgentState>(AgentState.Idle)
    val state: StateFlow<AgentState> = _state.asStateFlow()

    private val _resultLog = MutableStateFlow<List<DirectiveResult>>(emptyList())
    val resultLog: StateFlow<List<DirectiveResult>> = _resultLog.asStateFlow()

    var safetyLevel: Float = initialSafetyLevel
        private set

    val contextWindow = ContextWindowBuffer()

    private val buildPattern = Regex("""(?i)\b(build|compile|generate|create|make|output)\b.{0,30}?\b(apk|script|project|app|module)\b""")
    private val adbPattern   = Regex("""(?i)\b(adb|deploy|install|push|sideload)\b""")
    private val stopPattern  = Regex("""(?i)\b(stop|halt|pause|cancel|abort)\b""")

    fun transition(event: AgentEvent): Boolean {
        val current = _state.value
        val next: AgentState? = when (event) {
            is AgentEvent.SubmitDirective -> when (current) {
                is AgentState.Idle, is AgentState.Completed, is AgentState.Error -> {
                    contextWindow.push(ContextEntry("user", event.directive))
                    when {
                        buildPattern.containsMatchIn(event.directive) ->
                            AgentState.Building(target = event.directive)
                        adbPattern.containsMatchIn(event.directive) ->
                            AgentState.Executing(command = event.directive)
                        else ->
                            AgentState.Processing(directive = event.directive)
                    }
                }
                else -> null
            }
            is AgentEvent.Progress -> when (current) {
                is AgentState.Processing -> current.copy(step = event.value)
                is AgentState.Building   -> current.copy(progress = event.value)
                else -> null
            }
            is AgentEvent.Complete -> {
                val result = DirectiveResult(
                    directive = event.directive,
                    output    = event.output,
                    success   = event.success,
                    durationMs = event.durationMs,
                    artifacts  = event.artifacts
                )
                contextWindow.push(ContextEntry("result", event.output.take(512)))
                _resultLog.update { it + result }
                AgentState.Completed(result)
            }
            is AgentEvent.Fail -> AgentState.Error(event.message, event.recoverable)
            is AgentEvent.Stop -> if (stopPattern.containsMatchIn(event.reason)) AgentState.Paused else null
            is AgentEvent.Reset -> AgentState.Idle
            is AgentEvent.UpdateSafety -> { safetyLevel = event.level; null }
        }
        return if (next != null) { _state.value = next; true } else false
    }

    fun currentContextBlock(): String =
        contextWindow.buildContextBlock(systemInstructions, safetyLevel)
}

sealed class AgentEvent {
    data class SubmitDirective(val directive: String) : AgentEvent()
    data class Progress(val value: Int) : AgentEvent()
    data class Complete(
        val directive: String,
        val output: String,
        val success: Boolean,
        val durationMs: Long = 0L,
        val artifacts: List<String> = emptyList()
    ) : AgentEvent()
    data class Fail(val message: String, val recoverable: Boolean = true) : AgentEvent()
    data class Stop(val reason: String = "") : AgentEvent()
    object Reset : AgentEvent()
    data class UpdateSafety(val level: Float) : AgentEvent()
}
