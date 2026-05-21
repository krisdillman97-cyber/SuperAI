package com.superai.app.ui.dashboard

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.superai.app.agent.profile.AgentProfile

@Composable
fun DashboardScreen(
    onAgentClick: (String) -> Unit = {},
    onCompiler: () -> Unit = {},
    onStorage: () -> Unit = {},
    onSettings: () -> Unit = {},
    vm: DashboardViewModel = hiltViewModel()
) {
    val profiles by vm.profiles.collectAsState()
    val active by vm.activeProfile.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreate = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) { Icon(Icons.Default.Add, "New Agent") }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SuperAI", style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                Row {
                    IconButton(onClick = onCompiler) {
                        Icon(Icons.Default.Build, "Compiler", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    IconButton(onClick = onStorage) {
                        Icon(Icons.Default.Storage, "Storage", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, "Settings", tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
            // Active agent banner
            active?.let { a ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(a.avatarEmoji, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Active Agent", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary)
                            Text(a.name, style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Agents (${profiles.size})", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(profiles, key = { it.id }) { profile ->
                    AgentCard(
                        profile  = profile,
                        isActive = profile.id == active?.id,
                        onClick  = { onAgentClick(profile.id) },
                        onActivate = { vm.activateAgent(profile.id) },
                        onDelete  = { vm.deleteAgent(profile) }
                    )
                }
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("New Agent") },
            text = {
                OutlinedTextField(value = newName, onValueChange = { newName = it },
                    label = { Text("Agent Name") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) { vm.createAgent(newName); newName = ""; showCreate = false }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false; newName = "" }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AgentCard(
    profile: AgentProfile,
    isActive: Boolean,
    onClick: () -> Unit,
    onActivate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isActive) CardDefaults.outlinedCardBorder() else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(profile.color)),
                contentAlignment = Alignment.Center
            ) {
                Text(profile.avatarEmoji, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(profile.name, color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text("Directives: ${profile.totalDirectivesProcessed}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodySmall)
                if (!profile.safetyEnabled || !profile.illicitFilterEnabled) {
                    Text("⚠ Filter(s) OFF",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall)
                }
            }
            if (!isActive) {
                IconButton(onClick = onActivate) {
                    Icon(Icons.Default.PlayArrow, "Activate",
                        tint = MaterialTheme.colorScheme.primary)
                }
            } else {
                Icon(Icons.Default.CheckCircle, "Active",
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
            }
        }
    }
}
