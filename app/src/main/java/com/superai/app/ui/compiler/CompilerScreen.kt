package com.superai.app.ui.compiler

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompilerScreen(vm: CompilerViewModel = hiltViewModel()) {
    val state by vm.compilerState.collectAsState()
    var projectName by remember { mutableStateOf("SuperAIProject") }
    var packageName by remember { mutableStateOf("com.superai.generated") }
    val listState   = rememberLazyListState()

    LaunchedEffect(state.log.size) {
        if (state.log.isNotEmpty()) listState.animateScrollToItem(state.log.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compiler", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { vm.clearLog() }) {
                        Icon(Icons.Filled.Clear, "Clear")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {

            // Config card
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Build Configuration", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(value = projectName, onValueChange = { projectName = it },
                        label = { Text("Project Name") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = packageName, onValueChange = { packageName = it },
                        label = { Text("Package Name") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth())
                }
            }

            Spacer(Modifier.height(12.dp))

            // Progress
            if (state.isRunning) {
                LinearProgressIndicator(
                    progress = { state.progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text("${state.phase} — ${state.progress}%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
            } else if (state.phase.isNotBlank() && state.phase != "Idle") {
                val isError = state.errorMessage != null
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isError) Icons.Filled.Error else Icons.Filled.CheckCircle,
                        null,
                        tint = if (isError) MaterialTheme.colorScheme.error
                               else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        state.errorMessage ?: "Build complete: ${state.outputApkPath}",
                        fontSize = 12.sp,
                        color = if (isError) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // Log output
            Card(
                Modifier.weight(1f).fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (state.log.isEmpty()) {
                        item { Text("Build output will appear here…",
                            fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                            color = Color.Gray) }
                    } else {
                        items(state.log) { line ->
                            val color = when {
                                line.contains("ERROR", true) || line.contains("FAIL", true) -> Color(0xFFFF5252)
                                line.contains("SUCCESS", true) || line.contains("COMPLETE", true) -> Color(0xFF69F0AE)
                                line.contains("%") -> Color(0xFF82B1FF)
                                else -> Color(0xFFE0E0E0)
                            }
                            Text(line, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = color)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Action buttons
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { vm.startBuild(projectName, packageName) },
                    enabled = !state.isRunning,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Build, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Build APK")
                }
                if (state.outputApkPath != null) {
                    OutlinedButton(
                        onClick = { vm.deployApk() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PhoneAndroid, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Deploy")
                    }
                }
            }
        }
    }
}
