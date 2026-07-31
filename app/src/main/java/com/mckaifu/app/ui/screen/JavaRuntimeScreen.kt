@file:OptIn(ExperimentalMaterial3Api::class)

package com.mckaifu.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.mckaifu.app.service.JavaRuntimeManager
import com.mckaifu.app.ui.component.*
import com.mckaifu.app.ui.theme.*
import com.mckaifu.app.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JavaRuntimeScreen(
    navController: NavController,
    vm: MainViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    var statuses by remember {
        mutableStateOf(vm.getJavaRuntimeStatus())
    }
    var busyVersion by remember { mutableStateOf<Int?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Java 运行时", fontWeight = FontWeight.Bold) },
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
                .background(ZalithBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, null, tint = ZalithPrimary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("内置 Java 运行时", fontWeight = FontWeight.Bold)
                        Text(
                            "从应用资产中解压，无需额外安装。不同服务端核心需要不同的 Java 版本。",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            if (message != null) {
                GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = ServerError.copy(alpha = 0.4f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, null, tint = ServerError, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(message ?: "", style = MaterialTheme.typography.bodySmall, color = ServerError)
                    }
                }
            }

            statuses.forEach { status ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Storage,
                                null,
                                tint = if (status.installed) ServerOnline else TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Java ${status.version}", fontWeight = FontWeight.Bold)
                                Text(
                                    when {
                                        status.installed -> "已安装 ${status.javaPath?.let { shortPath(it) } ?: ""}"
                                        status.bundled -> "内置（未解压）"
                                        else -> "未内置"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Row {
                            if (status.installed) {
                                TextButton(
                                    onClick = {
                                        scope.launch(Dispatchers.IO) {
                                            vm.deleteJavaRuntime(status.version)
                                            withContext(Dispatchers.Main) {
                                                statuses = vm.getJavaRuntimeStatus()
                                            }
                                        }
                                    }
                                ) {
                                    Text("删除", color = ServerError)
                                }
                            } else if (status.bundled) {
                                Button(
                                    onClick = {
                                        busyVersion = status.version
                                        message = null
                                        vm.downloadJavaRuntime(status.version) { ok, err ->
                                            busyVersion = null
                                            statuses = vm.getJavaRuntimeStatus()
                                            message = if (ok) null else (err ?: "解压失败，请重试")
                                        }
                                    },
                                    enabled = busyVersion == null,
                                    colors = ButtonDefaults.buttonColors(containerColor = ZalithPrimary)
                                ) {
                                    if (busyVersion == status.version) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text("解压安装")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("使用建议", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "• 老版本服务器核心 (1.8~1.16) 使用 Java 17\n" +
                    "• Paper/Purpur/Pufferfish 1.17+ 推荐 Java 21\n" +
                    "• 最新版核心推荐 Java 25\n" +
                    "• 若未安装任何运行时，服务器启动时将回退到系统 Java",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun shortPath(path: String): String {
    return path.substringAfterLast('/')
}
