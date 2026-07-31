package com.mckaifu.app.service

import com.mckaifu.app.data.model.FileEntry
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FileManagerService {

    fun listFiles(directory: File, showHidden: Boolean = false): List<FileEntry> {
        if (!directory.exists() || !directory.isDirectory) return emptyList()

        val files = directory.listFiles() ?: return emptyList()
        return files
            .filter { showHidden || !it.name.startsWith(".") }
            .map { file ->
                FileEntry(
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = file.isDirectory,
                    size = if (file.isFile) file.length() else 0L,
                    lastModified = file.lastModified(),
                    permission = getPermissions(file),
                    extension = if (file.isFile) file.extension else ""
                )
            }
            .sortedWith(compareByDescending<FileEntry> { it.isDirectory }.thenBy { it.name.lowercase() })
    }

    fun listFiles(path: String, showHidden: Boolean = false): List<FileEntry> =
        listFiles(File(path), showHidden)

    fun readFile(file: File): String? {
        return try {
            if (file.exists() && file.isFile) file.readText() else null
        } catch (e: Exception) { null }
    }

    fun readFile(path: String): String? = readFile(File(path))

    fun writeFile(file: File, content: String): Boolean {
        return try {
            file.parentFile?.mkdirs()
            file.writeText(content)
            true
        } catch (e: Exception) { false }
    }

    fun writeFile(path: String, content: String): Boolean = writeFile(File(path), content)

    fun delete(path: File): Boolean {
        return try {
            if (path.isDirectory) path.deleteRecursively() else path.delete()
        } catch (e: Exception) { false }
    }

    fun delete(path: String): Boolean = delete(File(path))

    fun rename(path: File, newName: String): Boolean {
        return try {
            val newPath = File(path.parent, newName)
            path.renameTo(newPath)
        } catch (e: Exception) { false }
    }

    fun rename(path: String, newName: String): Boolean = rename(File(path), newName)

    fun createDirectory(parent: File, name: String): Boolean {
        return try {
            File(parent, name).mkdirs()
        } catch (e: Exception) { false }
    }

    fun createDirectory(path: String): Boolean {
        return try {
            File(path).mkdirs()
        } catch (e: Exception) { false }
    }

    fun createFile(parent: File, name: String): Boolean {
        return try {
            File(parent, name).createNewFile()
        } catch (e: Exception) { false }
    }

    fun createFile(path: String): Boolean {
        return try {
            File(path).createNewFile()
        } catch (e: Exception) { false }
    }

    fun copy(source: File, destination: File): Boolean {
        return try {
            if (source.isDirectory) {
                source.copyRecursively(destination, overwrite = true)
                true
            } else {
                source.copyTo(destination, overwrite = true)
                true
            }
        } catch (e: Exception) { false }
    }

    fun getFileSizeString(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }

    fun formatFileSize(bytes: Long): String = getFileSizeString(bytes)

    fun getDateString(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun getPermissions(file: File): String {
        val sb = StringBuilder()
        sb.append(if (file.isDirectory) 'd' else '-')
        sb.append(if (file.canRead()) 'r' else '-')
        sb.append(if (file.canWrite()) 'w' else '-')
        sb.append(if (file.canExecute()) 'x' else '-')
        return sb.toString()
    }

    fun getEditableExtensions(): List<String> = listOf(
        "txt", "yml", "yaml", "json", "xml", "properties", "conf",
        "cfg", "ini", "toml", "md", "log", "sh", "bat", "kts", "gradle",
        "java", "kt", "py", "js", "html", "css", "sql", "hocon"
    )

    fun isTextFile(extension: String): Boolean {
        return extension.lowercase() in getEditableExtensions()
    }

    fun isEditableFile(fileName: String): Boolean {
        val dotIndex = fileName.lastIndexOf('.')
        return if (dotIndex >= 0) isTextFile(fileName.substring(dotIndex + 1)) else false
    }
}
