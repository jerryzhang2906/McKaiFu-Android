@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.mckaifu.app.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mckaifu.app.data.model.ServerInstance
import com.mckaifu.app.data.model.ServerStatus
import com.mckaifu.app.ui.component.*
import com.mckaifu.app.ui.navigation.Screen
import com.mckaifu.app.ui.theme.*
import com.mckaifu.app.util.CompatibilityLevel
import com.mckaifu.app.util.getCompatibilityInfo
import com.mckaifu.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerDetailScreen(serverId: String, navController: NavController, vm: MainViewModel = viewModel()) {
    val servers by vm.servers.collectAsState()
    val statuses by vm.serverStatuses.collectAsState()
    val server = servers.find { it.id == serverId }
    val status = statuses[serverId] ?: server?.status ?: ServerStatus.OFFLINE

    if (server == null) {
        Text("服务器未找到", modifier = Modifier.padding(16.dp))
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(server.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate(Screen.Dashboard.createRoute(serverId))
                    }) {
                        Icon(Icons.Filled.Insights, "仪表盘")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(ZalithBackground)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ServerStatusCard(server, status, vm)
            QuickActionGrid(serverId, navController)
            ServerInfoSection(server)
            CompatibilitySection(server)
        }
    }
}

@Composable
fun ServerStatusCard(
    server: ServerInstance,
    status: ServerStatus,
    vm: MainViewModel
) {
    val statusColor = when (status) {
        ServerStatus.ONLINE -> ServerOnline
        ServerStatus.OFFLINE -> ServerOffline
        ServerStatus.STARTING, ServerStatus.RESTARTING -> ServerStarting
        ServerStatus.ERROR -> ServerError
        ServerStatus.STOPPING -> ServerStopping
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(isOnline = status == ServerStatus.ONLINE, size = 14.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(status.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = statusColor)
                    if (status == ServerStatus.ONLINE) {
                        Text("${server.playerCount}/${server.maxPlayers} 在线",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary)
                    }
                }
            }
            if (status == ServerStatus.ONLINE) {
                Surface(
                    color = ServerOnline.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(" 运行中 ",
                        style = MaterialTheme.typography.labelMedium,
                        color = ServerOnline,
                        fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val sid = server.id
            SmallActionChip(
                icon = Icons.Filled.PlayArrow, text = "启动",
                enabled = status == ServerStatus.OFFLINE || status == ServerStatus.ERROR,
                onClick = { vm.startServer(sid) }, color = ServerOnline
            )
            SmallActionChip(
                icon = Icons.Filled.Stop, text = "停止",
                enabled = status == ServerStatus.ONLINE,
                onClick = { vm.stopServer(sid) }, color = ServerError
            )
            SmallActionChip(
                icon = Icons.Filled.RestartAlt, text = "重启",
                enabled = status == ServerStatus.ONLINE,
                onClick = { vm.restartServer(sid) }, color = ServerStarting
            )
        }
    }
}

@Composable
fun QuickActionGrid(serverId: String, navController: NavController) {
    SectionHeader("快捷操作")
    Spacer(Modifier.height(8.dp))

    val actions = listOf(
        ActionItem("控制台", Icons.Filled.Terminal, Screen.Console.createRoute(serverId)),
        ActionItem("玩家管理", Icons.Filled.People, Screen.Players.createRoute(serverId)),
        ActionItem("世界管理", Icons.Filled.Public, Screen.Worlds.createRoute(serverId)),
        ActionItem("插件管理", Icons.Filled.Extension, Screen.Plugins.createRoute(serverId)),
        ActionItem("文件管理", Icons.Filled.Folder, Screen.FileManager.createRoute(serverId)),
        ActionItem("配置编辑", Icons.Filled.Tune, Screen.ConfigEditor.createRoute(serverId)),
        ActionItem("内网穿透", Icons.Filled.Wifi, Screen.Tunnel.createRoute(serverId)),
        ActionItem("聊天", Icons.Filled.Chat, Screen.Chat.createRoute(serverId)),
        ActionItem("Web地图", Icons.Filled.Map, Screen.WebMap.createRoute(serverId)),
        ActionItem("下载核心", Icons.Filled.CloudDownload, Screen.CoreDownload.createRoute(serverId)),
        ActionItem("插件商店", Icons.Filled.Store, Screen.PluginStore.createRoute(serverId)),
        ActionItem("封禁列表", Icons.Filled.Block, Screen.BanList.createRoute(serverId)),
        ActionItem("定时任务", Icons.Filled.Schedule, Screen.Schedule.createRoute(serverId)),
        ActionItem("社区分享", Icons.Filled.Groups, Screen.Community.createRoute(serverId)),
        ActionItem("设置", Icons.Filled.Settings, Screen.Settings.createRoute(serverId)),
    )

    val rows = actions.chunked(4)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { action ->
                    Box(modifier = Modifier.weight(1f)) {
                        Card(
                            onClick = { navController.navigate(action.route) },
                            colors = CardDefaults.cardColors(
                                containerColor = ZalithCard.copy(alpha = 0.5f)
                            ),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    ZalithCardBorder.copy(alpha = 0.3f)
                                )
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(action.icon, null,
                                    modifier = Modifier.size(22.dp),
                                    tint = ZalithPrimary)
                                Spacer(Modifier.height(4.dp))
                                Text(action.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ServerInfoSection(server: ServerInstance) {
    SectionHeader("服务器信息")
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        InfoRow("名称", server.name)
        InfoRow("核心", "${server.coreType.displayName} ${server.coreVersion}")
        InfoRow("端口", server.port.toString())
        InfoRow("内存", "${server.memoryMin}MB - ${server.memoryMax}MB")
        InfoRow("最大玩家", server.maxPlayers.toString())
        InfoRow("JAR文件", server.jarFileName)
        InfoRow("自定义核心", if (server.isCustomJar) "是" else "否")
        InfoRow("Geyser支持", if (server.geyserEnabled) "已启用" else "未启用")
        InfoRow("内网穿透", if (server.tunnelEnabled) "已启用" else "未启用")
        InfoRow("自动备份", if (server.autoBackup) "每${server.backupIntervalHours}小时" else "关闭")
    }
}

@Composable
fun CompatibilitySection(server: ServerInstance) {
    val compatibility = getCompatibilityInfo(server.coreVersion, server.coreType)
    SectionHeader("版本兼容性")

    val compColor = when (compatibility.level) {
        CompatibilityLevel.FULL -> ServerOnline
        CompatibilityLevel.PARTIAL -> ServerStarting
        CompatibilityLevel.INCOMPATIBLE -> ServerError
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = compColor.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when (compatibility.level) {
                    CompatibilityLevel.FULL -> Icons.Filled.CheckCircle
                    CompatibilityLevel.PARTIAL -> Icons.Filled.Warning
                    CompatibilityLevel.INCOMPATIBLE -> Icons.Filled.Error
                },
                null,
                tint = compColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(compatibility.title, fontWeight = FontWeight.Bold, color = compColor)
                Text(compatibility.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary)
            }
        }
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text("兼容性详情", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        val mcInfo = when {
            server.coreType.isBedrock() -> listOf(
                "客户端类型" to "基岩版 (Bedrock)",
                "推荐启动器" to "基岩版客户端 / 手机 / 主机",
                "端口" to "${server.port} (UDP)",
            )
            else -> listOf(
                "客户端类型" to "Java版",
                "推荐版本" to "${server.coreVersion}",
                "协议版本" to "兼容主流客户端",
                "跨平台支持" to if (server.geyserEnabled) "已启用Geyser，支持基岩版" else "仅Java版",
            )
        }
        mcInfo.forEach { (label, value) ->
            InfoRow(label, value)
        }
    }
}

data class ActionItem(val label: String, val icon: ImageVector, val route: String)
