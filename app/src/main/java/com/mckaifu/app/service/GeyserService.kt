package com.mckaifu.app.service

import com.mckaifu.app.data.model.ServerInstance
import java.io.File

class GeyserService {

    fun setupGeyser(server: ServerInstance, serverDir: File): Boolean {
        val pluginsDir = File(serverDir, "plugins")
        if (!pluginsDir.exists()) pluginsDir.mkdirs()

        val geyserJar = File(pluginsDir, "Geyser.jar")
        if (!geyserJar.exists()) {
            return downloadGeyser(geyserJar)
        }
        return true
    }

    private fun downloadGeyser(target: File): Boolean {
        return try {
            val url = java.net.URL(
                "https://download.geysermc.org/v2/projects/geyser/versions/latest/" +
                "builds/latest/downloads/geyser-spigot"
            )
            url.openStream().use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun configureGeyser(serverDir: File, bedrockPort: Int): Boolean {
        return try {
            val configDir = File(serverDir, "plugins/Geyser-Spigot")
            configDir.mkdirs()
            val configFile = File(configDir, "config.yml")
            if (!configFile.exists()) {
                configFile.writeText(
                    """
bedrock:
  port: $bedrockPort
  address: 0.0.0.0
remote:
  address: 127.0.0.1
  port: 25565
                    """.trimIndent()
                )
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
