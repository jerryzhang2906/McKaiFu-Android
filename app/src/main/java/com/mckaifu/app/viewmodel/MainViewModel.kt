package com.mckaifu.app.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mckaifu.app.McKaiFuApp
import com.mckaifu.app.data.model.*
import com.mckaifu.app.data.repository.ServerRepository
import com.mckaifu.app.service.*
import com.mckaifu.app.util.BackupManager
import com.mckaifu.app.util.PerformanceOptimizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val app = application as McKaiFuApp
    val repository: ServerRepository = app.repository
    val engine: ServerEngine = app.serverEngine
    val downloadManager = DownloadManager()
    val fileManager = FileManagerService()
    val backupManager = BackupManager()
    val geyserService = GeyserService()
    val tunnelService = app.tunnelService
    val performanceOptimizer = PerformanceOptimizer()

    val servers: StateFlow<List<ServerInstance>> = repository.servers
    val settings: StateFlow<AppSettings> = repository.settings
    val serverStatuses: StateFlow<Map<String, ServerStatus>> = engine.serverStatuses
    val selectedServerId: StateFlow<String?> = repository.selectedServerId
    val scheduledTasks: StateFlow<Map<String, List<ScheduledTask>>> = repository.scheduledTasks
    val players: StateFlow<Map<String, List<PlayerInfo>>> = engine.players

    val coreVersions: StateFlow<Map<CoreType, List<CoreVersion>>> = downloadManager.cachedCoreVersions
    val storePlugins: StateFlow<List<PluginInfo>> = downloadManager.storePlugins
    val downloadProgress: StateFlow<DownloadProgress> = downloadManager.downloadProgress

    private val _pluginDetail = MutableStateFlow<PluginInfo?>(null)
    val pluginDetail: StateFlow<PluginInfo?> = _pluginDetail.asStateFlow()

    private val _selectedCore = MutableStateFlow<CoreVersion?>(null)
    val selectedCore: StateFlow<CoreVersion?> = _selectedCore.asStateFlow()

    private val _pluginDownloading = MutableStateFlow(false)
    val pluginDownloading: StateFlow<Boolean> = _pluginDownloading.asStateFlow()

    private val _downloadError = MutableStateFlow<String?>(null)
    val downloadError: StateFlow<String?> = _downloadError.asStateFlow()

    private val _selectedServer = MutableStateFlow<ServerInstance?>(null)
    val selectedServer: StateFlow<ServerInstance?> = _selectedServer.asStateFlow()

    fun selectServer(serverId: String) {
        repository.setSelectedServerId(serverId)
        _selectedServer.value = repository.getServer(serverId)
    }

    fun createServer(server: ServerInstance) {
        repository.addServer(server)
        if (server.geyserEnabled) {
            viewModelScope.launch(Dispatchers.IO) {
                val dir = repository.getServerDir(server)
                val ok = geyserService.setupGeyser(server, dir) &&
                        geyserService.configureGeyser(dir, server.geyserPort)
                addConsoleMessage(server.id, ConsoleMessage(
                    content = if (ok) "§aGeyser 已安装(端口 ${server.geyserPort})"
                    else "§cGeyser 安装失败,可稍后在服务器详情重新开启",
                    type = if (ok) LogType.SUCCESS else LogType.ERROR
                ))
            }
        }
    }

    fun updateServer(server: ServerInstance) {
        repository.updateServer(server)
    }

    fun deleteServer(serverId: String) {
        engine.cleanup(serverId)
        repository.removeServer(serverId)
    }

    fun startServer(serverId: String) {
        val server = repository.getServer(serverId) ?: return
        val dir = repository.getServerDir(server)
        val serverFile = File(dir, server.jarFileName)

        if (server.isCustomJar && server.customJarPath != null) {
            val customFile = File(server.customJarPath)
            if (customFile.exists()) {
                customFile.copyTo(serverFile, overwrite = true)
            }
        }

        viewModelScope.launch {
            val javaExec = if (server.coreType.isJava()) {
                withContext(Dispatchers.IO) {
                    resolveJavaExec(server)
                }
            } else null
            engine.startServer(server, dir, javaExec) { id, msg ->
                repository.addConsoleMessage(id, msg)
            }
            updateForegroundService()
        }
    }

    private fun resolveJavaExec(server: ServerInstance): String? {
        return JavaRuntimeManager.ensureRuntime(getApplication(), server.javaVersion)
            ?: run {
                val v = JavaRuntimeManager.findAvailableVersion(getApplication())
                if (v != null) {
                    JavaRuntimeManager.jreHome(getApplication(), v)
                        ?: JavaRuntimeManager.javaExecutable(getApplication(), v)
                } else null
            }
            ?: engine.findJava()
    }

    fun stopServer(serverId: String) {
        engine.stopServer(serverId) { id, msg ->
            repository.addConsoleMessage(id, msg)
        }
        updateForegroundService()
    }

    fun restartServer(serverId: String) {
        val server = repository.getServer(serverId) ?: return
        val dir = repository.getServerDir(server)
        val javaExec = if (server.coreType.isJava()) {
            resolveJavaExec(server)
        } else null
        engine.restartServer(server, dir, javaExec) { id, msg ->
            repository.addConsoleMessage(id, msg)
        }
    }

    fun getJavaRuntimeStatus(): List<JavaRuntimeManager.RuntimeStatus> {
        return JavaRuntimeManager.getStatuses(getApplication())
    }

    fun downloadJavaRuntime(version: Int, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                JavaRuntimeManager.ensureRuntime(getApplication(), version) != null
            }
            onComplete(result.getOrElse { false }, result.exceptionOrNull()?.message)
        }
    }

    fun deleteJavaRuntime(version: Int) {
        JavaRuntimeManager.deleteRuntime(getApplication(), version)
    }

    fun sendCommand(serverId: String, command: String) {
        engine.sendCommand(serverId, command)
        repository.addConsoleMessage(serverId, ConsoleMessage(
            content = "> $command",
            type = LogType.COMMAND
        ))
    }

    fun getConsoleMessages(serverId: String): StateFlow<List<ConsoleMessage>> {
        return repository.consoleMessages.map { it[serverId] ?: emptyList() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    }

    fun clearConsole(serverId: String) {
        repository.clearConsole(serverId)
    }

    fun getServerStatus(serverId: String): ServerStatus? {
        return serverStatuses.value[serverId]
    }

    fun isServerRunning(serverId: String): Boolean {
        return engine.isRunning(serverId)
    }

    fun getServerTps(serverId: String): Double {
        return engine.getTps(serverId)
    }

    fun getPlayers(serverId: String): StateFlow<List<PlayerInfo>> =
        engine.players.map { it[serverId] ?: emptyList() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun refreshPlayersViaRcon(serverId: String) {
        val config = engine.rconConfig(serverId) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            RconPlayerProvider.fetchPlayers(serverId, config) { p ->
                engine.updatePlayer(serverId, p)
            }
        }
    }

    // ── 插件管理(真实目录) ──

    fun listPlugins(serverId: String): List<PluginInfo> {
        val server = repository.getServer(serverId) ?: return emptyList()
        val pluginsDir = repository.getPluginsDir(server)
        return pluginsDir.listFiles()
            ?.filter { it.isFile && it.extension == "jar" }
            ?.map { file ->
                PluginInfo(
                    name = file.nameWithoutExtension,
                    fileName = file.name,
                    description = "",
                    isEnabled = true,
                    serverId = serverId
                )
            }
            ?: emptyList()
    }

    fun listDisabledPlugins(serverId: String): List<PluginInfo> {
        val server = repository.getServer(serverId) ?: return emptyList()
        val pluginsDir = repository.getPluginsDir(server)
        return pluginsDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".jar.disabled") }
            ?.map { file ->
                PluginInfo(
                    name = file.nameWithoutExtension.removeSuffix(".jar"),
                    fileName = file.name,
                    description = "",
                    isEnabled = false,
                    serverId = serverId
                )
            }
            ?: emptyList()
    }

    fun togglePlugin(serverId: String, fileName: String, enable: Boolean): Boolean {
        val server = repository.getServer(serverId) ?: return false
        val pluginsDir = repository.getPluginsDir(server)
        val source = if (enable) {
            File(pluginsDir, "$fileName.disabled")
        } else {
            File(pluginsDir, fileName)
        }
        val target = if (enable) {
            File(pluginsDir, fileName.removeSuffix(".disabled"))
        } else {
            File(pluginsDir, "$fileName.disabled")
        }
        if (!source.exists()) return false
        return source.renameTo(target)
    }

    fun deletePlugin(serverId: String, fileName: String): Boolean {
        val server = repository.getServer(serverId) ?: return false
        val pluginsDir = repository.getPluginsDir(server)
        return File(pluginsDir, fileName).let { if (it.exists()) it.delete() else false }
    }

    // ── 封禁列表(真实文件解析) ──

    fun getBans(serverId: String): Pair<List<BanEntry>, List<BanEntry>> {
        val server = repository.getServer(serverId) ?: return emptyList<BanEntry>() to emptyList()
        val dir = repository.getServerDir(server)
        val playersFile = File(dir, "banned-players.json")
        val ipsFile = File(dir, "banned-ips.json")
        return parseBanFile(playersFile, BanType.PLAYER) to parseBanFile(ipsFile, BanType.IP)
    }

    private fun parseBanFile(file: File, type: BanType): List<BanEntry> {
        if (!file.exists()) return emptyList()
        return try {
            val json = file.readText()
            val jsonArray = org.json.JSONArray(json)
            (0 until jsonArray.length()).mapNotNull { i ->
                val obj = jsonArray.getJSONObject(i)
                BanEntry(
                    name = obj.optString("name"),
                    uuid = obj.optString("uuid"),
                    ip = obj.optString("ip"),
                    reason = obj.optString("reason", "违规行为"),
                    source = obj.optString("source", "管理员"),
                    createdAt = obj.optString("created").toLongOrNull() ?: System.currentTimeMillis(),
                    expiresAt = obj.optString("expires").toLongOrNull(),
                    type = type
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    // ── 世界管理(真实扫描) ──

    fun listWorlds(serverId: String): List<WorldInfo> {
        val server = repository.getServer(serverId) ?: return emptyList()
        val dir = repository.getServerDir(server)
        val levelName = readServerProperty(dir, "level-name") ?: "world"

        val candidates = listOf(
            Triple("主世界", levelName, "overworld"),
            Triple("下界", "${levelName}_nether", "nether"),
            Triple("末地", "${levelName}_the_end", "end")
        )
        return candidates.mapNotNull { (display, name, id) ->
            val worldDir = File(dir, name)
            if (!worldDir.exists() || !worldDir.isDirectory) return@mapNotNull null
            WorldInfo(
                id = id,
                name = name,
                displayName = display,
                sizeBytes = worldDir.walkTopDown().filter { it.isFile }.sumOf { it.length() },
                createdAt = worldDir.lastModified()
            )
        }
    }

    private fun readServerProperty(dir: File, key: String): String? {
        val props = File(dir, "server.properties")
        if (!props.exists()) return null
        return props.readLines().firstOrNull { it.startsWith("$key=") }?.substringAfter("=")?.trim()
    }

    fun backupWorld(serverId: String): BackupEntry? {
        val server = repository.getServer(serverId) ?: return null
        val worldsDir = repository.getWorldsDir(server)
        val backupsDir = repository.getBackupsDir(server)
        return backupManager.createBackup(worldsDir, backupsDir, name = server.worldName)
    }

    fun listBackups(serverId: String): List<BackupEntry> {
        val server = repository.getServer(serverId) ?: return emptyList()
        return backupManager.listBackups(repository.getBackupsDir(server))
    }

    fun restoreBackup(serverId: String, fileName: String): Boolean {
        val server = repository.getServer(serverId) ?: return false
        val backupsDir = repository.getBackupsDir(server)
        val worldsDir = repository.getWorldsDir(server)
        val backupFile = File(backupsDir, fileName)
        return backupManager.restoreBackup(backupFile, worldsDir)
    }

    // ── 配置编辑器(真实文件) ──

    fun listConfigFiles(serverId: String): List<FileEntry> {
        val server = repository.getServer(serverId) ?: return emptyList()
        val dir = repository.getServerDir(server)
        val files = mutableListOf<FileEntry>()

        dir.listFiles()?.filter { it.isFile && fileManager.isEditableFile(it.name) }?.forEach {
            files.add(FileEntry(name = it.name, path = it.absolutePath, isDirectory = false,
                size = it.length(), lastModified = it.lastModified(), extension = it.extension))
        }

        val pluginsDir = File(dir, "plugins")
        pluginsDir.listFiles()?.filter { it.isDirectory }?.forEach { pluginDir ->
            pluginDir.listFiles()?.filter { it.isFile && fileManager.isEditableFile(it.name) }?.forEach {
                files.add(FileEntry(
                    name = "${pluginDir.name}/${it.name}",
                    path = it.absolutePath, isDirectory = false,
                    size = it.length(), lastModified = it.lastModified(), extension = it.extension))
            }
        }
        return files.sortedBy { it.name }
    }

    fun readConfigFile(path: String): String? = fileManager.readFile(path)

    fun writeConfigFile(path: String, content: String): Boolean = fileManager.writeFile(path, content)

    // ── 定时任务 ──

    fun getTasks(serverId: String): List<ScheduledTask> = repository.getTasks(serverId)

    fun saveTasks(serverId: String, tasks: List<ScheduledTask>) {
        repository.saveTasks(serverId, tasks)
    }

    fun addTask(serverId: String, task: ScheduledTask) {
        val tasks = repository.getTasks(serverId).toMutableList()
        tasks.add(task.copy(serverId = serverId))
        repository.saveTasks(serverId, tasks)
    }

    fun updateTask(serverId: String, task: ScheduledTask) {
        val tasks = repository.getTasks(serverId).toMutableList()
        val idx = tasks.indexOfFirst { it.id == task.id }
        if (idx >= 0) {
            tasks[idx] = task.copy(serverId = serverId)
            repository.saveTasks(serverId, tasks)
        }
    }

    fun deleteTask(serverId: String, taskId: String) {
        val tasks = repository.getTasks(serverId).filter { it.id != taskId }
        repository.saveTasks(serverId, tasks)
    }

    private var schedulerJob: Job? = null

    fun startScheduler() {
        if (schedulerJob?.isActive == true) return
        schedulerJob = viewModelScope.launch {
            while (true) {
                try { runScheduledTasks() } catch (_: Exception) {}
                delay(30_000)
            }
        }
    }

    private fun runScheduledTasks() {
        repository.servers.value.forEach { server ->
            if (!engine.isRunning(server.id)) return@forEach

            if (server.autoBackup && server.backupIntervalHours > 0) {
                val backupsDir = repository.getBackupsDir(server)
                val latest = backupManager.listBackups(backupsDir).firstOrNull()?.createdAt ?: 0L
                val due = System.currentTimeMillis() - latest >= server.backupIntervalHours * 3600_000L
                if (due) {
                    val entry = backupWorld(server.id)
                    addConsoleMessage(server.id, ConsoleMessage(
                        content = if (entry != null) "§a自动备份完成: ${entry.fileName}"
                        else "§c自动备份失败",
                        type = if (entry != null) LogType.SUCCESS else LogType.ERROR
                    ))
                }
            }

            repository.getTasks(server.id).forEach { task ->
                if (!task.isEnabled) return@forEach
                val lastRun = task.lastRunAt ?: 0L
                val elapsed = System.currentTimeMillis() - lastRun
                if (elapsed < task.intervalHours * 3600_000L) return@forEach
                val updated = task.copy(lastRunAt = System.currentTimeMillis())
                updateTask(server.id, updated)
                when (task.type) {
                    TaskType.RESTART -> restartServer(server.id)
                    TaskType.BACKUP -> {
                        val entry = backupWorld(server.id)
                        addConsoleMessage(server.id, ConsoleMessage(
                            content = if (entry != null) "§a定时备份完成: ${entry.fileName}"
                            else "§c定时备份失败",
                            type = if (entry != null) LogType.SUCCESS else LogType.ERROR
                        ))
                    }
                    TaskType.COMMAND -> if (task.command.isNotBlank()) {
                        sendCommand(server.id, task.command)
                    }
                    TaskType.STOP -> stopServer(server.id)
                    TaskType.START -> startServer(server.id)
                }
            }
        }
    }

    // ── Geyser ──

    fun toggleGeyser(serverId: String, enabled: Boolean) {
        val server = repository.getServer(serverId) ?: return
        updateServer(server.copy(geyserEnabled = enabled))
        if (enabled) {
            viewModelScope.launch(Dispatchers.IO) {
                val dir = repository.getServerDir(server)
                val ok = geyserService.setupGeyser(server, dir) &&
                        geyserService.configureGeyser(dir, server.geyserPort)
                addConsoleMessage(serverId, ConsoleMessage(
                    content = if (ok) "§aGeyser 已安装并配置(端口 ${server.geyserPort})"
                    else "§cGeyser 安装失败,请检查网络",
                    type = if (ok) LogType.SUCCESS else LogType.ERROR
                ))
            }
        } else {
            addConsoleMessage(serverId, ConsoleMessage(
                content = "Geyser 已关闭(重启服务器后生效)",
                type = LogType.WARN
            ))
        }
    }

    // ── 性能优化建议 ──

    fun getOptimizationSuggestions(serverId: String): List<PerformanceOptimizer.OptimizationSuggestion> {
        val server = repository.getServer(serverId) ?: return emptyList()
        return performanceOptimizer.analyzeJavaArgs(server)
    }

    fun applySuggestion(serverId: String, suggestion: PerformanceOptimizer.OptimizationSuggestion) {
        val server = repository.getServer(serverId) ?: return
        when {
            suggestion.isJavaArg -> {
                val args = server.javaArgs
                val newArg = suggestion.configValue.ifBlank {
                    when {
                        suggestion.title.contains("G1GC") -> "-XX:+UseG1GC"
                        suggestion.title.contains("ParallelRefProc") -> "-XX:+ParallelRefProcEnabled"
                        suggestion.title.contains("MaxGCPause") -> "-XX:MaxGCPauseMillis=200"
                        else -> ""
                    }
                }
                if (newArg.isNotBlank() && !args.contains(newArg)) {
                    updateServer(server.copy(javaArgs = "$args $newArg".trim()))
                }
            }
            suggestion.category == "内存分配" -> {
                updateServer(server.copy(memoryMax = 2048))
            }
        }
    }

    // ── 前台服务(保持进程存活) ──

    fun updateForegroundService() {
        val running = repository.servers.value.any { engine.isRunning(it.id) }
        if (running) {
            val intent = Intent(getApplication(), ServerService::class.java)
            getApplication<McKaiFuApp>().startForegroundServiceCompat(intent)
        } else {
            val intent = Intent(getApplication(), ServerService::class.java)
            getApplication<McKaiFuApp>().stopServiceCompat(intent)
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        repository.updateSettings(newSettings)
    }

    fun addConsoleMessage(serverId: String, message: ConsoleMessage) {
        repository.addConsoleMessage(serverId, message)
    }

    // ── Download Features ──

    fun fetchCoreVersions(coreType: CoreType) {
        downloadManager.fetchCoreVersions(coreType, viewModelScope)
    }

    fun searchPlugins(query: String) {
        downloadManager.searchPlugins(query, viewModelScope)
    }

    fun getFeaturedPlugins() {
        downloadManager.getFeaturedPlugins(viewModelScope)
    }

    fun openPluginDetail(plugin: PluginInfo, gameVersion: String?) {
        viewModelScope.launch {
            _pluginDetail.value = downloadManager.fetchPluginDetail(
                plugin.source, plugin.id, gameVersion
            ) ?: plugin
        }
    }

    fun openCoreDetail(version: CoreVersion) {
        _selectedCore.value = version
    }

    fun downloadCore(version: CoreVersion, serverId: String) {
        val server = repository.getServer(serverId) ?: return
        val dir = repository.getServerDir(server)
        val jarFileName = when (version.coreType) {
            CoreType.PAPER -> "paper-${version.mcVersion}.jar"
            CoreType.PURPUR -> "purpur-${version.mcVersion}.jar"
            CoreType.PUFFERFISH -> "pufferfish-${version.mcVersion}.jar"
            CoreType.SPIGOT -> "spigot-${version.mcVersion}.jar"
            CoreType.VANILLA -> "server.jar"
            CoreType.NUKKIT -> "nukkit.jar"
            CoreType.POCKETMINE -> "pocketmine.phar"
            CoreType.CUSTOM -> "server.jar"
        }
        val targetFile = File(dir, jarFileName)
        updateServer(server.copy(jarFileName = jarFileName))
        downloadManager.downloadFile(version.downloadUrl, targetFile, viewModelScope)
    }

    fun installPlugin(plugin: PluginInfo, serverId: String) {
        val server = repository.getServer(serverId) ?: return
        val pluginsDir = repository.getPluginsDir(server)

        viewModelScope.launch {
            _pluginDownloading.value = true
            val resolved = downloadManager.resolvePluginDownload(plugin, server.coreVersion)
            _pluginDownloading.value = false
            if (resolved == null) {
                _downloadError.value = "无法解析插件下载地址"
                return@launch
            }
            val targetFile = File(pluginsDir, resolved.fileName)
            downloadManager.downloadFile(resolved.url, targetFile, viewModelScope)
        }
    }

    companion object {
        fun getDefaultJarName(coreType: CoreType, mcVersion: String = "1.20.4"): String = when (coreType) {
            CoreType.PAPER -> "paper-$mcVersion.jar"
            CoreType.PURPUR -> "purpur-$mcVersion.jar"
            CoreType.PUFFERFISH -> "pufferfish-$mcVersion.jar"
            CoreType.SPIGOT -> "spigot-$mcVersion.jar"
            CoreType.VANILLA -> "server.jar"
            CoreType.NUKKIT -> "nukkit.jar"
            CoreType.POCKETMINE -> "pocketmine.phar"
            CoreType.CUSTOM -> "server.jar"
        }
    }
}
