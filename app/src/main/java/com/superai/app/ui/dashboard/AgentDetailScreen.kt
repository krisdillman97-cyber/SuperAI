package com.superai.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AgentDetailScreen(
    profileId: String,
    onBack: () -> Unit = {},
    onSettings: () -> Unit = {},
    vm: AgentDetailViewModel = hiltViewModel()
) {
    val profile by vm.profile.collectAsState()
    LaunchedEffect(profileId) { vm.load(profileId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
                }
                Text("Agent Detail", style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, "Settings", tint = MaterialTheme.colorScheme.onBackground)
            }
        }

        profile?.let { p ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p.avatarEmoji, style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(p.name, style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            Text(if (p.isActive) "● Active" else "○ Inactive",
                                color = if (p.isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    DetailRow("Directives Processed", "${p.totalDirectivesProcessed}")
                    DetailRow("Safety Filter", if (p.safetyEnabled) "ON (${(p.safetyLevel * 100).toInt()}%)" else "OFF")
                    DetailRow("Illicit Filter", if (p.illicitFilterEnabled) "ON (${(p.illicitFilterLevel * 100).toInt()}%)" else "OFF")
                    if (p.modelEndpoint.isNotBlank()) DetailRow("Endpoint", p.modelEndpoint)
                    if (p.objectiveSummary.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Objective", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                        Text(p.objectiveSummary, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        } ?: CircularProgressIndicator(modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(32.dp))
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall)
        Text(value, color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}
