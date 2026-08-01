package com.mckaifu.app.service

import android.content.Context
import android.os.Build
import com.mckaifu.app.data.model.TunnelType
import java.io.File

object TunnelBinaryManager {

    const val FRPC_VERSION = "v0.51.3"

    private val ASSET_DIR = mapOf(
        TunnelType.PLAYIT to "playit",
        TunnelType.NGROK to "ngrok",
        TunnelType.SAKURA to "frpc",
        TunnelType.CUSTOM to "frpc",
        TunnelType.NATAPP to "natapp"
    )

    private val BINARY_NAME = mapOf(
        TunnelType.PLAYIT to "playit",
        TunnelType.NGROK to "ngrok",
        TunnelType.SAKURA to "frpc",
        TunnelType.CUSTOM to "frpc",
        TunnelType.NATAPP to "natapp"
    )

    fun pickAbi(): String? {
        val abis = Build.SUPPORTED_ABIS.toList()
        return when {
            "x86_64" in abis -> "x86_64"
            "arm64-v8a" in abis -> "arm64-v8a"
            else -> null
        }
    }

    fun binaryFile(context: Context, type: TunnelType): File {
        return File(File(context.filesDir, "bin"), BINARY_NAME[type] ?: "tunnel")
    }

    fun isBundled(context: Context, type: TunnelType): Boolean {
        val abi = pickAbi() ?: return false
        val dir = ASSET_DIR[type] ?: return false
        val name = BINARY_NAME[type] ?: return false
        return try {
            context.assets.open("tunnel/$dir/$abi/$name").use { true }
        } catch (_: Exception) {
            false
        }
    }

    fun isExtracted(context: Context, type: TunnelType): Boolean {
        val file = binaryFile(context, type)
        return file.exists() && file.canExecute()
    }

    fun ensureBinary(context: Context, type: TunnelType): File? {
        val file = binaryFile(context, type)
        if (file.exists() && file.canExecute()) return file

        val abi = pickAbi() ?: return null
        val dir = ASSET_DIR[type] ?: return null
        val name = BINARY_NAME[type] ?: return null
        return try {
            file.parentFile?.mkdirs()
            context.assets.open("tunnel/$dir/$abi/$name").use { input ->
                file.outputStream().use { input.copyTo(it) }
            }
            Runtime.getRuntime().exec(arrayOf("chmod", "755", file.absolutePath)).waitFor()
            if (file.canExecute()) file else null
        } catch (e: Exception) {
            null
        }
    }

    fun deleteBinary(context: Context, type: TunnelType) {
        val file = binaryFile(context, type)
        if (file.exists()) file.delete()
    }
}
