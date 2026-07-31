@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.mckaifu.app.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mckaifu.app.data.model.FileEntry
import com.mckaifu.app.service.FileManagerService
import com.mckaifu.app.ui.component.*
import com.mckaifu.app.ui.theme.*
import com.mckaifu.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(serverId: String, navController: NavController, vm: MainViewModel = viewModel()) {
    val servers by vm.servers.collectAsState()
    val server = servers.find { it.id == serverId }
    val fileManager = remember { FileManagerService() }

    var currentPath by remember { mutableStateOf("") }
    var files by remember { mutableStateOf<List<FileEntry>>(emptyList()) }
    var breadcrumbs by remember { mutableStateOf(listOf("根目录")) }
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showTextEditor by remember { mutableStateOf(false) }
    var editFileContent by remember { mutableStateOf("") }
    var editFileName by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var selectedFile by remember { mutableStateOf<FileEntry?>(null) }

    fun loadFiles(path: String) {
        val baseDir = if (server != null) vm.repository.getServerDir(server).absolutePath else "/"
        val fullPath = if (path.isEmpty()) baseDir else "$baseDir/$path"
        files = fileManager.listFiles(fullPath)
    }

    LaunchedEffect(currentPath) {
        loadFiles(currentPath)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("文件管理", fontWeight = FontWeight.Bold)
                        Text(server?.name ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { currentPath = "" }) {
                        Icon(Icons.Filled.Home, "根目录")
                    }
                    IconButton(onClick = { showCreateFolderDialog = true }) {
                        Icon(Icons.Filled.CreateNewFolder, "新建文件夹")
                    }
                    IconButton(onClick = { showCreateFileDialog = true }) {
                        Icon(Icons.Filled.NoteAdd, "新建文件")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(ZalithBackground)
        ) {
            // Breadcrumbs
            Surface(color = ZalithSurfaceVariant) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    breadcrumbs.forEachIndexed { index, crumb ->
                        if (index > 0) {
                            Icon(Icons.Filled.ChevronRight, null,
                                modifier = Modifier.size(16.dp),
                                tint = TextSecondary)
                        }
                        TextButton(
                            onClick = {
                                val newBreadcrumbs = breadcrumbs.take(index + 1)
                                breadcrumbs = newBreadcrumbs
                                currentPath = newBreadcrumbs.drop(1).joinToString("/")
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(crumb, style = MaterialTheme.typography.labelMedium,
                                color = if (index == breadcrumbs.lastIndex) ZalithPrimary else TextSecondary)
                        }
                    }
                }
            }

            // File list
            LazyColumn(
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (currentPath.isNotEmpty()) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = ZalithCardBorder.copy(alpha = 0.3f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val parent = currentPath.substringBeforeLast("/")
                                        currentPath = parent
                                        breadcrumbs = if (parent.isEmpty()) listOf("根目录")
                                        else listOf("根目录") + parent.split("/")
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.FolderOpen, null, tint = ServerStarting)
                                Spacer(Modifier.width(8.dp))
                                Text("返回上级", color = TextSecondary)
                            }
                        }
                    }
                }

                items(files) { file ->
                    FileItem(
                        file = file,
                        onClick = {
                            if (file.isDirectory) {
                                val newPath = if (currentPath.isEmpty()) file.name else "$currentPath/${file.name}"
                                currentPath = newPath
                                breadcrumbs = listOf("根目录") + newPath.split("/")
                            } else if (fileManager.isEditableFile(file.name)) {
                                selectedFile = file
                                val baseDir = if (server != null) vm.repository.getServerDir(server).absolutePath else "/"
                                val fullPath = if (currentPath.isEmpty()) "$baseDir/${file.name}" else "$baseDir/$currentPath/${file.name}"
                                editFileName = file.name
                                editFileContent = fileManager.readFile(fullPath) ?: ""
                                showTextEditor = true
                            }
                        },
                        onDelete = {
                            selectedFile = file
                            showDeleteConfirm = true
                        }
                    )
                }
            }
        }
    }

    if (showCreateFileDialog) {
        CreateFileDialog(
            onConfirm = { name ->
                val baseDir = if (server != null) vm.repository.getServerDir(server).absolutePath else "/"
                val fullPath = if (currentPath.isEmpty()) "$baseDir/$name" else "$baseDir/$currentPath/$name"
                fileManager.createFile(fullPath)
                showCreateFileDialog = false
                loadFiles(currentPath)
            },
            onDismiss = { showCreateFileDialog = false }
        )
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onConfirm = { name ->
                val baseDir = if (server != null) vm.repository.getServerDir(server).absolutePath else "/"
                val fullPath = if (currentPath.isEmpty()) "$baseDir/$name" else "$baseDir/$currentPath/$name"
                fileManager.createDirectory(fullPath)
                showCreateFolderDialog = false
                loadFiles(currentPath)
            },
            onDismiss = { showCreateFolderDialog = false }
        )
    }

    if (showTextEditor) {
        AlertDialog(
            onDismissRequest = { showTextEditor = false },
            containerColor = ZalithSurface,
            title = { Text("编辑: $editFileName", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editFileContent,
                    onValueChange = { editFileContent = it },
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    minLines = 5,
                    maxLines = 20,
                    colors = outFieldColors()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val baseDir = if (server != null) vm.repository.getServerDir(server).absolutePath else "/"
                        val fullPath = if (currentPath.isEmpty()) "$baseDir/$editFileName" else "$baseDir/$currentPath/$editFileName"
                        fileManager.writeFile(fullPath, editFileContent)
                        showTextEditor = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ZalithPrimary)
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showTextEditor = false }) { Text("取消", color = TextSecondary) }
            }
        )
    }

    if (showDeleteConfirm && selectedFile != null) {
        ConfirmDeleteDialog(
            message = "确定要删除 ${selectedFile!!.name} 吗？",
            onConfirm = {
                val baseDir = if (server != null) vm.repository.getServerDir(server).absolutePath else "/"
                val fullPath = if (currentPath.isEmpty()) "$baseDir/${selectedFile!!.name}" else "$baseDir/$currentPath/${selectedFile!!.name}"
                fileManager.delete(fullPath)
                showDeleteConfirm = false
                loadFiles(currentPath)
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@Composable
fun FileItem(file: FileEntry, onClick: () -> Unit, onDelete: () -> Unit) {
    val extension = file.extension.lowercase()
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = ZalithCardBorder.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (file.isDirectory) Icons.Filled.Folder else
                    when (extension) {
                        "jar" -> Icons.Filled.Extension
                        "yml", "yaml" -> Icons.Filled.Settings
                        "properties" -> Icons.Filled.Tune
                        "json" -> Icons.Filled.DataObject
                        "txt", "md" -> Icons.Filled.Description
                        "phar" -> Icons.Filled.Archive
                        else -> Icons.Filled.InsertDriveFile
                    },
                null,
                tint = if (file.isDirectory) ServerStarting
                else when (extension) {
                    "jar" -> ZalithPrimary
                    "yml", "yaml", "properties" -> ZalithSecondary
                    "phar" -> ServerError
                    else -> TextSecondary
                },
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(file.name, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row {
                    Text(formatBytes(file.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary)
                    if (file.isDirectory) {
                        Text(" | 目录",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary)
                    }
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Delete, "删除", tint = ServerError.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
    bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
    else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
}

@Composable
fun CreateFileDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ZalithSurface,
        title = { Text("新建文件", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("文件名") },
                placeholder = { Text("example.yml") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = outFieldColors()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                colors = ButtonDefaults.buttonColors(containerColor = ZalithPrimary)
            ) { Text("创建") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
        }
    )
}

@Composable
fun CreateFolderDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ZalithSurface,
        title = { Text("新建文件夹", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("文件夹名") },
                placeholder = { Text("my_folder") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = outFieldColors()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                colors = ButtonDefaults.buttonColors(containerColor = ZalithPrimary)
            ) { Text("创建") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
        }
    )
}
