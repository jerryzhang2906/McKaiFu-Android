package com.mckaifu.app.service

import com.mckaifu.app.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToLong

class DownloadManager {

    private val _downloadProgress = MutableStateFlow(DownloadProgress())
    val downloadProgress: StateFlow<DownloadProgress> = _downloadProgress.asStateFlow()

    private val _cachedCoreVersions = MutableStateFlow<Map<CoreType, List<CoreVersion>>>(emptyMap())
    val cachedCoreVersions: StateFlow<Map<CoreType, List<CoreVersion>>> = _cachedCoreVersions.asStateFlow()

    private val _storePlugins = MutableStateFlow<List<PluginInfo>>(emptyList())
    val storePlugins: StateFlow<List<PluginInfo>> = _storePlugins.asStateFlow()

    fun fetchCoreVersions(coreType: CoreType, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val versions = when (coreType) {
                    CoreType.PAPER -> fetchPaperVersions()
                    CoreType.PURPUR -> fetchPurpurVersions()
                    CoreType.PUFFERFISH -> fetchPufferfishVersions()
                    CoreType.SPIGOT -> fetchSpigotVersions()
                    CoreType.VANILLA -> fetchVanillaVersions()
                    CoreType.NUKKIT -> fetchNukkitVersions()
                    CoreType.POCKETMINE -> fetchPocketmineVersions()
                    CoreType.CUSTOM -> emptyList()
                }
                val all = _cachedCoreVersions.value.toMutableMap()
                all[coreType] = versions
                _cachedCoreVersions.value = all
            } catch (_: Exception) {}
        }
    }

    fun searchPlugins(query: String, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                var results: List<PluginInfo> = emptyList()
                try {
                    results = searchModrinth(query).map { it.copy(source = PluginSource.MODRINTH) }
                } catch (_: Exception) {
                    results = emptyList()
                }
                if (results.isEmpty()) {
                    results = searchSpiget(query).map { it.copy(source = PluginSource.SPIGET) }
                }
                _storePlugins.value = results
            } catch (_: Exception) {}
        }
    }

    fun getFeaturedPlugins(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                _storePlugins.value = fetchFeaturedModrinth()
            } catch (_: Exception) {
                try {
                    _storePlugins.value = fetchFeaturedSpiget()
                } catch (_: Exception) {}
            }
        }
    }

    private fun fetchFeaturedModrinth(): List<PluginInfo> {
        val popular = fetchUrl(
            "https://api.modrinth.org/v2/search?limit=30&index=downloads" +
                    "&facets=%5B%5B%22project_type%3Aplugin%22%5D%5D"
        )
        val json = JSONObject(popular)
        val hits = json.getJSONArray("hits")
        val featured = mutableListOf<PluginInfo>()
        for (i in 0 until hits.length()) {
            val hit = hits.getJSONObject(i)
            featured.add(parseModrinthHit(hit))
        }
        return featured
    }

    private fun fetchFeaturedSpiget(): List<PluginInfo> {
        val url = "https://api.spiget.org/v2/resources?size=30&sort=-downloads"
        val json = JSONArray(fetchUrl(url))
        val results = mutableListOf<PluginInfo>()
        for (i in 0 until json.length()) {
            results.add(parseSpigetItem(json.getJSONObject(i)))
        }
        return results
    }

    suspend fun fetchPluginDetail(
        source: PluginSource,
        pluginId: String,
        gameVersion: String? = null,
    ): PluginInfo? = withContext(Dispatchers.IO) {
        try {
            when (source) {
                PluginSource.MODRINTH -> fetchModrinthDetail(pluginId, gameVersion)
                PluginSource.SPIGET -> fetchSpigetDetail(pluginId)
                else -> null
            }
        } catch (_: Exception) { null }
    }

    private fun fetchModrinthDetail(projectId: String, gameVersion: String?): PluginInfo {
        val project = JSONObject(fetchUrl("https://api.modrinth.org/v2/project/$projectId"))

        var latestVersion = project.optString("latest_version", "")
        var fileUrl: String? = null
        var fileName = "${project.optString("slug", projectId)}.jar"
        var fileSize = 0L
        var versionName = latestVersion

        try {
            val versions = JSONArray(fetchUrl("https://api.modrinth.org/v2/project/$projectId/version"))
            for (i in 0 until versions.length()) {
                val v = versions.getJSONObject(i)
                if (gameVersion != null && v.has("game_versions")) {
                    val gvs = v.getJSONArray("game_versions")
                    var match = false
                    for (j in 0 until gvs.length()) {
                        if (gvs.getString(j).startsWith(gameVersion)) { match = true; break }
                    }
                    if (!match) continue
                }
                latestVersion = v.optString("version_number", latestVersion)
                versionName = latestVersion
                val files = v.optJSONArray("files")
                if (files != null && files.length() > 0) {
                    val file = files.getJSONObject(0)
                    fileUrl = file.optString("url")
                    fileName = file.optString("filename", fileName)
                    fileSize = file.optLong("size", 0)
                }
                break
            }
        } catch (_: Exception) {}

        val loaders = mutableListOf<String>()
        try {
            val ls = project.getJSONArray("loaders")
            for (i in 0 until ls.length()) loaders.add(ls.getString(i))
        } catch (_: Exception) {}

        val gameVersions = mutableListOf<String>()
        try {
            val gvs = project.getJSONArray("game_versions")
            for (i in 0 until gvs.length()) gameVersions.add(gvs.getString(i))
        } catch (_: Exception) {}

        val categories = mutableListOf<String>()
        try {
            val cats = project.getJSONArray("categories")
            for (i in 0 until cats.length()) categories.add(cats.getString(i))
        } catch (_: Exception) {}

        var author = ""
        try {
            val members = JSONArray(fetchUrl("https://api.modrinth.org/v2/project/$projectId/members"))
            for (i in 0 until members.length()) {
                val m = members.getJSONObject(i)
                if (m.optString("role", "").contains("owner")) {
                    author = m.getJSONObject("user").optString("username", "")
                    break
                }
            }
            if (author.isEmpty() && members.length() > 0) {
                author = members.getJSONObject(0).getJSONObject("user").optString("username", "")
            }
        } catch (_: Exception) {}

        return PluginInfo(
            id = projectId,
            name = project.optString("title", "未知插件"),
            version = versionName,
            description = project.optString("description", ""),
            body = project.optString("body", ""),
            author = author,
            fileName = fileName,
            fileSize = fileSize,
            downloadUrl = fileUrl,
            source = PluginSource.MODRINTH,
            iconUrl = project.optString("icon_url"),
            downloadsCount = project.optLong("downloads", 0),
            categories = categories,
            gameVersions = gameVersions,
            loaders = loaders,
        )
    }

    private fun fetchSpigetDetail(resourceId: String): PluginInfo {
        val item = JSONObject(fetchUrl("https://api.spiget.org/v2/resources/$resourceId"))
        val file = item.optJSONObject("file")
        var downloadUrl = "https://api.spiget.org/v2/resources/$resourceId/download"
        if (file != null && file.has("type")) {
            val type = file.optString("type")
            if (type == "external") {
                downloadUrl = "https://www.spigotmc.org/resources/$resourceId/"
            }
        }
        return PluginInfo(
            id = resourceId,
            name = item.optString("name", "未知插件"),
            version = item.optJSONObject("version")?.optString("name") ?: "latest",
            description = item.optString("tag", ""),
            body = item.optString("description", ""),
            author = item.optJSONObject("author")?.optString("name") ?: "未知",
            fileName = file?.optString("name") ?: "${item.optString("name", "plugin")}.jar",
            downloadUrl = downloadUrl,
            source = PluginSource.SPIGET,
            iconUrl = null,
            downloadsCount = item.optLong("downloads", 0),
        )
    }

    fun downloadFile(
        url: String,
        targetFile: File,
        scope: CoroutineScope,
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                _downloadProgress.value = DownloadProgress(
                    isDownloading = true,
                    currentFile = targetFile.name
                )

                targetFile.parentFile?.mkdirs()

                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 30000
                connection.readTimeout = 30000
                connection.connect()

                val totalBytes = connection.contentLengthLong
                val inputStream = connection.inputStream
                val outputStream = FileOutputStream(targetFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalRead: Long = 0
                var lastUpdate = System.nanoTime()
                var lastBytes = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalRead += bytesRead

                    val now = System.nanoTime()
                    if (now - lastUpdate > 500_000_000) {
                        val elapsed = (now - lastUpdate) / 1_000_000_000.0
                        val speed = if (elapsed > 0) {
                            ((totalRead - lastBytes) / elapsed).toLong()
                        } else 0L

                        _downloadProgress.value = DownloadProgress(
                            isDownloading = true,
                            progress = if (totalBytes > 0) totalRead.toFloat() / totalBytes else 0f,
                            bytesDownloaded = totalRead,
                            totalBytes = totalBytes,
                            speed = formatSpeed(speed),
                            currentFile = targetFile.name,
                        )
                        lastUpdate = now
                        lastBytes = totalRead
                    }
                }

                outputStream.close()
                inputStream.close()

                _downloadProgress.value = DownloadProgress(
                    isDownloading = false,
                    progress = 1f,
                    bytesDownloaded = totalBytes,
                    totalBytes = totalBytes,
                )
            } catch (e: Exception) {
                _downloadProgress.value = DownloadProgress(
                    isDownloading = false,
                    error = e.message
                )
            }
        }
    }

    // ── Paper API ──
    private fun fetchPaperVersions(): List<CoreVersion> {
        val project = JSONObject(fetchUrl("https://fill.papermc.io/v3/projects/paper"))
        val versionsObj = project.getJSONObject("versions")
        val allVersions = mutableListOf<String>()

        val groups = versionsObj.keys().asSequence().toList()
        for (group in groups) {
            val patches = versionsObj.getJSONArray(group)
            for (i in 0 until patches.length()) {
                allVersions.add(patches.getString(i))
            }
        }
        allVersions.sortWith(Comparator { a, b ->
            val ta = versionTuple(a)
            val tb = versionTuple(b)
            for (i in 0 until maxOf(ta.size, tb.size)) {
                val x = ta.getOrNull(i) ?: 0
                val y = tb.getOrNull(i) ?: 0
                if (x != y) return@Comparator y.compareTo(x)
            }
            0
        })

        val result = mutableListOf<CoreVersion>()

        for ((index, ver) in allVersions.withIndex()) {
            try {
                val buildsJson = fetchUrl("https://fill.papermc.io/v3/projects/paper/versions/$ver/builds")
                val builds = JSONObject(buildsJson).getJSONArray("builds")
                if (builds.length() > 0) {
                    var lastBuild: JSONObject? = null
                    for (i in 0 until builds.length()) {
                        val b = builds.getJSONObject(i)
                        if ("STABLE" == b.optString("channel", "ALPHA")) { lastBuild = b; break }
                    }
                    if (lastBuild == null) lastBuild = builds.getJSONObject(0)
                    val buildObj = lastBuild!!
                    val buildNum = buildObj.getInt("id")
                    val downloads = buildObj.getJSONObject("downloads")
                    val download = when {
                        downloads.has("server:default") -> downloads.getJSONObject("server:default")
                        downloads.has("server:mojang") -> downloads.getJSONObject("server:mojang")
                        else -> downloads.getJSONObject(downloads.keys().next())
                    }

                    result.add(CoreVersion(
                        coreType = CoreType.PAPER,
                        version = "$ver-$buildNum",
                        mcVersion = ver,
                        buildNumber = buildNum,
                        downloadUrl = download.getString("url"),
                        fileSize = download.optLong("size", 0),
                        isRecommended = index == 0
                    ))
                }
            } catch (_: Exception) {}
        }
        return result.reversed()
    }

    private fun versionTuple(v: String): List<Int> =
        v.split(".").mapNotNull { it.toIntOrNull() }

    // ── Purpur API ──
    private fun fetchPurpurVersions(): List<CoreVersion> {
        val json = JSONObject(fetchUrl("https://api.purpurmc.org/v2/purpur"))
        val versions = json.getJSONObject("versions")
        val result = mutableListOf<CoreVersion>()
        val keys = versions.keys().asSequence().toList()

        for (key in keys) {
            try {
                val verJson = versions.getJSONObject(key)
                val builds = verJson.getJSONArray("builds")
                if (builds.length() > 0) {
                    val latestBuild = builds.getInt(builds.length() - 1)
                    result.add(CoreVersion(
                        coreType = CoreType.PURPUR,
                        version = "$key-$latestBuild",
                        mcVersion = key,
                        buildNumber = latestBuild,
                        downloadUrl = "https://api.purpurmc.org/v2/purpur/$key/$latestBuild/download",
                        isRecommended = key == keys.last()
                    ))
                }
            } catch (_: Exception) {}
        }
        return result.reversed()
    }

    // ── Pufferfish API ──
    private fun fetchPufferfishVersions(): List<CoreVersion> {
        val result = mutableListOf<CoreVersion>()
        for (mcVer in listOf("1.20.4", "1.20.2", "1.20.1", "1.19.4", "1.19.3")) {
            result.add(CoreVersion(
                coreType = CoreType.PUFFERFISH,
                version = mcVer,
                mcVersion = mcVer,
                downloadUrl = "https://pufferfish.host/downloads/pufferfish/builds/latest/$mcVer",
                isRecommended = mcVer == "1.20.4"
            ))
        }
        return result
    }

    // ── Spigot API ──
    private fun fetchSpigotVersions(): List<CoreVersion> {
        return listOf(
            CoreVersion(CoreType.SPIGOT, "1.20.4", "1.20.4",
                downloadUrl = "https://download.getbukkit.org/spigot/spigot-1.20.4.jar", isRecommended = true),
            CoreVersion(CoreType.SPIGOT, "1.20.2", "1.20.2",
                downloadUrl = "https://download.getbukkit.org/spigot/spigot-1.20.2.jar"),
            CoreVersion(CoreType.SPIGOT, "1.20.1", "1.20.1",
                downloadUrl = "https://download.getbukkit.org/spigot/spigot-1.20.1.jar"),
            CoreVersion(CoreType.SPIGOT, "1.19.4", "1.19.4",
                downloadUrl = "https://download.getbukkit.org/spigot/spigot-1.19.4.jar"),
        )
    }

    // ── Vanilla API ──
    private fun fetchVanillaVersions(): List<CoreVersion> {
        val manifest = JSONObject(fetchUrl("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json"))
        val versions = manifest.getJSONArray("versions")
        val result = mutableListOf<CoreVersion>()

        for (i in 0 until versions.length()) {
            val entry = versions.getJSONObject(i)
            if (entry.getString("type") == "release") {
                val ver = entry.getString("id")
                try {
                    val details = JSONObject(fetchUrl(entry.getString("url")))
                    val server = details.getJSONObject("downloads").getJSONObject("server")
                    result.add(CoreVersion(
                        coreType = CoreType.VANILLA,
                        version = ver,
                        mcVersion = ver,
                        downloadUrl = server.getString("url"),
                        fileSize = server.optLong("size", 0),
                        isRecommended = ver == "1.20.4"
                    ))
                } catch (_: Exception) {}
            }
        }
        return result.take(20)
    }

    // ── Nukkit API ──
    private fun fetchNukkitVersions(): List<CoreVersion> {
        return listOf(
            CoreVersion(CoreType.NUKKIT, "1.0.0", "基岩版1.20",
                downloadUrl = "https://ci.opencollab.dev/job/NukkitX/job/Nukkit/job/master/lastSuccessfulBuild/artifact/target/nukkit-1.0-SNAPSHOT.jar",
                isRecommended = true),
        )
    }

    // ── PocketMine API ──
    private fun fetchPocketmineVersions(): List<CoreVersion> {
        return listOf(
            CoreVersion(CoreType.POCKETMINE, "5.0.0", "基岩版1.20",
                downloadUrl = "https://github.com/pmmp/PocketMine-MP/releases/download/5.0.0/PocketMine-MP.phar",
                isRecommended = true),
        )
    }

    // ── Modrinth Search ──
    private fun searchModrinth(query: String): List<PluginInfo> {
        val url = "https://api.modrinth.org/v2/search?query=${java.net.URLEncoder.encode(query, "UTF-8")}" +
                "&limit=20&facets=%5B%5B%22project_type%3Aplugin%22%5D%5D"
        val json = JSONObject(fetchUrl(url))
        val hits = json.getJSONArray("hits")
        val results = mutableListOf<PluginInfo>()

        for (i in 0 until hits.length()) {
            results.add(parseModrinthHit(hits.getJSONObject(i)))
        }
        return results
    }

    private fun parseModrinthHit(hit: JSONObject): PluginInfo {
        return PluginInfo(
            id = hit.optString("project_id", hit.optString("slug", "")),
            name = hit.optString("title", "未知"),
            version = hit.optString("latest_version", "latest"),
            description = hit.optString("description", ""),
            author = hit.optString("author", "未知"),
            fileName = "${hit.optString("title", "plugin")}.jar",
            downloadUrl = null,
            source = PluginSource.MODRINTH,
            iconUrl = hit.optString("icon_url"),
            downloadsCount = hit.optLong("downloads", 0),
        )
    }

    // ── Spiget Search ──
    private fun searchSpiget(query: String): List<PluginInfo> {
        val url = "https://api.spiget.org/v2/search/resources/${java.net.URLEncoder.encode(query, "UTF-8")}?size=20"
        val json = JSONArray(fetchUrl(url))
        val results = mutableListOf<PluginInfo>()

        for (i in 0 until json.length()) {
            results.add(parseSpigetItem(json.getJSONObject(i)))
        }
        return results
    }

    private fun parseSpigetItem(item: JSONObject): PluginInfo {
        val file = item.optJSONObject("file")
        return PluginInfo(
            id = item.optString("id", ""),
            name = item.optString("name", "未知"),
            version = item.optJSONObject("version")?.optString("name") ?: "latest",
            description = item.optString("tag", ""),
            author = item.optJSONObject("author")?.optString("name") ?: "未知",
            fileName = file?.optString("name") ?: "${item.optString("name", "plugin")}.jar",
            downloadUrl = null,
            source = PluginSource.SPIGET,
            downloadsCount = item.optLong("downloads", 0),
        )
    }

    data class ResolvedDownload(
        val url: String,
        val fileName: String,
        val fileSize: Long,
    )

    suspend fun resolvePluginDownload(plugin: PluginInfo, gameVersion: String?): ResolvedDownload? =
        withContext(Dispatchers.IO) {
            try {
                when (plugin.source) {
                    PluginSource.MODRINTH -> resolveModrinthDownload(plugin.id, gameVersion)
                    PluginSource.SPIGET -> resolveSpigetDownload(plugin.id)
                    else -> null
                }
            } catch (_: Exception) { null }
        }

    private fun resolveModrinthDownload(projectId: String, gameVersion: String?): ResolvedDownload? {
        val versions = JSONArray(fetchUrl("https://api.modrinth.org/v2/project/$projectId/version"))
        if (versions.length() == 0) return null

        for (i in 0 until versions.length()) {
            val v = versions.getJSONObject(i)
            if (gameVersion != null && v.has("game_versions")) {
                val gvs = v.getJSONArray("game_versions")
                var match = false
                for (j in 0 until gvs.length()) {
                    if (gvs.getString(j).startsWith(gameVersion)) { match = true; break }
                }
                if (!match) continue
            }
            val files = v.optJSONArray("files")
            if (files != null && files.length() > 0) {
                val file = files.getJSONObject(0)
                return ResolvedDownload(
                    url = file.getString("url"),
                    fileName = file.optString("filename", "plugin.jar"),
                    fileSize = file.optLong("size", 0),
                )
            }
        }
        return null
    }

    private fun resolveSpigetDownload(resourceId: String): ResolvedDownload {
        return ResolvedDownload(
            url = "https://api.spiget.org/v2/resources/$resourceId/download",
            fileName = "plugin-$resourceId.jar",
            fileSize = 0,
        )
    }

    private fun fetchUrl(urlString: String): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.setRequestProperty("User-Agent", "McKaiFu/1.0")
        return conn.inputStream.bufferedReader().readText()
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec < 1024 -> "$bytesPerSec B/s"
            bytesPerSec < 1024 * 1024 -> "${bytesPerSec / 1024} KB/s"
            else -> String.format("%.1f MB/s", bytesPerSec / (1024.0 * 1024))
        }
    }

    fun resetProgress() {
        _downloadProgress.value = DownloadProgress()
    }
}
