@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.mckaifu.app.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mckaifu.app.data.model.ConsoleMessage
import com.mckaifu.app.data.model.LogType
import com.mckaifu.app.data.model.PlayerInfo
import com.mckaifu.app.ui.component.*
import com.mckaifu.app.ui.theme.*
import com.mckaifu.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(serverId: String, navController: NavController, vm: MainViewModel = viewModel()) {
    val servers by vm.servers.collectAsState()
    val server = servers.find { it.id == serverId }
    val messages by vm.getConsoleMessages(serverId).collectAsState()
    val chatMessages = messages.filter { it.type == LogType.CHAT }
    val players = vm.getPlayerInfo(serverId)

    var selectedPlayer by remember { mutableStateOf<PlayerInfo?>(null) }
    var messageText by remember { mutableStateOf("") }
    var showPlayerSelector by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("聊天 - ${server?.name ?: ""}", fontWeight = FontWeight.Bold)
                        if (selectedPlayer != null) {
                            Text("私信: ${selectedPlayer!!.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ZalithPrimary)
                        } else {
                            Text("全服聊天",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showPlayerSelector = !showPlayerSelector }) {
                        Icon(Icons.Filled.PersonSearch, "选择私信对象",
                            tint = if (selectedPlayer != null) ZalithPrimary else TextSecondary)
                    }
                    if (selectedPlayer != null) {
                        IconButton(onClick = { selectedPlayer = null }) {
                            Icon(Icons.Filled.Close, "取消私信")
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(color = ZalithSurface) {
                Column {
                    if (showPlayerSelector) {
                        PlayerSelectorRow(
                            players = players,
                            selectedPlayer = selectedPlayer,
                            onSelect = { selectedPlayer = it; showPlayerSelector = false },
                            onDismiss = { showPlayerSelector = false }
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
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    if (selectedPlayer != null) "私信 ${selectedPlayer!!.name}..."
                                    else "发送全服消息...",
                                    color = TextSecondary
                                )
                            },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ZalithPrimary,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = ZalithPrimary,
                                focusedContainerColor = ZalithSurfaceVariant,
                                unfocusedContainerColor = ZalithSurfaceVariant
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (messageText.isNotBlank()) {
                                    val cmd = if (selectedPlayer != null)
                                        "tell ${selectedPlayer!!.name} $messageText"
                                    else
                                        "say $messageText"
                                    vm.sendCommand(serverId, cmd)
                                    vm.addConsoleMessage(serverId, ConsoleMessage(
                                        content = "[${if (selectedPlayer != null) "私信" else "广播"}] $messageText",
                                        type = LogType.CHAT
                                    ))
                                    messageText = ""
                                }
                            })
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = {
                                if (messageText.isNotBlank()) {
                                    val cmd = if (selectedPlayer != null)
                                        "tell ${selectedPlayer!!.name} $messageText"
                                    else
                                        "say $messageText"
                                    vm.sendCommand(serverId, cmd)
                                    vm.addConsoleMessage(serverId, ConsoleMessage(
                                        content = "[${if (selectedPlayer != null) "私信" else "广播"}] $messageText",
                                        type = LogType.CHAT
                                    ))
                                    messageText = ""
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
                .background(ZalithBackground)
        ) {
            if (chatMessages.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Filled.Chat,
                    title = "暂无聊天消息",
                    subtitle = "服务器聊天内容将显示在此，支持全服广播和私信"
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(chatMessages, key = { "${it.timestamp}_${it.hashCode()}" }) { msg ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = ZalithCardBorder.copy(alpha = 0.3f)
                        ) {
                            Text(
                                msg.content,
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerSelectorRow(
    players: List<PlayerInfo>,
    selectedPlayer: PlayerInfo?,
    onSelect: (PlayerInfo) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(color = ZalithSurfaceVariant) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("选择私信对象", style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary, fontWeight = FontWeight.Bold)
                TextButton(onClick = onDismiss) {
                    Text("取消", color = TextSecondary)
                }
            }
            if (players.isEmpty()) {
                Text("没有在线玩家", color = TextSecondary,
                    modifier = Modifier.padding(8.dp))
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    players.forEach { player ->
                        FilterChip(
                            selected = player == selectedPlayer,
                            onClick = { onSelect(player) },
                            label = { Text(player.name) },
                            leadingIcon = {
                                Icon(Icons.Filled.Person, null,
                                    modifier = Modifier.size(16.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ZalithPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = ZalithPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}
