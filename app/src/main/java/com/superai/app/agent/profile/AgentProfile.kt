package com.superai.app.agent.profile

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.parcelize.Parcelize
import java.util.UUID

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

    @ColumnInfo(name = "illicit_filter_level")
    val illicitFilterLevel: Float = 0.7f,

    @ColumnInfo(name = "safety_enabled")
    val safetyEnabled: Boolean = true,

    @ColumnInfo(name = "illicit_filter_enabled")
    val illicitFilterEnabled: Boolean = true,

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
    val apiKeyHint: String = "",

    @ColumnInfo(name = "avatar_emoji")
    val avatarEmoji: String = "🤖"
) : Parcelable
