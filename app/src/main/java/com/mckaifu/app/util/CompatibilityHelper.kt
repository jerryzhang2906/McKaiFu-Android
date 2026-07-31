package com.mckaifu.app.util

import com.mckaifu.app.data.model.CoreType

enum class CompatibilityLevel { FULL, PARTIAL, INCOMPATIBLE }

data class CompatibilityInfo(
    val level: CompatibilityLevel,
    val title: String,
    val description: String
)

fun getCompatibilityInfo(mcVersion: String, coreType: CoreType): CompatibilityInfo {
    val parts = mcVersion.split(".")
    val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0

    return when {
        coreType.isJava() && major >= 1 && minor >= 20 -> CompatibilityInfo(
            CompatibilityLevel.FULL, "完全兼容",
            "此核心完美支持Minecraft $mcVersion 的所有特性"
        )
        coreType.isJava() && major >= 1 && minor >= 17 -> CompatibilityInfo(
            CompatibilityLevel.FULL, "兼容",
            "支持Minecraft $mcVersion 的大部分特性"
        )
        coreType.isJava() && major >= 1 && minor >= 13 -> CompatibilityInfo(
            CompatibilityLevel.PARTIAL, "部分兼容",
            "部分新特性可能不支持，建议升级版本"
        )
        coreType.isJava() && major >= 1 -> CompatibilityInfo(
            CompatibilityLevel.PARTIAL, "旧版兼容",
            "适用于旧版Minecraft，建议使用1.16+版本"
        )
        coreType.isBedrock() -> CompatibilityInfo(
            CompatibilityLevel.FULL, "基岩版兼容",
            "此核心专为基岩版设计，可与基岩版客户端连接"
        )
        else -> CompatibilityInfo(
            CompatibilityLevel.INCOMPATIBLE, "未知兼容性",
            "请确认此核心与你的Minecraft版本匹配"
        )
    }
}

fun formatMcVersion(mcVersion: String, coreType: CoreType): String =
    if (coreType.isBedrock()) mcVersion else "Minecraft $mcVersion"

fun getClientVersionHint(mcVersion: String, coreType: CoreType): String = when {
    coreType == CoreType.NUKKIT -> "基岩版 1.20.x 客户端可加入"
    coreType == CoreType.POCKETMINE -> "基岩版 1.20.x 客户端可加入"
    coreType.isJava() -> "Java版 $mcVersion 客户端可加入"
    else -> "基岩版客户端可加入"
}
