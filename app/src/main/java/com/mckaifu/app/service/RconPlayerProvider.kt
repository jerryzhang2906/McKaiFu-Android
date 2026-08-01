package com.mckaifu.app.service

import com.mckaifu.app.data.model.PlayerInfo
import com.mckaifu.app.data.model.Position
import java.io.File
import java.util.concurrent.ThreadLocalRandom

object RconPlayerProvider {

    data class RconConfig(val port: Int, val password: String)

    private const val DEFAULT_PORT = 25575
    private val propsRegex = Regex("""^\s*([^#][^=]*?)\s*=\s*(.*?)\s*$""")

    fun ensureRcon(serverDir: File): RconConfig {
        val propsFile = File(serverDir, "server.properties")
        val props = readProperties(propsFile)
        if (props["enable-rcon"]?.trim()?.equals("true", true) == true) {
            val password = props["rcon.password"]?.trim().orEmpty()
            if (password.isNotEmpty()) {
                return RconConfig(
                    props["rcon.port"]?.trim()?.toIntOrNull() ?: DEFAULT_PORT,
                    password
                )
            }
        }
        val password = generatePassword()
        val lines = if (propsFile.exists()) propsFile.readLines().toMutableList() else mutableListOf()
        val updated = lines.map { line ->
            when {
                line.startsWith("enable-rcon") -> "enable-rcon=true"
                line.startsWith("rcon.port") -> "rcon.port=$DEFAULT_PORT"
                line.startsWith("rcon.password") -> "rcon.password=$password"
                else -> line
            }
        }.toMutableList()
        val keys = updated.map { it.substringBefore('=') }.toSet()
        if ("enable-rcon" !in keys) updated.add("enable-rcon=true")
        if ("rcon.port" !in keys) updated.add("rcon.port=$DEFAULT_PORT")
        if ("rcon.password" !in keys) updated.add("rcon.password=$password")
        propsFile.writeText(updated.joinToString("\n") + "\n")
        return RconConfig(DEFAULT_PORT, password)
    }

    fun fetchPlayers(serverId: String, config: RconConfig, onUpdate: (PlayerInfo) -> Unit) {
        val rcon = RconService("127.0.0.1", config.port, config.password)
        try {
            if (!rcon.connect()) return
            val names = parseOnlineNames(rcon.sendCommand("list"))
            for (name in names) {
                val out = rcon.sendCommand("data get entity $name")
                val info = parseEntityData(serverId, name, out) ?: continue
                onUpdate(info)
            }
        } catch (_: Exception) {
        } finally {
            rcon.disconnect()
        }
    }

    private fun readProperties(file: File): Map<String, String> {
        if (!file.exists()) return emptyMap()
        return file.readLines().mapNotNull { line ->
            val m = propsRegex.find(line) ?: return@mapNotNull null
            m.groupValues[1].trim() to m.groupValues[2].trim()
        }.toMap()
    }

    private fun generatePassword(): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..16).map { chars[ThreadLocalRandom.current().nextInt(chars.length)] }.joinToString("")
    }

    private fun parseOnlineNames(output: String): List<String> {
        val idx = output.indexOf("online:")
        if (idx < 0) return emptyList()
        return output.substring(idx + "online:".length)
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun parseEntityData(serverId: String, name: String, output: String): PlayerInfo? {
        val start = output.indexOf('{')
        val end = output.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        val nbt = output.substring(start + 1, end)

        fun field(regex: Regex): String? = regex.find(nbt)?.groupValues?.get(1)

        val health = field(Regex("""Health: ([0-9.]+)f"""))?.toDoubleOrNull() ?: 20.0
        val maxHealth = Regex("""generic\.max_health", Base: ([0-9.]+)""")
            .find(nbt)?.groupValues?.get(1)?.toDoubleOrNull() ?: 20.0
        val hunger = field(Regex("""foodLevel: (\d+)"""))?.toIntOrNull() ?: 20
        val saturation = field(Regex("""foodSaturationLevel: ([0-9.]+)f"""))?.toFloatOrNull() ?: 5.0f
        val level = field(Regex("""XpLevel: (\d+)"""))?.toIntOrNull() ?: 0
        val xpProgress = field(Regex("""XpP: ([0-9.]+)f"""))?.toFloatOrNull() ?: 0f
        val xp = (level * 100 + xpProgress * 100).toInt()

        val pos = Regex("""Pos: \[([-0-9.]+)d, ([-0-9.]+)d, ([-0-9.]+)d\]""").find(nbt)
        val position = if (pos != null) Position(
            x = pos.groupValues[1].toDouble(),
            y = pos.groupValues[2].toDouble(),
            z = pos.groupValues[3].toDouble()
        ) else Position()

        val dim = field(Regex("""Dimension: (-?\d+)"""))?.toIntOrNull()
        val world = when (dim) {
            -1 -> "the_nether"
            1 -> "the_end"
            else -> "overworld"
        }

        return PlayerInfo(
            uuid = "",
            name = name,
            displayName = name,
            ip = "",
            ping = 0,
            world = world,
            gameMode = com.mckaifu.app.data.model.GameMode.SURVIVAL,
            health = health,
            maxHealth = maxHealth,
            hunger = hunger,
            saturation = saturation,
            xp = xp,
            level = level,
            position = position,
            isOp = false,
            isWhitelisted = false,
            isBanned = false,
            serverId = serverId
        )
    }
}
