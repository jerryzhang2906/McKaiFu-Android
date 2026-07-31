package com.mckaifu.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PlayerInfo(
    val uuid: String = "",
    val name: String = "",
    val displayName: String = "",
    val ip: String = "",
    val ping: Int = 0,
    val world: String = "world",
    val gameMode: GameMode = GameMode.SURVIVAL,
    val health: Double = 20.0,
    val maxHealth: Double = 20.0,
    val hunger: Int = 20,
    val saturation: Float = 5.0f,
    val xp: Int = 0,
    val level: Int = 0,
    val position: Position = Position(),
    val isOp: Boolean = false,
    val isWhitelisted: Boolean = false,
    val isBanned: Boolean = false,
    val firstJoined: Long = System.currentTimeMillis(),
    val lastJoined: Long = System.currentTimeMillis(),
    val serverId: String = ""
)

@Serializable
data class Position(
    val x: Double = 0.0,
    val y: Double = 64.0,
    val z: Double = 0.0,
    val yaw: Float = 0.0f,
    val pitch: Float = 0.0f
)

@Serializable
enum class GameMode(val displayName: String) {
    SURVIVAL("生存"),
    CREATIVE("创造"),
    ADVENTURE("冒险"),
    SPECTATOR("旁观")
}

@Serializable
data class BanEntry(
    val uuid: String = "",
    val name: String = "",
    val ip: String = "",
    val reason: String = "违规行为",
    val source: String = "管理员",
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val type: BanType = BanType.PLAYER
)

@Serializable
enum class BanType {
    PLAYER, IP
}
