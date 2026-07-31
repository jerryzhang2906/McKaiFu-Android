@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.mckaifu.app.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.mckaifu.app.data.model.TunnelInfo
import com.mckaifu.app.data.model.TunnelRegion
import com.mckaifu.app.data.model.TunnelType
import com.mckaifu.app.ui.component.*
import com.mckaifu.app.ui.theme.*
import com.mckaifu.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunnelScreen(serverId: String, navController: NavController, vm: MainViewModel = viewModel()) {
    val servers by vm.servers.collectAsState()
    val server = servers.find { it.id == serverId }
    var tunnelInfo by remember { mutableStateOf(TunnelInfo(serverId = serverId)) }
    var showRegionDropdown by remember { mutableStateOf(false) }
    var tunnelActive by remember { mutableStateOf(false) }
    var tunnelAddress by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("内网穿透", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "返回")
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("${server?.name ?: ""} 内网穿透",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)

            // Status card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (tunnelActive) ServerOnline.copy(alpha = 0.3f)
                else ServerOffline.copy(alpha = 0.3f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(isOnline = tunnelActive, size = 16.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (tunnelActive) "穿透已连接" else "穿透未连接",
                            fontWeight = FontWeight.Bold,
                            color = if (tunnelActive) ServerOnline else TextSecondary
                        )
                        if (tunnelAddress != null) {
                            Text(tunnelAddress!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ZalithPrimary,
                                fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Tunnel type
            SectionHeader("穿透服务")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("选择服务", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TunnelType.entries.forEach { type ->
                        FilterChip(
                            selected = tunnelInfo.type == type,
                            onClick = { tunnelInfo = tunnelInfo.copy(type = type) },
                            label = { Text(type.displayName, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Icon(
                                    when (type) {
                                        TunnelType.PLAYIT -> Icons.Filled.Sensors
                                        TunnelType.NGROK -> Icons.Filled.Cloud
                                        TunnelType.NATAPP, TunnelType.SAKURA -> Icons.Filled.NetworkCheck
                                        TunnelType.CUSTOM -> Icons.Filled.Dns
                                    }, null, modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ZalithPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = ZalithPrimary
                            )
                        )
                    }
                }
            }

            // Region selector
            SectionHeader("区域选择")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("选择服务器节点区域，优化玩家连接速度",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = showRegionDropdown,
                    onExpandedChange = { showRegionDropdown = it }
                ) {
                    OutlinedTextField(
                        value = tunnelInfo.region?.displayName ?: TunnelRegion.AUTO.displayName,
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = { Icon(Icons.Filled.Public, null, tint = ZalithPrimary) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showRegionDropdown) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = outFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = showRegionDropdown,
                        onDismissRequest = { showRegionDropdown = false }
                    ) {
                        TunnelRegion.entries.forEach { region ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            when (region) {
                                                TunnelRegion.AUTO -> Icons.Filled.Autorenew
                                                TunnelRegion.CN_NORTH, TunnelRegion.CN_EAST,
                                                TunnelRegion.CN_SOUTH, TunnelRegion.CN_WEST -> Icons.Filled.Flag
                                                TunnelRegion.US_EAST, TunnelRegion.US_WEST -> Icons.Filled.LocationOn
                                                TunnelRegion.EU_WEST, TunnelRegion.EU_CENTRAL -> Icons.Filled.LocationOn
                                                TunnelRegion.ASIA_EAST, TunnelRegion.ASIA_SE -> Icons.Filled.Public
                                                TunnelRegion.OCEANIA -> Icons.Filled.Public
                                            }, null,
                                            tint = when (region) {
                                                TunnelRegion.AUTO -> ZalithSecondary
                                                TunnelRegion.CN_NORTH, TunnelRegion.CN_EAST,
                                                TunnelRegion.CN_SOUTH, TunnelRegion.CN_WEST -> ZalithPrimary
                                                else -> TextSecondary
                                            },
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(region.displayName, fontWeight = FontWeight.Medium)
                                            Text(region.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary)
                                        }
                                    }
                                },
                                onClick = {
                                    tunnelInfo = tunnelInfo.copy(region = region)
                                    showRegionDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // Auth token
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("认证令牌", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = tunnelInfo.authToken,
                    onValueChange = { tunnelInfo = tunnelInfo.copy(authToken = it) },
                    placeholder = { Text("输入 auth token...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = outFieldColors()
                )
            }

            // Local port
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("本地端口", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = tunnelInfo.localPort?.toString() ?: (server?.port?.toString() ?: "25565"),
                    onValueChange = { tunnelInfo = tunnelInfo.copy(localPort = it.toIntOrNull() ?: 25565) },
                    placeholder = { Text(server?.port?.toString() ?: "25565") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = outFieldColors()
                )
            }

            // Public address display
            if (tunnelActive && tunnelAddress != null) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = ServerOnline.copy(alpha = 0.3f)
                ) {
                    Text("公网地址", fontWeight = FontWeight.Bold, color = ServerOnline)
                    Spacer(Modifier.height(4.dp))
                    Text(tunnelAddress!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Text("将此地址分享给朋友即可加入游戏",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary)
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    tunnelActive = !tunnelActive
                    if (tunnelActive) {
                        tunnelAddress = when (tunnelInfo.type) {
                            TunnelType.PLAYIT -> "playit.gg:${tunnelInfo.localPort ?: server?.port ?: 25565}"
                            TunnelType.NGROK -> "${tunnelInfo.region?.name?.lowercase() ?: "us"}.ngrok.io:${tunnelInfo.localPort ?: server?.port ?: 25565}"
                            TunnelType.NATAPP -> "mckaifu.natappfree.cc:${tunnelInfo.localPort ?: server?.port ?: 25565}"
                            TunnelType.SAKURA -> when (tunnelInfo.region) {
                                TunnelRegion.CN_NORTH -> "bj.sakurafrp.com"
                                TunnelRegion.CN_EAST -> "sh.sakurafrp.com"
                                TunnelRegion.CN_SOUTH -> "gz.sakurafrp.com"
                                TunnelRegion.CN_WEST -> "cd.sakurafrp.com"
                                else -> "cn.sakurafrp.com"
                            } + ":${tunnelInfo.localPort ?: server?.port ?: 25565}"
                            TunnelType.CUSTOM -> "自定义地址"
                        }
                        server?.let { vm.updateServer(it.copy(tunnelEnabled = true)) }
                    } else {
                        tunnelAddress = null
                        server?.let { vm.updateServer(it.copy(tunnelEnabled = false)) }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (tunnelActive) ServerError else ZalithPrimary
                )
            ) {
                Icon(
                    if (tunnelActive) Icons.Filled.PowerSettingsNew else Icons.Filled.Wifi,
                    null, modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (tunnelActive) "断开穿透" else "启动穿透",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
