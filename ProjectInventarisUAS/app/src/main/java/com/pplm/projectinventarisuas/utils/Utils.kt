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
    val sdf = SimpleDateFormat("ssmmHHddMMyyyy", Locale.getDefault())
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
        private const val TIME_REMINDER_CHANNEL_ID = "time_reminder_channel"
        private const val TIME_REMINDER_CHANNEL_NAME = "Time Reminders"
        private const val OVERDUE_REMINDER_CHANNEL_ID = "overdue_reminder_channel"
        private const val OVERDUE_REMINDER_CHANNEL_NAME = "Overdue Reminders"
        private const val LOCATION_ALERT_CHANNEL_ID = "location_alert_channel"
        private const val LOCATION_ALERT_CHANNEL_NAME = "Location Alerts"

        private const val TIME_REMINDER_NOTIFICATION_ID = 1001
        private const val OVERDUE_REMINDER_NOTIFICATION_ID = 1002
        private const val LOCATION_ALERT_NOTIFICATION_ID = 2001

        const val ACTION_TIME_REMINDER = "TIME_REMINDER_ACTION"
        const val ACTION_OVERDUE_REMINDER = "OVERDUE_REMINDER_ACTION"
        const val ACTION_OUT_OF_RANGE = "OUT_OF_RANGE_ACTION"
        const val ACTION_SEND_LOCATION = "SEND_LOCATION_ACTION"

        const val REQUEST_CODE_TIME_REMINDER = 100
        const val REQUEST_CODE_OVERDUE_REMINDER = 150
        const val REQUEST_CODE_OUT_OF_RANGE = 200
        const val REQUEST_CODE_LOCATION_SEND = 300

        private const val TAG_MAIN = "ReminderReceiver"
        private const val TAG_TIME = "TimeReminder"
        private const val TAG_OVERDUE = "OverdueReminder"
        private const val TAG_LOCATION = "LocationAlert"
        private const val TAG_NOTIFICATION = "NotificationManager"
        private const val TAG_DATABASE = "DatabaseUpdate"
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val borrowingId = intent.getStringExtra("BORROWING_ID")

        Log.d(TAG_MAIN, "onReceive triggered - Action: $action, BorrowingID: $borrowingId")

        when (action) {
            ACTION_SEND_LOCATION -> {
                Log.d(TAG_LOCATION, "Processing send location action")
                sendLastLocation(context, borrowingId)
            }
            ACTION_OUT_OF_RANGE -> {
                Log.d(TAG_LOCATION, "Processing out of range action")
                showOutOfRangeNotification(context, borrowingId)
            }
            ACTION_TIME_REMINDER -> {
                val minutesRemaining = intent.getIntExtra("MINUTES_REMAINING", 0)
                Log.d(TAG_TIME, "Processing time reminder action - Minutes remaining: $minutesRemaining")
                showTimeReminderNotification(context, minutesRemaining)
            }
            ACTION_OVERDUE_REMINDER -> {
                val minutesOverdue = intent.getIntExtra("MINUTES_OVERDUE", 0)
                Log.d(TAG_OVERDUE, "Processing overdue reminder action - Minutes overdue: $minutesOverdue")
                showOverdueReminderNotification(context, minutesOverdue)
            }
            else -> {
                Log.w(TAG_MAIN, "Unknown action received: $action")
            }
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showTimeReminderNotification(context: Context, minutesRemaining: Int) {
        Log.d(TAG_TIME, "Creating time reminder notification for $minutesRemaining minutes remaining")

        createNotificationChannel(
            context,
            TIME_REMINDER_CHANNEL_ID,
            TIME_REMINDER_CHANNEL_NAME,
            "Notifications for borrowing time reminders"
        )

        val title = "Pengingat Waktu Peminjaman"
        val message = when (minutesRemaining) {
            30 -> "Waktu peminjaman akan berakhir dalam 30 menit!"
            15 -> "Waktu peminjaman akan berakhir dalam 15 menit!"
            5 -> "Waktu peminjaman akan berakhir dalam 5 menit!"
            else -> "Waktu peminjaman akan segera berakhir!"
        }

        val builder = NotificationCompat.Builder(context, TIME_REMINDER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVibrate(longArrayOf(0, 500, 250, 500))

        with(NotificationManagerCompat.from(context)) {
            notify(TIME_REMINDER_NOTIFICATION_ID, builder.build())
            Log.d(TAG_TIME, "Time reminder notification displayed successfully")
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showOverdueReminderNotification(context: Context, minutesOverdue: Int) {
        Log.d(TAG_OVERDUE, "Creating overdue reminder notification for $minutesOverdue minutes overdue")

        createNotificationChannel(
            context,
            OVERDUE_REMINDER_CHANNEL_ID,
            OVERDUE_REMINDER_CHANNEL_NAME,
            "Notifications for overdue borrowing items",
            NotificationManager.IMPORTANCE_MAX
        )

        val title = "Peringatan Keterlambatan!"
        val message = when (minutesOverdue) {
            5 -> "Anda terlambat 5 menit mengembalikan barang! Segera kembalikan!"
            15 -> "Anda terlambat 15 menit mengembalikan barang! Harap segera kembalikan!"
            30 -> "Anda terlambat 30 menit mengembalikan barang! Segera kembalikan sekarang!"
            60 -> "Anda terlambat 1 jam mengembalikan barang! Kembalikan sekarang juga!"
            else -> "Anda terlambat mengembalikan barang! Segera kembalikan!"
        }

        val notificationIntent = Intent(context, Class.forName("com.pplm.projectinventarisuas.ui.studentsection.borrowing.BorrowingTimerActivity"))
        val pendingIntent = PendingIntent.getActivity(
            context, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, OVERDUE_REMINDER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))
            .setColor(0xFFFF0000.toInt()) // Red color for urgent warning
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        with(NotificationManagerCompat.from(context)) {
            notify(OVERDUE_REMINDER_NOTIFICATION_ID, builder.build())
            Log.d(TAG_OVERDUE, "Overdue reminder notification displayed successfully")
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showOutOfRangeNotification(context: Context, borrowingId: String?) {
        if (borrowingId == null) {
            Log.w(TAG_LOCATION, "Cannot show out of range notification - borrowingId is null")
            return
        }

        Log.d(TAG_LOCATION, "Creating out of range notification for borrowing: $borrowingId")

        createNotificationChannel(
            context,
            LOCATION_ALERT_CHANNEL_ID,
            LOCATION_ALERT_CHANNEL_NAME,
            "Notifications for location-based alerts and warnings"
        )

        val notificationIntent = Intent(context, Class.forName("com.pplm.projectinventarisuas.ui.studentsection.borrowing.BorrowingTimerActivity")).apply {
            putExtra("BORROWING_ID", borrowingId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, LOCATION_ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Peringatan Lokasi")
            .setContentText("Anda berada di luar area yang diperbolehkan. Harap kembali ke lokasi yang ditentukan.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .setColor(0xFFFF0000.toInt()) // Red color for warning

        with(NotificationManagerCompat.from(context)) {
            notify(LOCATION_ALERT_NOTIFICATION_ID, builder.build())
            Log.d(TAG_LOCATION, "Out of range notification displayed successfully")
        }

        updateOutOfRangeStatus(borrowingId, true)
    }

    private fun updateOutOfRangeStatus(borrowingId: String, isOutOfRange: Boolean) {
        Log.d(TAG_DATABASE, "Updating out_of_range status to $isOutOfRange for borrowing: $borrowingId")

        val borrowingRef = FirebaseDatabase.getInstance().getReference("borrowing")
            .child(borrowingId)
        borrowingRef.child("out_of_range").setValue(isOutOfRange)
            .addOnSuccessListener {
                Log.d(TAG_DATABASE, "Successfully updated out_of_range status to $isOutOfRange")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG_DATABASE, "Failed to update out_of_range status: ${exception.message}")
            }
    }

    private fun createNotificationChannel(
        context: Context,
        channelId: String,
        channelName: String,
        description: String,
        importance: Int = NotificationManager.IMPORTANCE_HIGH
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG_NOTIFICATION, "Creating notification channel: $channelId")

            val channel = NotificationChannel(
                channelId,
                channelName,
                importance
            ).apply {
                this.description = description
                enableVibration(true)
                vibrationPattern = if (importance == NotificationManager.IMPORTANCE_MAX) {
                    longArrayOf(0, 1000, 500, 1000, 500, 1000)
                } else {
                    longArrayOf(0, 500, 250, 500)
                }
                if (importance == NotificationManager.IMPORTANCE_MAX) {
                    setBypassDnd(true)
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                }
            }

            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
            Log.d(TAG_NOTIFICATION, "Notification channel created successfully: $channelId")
        } else {
            Log.d(TAG_NOTIFICATION, "Notification channel not required for Android version < O")
        }
    }

    private fun sendLastLocation(context: Context, borrowingId: String?) {
        if (borrowingId == null) {
            Log.w(TAG_LOCATION, "Cannot send location - borrowingId is null")
            return
        }

        Log.d(TAG_LOCATION, "Attempting to send location for borrowing: $borrowingId")

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG_LOCATION, "Location permission granted, getting last location")

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    Log.d(TAG_LOCATION, "Last location obtained: ${location.latitude}, ${location.longitude}")
                    processLocationUpdate(context, location, borrowingId)
                } else {
                    Log.w(TAG_LOCATION, "Last location is null, requesting single location update")
                    requestSingleLocationUpdate(context, fusedLocationClient, borrowingId)
                }
            }.addOnFailureListener { exception ->
                Log.e(TAG_LOCATION, "Failed to get last location: ${exception.message}")
            }
        } else {
            Log.w(TAG_LOCATION, "Location permission not granted")
            Toast.makeText(context, "Location permission not granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestSingleLocationUpdate(context: Context, fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient, borrowingId: String) {
        Log.d(TAG_LOCATION, "Requesting single location update for borrowing: $borrowingId")

        val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 0
        ).setMaxUpdates(1).build()

        val singleUpdateCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                val loc = result.lastLocation
                if (loc != null) {
                    Log.d(TAG_LOCATION, "Single location update received: ${loc.latitude}, ${loc.longitude}")
                    processLocationUpdate(context, loc, borrowingId)
                } else {
                    Log.w(TAG_LOCATION, "Single location update returned null")
                }
                fusedLocationClient.removeLocationUpdates(this)
                Log.d(TAG_LOCATION, "Single location update callback removed")
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                singleUpdateCallback,
                null
            )
            Log.d(TAG_LOCATION, "Single location update requested successfully")
        } catch (e: SecurityException) {
            Log.e(TAG_LOCATION, "Security exception during single location update: ${e.message}")
        }
    }

    private fun processLocationUpdate(context: Context, location: Location, borrowingId: String) {
        val latLng = "${location.latitude},${location.longitude}"
        Log.d(TAG_LOCATION, "Processing location update: $latLng for borrowing: $borrowingId")

        val borrowingRef = FirebaseDatabase.getInstance().getReference("borrowing")
            .child(borrowingId)
        borrowingRef.child("last_location").setValue(latLng)
            .addOnSuccessListener {
                Log.d(TAG_DATABASE, "Location successfully saved to database: $latLng")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG_DATABASE, "Failed to save location to database: ${exception.message}")
            }

        checkDistanceToTarget(context, location, borrowingId)
        scheduleNextLocationSend(context, borrowingId)
    }

    private fun checkDistanceToTarget(context: Context, currentLocation: Location, borrowingId: String) {
        val targetLatitude = 0.4801978305934569
        val targetLongitude = 101.37665907336893
        val radiusInMeters = 50.0f

        Log.d(TAG_LOCATION, "Checking distance to target for borrowing: $borrowingId")

        val targetLocation = Location("Target").apply {
            latitude = targetLatitude
            longitude = targetLongitude
        }

        val distanceInMeters = currentLocation.distanceTo(targetLocation)
        Log.d(TAG_LOCATION, "Distance calculated: ${distanceInMeters}m (limit: ${radiusInMeters}m) for borrowing: $borrowingId")

        val borrowingRef = FirebaseDatabase.getInstance().getReference("borrowing")
            .child(borrowingId)

        borrowingRef.child("out_of_range").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val wasOutOfRange = snapshot.getValue(Boolean::class.java) ?: false
                Log.d(TAG_LOCATION, "Previous out_of_range status: $wasOutOfRange for borrowing: $borrowingId")

                if (distanceInMeters > radiusInMeters && !wasOutOfRange) {
                    Log.w(TAG_LOCATION, "User moved outside radius - triggering out of range notification")
                    val intent = Intent(context, ReminderReceiver::class.java).apply {
                        action = ACTION_OUT_OF_RANGE
                        putExtra("BORROWING_ID", borrowingId)
                    }
                    context.sendBroadcast(intent)
                } else if (distanceInMeters <= radiusInMeters && wasOutOfRange) {
                    Log.i(TAG_LOCATION, "User returned to permitted radius - cancelling out of range notification")
                    cancelOutOfRangeNotification(context)
                    updateOutOfRangeStatus(borrowingId, false)
                } else {
                    Log.d(TAG_LOCATION, "No change in range status - current: ${distanceInMeters <= radiusInMeters}, previous: ${!wasOutOfRange}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG_DATABASE, "Failed to read out_of_range status: ${error.message}")
            }
        })
    }

    private fun cancelOutOfRangeNotification(context: Context) {
        Log.d(TAG_NOTIFICATION, "Cancelling out of range notification")
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(LOCATION_ALERT_NOTIFICATION_ID)
        Log.d(TAG_NOTIFICATION, "Out of range notification cancelled successfully")
    }

    private fun scheduleNextLocationSend(context: Context, borrowingId: String) {
        val nextTime = System.currentTimeMillis() + (1 * 60 * 1000) // 1 minute from now
        Log.d(TAG_LOCATION, "Scheduling next location send at $nextTime for borrowing: $borrowingId")

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SEND_LOCATION
            putExtra("BORROWING_ID", borrowingId)
        }

        val requestCode = (borrowingId.hashCode() + REQUEST_CODE_LOCATION_SEND) and 0xFFFFFFF
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, nextTime, pendingIntent)
            Log.d(TAG_LOCATION, "Non-exact alarm scheduled for next location send")
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
            Log.d(TAG_LOCATION, "Exact alarm scheduled for next location send")
        }
    }
}