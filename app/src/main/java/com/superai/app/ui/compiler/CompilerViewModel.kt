package com.superai.app.ui.compiler

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.superai.app.compiler.builder.CompilerOrchestrator
import com.superai.app.compiler.builder.CompilerState
import com.superai.app.compiler.script.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompilerViewModel @Inject constructor(
    private val orchestrator: CompilerOrchestrator
) : ViewModel() {

    val compilerState: StateFlow<CompilerState> = orchestrator.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CompilerState())

    fun startBuild(projectName: String, packageName: String) = viewModelScope.launch {
        orchestrator.build(
            BuildConfig(
                projectName = projectName.ifBlank { "SuperAIProject" },
                packageName = packageName.ifBlank { "com.superai.generated" }
            )
        )
    }

    fun clearLog() = orchestrator.clearLog()

    fun deployApk(serial: String? = null) = viewModelScope.launch {
        val apkPath = orchestrator.state.value.outputApkPath ?: return@launch
        orchestrator.deployToDevice(apkPath, serial?.ifBlank { null })
    }
}
