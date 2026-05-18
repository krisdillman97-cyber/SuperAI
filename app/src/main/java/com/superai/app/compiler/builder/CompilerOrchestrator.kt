package com.superai.app.compiler.builder

import android.content.Context
import com.superai.app.compiler.adb.AdbOrchestrator
import com.superai.app.compiler.script.BuildConfig
import com.superai.app.compiler.script.BuildScriptGenerator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class CompilerState(
    val isRunning: Boolean = false,
    val progress: Int = 0,          // 0–100
    val phase: String = "Idle",
    val log: List<String> = emptyList(),
    val outputApkPath: String? = null,
    val errorMessage: String? = null
)

@Singleton
class CompilerOrchestrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scriptGen: BuildScriptGenerator,
    private val adb: AdbOrchestrator
) {
    private val _state = MutableStateFlow(CompilerState())
    val state: StateFlow<CompilerState> = _state.asStateFlow()

    private fun log(msg: String) {
        Timber.d(msg)
        _state.value = _state.value.copy(log = _state.value.log + msg)
    }

    private fun phase(name: String, progress: Int) {
        _state.value = _state.value.copy(phase = name, progress = progress)
        log("[$progress%] $name")
    }

    suspend fun build(config: BuildConfig): Boolean = withContext(Dispatchers.IO) {
        _state.value = CompilerState(isRunning = true, phase = "Starting", log = listOf("Build started"))
        try {
            phase("Generating build script", 10)
            val script = scriptGen.generateBuildScript(config)

            phase("Running build script", 30)
            val proc = ProcessBuilder("bash", script.absolutePath)
                .redirectErrorStream(true)
                .start()
            val output = proc.inputStream.bufferedReader().readText()
            val code   = proc.waitFor()
            output.lines().forEach { log(it) }

            if (code != 0) {
                _state.value = _state.value.copy(
                    isRunning = false, phase = "Failed",
                    errorMessage = "Build exited with code $code"
                )
                return@withContext false
            }

            phase("Locating output APK", 80)
            val outDir  = File(config.outputDir, config.projectName)
            val apkFile = outDir.listFiles()?.firstOrNull { it.name.endsWith("-debug.apk") }

            if (apkFile == null) {
                _state.value = _state.value.copy(isRunning = false, phase = "Failed",
                    errorMessage = "APK not found in ${outDir.absolutePath}")
                return@withContext false
            }

            phase("Build complete", 100)
            _state.value = _state.value.copy(
                isRunning = false, phase = "Complete",
                outputApkPath = apkFile.absolutePath
            )
            true
        } catch (e: Exception) {
            Timber.e(e, "Compiler error")
            _state.value = _state.value.copy(isRunning = false, phase = "Error",
                errorMessage = e.message)
            false
        }
    }

    suspend fun deployToDevice(apkPath: String, serial: String? = null): Boolean {
        log("Deploying $apkPath to device ${serial ?: "any"}…")
        val result = adb.install(apkPath, serial)
        log(if (result.success) "Install SUCCESS" else "Install FAILED: ${result.stderr}")
        return result.success
    }

    fun clearLog() {
        _state.value = CompilerState()
    }
}
