package com.superai.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.superai.app.agent.core.AgentRepository
import com.superai.app.agent.profile.AgentProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AgentDetailViewModel @Inject constructor(
    private val repo: AgentRepository
) : ViewModel() {

    private val _profile = MutableStateFlow<AgentProfile?>(null)
    val profile: StateFlow<AgentProfile?> = _profile.asStateFlow()

    fun load(id: String) = viewModelScope.launch { _profile.value = repo.getById(id) }
    fun update(p: AgentProfile) = viewModelScope.launch { repo.update(p); _profile.value = p }
    fun delete(p: AgentProfile) = viewModelScope.launch { repo.delete(p) }
}
