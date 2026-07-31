package com.mckaifu.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ServerInstance(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "我的服务器",
    val coreType: CoreType = CoreType.PAPER,
    val coreVersion: String = "1.20.4",
    val status: ServerStatus = ServerStatus.OFFLINE,
    val port: Int = 25565,
    val memoryMin: Int = 512,
    val memoryMax: Int = 2048,
    val javaVersion: Int = 17,
    val javaArgs: String = "-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200",
    val serverPath: String = "",
    val jarFileName: String = "server.jar",
    val autoRestart: Boolean = false,
    val autoBackup: Boolean = false,
    val backupIntervalHours: Int = 24,
    val tunnelEnabled: Boolean = false,
    val tunnelType: TunnelType = TunnelType.PLAYIT,
    val geyserEnabled: Boolean = false,
    val geyserPort: Int = 19132,
    val worldName: String = "world",
    val createdAt: Long = System.currentTimeMillis(),
    val lastStartedAt: Long? = null,
    val playerCount: Int = 0,
    val maxPlayers: Int = 20,
    val tps: Double = 20.0,
    val memoryUsage: Long = 0L,
    val cpuUsage: Double = 0.0,
    val isCustomJar: Boolean = false,
    val customJarPath: String? = null,
)

@Serializable
enum class CoreType(val displayName: String, val description: String) {
    PAPER("PaperMC", "高性能Paper服务端，兼容Spigot/Bukkit插件"),
    PURPUR("Purpur", "基于Paper，提供更多配置选项"),
    PUFFERFISH("Pufferfish", "优化版Paper，提升TPS"),
    SPIGOT("Spigot", "经典Bukkit改进版"),
    VANILLA("原版", "Minecraft官方服务端"),
    NUKKIT("Nukkit", "基岩版Java服务端"),
    POCKETMINE("PocketMine-MP", "基岩版PHP服务端"),
    CUSTOM("自定义", "用户上传的自定义JAR文件");

    fun isBedrock(): Boolean = this == NUKKIT || this == POCKETMINE
    fun isJava(): Boolean = !isBedrock()
}

@Serializable
enum class ServerStatus(val displayName: String) {
    ONLINE("在线"),
    OFFLINE("离线"),
    STARTING("启动中"),
    STOPPING("停止中"),
    ERROR("错误"),
    RESTARTING("重启中")
}

@Serializable
enum class TunnelType(val displayName: String) {
    PLAYIT("Playit.gg"),
    NGROK("Ngrok"),
    NATAPP("NATAPP"),
    SAKURA("樱花frp"),
    CUSTOM("自定义隧道")
}
