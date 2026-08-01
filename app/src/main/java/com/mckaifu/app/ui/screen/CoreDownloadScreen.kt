@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.mckaifu.app.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.mckaifu.app.data.model.CoreType
import com.mckaifu.app.data.model.CoreVersion
import com.mckaifu.app.data.model.DownloadProgress
import com.mckaifu.app.ui.component.*
import com.mckaifu.app.ui.navigation.Screen
import com.mckaifu.app.ui.theme.*
import com.mckaifu.app.util.CompatibilityLevel
import com.mckaifu.app.util.formatMcVersion
import com.mckaifu.app.util.getClientVersionHint
import com.mckaifu.app.util.getCompatibilityInfo
import com.mckaifu.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoreDownloadScreen(serverId: String, navController: NavController, vm: MainViewModel = viewModel()) {
    val servers by vm.servers.collectAsState()
    val coreVersions by vm.coreVersions.collectAsState()
    val downloadProgress by vm.downloadProgress.collectAsState()
    var selectedCoreType by remember { mutableStateOf(CoreType.PAPER) }
    val versions = coreVersions[selectedCoreType] ?: emptyList()

    LaunchedEffect(selectedCoreType) {
        if (coreVersions[selectedCoreType] == null) {
            vm.fetchCoreVersions(selectedCoreType)
        }
    }

    val coreTypes = listOf(
        CoreType.PAPER, CoreType.PURPUR, CoreType.PUFFERFISH,
        CoreType.SPIGOT, CoreType.VANILLA, CoreType.NUKKIT, CoreType.POCKETMINE
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("下载服务端核心", fontWeight = FontWeight.Bold) },
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
        ) {
            // Core type selector
            Surface(color = ZalithSurface) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(coreTypes) { coreType ->
                        FilterChip(
                            selected = selectedCoreType == coreType,
                            onClick = { selectedCoreType = coreType },
                            label = { Text(coreType.displayName, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Icon(
                                    if (selectedCoreType == coreType) Icons.Filled.CheckCircle else Icons.Filled.Memory,
                                    null, modifier = Modifier.size(16.dp)
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

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    SectionHeader("${selectedCoreType.displayName} 版本列表")
                    Spacer(Modifier.height(4.dp))
                    Text("共 ${versions.size} 个版本",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary)
                }

                if (versions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = ZalithPrimary)
                        }
                    }
                }

                if (downloadProgress.isDownloading) {
                    item {
                        DownloadProgressBar(downloadProgress)
                    }
                }

                items(versions.take(30), key = { "${it.coreType}_${it.version}_${it.buildNumber}" }) { version ->
                    CoreVersionCard(
                        version = version,
                        onClick = {
                            navController.navigate(
                                Screen.CoreDetail.createRoute(
                                    serverId, version.coreType.name, version.mcVersion, version.buildNumber
                                )
                            )
                        },
                        onDownload = { vm.downloadCore(version, serverId) },
                        isDownloading = downloadProgress.isDownloading
                    )
                }

                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
fun CoreVersionCard(
    version: CoreVersion,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    isDownloading: Boolean
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.clickable(onClick = onClick)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatMcVersion(version.mcVersion, version.coreType),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall)
                        if (version.isRecommended) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = ServerOnline.copy(alpha = 0.15f),
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(" 推荐 ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ServerOnline)
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row {
                        Text("Build #${version.buildNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary)
                        if (version.fileSize > 0) {
                            Text(" | ${formatFileSize(version.fileSize)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary)
                        }
                    }
                }
                Button(
                    onClick = onDownload,
                    enabled = !isDownloading,
                    modifier = Modifier.height(34.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZalithPrimary,
                        disabledContainerColor = ZalithSurfaceVariant
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Filled.Download, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("下载", style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold)
                }
            }
            val compInfo = getCompatibilityInfo(version.mcVersion, version.coreType)
            Spacer(Modifier.height(4.dp))
            Text(compInfo.title, style = MaterialTheme.typography.labelSmall,
                color = when (compInfo.level) {
                    CompatibilityLevel.FULL -> ServerOnline
                    CompatibilityLevel.PARTIAL -> ServerStarting
                    CompatibilityLevel.INCOMPATIBLE -> ServerError
                })
            Spacer(Modifier.height(2.dp))
            Text("客户端: ${getClientVersionHint(version.mcVersion, version.coreType)}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary)
        }
    }
}

fun formatFileSize(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> String.format("%.1f GB", bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> String.format("%.1f MB", bytes / 1_000_000.0)
    bytes >= 1_000 -> String.format("%.1f KB", bytes / 1_000.0)
    else -> "$bytes B"
}
