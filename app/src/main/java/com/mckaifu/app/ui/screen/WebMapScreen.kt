@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.mckaifu.app.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mckaifu.app.ui.component.*
import com.mckaifu.app.ui.theme.*
import com.mckaifu.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebMapScreen(serverId: String, navController: NavController, vm: MainViewModel = viewModel()) {
    val servers by vm.servers.collectAsState()
    val server = servers.find { it.id == serverId }
    var mapUrl by remember { mutableStateOf("") }
    var selectedMapType by remember { mutableStateOf("Dynmap") }
    var showUrlInput by remember { mutableStateOf(true) }

    val mapTypes = listOf(
        "Dynmap" to "http://localhost:8123",
        "BlueMap" to "http://localhost:8100",
        "Pl3xMap" to "http://localhost:8090",
        "Squaremap" to "http://localhost:8080",
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Web地图", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showUrlInput = !showUrlInput }) {
                        Icon(Icons.Filled.Edit, "编辑地址",
                            tint = if (showUrlInput) ZalithPrimary else TextSecondary)
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
            if (showUrlInput) {
                Surface(color = ZalithSurfaceVariant) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("选择地图类型",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            mapTypes.forEach { (name, url) ->
                                FilterChip(
                                    selected = selectedMapType == name,
                                    onClick = {
                                        selectedMapType = name
                                        mapUrl = url
                                        showUrlInput = false
                                    },
                                    label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ZalithPrimary.copy(alpha = 0.2f),
                                        selectedLabelColor = ZalithPrimary
                                    )
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = mapUrl,
                            onValueChange = { mapUrl = it },
                            label = { Text("或输入自定义URL") },
                            placeholder = { Text("http://your-server:8123") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = outFieldColors()
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { showUrlInput = false },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ZalithPrimary)
                        ) {
                            Icon(Icons.Filled.OpenInNew, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("打开地图", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (mapUrl.isNotBlank() && !showUrlInput) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                webViewClient = WebViewClient()
                                webChromeClient = WebChromeClient()
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    allowFileAccess = true
                                    setSupportZoom(true)
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                }
                                loadUrl(mapUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Floating reload button
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.End
                    ) {
                        FloatingActionButton(
                            onClick = { showUrlInput = true },
                            containerColor = ZalithSurface,
                            contentColor = ZalithPrimary
                        ) {
                            Icon(Icons.Filled.Refresh, "更换地图")
                        }
                    }
                }
            } else if (!showUrlInput) {
                EmptyStateView(
                    icon = Icons.Filled.Map,
                    title = "未设置地图地址",
                    subtitle = "选择地图类型或输入自定义URL"
                )
            }
        }
    }
}
