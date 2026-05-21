package com.superai.app.agent.profile

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentProfileDao {

    @Query("SELECT * FROM agent_profiles ORDER BY created_at DESC")
    fun getAllProfiles(): Flow<List<AgentProfile>>

    @Query("SELECT * FROM agent_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: String): AgentProfile?

    @Query("SELECT * FROM agent_profiles WHERE is_active = 1 LIMIT 1")
    fun getActiveProfile(): Flow<AgentProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: AgentProfile)

    @Update
    suspend fun updateProfile(profile: AgentProfile)

    @Delete
    suspend fun deleteProfile(profile: AgentProfile)

    @Query("UPDATE agent_profiles SET is_active = 0")
    suspend fun deactivateAll()

    @Query("UPDATE agent_profiles SET is_active = 1 WHERE id = :id")
    suspend fun activateProfile(id: String)

    @Query("UPDATE agent_profiles SET total_directives = total_directives + 1, updated_at = :time WHERE id = :id")
    suspend fun incrementDirectiveCount(id: String, time: Long = System.currentTimeMillis())

    @Query("DELETE FROM agent_profiles WHERE id = :id")
    suspend fun deleteById(id: String)
}
