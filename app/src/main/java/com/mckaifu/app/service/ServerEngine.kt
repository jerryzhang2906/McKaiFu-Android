package com.mckaifu.app.service

import com.mckaifu.app.data.model.ConsoleMessage
import com.mckaifu.app.data.model.LogType
import com.mckaifu.app.data.model.PlayerInfo
import com.mckaifu.app.data.model.ServerInstance
import com.mckaifu.app.data.model.ServerStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.*
import java.util.concurrent.ConcurrentHashMap

class ServerEngine {

    private val processes = ConcurrentHashMap<String, Process>()
    private val bionicThreads = ConcurrentHashMap<String, Thread>()
    private val childPids = ConcurrentHashMap<String, Int>()
    private val stdinFds = ConcurrentHashMap<String, Int>()
    private val tpsTrackers = ConcurrentHashMap<String, TpsTracker>()
    private val jobs = ConcurrentHashMap<String, Job>()
    private val rconConfigs = ConcurrentHashMap<String, RconPlayerProvider.RconConfig>()

    private val _serverStatuses = MutableStateFlow<Map<String, ServerStatus>>(emptyMap())
    val serverStatuses: StateFlow<Map<String, ServerStatus>> = _serverStatuses.asStateFlow()

    private val _players = MutableStateFlow<Map<String, List<PlayerInfo>>>(emptyMap())
    val players: StateFlow<Map<String, List<PlayerInfo>>> = _players.asStateFlow()

    val consoleOutput = MutableStateFlow<Map<String, List<ConsoleMessage>>>(emptyMap())

    data class TpsTracker(
        var lastTickTime: Long = System.nanoTime(),
        var tickTimes: LongArray = LongArray(100),
        var tickIndex: Int = 0,
        var tps: Double = 20.0,
    )

    fun startServer(server: ServerInstance, serverDir: File, javaExec: String?, onMessage: (String, ConsoleMessage) -> Unit) {
        if (isRunning(server.id)) return

        updateStatus(server.id, ServerStatus.STARTING)

        val java = javaExec ?: findJava()
        if (java == null) {
            onMessage(server.id, ConsoleMessage(
                content = "未找到Java ${server.javaVersion} 运行时！请在设置中检查内置Java运行时，或安装Java 17+",
                type = LogType.ERROR
            ))
            updateStatus(server.id, ServerStatus.ERROR)
            return
        }

        try {
            val jarFile = if (server.isCustomJar && server.customJarPath != null) {
                File(server.customJarPath)
            } else {
                File(serverDir, server.jarFileName)
            }

            if (!jarFile.exists()) {
                onMessage(server.id, ConsoleMessage(
                    content = "未找到服务端JAR文件: ${jarFile.absolutePath}",
                    type = LogType.ERROR
                ))
                updateStatus(server.id, ServerStatus.ERROR)
                return
            }

            ensureEulaAccepted(serverDir)
            rconConfigs[server.id] = RconPlayerProvider.ensureRcon(serverDir)

            if (isBionicJre(java)) {
                startServerBionic(server, serverDir, java, jarFile, onMessage)
            } else {
                startServerProcess(server, serverDir, java, jarFile, onMessage)
            }
        } catch (e: Exception) {
            onMessage(server.id, ConsoleMessage(
                content = "启动失败: ${e.message}",
                type = LogType.ERROR
            ))
            updateStatus(server.id, ServerStatus.ERROR)
        }
    }

    private fun isBionicJre(javaExec: String): Boolean {
        return File(javaExec, "lib/libjli.so").exists() || File(javaExec, "lib/server/libjvm.so").exists()
    }

    private fun ensureEulaAccepted(serverDir: File) {
        val eula = File(serverDir, "eula.txt")
        val desired = "eula=true\n"
        val current = if (eula.exists()) eula.readText() else null
        if (current != desired) eula.writeText(desired)
    }

    private fun startServerBionic(
        server: ServerInstance,
        serverDir: File,
        jreHome: String,
        jarFile: File,
        onMessage: (String, ConsoleMessage) -> Unit
    ) {
        if (!loadJreLibraries(jreHome)) {
            onMessage(server.id, ConsoleMessage(
                content = "加载内置Java运行时失败(JRE库缺失或损坏)",
                type = LogType.ERROR
            ))
            updateStatus(server.id, ServerStatus.ERROR)
            return
        }

        val outPipe = VMLauncher.createPipe() ?: run {
            onMessage(server.id, ConsoleMessage(content = "创建输出管道失败", type = LogType.ERROR))
            updateStatus(server.id, ServerStatus.ERROR)
            return
        }
        val inPipe = VMLauncher.createPipe() ?: run {
            onMessage(server.id, ConsoleMessage(content = "创建输入管道失败", type = LogType.ERROR))
            updateStatus(server.id, ServerStatus.ERROR)
            return
        }
        val outFd = outPipe[0]
        val outWriteFd = outPipe[1]
        val inReadFd = inPipe[0]
        val inFd = inPipe[1]

        val tmpDir = File(serverDir, "tmp").apply { mkdirs() }

        val args = mutableListOf(
            "java",
            "-Xms${server.memoryMin}M",
            "-Xmx${server.memoryMax}M",
        )
        server.javaArgs.split(" ").filter { it.isNotBlank() }.let { args.addAll(it) }
        args.add("-Djava.io.tmpdir=${tmpDir.absolutePath}")
        args.add("-DPaper.IgnoreJavaVersion=true")
        args.addAll(listOf("-jar", jarFile.absolutePath, "--nogui"))

        onMessage(server.id, ConsoleMessage(
            content = "§a服务端启动中... 内存: ${server.memoryMin}-${server.memoryMax}MB",
            type = LogType.SUCCESS
        ))

        val handleLine: (String) -> Unit = { rawLine ->
            val logType = detectLogType(rawLine)
            val cleanLine = cleanLogLine(rawLine)
            if (cleanLine.isNotEmpty()) {
                android.util.Log.d("mcserver", cleanLine)
                onMessage(server.id, ConsoleMessage(content = cleanLine, type = logType))
                if (cleanLine.contains("Done (", ignoreCase = true) ||
                    cleanLine.contains("Server started", ignoreCase = true)) {
                    updateStatus(server.id, ServerStatus.ONLINE)
                    onMessage(server.id, ConsoleMessage(
                        content = "§a✓ 服务器已成功启动！端口: ${server.port}",
                        type = LogType.SUCCESS
                    ))
                }
                updateTps(server.id, cleanLine)
                trackPlayers(server.id, cleanLine)
            }
        }

        VMLauncher.chdir(serverDir.absolutePath)

        val pid = VMLauncher.launchJvmChild(args.toTypedArray(), inReadFd, outWriteFd)
        if (pid <= 0) {
            android.util.Log.e("mcserver", "launchJvmChild failed pid=$pid")
            onMessage(server.id, ConsoleMessage(
                content = "JVM 子进程启动失败(fork 失败)",
                type = LogType.ERROR
            ))
            updateStatus(server.id, ServerStatus.ERROR)
            try { VMLauncher.closeFd(outWriteFd) } catch (_: Exception) {}
            try { VMLauncher.closeFd(inFd) } catch (_: Exception) {}
            try { VMLauncher.closeFd(outFd) } catch (_: Exception) {}
            try { VMLauncher.closeFd(inReadFd) } catch (_: Exception) {}
            return
        }
        android.util.Log.e("mcserver", "forked JVM child pid=$pid")
        stdinFds[server.id] = inFd
        childPids[server.id] = pid

        val tpsTracker = TpsTracker()
        tpsTrackers[server.id] = tpsTracker

        val job = CoroutineScope(Dispatchers.IO + CoroutineName("server-reader-${server.id}")).launch {
            val buf = ByteArray(64 * 1024)
            val sb = StringBuilder()
            android.util.Log.e("mcserver", "reader start, outFd=$outFd")
            try {
                while (isActive) {
                    val n = VMLauncher.readFd(outFd, buf, 0, buf.size)
                    if (n <= 0) break
                    android.util.Log.d("mcserver", "read ${n}b")
                    sb.append(String(buf, 0, n, Charsets.UTF_8))
                    var idx: Int
                    while (sb.indexOf("\n").also { idx = it } >= 0) {
                        val line = sb.substring(0, idx).trimEnd('\r')
                        sb.delete(0, idx + 1)
                        handleLine(line)
                    }
                }
                if (sb.isNotEmpty()) handleLine(sb.toString().trimEnd('\r'))
                android.util.Log.e("mcserver", "reader EOF")
            } catch (e: Exception) {
                android.util.Log.e("mcserver", "reader error: ${e.message}")
                if (isActive) {
                    onMessage(server.id, ConsoleMessage(
                        content = "读取服务器输出失败: ${e.message}",
                        type = LogType.ERROR
                    ))
                }
            } finally {
                try { VMLauncher.closeFd(outFd) } catch (_: Exception) {}
            }
        }
        jobs[server.id] = job

        launchWatchdogBionic(server.id, onMessage)
    }

    private fun loadJreLibraries(jreHome: String): Boolean {
        val libDir = File(jreHome, "lib")
        if (!libDir.exists()) return false

        val priority = listOf(
            "libjli.so",
            "server/libjvm.so",
            "libverify.so",
            "libjava.so",
            "libnet.so",
            "libnio.so",
            "libzip.so",
            "libjimage.so",
            "libfreetype.so",
            "libfontmanager.so",
        )

        val all = mutableListOf<File>()
        libDir.walkTopDown().forEach { f ->
            if (f.isFile && f.name.endsWith(".so")) all.add(f)
        }

        val sorted = all.sortedWith(compareBy({ f ->
            val i = priority.indexOfFirst { f.absolutePath.endsWith(it) }
            if (i < 0) 999 else i
        }, { it.absolutePath }))

        var criticalOk = true
        for (f in sorted) {
            if (!VMLauncher.dlopen(f.absolutePath, true)) {
                if (f.absolutePath.endsWith("libjli.so") || f.absolutePath.endsWith("libjvm.so")) {
                    criticalOk = false
                }
            }
        }
        return criticalOk
    }

    private fun launchWatchdogBionic(serverId: String, onMessage: (String, ConsoleMessage) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val pid = childPids[serverId]
                while (isActive && pid != null && VMLauncher.isProcessAlive(pid)) {
                    delay(1000)
                }
                if (_serverStatuses.value[serverId] != ServerStatus.STOPPING &&
                    _serverStatuses.value[serverId] != ServerStatus.RESTARTING
                ) {
                    onMessage(serverId, ConsoleMessage(
                        content = "§c服务器进程异常退出",
                        type = LogType.ERROR
                    ))
                    updateStatus(serverId, ServerStatus.ERROR)
                }
            } catch (_: Exception) {
            } finally {
                childPids.remove(serverId)
                stdinFds.remove(serverId)?.let { fd ->
                    try { VMLauncher.closeFd(fd) } catch (_: Exception) {}
                }
                jobs[serverId]?.cancel()
                tpsTrackers.remove(serverId)
                clearPlayers(serverId)
                if (_serverStatuses.value[serverId] == ServerStatus.STOPPING ||
                    _serverStatuses.value[serverId] == ServerStatus.RESTARTING
                ) {
                    updateStatus(serverId, ServerStatus.OFFLINE)
                }
            }
        }
    }

    private fun startServerProcess(
        server: ServerInstance,
        serverDir: File,
        java: String,
        jarFile: File,
        onMessage: (String, ConsoleMessage) -> Unit
    ) {
        val commands = mutableListOf(
            java,
            "-Xms${server.memoryMin}M",
            "-Xmx${server.memoryMax}M",
        )
        server.javaArgs.split(" ").filter { it.isNotBlank() }.let { commands.addAll(it) }
        commands.addAll(listOf("-jar", jarFile.absolutePath, "--nogui"))

        val pb = ProcessBuilder(commands)
            .directory(serverDir)
            .redirectErrorStream(true)

        val env = pb.environment()
        env["JAVA_HOME"] = File(java).parentFile?.parentFile?.absolutePath ?: ""

        val process = pb.start()
        processes[server.id] = process

        onMessage(server.id, ConsoleMessage(
            content = "§a服务端启动中... 内存: ${server.memoryMin}-${server.memoryMax}MB",
            type = LogType.SUCCESS
        ))

        val tpsTracker = TpsTracker()
        tpsTrackers[server.id] = tpsTracker

        val job = CoroutineScope(Dispatchers.IO + CoroutineName("server-${server.id}")).launch {
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            var lastLine: String? = null
            var repeatCount = 0

            while (isActive) {
                try {
                    line = reader.readLine()
                    if (line == null) break

                    val logType = detectLogType(line)
                    val cleanLine = cleanLogLine(line)

                    if (cleanLine == lastLine) {
                        repeatCount++
                    } else {
                        if (repeatCount > 0 && lastLine != null) {
                            onMessage(server.id, ConsoleMessage(
                                content = "↑ 上一条信息重复 ${repeatCount + 1} 次",
                                type = LogType.SYSTEM,
                                isRepeat = true,
                                repeatCount = repeatCount + 1
                            ))
                        }
                        repeatCount = 0
                        onMessage(server.id, ConsoleMessage(
                            content = cleanLine,
                            type = logType
                        ))
                    }
                    lastLine = cleanLine

                    if (cleanLine.contains("Done (", ignoreCase = true) ||
                        cleanLine.contains("Server started", ignoreCase = true)) {
                        updateStatus(server.id, ServerStatus.ONLINE)
                        onMessage(server.id, ConsoleMessage(
                            content = "§a✓ 服务器已成功启动！端口: ${server.port}",
                            type = LogType.SUCCESS
                        ))
                    }

                    updateTps(server.id, cleanLine)
                    trackPlayers(server.id, cleanLine)

                } catch (e: IOException) {
                    break
                }
            }
        }
        jobs[server.id] = job

        launchWatchdog(server.id, process, onMessage)
    }

    fun stopServer(serverId: String, onMessage: (String, ConsoleMessage) -> Unit) {
        if (!isRunning(serverId)) {
            cleanupServer(serverId)
            updateStatus(serverId, ServerStatus.OFFLINE)
            return
        }
        updateStatus(serverId, ServerStatus.STOPPING)
        onMessage(serverId, ConsoleMessage(content = "正在停止服务器...", type = LogType.WARN))

        stdinFds[serverId]?.let { fd ->
            try {
                val data = "stop\n".toByteArray(Charsets.UTF_8)
                var off = 0
                while (off < data.size) {
                    val n = VMLauncher.writeFd(fd, data, off, data.size - off)
                    if (n <= 0) break
                    off += n
                }
            } catch (_: Exception) {}
        }

        processes[serverId]?.let { process ->
            try {
                val writer = BufferedWriter(OutputStreamWriter(process.outputStream))
                writer.write("stop\n")
                writer.flush()
            } catch (_: Exception) {}
        }

        GlobalScope.launch(Dispatchers.IO) {
            val pid = childPids[serverId]
            val process = processes[serverId]
            var waited = 0
            while (waited < 15000 &&
                ((pid != null && VMLauncher.isProcessAlive(pid)) || (process != null && process.isAlive))
            ) {
                delay(500)
                waited += 500
            }
            if (pid != null && VMLauncher.isProcessAlive(pid)) {
                VMLauncher.killProcess(pid, 9)
                onMessage(serverId, ConsoleMessage(content = "服务器进程已强制终止", type = LogType.ERROR))
            }
            if (process != null && process.isAlive) {
                process.destroyForcibly()
                onMessage(serverId, ConsoleMessage(content = "服务器已强制停止", type = LogType.ERROR))
            }
            cleanupServer(serverId)
            updateStatus(serverId, ServerStatus.OFFLINE)
            onMessage(serverId, ConsoleMessage(content = "§c服务器已停止", type = LogType.WARN))
        }
    }

    private fun cleanupServer(serverId: String) {
        bionicThreads.remove(serverId)
        childPids.remove(serverId)
        stdinFds.remove(serverId)?.let { fd ->
            try { VMLauncher.closeFd(fd) } catch (_: Exception) {}
        }
        jobs[serverId]?.cancel()
        processes.remove(serverId)?.let { p ->
            if (p.isAlive) p.destroyForcibly()
        }
        tpsTrackers.remove(serverId)
        rconConfigs.remove(serverId)
        clearPlayers(serverId)
    }

    fun restartServer(server: ServerInstance, serverDir: File, javaExec: String?, onMessage: (String, ConsoleMessage) -> Unit) {
        updateStatus(server.id, ServerStatus.RESTARTING)
        onMessage(server.id, ConsoleMessage(content = "正在重启服务器...", type = LogType.SYSTEM))
        stopServer(server.id, onMessage)

        GlobalScope.launch(Dispatchers.IO) {
            var waited = 0
            while (isRunning(server.id) && waited < 30000) {
                delay(500)
                waited += 500
            }
            startServer(server, serverDir, javaExec, onMessage)
        }
    }

    fun sendCommand(serverId: String, command: String) {
        stdinFds[serverId]?.let { fd ->
            try {
                val data = "$command\n".toByteArray(Charsets.UTF_8)
                var off = 0
                while (off < data.size) {
                    val n = VMLauncher.writeFd(fd, data, off, data.size - off)
                    if (n <= 0) break
                    off += n
                }
            } catch (_: Exception) {}
        }

        processes[serverId]?.let { process ->
            try {
                val writer = BufferedWriter(OutputStreamWriter(process.outputStream))
                writer.write("$command\n")
                writer.flush()
            } catch (_: Exception) {}
        }
    }

    fun isRunning(serverId: String): Boolean {
        return childPids[serverId]?.let { VMLauncher.isProcessAlive(it) } == true ||
            processes[serverId]?.isAlive == true
    }

    fun getTps(serverId: String): Double {
        return tpsTrackers[serverId]?.tps ?: 0.0
    }

    fun getProcess(serverId: String): Process? = processes[serverId]

    fun findJava(): String? {
        val javaHome = System.getenv("JAVA_HOME")
        if (javaHome != null) {
            val javaBin = File(javaHome, "bin/java")
            if (javaBin.exists()) return javaBin.absolutePath
            val javaExe = File(javaHome, "bin/java.exe")
            if (javaExe.exists()) return javaExe.absolutePath
        }

        val path = System.getenv("PATH") ?: ""
        val pathDirs = path.split(File.pathSeparator)
        for (dir in pathDirs) {
            val javaFile = File(dir, "java")
            if (javaFile.exists()) return javaFile.absolutePath
            val javaExe = File(dir, "java.exe")
            if (javaExe.exists()) return javaExe.absolutePath
        }

        return null
    }

    private fun updateStatus(serverId: String, status: ServerStatus) {
        val map = _serverStatuses.value.toMutableMap()
        map[serverId] = status
        _serverStatuses.value = map
    }

    private fun detectLogType(line: String): LogType {
        return when {
            line.startsWith("[") && line.contains("WARN") -> LogType.WARN
            line.startsWith("[") && line.contains("ERROR") -> LogType.ERROR
            line.startsWith("[") && line.contains("FATAL") -> LogType.ERROR
            line.startsWith("[") && line.contains("INFO") -> LogType.INFO
            line.startsWith("[") && line.contains("DEBUG") -> LogType.DEBUG
            line.contains("§c") || line.contains("[CRITICAL]") -> LogType.ERROR
            line.contains("§a") || line.contains("§e[") && line.contains("Done") -> LogType.SUCCESS
            line.contains("<") && line.contains(">") -> LogType.CHAT
            line.startsWith(">") || line.startsWith("/") -> LogType.COMMAND
            else -> LogType.INFO
        }
    }

    private fun cleanLogLine(line: String): String {
        return line
            .replace(Regex("§[0-9a-fklmnor]"), "")
            .replace(Regex("\\u001B\\[[;\\d]*m"), "")
            .trim()
    }

    private val uuidOfPlayer = Regex("UUID of player (\\S+) is ([0-9a-fA-F-]{36})")
    private val joinedGame = Regex("(\\S+) joined the game")
    private val leftGame = Regex("(\\S+) left the game")
    private val lostConnection = Regex("(\\S+) lost connection")
    private val playersOnline = Regex("There are (\\d+) of a max of (\\d+) players online: ?(.*)")

    private fun trackPlayers(serverId: String, line: String) {
        val current = _players.value[serverId]?.toMutableList() ?: mutableListOf()
        var changed = false

        uuidOfPlayer.find(line)?.let { m ->
            val name = m.groupValues[1]
            val idx = current.indexOfFirst { it.name == name }
            if (idx >= 0) {
                current[idx] = current[idx].copy(uuid = m.groupValues[2])
                changed = true
            }
        }

        joinedGame.find(line)?.let { m ->
            val name = m.groupValues[1]
            if (name.startsWith("[")) return
            if (current.none { it.name == name }) {
                current.add(PlayerInfo(name = name, uuid = "", world = "world",
                    health = 20.0, hunger = 20, ping = 0, serverId = serverId))
                changed = true
            }
        }

        leftGame.find(line)?.let { m ->
            changed = current.removeAll { it.name == m.groupValues[1] } || changed
        }

        lostConnection.find(line)?.let { m ->
            changed = current.removeAll { it.name == m.groupValues[1] } || changed
        }

        playersOnline.find(line)?.let { m ->
            val names = m.groupValues[3]
            val newList = if (names.isBlank()) emptyList() else names.split(", ").map { name ->
                current.find { it.name == name } ?: PlayerInfo(name = name, serverId = serverId)
            }
            if (newList.map { it.name } != current.map { it.name }) {
                val map = _players.value.toMutableMap()
                map[serverId] = newList
                _players.value = map
            }
            return
        }

        if (changed) {
            val map = _players.value.toMutableMap()
            map[serverId] = current
            _players.value = map
        }
    }

    private fun clearPlayers(serverId: String) {
        if (_players.value.containsKey(serverId)) {
            val map = _players.value.toMutableMap()
            map[serverId] = emptyList()
            _players.value = map
        }
    }

    fun rconConfig(serverId: String): RconPlayerProvider.RconConfig? = rconConfigs[serverId]

    fun updatePlayer(serverId: String, updated: PlayerInfo) {
        val current = _players.value[serverId] ?: emptyList()
        val names = current.map { it.name }.toMutableList()
        val map = _players.value.toMutableMap()
        map[serverId] = if (updated.name in names) {
            current.map { if (it.name == updated.name) updated else it }
        } else {
            current + updated
        }
        _players.value = map
    }

    private fun updateTps(serverId: String, line: String) {
        val tracker = tpsTrackers[serverId] ?: return
        val now = System.nanoTime()
        val diff = now - tracker.lastTickTime

        if (diff > 0) {
            tracker.tickTimes[tracker.tickIndex % 100] = diff
            tracker.tickIndex++
            if (tracker.tickIndex % 20 == 0) {
                val recent = tracker.tickTimes.take(20)
                val avg = recent.average()
                tracker.tps = if (avg > 0) 1_000_000_000.0 / avg else 20.0
                if (tracker.tps > 20.0) tracker.tps = 20.0
            }
        }
        tracker.lastTickTime = now
    }

    private fun launchWatchdog(serverId: String, process: Process, onMessage: (String, ConsoleMessage) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                process.waitFor()
                if (_serverStatuses.value[serverId] != ServerStatus.STOPPING) {
                    onMessage(serverId, ConsoleMessage(
                        content = "§c服务器进程异常退出，退出码: ${process.exitValue()}",
                        type = LogType.ERROR
                    ))
                    updateStatus(serverId, ServerStatus.ERROR)
                }
                processes.remove(serverId)
                jobs[serverId]?.cancel()
                tpsTrackers.remove(serverId)
                clearPlayers(serverId)
            } catch (_: InterruptedException) {}
        }
    }

    fun cleanup(serverId: String) {
        jobs[serverId]?.cancel()
        bionicThreads.remove(serverId)
        stdinFds.remove(serverId)?.let { fd -> try { VMLauncher.closeFd(fd) } catch (_: Exception) {} }
        processes.remove(serverId)
        tpsTrackers.remove(serverId)
        updateStatus(serverId, ServerStatus.OFFLINE)
    }

    fun shutdownAll() {
        processes.forEach { (id, process) ->
            try {
                val writer = BufferedWriter(OutputStreamWriter(process.outputStream))
                writer.write("stop\n")
                writer.flush()
                process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
                if (process.isAlive) process.destroyForcibly()
            } catch (_: Exception) {
                process.destroyForcibly()
            }
        }
        stdinFds.forEach { (id, fd) ->
            try {
                val data = "stop\n".toByteArray(Charsets.UTF_8)
                VMLauncher.writeFd(fd, data, 0, data.size)
            } catch (_: Exception) {}
        }
        processes.clear()
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        tpsTrackers.clear()
    }
}
