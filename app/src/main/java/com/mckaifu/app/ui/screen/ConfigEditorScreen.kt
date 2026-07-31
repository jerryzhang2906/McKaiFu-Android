@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.mckaifu.app.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.foundation.*
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mckaifu.app.data.model.ServerConfig
import com.mckaifu.app.ui.component.*
import com.mckaifu.app.ui.theme.*
import com.mckaifu.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigEditorScreen(serverId: String, navController: NavController, vm: MainViewModel = viewModel()) {
    val servers by vm.servers.collectAsState()
    val server = servers.find { it.id == serverId }
    var selectedFile by remember { mutableStateOf<String?>(null) }
    var config by remember { mutableStateOf(ServerConfig()) }
    var editedValues by remember { mutableStateOf(mapOf<String, String>()) }
    var rawContent by remember { mutableStateOf("") }
    var showSaveSuccess by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }

    val configFiles = remember(serverId, refreshKey) { vm.listConfigFiles(serverId) }

    val selectedIsProperties = selectedFile?.endsWith(".properties") == true

    LaunchedEffect(selectedFile) {
        val path = selectedFile ?: return@LaunchedEffect
        val content = vm.readConfigFile(path) ?: return@LaunchedEffect
        if (selectedIsProperties) {
            val props = content.lines()
                .filter { it.contains("=") && !it.trim().startsWith("#") }
                .associate { line ->
                    val idx = line.indexOf("=")
                    line.substring(0, idx).trim() to line.substring(idx + 1).trim()
                }
            editedValues = props
        } else {
            rawContent = content
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("配置文件编辑器", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { showSaveSuccess = true }) {
                        Icon(Icons.Filled.Save, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("保存", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Text("服务器: ${server?.name ?: ""}",
                        style = MaterialTheme.typography.titleSmall, color = TextSecondary)
                    Spacer(Modifier.height(12.dp))
                }

                item {
                    SectionHeader("选择配置文件")
                    Spacer(Modifier.height(8.dp))
                    if (configFiles.isEmpty()) {
                        Text("服务器目录下暂无可编辑的配置文件\n(启动服务器生成配置后会显示)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary)
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            configFiles.forEach { file ->
                                FilterChip(
                                    selected = selectedFile == file.path,
                                    onClick = { selectedFile = file.path },
                                    label = { Text(file.name, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ZalithPrimary.copy(alpha = 0.2f),
                                        selectedLabelColor = ZalithPrimary
                                    )
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                if (selectedFile == null) {
                    item {
                        EmptyStateView(
                            icon = Icons.Filled.Tune,
                            title = "请选择配置文件",
                            subtitle = "server.properties、bukkit.yml 等"
                        )
                    }
                } else {
                    val currentFile = selectedFile!!
                    val currentIsProperties = currentFile.endsWith(".properties")
                    if (currentIsProperties) {
                        item {
                            SectionHeader("${currentFile.substringAfterLast('/')} 配置项")
                            Spacer(Modifier.height(8.dp))
                        }

                        items(editedValues.toList(), key = { it.first }) { (key, value) ->
                            PropertyEditorItem(
                                key = key,
                                value = editedValues[key] ?: value,
                                onValueChange = { newVal ->
                                    editedValues = editedValues + (key to newVal)
                                },
                                configFile = currentFile.substringAfterLast('/')
                            )
                        }

                        item {
                            Spacer(Modifier.height(24.dp))
                            GradientButton(
                                text = "保存配置",
                                icon = Icons.Filled.Save,
                                onClick = { showSaveSuccess = true },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(32.dp))
                        }
                    } else {
                        item {
                            SectionHeader("${currentFile.substringAfterLast('/')} (文本编辑)")
                            Spacer(Modifier.height(8.dp))
                        }
                        item {
                            OutlinedTextField(
                                value = rawContent,
                                onValueChange = { rawContent = it },
                                modifier = Modifier.fillMaxWidth().height(400.dp),
                                textStyle = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ZalithPrimary,
                                    unfocusedBorderColor = ZalithCardBorder,
                                    focusedContainerColor = ZalithSurfaceVariant.copy(alpha = 0.5f),
                                    unfocusedContainerColor = ZalithSurfaceVariant.copy(alpha = 0.3f)
                                )
                            )
                            Spacer(Modifier.height(24.dp))
                            GradientButton(
                                text = "保存配置",
                                icon = Icons.Filled.Save,
                                onClick = { showSaveSuccess = true },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }

    if (showSaveSuccess) {
        AlertDialog(
            onDismissRequest = { showSaveSuccess = false },
            containerColor = ZalithSurface,
            icon = { Icon(Icons.Filled.CheckCircle, null, tint = ServerOnline, modifier = Modifier.size(32.dp)) },
            title = { Text("配置已保存") },
            text = {
                val path = selectedFile ?: ""
                val saved = if (selectedIsProperties) {
                    val sb = StringBuilder()
                    editedValues.forEach { (k, v) -> sb.append("$k=$v\n") }
                    vm.writeConfigFile(path, sb.toString())
                } else {
                    vm.writeConfigFile(path, rawContent)
                }
                if (saved) {
                    Text("${selectedFile?.substringAfterLast('/') ?: ""} 已保存，重启服务器后生效。", color = TextSecondary)
                } else {
                    Text("保存失败，请重试。", color = ServerError)
                }
            },
            confirmButton = {
                Button(onClick = { showSaveSuccess = false; navController.popBackStack() }) { Text("完成") }
            }
        )
    }
}

@Composable
fun PropertyEditorItem(
    key: String,
    value: String,
    onValueChange: (String) -> Unit,
    configFile: String
) {
    val isBoolean = value.lowercase() in listOf("true", "false")
    val isNumber = value.toIntOrNull() != null

    GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(key, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text(getPropertyDescription(configFile, key),
            style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.height(8.dp))

        if (isBoolean) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (value == "true") "已启用" else "已禁用",
                    color = if (value == "true") ServerOnline else ServerOffline)
                Switch(
                    checked = value == "true",
                    onCheckedChange = { onValueChange(if (it) "true" else "false") },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = ZalithPrimary,
                        checkedThumbColor = Color.White
                    )
                )
            }
        } else if (isNumber && key.contains("port", ignoreCase = true)) {
            OutlinedTextField(
                value = value,
                onValueChange = { onValueChange(it) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ZalithPrimary,
                    unfocusedBorderColor = ZalithCardBorder,
                    focusedContainerColor = ZalithSurfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = ZalithSurfaceVariant.copy(alpha = 0.3f)
                )
            )
        } else {
            OutlinedTextField(
                value = value,
                onValueChange = { onValueChange(it) },
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
    }
}

fun getPropertyDescription(file: String, key: String): String = when (key) {
    "server-port" -> "服务器监听端口 (默认: 25565)"
    "max-players" -> "最大同时在线玩家数"
    "online-mode" -> "是否启用正版验证"
    "difficulty" -> "游戏难度: peaceful/easy/normal/hard"
    "gamemode" -> "默认游戏模式: survival/creative/adventure/spectator"
    "pvp" -> "是否允许玩家互相攻击"
    "view-distance" -> "服务端发送给客户端的区块距离"
    "simulation-distance" -> "服务端模拟的区块距离"
    "motd" -> "服务器列表显示的描述信息"
    "enable-command-block" -> "是否启用命令方块"
    "white-list" -> "是否启用白名单"
    "spawn-protection" -> "出生点保护半径 (0=关闭)"
    "max-tick-time" -> "单个游戏刻最大执行时间(ms)"
    "max-world-size" -> "世界最大半径 (方块)"
    "network-compression-threshold" -> "网络压缩阈值"
    "enforce-whitelist" -> "是否强制白名单"
    "enforce-secure-profile" -> "是否强制安全配置文件"
    "allow-flight" -> "是否允许飞行"
    else -> "配置文件参数"
}
