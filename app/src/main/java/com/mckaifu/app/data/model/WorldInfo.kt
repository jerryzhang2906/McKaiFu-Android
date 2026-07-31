package com.mckaifu.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class WorldInfo(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "world",
    val displayName: String = "主世界",
    val serverId: String = "",
    val sizeBytes: Long = 0L,
    val seed: Long = 0L,
    val gameMode: GameMode = GameMode.SURVIVAL,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val players: List<String> = emptyList(),
    val lastBackupAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

@Serializable
enum class Difficulty(val displayName: String) {
    PEACEFUL("和平"),
    EASY("简单"),
    NORMAL("普通"),
    HARD("困难")
}

@Serializable
data class BackupEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val worldId: String = "",
    val worldName: String = "",
    val fileName: String = "",
    val sizeBytes: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val type: BackupType = BackupType.MANUAL
)

@Serializable
enum class BackupType {
    MANUAL, AUTO, SCHEDULED
}
