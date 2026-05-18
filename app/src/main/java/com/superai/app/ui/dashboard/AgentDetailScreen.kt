package com.superai.app.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.superai.app.agent.state.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentDetailScreen(
    agentId: String,
    onBack: () -> Unit,
    vm: AgentDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(agentId) { vm.loadProfile(agentId) }

    val profile   by vm.profile.collectAsState()
    val state     by vm.agentState.collectAsState()
    val resultLog by vm.resultLog.collectAsState()
    var input     by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(profile?.name ?: "Agent", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = { vm.resetState() }) {
                        Icon(Icons.Filled.Refresh, "Reset")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = input, onValueChange = { input = it },
                        placeholder = { Text("Enter directive…") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = { if (input.isNotBlank()) { vm.submitDirective(input); input = "" } },
                        enabled = input.isNotBlank() && state is AgentState.Idle ||
                                  state is AgentState.Completed || state is AgentState.Error
                    ) { Icon(Icons.Filled.Send, "Send") }
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {

            // State chip
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                StateChip(state)
            }

            // Result log
            if (resultLog.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(profile?.avatarEmoji ?: "🤖", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("No directives yet", color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                    }
                }
            } else {
                LazyColumn(
                    Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    reverseLayout = true
                ) {
                    items(resultLog.reversed(), key = { it.id }) { result ->
                        ResultCard(result)
                    }
                }
            }
        }
    }
}

@Composable
private fun StateChip(state: AgentState) {
    val (label, color) = when (state) {
        is AgentState.Idle        -> "Idle"       to MaterialTheme.colorScheme.surfaceVariant
        is AgentState.Processing  -> "Processing…" to MaterialTheme.colorScheme.secondary
        is AgentState.Building    -> "Building ${state.progress}%" to MaterialTheme.colorScheme.tertiary
        is AgentState.Executing   -> "Executing"  to MaterialTheme.colorScheme.secondary
        is AgentState.Completed   -> "Done"        to MaterialTheme.colorScheme.primary
        is AgentState.Error       -> "Error"       to MaterialTheme.colorScheme.error
        is AgentState.Paused      -> "Paused"      to MaterialTheme.colorScheme.outline
    }
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.2f)) {
        Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            fontSize = 12.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ResultCard(result: DirectiveResult) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (result.success)
                MaterialTheme.colorScheme.primary.copy(0.07f)
            else
                MaterialTheme.colorScheme.error.copy(0.07f)
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (result.success) Icons.Filled.CheckCircle else Icons.Filled.Error,
                    null,
                    tint = if (result.success) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(result.directive, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                    modifier = Modifier.weight(1f))
                Text(
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(result.timestamp)),
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.4f)
                )
            }
            if (result.output.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(result.output, fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
            }
            if (result.durationMs > 0) {
                Spacer(Modifier.height(4.dp))
                Text("${result.durationMs}ms", fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.35f))
            }
        }
    }
}
