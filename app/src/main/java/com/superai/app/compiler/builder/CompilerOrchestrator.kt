package com.superai.app.compiler.builder

import android.content.Context
import com.superai.app.compiler.adb.AdbOrchestrator
import com.superai.app.compiler.script.BuildConfig
import com.superai.app.compiler.script.BuildScriptGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

data class CompilerState(
    val isRunning: Boolean = false,
    val progress: Int = 0,
    val phase: String = "Idle",
    val log: List<String> = emptyList(),
    val outputApkPath: String? = null,
    val errorMessage: String? = null
)

class CompilerOrchestrator(
    private val context: Context,
    private val scriptGen: BuildScriptGenerator,
    private val adb: AdbOrchestrator
) {
    private val _state = MutableStateFlow(CompilerState())
    val state: StateFlow<CompilerState> = _state.asStateFlow()

    private fun log(msg: String) {
        Timber.d(msg)
        _state.value = _state.value.copy(log = _state.value.log + msg)
    }

    private fun phase(name: String, pct: Int) {
        _state.value = _state.value.copy(phase = name, progress = pct)
        log("[$pct%] $name")
    }

    suspend fun build(config: BuildConfig): Boolean = withContext(Dispatchers.IO) {
        _state.value = CompilerState(isRunning = true, phase = "Starting", log = listOf("Build started"))
        try {
            phase("Generating build script", 10)
            val script = scriptGen.generateBuildScript(config)

            phase("Running build script", 30)
            val proc = ProcessBuilder("bash", script.absolutePath)
                .redirectErrorStream(true).start()
            proc.inputStream.bufferedReader().readText().lines().forEach { log(it) }
            val code = proc.waitFor()

            if (code != 0) {
                _state.value = _state.value.copy(isRunning = false, phase = "Failed",
                    errorMessage = "Build exited with code $code")
                return@withContext false
            }

            phase("Locating APK", 80)
            val apk = File(config.outputDir, config.projectName).listFiles()
                ?.firstOrNull { it.name.endsWith("-debug.apk") }

            if (apk == null) {
                _state.value = _state.value.copy(isRunning = false, phase = "Failed",
                    errorMessage = "APK not found")
                return@withContext false
            }

            phase("Build complete", 100)
            _state.value = _state.value.copy(isRunning = false, phase = "Complete",
                outputApkPath = apk.absolutePath)
            true
        } catch (e: Exception) {
            Timber.e(e, "Compiler error")
            _state.value = _state.value.copy(isRunning = false, phase = "Error",
                errorMessage = e.message)
            false
        }
    }

    suspend fun deployToDevice(apkPath: String, serial: String? = null): Boolean {
        log("Deploying $apkPath to ${serial ?: "any"}…")
        val result = adb.install(apkPath, serial)
        log(if (result.success) "Install SUCCESS" else "Install FAILED: ${result.stderr}")
        return result.success
    }

    fun clearLog() { _state.value = CompilerState() }
}
