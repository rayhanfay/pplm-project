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
import android.location.Location
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
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
        private const val RADAR_CHANNEL_ID = "radar_channel" // Channel baru untuk radar
        private const val RADAR_CHANNEL_NAME = "Location Alerts" // Nama channel radar
        private const val NOTIFICATION_ID = 1001
        private const val RADAR_NOTIFICATION_ID = 2001 // ID berbeda untuk notifikasi radar
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
            "OUT_OF_RANGE_ACTION" -> {
                Log.d(TAG, "User is outside permitted radius")
                showOutOfRangeNotification(context, borrowingId)
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
        createNotificationChannel(context, CHANNEL_ID, CHANNEL_NAME, "Channel for reminder notifications")

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

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showOutOfRangeNotification(context: Context, borrowingId: String?) {
        if (borrowingId == null) {
            Log.w(TAG, "showOutOfRangeNotification: borrowingId is null")
            return
        }

        Log.d(TAG, "Showing out of range notification")
        createNotificationChannel(context, RADAR_CHANNEL_ID, RADAR_CHANNEL_NAME,
            "Channel for location alert notifications")

        val notificationIntent = Intent(context, Class.forName("com.pplm.projectinventarisuas.ui.studentsection.borrowing.BorrowingTimerActivity")).apply {
            putExtra("BORROWING_ID", borrowingId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, RADAR_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Peringatan Lokasi")
            .setContentText("Anda berada di luar area yang diperbolehkan. Harap kembali ke lokasi yang ditentukan.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)

        with(NotificationManagerCompat.from(context)) {
            notify(RADAR_NOTIFICATION_ID, builder.build())
        }

        updateOutOfRangeStatus(borrowingId, true)
    }

    private fun updateOutOfRangeStatus(borrowingId: String, isOutOfRange: Boolean) {
        val borrowingRef = FirebaseDatabase.getInstance().getReference("borrowing")
            .child(borrowingId)
        borrowingRef.child("out_of_range").setValue(isOutOfRange)
        Log.d(TAG, "Updated out_of_range status to $isOutOfRange for borrowingId: $borrowingId")
    }

    private fun createNotificationChannel(context: Context, channelId: String, channelName: String, description: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                this.description = description
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created: $channelId")
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

                    checkDistanceToTarget(context, location, borrowingId)

                    scheduleNextLocationSend(context, borrowingId)
                } else {
                    Log.w(TAG, "lastLocation is null, requesting single update")
                    val locationRequest =
                        com.google.android.gms.location.LocationRequest.Builder(
                            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 0
                        ).setMaxUpdates(1).build()
                    val singleUpdateCallback = object : com.google.android.gms.location.LocationCallback() {
                        override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                            val loc = result.lastLocation
                            if (loc != null) {
                                val latLng = "${loc.latitude},${loc.longitude}"
                                val borrowingRef = FirebaseDatabase.getInstance().getReference("borrowing")
                                    .child(borrowingId)
                                borrowingRef.child("last_location").setValue(latLng)
                                Log.d(TAG, "Location (single update) sent: $latLng for borrowingId: $borrowingId")

                                checkDistanceToTarget(context, loc, borrowingId)
                                scheduleNextLocationSend(context, borrowingId)
                            } else {
                                Log.w(TAG, "Single update location is still null")
                            }
                            fusedLocationClient.removeLocationUpdates(this)
                        }
                    }
                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        singleUpdateCallback,
                        null
                    )
                }
            }.addOnFailureListener {
                Log.e(TAG, "Failed to get location: ${it.message}")
            }
        } else {
            Toast.makeText(context, "Location permission not granted", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "Location permission not granted")
        }
    }

    private fun checkDistanceToTarget(context: Context, currentLocation: Location, borrowingId: String) {
        val targetLatitude = 0.4801978305934569
        val targetLongitude = 101.37665907336893
        val radiusInMeters = 50.0f

        val targetLocation = Location("Target").apply {
            latitude = targetLatitude
            longitude = targetLongitude
        }

        val distanceInMeters = currentLocation.distanceTo(targetLocation)
        Log.d(TAG, "Distance to target: $distanceInMeters meters for borrowingId: $borrowingId")

        val borrowingRef = FirebaseDatabase.getInstance().getReference("borrowing")
            .child(borrowingId)

        borrowingRef.child("out_of_range").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val wasOutOfRange = snapshot.getValue(Boolean::class.java) ?: false

                if (distanceInMeters > radiusInMeters && !wasOutOfRange) {
                    val intent = Intent(context, ReminderReceiver::class.java).apply {
                        action = "OUT_OF_RANGE_ACTION"
                        putExtra("BORROWING_ID", borrowingId)
                    }
                    context.sendBroadcast(intent)
                } else if (distanceInMeters <= radiusInMeters && wasOutOfRange) {
                    cancelOutOfRangeNotification(context)
                    updateOutOfRangeStatus(borrowingId, false)
                    Log.d(TAG, "User returned to permitted radius")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to read out_of_range status: ${error.message}")
            }
        })
    }

    private fun cancelOutOfRangeNotification(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(RADAR_NOTIFICATION_ID)
        Log.d(TAG, "Cancelled out of range notification")
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
