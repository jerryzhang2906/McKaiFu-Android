@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.mckaifu.app.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mckaifu.app.ui.component.*
import com.mckaifu.app.ui.navigation.Screen
import com.mckaifu.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(ZalithPrimary, Color(0xFF7B4FFF))
                        ),
                        MaterialTheme.shapes.extraLarge
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Dns, null, modifier = Modifier.size(50.dp),
                    tint = Color.White)
            }

            Spacer(Modifier.height(24.dp))

            Text("McKaiFu", style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold)
            Text("开服大师", style = MaterialTheme.typography.titleMedium,
                color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            Text("v1.0.0", style = MaterialTheme.typography.bodyLarge,
                color = ZalithPrimary, fontWeight = FontWeight.Medium)

            Spacer(Modifier.height(32.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                InfoRow("版本号", "1.0.0")
                InfoRow("构建号", "20240731")
                InfoRow("最低Android", "8.0 (API 26)")
                InfoRow("目标Android", "14.0 (API 34)")
                InfoRow("开发框架", "Jetpack Compose + MD3")
            }

            Spacer(Modifier.height(16.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate(Screen.JavaRuntime.route) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Storage, null, tint = ZalithPrimary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Java 运行时管理", fontWeight = FontWeight.Bold)
                        Text("查看/安装内置 Java 17、21、25 运行时",
                            style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = TextSecondary)
                }
            }

            Spacer(Modifier.height(16.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("功能特性", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "• 多服务端核心支持 (Paper, Purpur, Pufferfish, Nukkit等)\n" +
                    "• 全生命周期管理 (启动/停止/重启)\n" +
                    "• 实时控制台带日志颜色区分\n" +
                    "• 性能监控仪表盘 (TPS/内存/CPU)\n" +
                    "• 玩家管理与封禁系统\n" +
                    "• 世界管理与自动备份\n" +
                    "• 内置文件管理器\n" +
                    "• 内网穿透 (Playit.gg / Ngrok)\n" +
                    "• Geyser 跨平台支持\n" +
                    "• 插件商店与核心下载\n" +
                    "• 定时任务与自动维护",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                )
            }

            Spacer(Modifier.weight(1f))

            Text("© 2024 McKaiFu Team",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center)
            Text("Minecraft 是 Mojang AB 的商标",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary.copy(alpha = 0.5f),
                textAlign = TextAlign.Center)

            Spacer(Modifier.height(16.dp))
        }
    }
}
