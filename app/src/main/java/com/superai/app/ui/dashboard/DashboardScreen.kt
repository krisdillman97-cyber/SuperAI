package com.superai.app.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.superai.app.agent.profile.AgentProfile
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAgentClick: (String) -> Unit,
    vm: DashboardViewModel = hiltViewModel()
) {
    val profiles     by vm.profiles.collectAsState()
    val activeProfile by vm.activeProfile.collectAsState()
    var showCreate   by remember { mutableStateOf(false) }
    var hudRunning   by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SuperAI", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            "${profiles.size} agent${if (profiles.size != 1) "s" else ""}" +
                                (activeProfile?.let { " · Active: ${it.name}" } ?: ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { hudRunning = !hudRunning; vm.toggleOverlay(hudRunning) }) {
                        Icon(
                            if (hudRunning) Icons.Filled.LayersClear else Icons.Filled.Layers,
                            "Toggle HUD",
                            tint = if (hudRunning) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { showCreate = true }) {
                        Icon(Icons.Filled.Add, "New Agent")
                    }
                }
            )
        }
    ) { padding ->
        if (profiles.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🤖", fontSize = 64.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("No agents yet", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Tap + to create your first agent",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { showCreate = true }) { Text("Create Agent") }
                }
            }
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(profiles, key = { it.id }) { profile ->
                    AgentCard(
                        profile   = profile,
                        isActive  = profile.id == activeProfile?.id,
                        onClick   = { onAgentClick(profile.id) },
                        onActivate = { vm.activateAgent(profile.id) },
                        onDelete  = { vm.deleteAgent(profile) }
                    )
                }
            }
        }
    }

    if (showCreate) {
        CreateAgentDialog(onCreate = { name, inst, emoji ->
            vm.createAgent(name, inst, emoji)
            showCreate = false
        }, onDismiss = { showCreate = false })
    }
}

@Composable
private fun AgentCard(
    profile: AgentProfile,
    isActive: Boolean,
    onClick: () -> Unit,
    onActivate: () -> Unit,
    onDelete: () -> Unit
) {
    val borderColor = if (isActive) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            width = if (isActive) 2.dp else 1.dp
        )
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(profile.color)),
                Alignment.Center
            ) {
                Text(profile.avatarEmoji, fontSize = 26.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(profile.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (profile.systemInstructions.isNotBlank())
                    Text(profile.systemInstructions, fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "${profile.totalDirectivesProcessed} directives · " +
                        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(profile.createdAt)),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (!isActive)
                    IconButton(onClick = onActivate) {
                        Icon(Icons.Filled.PlayArrow, "Activate",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                else
                    Icon(Icons.Filled.CheckCircle, "Active",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun CreateAgentDialog(onCreate: (String, String, String) -> Unit, onDismiss: () -> Unit) {
    var name  by remember { mutableStateOf("") }
    var inst  by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🤖") }
    val emojis = listOf("🤖","🧠","⚡","🔥","🌟","🛸","👾","🦾","🧬","🌌")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Agent") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = inst, onValueChange = { inst = it },
                    label = { Text("System Instructions") }, minLines = 3,
                    modifier = Modifier.fillMaxWidth())
                Text("Avatar", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    emojis.forEach { e ->
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (e == emoji) MaterialTheme.colorScheme.primary.copy(0.3f)
                                    else Color.Transparent
                                )
                                .clickable { emoji = e },
                            Alignment.Center
                        ) { Text(e, fontSize = 20.sp) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onCreate(name, inst, emoji) },
                enabled = name.isNotBlank()) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
