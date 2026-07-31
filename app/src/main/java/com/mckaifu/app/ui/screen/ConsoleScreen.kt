@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.mckaifu.app.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mckaifu.app.data.model.ConsoleMessage
import com.mckaifu.app.data.model.LogType
import com.mckaifu.app.ui.component.*
import com.mckaifu.app.ui.theme.*
import com.mckaifu.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsoleScreen(serverId: String, navController: NavController, vm: MainViewModel = viewModel()) {
    val servers by vm.servers.collectAsState()
    val server = servers.find { it.id == serverId }
    val messages by vm.getConsoleMessages(serverId).collectAsState()
    val listState = rememberLazyListState()
    var commandText by remember { mutableStateOf("") }
    var autoScroll by remember { mutableStateOf(true) }
    var showFilter by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var filterTypes by remember { mutableStateOf(setOf<LogType>()) }

    LaunchedEffect(messages.size, autoScroll) {
        if (autoScroll && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(server?.name ?: "控制台", fontWeight = FontWeight.Bold)
                        Text("${messages.size} 条日志",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary)
                    }
                },
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
                    IconButton(onClick = { autoScroll = !autoScroll }) {
                        Icon(
                            if (autoScroll) Icons.Filled.VerticalAlignBottom else Icons.Filled.VerticalAlignCenter,
                            if (autoScroll) "自动滚动开" else "自动滚动关",
                            tint = if (autoScroll) ZalithPrimary else TextSecondary
                        )
                    }
                    IconButton(onClick = { vm.clearConsole(serverId) }) {
                        Icon(Icons.Filled.DeleteSweep, "清空控制台")
                    }
                    IconButton(onClick = { showFilter = !showFilter }) {
                        Icon(Icons.Filled.FilterList, "过滤",
                            tint = if (showFilter) ZalithPrimary else TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ConsoleBg)
            )
        },
        bottomBar = {
            Surface(color = ConsoleBg, tonalElevation = 0.dp) {
                Column {
                    if (showSearch) {
                        SearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onClose = { showSearch = false; searchQuery = "" }
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .navigationBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = commandText,
                            onValueChange = { commandText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("输入命令...", color = TextSecondary) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ZalithPrimary,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = ZalithPrimary,
                                focusedContainerColor = ZalithSurfaceVariant,
                                unfocusedContainerColor = ZalithSurfaceVariant
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (commandText.isNotBlank()) {
                                        vm.sendCommand(serverId, commandText)
                                        commandText = ""
                                    }
                                }
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = {
                                if (commandText.isNotBlank()) {
                                    vm.sendCommand(serverId, commandText)
                                    commandText = ""
                                }
                            },
                            modifier = Modifier.size(48.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = ZalithPrimary
                            )
                        ) {
                            Icon(Icons.Filled.Send, "发送")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(ConsoleBg)
        ) {
            if (showFilter) {
                FilterBar(
                    selectedTypes = filterTypes,
                    onToggle = { type ->
                        filterTypes = if (type in filterTypes)
                            filterTypes - type else filterTypes + type
                    }
                )
            }

            val filteredMessages = messages
                .let { if (filterTypes.isEmpty()) it else it.filter { m -> m.type in filterTypes } }
                .let { if (searchQuery.isBlank()) it else it.filter { m -> m.content.contains(searchQuery, ignoreCase = true) } }

            if (filteredMessages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Terminal, null,
                            modifier = Modifier.size(48.dp),
                            tint = TextSecondary.copy(alpha = 0.5f))
                        Spacer(Modifier.height(8.dp))
                        Text("控制台为空", color = TextSecondary)
                        Text("启动服务器后将在此显示日志",
                            color = TextSecondary.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(filteredMessages, key = { "${it.timestamp}_${it.hashCode()}" }) { msg ->
                        ConsoleLine(msg)
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    Surface(color = ZalithSurfaceVariant) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("搜索日志...", color = TextSecondary) },
                textStyle = MaterialTheme.typography.bodySmall.copy(color = TextPrimary),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = ZalithPrimary
                ),
                singleLine = true
            )
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, null, tint = TextSecondary)
                }
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, null, tint = TextSecondary)
            }
        }
    }
}

@Composable
fun ConsoleLine(msg: ConsoleMessage) {
    val color = when (msg.type) {
        LogType.INFO -> LogInfo
        LogType.WARN -> LogWarn
        LogType.ERROR -> LogError
        LogType.SUCCESS -> LogSuccess
        LogType.DEBUG -> LogDebug
        LogType.CHAT -> LogChat
        LogType.COMMAND -> LogCommand
        LogType.SYSTEM -> LogSystem
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
    ) {
        if (msg.isRepeat) {
            Text(
                "[x${msg.repeatCount}] ",
                color = LogWarn,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }
        Text(
            text = msg.content,
            color = color,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterBar(
    selectedTypes: Set<LogType>,
    onToggle: (LogType) -> Unit
) {
    Surface(color = ZalithSurfaceVariant) {
        FlowRow(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                selected = selectedTypes.isEmpty(),
                onClick = {
                    LogType.entries.forEach { if (it in selectedTypes) onToggle(it) }
                },
                label = { Text("全部", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ZalithPrimary.copy(alpha = 0.2f),
                    selectedLabelColor = ZalithPrimary
                )
            )
            LogType.entries.forEach { type ->
                FilterChip(
                    selected = type in selectedTypes,
                    onClick = { onToggle(type) },
                    label = { Text(type.displayName, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ZalithPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = ZalithPrimary
                    )
                )
            }
        }
    }
}
