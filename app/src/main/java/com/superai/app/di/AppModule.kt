package com.superai.app.di

import android.content.Context
import androidx.room.Room
import com.superai.app.agent.profile.AgentProfileDao
import com.superai.app.compiler.adb.AdbOrchestrator
import com.superai.app.compiler.builder.CompilerOrchestrator
import com.superai.app.compiler.script.BuildScriptGenerator
import com.superai.app.storage.drive.DriveRepository
import com.superai.app.storage.local.SuperAIDatabase
import com.superai.app.ui.theme.ThemeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): SuperAIDatabase =
        Room.databaseBuilder(ctx, SuperAIDatabase::class.java, "superai.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides @Singleton
    fun provideAgentProfileDao(db: SuperAIDatabase): AgentProfileDao =
        db.agentProfileDao()

    @Provides @Singleton
    fun provideDriveRepository(@ApplicationContext ctx: Context): DriveRepository =
        DriveRepository(ctx)

    @Provides @Singleton
    fun provideThemeRepository(@ApplicationContext ctx: Context): ThemeRepository =
        ThemeRepository(ctx)

    @Provides @Singleton
    fun provideBuildScriptGenerator(@ApplicationContext ctx: Context): BuildScriptGenerator =
        BuildScriptGenerator(ctx)

    @Provides @Singleton
    fun provideAdbOrchestrator(): AdbOrchestrator = AdbOrchestrator()

    @Provides @Singleton
    fun provideCompilerOrchestrator(
        @ApplicationContext ctx: Context,
        scriptGen: BuildScriptGenerator,
        adb: AdbOrchestrator
    ): CompilerOrchestrator = CompilerOrchestrator(ctx, scriptGen, adb)
}
