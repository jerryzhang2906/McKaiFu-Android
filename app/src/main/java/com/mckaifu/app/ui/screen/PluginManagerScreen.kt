@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.mckaifu.app.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.foundation.background
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
import com.mckaifu.app.data.model.PluginInfo
import com.mckaifu.app.ui.component.*
import com.mckaifu.app.ui.navigation.Screen
import com.mckaifu.app.ui.theme.*
import com.mckaifu.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginManagerScreen(serverId: String, navController: NavController, vm: MainViewModel = viewModel()) {
    val servers by vm.servers.collectAsState()
    val server = servers.find { it.id == serverId }
    var selectedTab by remember { mutableIntStateOf(0) }
    var refreshKey by remember { mutableIntStateOf(0) }

    val enabledPlugins = remember(serverId, refreshKey) { vm.listPlugins(serverId) }
    val disabledPlugins = remember(serverId, refreshKey) { vm.listDisabledPlugins(serverId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("插件管理", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { refreshKey++ }) {
                        Icon(Icons.Filled.Refresh, "刷新")
                    }
                    IconButton(onClick = {
                        navController.navigate(Screen.PluginStore.createRoute(serverId))
                    }) {
                        Icon(Icons.Filled.Store, "插件商店")
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
            TabRow(selectedTabIndex = selectedTab, containerColor = ZalithSurface, contentColor = ZalithPrimary) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    text = { Text("已启用 (${enabledPlugins.size})") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    text = { Text("已禁用 (${disabledPlugins.size})") })
                Tab(selected = selectedTab == 2, onClick = {
                    selectedTab = 2
                    navController.navigate(Screen.PluginStore.createRoute(serverId))
                }, text = { Text("插件商店") })
            }

            when (selectedTab) {
                0 -> if (enabledPlugins.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Filled.Extension,
                        title = "暂无插件",
                        subtitle = "去插件商店安装,或将 JAR 放入 plugins 目录"
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            SectionHeader("已启用的插件")
                            Spacer(Modifier.height(4.dp))
                            Text("共 ${enabledPlugins.size} 个插件",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary)
                        }
                        items(enabledPlugins, key = { it.name }) { plugin ->
                            PluginInstalledCard(plugin, serverId, vm, onChanged = { refreshKey++ })
                        }
                    }
                }
                1 -> if (disabledPlugins.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Filled.ExtensionOff,
                        title = "没有禁用的插件",
                        subtitle = "在已启用列表关闭插件后会显示在这里"
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            SectionHeader("已禁用的插件")
                            Spacer(Modifier.height(4.dp))
                            Text("共 ${disabledPlugins.size} 个插件",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary)
                        }
                        items(disabledPlugins, key = { it.name }) { plugin ->
                            PluginInstalledCard(plugin, serverId, vm, onChanged = { refreshKey++ })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PluginInstalledCard(plugin: PluginInfo, serverId: String, vm: MainViewModel, onChanged: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(ZalithPrimary.copy(alpha = 0.2f), MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Extension, null,
                        tint = if (plugin.isEnabled) ZalithPrimary else TextSecondary,
                        modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(plugin.name, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall)
                    Text(plugin.fileName,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary)
                }
            }
            Switch(
                checked = plugin.isEnabled,
                onCheckedChange = { enable ->
                    if (vm.togglePlugin(serverId, plugin.fileName, enable)) {
                        onChanged()
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = ServerOnline,
                    checkedThumbColor = Color.White,
                    uncheckedTrackColor = ZalithCardBorder
                )
            )
        }
        if (!plugin.isEnabled) {
            Spacer(Modifier.height(4.dp))
            Text("已禁用(文件重命名为 .jar.disabled,重启后生效)",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary)
        }
    }
}
