@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.mckaifu.app.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mckaifu.app.data.model.ServerStatus
import com.mckaifu.app.ui.component.*
import com.mckaifu.app.ui.theme.*
import com.mckaifu.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(serverId: String, navController: NavController, vm: MainViewModel = viewModel()) {
    val servers by vm.servers.collectAsState()
    val server = servers.find { it.id == serverId }
    val statuses by vm.serverStatuses.collectAsState()
    val status = statuses[serverId] ?: server?.status ?: ServerStatus.OFFLINE
    val tps = if (status == ServerStatus.ONLINE) (18.5 + Math.random() * 1.5) else 0.0
    val memoryUsed = if (status == ServerStatus.ONLINE) (40 + (Math.random() * 30).toInt()) else 0
    val cpuUsed = if (status == ServerStatus.ONLINE) (20 + Math.random() * 40) else 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("性能仪表盘", fontWeight = FontWeight.Bold) },
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
            Text("${server?.name ?: "服务器"} 性能监控",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricTile("TPS", String.format("%.1f", tps), "20", Icons.Filled.Speed,
                    if (tps > 18) ServerOnline else if (tps > 10) ServerStarting else ServerError,
                    Modifier.weight(1f))
                MetricTile("内存", "$memoryUsed%", "${server?.memoryMax ?: 2048}MB", Icons.Filled.Memory,
                    if (memoryUsed < 70) ServerOnline else if (memoryUsed < 85) ServerStarting else ServerError,
                    Modifier.weight(1f))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricTile("CPU", "${cpuUsed.toInt()}%", "", Icons.Filled.Memory,
                    if (cpuUsed < 50) ServerOnline else if (cpuUsed < 80) ServerStarting else ServerError,
                    Modifier.weight(1f))
                MetricTile("玩家", "${server?.playerCount ?: 0}", "/${server?.maxPlayers ?: 20}", Icons.Filled.People,
                    ServerOnline, Modifier.weight(1f))
            }

            // TPS Gauge
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("TPS (每秒事务数)", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                        Text(
                            String.format("%.1f", tps),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                tps > 18 -> ServerOnline
                                tps > 10 -> ServerStarting
                                else -> ServerError
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Text("基准: 20.0 TPS",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center)
                    }
                }
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = (tps / 20.0).toFloat(),
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                        .clip(MaterialTheme.shapes.extraSmall),
                    color = when {
                        tps > 18 -> ServerOnline
                        tps > 10 -> ServerStarting
                        else -> ServerError
                    },
                    trackColor = ZalithCardBorder
                )
            }

            // Memory Usage
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("内存使用", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = memoryUsed / 100f,
                    modifier = Modifier.fillMaxWidth().height(12.dp)
                        .clip(MaterialTheme.shapes.extraSmall),
                    color = if (memoryUsed < 70) ServerOnline
                    else if (memoryUsed < 85) ServerStarting else ServerError,
                    trackColor = ZalithCardBorder
                )
                Spacer(Modifier.height(8.dp))
                Text("已用: ${server?.memoryMax?.times(memoryUsed)?.div(100) ?: 0}MB / ${server?.memoryMax ?: 2048}MB",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary)
            }

            // CPU Usage
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("CPU负载", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = (cpuUsed / 100.0).toFloat(),
                    modifier = Modifier.fillMaxWidth().height(12.dp)
                        .clip(MaterialTheme.shapes.extraSmall),
                    color = if (cpuUsed < 50) ServerOnline
                    else if (cpuUsed < 80) ServerStarting else ServerError,
                    trackColor = ZalithCardBorder
                )
                Spacer(Modifier.height(8.dp))
                Text("当前: ${cpuUsed.toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary)
            }

            // Server Info
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("服务器信息", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                InfoRow("状态", status.displayName)
                InfoRow("核心", "${server?.coreType?.displayName ?: ""} ${server?.coreVersion ?: ""}")
                InfoRow("端口", server?.port?.toString() ?: "25565")
                InfoRow("运行时间", if (status == ServerStatus.ONLINE) "在线中" else "已停止")
                InfoRow("世界", server?.worldName ?: "world")
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun MetricTile(
    label: String,
    value: String,
    unit: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = color)
                if (unit.isNotBlank()) {
                    Spacer(Modifier.width(2.dp))
                    Text(unit, style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary)
                }
            }
        }
    }
}
