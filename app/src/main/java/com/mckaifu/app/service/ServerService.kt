package com.mckaifu.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mckaifu.app.McKaiFuApp
import com.mckaifu.app.MainActivity
import com.mckaifu.app.R
import kotlinx.coroutines.*

class ServerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        val app = application as McKaiFuApp

        serviceScope.launch {
            app.repository.servers.collect { servers ->
                val onlineCount = servers.count { server ->
                    app.serverEngine.isRunning(server.id)
                }
                updateNotification(onlineCount, servers.size)
            }
        }

        return START_STICKY
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, McKaiFuApp.CHANNEL_SERVER)
            .setContentTitle("McKaiFu 开服大师")
            .setContentText("服务器管理服务运行中")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(online: Int, total: Int) {
        val notification = NotificationCompat.Builder(this, McKaiFuApp.CHANNEL_SERVER)
            .setContentTitle("McKaiFu 开服大师")
            .setContentText("$online/$total 个服务器在线")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
    }
}
