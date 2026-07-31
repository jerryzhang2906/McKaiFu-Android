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
import com.mckaifu.app.data.model.BanEntry
import com.mckaifu.app.data.model.BanType
import com.mckaifu.app.ui.component.*
import com.mckaifu.app.ui.theme.*
import com.mckaifu.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BanListScreen(serverId: String, navController: NavController, vm: MainViewModel = viewModel()) {
    val servers by vm.servers.collectAsState()
    val server = servers.find { it.id == serverId }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showUnbanDialog by remember { mutableStateOf(false) }
    var selectedBan by remember { mutableStateOf<BanEntry?>(null) }

    val playerBans = listOf(
        BanEntry(name = "BadPlayer123", reason = "违规行为", source = "管理员", type = BanType.PLAYER),
        BanEntry(name = "Griefer456", reason = "破坏建筑", source = "自动检测", type = BanType.PLAYER),
    )
    val ipBans = listOf(
        BanEntry(ip = "192.168.1.100", reason = "恶意攻击", source = "系统", type = BanType.IP),
        BanEntry(ip = "10.0.0.50", reason = "违规玩家关联IP", source = "系统", type = BanType.IP),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("封禁列表", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Add ban */ }) {
                        Icon(Icons.Filled.PersonOff, "添加封禁")
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
            TabRow(selectedTabIndex = selectedTab, containerColor = ZalithSurface, contentColor = ZalithPrimary) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    text = { Text("玩家封禁 (${playerBans.size})") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    text = { Text("IP封禁 (${ipBans.size})") })
            }

            when (selectedTab) {
                0 -> BanList(
                    bans = playerBans,
                    onUnban = { ban ->
                        selectedBan = ban
                        showUnbanDialog = true
                    },
                    emptyMessage = "暂无玩家封禁记录"
                )
                1 -> BanList(
                    bans = ipBans,
                    onUnban = { ban ->
                        selectedBan = ban
                        showUnbanDialog = true
                    },
                    emptyMessage = "暂无IP封禁记录"
                )
            }
        }
    }

    if (showUnbanDialog && selectedBan != null) {
        AlertDialog(
            onDismissRequest = { showUnbanDialog = false },
            containerColor = ZalithSurface,
            icon = { Icon(Icons.Filled.Unpublished, null, tint = ServerStarting, modifier = Modifier.size(32.dp)) },
            title = { Text("解除封禁", fontWeight = FontWeight.Bold) },
            text = {
                Text("确定要解除对 ${selectedBan!!.name.ifBlank { selectedBan!!.ip }} 的封禁吗？",
                    color = TextSecondary)
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cmd = if (selectedBan!!.type == BanType.PLAYER)
                            "pardon ${selectedBan!!.name}"
                        else "pardon-ip ${selectedBan!!.ip}"
                        vm.sendCommand(serverId, cmd)
                        showUnbanDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ServerStarting)
                ) { Text("解除") }
            },
            dismissButton = {
                TextButton(onClick = { showUnbanDialog = false }) { Text("取消", color = TextSecondary) }
            }
        )
    }
}

@Composable
fun BanList(
    bans: List<BanEntry>,
    onUnban: (BanEntry) -> Unit,
    emptyMessage: String
) {
    if (bans.isEmpty()) {
        EmptyStateView(
            icon = Icons.Filled.Block,
            title = emptyMessage
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(bans, key = { it.name + it.ip }) { ban ->
                BanCard(ban = ban, onUnban = { onUnban(ban) })
            }
        }
    }
}

@Composable
fun BanCard(ban: BanEntry, onUnban: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(ServerError.copy(alpha = 0.2f), MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (ban.type == BanType.PLAYER) Icons.Filled.PersonOff else Icons.Filled.Lan,
                        null,
                        tint = ServerError,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        ban.name.ifBlank { ban.ip },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (ban.reason.isNotBlank()) {
                        Text("原因: ${ban.reason}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary)
                    }
                    Text("封禁者: ${ban.source}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary)
                }
            }
            TextButton(onClick = onUnban) {
                Icon(Icons.Filled.Unpublished, null, modifier = Modifier.size(16.dp),
                    tint = ServerStarting)
                Spacer(Modifier.width(4.dp))
                Text("解封", color = ServerStarting,
                    style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
