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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mckaifu.app.data.model.TunnelInfo
import com.mckaifu.app.data.model.TunnelRegion
import com.mckaifu.app.data.model.TunnelType
import com.mckaifu.app.service.TunnelBinaryManager
import com.mckaifu.app.ui.component.*
import com.mckaifu.app.ui.theme.*
import com.mckaifu.app.util.AppPrefs
import com.mckaifu.app.viewmodel.MainViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunnelScreen(serverId: String, navController: NavController, vm: MainViewModel = viewModel()) {
    val servers by vm.servers.collectAsState()
    val server = servers.find { it.id == serverId }
    val context = LocalContext.current
    var tunnelInfo by remember {
        mutableStateOf(
            AppPrefs.loadTunnelInfo(context, serverId)?.copy(serverId = serverId)
                ?: TunnelInfo(serverId = serverId)
        )
    }
    var configName by remember {
        mutableStateOf(
            tunnelInfo.configPath.takeIf { it.isNotBlank() }?.let { File(it).name }
        )
    }
    var showRegionDropdown by remember { mutableStateOf(false) }
    var binaryPath by remember { mutableStateOf("") }
    var tunnelActive by remember { mutableStateOf(false) }
    var tunnelAddress by remember { mutableStateOf<String?>(null) }
    var tunnelError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tunnelInfo) {
        AppPrefs.saveTunnelInfo(context, serverId, tunnelInfo)
    }

    val configDir = remember { File(context.filesDir, "tunnel_configs") }
    val supportsBundled = tunnelInfo.type != TunnelType.NATAPP
    val bundledReady = supportsBundled && TunnelBinaryManager.isBundled(context, tunnelInfo.type)

    val configPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val displayName = context.contentResolver.query(
                uri, null, null, null, null
            )?.use { c ->
                val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
            } ?: "tunnel-config.conf"
            configDir.mkdirs()
            val target = File(configDir, displayName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { input.copyTo(it) }
            }
            tunnelInfo = tunnelInfo.copy(configPath = target.absolutePath)
            configName = target.name
            tunnelError = null
        } catch (e: Exception) {
            android.util.Log.e("mckaifu-tunnel", "导入配置文件失败", e)
            tunnelError = "导入配置文件失败: ${e.message}"
        }
    }

    val tunnelStatus by vm.tunnelService.status.collectAsState()

    LaunchedEffect(tunnelStatus) {
        tunnelActive = tunnelStatus.isActive
        if (tunnelStatus.publicAddress.isNotBlank()) {
            tunnelAddress = tunnelStatus.publicAddress
        }
        if (tunnelStatus.error.isNotBlank()) {
            tunnelError = tunnelStatus.error
        }
    }

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
                        if (tunnelError != null) {
                            Text(tunnelError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = ServerError)
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

            // Binary path
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("客户端程序路径", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = binaryPath,
                    onValueChange = { binaryPath = it },
                    placeholder = {
                        Text(
                            when (tunnelInfo.type) {
                                TunnelType.PLAYIT -> "如 /sdcard/playit/playit"
                                TunnelType.NGROK -> "如 /sdcard/ngrok/ngrok"
                                TunnelType.NATAPP -> "如 /sdcard/natapp/natapp"
                                TunnelType.SAKURA -> "留空使用内置 frpc"
                                TunnelType.CUSTOM -> "留空使用内置 frpc"
                            },
                            color = TextSecondary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = outFieldColors()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    when {
                        bundledReady -> when (tunnelInfo.type) {
                            TunnelType.PLAYIT -> "已内置 playit 客户端,留空将自动使用;也可填写自定义路径"
                            TunnelType.NGROK -> "已内置 ngrok 客户端,留空将自动使用;也可填写自定义路径"
                            else -> "已内置 frpc ${TunnelBinaryManager.FRPC_VERSION},留空将自动使用(需导入配置文件);也可填写自定义路径"
                        }
                        else -> "需自行下载对应平台的客户端可执行文件"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
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

            // Config file import
            SectionHeader("配置文件(可选)")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("导入已有配置文件(ngrok.yml / natapp.ini / frpc.toml 等)",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                if (configName != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Description, null, tint = ServerOnline, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(configName!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            tunnelInfo = tunnelInfo.copy(configPath = "")
                            configName = null
                        }) { Text("移除") }
                    }
                } else {
                    Text("未导入。使用配置文件时无需填写认证令牌,连接参数以配置文件为准。",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { configPicker.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.FileOpen, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("选择并导入配置文件")
                    }
                }
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
                    if (tunnelActive) {
                        vm.tunnelService.stopTunnel()
                        tunnelAddress = null
                        tunnelError = null
                        server?.let { vm.updateServer(it.copy(tunnelEnabled = false)) }
                    } else {
                        tunnelError = null
                        val exe = when {
                            binaryPath.isNotBlank() -> File(binaryPath).takeIf { it.exists() }
                            supportsBundled -> TunnelBinaryManager.ensureBinary(context, tunnelInfo.type)
                            else -> null
                        }
                        if (exe == null) {
                            tunnelError = if (binaryPath.isNotBlank()) {
                                "未找到客户端程序: $binaryPath"
                            } else if (supportsBundled) {
                                if ((tunnelInfo.type == TunnelType.CUSTOM || tunnelInfo.type == TunnelType.SAKURA) && configName == null) {
                                    "内置 frpc 需要先导入配置文件"
                                } else {
                                    "内置二进制解压失败,请检查存储空间"
                                }
                            } else {
                                "请填写客户端程序路径"
                            }
                        } else {
                            vm.tunnelService.startTunnel(tunnelInfo, exe) { line ->
                                android.util.Log.d("mckaifu-tunnel", line)
                            }
                            server?.let { vm.updateServer(it.copy(tunnelEnabled = true)) }
                        }
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
