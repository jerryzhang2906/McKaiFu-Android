@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.mckaifu.app.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mckaifu.app.data.model.CoreType
import com.mckaifu.app.data.model.ServerInstance
import com.mckaifu.app.data.model.ServerStatus
import com.mckaifu.app.ui.component.*
import com.mckaifu.app.ui.navigation.Screen
import com.mckaifu.app.ui.theme.*
import com.mckaifu.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerListScreen(navController: NavController, vm: MainViewModel = viewModel()) {
    val servers by vm.servers.collectAsState()
    val statuses by vm.serverStatuses.collectAsState()
    val context = LocalContext.current
    var deleteTarget by remember { mutableStateOf<ServerInstance?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("McKaiFu", fontWeight = FontWeight.Bold)
                        Text("开服大师",
                            style = MaterialTheme.typography.bodySmall,
                            color = ZalithPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val current = ZalithThemeState.themeMode.value
                        val effDark = ZalithThemeState.isDark.value
                        val next = if (current == "system") {
                            if (effDark) "light" else "dark"
                        } else if (current == "dark") "light" else "dark"
                        ZalithThemeState.setMode(next, context)
                    }) {
                        Icon(
                            if (ZalithThemeState.isDark.value) Icons.Outlined.LightMode else Icons.Filled.DarkMode,
                            "切换主题"
                        )
                    }
                    IconButton(onClick = { navController.navigate(Screen.About.route) }) {
                        Icon(Icons.Outlined.Info, "关于")
                    }
                    IconButton(onClick = { navController.navigate(Screen.CreateServer.route) }) {
                        Icon(Icons.Filled.Add, "创建服务器",
                            tint = ZalithPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(ZalithBackground)
        ) {
            if (servers.isEmpty()) {
                EmptyServerView(
                    onCreateClick = { navController.navigate(Screen.CreateServer.route) }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("我的服务器",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("共 ${servers.size} 个服务器",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary)
                    }
                    items(servers, key = { it.id }) { server ->
                        ServerCard(
                            server = server,
                            status = statuses[server.id] ?: server.status,
                            onClick = {
                                vm.selectServer(server.id)
                                navController.navigate(Screen.ServerDetail.createRoute(server.id))
                            },
                            onStart = {
                                vm.selectServer(server.id)
                                vm.startServer(server.id)
                            },
                            onStop = {
                                vm.selectServer(server.id)
                                vm.stopServer(server.id)
                            },
                            onRestart = {
                                vm.selectServer(server.id)
                                vm.restartServer(server.id)
                            },
                            onDelete = { deleteTarget = server }
                        )
                    }
                }
            }
        }
    }

    deleteTarget?.let { server ->
        ConfirmDeleteDialog(
            title = "删除服务器",
            message = "确定删除「${server.name}」吗?\n将同时删除其世界、插件、备份等所有文件,且不可恢复。",
            onConfirm = {
                vm.deleteServer(server.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
fun ServerCard(
    server: ServerInstance,
    status: ServerStatus,
    onClick: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onDelete: () -> Unit = {}
) {
    val statusColor = when (status) {
        ServerStatus.ONLINE -> ServerOnline
        ServerStatus.OFFLINE -> ServerOffline
        ServerStatus.STARTING, ServerStatus.RESTARTING -> ServerStarting
        ServerStatus.ERROR -> ServerError
        ServerStatus.STOPPING -> ServerStopping
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            ZalithCard.copy(alpha = 0.6f),
                            ZalithCard.copy(alpha = 0.3f)
                        )
                    ),
                    MaterialTheme.shapes.medium
                )
                .border(
                    1.dp, ZalithCardBorder.copy(alpha = 0.5f),
                    MaterialTheme.shapes.medium
                )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        StatusDot(
                            isOnline = status == ServerStatus.ONLINE,
                            size = 12.dp,
                            animated = true
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                server.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    server.coreType.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ZalithPrimary
                                )
                                Text(
                                    " ${server.coreVersion}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = statusColor.copy(alpha = 0.15f),
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    " ${status.displayName} ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "删除",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        if (status == ServerStatus.ONLINE) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${server.playerCount}/${server.maxPlayers} 在线",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SmallActionChip(
                        icon = Icons.Filled.PlayArrow,
                        text = "启动",
                        enabled = status == ServerStatus.OFFLINE || status == ServerStatus.ERROR,
                        onClick = onStart,
                        color = ServerOnline
                    )
                    SmallActionChip(
                        icon = Icons.Filled.Stop,
                        text = "停止",
                        enabled = status == ServerStatus.ONLINE,
                        onClick = onStop,
                        color = ServerError
                    )
                    SmallActionChip(
                        icon = Icons.Filled.RestartAlt,
                        text = "重启",
                        enabled = status == ServerStatus.ONLINE,
                        onClick = onRestart,
                        color = ServerStarting
                    )
                }

                if (status == ServerStatus.STARTING || status == ServerStatus.RESTARTING) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = ServerStarting,
                        trackColor = ZalithCardBorder
                    )
                }
            }
        }
    }
}

@Composable
fun SmallActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    color: Color
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.height(32.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.15f),
            contentColor = color,
            disabledContainerColor = ZalithSurfaceVariant,
            disabledContentColor = TextSecondary.copy(alpha = 0.3f)
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
    ) {
        Icon(icon, text, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun EmptyServerView(onCreateClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    Brush.linearGradient(
                        listOf(ZalithPrimary.copy(alpha = 0.3f), Color.Transparent)
                    ),
                    MaterialTheme.shapes.extraLarge
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Dns,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = ZalithPrimary.copy(alpha = 0.6f)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "还没有服务器",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "点击下方按钮创建你的第一个\nMinecraft服务器",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onCreateClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = ZalithPrimary
            ),
            modifier = Modifier.height(50.dp)
        ) {
            Icon(Icons.Filled.Add, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("创建服务器", fontWeight = FontWeight.Bold)
        }
    }
}
