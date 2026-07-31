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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mckaifu.app.data.model.PluginInfo
import com.mckaifu.app.data.model.PluginSource
import com.mckaifu.app.ui.component.*
import com.mckaifu.app.ui.theme.*
import com.mckaifu.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginDetailScreen(
    serverId: String,
    sourceName: String,
    pluginId: String,
    navController: NavController,
    vm: MainViewModel = viewModel()
) {
    val servers by vm.servers.collectAsState()
    val detail by vm.pluginDetail.collectAsState()
    val downloadProgress by vm.downloadProgress.collectAsState()
    val downloading by vm.pluginDownloading.collectAsState()
    val error by vm.downloadError.collectAsState()
    val server = servers.find { it.id == serverId }

    var showServerPicker by remember { mutableStateOf(false) }

    val source = PluginSource.entries.find { it.name == sourceName } ?: PluginSource.UNKNOWN
    val plugin = detail

    LaunchedEffect(pluginId) {
        val cached = vm.storePlugins.value.find { it.id == pluginId }
        val seed = cached ?: PluginInfo(id = pluginId, source = source)
        vm.openPluginDetail(seed, server?.coreVersion)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("插件详情", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        val plugin = detail
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(ZalithBackground)
        ) {
            if (plugin == null || plugin.name.isBlank()) {                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ZalithPrimary)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PluginHeader(plugin)

                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        InfoRow("作者", plugin.author.ifBlank { "未知" })
                        InfoRow("版本", plugin.version)
                        InfoRow("来源", plugin.source.displayName)
                        if (plugin.downloadsCount > 0) {
                            InfoRow("下载次数", formatNumber(plugin.downloadsCount))
                        }
                        if (plugin.fileSize > 0) {
                            InfoRow("文件大小", formatFileSize(plugin.fileSize))
                        }
                        if (plugin.categories.isNotEmpty()) {
                            InfoRow("分类", plugin.categories.joinToString("、"))
                        }
                        if (plugin.loaders.isNotEmpty()) {
                            InfoRow("加载器", plugin.loaders.joinToString("、"))
                        }
                        if (plugin.gameVersions.isNotEmpty()) {
                            InfoRow(
                                "支持版本",
                                plugin.gameVersions.takeLast(8).joinToString("、") +
                                        if (plugin.gameVersions.size > 8) " 等${plugin.gameVersions.size}个版本" else ""
                            )
                        }
                    }

                    if (!plugin.body.isNullOrBlank()) {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("插件介绍", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stripMarkdown(plugin.body ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                        }
                    }

                    if (error != null) {
                        GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = ServerError.copy(alpha = 0.4f)) {
                            Text(error ?: "", color = ServerError, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    if (downloadProgress.isDownloading) {
                        DownloadProgressBar(downloadProgress)
                    }

                    GradientButton(
                        text = if (downloading) "正在解析下载地址..." else "安装到服务器",
                        icon = Icons.Filled.Download,
                        onClick = { showServerPicker = true },
                        loading = downloading,
                        enabled = !downloading && !downloadProgress.isDownloading,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    if (showServerPicker) {
        ServerSelectDialog(
            title = "选择安装到哪个服务器",
            servers = servers,
            currentServerId = serverId,
            onDismiss = { showServerPicker = false },
            onSelect = { target ->
                showServerPicker = false
                plugin?.let { vm.installPlugin(it, target.id) }
            }
        )
    }
}

@Composable
private fun PluginHeader(plugin: PluginInfo) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    Brush.linearGradient(listOf(ZalithPrimary, Color(0xFF7B4FFF))),
                    MaterialTheme.shapes.medium
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Extension, null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(plugin.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${plugin.author.ifBlank { "未知作者" }} · v${plugin.version}",
                style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Surface(
                color = when (plugin.source) {
                    PluginSource.MODRINTH -> ZalithPrimary.copy(alpha = 0.15f)
                    PluginSource.SPIGET -> ServerStarting.copy(alpha = 0.15f)
                    else -> TextSecondary.copy(alpha = 0.15f)
                },
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text(" ${plugin.source.displayName} ", style = MaterialTheme.typography.labelSmall,
                    color = when (plugin.source) {
                        PluginSource.MODRINTH -> ZalithPrimary
                        PluginSource.SPIGET -> ServerStarting
                        else -> TextSecondary
                    })
            }
        }
    }
}

private fun stripMarkdown(md: String): String {
    return md
        .replace(Regex("!\\[[^]]*]\\([^)]*\\)"), "")
        .replace(Regex("\\[[^]]*]\\([^)]*\\)"), "")
        .replace(Regex("`{1,3}"), "")
        .replace(Regex("\\*\\*|__"), "")
        .replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE), "")
        .replace(Regex("^[\\s]*[-*+]\\s+", RegexOption.MULTILINE), "• ")
        .replace(Regex("^\\s*\\d+\\.\\s+", RegexOption.MULTILINE), "• ")
        .replace(Regex("^\\s*>\\s?", RegexOption.MULTILINE), "")
        .trim()
}

private fun formatNumber(n: Long): String = when {
    n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000.0)
    n >= 1_000 -> String.format("%.1fK", n / 1_000.0)
    else -> n.toString()
}
