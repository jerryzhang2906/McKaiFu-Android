package com.mckaifu.app.service

import com.mckaifu.app.data.model.TunnelInfo
import com.mckaifu.app.data.model.TunnelRegion
import com.mckaifu.app.data.model.TunnelType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class TunnelService {

    private var tunnelProcess: Process? = null
    private var job: Job? = null

    data class TunnelStatus(
        val isActive: Boolean = false,
        val publicAddress: String = "",
        val publicPort: Int = 0,
        val region: String = "",
        val error: String = ""
    )

    private val _status = MutableStateFlow(TunnelStatus())
    val status: kotlinx.coroutines.flow.StateFlow<TunnelStatus> = _status.asStateFlow()

    fun startTunnel(tunnel: TunnelInfo, executable: File, onLog: (String) -> Unit) {
        if (tunnelProcess?.isAlive == true) return

        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                val configFile = tunnel.configPath.takeIf { it.isNotBlank() }
                    ?.let { File(it) }?.takeIf { it.exists() }

                val commands = when (tunnel.type) {
                    TunnelType.PLAYIT -> listOf(
                        executable.absolutePath,
                        "--secret", tunnel.authToken,
                        "--port", tunnel.localPort.toString()
                    )
                    TunnelType.NGROK -> if (configFile != null) {
                        listOf(executable.absolutePath, "start", "--config", configFile.absolutePath)
                    } else listOf(
                        executable.absolutePath, "tcp", tunnel.localPort.toString(),
                        "--authtoken", tunnel.authToken
                    )
                    TunnelType.NATAPP -> if (configFile != null) {
                        listOf(executable.absolutePath, "-config", configFile.absolutePath)
                    } else listOf(
                        executable.absolutePath, "-authtoken", tunnel.authToken,
                        "-servername", tunnel.region.name.lowercase().ifEmpty { "auto" }
                    )
                    TunnelType.SAKURA -> if (configFile != null) {
                        listOf(executable.absolutePath, "-c", configFile.absolutePath)
                    } else listOf(
                        executable.absolutePath, "-f", tunnel.authToken,
                        "-p", tunnel.region.name.lowercase().ifEmpty { "auto" }
                    )
                    TunnelType.CUSTOM -> if (configFile != null) {
                        listOf(executable.absolutePath, "-c", configFile.absolutePath)
                    } else listOf(executable.absolutePath)
                }

                val pb = ProcessBuilder(commands)
                    .redirectErrorStream(true)
                tunnelProcess = pb.start()

                _status.value = TunnelStatus(isActive = true, region = tunnel.region.displayName)

                if (configFile != null &&
                    (tunnel.type == TunnelType.CUSTOM || tunnel.type == TunnelType.SAKURA)
                ) {
                    val addr = parseFrpPublicAddress(configFile)
                    if (addr.isNotEmpty()) {
                        _status.value = _status.value.copy(
                            publicAddress = addr,
                            publicPort = tunnel.localPort
                        )
                    }
                }

                val reader = BufferedReader(InputStreamReader(tunnelProcess!!.inputStream))
                var line: String?
                while (isActive) {
                    line = reader.readLine() ?: break
                    onLog(line)

                    when {
                        line.contains("https://", ignoreCase = true) &&
                            line.contains(".playit.", ignoreCase = true) -> {
                            val addr = Regex("https://[^\\s]+").find(line)?.value ?: ""
                            _status.value = _status.value.copy(
                                publicAddress = addr,
                                publicPort = tunnel.localPort
                            )
                        }
                        line.contains("Forwarding", ignoreCase = true) &&
                            line.contains("tcp://", ignoreCase = true) -> {
                            val addr = Regex("tcp://[^\\s]+").find(line)?.value ?: ""
                            _status.value = _status.value.copy(
                                publicAddress = addr,
                                publicPort = tunnel.localPort
                            )
                        }
                        line.contains("natapp", ignoreCase = true) &&
                            line.contains("established", ignoreCase = true) -> {
                            val addr = Regex("[\\w.-]+\\.natapp[\\w.]+:\\d+").find(line)?.value
                                ?: Regex("[\\w.-]+\\.natappfree[\\w.]+:\\d+").find(line)?.value ?: ""
                            _status.value = _status.value.copy(
                                publicAddress = addr,
                                publicPort = tunnel.localPort
                            )
                        }
                        tunnel.type == TunnelType.CUSTOM -> {
                            val addr = Regex("[\\w.-]+:\\d+").find(line)?.value ?: ""
                            if (addr.isNotEmpty()) {
                                _status.value = _status.value.copy(
                                    publicAddress = addr,
                                    publicPort = tunnel.localPort
                                )
                            }
                        }
                        line.contains("sakura", ignoreCase = true) ||
                            line.contains("frpc", ignoreCase = true) -> {
                            val addr = Regex("[\\w.-]+\\.sakurafrp[\\w.]+:\\d+").find(line)?.value ?: ""
                            if (addr.isNotEmpty()) {
                                _status.value = _status.value.copy(
                                    publicAddress = addr,
                                    publicPort = tunnel.localPort
                                )
                            }
                        }
                        line.contains("error", ignoreCase = true) -> {
                            _status.value = _status.value.copy(error = line)
                        }
                    }
                }

                tunnelProcess?.waitFor()
                _status.value = TunnelStatus()

            } catch (e: Exception) {
                _status.value = TunnelStatus(error = e.message ?: "隧道启动失败")
            }
        }
    }

    fun stopTunnel() {
        tunnelProcess?.destroyForcibly()
        tunnelProcess = null
        job?.cancel()
        _status.value = TunnelStatus()
    }

    fun isActive(): Boolean = tunnelProcess?.isAlive == true

    private fun parseFrpPublicAddress(configFile: File): String {
        val text = try { configFile.readText(Charsets.UTF_8) } catch (_: Exception) { return "" }
        val isToml = text.contains("[[proxies]]")
        var server = ""
        var serverPort = ""
        var remotePort = ""
        for (rawLine in text.lineSequence()) {
            val line = rawLine.substringBefore('#').trim()
            if (isToml) {
                if (server.isEmpty() && line.startsWith("serverAddr")) {
                    server = line.substringAfter('=').trim().trim('"', ' ')
                        .removePrefix("https://").removePrefix("http://").removePrefix("tcp://")
                } else if (serverPort.isEmpty() && line.startsWith("serverPort")) {
                    serverPort = line.substringAfter('=').trim()
                } else if (remotePort.isEmpty() && line.startsWith("remotePort")) {
                    remotePort = line.substringAfter('=').trim()
                }
            } else {
                if (server.isEmpty() && line.startsWith("server_addr")) {
                    server = line.substringAfter('=').trim()
                        .removePrefix("https://").removePrefix("http://").removePrefix("tcp://")
                } else if (remotePort.isEmpty() && line.startsWith("remote_port")) {
                    remotePort = line.substringAfter('=').trim()
                }
            }
            if (server.isNotEmpty() && remotePort.isNotEmpty()) break
        }
        val port = remotePort.ifEmpty { serverPort }
        if (server.isEmpty() || port.isEmpty()) return ""
        return "$server:$port"
    }
}
