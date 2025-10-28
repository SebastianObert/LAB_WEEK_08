package com.example.lab_week_08

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class SecondNotificationService : Service() {

    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var serviceHandler: Handler

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationBuilder = startForegroundService()

        val handlerThread = HandlerThread("ThirdThread").apply { start() }
        serviceHandler = Handler(handlerThread.looper)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val returnValue = super.onStartCommand(intent, flags, startId)
        val id = intent?.getStringExtra(EXTRA_ID) ?: throw IllegalStateException("Channel ID must be provided")

        serviceHandler.post {
            countDownFromFiveToZero(notificationBuilder) // Menggunakan timer 5 detik untuk pembeda
            notifyCompletion(id)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return returnValue
    }

    private fun startForegroundService(): NotificationCompat.Builder {
        val pendingIntent = getPendingIntent()
        val channelId = createNotificationChannel()
        val notificationBuilder = getNotificationBuilder(pendingIntent, channelId)

        startForeground(NOTIFICATION_ID_2, notificationBuilder.build())
        return notificationBuilder
    }

    private fun getPendingIntent(): PendingIntent {
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), flag)
    }

    private fun createNotificationChannel(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "002" // Channel ID berbeda
            val channelName = "002 Channel"
            val channelPriority = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, channelPriority)
            val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            service.createNotificationChannel(channel)
            return channelId
        }
        return ""
    }

    private fun getNotificationBuilder(pendingIntent: PendingIntent, channelId: String) =
        NotificationCompat.Builder(this, channelId)
            .setContentTitle("Third worker process is done")
            .setContentText("Another check it out!")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setTicker("Third worker process is done, check it out!")
            .setOngoing(true)

    private fun countDownFromFiveToZero(notificationBuilder: NotificationCompat.Builder) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        for (i in 5 downTo 0) {
            Thread.sleep(1000L)
            notificationBuilder.setContentText("$i seconds until final warning").setSilent(true)
            notificationManager.notify(NOTIFICATION_ID_2, notificationBuilder.build())
        }
    }

    private fun notifyCompletion(id: String) {
        Handler(Looper.getMainLooper()).post {
            trackingCompletion.value = id
        }
    }

    companion object {
        const val NOTIFICATION_ID_2 = 0xCA8
        const val EXTRA_ID = "Id"
        val trackingCompletion = MutableLiveData<String>()
    }
}