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
import com.mckaifu.app.data.model.Difficulty
import com.mckaifu.app.data.model.GameMode
import com.mckaifu.app.data.model.WorldInfo
import com.mckaifu.app.ui.component.*
import com.mckaifu.app.ui.theme.*
import com.mckaifu.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldManagementScreen(serverId: String, navController: NavController, vm: MainViewModel = viewModel()) {
    val servers by vm.servers.collectAsState()
    val server = servers.find { it.id == serverId }
    var refreshKey by remember { mutableIntStateOf(0) }

    val worlds = remember(serverId, refreshKey) { vm.listWorlds(serverId) }
    val backups = remember(serverId, refreshKey) { vm.listBackups(serverId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("世界管理", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { refreshKey++ }) {
                        Icon(Icons.Filled.Refresh, "刷新")
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
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("${server?.name ?: ""} 的世界",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("管理世界文件、备份与导入",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary)
                }

                item {
                    SectionHeader("操作")
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SmallActionChip(Icons.Filled.Backup, "立即备份", true, {
                            vm.backupWorld(serverId)
                            refreshKey++
                        }, ZalithPrimary)
                        SmallActionChip(Icons.Filled.Download, "下载世界", false, { }, ZalithPrimary)
                        SmallActionChip(Icons.Filled.Add, "创建世界", false, { }, ServerOnline)
                    }
                }

                if (worlds.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Filled.Public,
                            title = "暂无世界",
                            subtitle = "启动服务器生成世界后,将在这里显示"
                        )
                    }
                } else {
                    items(worlds) { world ->
                        WorldCard(world, serverId, vm)
                    }
                }

                item {
                    SectionHeader("备份管理")
                    Spacer(Modifier.height(8.dp))
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("自动备份", fontWeight = FontWeight.Bold)
                                    Text(
                                        if (server?.autoBackup == true) "每${server?.backupIntervalHours ?: 24}小时"
                                        else "已关闭",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                Switch(
                                    checked = server?.autoBackup ?: false,
                                    onCheckedChange = {
                                        server?.let { s ->
                                            vm.updateServer(s.copy(autoBackup = it))
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = ZalithPrimary,
                                        checkedThumbColor = Color.White
                                    )
                                )
                            }
                            if (backups.isEmpty()) {
                                Text("暂无备份",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary)
                            } else {
                                backups.take(5).forEach { backup ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(backup.fileName,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium)
                                            Text(formatFileSize(backup.sizeBytes),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary)
                                        }
                                        TextButton(onClick = {
                                            vm.restoreBackup(serverId, backup.fileName)
                                            refreshKey++
                                        }) { Text("恢复", color = ServerStarting) }
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
fun WorldCard(world: WorldInfo, serverId: String, vm: MainViewModel) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            when (world.id) {
                                "overworld" -> ServerOnline
                                "nether" -> ServerError
                                "end" -> ZalithPrimary
                                else -> ZalithSecondary
                            }.copy(alpha = 0.2f),
                            MaterialTheme.shapes.medium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        when (world.id) {
                            "overworld" -> Icons.Filled.Park
                            "nether" -> Icons.Filled.LocalFireDepartment
                            "end" -> Icons.Filled.NightsStay
                            else -> Icons.Filled.Public
                        }, null,
                        tint = when (world.id) {
                            "overworld" -> ServerOnline
                            "nether" -> ServerError
                            "end" -> ZalithPrimary
                            else -> ZalithSecondary
                        }, modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(world.name, fontWeight = FontWeight.Bold)
                    Text("难度: ${world.difficulty.displayName} | 模式: ${world.gameMode.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = { vm.sendCommand(serverId, "mv tp ${world.name}") }) {
                Icon(Icons.Filled.Send, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("传送", style = MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick = { /* backup */ }) {
                Icon(Icons.Filled.Backup, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("备份", style = MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick = { /* download */ }) {
                Icon(Icons.Filled.Download, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("下载", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
