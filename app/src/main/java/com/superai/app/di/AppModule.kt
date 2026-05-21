package com.superai.app.di

import android.content.Context
import androidx.room.Room
import com.superai.app.agent.profile.AgentProfileDao
import com.superai.app.storage.local.SuperAIDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): SuperAIDatabase =
        Room.databaseBuilder(ctx, SuperAIDatabase::class.java, "superai.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideAgentProfileDao(db: SuperAIDatabase): AgentProfileDao =
        db.agentProfileDao()

    // DriveRepository, ThemeRepository, CompilerOrchestrator, AdbOrchestrator,
    // and BuildScriptGenerator all use @Singleton + @Inject constructor — Hilt
    // auto-provides them, so no manual @Provides entries are needed here.
}
