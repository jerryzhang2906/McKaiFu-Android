@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.mckaifu.app.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mckaifu.app.data.model.DownloadProgress
import com.mckaifu.app.data.model.PluginInfo
import com.mckaifu.app.data.model.PluginSource
import com.mckaifu.app.ui.component.*
import com.mckaifu.app.ui.navigation.Screen
import kotlinx.coroutines.delay
import com.mckaifu.app.ui.theme.*
import com.mckaifu.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginStoreScreen(serverId: String, navController: NavController, vm: MainViewModel = viewModel()) {
    val servers by vm.servers.collectAsState()
    val storePlugins by vm.storePlugins.collectAsState()
    val downloadProgress by vm.downloadProgress.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var selectedSource by remember { mutableStateOf<PluginSource?>(null) }
    var showServerPicker by remember { mutableStateOf(false) }
    var pendingPlugin by remember { mutableStateOf<PluginInfo?>(null) }
    var showLoadError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.getFeaturedPlugins()
    }

    LaunchedEffect(storePlugins.isEmpty(), vm.downloadError.collectAsState().value) {
        showLoadError = vm.downloadError.value != null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("插件商店", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(Icons.Filled.Search, "搜索",
                            tint = if (showSearch) ZalithPrimary else TextSecondary)
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
            if (showSearch) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = {
                        searchQuery = it
                        if (it.length >= 2) vm.searchPlugins(it)
                    },
                    onClose = { showSearch = false }
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("全部", "Modrinth", "Spigot").forEach { source ->
                            FilterChip(
                                selected = selectedSource?.name?.lowercase() == source.lowercase() || (source == "全部" && selectedSource == null),
                                onClick = {
                                    selectedSource = when (source) {
                                        "Modrinth" -> PluginSource.MODRINTH
                                        "Spigot" -> PluginSource.SPIGET
                                        else -> null
                                    }
                                },
                                label = { Text(source, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ZalithPrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = ZalithPrimary
                                )
                            )
                        }
                    }
                }

                if (downloadProgress.isDownloading) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        DownloadProgressBar(downloadProgress)
                    }
                }

                val displayPlugins = storePlugins.let { list ->
                    if (selectedSource != null) list.filter { it.source == selectedSource } else list
                }

                if (displayPlugins.isEmpty() && !downloadProgress.isDownloading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(240.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (showLoadError) {
                                    Icon(Icons.Filled.CloudOff, null, tint = ServerError,
                                        modifier = Modifier.size(40.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("加载失败，请检查网络", color = ServerError)
                                    Spacer(Modifier.height(8.dp))
                                    Button(
                                        onClick = { showLoadError = false; vm.getFeaturedPlugins() },
                                        colors = ButtonDefaults.buttonColors(containerColor = ZalithPrimary)
                                    ) { Text("重试") }
                                } else if (searchQuery.isNotBlank()) {
                                    Icon(Icons.Filled.SearchOff, null, tint = TextSecondary,
                                        modifier = Modifier.size(40.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("未找到相关插件", color = TextSecondary)
                                } else {
                                    CircularProgressIndicator(color = ZalithPrimary)
                                }
                            }
                        }
                    }
                }

                items(displayPlugins, key = { "${it.source.name}_${it.id}" }) { plugin ->
                    PluginStoreCard(
                        plugin = plugin,
                        onClick = {
                            navController.navigate(
                                Screen.PluginDetail.createRoute(serverId, plugin.source.name, plugin.id)
                            )
                        },
                        onInstall = {
                            pendingPlugin = plugin
                            showServerPicker = true
                        }
                    )
                }

                item { Spacer(Modifier.height(32.dp)) }
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
                pendingPlugin?.let { vm.installPlugin(it, target.id) }
            }
        )
    }
}

@Composable
fun PluginStoreCard(plugin: PluginInfo, onClick: () -> Unit, onInstall: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.clickable(onClick = onClick)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(plugin.name, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = when (plugin.source) {
                                PluginSource.MODRINTH -> ZalithPrimary.copy(alpha = 0.15f)
                                PluginSource.SPIGET -> ServerStarting.copy(alpha = 0.15f)
                                else -> TextSecondary.copy(alpha = 0.15f)
                            },
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(" ${plugin.source.name} ",
                                style = MaterialTheme.typography.labelSmall,
                                color = when (plugin.source) {
                                    PluginSource.MODRINTH -> ZalithPrimary
                                    PluginSource.SPIGET -> ServerStarting
                                    else -> TextSecondary
                                })
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("v${plugin.version} | ${plugin.author}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text(plugin.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    if (plugin.downloadsCount > 0) {
                        Spacer(Modifier.height(4.dp))
                        Text("${plugin.downloadsCount} 次下载",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onInstall,
                modifier = Modifier.height(34.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ZalithPrimary
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
            ) {
                Icon(Icons.Filled.Download, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("安装", style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DownloadProgressBar(progress: DownloadProgress) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                progress = progress.progress,
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp,
                color = ZalithPrimary
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("正在下载...", style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold)
                Text(progress.currentFile,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                LinearProgressIndicator(
                    progress = progress.progress,
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = ZalithPrimary,
                    trackColor = ZalithCardBorder
                )
            }
            Text("${(progress.progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = ZalithPrimary,
                fontWeight = FontWeight.Bold)
        }
    }
}
