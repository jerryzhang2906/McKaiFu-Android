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
import com.mckaifu.app.data.model.GameMode
import com.mckaifu.app.data.model.PlayerInfo
import com.mckaifu.app.ui.component.*
import com.mckaifu.app.ui.navigation.Screen
import com.mckaifu.app.ui.theme.*
import com.mckaifu.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerManagementScreen(serverId: String, navController: NavController, vm: MainViewModel = viewModel()) {
    val servers by vm.servers.collectAsState()
    val server = servers.find { it.id == serverId }
    val players by vm.getPlayers(serverId).collectAsState()
    var showKickDialog by remember { mutableStateOf(false) }
    var showBanDialog by remember { mutableStateOf(false) }
    var selectedPlayer by remember { mutableStateOf<PlayerInfo?>(null) }
    var chatMessage by remember { mutableStateOf("") }
    var showChatWindow by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("玩家管理", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate(Screen.Chat.createRoute(serverId))
                    }) {
                        Icon(Icons.Filled.Chat, "聊天窗口")
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
            if (players.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Filled.PeopleOutline,
                    title = "暂无在线玩家",
                    subtitle = "启动服务器后玩家加入将显示在此"
                )
            } else {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = ZalithSurface,
                    contentColor = ZalithPrimary
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                        text = { Text("在线玩家 (${players.size})") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                        text = { Text("封禁列表") })
                }

                when (selectedTab) {
                    0 -> OnlinePlayersTab(players, onKick = {
                        selectedPlayer = it; showKickDialog = true
                    }, onBan = {
                        selectedPlayer = it; showBanDialog = true
                    }, onOp = { vm.sendCommand(serverId, "op ${it.name}") },
                        onWhitelist = { vm.sendCommand(serverId, "whitelist add ${it.name}") },
                        onChat = { selectedPlayer = it; showChatWindow = true }
                    )
                    1 -> BanListTab(serverId, navController)
                }
            }
        }
    }

    if (showKickDialog && selectedPlayer != null) {
        KickDialog(player = selectedPlayer!!, onConfirm = { reason ->
            vm.sendCommand(serverId, "kick ${selectedPlayer!!.name} $reason")
            showKickDialog = false
        }, onDismiss = { showKickDialog = false })
    }

    if (showBanDialog && selectedPlayer != null) {
        BanDialog(player = selectedPlayer!!, onConfirm = { reason ->
            vm.sendCommand(serverId, "ban ${selectedPlayer!!.name} $reason")
            showBanDialog = false
        }, onDismiss = { showBanDialog = false })
    }

    if (showChatWindow && selectedPlayer != null) {
        AlertDialog(
            onDismissRequest = { showChatWindow = false },
            containerColor = ZalithSurface,
            title = { Text("与 ${selectedPlayer!!.name} 聊天", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("使用tell命令发送私信", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = chatMessage,
                        onValueChange = { chatMessage = it },
                        placeholder = { Text("输入消息...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ZalithPrimary,
                            unfocusedBorderColor = ZalithCardBorder,
                            focusedContainerColor = ZalithSurfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = ZalithSurfaceVariant.copy(alpha = 0.3f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.sendCommand(serverId, "tell ${selectedPlayer!!.name} $chatMessage")
                        chatMessage = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ZalithPrimary)
                ) { Text("发送") }
            },
            dismissButton = {
                TextButton(onClick = { showChatWindow = false }) { Text("关闭", color = TextSecondary) }
            }
        )
    }
}

@Composable
fun OnlinePlayersTab(
    players: List<PlayerInfo>,
    onKick: (PlayerInfo) -> Unit,
    onBan: (PlayerInfo) -> Unit,
    onOp: (PlayerInfo) -> Unit,
    onWhitelist: (PlayerInfo) -> Unit,
    onChat: (PlayerInfo) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(players) { player ->
            PlayerCard(
                player = player,
                onKick = { onKick(player) },
                onBan = { onBan(player) },
                onOp = { onOp(player) },
                onWhitelist = { onWhitelist(player) },
                onChat = { onChat(player) }
            )
        }
    }
}

@Composable
fun PlayerCard(
    player: PlayerInfo,
    onKick: () -> Unit,
    onBan: () -> Unit,
    onOp: () -> Unit,
    onWhitelist: () -> Unit,
    onChat: () -> Unit
) {
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
                        .background(ZalithPrimary.copy(alpha = 0.2f), MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, null, Modifier.size(24.dp),
                        tint = ZalithPrimary)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(player.name, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall)
                    Text("Ping: ${player.ping}ms | ${player.world}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Health bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Favorite, null,
                tint = when {
                    player.health > 15 -> ServerOnline
                    player.health > 8 -> ServerStarting
                    else -> ServerError
                }, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            LinearProgressIndicator(
                progress = (player.health / player.maxHealth).toFloat(),
                modifier = Modifier.weight(1f).height(6.dp)
                    .background(ZalithCardBorder, MaterialTheme.shapes.extraSmall),
                color = when {
                    player.health > 15 -> ServerOnline
                    player.health > 8 -> ServerStarting
                    else -> ServerError
                },
                trackColor = Color.Transparent
            )
            Spacer(Modifier.width(8.dp))
            Text("${player.health.toInt()}/${player.maxHealth.toInt()}",
                style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }

        Spacer(Modifier.height(4.dp))

        // Hunger bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Restaurant, null,
                tint = when {
                    player.hunger > 15 -> ServerOnline
                    player.hunger > 8 -> ServerStarting
                    else -> ServerError
                }, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            LinearProgressIndicator(
                progress = player.hunger / 20f,
                modifier = Modifier.weight(1f).height(6.dp)
                    .background(ZalithCardBorder, MaterialTheme.shapes.extraSmall),
                color = when {
                    player.hunger > 15 -> ServerOnline
                    player.hunger > 8 -> ServerStarting
                    else -> ServerError
                },
                trackColor = Color.Transparent
            )
            Spacer(Modifier.width(8.dp))
            Text("${player.hunger}/20",
                style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }

        Spacer(Modifier.height(4.dp))

        // XP bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AutoAwesome, null,
                tint = ZalithSecondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            LinearProgressIndicator(
                progress = (player.xp % 1).toFloat(),
                modifier = Modifier.weight(1f).height(6.dp)
                    .background(ZalithCardBorder, MaterialTheme.shapes.extraSmall),
                color = ZalithSecondary,
                trackColor = Color.Transparent
            )
            Spacer(Modifier.width(8.dp))
            Text("Lv ${player.xp.toInt()}",
                style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PlayerActionButton(Icons.Filled.RemoveCircle, "踢出", onKick, ServerStarting)
            PlayerActionButton(Icons.Filled.Block, "封禁", onBan, ServerError)
            PlayerActionButton(Icons.Filled.AdminPanelSettings, "OP", onOp, ZalithPrimary)
            PlayerActionButton(Icons.Filled.PersonAdd, "白名单", onWhitelist, ServerOnline)
            PlayerActionButton(Icons.Filled.Chat, "私信", onChat, ZalithSecondary)
        }
    }
}

@Composable
fun PlayerActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    color: Color
) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, text, tint = color, modifier = Modifier.size(18.dp))
            Text(text, fontSize = MaterialTheme.typography.labelSmall.fontSize,
                color = color)
        }
    }
}

@Composable
fun KickDialog(player: PlayerInfo, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ZalithSurface,
        title = { Text("踢出 ${player.name}", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("原因 (可选)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("请遵守服务器规则") },
                colors = outFieldColors()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(reason) },
                colors = ButtonDefaults.buttonColors(containerColor = ServerStarting)) {
                Text("踢出")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) } }
    )
}

@Composable
fun BanDialog(player: PlayerInfo, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var reason by remember { mutableStateOf("违规行为") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ZalithSurface,
        title = { Text("封禁 ${player.name}", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("封禁原因") },
                modifier = Modifier.fillMaxWidth(),
                colors = outFieldColors()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(reason) },
                colors = ButtonDefaults.buttonColors(containerColor = ServerError)) {
                Text("封禁")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) } }
    )
}

@Composable
fun BanListTab(serverId: String, navController: NavController) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Block, null, Modifier.size(48.dp),
                tint = TextSecondary.copy(alpha = 0.5f))
            Spacer(Modifier.height(8.dp))
            Text("封禁列表", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { navController.navigate(Screen.BanList.createRoute(serverId)) },
                colors = ButtonDefaults.buttonColors(containerColor = ZalithPrimary)
            ) { Text("管理封禁") }
        }
    }
}
