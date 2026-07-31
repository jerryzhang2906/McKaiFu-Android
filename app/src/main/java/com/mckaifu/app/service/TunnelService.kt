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

    fun startTunnel(tunnel: TunnelInfo, playitExecutable: File, onLog: (String) -> Unit) {
        if (tunnelProcess?.isAlive == true) return

        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                val commands = when (tunnel.type) {
                    TunnelType.PLAYIT -> listOf(
                        playitExecutable.absolutePath,
                        "--secret", tunnel.authToken,
                        "--port", tunnel.localPort.toString()
                    )
                    TunnelType.NGROK -> listOf(
                        "ngrok", "tcp", tunnel.localPort.toString(),
                        "--authtoken", tunnel.authToken
                    )
                    TunnelType.NATAPP -> listOf(
                        "natapp", "-authtoken", tunnel.authToken,
                        "-servername", tunnel.region.name.lowercase().ifEmpty { "auto" }
                    )
                    TunnelType.SAKURA -> listOf(
                        "frpc", "-f", tunnel.authToken,
                        "-p", tunnel.region.name.lowercase().ifEmpty { "auto" }
                    )
                    else -> return@launch
                }

                val pb = ProcessBuilder(commands)
                    .redirectErrorStream(true)
                tunnelProcess = pb.start()

                _status.value = TunnelStatus(isActive = true, region = tunnel.region.displayName)

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
}
