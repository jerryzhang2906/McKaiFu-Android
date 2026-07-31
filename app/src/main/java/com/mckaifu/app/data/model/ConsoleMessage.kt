package com.mckaifu.app.data.model

data class ConsoleMessage(
    val timestamp: Long = System.currentTimeMillis(),
    val content: String = "",
    val type: LogType = LogType.INFO,
    val isRepeat: Boolean = false,
    val repeatCount: Int = 0
)

enum class LogType(val displayName: String, val colorHex: Long) {
    INFO("信息", 0xFFAAAAAA),
    WARN("警告", 0xFFFFAA00),
    ERROR("错误", 0xFFFF5555),
    SUCCESS("成功", 0xFF55FF55),
    DEBUG("调试", 0xFF5555FF),
    CHAT("聊天", 0xFFFFFFFF),
    COMMAND("命令", 0xFFFF55FF),
    SYSTEM("系统", 0xFF55FFFF)
}
