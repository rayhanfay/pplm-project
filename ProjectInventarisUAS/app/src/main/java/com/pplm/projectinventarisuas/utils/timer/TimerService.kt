package com.pplm.projectinventarisuas.utils.timer

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.pplm.projectinventarisuas.ui.studentsection.borrowing.BorrowingTimerActivity

class TimerService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var seconds = 0
    private var running = false
    private val notificationId = 1

    private val runnable = object : Runnable {
        override fun run() {
            if (running) {
                seconds++
                TimerState.updateTime(seconds)
                updateNotification()
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called with action: ${intent?.action}")

        when (intent?.action) {
            ACTION_START -> {
                if (!running) {
                    Log.d(TAG, "Timer started")
                    running = true
                    startForeground(notificationId, createNotification())
                    handler.post(runnable)
                }
            }
            ACTION_STOP -> {
                Log.d(TAG, "Timer stopped")
                running = false
                stopForeground(true)
                stopSelf()
            }
            ACTION_RESET -> {
                Log.d(TAG, "Timer reset")
                running = false
                seconds = 0
                TimerState.reset()
                updateNotification()
            }
            ACTION_UPDATE -> {
                TimerState.updateTime(seconds)
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for Timer Service"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, BorrowingTimerActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Timer Running")
            .setContentText(formatTime(seconds))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        if (running) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(notificationId, createNotification())
        }
    }

    @SuppressLint("DefaultLocale")
    private fun formatTime(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        running = false
        handler.removeCallbacks(runnable)
    }

    companion object {
        private const val TAG = "TimerService"
        const val CHANNEL_ID = "timer_channel"
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
        const val ACTION_RESET = "RESET"
        const val ACTION_UPDATE = "UPDATE"
    }
}