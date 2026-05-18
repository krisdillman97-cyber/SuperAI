package com.superai.app.agent.core

import android.content.Context
import android.content.Intent
import com.superai.app.agent.profile.AgentProfile
import com.superai.app.agent.profile.AgentProfileDao
import com.superai.app.agent.state.AgentEvent
import com.superai.app.agent.state.AgentStateMachine
import com.superai.app.ui.overlay.FloatingHudService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: AgentProfileDao
) {
    // ── Live state machines ───────────────────────────────────────────────────
    private val machines = ConcurrentHashMap<String, AgentStateMachine>()

    // ── Active profile id ─────────────────────────────────────────────────────
    private val _activeId = MutableStateFlow<String?>(null)
    val activeProfileId: Flow<String?> = _activeId.asStateFlow()

    // ── Profile CRUD ──────────────────────────────────────────────────────────
    fun getAllProfiles(): Flow<List<AgentProfile>> = dao.getAllProfiles()
    fun getActiveProfile(): Flow<AgentProfile?> = dao.getActiveProfile()
    suspend fun getProfileById(id: String): AgentProfile? = dao.getProfileById(id)
    suspend fun createProfile(profile: AgentProfile) = dao.insertProfile(profile)
    suspend fun updateProfile(profile: AgentProfile) = dao.updateProfile(profile)
    suspend fun deleteProfile(profile: AgentProfile) {
        machines.remove(profile.id)
        dao.deleteProfile(profile)
    }

    suspend fun activateProfile(id: String) {
        dao.deactivateAll()
        dao.activateProfile(id)
        _activeId.value = id
        Timber.d("Activated agent profile: $id")
    }

    // ── State machine access ──────────────────────────────────────────────────
    fun getOrCreateMachine(profile: AgentProfile): AgentStateMachine =
        machines.getOrPut(profile.id) {
            AgentStateMachine(
                agentId = profile.id,
                systemInstructions = profile.systemInstructions,
                initialSafetyLevel = profile.safetyLevel
            )
        }

    fun getMachine(id: String): AgentStateMachine? = machines[id]

    fun dispatchEvent(profileId: String, event: AgentEvent): Boolean =
        machines[profileId]?.transition(event) ?: false

    // ── Overlay control ───────────────────────────────────────────────────────
    fun startOverlay() {
        val intent = Intent(context, FloatingHudService::class.java)
        context.startForegroundService(intent)
    }

    fun stopOverlay() {
        val intent = Intent(context, FloatingHudService::class.java).apply {
            action = FloatingHudService.ACTION_STOP
        }
        context.startService(intent)
    }
}
