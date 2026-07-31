@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.mckaifu.app.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mckaifu.app.data.model.ScheduleConfig
import com.mckaifu.app.data.model.ScheduledTask
import com.mckaifu.app.data.model.TaskType
import com.mckaifu.app.ui.component.*
import com.mckaifu.app.ui.theme.*
import com.mckaifu.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(serverId: String, navController: NavController, vm: MainViewModel = viewModel()) {
    val servers by vm.servers.collectAsState()
    val server = servers.find { it.id == serverId }
    var memoryMin by remember { mutableStateOf(server?.memoryMin?.toString() ?: "512") }
    var memoryMax by remember { mutableStateOf(server?.memoryMax?.toString() ?: "2048") }
    var maxPlayers by remember { mutableStateOf(server?.maxPlayers?.toString() ?: "20") }
    var viewDistance by remember { mutableStateOf("10") }
    var simulationDistance by remember { mutableStateOf("10") }
    var difficulty by remember { mutableStateOf("easy") }
    var pvp by remember { mutableStateOf(true) }
    var commandBlocks by remember { mutableStateOf(false) }
    var whitelist by remember { mutableStateOf(false) }
    var onlineMode by remember { mutableStateOf(true) }
    var javaArgs by remember { mutableStateOf(server?.javaArgs ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("服务器设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        server?.let { s ->
                            vm.updateServer(s.copy(
                                memoryMin = memoryMin.toIntOrNull() ?: s.memoryMin,
                                memoryMax = memoryMax.toIntOrNull() ?: s.memoryMax,
                                maxPlayers = maxPlayers.toIntOrNull() ?: s.maxPlayers,
                                javaArgs = javaArgs
                            ))
                        }
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Filled.Save, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("保存", fontWeight = FontWeight.Bold, color = ZalithPrimary)
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
            Text("${server?.name ?: "服务器"} 设置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)

            SectionHeader("内存与性能")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("最小内存 (MB)", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                        OutlinedTextField(
                            value = memoryMin,
                            onValueChange = { memoryMin = it.filter { c -> c.isDigit() }.take(5) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = outFieldColors()
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("最大内存 (MB)", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                        OutlinedTextField(
                            value = memoryMax,
                            onValueChange = { memoryMax = it.filter { c -> c.isDigit() }.take(5) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = outFieldColors()
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("JVM 参数", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                OutlinedTextField(
                    value = javaArgs,
                    onValueChange = { javaArgs = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    colors = outFieldColors()
                )
            }

            SectionHeader("游戏设置")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("最大玩家", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                        OutlinedTextField(
                            value = maxPlayers,
                            onValueChange = { maxPlayers = it.filter { c -> c.isDigit() }.take(3) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = outFieldColors()
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("视距", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                        OutlinedTextField(
                            value = viewDistance,
                            onValueChange = { viewDistance = it.filter { c -> c.isDigit() }.take(2) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = outFieldColors()
                        )
                    }
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                SettingSwitch("PVP", "允许玩家互相攻击", pvp) { pvp = it }
                SettingSwitch("命令方块", "启用命令方块功能", commandBlocks) { commandBlocks = it }
                SettingSwitch("白名单", "仅允许白名单玩家加入", whitelist) { whitelist = it }
                SettingSwitch("正版验证", "仅允许正版玩家加入", onlineMode) { onlineMode = it }
            }

            SectionHeader("性能优化建议")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                SuggestionItem(Icons.Filled.Speed, "使用 G1GC 垃圾回收器", "建议添加 -XX:+UseG1GC 到JVM参数", "高")
                SuggestionItem(Icons.Filled.Memory, "Aikar's Flags", "推荐使用 Aikar 的 JVM 优化参数", "中")
                SuggestionItem(Icons.Filled.Tune, "调整视距", "将视距调至 6-8 可改善性能", "中")
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = ZalithPrimary,
                checkedThumbColor = Color.White
            )
        )
    }
}

@Composable
fun SuggestionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String, impact: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = ZalithPrimary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Surface(
            color = when (impact) {
                "高" -> ServerError.copy(alpha = 0.2f)
                "中" -> ServerStarting.copy(alpha = 0.2f)
                else -> ServerOnline.copy(alpha = 0.2f)
            },
            shape = MaterialTheme.shapes.extraSmall
        ) {
            Text(" $impact ", style = MaterialTheme.typography.labelSmall,
                color = when (impact) {
                    "高" -> ServerError
                    "中" -> ServerStarting
                    else -> ServerOnline
                })
        }
    }
}
