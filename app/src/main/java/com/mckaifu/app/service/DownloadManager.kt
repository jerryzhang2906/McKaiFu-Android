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
                android.util.Log.d("mckaifu-core", "$coreType -> ${versions.size} versions, first=${versions.firstOrNull()?.mcVersion}")
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

    // ── Paper API (fill.papermc.io mirror) ──
    private suspend fun fetchPaperVersions(): List<CoreVersion> = coroutineScope {
        val manifest = try {
            JSONObject(fetchUrl("https://fill.papermc.io/v3/projects/paper/versions"))
        } catch (_: Exception) { return@coroutineScope emptyList() }
        val versionsArr = manifest.getJSONArray("versions")
        val mcVersions = mutableListOf<String>()
        for (i in 0 until versionsArr.length()) {
            val v = versionsArr.getJSONObject(i)
            val status = v.optJSONObject("support")?.optString("status", "") ?: ""
            if (status == "RETIRED" || status == "LEGACY") continue
            mcVersions.add(v.getJSONObject("version").getString("id"))
        }
        mcVersions.sortWith(versionComparator(reverse = true))
        // 只对最近 25 个拉 builds(并行),避免数百次串行请求
        val recent = mcVersions.take(25)
        val deferreds = recent.map { ver ->
            async(Dispatchers.IO) {
                try {
                    val raw = fetchUrl("https://fill.papermc.io/v3/projects/paper/versions/$ver/builds")
                    val builds = JSONArray(raw)
                    if (builds.length() == 0) return@async null
                    var chosen: JSONObject? = null
                    for (i in 0 until builds.length()) {
                        val b = builds.getJSONObject(i)
                        if (b.optString("channel", "ALPHA") == "STABLE") { chosen = b; break }
                    }
                    if (chosen == null) chosen = builds.getJSONObject(0)
                    val buildObj = chosen!!
                    val buildNum = buildObj.getInt("id")
                    val downloads = buildObj.getJSONObject("downloads")
                    val download = when {
                        downloads.has("server:default") -> downloads.getJSONObject("server:default")
                        downloads.has("server:mojang") -> downloads.getJSONObject("server:mojang")
                        else -> downloads.getJSONObject(downloads.keys().next())
                    }
                    CoreVersion(
                        coreType = CoreType.PAPER,
                        version = "$ver-$buildNum",
                        mcVersion = ver,
                        buildNumber = buildNum,
                        downloadUrl = download.getString("url"),
                        fileSize = download.optLong("size", 0),
                    )
                } catch (_: Exception) { null }
            }
        }
        val result = deferreds.awaitAll().filterNotNull()
        if (result.isEmpty()) return@coroutineScope emptyList()
        // 按 mcVersion 倒序(最新在最前)
        val sorted = result.sortedWith(Comparator { a, b ->
            val ta = versionTuple(a.mcVersion)
            val tb = versionTuple(b.mcVersion)
            for (i in 0 until maxOf(ta.size, tb.size)) {
                val x = ta.getOrNull(i) ?: 0
                val y = tb.getOrNull(i) ?: 0
                if (x != y) return@Comparator y.compareTo(x)
            }
            0
        })
        sorted.mapIndexed { idx, v -> v.copy(isRecommended = idx == 0) }
    }

    private fun versionTuple(v: String): List<Int> =
        v.split(".").mapNotNull { it.toIntOrNull() }

    private fun versionComparator(reverse: Boolean = false) = Comparator<String> { a, b ->
        val ta = versionTuple(a)
        val tb = versionTuple(b)
        for (i in 0 until maxOf(ta.size, tb.size)) {
            val x = ta.getOrNull(i) ?: 0
            val y = tb.getOrNull(i) ?: 0
            if (x != y) {
                return@Comparator if (reverse) y.compareTo(x) else x.compareTo(y)
            }
        }
        0
    }

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

    // ── Pufferfish (官方 Jenkins CI,自动爬取) ──
    private suspend fun fetchPufferfishVersions(): List<CoreVersion> = coroutineScope {
        val jobs = try {
            val root = JSONObject(fetchUrl("https://ci.pufferfish.host/api/json?tree=jobs[name,url]"))
            val arr = root.getJSONArray("jobs")
            (0 until arr.length()).map { arr.getJSONObject(it).optString("name", "") }
                .filter { it.startsWith("Pufferfish-") && !it.startsWith("Pufferfish-Purpur") }
        } catch (_: Exception) { emptyList() }
        if (jobs.isEmpty()) return@coroutineScope emptyList()

        jobs.map { job ->
            async(Dispatchers.IO) {
                try {
                    val buildUrl = "https://ci.pufferfish.host/job/$job/lastSuccessfulBuild"
                    val json = JSONObject(fetchUrl(
                        "$buildUrl/api/json?tree=number,url,artifacts[fileName,relativePath]"
                    ))
                    val arts = json.optJSONArray("artifacts")
                    if (arts == null || arts.length() == 0) return@async null
                    val a = arts.getJSONObject(0)
                    val fileName = a.optString("fileName", "")
                    val relativePath = a.optString("relativePath", fileName)
                    val buildNum = json.optInt("number", 0)
                    val mcVer = Regex("paperclip-(\\d+\\.\\d+(?:\\.\\d+)?)").find(fileName)?.groupValues?.get(1)
                        ?: job.removePrefix("Pufferfish-")
                    CoreVersion(
                        coreType = CoreType.PUFFERFISH,
                        version = "$mcVer-$buildNum",
                        mcVersion = mcVer,
                        buildNumber = buildNum,
                        downloadUrl = "${json.optString("url", "$buildUrl/")}artifact/$relativePath",
                    )
                } catch (_: Exception) { null }
            }
        }.awaitAll().filterNotNull()
            .sortedWith(Comparator { a, b ->
                val ta = versionTuple(a.mcVersion)
                val tb = versionTuple(b.mcVersion)
                for (i in 0 until maxOf(ta.size, tb.size)) {
                    val x = ta.getOrNull(i) ?: 0
                    val y = tb.getOrNull(i) ?: 0
                    if (x != y) return@Comparator y.compareTo(x)
                }
                0
            })
            .mapIndexed { i, v -> v.copy(isRecommended = i == 0) }
    }

    // ── Spigot (getbukkit.org 页面解析,自动爬取) ──
    private fun fetchSpigotVersions(): List<CoreVersion> {
        val html = try {
            fetchUrl("https://getbukkit.org/download/spigot")
        } catch (_: Exception) { return emptyList() }

        val result = mutableListOf<CoreVersion>()
        // 每个 download-pane 块:Version(h2) / Size(h3) / 下载按钮 href="/get/{token}"
        val panePattern = Regex(
            "class=\"download-pane\".*?<h2>([^<]+)</h2>.*?<h3>([^<]+)</h3>.*?href=\"(https://getbukkit\\.org/get/[^\"]+)\"",
            RegexOption.DOT_MATCHES_ALL
        )
        for (m in panePattern.findAll(html)) {
            try {
                val mcVer = m.groupValues[1].trim()
                val sizeText = m.groupValues[2].trim()
                val downloadUrl = m.groupValues[3].trim()
                val sizeMb = sizeText.substringBefore("MB").trim().toFloatOrNull() ?: 0f
                if (mcVer.matches(Regex("\\d+\\.\\d+(\\.\\d+)?"))) {
                    result.add(CoreVersion(
                        coreType = CoreType.SPIGOT,
                        version = mcVer,
                        mcVersion = mcVer,
                        downloadUrl = downloadUrl,
                        fileSize = (sizeMb * 1024 * 1024).toLong(),
                    ))
                }
            } catch (_: Exception) {}
        }
        return result
            .sortedWith(Comparator { a, b ->
                val ta = versionTuple(a.mcVersion)
                val tb = versionTuple(b.mcVersion)
                for (i in 0 until maxOf(ta.size, tb.size)) {
                    val x = ta.getOrNull(i) ?: 0
                    val y = tb.getOrNull(i) ?: 0
                    if (x != y) return@Comparator y.compareTo(x)
                }
                0
            })
            .mapIndexed { i, v -> v.copy(isRecommended = i == 0) }
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

    // ── Nukkit (Jenkins CI, auto latest) ──
    private fun fetchNukkitVersions(): List<CoreVersion> {
        return try {
            val job = "https://ci.opencollab.dev/job/NukkitX/job/Nukkit/job/master/lastSuccessfulBuild"
            val json = JSONObject(fetchUrl("$job/api/json?tree=number,artifacts[fileName,relativePath]"))
            val buildNum = json.optInt("number", 0)
            val arts = json.optJSONArray("artifacts")
            var fileName = "nukkit-1.0-SNAPSHOT.jar"
            if (arts != null && arts.length() > 0) {
                val a = arts.getJSONObject(0)
                fileName = a.optString("relativePath", a.optString("fileName", fileName))
            }
            listOf(CoreVersion(
                coreType = CoreType.NUKKIT,
                version = "build-$buildNum",
                mcVersion = "基岩版",
                buildNumber = buildNum,
                downloadUrl = "$job/artifact/$fileName",
                isRecommended = true,
            ))
        } catch (_: Exception) {
            listOf(CoreVersion(
                coreType = CoreType.NUKKIT,
                version = "latest",
                mcVersion = "基岩版",
                downloadUrl = "https://ci.opencollab.dev/job/NukkitX/job/Nukkit/job/master/lastSuccessfulBuild/artifact/target/nukkit-1.0-SNAPSHOT.jar",
                isRecommended = true,
            ))
        }
    }

    // ── PocketMine (GitHub releases API, auto crawl) ──
    private fun fetchPocketmineVersions(): List<CoreVersion> {
        val releases = try {
            JSONArray(fetchUrl("https://api.github.com/repos/pmmp/PocketMine-MP/releases?per_page=20"))
        } catch (_: Exception) { return emptyList() }
        val result = mutableListOf<CoreVersion>()
        for (i in 0 until releases.length()) {
            try {
                val rel = releases.getJSONObject(i)
                if (rel.optBoolean("draft", false) || rel.optBoolean("prerelease", false)) continue
                val tag = rel.optString("tag_name", "") ?: ""
                if (tag.isEmpty()) continue
                var dlUrl = ""
                var size = 0L
                val assets = rel.optJSONArray("assets")
                if (assets != null) {
                    for (j in 0 until assets.length()) {
                        val a = assets.getJSONObject(j)
                        val name = a.optString("name", "")
                        if (name.endsWith(".phar")) {
                            dlUrl = a.optString("browser_download_url", "")
                            size = a.optLong("size", 0)
                            break
                        }
                    }
                }
                if (dlUrl.isEmpty()) continue
                result.add(CoreVersion(
                    coreType = CoreType.POCKETMINE,
                    version = tag,
                    mcVersion = "基岩版",
                    downloadUrl = dlUrl,
                    fileSize = size,
                    isRecommended = result.isEmpty(),
                ))
            } catch (_: Exception) {}
        }
        return result
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
