package com.mckaifu.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ServerConfig(
    val serverProperties: Map<String, String> = defaultServerProperties(),
    val bukkitConfig: Map<String, String> = emptyMap(),
    val spigotConfig: Map<String, String> = emptyMap(),
    val paperConfig: Map<String, String> = emptyMap(),
    val purpurConfig: Map<String, String> = emptyMap(),
    val pufferfishConfig: Map<String, String> = emptyMap(),
)

fun defaultServerProperties(): Map<String, String> = mapOf(
    "server-port" to "25565",
    "max-players" to "20",
    "online-mode" to "true",
    "motd" to "A Minecraft Server",
    "difficulty" to "normal",
    "gamemode" to "survival",
    "pvp" to "true",
    "allow-flight" to "false",
    "view-distance" to "10",
    "simulation-distance" to "10",
    "spawn-protection" to "16",
    "enable-command-block" to "false",
    "white-list" to "false",
    "enforce-secure-profile" to "true",
    "enforce-whitelist" to "false",
    "hardcore" to "false",
    "max-tick-time" to "60000",
    "network-compression-threshold" to "256",
    "rate-limit" to "0",
    "sync-chunk-writes" to "true",
    "entity-broadcast-range-percentage" to "100",
    "max-world-size" to "29999984",
)

@Serializable
data class PerformanceConfig(
    val memoryMin: Int = 512,
    val memoryMax: Int = 2048,
    val javaArgs: String = "-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:TargetSurvivorRatio=90 -XX:+UnlockExperimentalVMOptions -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M",
    val useAikarFlags: Boolean = true,
    val optimizeForLowRam: Boolean = false,
)

@Serializable
data class ScheduleConfig(
    val enabled: Boolean = false,
    val tasks: List<ScheduledTask> = emptyList()
)

@Serializable
data class ScheduledTask(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "新任务",
    val type: TaskType = TaskType.RESTART,
    val intervalHours: Int = 24,
    val isEnabled: Boolean = false,
    val lastRunAt: Long? = null,
    val serverId: String = "",
    val command: String = ""
)

@Serializable
enum class TaskType(val displayName: String) {
    RESTART("定时重启"),
    BACKUP("定时备份"),
    COMMAND("执行命令"),
    STOP("定时停止"),
    START("定时启动")
}
