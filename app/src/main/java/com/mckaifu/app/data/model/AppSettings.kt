package com.mckaifu.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val darkMode: Boolean = true,
    val language: String = "zh",
    val passwordEnabled: Boolean = false,
    val passwordHash: String = "",
    val notificationEnabled: Boolean = true,
    val consoleMaxLines: Int = 1000,
    val autoScroll: Boolean = true,
    val collapseRepeatLogs: Boolean = true,
    val serverScanPath: String = "",
    val javaHomePath: String = "",
    val tunnelAutoConnect: Boolean = false,
    val backupCompression: Int = 5,
    val backupRetentionDays: Int = 7,
    val dataEncryption: Boolean = true,
    val themeColor: Int = 0xFF6C63FF.toInt()
)
