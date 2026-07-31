package com.mckaifu.app.service

import android.content.Context
import org.tukaani.xz.XZInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

object JavaRuntimeManager {

    val supportedVersions: List<Int> = listOf(17, 21, 25)

    data class RuntimeStatus(
        val version: Int,
        val installed: Boolean,
        val bundled: Boolean,
        val javaPath: String? = null,
    )

    fun runtimeDir(context: Context, version: Int): File {
        return File(context.filesDir, "jdk/$version")
    }

    fun jreHome(context: Context, version: Int): String? {
        val dir = runtimeDir(context, version)
        if (File(dir, "lib/libjli.so").exists()) return dir.absolutePath
        return null
    }

    fun javaExecutable(context: Context, version: Int): String? {
        val dir = runtimeDir(context, version)
        val java = File(dir, "bin/java")
        if (java.exists()) return java.absolutePath
        val javaExe = File(dir, "bin/java.exe")
        if (javaExe.exists()) return javaExe.absolutePath
        return null
    }

    fun isBionic(context: Context, version: Int): Boolean {
        return File(runtimeDir(context, version), "lib/libjli.so").exists()
    }

    fun isInstalled(context: Context, version: Int): Boolean {
        return jreHome(context, version) != null || javaExecutable(context, version) != null
    }

    fun isBundledInAssets(context: Context, version: Int): Boolean {
        return try {
            context.assets.open("jdk/$version.tar.xz").use { true }
        } catch (_: Exception) {
            false
        }
    }

    fun getStatus(context: Context, version: Int): RuntimeStatus {
        val installed = isInstalled(context, version)
        val bundled = isBundledInAssets(context, version)
        return RuntimeStatus(
            version = version,
            installed = installed,
            bundled = bundled,
            javaPath = jreHome(context, version) ?: javaExecutable(context, version)
        )
    }

    fun getStatuses(context: Context): List<RuntimeStatus> {
        return supportedVersions.map { getStatus(context, it) }
    }

    fun hasAnyRuntime(context: Context): Boolean {
        return supportedVersions.any { isInstalled(context, it) }
    }

    fun hasAnyBundled(context: Context): Boolean {
        return supportedVersions.any { isBundledInAssets(context, it) }
    }

    fun findAvailableVersion(context: Context): Int? {
        return supportedVersions.firstOrNull { isInstalled(context, it) }
    }

    fun ensureRuntime(context: Context, version: Int): String? {
        jreHome(context, version)?.let { home ->
            patchRelease(File(home))
            return home
        }
        javaExecutable(context, version)?.let { return it }

        if (isBundledInAssets(context, version)) {
            if (extractRuntime(context, version)) {
                return jreHome(context, version) ?: javaExecutable(context, version)
            }
        }
        return null
    }

    fun deleteRuntime(context: Context, version: Int) {
        val target = runtimeDir(context, version)
        if (target.exists()) {
            target.deleteRecursively()
        }
    }

    fun extractRuntime(context: Context, version: Int): Boolean {
        return try {
            val target = runtimeDir(context, version)
            if (target.exists() && File(target, "lib/libjli.so").exists()) {
                chmodTree(target)
                patchRelease(target)
                true
            } else {
                if (target.exists()) target.deleteRecursively()
                target.mkdirs()
                context.assets.open("jdk/$version.tar.xz").use { input ->
                    extractTarXz(input, target)
                }
                chmodTree(target)
                patchRelease(target)
                File(target, "lib/libjli.so").exists()
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun patchRelease(target: File) {
        try {
            val release = File(target, "release")
            if (!release.exists()) return
            val text = release.readText(Charsets.UTF_8)
            val patched = text.replace(
                Regex("JAVA_RUNTIME_VERSION=\"[^\"]*\""),
                "JAVA_RUNTIME_VERSION=\"${cleanRuntimeVersion(target)}\""
            )
            if (patched != text) {
                release.writeText(patched, Charsets.UTF_8)
            }
        } catch (_: Exception) {}
    }

    private fun cleanRuntimeVersion(target: File): String {
        val javaVersion = try {
            File(target, "release").readText(Charsets.UTF_8)
                .substringAfter("JAVA_VERSION=\"").substringBefore("\"")
        } catch (_: Exception) { "17.0.10" }
        return if (javaVersion.isBlank()) "17.0.10" else javaVersion
    }

    private fun extractTarXz(input: InputStream, target: File) {
        val xz = XZInputStream(input)
        val buf = ByteArray(64 * 1024)
        val header = ByteArray(512)

        while (true) {
            var off = 0
            while (off < 512) {
                val n = xz.read(header, off, 512 - off)
                if (n < 0) return
                off += n
            }
            if (header.all { it == 0.toByte() }) return

            val type = header[156].toInt().toChar()
            val size = parseOctal(header, 124, 12)

            val rawName = String(header, 0, 100, Charsets.US_ASCII).trimEnd('\u0000', ' ')
            val name = rawName.substringAfter('/', missingDelimiterValue = rawName).trimEnd('/')
            if (name.isEmpty()) {
                skipBlock(xz, buf, size)
                continue
            }

            val entry = File(target, name)
            when (type) {
                '0', '7' -> {
                    entry.parentFile?.mkdirs()
                    FileOutputStream(entry).use { out -> copyBlock(xz, buf, out, size) }
                }
                '5' -> entry.mkdirs()
                'x', 'g' -> skipBlock(xz, buf, size)
                'L' -> skipBlock(xz, buf, size)
                '2' -> skipBlock(xz, buf, size)
                else -> skipBlock(xz, buf, size)
            }
        }
    }

    private fun parseOctal(header: ByteArray, offset: Int, length: Int): Long {
        var s = String(header, offset, length, Charsets.US_ASCII).trim(' ', '\u0000')
        if (s.endsWith("\u0000")) s = s.dropLast(1)
        return s.toLongOrNull(8) ?: 0L
    }

    private fun copyBlock(xz: XZInputStream, buf: ByteArray, out: OutputStream, size: Long) {
        var remaining = size
        while (remaining > 0) {
            val n = xz.read(buf, 0, minOf(buf.size.toLong(), remaining).toInt())
            if (n < 0) return
            out.write(buf, 0, n)
            remaining -= n
        }
        skipPadding(xz, buf, size)
    }

    private fun skipBlock(xz: XZInputStream, buf: ByteArray, size: Long) {
        var remaining = size
        while (remaining > 0) {
            val n = xz.read(buf, 0, minOf(buf.size.toLong(), remaining).toInt())
            if (n < 0) return
            remaining -= n
        }
        skipPadding(xz, buf, size)
    }

    private fun skipPadding(xz: XZInputStream, buf: ByteArray, size: Long) {
        val pad = ((512 - (size % 512)) % 512).toInt()
        var remaining = pad
        while (remaining > 0) {
            val n = xz.read(buf, 0, minOf(buf.size, remaining))
            if (n < 0) return
            remaining -= n
        }
    }

    private fun chmodTree(root: File) {
        root.walkTopDown().forEach { file ->
            try {
                val mode = if (file.isDirectory) "755" else if (file.name.endsWith(".so")) "755" else "644"
                Runtime.getRuntime().exec(arrayOf("chmod", mode, file.absolutePath)).waitFor()
            } catch (_: Exception) {}
        }
    }
}
