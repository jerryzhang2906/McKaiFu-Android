@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.mckaifu.app.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mckaifu.app.data.model.ServerInstance
import com.mckaifu.app.ui.component.*
import com.mckaifu.app.ui.theme.*
import com.mckaifu.app.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(serverId: String, navController: NavController, vm: MainViewModel = viewModel()) {
    val servers by vm.servers.collectAsState()
    val server = servers.find { it.id == serverId }
    val context = LocalContext.current
    var communityServers by remember { mutableStateOf(listOf<CommunityServer>()) }
    var loadFailed by remember { mutableStateOf(false) }

    val lanAddress = remember(server) {
        server?.let { getLanAddress(context, it.port) } ?: ""
    }

    val scope = rememberCoroutineScope()

    fun reloadCommunity() {
        scope.launch {
            communityServers = fetchCommunityServers()
            loadFailed = communityServers.isEmpty()
        }
    }

    LaunchedEffect(Unit) {
        reloadCommunity()
    }

    fun copyToClipboard(text: String, label: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("server", text))
        Toast.makeText(context, label, Toast.LENGTH_SHORT).show()
    }

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
                    IconButton(onClick = {
                        server?.let {
                            val text = buildString {
                                append("${it.name}\n")
                                append("${it.coreType.displayName} ${it.coreVersion}\n")
                                if (lanAddress.isNotBlank()) append("地址: $lanAddress\n")
                                if (it.tunnelEnabled) append("穿透: 通过内网穿透获取")
                            }
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(share, "分享服务器"))
                        }
                    }) {
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
                        if (lanAddress.isNotBlank()) {
                            InfoRow("局域网地址", lanAddress, valueColor = ZalithPrimary)
                        }
                        if (it.tunnelEnabled) {
                            InfoRow("穿透地址", "已开启，见内网穿透页", valueColor = ZalithPrimary)
                        }
                    }
                }
            }

            item {
                SectionHeader("分享方式")
            }

            item {
                ShareMethodCard(
                    icon = Icons.Filled.ContentCopy,
                    title = "复制连接信息",
                    description = "复制 服务器名 + 地址:端口 到剪贴板",
                    onClick = {
                        server?.let {
                            copyToClipboard(
                                "${it.name} - ${lanAddress.ifBlank { "${it.port}" }}",
                                "已复制连接信息"
                            )
                        }
                    }
                )
            }

            item {
                ShareMethodCard(
                    icon = Icons.Filled.IosShare,
                    title = "系统分享",
                    description = "通过微信、QQ 等应用发送服务器信息",
                    onClick = {
                        server?.let {
                            val text = "${it.name} ${it.coreType.displayName} ${it.coreVersion}\n" +
                                "地址: ${lanAddress.ifBlank { "端口 ${it.port}" }}"
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(share, "分享服务器"))
                        }
                    }
                )
            }

            item {
                ShareMethodCard(
                    icon = Icons.Filled.Wifi,
                    title = "查看局域网地址",
                    description = "同一 WiFi 下的玩家可直接连接",
                    onClick = {
                        if (lanAddress.isNotBlank()) {
                            copyToClipboard(lanAddress, "已复制局域网地址")
                        } else {
                            Toast.makeText(context, "未连接到 WiFi", Toast.LENGTH_SHORT).show()
                        }
                    }
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
                Text("发现其他玩家分享的Minecraft服务器(点击复制地址)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary)
                Spacer(Modifier.height(12.dp))
            }

            if (loadFailed) {
                item {
                    Text("社区列表加载失败(需联网)，点击重试",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.clickable { reloadCommunity() })
                }
            }

            items(communityServers) { cs ->
                CommunityServerCard(
                    cs = cs,
                    onClick = { copyToClipboard(cs.address, "已复制 ${cs.name} 地址") }
                )
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

private suspend fun fetchCommunityServers(): List<CommunityServer> = withContext(Dispatchers.IO) {
    try {
        val url = java.net.URL(
            "https://raw.githubusercontent.com/jerryzhang2906/McKaiFu-Android/master/community_servers.json"
        )
        val text = url.readText()
        val array = org.json.JSONArray(text)
        (0 until array.length()).mapNotNull { i ->
            val obj = array.getJSONObject(i)
            CommunityServer(
                name = obj.optString("name"),
                coreInfo = obj.optString("core"),
                address = obj.optString("address"),
                onlinePlayers = obj.optInt("online", 0),
                tag = obj.optString("tag")
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}

private fun getLanAddress(context: Context, port: Int): String {
    return try {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ip = wifiManager.connectionInfo?.ipAddress ?: return ""
        if (ip == 0) return ""
        val addr = "${ip and 0xFF}.${(ip shr 8) and 0xFF}.${(ip shr 16) and 0xFF}.${(ip shr 24) and 0xFF}"
        "$addr:$port"
    } catch (e: Exception) {
        ""
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
fun CommunityServerCard(cs: CommunityServer, onClick: () -> Unit = {}) {
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
                onClick = onClick,
                modifier = Modifier.height(32.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ZalithPrimary),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(ZalithPrimary.copy(alpha = 0.5f))
                )
            ) { Text("复制地址", style = MaterialTheme.typography.labelSmall) }
        }
    }
}
