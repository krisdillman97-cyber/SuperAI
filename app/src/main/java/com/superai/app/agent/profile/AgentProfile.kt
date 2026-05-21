package com.superai.app.agent.profile

import android.os.Parcelable
import androidx.room.*
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@Parcelize
@Entity(tableName = "agent_profiles")
data class AgentProfile(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "name")
    val name: String = "New Agent",

    @ColumnInfo(name = "system_instructions")
    val systemInstructions: String = "",

    @ColumnInfo(name = "compilation_target")
    val compilationTarget: String = "",

    @ColumnInfo(name = "safety_level")
    val safetyLevel: Float = 0.7f,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "color")
    val color: Long = 0xFF6200EEL,

    @ColumnInfo(name = "objective_summary")
    val objectiveSummary: String = "",

    @ColumnInfo(name = "total_directives")
    val totalDirectivesProcessed: Int = 0,

    @ColumnInfo(name = "last_build_script_path")
    val lastBuildScriptPath: String = "",

    @ColumnInfo(name = "model_endpoint")
    val modelEndpoint: String = "",

    @ColumnInfo(name = "api_key_hint")
    val apiKeyHint: String = "",      // last 4 chars only — never store full key here

    @ColumnInfo(name = "avatar_emoji")
    val avatarEmoji: String = "🤖"
) : Parcelable

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
