package com.mckaifu.app.util

import com.mckaifu.app.data.model.BackupEntry
import com.mckaifu.app.data.model.BackupType
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupManager {

    data class BackupProgress(
        val isRunning: Boolean = false,
        val progress: Float = 0f,
        val currentFile: String = "",
        val message: String = ""
    )

    fun createBackup(worldDir: File, backupDir: File, name: String = "", type: BackupType = BackupType.MANUAL): BackupEntry? {
        if (!worldDir.exists()) return null

        try {
            backupDir.mkdirs()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "${name.ifBlank { worldDir.name }}_$timestamp.zip"
            val backupFile = File(backupDir, fileName)

            ZipOutputStream(FileOutputStream(backupFile)).use { zos ->
                worldDir.walkTopDown().forEach { file ->
                    val entryName = file.relativeTo(worldDir.parentFile).path.replace("\\", "/")
                    if (file.isFile) {
                        zos.putNextEntry(ZipEntry(entryName))
                        FileInputStream(file).use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }

            return BackupEntry(
                worldName = worldDir.name,
                fileName = fileName,
                sizeBytes = backupFile.length(),
                createdAt = System.currentTimeMillis(),
                type = type
            )
        } catch (e: Exception) {
            return null
        }
    }

    fun restoreBackup(backupFile: File, targetDir: File): Boolean {
        return try {
            if (targetDir.exists()) targetDir.deleteRecursively()
            targetDir.mkdirs()

            java.util.zip.ZipFile(backupFile).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    val file = File(targetDir, entry.name)
                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        zip.getInputStream(entry).use { it.copyTo(file.outputStream()) }
                    }
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun listBackups(backupDir: File): List<BackupEntry> {
        if (!backupDir.exists()) return emptyList()
        return backupDir.listFiles()
            ?.filter { it.name.endsWith(".zip") }
            ?.map { file ->
                BackupEntry(
                    fileName = file.name,
                    worldName = file.nameWithoutExtension,
                    sizeBytes = file.length(),
                    createdAt = file.lastModified(),
                    type = detectBackupType(file.name)
                )
            }
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()
    }

    fun deleteOldBackups(backupDir: File, retentionDays: Int) {
        val cutoff = System.currentTimeMillis() - retentionDays * 24 * 60 * 60 * 1000L
        backupDir.listFiles()
            ?.filter { it.name.endsWith(".zip") && it.lastModified() < cutoff }
            ?.forEach { it.delete() }
    }

    private fun detectBackupType(fileName: String): BackupType {
        return when {
            fileName.contains("auto") || fileName.contains("scheduled") -> BackupType.AUTO
            else -> BackupType.MANUAL
        }
    }
}
