package com.superai.app.ui.compiler

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@Composable
fun CompilerScreen(onBack: () -> Unit = {}, vm: CompilerViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    var projectName by remember { mutableStateOf("SuperAIProject") }
    var packageName by remember { mutableStateOf("com.superai.generated") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text("Compiler Engine", style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = projectName, onValueChange = { projectName = it },
                    label = { Text("Project Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = packageName, onValueChange = { packageName = it },
                    label = { Text("Package Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                if (state.isRunning) {
                    LinearProgressIndicator(progress = { state.progress / 100f }, modifier = Modifier.fillMaxWidth())
                    Text(state.phase, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { vm.build(projectName, packageName) },
                        enabled = !state.isRunning,
                        modifier = Modifier.weight(1f)
                    ) { Text(if (state.isRunning) "Building…" else "Build APK") }
                    OutlinedButton(onClick = vm::clearLog) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                    }
                }
                state.outputApkPath?.let {
                    Text("✓ APK: $it", color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall)
                }
                state.errorMessage?.let {
                    Text("✗ Error: $it", color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        // Build log
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                reverseLayout = true
            ) {
                items(state.log.reversed()) { line ->
                    Text(line, fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
