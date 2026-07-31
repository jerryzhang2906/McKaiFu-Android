@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.mckaifu.app.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mckaifu.app.data.model.ScheduledTask
import com.mckaifu.app.data.model.TaskType
import com.mckaifu.app.ui.component.*
import com.mckaifu.app.ui.theme.*
import com.mckaifu.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(serverId: String, navController: NavController, vm: MainViewModel = viewModel()) {
    val servers by vm.servers.collectAsState()
    val server = servers.find { it.id == serverId }
    var showAddDialog by remember { mutableStateOf(false) }

    val tasks = remember {
        mutableStateListOf(
            ScheduledTask(name = "每日重启", type = TaskType.RESTART, intervalHours = 24, isEnabled = true),
            ScheduledTask(name = "备份世界", type = TaskType.BACKUP, intervalHours = 12, isEnabled = true),
            ScheduledTask(name = "内存清理", type = TaskType.COMMAND, intervalHours = 6, isEnabled = false, command = "gc"),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("定时任务", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, "添加任务",
                            tint = ZalithPrimary)
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
            if (tasks.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Filled.Schedule,
                    title = "暂无定时任务",
                    subtitle = "添加定时重启、备份等计划任务",
                    action = {
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ZalithPrimary)
                        ) { Text("添加任务") }
                    }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        SectionHeader("计划任务")
                        Spacer(Modifier.height(4.dp))
                        Text("${tasks.size} 个任务 | 服务器将根据计划自动执行",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary)
                    }

                    items(tasks, key = { it.name }) { task ->
                        ScheduleTaskCard(
                            task = task,
                            onToggle = { enabled ->
                                val idx = tasks.indexOf(task)
                                if (idx >= 0) tasks[idx] = task.copy(isEnabled = enabled)
                            },
                            onDelete = { tasks.remove(task) }
                        )
                    }

                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }

    if (showAddDialog) {
        AddScheduleDialog(
            onConfirm = { newTask ->
                tasks.add(newTask)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
fun ScheduleTaskCard(
    task: ScheduledTask,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        when (task.type) {
                            TaskType.RESTART -> ServerStarting.copy(alpha = 0.2f)
                            TaskType.BACKUP -> ServerOnline.copy(alpha = 0.2f)
                            TaskType.COMMAND -> ZalithPrimary.copy(alpha = 0.2f)
                            TaskType.STOP -> ServerError.copy(alpha = 0.2f)
                            TaskType.START -> ServerOnline.copy(alpha = 0.2f)
                        },
                        MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (task.type) {
                        TaskType.RESTART -> Icons.Filled.RestartAlt
                        TaskType.BACKUP -> Icons.Filled.Backup
                        TaskType.COMMAND -> Icons.Filled.Terminal
                        TaskType.STOP -> Icons.Filled.Stop
                        TaskType.START -> Icons.Filled.PlayArrow
                    }, null,
                    tint = when (task.type) {
                        TaskType.RESTART -> ServerStarting
                        TaskType.BACKUP -> ServerOnline
                        TaskType.COMMAND -> ZalithPrimary
                        TaskType.STOP -> ServerError
                        TaskType.START -> ServerOnline
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(task.name, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall)
                Row {
                    Surface(
                        color = when (task.type) {
                            TaskType.RESTART -> ServerStarting.copy(alpha = 0.15f)
                            TaskType.BACKUP -> ServerOnline.copy(alpha = 0.15f)
                            TaskType.COMMAND -> ZalithPrimary.copy(alpha = 0.15f)
                            TaskType.STOP -> ServerError.copy(alpha = 0.15f)
                            TaskType.START -> ServerOnline.copy(alpha = 0.15f)
                        },
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(" ${task.type.displayName} ",
                            style = MaterialTheme.typography.labelSmall,
                            color = when (task.type) {
                                TaskType.RESTART -> ServerStarting
                                TaskType.BACKUP -> ServerOnline
                                TaskType.COMMAND -> ZalithPrimary
                                TaskType.STOP -> ServerError
                                TaskType.START -> ServerOnline
                            })
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("每${task.intervalHours}小时",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary)
                }
            }
            Switch(
                checked = task.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = ZalithPrimary,
                    checkedThumbColor = Color.White,
                    uncheckedTrackColor = ZalithCardBorder
                )
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = ServerError)) {
                Icon(Icons.Filled.Delete, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("删除", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun AddScheduleDialog(
    onConfirm: (ScheduledTask) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var taskType by remember { mutableStateOf(TaskType.RESTART) }
    var interval by remember { mutableStateOf("24") }
    var command by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ZalithSurface,
        title = { Text("添加定时任务", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("任务名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = outFieldColors()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskType.entries.forEach { type ->
                        FilterChip(
                            selected = taskType == type,
                            onClick = { taskType = type },
                            label = { Text(type.displayName, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ZalithPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = ZalithPrimary
                            )
                        )
                    }
                }
                OutlinedTextField(
                    value = interval,
                    onValueChange = { interval = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("间隔 (小时)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = outFieldColors()
                )
                if (taskType == TaskType.COMMAND) {
                    OutlinedTextField(
                        value = command,
                        onValueChange = { command = it },
                        label = { Text("命令") },
                        placeholder = { Text("输入要执行的命令") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = outFieldColors()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(ScheduledTask(
                        name = name.ifBlank { taskType.displayName },
                        type = taskType,
                        intervalHours = interval.toIntOrNull() ?: 24,
                        command = command
                    ))
                },
                colors = ButtonDefaults.buttonColors(containerColor = ZalithPrimary)
            ) { Text("添加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
        }
    )
}
