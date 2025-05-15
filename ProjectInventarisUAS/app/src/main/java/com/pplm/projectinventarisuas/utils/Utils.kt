package com.pplm.projectinventarisuas.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Locale

@SuppressLint("DefaultLocale")
fun formatTime(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return String.format("%02d:%02d:%02d", h, m, s)
}

fun generateBorrowingId(): String {
    val now = java.util.Date()
    val sdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
    return sdf.format(now)
}

fun getGreetingWithName(fullName: String): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val name = fullName.split(" ").first().replaceFirstChar { it.uppercase() }

    return when (hour) {
        in 4..10 -> "Good Morning $name"
        in 11..14 -> "Good Afternoon $name"
        in 15..18 -> "Good Evening $name"
        else -> "Good Night $name"
    }
}

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "reminder_channel"
        private const val CHANNEL_NAME = "Reminder Notifications"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "BorrowingLog"
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val borrowingId = intent.getStringExtra("BORROWING_ID")
        Log.d(TAG, "onReceive triggered with action: $action and borrowingId: $borrowingId")

        when (action) {
            "SEND_LOCATION_ACTION" -> {
                Log.d(TAG, "Calling sendLastLocation()")
                sendLastLocation(context, borrowingId)
            }
            else -> {
                Log.d(TAG, "Calling showReminderNotification()")
                showReminderNotification(context)
            }
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showReminderNotification(context: Context) {
        Log.d(TAG, "Showing reminder notification")
        createNotificationChannel(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Reminder")
            .setContentText("Waktu peminjaman akan segera berakhir!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for reminder notifications"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }

    private fun sendLastLocation(context: Context, borrowingId: String?) {
        if (borrowingId == null) {
            Log.w(TAG, "sendLastLocation: borrowingId is null")
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val latLng = "${location.latitude},${location.longitude}"
                    val borrowingRef = FirebaseDatabase.getInstance().getReference("borrowing")
                        .child(borrowingId)
                    borrowingRef.child("last_location").setValue(latLng)
                    Log.d(TAG, "Location sent: $latLng for borrowingId: $borrowingId")

                    scheduleNextLocationSend(context, borrowingId)
                } else {
                    Log.w(TAG, "Location is null")
                }
            }.addOnFailureListener {
                Log.e(TAG, "Failed to get location: ${it.message}")
            }
        } else {
            Toast.makeText(context, "Location permission not granted", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "Location permission not granted")
        }
    }

    private fun scheduleNextLocationSend(context: Context, borrowingId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val nextTime = System.currentTimeMillis() + (1 * 60 * 1000)

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "SEND_LOCATION_ACTION"
            putExtra("BORROWING_ID", borrowingId)
        }

        val requestCode = (borrowingId.hashCode() + 999) and 0xFFFFFFF
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, nextTime, pendingIntent)
            Log.d(TAG, "Alarm scheduled (non-exact) for borrowingId: $borrowingId at $nextTime")
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTime,
                    pendingIntent
                )
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, nextTime, pendingIntent)
            }
            Log.d(TAG, "Exact alarm scheduled for borrowingId: $borrowingId at $nextTime")
        }
    }
}