package com.mckaifu.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.mckaifu.app.data.repository.ServerRepository
import com.mckaifu.app.service.ServerEngine

class McKaiFuApp : Application() {

    lateinit var repository: ServerRepository
        private set

    lateinit var serverEngine: ServerEngine
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = ServerRepository(this)
        serverEngine = ServerEngine()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_SERVER,
                getString(R.string.notification_channel_server),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Minecraft服务器运行状态通知"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onTerminate() {
        serverEngine.shutdownAll()
        super.onTerminate()
    }

    companion object {
        const val CHANNEL_SERVER = "server_status"

        lateinit var instance: McKaiFuApp
            private set
    }
}
