package com.mckaifu.app.data.model

data class FileEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0L,
    val lastModified: Long = System.currentTimeMillis(),
    val permission: String = "rw-r--r--",
    val extension: String = ""
)
