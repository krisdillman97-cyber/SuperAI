package com.superai.app.storage.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.superai.app.agent.profile.AgentProfile
import com.superai.app.agent.profile.AgentProfileDao

@Database(
    entities = [AgentProfile::class],
    version  = 2,
    exportSchema = false
)
abstract class SuperAIDatabase : RoomDatabase() {
    abstract fun agentProfileDao(): AgentProfileDao
}
