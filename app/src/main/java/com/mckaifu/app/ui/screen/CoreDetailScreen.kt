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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mckaifu.app.data.model.CoreType
import com.mckaifu.app.data.model.CoreVersion
import com.mckaifu.app.ui.component.*
import com.mckaifu.app.ui.theme.*
import com.mckaifu.app.util.CompatibilityLevel
import com.mckaifu.app.util.formatMcVersion
import com.mckaifu.app.util.getClientVersionHint
import com.mckaifu.app.util.getCompatibilityInfo
import com.mckaifu.app.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoreDetailScreen(
    serverId: String,
    coreTypeName: String,
    mcVersion: String,
    build: Int,
    navController: NavController,
    vm: MainViewModel = viewModel()
) {
    val servers by vm.servers.collectAsState()
    val coreVersions by vm.coreVersions.collectAsState()
    val downloadProgress by vm.downloadProgress.collectAsState()
    val server = servers.find { it.id == serverId }

    var showServerPicker by remember { mutableStateOf(false) }

    val coreType = CoreType.entries.find { it.name == coreTypeName } ?: CoreType.PAPER

    val version = remember(coreVersions, coreTypeName, mcVersion, build) {
        coreVersions[coreType]?.find {
            it.mcVersion == mcVersion && it.buildNumber == build
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("核心详情", fontWeight = FontWeight.Bold) },
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
            if (version == null) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ZalithPrimary)
                }
            } else {
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
                        Icon(Icons.Filled.Memory, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(version.coreType.displayName, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        Text(formatMcVersion(version.mcVersion, version.coreType) + " · Build #${version.buildNumber}",
                            style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    if (version.isRecommended) {
                        Surface(
                            color = ServerOnline.copy(alpha = 0.15f),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(" 推荐 ", style = MaterialTheme.typography.labelSmall, color = ServerOnline)
                        }
                    }
                }

                val compInfo = getCompatibilityInfo(version.mcVersion, version.coreType)
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = when (compInfo.level) {
                        CompatibilityLevel.FULL -> ServerOnline.copy(alpha = 0.3f)
                        CompatibilityLevel.PARTIAL -> ServerStarting.copy(alpha = 0.3f)
                        else -> ServerError.copy(alpha = 0.3f)
                    }
                ) {
                    InfoRow("核心类型", version.coreType.displayName)
                    InfoRow("Minecraft 版本", formatMcVersion(version.mcVersion, version.coreType))
                    InfoRow("可加入客户端", getClientVersionHint(version.mcVersion, version.coreType))
                    if (version.buildNumber > 0) InfoRow("构建号", "#${version.buildNumber}")
                    if (version.fileSize > 0) InfoRow("文件大小", formatFileSize(version.fileSize))
                    if (version.releaseDate > 0) {
                        InfoRow("发布日期", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(version.releaseDate)))
                    }
                    InfoRow("兼容性", compInfo.title)
                    Spacer(Modifier.height(8.dp))
                    Text(compInfo.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(version.coreType.description,
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }

                if (downloadProgress.isDownloading) {
                    DownloadProgressBar(downloadProgress)
                }

                GradientButton(
                    text = "下载并安装到服务器",
                    icon = Icons.Filled.Download,
                    onClick = { showServerPicker = true },
                    enabled = !downloadProgress.isDownloading,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(24.dp))
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
                version?.let { vm.downloadCore(it, target.id) }
            }
        )
    }
}
