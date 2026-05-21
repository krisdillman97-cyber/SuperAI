package com.superai.app.ui.compiler

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.superai.app.compiler.builder.CompilerOrchestrator
import com.superai.app.compiler.builder.CompilerState
import com.superai.app.compiler.script.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompilerViewModel @Inject constructor(
    private val orchestrator: CompilerOrchestrator
) : ViewModel() {
    val state: StateFlow<CompilerState> = orchestrator.state
    fun build(projectName: String, packageName: String) = viewModelScope.launch {
        orchestrator.build(BuildConfig(projectName = projectName, packageName = packageName))
    }
    fun clearLog() = orchestrator.clearLog()
}
