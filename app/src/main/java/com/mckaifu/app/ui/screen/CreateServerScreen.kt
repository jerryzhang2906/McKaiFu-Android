@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.mckaifu.app.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mckaifu.app.data.model.CoreType
import com.mckaifu.app.data.model.CoreVersion
import com.mckaifu.app.data.model.DownloadProgress
import com.mckaifu.app.data.model.ServerInstance
import com.mckaifu.app.ui.component.*
import com.mckaifu.app.ui.theme.*
import com.mckaifu.app.util.CompatibilityInfo
import com.mckaifu.app.util.CompatibilityLevel
import com.mckaifu.app.util.formatMcVersion
import com.mckaifu.app.util.getClientVersionHint
import com.mckaifu.app.util.getCompatibilityInfo
import com.mckaifu.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateServerScreen(navController: NavController, vm: MainViewModel = viewModel()) {
    var name by remember { mutableStateOf("我的服务器") }
    var coreType by remember { mutableStateOf(CoreType.PAPER) }
    var selectedMcVersion by remember { mutableStateOf("1.20.4") }
    var port by remember { mutableStateOf("25565") }
    var memoryMin by remember { mutableStateOf("512") }
    var memoryMax by remember { mutableStateOf("2048") }
    var maxPlayers by remember { mutableStateOf("20") }
    var javaVersion by remember { mutableIntStateOf(17) }
    var showCoreDropdown by remember { mutableStateOf(false) }
    var showVersionDropdown by remember { mutableStateOf(false) }
    var showGeyserOption by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var currentStep by remember { mutableIntStateOf(0) }

    val coreVersions by vm.coreVersions.collectAsState()
    val versions = coreVersions[coreType] ?: emptyList()
    val downloadProgress by vm.downloadProgress.collectAsState()

    LaunchedEffect(coreType) {
        if (coreVersions[coreType] == null) {
            vm.fetchCoreVersions(coreType)
        }
    }

    val compatibilityInfo = getCompatibilityInfo(selectedMcVersion, coreType)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("创建服务器", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showDownloadDialog = true }) {
                        Icon(Icons.Filled.CloudDownload, "下载核心")
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
            // Step indicator
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("基本信息", "配置", "确认").forEachIndexed { i, step ->
                    Box(
                        modifier = Modifier.weight(1f).height(4.dp)
                            .background(
                                if (i <= currentStep) ZalithPrimary else ZalithCardBorder,
                                MaterialTheme.shapes.extraSmall
                            )
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (currentStep) {
                    0 -> StepBasicInfo(
                        name, coreType, versions, selectedMcVersion,
                        compatibilityInfo, showCoreDropdown, showVersionDropdown,
                        onNameChange = { name = it },
                        onCoreTypeChange = { coreType = it; showGeyserOption = it.isJava() },
                        onVersionChange = { selectedMcVersion = it },
                        onCoreDropdownToggle = { showCoreDropdown = it },
                        onVersionDropdownToggle = { showVersionDropdown = it }
                    )
                    1 -> StepConfig(
                        port, maxPlayers, memoryMin, memoryMax, showGeyserOption, javaVersion,
                        coreType.isJava(),
                        onPortChange = { port = it },
                        onMaxPlayersChange = { maxPlayers = it },
                        onMemoryMinChange = { memoryMin = it },
                        onMemoryMaxChange = { memoryMax = it },
                        onGeyserChange = { showGeyserOption = it },
                        onJavaVersionChange = { javaVersion = it }
                    )
                    2 -> StepConfirm(
                        name, coreType, selectedMcVersion, port, maxPlayers,
                        memoryMin, memoryMax, showGeyserOption, javaVersion, compatibilityInfo,
                        downloadProgress,
                        onCreate = {
                            val jarFileName = MainViewModel.getDefaultJarName(coreType, selectedMcVersion)
                            val server = ServerInstance(
                                name = name.ifBlank { "我的服务器" },
                                coreType = coreType,
                                coreVersion = selectedMcVersion,
                                port = port.toIntOrNull() ?: 25565,
                                memoryMin = memoryMin.toIntOrNull() ?: 512,
                                memoryMax = memoryMax.toIntOrNull() ?: 2048,
                                maxPlayers = maxPlayers.toIntOrNull() ?: 20,
                                geyserEnabled = showGeyserOption,
                                javaVersion = javaVersion,
                                jarFileName = jarFileName,
                            )
                            vm.createServer(server)
                            val recommended = versions.find { it.isRecommended } ?: versions.firstOrNull()
                            if (recommended != null) {
                                vm.downloadCore(recommended, server.id)
                            }
                            navController.popBackStack()
                        }
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            modifier = Modifier.height(48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ZalithPrimary),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(ZalithPrimary.copy(alpha = 0.5f))
                            )
                        ) {
                            Icon(Icons.Filled.ArrowBack, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("上一步")
                        }
                    } else { Spacer(Modifier.width(1.dp)) }

                    if (currentStep < 2) {
                        Button(
                            onClick = { currentStep++ },
                            modifier = Modifier.height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ZalithPrimary)
                        ) {
                            Text("下一步")
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showDownloadDialog) {
        CoreDownloadDialog(coreType = coreType, versions = versions,
            downloadProgress = downloadProgress,
            onDismiss = { showDownloadDialog = false },
            onDownload = { version ->
                val jarFileName = MainViewModel.getDefaultJarName(coreType, version.mcVersion)
                val server = ServerInstance(
                    name = name, coreType = coreType, port = port.toIntOrNull() ?: 25565,
                    jarFileName = jarFileName
                )
                vm.createServer(server)
                vm.downloadCore(version, server.id)
                showDownloadDialog = false
                navController.popBackStack()
            }
        )
    }
}

@Composable
fun StepBasicInfo(
    name: String, coreType: CoreType, versions: List<CoreVersion>,
    selectedMcVersion: String, compatibilityInfo: CompatibilityInfo,
    showCoreDropdown: Boolean, showVersionDropdown: Boolean,
    onNameChange: (String) -> Unit, onCoreTypeChange: (CoreType) -> Unit,
    onVersionChange: (String) -> Unit,
    onCoreDropdownToggle: (Boolean) -> Unit, onVersionDropdownToggle: (Boolean) -> Unit
) {
    SectionHeader("基本信息")
    Spacer(Modifier.height(8.dp))

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text("服务器名称", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            leadingIcon = { Icon(Icons.Filled.PlayCircle, null, tint = ZalithPrimary) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = outFieldColors()
        )
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text("服务端核心", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        ExposedDropdownMenuBox(
            expanded = showCoreDropdown,
            onExpandedChange = onCoreDropdownToggle
        ) {
            OutlinedTextField(
                value = coreType.displayName,
                onValueChange = {},
                readOnly = true,
                leadingIcon = { Icon(Icons.Filled.Memory, null, tint = ZalithPrimary) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCoreDropdown) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                colors = outFieldColors()
            )
            ExposedDropdownMenu(
                expanded = showCoreDropdown,
                onDismissRequest = { onCoreDropdownToggle(false) }
            ) {
                CoreType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(type.displayName, fontWeight = FontWeight.Medium)
                                Text(type.description, style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary)
                            }
                        },
                        onClick = {
                            onCoreTypeChange(type)
                            onCoreDropdownToggle(false)
                            onVersionChange(when (type) {
                                CoreType.NUKKIT, CoreType.POCKETMINE -> "基岩版"
                                else -> "1.20.4"
                            })
                        }
                    )
                }
            }
        }
    }

    if (versions.isNotEmpty() && coreType != CoreType.CUSTOM) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("Minecraft版本", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            ExposedDropdownMenuBox(
                expanded = showVersionDropdown,
                onExpandedChange = onVersionDropdownToggle
            ) {
                OutlinedTextField(
                    value = formatMcVersion(selectedMcVersion, coreType),
                    onValueChange = {},
                    readOnly = true,
                    leadingIcon = { Icon(Icons.Filled.Tag, null, tint = ZalithPrimary) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showVersionDropdown) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    colors = outFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = showVersionDropdown,
                    onDismissRequest = { onVersionDropdownToggle(false) }
                ) {
                    versions.forEach { ver ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(formatMcVersion(ver.mcVersion, coreType))
                                    if (ver.isRecommended) {
                                        Spacer(Modifier.width(8.dp))
                                        Surface(color = ServerOnline.copy(alpha = 0.2f),
                                            shape = MaterialTheme.shapes.extraSmall) {
                                            Text("推荐", modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                                style = MaterialTheme.typography.labelSmall, color = ServerOnline)
                                        }
                                    }
                                }
                            },
                            onClick = {
                                onVersionChange(ver.mcVersion)
                                onVersionDropdownToggle(false)
                            }
                        )
                    }
                }
            }
        }
    }

    if (coreType != CoreType.CUSTOM) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = when (compatibilityInfo.level) {
                CompatibilityLevel.FULL -> ServerOnline.copy(alpha = 0.3f)
                CompatibilityLevel.PARTIAL -> ServerStarting.copy(alpha = 0.3f)
                CompatibilityLevel.INCOMPATIBLE -> ServerError.copy(alpha = 0.3f)
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    when (compatibilityInfo.level) {
                        CompatibilityLevel.FULL -> Icons.Filled.CheckCircle
                        CompatibilityLevel.PARTIAL -> Icons.Filled.Warning
                        CompatibilityLevel.INCOMPATIBLE -> Icons.Filled.Error
                    }, null,
                    tint = when (compatibilityInfo.level) {
                        CompatibilityLevel.FULL -> ServerOnline
                        CompatibilityLevel.PARTIAL -> ServerStarting
                        CompatibilityLevel.INCOMPATIBLE -> ServerError
                    },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(compatibilityInfo.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = when (compatibilityInfo.level) {
                            CompatibilityLevel.FULL -> ServerOnline
                            CompatibilityLevel.PARTIAL -> ServerStarting
                            CompatibilityLevel.INCOMPATIBLE -> ServerError
                        })
                    Text(compatibilityInfo.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary)
                    Spacer(Modifier.height(2.dp))
                    Text("客户端: ${getClientVersionHint(selectedMcVersion, coreType)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun StepConfig(
    port: String, maxPlayers: String, memoryMin: String, memoryMax: String,
    showGeyserOption: Boolean, javaVersion: Int, isJavaServer: Boolean,
    onPortChange: (String) -> Unit, onMaxPlayersChange: (String) -> Unit,
    onMemoryMinChange: (String) -> Unit, onMemoryMaxChange: (String) -> Unit,
    onGeyserChange: (Boolean) -> Unit, onJavaVersionChange: (Int) -> Unit
) {
    SectionHeader("网络配置")
    Spacer(Modifier.height(8.dp))

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("端口", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                OutlinedTextField(
                    value = port,
                    onValueChange = { onPortChange(it.filter { c -> c.isDigit() }.take(5)) },
                    leadingIcon = { Icon(Icons.Filled.Lan, null, tint = ZalithPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = outFieldColors()
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("最大玩家", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                OutlinedTextField(
                    value = maxPlayers,
                    onValueChange = { onMaxPlayersChange(it.filter { c -> c.isDigit() }.take(3)) },
                    leadingIcon = { Icon(Icons.Filled.People, null, tint = ZalithPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = outFieldColors()
                )
            }
        }
    }

    SectionHeader("内存分配")
    Spacer(Modifier.height(8.dp))

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("最小内存 (MB)", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                OutlinedTextField(
                    value = memoryMin,
                    onValueChange = { onMemoryMinChange(it.filter { c -> c.isDigit() }.take(5)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    supportingText = { Text("推荐: 512") },
                    colors = outFieldColors()
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("最大内存 (MB)", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                OutlinedTextField(
                    value = memoryMax,
                    onValueChange = { onMemoryMaxChange(it.filter { c -> c.isDigit() }.take(5)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    supportingText = { Text("推荐: 2048") },
                    colors = outFieldColors()
                )
            }
        }
    }

    if (isJavaServer) {
        SectionHeader("Java 运行时")
        Spacer(Modifier.height(8.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("Java 版本", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(17, 21, 25).forEach { v ->
                    FilterChip(
                        selected = javaVersion == v,
                        onClick = { onJavaVersionChange(v) },
                        label = { Text("Java $v") },
                        leadingIcon = if (javaVersion == v) {
                            { Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ZalithPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("现代服务器核心建议使用 Java 21+，内置运行时可在设置中下载",
                style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Spacer(Modifier.height(16.dp))
    }

    SectionHeader("跨平台支持")
    Spacer(Modifier.height(8.dp))

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Phonelink, null, tint = ZalithPrimary)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Geyser 基岩版支持", style = MaterialTheme.typography.bodyLarge)
                    Text("允许手机/主机玩家连接", style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary)
                }
            }
            Switch(
                checked = showGeyserOption,
                onCheckedChange = onGeyserChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = ZalithPrimary,
                    checkedThumbColor = Color.White
                )
            )
        }
    }
}

@Composable
fun StepConfirm(
    name: String, coreType: CoreType, selectedMcVersion: String,
    port: String, maxPlayers: String, memoryMin: String, memoryMax: String,
    showGeyserOption: Boolean, javaVersion: Int, compatibilityInfo: CompatibilityInfo,
    downloadProgress: DownloadProgress,
    onCreate: () -> Unit
) {
    SectionHeader("确认配置")
    Spacer(Modifier.height(8.dp))

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        InfoRow("服务器名称", name)
        InfoRow("服务端核心", "${coreType.displayName} $selectedMcVersion")
        InfoRow("端口", port)
        InfoRow("最大玩家", maxPlayers)
        InfoRow("内存", "$memoryMin MB ~ $memoryMax MB")
        if (coreType.isJava()) {
            InfoRow("Java 版本", "Java $javaVersion")
        }
        InfoRow("Geyser支持", if (showGeyserOption) "是" else "否")
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = when (compatibilityInfo.level) {
            CompatibilityLevel.FULL -> ServerOnline.copy(alpha = 0.3f)
            CompatibilityLevel.PARTIAL -> ServerStarting.copy(alpha = 0.3f)
            else -> ServerError.copy(alpha = 0.3f)
        }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CheckCircle, null,
                tint = when (compatibilityInfo.level) {
                    CompatibilityLevel.FULL -> ServerOnline
                    CompatibilityLevel.PARTIAL -> ServerStarting
                    else -> ServerError
                }, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text(compatibilityInfo.title, color = when (compatibilityInfo.level) {
                CompatibilityLevel.FULL -> ServerOnline
                CompatibilityLevel.PARTIAL -> ServerStarting
                else -> ServerError
            })
        }
    }

    if (downloadProgress.isDownloading) {
        DownloadProgressBar(downloadProgress)
    }

    Spacer(Modifier.height(8.dp))

    GradientButton(
        text = "创建服务器",
        icon = Icons.Filled.Check,
        onClick = onCreate,
        modifier = Modifier.fillMaxWidth(),
        loading = downloadProgress.isDownloading
    )
}

@Composable
fun outFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ZalithPrimary,
    unfocusedBorderColor = ZalithCardBorder,
    focusedContainerColor = ZalithSurfaceVariant.copy(alpha = 0.5f),
    unfocusedContainerColor = ZalithSurfaceVariant.copy(alpha = 0.3f),
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = ZalithPrimary,
    errorTextColor = ServerError,
    errorBorderColor = ServerError
)
