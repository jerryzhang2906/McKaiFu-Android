@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.mckaifu.app.ui.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.animation.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mckaifu.app.ui.screen.*
import com.mckaifu.app.util.AppPrefs
import com.mckaifu.app.viewmodel.MainViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    data object Servers : Screen("servers", "服务器", Icons.Outlined.Dns, Icons.Filled.Dns)
    data object Console : Screen("console/{serverId}", "控制台", Icons.Outlined.Terminal, Icons.Filled.Terminal) {
        fun createRoute(serverId: String) = "console/$serverId"
    }
    data object Players : Screen("players/{serverId}", "玩家", Icons.Outlined.People, Icons.Filled.People) {
        fun createRoute(serverId: String) = "players/$serverId"
    }
    data object Worlds : Screen("worlds/{serverId}", "世界", Icons.Outlined.Public, Icons.Filled.Public) {
        fun createRoute(serverId: String) = "worlds/$serverId"
    }
    data object Settings : Screen("settings/{serverId}", "设置", Icons.Outlined.Settings, Icons.Filled.Settings) {
        fun createRoute(serverId: String) = "settings/$serverId"
    }
    data object ServerDetail : Screen("server_detail/{serverId}", "服务器详情", Icons.Outlined.Dashboard, Icons.Filled.Dashboard) {
        fun createRoute(serverId: String) = "server_detail/$serverId"
    }
    data object CreateServer : Screen("create_server", "创建服务器", Icons.Outlined.Add, Icons.Filled.Add)
    data object FileManager : Screen("files/{serverId}", "文件管理", Icons.Outlined.Folder, Icons.Filled.Folder) {
        fun createRoute(serverId: String) = "files/$serverId"
    }
    data object Plugins : Screen("plugins/{serverId}", "插件管理", Icons.Outlined.Extension, Icons.Filled.Extension) {
        fun createRoute(serverId: String) = "plugins/$serverId"
    }
    data object Dashboard : Screen("dashboard/{serverId}", "仪表盘", Icons.Outlined.Insights, Icons.Filled.Insights) {
        fun createRoute(serverId: String) = "dashboard/$serverId"
    }
    data object Tunnel : Screen("tunnel/{serverId}", "内网穿透", Icons.Outlined.Wifi, Icons.Filled.Wifi) {
        fun createRoute(serverId: String) = "tunnel/$serverId"
    }
    data object BanList : Screen("bans/{serverId}", "封禁列表", Icons.Outlined.Block, Icons.Filled.Block) {
        fun createRoute(serverId: String) = "bans/$serverId"
    }
    data object Schedule : Screen("schedule/{serverId}", "定时任务", Icons.Outlined.Schedule, Icons.Filled.Schedule) {
        fun createRoute(serverId: String) = "schedule/$serverId"
    }
    data object WebMap : Screen("webmap/{serverId}", "Web地图", Icons.Outlined.Map, Icons.Filled.Map) {
        fun createRoute(serverId: String) = "webmap/$serverId"
    }
    data object CoreDownload : Screen("core_download/{serverId}", "下载核心", Icons.Outlined.CloudDownload, Icons.Filled.CloudDownload) {
        fun createRoute(serverId: String) = "core_download/$serverId"
    }
    data object PluginStore : Screen("plugin_store/{serverId}", "插件商店", Icons.Outlined.Store, Icons.Filled.Store) {
        fun createRoute(serverId: String) = "plugin_store/$serverId"
    }
    data object PluginDetail : Screen("plugin_detail/{serverId}/{source}/{pluginId}", "插件详情", Icons.Outlined.Info, Icons.Filled.Info) {
        fun createRoute(serverId: String, source: String, pluginId: String) = "plugin_detail/$serverId/$source/$pluginId"
    }
    data object CoreDetail : Screen("core_detail/{serverId}/{coreType}/{mcVersion}/{build}", "核心详情", Icons.Outlined.Info, Icons.Filled.Info) {
        fun createRoute(serverId: String, coreType: String, mcVersion: String, build: Int) = "core_detail/$serverId/$coreType/$mcVersion/$build"
    }
    data object ConfigEditor : Screen("config/{serverId}", "配置编辑", Icons.Outlined.Tune, Icons.Filled.Tune) {
        fun createRoute(serverId: String) = "config/$serverId"
    }
    data object Chat : Screen("chat/{serverId}", "聊天", Icons.Outlined.Chat, Icons.Filled.Chat) {
        fun createRoute(serverId: String) = "chat/$serverId"
    }
    data object Community : Screen("community/{serverId}", "社区", Icons.Outlined.Groups, Icons.Filled.Groups) {
        fun createRoute(serverId: String) = "community/$serverId"
    }
    data object About : Screen("about", "关于", Icons.Outlined.Info, Icons.Filled.Info)
    data object JavaRuntime : Screen("java_runtime", "Java运行时", Icons.Outlined.Storage, Icons.Filled.Storage)
    data object Onboarding : Screen("onboarding", "引导", Icons.Outlined.TravelExplore, Icons.Filled.TravelExplore)
    data object Auth : Screen("auth", "验证", Icons.Outlined.Lock, Icons.Filled.Lock)
}

val bottomNavItems = listOf(
    Screen.Servers,
    Screen.Console,
    Screen.Players,
    Screen.Worlds,
    Screen.Settings,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val vm: MainViewModel = viewModel()
    val selectedServerId by vm.selectedServerId.collectAsState()
    val currentServerId = navBackStackEntry?.arguments?.getString("serverId")

    LaunchedEffect(Unit) {
        vm.startScheduler()
    }

    LaunchedEffect(currentDestination) {
        val sid = navBackStackEntry?.arguments?.getString("serverId")
        if (sid != null && sid != selectedServerId) vm.selectServer(sid)
    }

    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route } ||
            currentDestination?.route?.startsWith("console/") == true ||
            currentDestination?.route?.startsWith("players/") == true ||
            currentDestination?.route?.startsWith("worlds/") == true ||
            currentDestination?.route?.startsWith("settings/") == true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == screen.route
                        } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.icon,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title, style = MaterialTheme.typography.labelSmall) },
                            selected = selected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ),
                            onClick = {
                                val currentRoute = currentDestination?.route
                                val tabPrefix = screen.route.substringBefore("/")
                                if (currentRoute == screen.route ||
                                    currentRoute?.startsWith("$tabPrefix/") == true
                                ) {
                                    return@NavigationBarItem
                                }
                                val sid = currentServerId ?: selectedServerId
                                val route = when (screen) {
                                    Screen.Console -> if (sid != null)
                                        Screen.Console.createRoute(sid) else Screen.Servers.route
                                    Screen.Players -> if (sid != null)
                                        Screen.Players.createRoute(sid) else Screen.Servers.route
                                    Screen.Worlds -> if (sid != null)
                                        Screen.Worlds.createRoute(sid) else Screen.Servers.route
                                    Screen.Settings -> if (sid != null)
                                        Screen.Settings.createRoute(sid) else Screen.Servers.route
                                    else -> screen.route
                                }
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        val context = LocalContext.current
        NavHost(
            navController = navController,
            startDestination = if (AppPrefs.isOnboardingDone(context)) Screen.Servers.route else Screen.Onboarding.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(navController)
            }
            composable(Screen.Servers.route) {
                ServerListScreen(navController)
            }
            composable(
                route = Screen.Console.route,
                arguments = listOf(navArgument("serverId") { type = NavType.StringType })
            ) { backStackEntry ->
                val sid = backStackEntry.arguments?.getString("serverId") ?: ""
                ConsoleScreen(sid, navController)
            }
            composable(
                route = Screen.Players.route,
                arguments = listOf(navArgument("serverId") { type = NavType.StringType })
            ) { backStackEntry ->
                val sid = backStackEntry.arguments?.getString("serverId") ?: ""
                PlayerManagementScreen(sid, navController)
            }
            composable(
                route = Screen.Worlds.route,
                arguments = listOf(navArgument("serverId") { type = NavType.StringType })
            ) { backStackEntry ->
                val sid = backStackEntry.arguments?.getString("serverId") ?: ""
                WorldManagementScreen(sid, navController)
            }
            composable(
                route = Screen.Settings.route,
                arguments = listOf(navArgument("serverId") { type = NavType.StringType })
            ) { backStackEntry ->
                val sid = backStackEntry.arguments?.getString("serverId") ?: ""
                SettingsScreen(sid, navController)
            }
            composable(
                route = Screen.ServerDetail.route,
                arguments = listOf(navArgument("serverId") { type = NavType.StringType })
            ) { backStackEntry ->
                val sid = backStackEntry.arguments?.getString("serverId") ?: ""
                ServerDetailScreen(sid, navController)
            }
            composable(Screen.CreateServer.route) {
                CreateServerScreen(navController)
            }
            composable(
                route = Screen.FileManager.route,
                arguments = listOf(navArgument("serverId") { type = NavType.StringType })
            ) { backStackEntry ->
                val sid = backStackEntry.arguments?.getString("serverId") ?: ""
                FileManagerScreen(sid, navController)
            }
            composable(
                route = Screen.Plugins.route,
                arguments = listOf(navArgument("serverId") { type = NavType.StringType })
            ) { backStackEntry ->
                val sid = backStackEntry.arguments?.getString("serverId") ?: ""
                PluginManagerScreen(sid, navController)
            }
            composable(
                route = Screen.Dashboard.route,
                arguments = listOf(navArgument("serverId") { type = NavType.StringType })
            ) { backStackEntry ->
                val sid = backStackEntry.arguments?.getString("serverId") ?: ""
                DashboardScreen(sid, navController)
            }
            composable(
                route = Screen.Tunnel.route,
                arguments = listOf(navArgument("serverId") { type = NavType.StringType })
            ) { backStackEntry ->
                val sid = backStackEntry.arguments?.getString("serverId") ?: ""
                TunnelScreen(sid, navController)
            }
            composable(
                route = Screen.BanList.route,
                arguments = listOf(navArgument("serverId") { type = NavType.StringType })
            ) { backStackEntry ->
                val sid = backStackEntry.arguments?.getString("serverId") ?: ""
                BanListScreen(sid, navController)
            }
            composable(
                route = Screen.Schedule.route,
                arguments = listOf(navArgument("serverId") { type = NavType.StringType })
            ) { backStackEntry ->
                val sid = backStackEntry.arguments?.getString("serverId") ?: ""
                ScheduleScreen(sid, navController)
            }
            composable(
                route = Screen.WebMap.route,
                arguments = listOf(navArgument("serverId") { type = NavType.StringType })
            ) { backStackEntry ->
                val sid = backStackEntry.arguments?.getString("serverId") ?: ""
                WebMapScreen(sid, navController)
            }
            composable(
                route = Screen.CoreDownload.route,
                arguments = listOf(navArgument("serverId") { type = NavType.StringType })
            ) { backStackEntry ->
                val sid = backStackEntry.arguments?.getString("serverId") ?: ""
                CoreDownloadScreen(sid, navController)
            }
            composable(
                route = Screen.PluginStore.route,
                arguments = listOf(navArgument("serverId") { type = NavType.StringType })
            ) { backStackEntry ->
                val sid = backStackEntry.arguments?.getString("serverId") ?: ""
                PluginStoreScreen(sid, navController)
            }
            composable(
                route = Screen.PluginDetail.route,
                arguments = listOf(
                    navArgument("serverId") { type = NavType.StringType },
                    navArgument("source") { type = NavType.StringType },
                    navArgument("pluginId") { type = NavType.StringType },
                )
            ) { backStackEntry ->
                val sid = backStackEntry.arguments?.getString("serverId") ?: ""
                val source = backStackEntry.arguments?.getString("source") ?: "MODRINTH"
                val pluginId = backStackEntry.arguments?.getString("pluginId") ?: ""
                PluginDetailScreen(sid, source, pluginId, navController)
            }
            composable(
                route = Screen.CoreDetail.route,
                arguments = listOf(
                    navArgument("serverId") { type = NavType.StringType },
                    navArgument("coreType") { type = NavType.StringType },
                    navArgument("mcVersion") { type = NavType.StringType },
                    navArgument("build") { type = NavType.IntType },
                )
            ) { backStackEntry ->
                val sid = backStackEntry.arguments?.getString("serverId") ?: ""
                val coreType = backStackEntry.arguments?.getString("coreType") ?: "PAPER"
                val mcVersion = backStackEntry.arguments?.getString("mcVersion") ?: ""
                val build = backStackEntry.arguments?.getInt("build") ?: 0
                CoreDetailScreen(sid, coreType, mcVersion, build, navController)
            }
            composable(
                route = Screen.ConfigEditor.route,
                arguments = listOf(navArgument("serverId") { type = NavType.StringType })
            ) { backStackEntry ->
                val sid = backStackEntry.arguments?.getString("serverId") ?: ""
                ConfigEditorScreen(sid, navController)
            }
            composable(
                route = Screen.Chat.route,
                arguments = listOf(navArgument("serverId") { type = NavType.StringType })
            ) { backStackEntry ->
                val sid = backStackEntry.arguments?.getString("serverId") ?: ""
                ChatScreen(sid, navController)
            }
            composable(
                route = Screen.Community.route,
                arguments = listOf(navArgument("serverId") { type = NavType.StringType })
            ) { backStackEntry ->
                val sid = backStackEntry.arguments?.getString("serverId") ?: ""
                CommunityScreen(sid, navController)
            }
            composable(Screen.About.route) {
                AboutScreen(navController)
            }
            composable(Screen.JavaRuntime.route) {
                JavaRuntimeScreen(navController)
            }
            composable(Screen.Auth.route) {
                AuthScreen(
                    isSettingPassword = false,
                    onSuccess = {
                        navController.navigate(Screen.Servers.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    },
                    navController = navController
                )
            }
        }
    }
}
