package com.mckaifu.app.service

import com.mckaifu.app.data.model.CoreType
import java.io.File
import java.util.jar.JarFile

class CoreDetector {

    data class CoreInfo(
        val type: CoreType,
        val version: String = "未知",
        val isCompatible: Boolean = true,
        val minecraftVersion: String = ""
    )

    fun detectCore(jarFile: File): CoreInfo? {
        if (!jarFile.exists() || !jarFile.name.endsWith(".jar")) return null

        return try {
            val jar = JarFile(jarFile)
            val pluginYml = jar.getEntry("plugin.yml")
            val serverProps = jar.getEntry("server.properties")
            val paperYml = jar.getEntry("paper.yml")
            val purpurYml = jar.getEntry("purpur.yml")
            val bukkitJson = jar.getEntry("org/bukkit/Server.class")

            val manifest = jar.manifest?.mainAttributes
            val mainClass = manifest?.getValue("Main-Class") ?: ""

            val coreType = when {
                mainClass.contains("net.pl3x.purpur", ignoreCase = true) ||
                    purpurYml != null -> CoreType.PURPUR
                mainClass.contains("io.papermc.paper", ignoreCase = true) ||
                    paperYml != null -> CoreType.PAPER
                mainClass.contains("net.techcable.tacospigot", ignoreCase = true) ||
                    mainClass.contains("co.aikar.timings", ignoreCase = true) -> CoreType.PUFFERFISH
                mainClass.contains("org.bukkit.craftbukkit", ignoreCase = true) ||
                    mainClass.contains("net.minecraft.server", ignoreCase = true) -> CoreType.SPIGOT
                mainClass.contains("net.nukkit", ignoreCase = true) ||
                    mainClass.contains("cn.nukkit", ignoreCase = true) -> CoreType.NUKKIT
                mainClass.contains("pocketmine", ignoreCase = true) -> CoreType.POCKETMINE
                serverProps != null -> CoreType.VANILLA
                else -> CoreType.CUSTOM
            }

            val mcVersion = extractVersion(jar)
            val implVersion = manifest?.getValue("Implementation-Version") ?: mcVersion

            jar.close()

            CoreInfo(
                type = coreType,
                version = implVersion,
                minecraftVersion = mcVersion,
                isCompatible = checkCompatibility(coreType, mcVersion)
            )

        } catch (e: Exception) {
            CoreInfo(type = CoreType.CUSTOM, version = "检测失败")
        }
    }

    private fun extractVersion(jar: JarFile): String {
        try {
            jar.getEntry("version.json")?.let { entry ->
                val text = jar.getInputStream(entry).bufferedReader().readText()
                val regex = Regex("\"name\"\\s*:\\s*\"([^\"]+)\"")
                regex.find(text)?.groupValues?.getOrNull(1)?.let { return it }
            }
        } catch (_: Exception) {}

        return try {
            val manifest = jar.manifest?.mainAttributes
            manifest?.getValue("Open-Source-Implementation-Version") ?:
            manifest?.getValue("Implementation-Version") ?:
            "未知"
        } catch (_: Exception) { "未知" }
    }

    private fun checkCompatibility(type: CoreType, version: String): Boolean {
        return when (type) {
            CoreType.PAPER, CoreType.PURPUR, CoreType.PUFFERFISH, CoreType.SPIGOT -> true
            CoreType.NUKKIT -> version.startsWith("1.")
            CoreType.POCKETMINE -> true
            else -> true
        }
    }

    fun getRecommendedCores(): List<CoreInfo> = listOf(
        CoreInfo(CoreType.PAPER, "1.20.4", minecraftVersion = "1.20.4"),
        CoreInfo(CoreType.PURPUR, "1.20.4", minecraftVersion = "1.20.4"),
        CoreInfo(CoreType.PUFFERFISH, "1.20.4", minecraftVersion = "1.20.4"),
        CoreInfo(CoreType.NUKKIT, "1.0.0", minecraftVersion = "基岩版1.20"),
        CoreInfo(CoreType.POCKETMINE, "5.0.0", minecraftVersion = "基岩版1.20"),
    )

    companion object {
        fun createDefaultJarName(coreType: CoreType): String = when (coreType) {
            CoreType.PAPER -> "paper-1.20.4.jar"
            CoreType.PURPUR -> "purpur-1.20.4.jar"
            CoreType.PUFFERFISH -> "pufferfish-1.20.4.jar"
            CoreType.SPIGOT -> "spigot-1.20.4.jar"
            CoreType.VANILLA -> "server.jar"
            CoreType.NUKKIT -> "nukkit-1.0.0.jar"
            CoreType.POCKETMINE -> "pocketmine-mp.phar"
            CoreType.CUSTOM -> "server.jar"
        }
    }
}
