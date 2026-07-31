@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.mckaifu.app.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mckaifu.app.data.model.ServerInstance
import com.mckaifu.app.ui.component.*
import com.mckaifu.app.ui.theme.*
import com.mckaifu.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(serverId: String, navController: NavController, vm: MainViewModel = viewModel()) {
    val servers by vm.servers.collectAsState()
    val server = servers.find { it.id == serverId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("服务器分享", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Share server */ }) {
                        Icon(Icons.Filled.Share, "分享")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("分享你的服务器",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("让你的朋友轻松加入你的Minecraft世界",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary)
            }

            item {
                SectionHeader("服务器信息")
                Spacer(Modifier.height(8.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    server?.let {
                        InfoRow("服务器名称", it.name)
                        InfoRow("核心类型", "${it.coreType.displayName} ${it.coreVersion}")
                        InfoRow("端口", it.port.toString())
                        if (it.tunnelEnabled) {
                            InfoRow("穿透地址", "通过内网穿透获取", valueColor = ZalithPrimary)
                        }
                    }
                }
            }

            item {
                SectionHeader("分享方式")
            }

            item {
                ShareMethodCard(
                    icon = Icons.Filled.QrCodeScanner,
                    title = "二维码分享",
                    description = "生成二维码，对方扫码即可看到服务器信息",
                    onClick = { /* Generate QR */ }
                )
            }

            item {
                ShareMethodCard(
                    icon = Icons.Filled.ContentCopy,
                    title = "复制连接信息",
                    description = "复制 IP:端口 等信息到剪贴板",
                    onClick = { /* Copy to clipboard */ }
                )
            }

            item {
                ShareMethodCard(
                    icon = Icons.Filled.LocalOffer,
                    title = "邀请码",
                    description = "生成一次性邀请码，有效期内可使用",
                    onClick = { /* Generate invite code */ }
                )
            }

            item {
                ShareMethodCard(
                    icon = Icons.Filled.Groups,
                    title = "社区发现",
                    description = "将服务器发布到社区，让更多玩家发现",
                    onClick = { /* Publish to community */ }
                )
            }

            item {
                Spacer(Modifier.height(16.dp))
                Divider(color = ZalithCardBorder)
                Spacer(Modifier.height(16.dp))
                Text("社区服务器",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("发现其他玩家分享的Minecraft服务器",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary)
                Spacer(Modifier.height(12.dp))
            }

            val communityServers = listOf(
                CommunityServer("生存服 #1", "Paper 1.20.4", "playing.xxx.com:25565", 45, "生存"),
                CommunityServer("小游戏服", "Purpur 1.20.1", "mc.xxx.com:25566", 12, "小游戏"),
                CommunityServer("模组生存", "Fabric 1.19.2", "mod.xxx.com:25565", 8, "模组"),
            )

            items(communityServers) { cs ->
                CommunityServerCard(cs)
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun ShareMethodCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ZalithCard.copy(alpha = 0.5f)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(ZalithCardBorder)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = ZalithPrimary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = TextSecondary)
        }
    }
}

data class CommunityServer(
    val name: String,
    val coreInfo: String,
    val address: String,
    val onlinePlayers: Int,
    val tag: String
)

@Composable
fun CommunityServerCard(cs: CommunityServer) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(cs.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(cs.coreInfo, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(cs.address, style = MaterialTheme.typography.bodyMedium,
                    color = ZalithPrimary, fontWeight = FontWeight.Medium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = ServerOnline.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(" ${cs.onlinePlayers}人在线 ",
                        style = MaterialTheme.typography.labelSmall,
                        color = ServerOnline)
                }
                Spacer(Modifier.height(4.dp))
                Surface(
                    color = ZalithPrimary.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(" ${cs.tag} ",
                        style = MaterialTheme.typography.labelSmall,
                        color = ZalithPrimary)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { },
                modifier = Modifier.height(32.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ZalithPrimary),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(ZalithPrimary.copy(alpha = 0.5f))
                )
            ) { Text("连接", style = MaterialTheme.typography.labelSmall) }
            OutlinedButton(
                onClick = { },
                modifier = Modifier.height(32.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
            ) { Text("收藏", style = MaterialTheme.typography.labelSmall) }
        }
    }
}
