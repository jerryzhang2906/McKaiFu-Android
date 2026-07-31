package com.mckaifu.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PluginInfo(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val version: String = "1.0.0",
    val description: String = "",
    val author: String = "",
    val mainClass: String = "",
    val serverId: String = "",
    val fileName: String = "",
    val fileSize: Long = 0L,
    val isEnabled: Boolean = true,
    val isLoaded: Boolean = false,
    val website: String? = null,
    val apiVersion: String? = null,
    val downloadUrl: String? = null,
    val source: PluginSource = PluginSource.UNKNOWN,
    val iconUrl: String? = null,
    val downloadsCount: Long = 0,
    val body: String? = null,
    val categories: List<String> = emptyList(),
    val gameVersions: List<String> = emptyList(),
    val loaders: List<String> = emptyList(),
)

@Serializable
enum class PluginSource(val displayName: String) {
    MODRINTH("Modrinth"),
    SPIGET("Spiget"),
    HANGAR("Hangar"),
    BUILTIN("内置"),
    UPLOADED("上传"),
    UNKNOWN("未知")
}

@Serializable
data class ModInfo(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val version: String = "1.0.0",
    val serverId: String = "",
    val fileName: String = "",
    val isEnabled: Boolean = true,
    val downloadUrl: String? = null,
    val source: PluginSource = PluginSource.UNKNOWN,
)

@Serializable
data class CoreVersion(
    val coreType: CoreType,
    val version: String,
    val mcVersion: String = "",
    val buildNumber: Int = 0,
    val downloadUrl: String = "",
    val fileSize: Long = 0,
    val releaseDate: Long = 0,
    val isRecommended: Boolean = false,
)

data class DownloadProgress(
    val isDownloading: Boolean = false,
    val progress: Float = 0f,
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = 0,
    val speed: String = "",
    val currentFile: String = "",
    val error: String? = null,
)
