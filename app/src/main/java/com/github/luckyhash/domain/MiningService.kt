package com.github.luckyhash.domain

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.github.luckyhash.LuckyHashApplication
import com.github.luckyhash.MainActivity
import com.github.luckyhash.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MiningService : Service() {

    private val miningRepository: MiningRepository by inject()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        setupService()
    }

    private fun setupService() {
        serviceScope.launch {
            val config = miningRepository.miningConfig.first()
            startForeground(NOTIFICATION_ID, createNotification())
            miningRepository.startMining(config.threads)

            launch {
                miningRepository.miningStats.collect { stats ->
                    if (stats.isRunning) {
                        val notification = createNotification(
                            "Mining: ${String.format("%.2f", stats.hashRate)} H/s"
                        )
                        startForeground(NOTIFICATION_ID, notification)
                    }
                }
            }
        }
    }

    private fun createNotification(
        contentText: String = "Mining in progress"
    ): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, LuckyHashApplication.MINING_CHANNEL_ID)
            .setContentTitle("LuckyHash")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.launch {
            miningRepository.stopMining()
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 1
    }
}