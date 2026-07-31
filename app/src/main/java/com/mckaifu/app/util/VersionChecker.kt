package com.mckaifu.app.util

import com.mckaifu.app.data.model.CoreType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class VersionChecker {

    data class VersionInfo(
        val currentVersion: String = "",
        val latestVersion: String = "",
        val hasUpdate: Boolean = false,
        val isCompatible: Boolean = true,
        val mcVersion: String = "",
        val downloadUrl: String = ""
    )

    suspend fun checkCoreVersion(coreType: CoreType): VersionInfo = withContext(Dispatchers.IO) {
        try {
            val apiUrl = when (coreType) {
                CoreType.PAPER -> "https://api.papermc.io/v2/projects/paper"
                CoreType.PURPUR -> "https://api.purpurmc.org/v2/purpur"
                CoreType.PUFFERFISH -> "https://api.pufferfish.host/v1/pufferfish"
                CoreType.NUKKIT -> "https://api.nukkitx.com/v1/nukkit"
                else -> return@withContext VersionInfo()
            }

            val response = fetchUrl(apiUrl)
            val json = JSONObject(response)
            val versions = json.getJSONArray("versions")
            val latest = versions.getString(versions.length() - 1)

            VersionInfo(
                latestVersion = latest,
                mcVersion = latest,
                downloadUrl = "$apiUrl/versions/$latest"
            )
        } catch (e: Exception) {
            VersionInfo()
        }
    }

    fun checkMinecraftCompatibility(coreVersion: String, mcVersion: String): Boolean {
        try {
            val coreParts = coreVersion.split(".").map { it.toIntOrNull() ?: 0 }
            val mcParts = mcVersion.split(".").map { it.toIntOrNull() ?: 0 }

            for (i in 0 until minOf(coreParts.size, mcParts.size)) {
                if (coreParts[i] != mcParts[i]) return false
            }
            return true
        } catch (_: Exception) {
            return true
        }
    }

    private fun fetchUrl(urlString: String): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        return conn.inputStream.bufferedReader().readText()
    }
}
