package com.superai.app.ui.storage

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.superai.app.storage.drive.DriveFileInfo
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(vm: StorageViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val uiState by vm.uiState.collectAsState()

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> vm.handleSignInResult(context, result.data) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage", fontWeight = FontWeight.Bold) },
                actions = {
                    if (uiState.isSignedIn) {
                        IconButton(onClick = { vm.refreshFiles() }) {
                            Icon(Icons.Filled.Refresh, "Refresh")
                        }
                        IconButton(onClick = { vm.signOut(context) }) {
                            Icon(Icons.Filled.Logout, "Sign Out")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (!uiState.isSignedIn) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.CloudOff, null, Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(0.3f))
                        Spacer(Modifier.height(16.dp))
                        Text("Not connected to Drive", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { signInLauncher.launch(vm.getSignInIntent(context)) }) {
                            Icon(Icons.Filled.CloudUpload, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Connect Google Drive")
                        }
                    }
                }
            } else {
                // Account info
                Card(Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AccountCircle, null, Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Google Drive", fontWeight = FontWeight.SemiBold)
                            Text(uiState.accountName ?: "", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                        }
                        Spacer(Modifier.weight(1f))
                        Text("${uiState.files.size} files", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }

                if (uiState.isLoading) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.files.isEmpty()) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text("No files found", color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(uiState.files, key = { it.id }) { file ->
                            DriveFileCard(file, onDelete = { vm.deleteFile(file.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DriveFileCard(file: DriveFileInfo, onDelete: () -> Unit) {
    Card(shape = RoundedCornerShape(10.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                when {
                    file.mimeType.contains("folder") -> Icons.Filled.Folder
                    file.mimeType.contains("image")  -> Icons.Filled.Image
                    else -> Icons.Filled.InsertDriveFile
                }, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(file.name, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(
                    buildString {
                        if (file.size > 0) append("${file.size / 1024} KB · ")
                        append(SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(file.modifiedTime)))
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error.copy(0.6f))
            }
        }
    }
}
