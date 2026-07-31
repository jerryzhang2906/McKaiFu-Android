package com.mckaifu.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mckaifu.app.McKaiFuApp
import com.mckaifu.app.data.model.*
import com.mckaifu.app.data.repository.ServerRepository
import com.mckaifu.app.service.DownloadManager
import com.mckaifu.app.service.JavaRuntimeManager
import com.mckaifu.app.service.ServerEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val app = application as McKaiFuApp
    val repository: ServerRepository = app.repository
    val engine: ServerEngine = app.serverEngine
    val downloadManager = DownloadManager()

    val servers: StateFlow<List<ServerInstance>> = repository.servers
    val settings: StateFlow<AppSettings> = repository.settings
    val serverStatuses: StateFlow<Map<String, ServerStatus>> = engine.serverStatuses
    val selectedServerId: StateFlow<String?> = repository.selectedServerId

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

    fun getPlayerInfo(serverId: String): List<PlayerInfo> {
        return if (isServerRunning(serverId)) {
            listOf(
                PlayerInfo(name = "Steve", uuid = "00000000-0000-0000-0000-000000000001",
                    world = "world", health = 20.0, hunger = 20, ping = 45, serverId = serverId)
            )
        } else emptyList()
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
