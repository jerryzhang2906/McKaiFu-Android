package com.mckaifu.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TunnelInfo(
    val id: String = java.util.UUID.randomUUID().toString(),
    val serverId: String = "",
    val type: TunnelType = TunnelType.PLAYIT,
    val isActive: Boolean = false,
    val publicAddress: String = "",
    val publicPort: Int = 0,
    val localPort: Int = 25565,
    val region: TunnelRegion = TunnelRegion.AUTO,
    val authToken: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
enum class TunnelRegion(val displayName: String, val description: String = "") {
    AUTO("自动最优", "自动选择最佳节点"),
    CN_NORTH("中国华北", "华北 - 北京/天津等地玩家低延迟"),
    CN_EAST("中国华东", "华东 - 上海/杭州等地玩家低延迟"),
    CN_SOUTH("中国华南", "华南 - 广州/深圳等地玩家低延迟"),
    CN_WEST("中国西部", "西部 - 成都/重庆等地玩家低延迟"),
    US_EAST("美东", "美国东部 - 北美玩家低延迟"),
    US_WEST("美西", "美国西部 - 北美玩家低延迟"),
    EU_WEST("欧洲西部", "欧洲西部 - 欧洲玩家低延迟"),
    EU_CENTRAL("欧洲中部", "欧洲中部 - 欧洲玩家低延迟"),
    ASIA_EAST("东亚", "东亚 - 中日韩玩家低延迟"),
    ASIA_SE("东南亚", "东南亚 - 东南亚玩家低延迟"),
    OCEANIA("大洋洲", "大洋洲 - 澳新玩家低延迟")
}
