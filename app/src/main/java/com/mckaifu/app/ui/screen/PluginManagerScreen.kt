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

    val mockPlugins = listOf(
        PluginInfo("EssentialsX", "2.20.1", "EssentialsX团队", "基础插件", "essentials.jar", isEnabled = true),
        PluginInfo("LuckPerms", "5.4.7", "Luck", "权限管理", "luckperms.jar", isEnabled = true),
        PluginInfo("WorldEdit", "7.3.0", "EngineHub", "世界编辑", "worldedit.jar", isEnabled = true),
        PluginInfo("Vault", "1.7.3", "MilkBowl", "经济接口", "vault.jar", isEnabled = false),
        PluginInfo("PlaceholderAPI", "2.11.5", "PlaceholderAPI", "变量插件", "placeholderapi.jar", isEnabled = true),
    )

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
                    text = { Text("已安装 (${mockPlugins.size})") })
                Tab(selected = selectedTab == 1, onClick = {
                    selectedTab = 1
                    navController.navigate(Screen.PluginStore.createRoute(serverId))
                }, text = { Text("插件商店") })
            }

            when (selectedTab) {
                0 -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        SectionHeader("已安装的插件")
                        Spacer(Modifier.height(4.dp))
                        Text("共 ${mockPlugins.size} 个插件",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary)
                    }
                    items(mockPlugins, key = { it.name }) { plugin ->
                        PluginInstalledCard(plugin, serverId, vm)
                    }
                }
            }
        }
    }
}

@Composable
fun PluginInstalledCard(plugin: PluginInfo, serverId: String, vm: MainViewModel) {
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
                    Text("v${plugin.version} | ${plugin.author}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary)
                }
            }
            Switch(
                checked = plugin.isEnabled,
                onCheckedChange = {
                    val cmd = if (it) "plugin enable ${plugin.name}" else "plugin disable ${plugin.name}"
                    vm.sendCommand(serverId, cmd)
                },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = ServerOnline,
                    checkedThumbColor = Color.White,
                    uncheckedTrackColor = ZalithCardBorder
                )
            )
        }
        if (plugin.description.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(plugin.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary)
        }
        if (!plugin.isLoaded) {
            Spacer(Modifier.height(4.dp))
            Text("插件未加载", style = MaterialTheme.typography.labelSmall,
                color = ServerError)
        }
    }
}
