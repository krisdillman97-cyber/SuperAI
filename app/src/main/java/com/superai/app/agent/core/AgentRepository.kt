package com.superai.app.agent.core

import com.superai.app.agent.profile.AgentProfile
import com.superai.app.agent.profile.AgentProfileDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentRepository @Inject constructor(
    private val dao: AgentProfileDao
) {
    fun getAllProfiles(): Flow<List<AgentProfile>> = dao.getAllProfiles()
    fun getActiveProfile(): Flow<AgentProfile?> = dao.getActiveProfile()
    suspend fun getById(id: String): AgentProfile? = dao.getProfileById(id)
    suspend fun save(profile: AgentProfile) = dao.insertProfile(profile)
    suspend fun update(profile: AgentProfile) = dao.updateProfile(
        profile.copy(updatedAt = System.currentTimeMillis())
    )
    suspend fun delete(profile: AgentProfile) = dao.deleteProfile(profile)
    suspend fun deleteById(id: String) = dao.deleteById(id)
    suspend fun setActive(id: String) {
        dao.deactivateAll()
        dao.activateProfile(id)
    }
    suspend fun incrementDirective(id: String) = dao.incrementDirectiveCount(id)
}
