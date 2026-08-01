package com.mckaifu.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mckaifu.app.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class ServerRepository(private val context: Context) {

    private val gson = Gson()
    private val serversFile = File(context.filesDir, "servers.json")
    private val settingsFile = File(context.filesDir, "settings.json")

    private val _servers = MutableStateFlow<List<ServerInstance>>(emptyList())
    val servers: StateFlow<List<ServerInstance>> = _servers.asStateFlow()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _consoleMessages = MutableStateFlow<Map<String, List<ConsoleMessage>>>(emptyMap())
    val consoleMessages: StateFlow<Map<String, List<ConsoleMessage>>> = _consoleMessages.asStateFlow()

    private val _selectedServerId = MutableStateFlow<String?>(null)
    val selectedServerId: StateFlow<String?> = _selectedServerId.asStateFlow()

    private val tasksFile = File(context.filesDir, "scheduled_tasks.json")
    private val _scheduledTasks = MutableStateFlow<Map<String, List<ScheduledTask>>>(emptyMap())
    val scheduledTasks: StateFlow<Map<String, List<ScheduledTask>>> = _scheduledTasks.asStateFlow()

    init {
        loadServers()
        loadSettings()
        loadTasks()
    }

    private fun loadTasks() {
        try {
            if (tasksFile.exists()) {
                val json = tasksFile.readText()
                val type = object : TypeToken<Map<String, List<ScheduledTask>>>() {}.type
                val map: Map<String, List<ScheduledTask>> = gson.fromJson(json, type) ?: emptyMap()
                _scheduledTasks.value = map
            }
        } catch (_: Exception) {}
    }

    private fun saveTasks() {
        try {
            tasksFile.writeText(gson.toJson(_scheduledTasks.value))
        } catch (_: Exception) {}
    }

    fun getTasks(serverId: String): List<ScheduledTask> =
        _scheduledTasks.value[serverId] ?: emptyList()

    fun saveTasks(serverId: String, tasks: List<ScheduledTask>) {
        val map = _scheduledTasks.value.toMutableMap()
        map[serverId] = tasks
        _scheduledTasks.value = map
        saveTasks()
    }

    fun setSelectedServerId(serverId: String) {
        _selectedServerId.value = serverId
    }

    private fun loadServers() {
        try {
            if (serversFile.exists()) {
                val json = serversFile.readText()
                val type = object : TypeToken<List<ServerInstance>>() {}.type
                val list: List<ServerInstance> = gson.fromJson(json, type) ?: emptyList()
                _servers.value = list
            }
        } catch (e: Exception) {
            _servers.value = emptyList()
        }
    }

    private fun saveServers() {
        try {
            serversFile.parentFile?.mkdirs()
            serversFile.writeText(gson.toJson(_servers.value))
        } catch (_: Exception) {}
    }

    private fun loadSettings() {
        try {
            if (settingsFile.exists()) {
                val json = settingsFile.readText()
                _settings.value = gson.fromJson(json, AppSettings::class.java) ?: AppSettings()
            }
        } catch (_: Exception) {}
    }

    private fun saveSettings() {
        try {
            settingsFile.parentFile?.mkdirs()
            settingsFile.writeText(gson.toJson(_settings.value))
        } catch (_: Exception) {}
    }

    fun addServer(server: ServerInstance) {
        val list = _servers.value.toMutableList()
        list.add(server)
        _servers.value = list
        saveServers()
    }

    fun updateServer(server: ServerInstance) {
        val list = _servers.value.toMutableList()
        val index = list.indexOfFirst { it.id == server.id }
        if (index >= 0) {
            list[index] = server
            _servers.value = list
            saveServers()
        }
    }

    fun removeServer(serverId: String) {
        val list = _servers.value.toMutableList()
        val server = list.find { it.id == serverId }
        list.removeAll { it.id == serverId }
        _servers.value = list
        saveServers()

        // 递归删除服务器目录(jar/世界/plugins/backups)
        if (server != null) {
            try {
                val baseDir = File(context.filesDir, "servers")
                val dir = File(baseDir, serverId)
                if (dir.exists()) dir.deleteRecursively()
            } catch (_: Exception) {}
        }
        // 清理该服务器的定时任务
        if (_scheduledTasks.value.containsKey(serverId)) {
            val tasks = _scheduledTasks.value.toMutableMap()
            tasks.remove(serverId)
            _scheduledTasks.value = tasks
            saveTasks()
        }
        // 清理控制台消息
        if (_consoleMessages.value.containsKey(serverId)) {
            val msgs = _consoleMessages.value.toMutableMap()
            msgs.remove(serverId)
            _consoleMessages.value = msgs
        }
        // 清理穿透配置
        try {
            File(context.filesDir, "tunnel_configs").listFiles()?.forEach { f ->
                if (f.name.contains(serverId)) f.delete()
            }
        } catch (_: Exception) {}
    }

    fun getServer(serverId: String): ServerInstance? {
        return _servers.value.find { it.id == serverId }
    }

    fun updateSettings(settings: AppSettings) {
        _settings.value = settings
        saveSettings()
    }

    fun addConsoleMessage(serverId: String, message: ConsoleMessage) {
        val map = _consoleMessages.value.toMutableMap()
        val messages = map.getOrDefault(serverId, emptyList()).toMutableList()
        val settings = _settings.value
        val maxLines = settings.consoleMaxLines

        if (settings.collapseRepeatLogs && messages.isNotEmpty()) {
            val last = messages.last()
            if (last.content == message.content && last.type == message.type) {
                val collapsed = last.copy(repeatCount = last.repeatCount + 1, isRepeat = true)
                messages[messages.size - 1] = collapsed
                map[serverId] = messages
                _consoleMessages.value = map
                return
            }
        }

        messages.add(message)
        if (messages.size > maxLines) {
            val excess = messages.size - maxLines
            messages.subList(0, excess).clear()
        }
        map[serverId] = messages
        _consoleMessages.value = map
    }

    fun clearConsole(serverId: String) {
        val map = _consoleMessages.value.toMutableMap()
        map[serverId] = emptyList()
        _consoleMessages.value = map
    }

    fun getServerDir(server: ServerInstance): File {
        val baseDir = File(context.filesDir, "servers")
        val dir = File(baseDir, server.id)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getWorldsDir(server: ServerInstance): File {
        val dir = File(getServerDir(server), server.worldName)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getPluginsDir(server: ServerInstance): File {
        val dir = File(getServerDir(server), "plugins")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getBackupsDir(server: ServerInstance): File {
        val dir = File(getServerDir(server), "backups")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getConfigDir(server: ServerInstance): File {
        return getServerDir(server)
    }
}
