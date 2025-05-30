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
        const val ACTION_SEND_LOCATION_AND_CHECK_LATE =
            "com.pplm.projectinventarisuas.ACTION_SEND_LOCATION_AND_CHECK_LATE"

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
        private const val TAG_PERMISSION = "PermissionCheck"
    }

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
                if (hasNotificationPermission(context)) {
                    showOutOfRangeNotification(context, borrowingId)
                } else {
                    Log.w(
                        TAG_PERMISSION,
                        "Notification permission not available for out of range alert"
                    )
                }
            }

            ACTION_TIME_REMINDER -> {
                val minutesRemaining = intent.getIntExtra("MINUTES_REMAINING", 0)
                Log.d(
                    TAG_TIME,
                    "Processing time reminder action - Minutes remaining: $minutesRemaining"
                )
                if (hasNotificationPermission(context)) {
                    showTimeReminderNotification(context, minutesRemaining)
                } else {
                    Log.w(TAG_PERMISSION, "Notification permission not available for time reminder")
                }
            }

            ACTION_OVERDUE_REMINDER -> {
                val minutesOverdue = intent.getIntExtra("MINUTES_OVERDUE", 0)
                Log.d(
                    TAG_OVERDUE,
                    "Processing overdue reminder action - Minutes overdue: $minutesOverdue"
                )
                if (hasNotificationPermission(context)) {
                    showOverdueReminderNotification(context, minutesOverdue)
                } else {
                    Log.w(
                        TAG_PERMISSION,
                        "Notification permission not available for overdue reminder"
                    )
                }
            }

            ACTION_SEND_LOCATION_AND_CHECK_LATE -> {
                Log.d(TAG_LOCATION, "Processing send location and check late action")
                sendLastLocation(context, borrowingId)
                if (borrowingId != null) {
                    checkAndSetLateStatusFromAlarm(context, borrowingId)
                } else {
                    Log.e(TAG_DATABASE, "Borrowing ID is null for late status check from alarm.")
                }
            }

            else -> {
                Log.w(TAG_MAIN, "Unknown action received: $action")
            }
        }
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    private fun hasLocationPermission(context: Context): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocationGranted || coarseLocationGranted
    }

    private fun checkAndSetLateStatusFromAlarm(context: Context, borrowingId: String) {
        val databaseRef =
            FirebaseDatabase.getInstance().getReference("borrowing").child(borrowingId)
        databaseRef.child("status").get().addOnSuccessListener { dataSnapshot ->
            val currentStatus = dataSnapshot.getValue(String::class.java)
            if (currentStatus != "Returned" && currentStatus != "Late") {
                databaseRef.child("status").setValue("Late")
                    .addOnSuccessListener {
                        Log.d(
                            TAG_DATABASE,
                            "Borrowing status successfully updated to 'Late' via alarm for ID: $borrowingId"
                        )
                        if (hasNotificationPermission(context)) {
                            showNotification(
                                context = context,
                                title = "Peringatan Keterlambatan!",
                                message = "Waktu peminjaman telah habis. Status peminjaman Anda diperbarui menjadi Terlambat.",
                                notificationId = borrowingId.hashCode() + REQUEST_CODE_OVERDUE_REMINDER,
                                channelId = OVERDUE_REMINDER_CHANNEL_ID,
                                channelName = OVERDUE_REMINDER_CHANNEL_NAME,
                                channelDescription = "Notifikasi untuk barang pinjaman yang terlambat",
                                priority = NotificationCompat.PRIORITY_HIGH,
                                isOngoing = true,
                                isLateWarning = true
                            )
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(
                            TAG_DATABASE,
                            "Failed to update borrowing status to 'Late' via alarm: ${e.message}"
                        )
                    }
            } else {
                Log.d(
                    TAG_DATABASE,
                    "Status is already '$currentStatus', no need to update to 'Late'"
                )
            }
        }.addOnFailureListener { e ->
            Log.e(TAG_DATABASE, "Failed to get current status for late check: ${e.message}")
        }
    }

    private fun showNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int,
        channelId: String,
        channelName: String,
        channelDescription: String,
        priority: Int = NotificationCompat.PRIORITY_HIGH,
        isOngoing: Boolean = false,
        isLateWarning: Boolean = false
    ) {
        try {
            if (!hasNotificationPermission(context)) {
                Log.w(TAG_PERMISSION, "Cannot show notification - permission not granted")
                return
            }

            val importance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                when (priority) {
                    NotificationCompat.PRIORITY_DEFAULT -> NotificationManager.IMPORTANCE_DEFAULT
                    NotificationCompat.PRIORITY_LOW -> NotificationManager.IMPORTANCE_LOW
                    NotificationCompat.PRIORITY_MIN -> NotificationManager.IMPORTANCE_MIN
                    NotificationCompat.PRIORITY_HIGH -> NotificationManager.IMPORTANCE_HIGH
                    NotificationCompat.PRIORITY_MAX -> NotificationManager.IMPORTANCE_HIGH
                    else -> NotificationManager.IMPORTANCE_DEFAULT
                }
            } else {
                NotificationManager.IMPORTANCE_DEFAULT
            }

            createNotificationChannel(
                context,
                channelId,
                channelName,
                channelDescription,
                importance
            )

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(priority)
                .setAutoCancel(!isOngoing)
                .setOngoing(isOngoing)
                .setCategory(if (isLateWarning) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
                .setVibrate(longArrayOf(0, 1000, 500, 1000))
                .setColor(
                    if (isLateWarning) 0xFFFF0000.toInt() else ContextCompat.getColor(
                        context,
                        android.R.color.holo_blue_light
                    )
                )

            try {
                val notificationIntent = Intent(
                    context,
                    Class.forName("com.pplm.projectinventarisuas.ui.studentsection.borrowing.BorrowingTimerActivity")
                )
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    notificationIntent,
                    getPendingIntentFlags()
                )
                builder.setContentIntent(pendingIntent)
            } catch (e: ClassNotFoundException) {
                Log.w(TAG_NOTIFICATION, "BorrowingTimerActivity class not found: ${e.message}")
            }

            val notificationManager = NotificationManagerCompat.from(context)
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(notificationId, builder.build())
                Log.d(
                    TAG_NOTIFICATION,
                    "Notification displayed successfully for ID: $notificationId"
                )
            } else {
                Log.w(TAG_NOTIFICATION, "Notifications are disabled for this app")
            }
        } catch (e: Exception) {
            Log.e(TAG_NOTIFICATION, "Failed to show notification: ${e.message}")
        }
    }

    private fun getPendingIntentFlags(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
    }

    private fun createNotificationChannel(
        context: Context,
        channelId: String,
        channelName: String,
        description: String,
        importance: Int
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

                val isHighImportance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    importance >= NotificationManager.IMPORTANCE_HIGH
                } else {
                    true
                }

                vibrationPattern = if (isHighImportance) {
                    longArrayOf(0, 1000, 500, 1000, 500, 1000)
                } else {
                    longArrayOf(0, 500, 250, 500)
                }

                if (isHighImportance) {
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

    private fun showTimeReminderNotification(context: Context, minutesRemaining: Int) {
        val title = "Pengingat Waktu Peminjaman"
        val message = when (minutesRemaining) {
            30 -> "Waktu peminjaman akan berakhir dalam 30 menit!"
            15 -> "Waktu peminjaman akan berakhir dalam 15 menit!"
            5 -> "Waktu peminjaman akan berakhir dalam 5 menit!"
            else -> "Waktu peminjaman akan segera berakhir!"
        }
        showNotification(
            context = context,
            title = title,
            message = message,
            notificationId = TIME_REMINDER_NOTIFICATION_ID,
            channelId = TIME_REMINDER_CHANNEL_ID,
            channelName = TIME_REMINDER_CHANNEL_NAME,
            channelDescription = "Notifikasi untuk pengingat waktu peminjaman",
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }

    private fun showOverdueReminderNotification(context: Context, minutesOverdue: Int) {
        val title = "Peringatan Keterlambatan!"
        val message = when (minutesOverdue) {
            5 -> "Anda terlambat 5 menit mengembalikan barang! Segera kembalikan!"
            15 -> "Anda terlambat 15 menit mengembalikan barang! Harap segera kembalikan!"
            30 -> "Anda terlambat 30 menit mengembalikan barang! Segera kembalikan sekarang!"
            60 -> "Anda terlambat 1 jam mengembalikan barang! Kembalikan sekarang juga!"
            else -> "Anda terlambat mengembalikan barang! Segera kembalikan!"
        }
        showNotification(
            context = context,
            title = title,
            message = message,
            notificationId = OVERDUE_REMINDER_NOTIFICATION_ID,
            channelId = OVERDUE_REMINDER_CHANNEL_ID,
            channelName = OVERDUE_REMINDER_CHANNEL_NAME,
            channelDescription = "Notifikasi untuk barang pinjaman yang terlambat",
            priority = NotificationCompat.PRIORITY_HIGH,
            isOngoing = true,
            isLateWarning = true
        )
    }

    private fun showOutOfRangeNotification(context: Context, borrowingId: String?) {
        if (borrowingId == null) {
            Log.w(TAG_LOCATION, "Cannot show out of range notification - borrowingId is null")
            return
        }

        val title = "Peringatan Lokasi"
        val message =
            "Anda berada di luar area yang diperbolehkan. Harap kembali ke lokasi yang ditentukan."

        showNotification(
            context = context,
            title = title,
            message = message,
            notificationId = LOCATION_ALERT_NOTIFICATION_ID,
            channelId = LOCATION_ALERT_CHANNEL_ID,
            channelName = LOCATION_ALERT_CHANNEL_NAME,
            channelDescription = "Notifikasi untuk peringatan berbasis lokasi",
            priority = NotificationCompat.PRIORITY_HIGH,
            isOngoing = true,
            isLateWarning = true
        )
        updateOutOfRangeStatus(borrowingId, true)
    }

    private fun updateOutOfRangeStatus(borrowingId: String, isOutOfRange: Boolean) {
        Log.d(
            TAG_DATABASE,
            "Updating out_of_range status to $isOutOfRange for borrowing: $borrowingId"
        )

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

    private fun sendLastLocation(context: Context, borrowingId: String?) {
        if (borrowingId == null) {
            Log.w(TAG_LOCATION, "Cannot send location - borrowingId is null")
            return
        }

        if (!hasLocationPermission(context)) {
            Log.w(TAG_PERMISSION, "Location permission not granted")
            Toast.makeText(context, "Location permission not granted", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG_LOCATION, "Attempting to send location for borrowing: $borrowingId")

        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            Log.d(TAG_LOCATION, "Location permission granted, getting last location")

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    Log.d(
                        TAG_LOCATION,
                        "Last location obtained: ${location.latitude}, ${location.longitude}"
                    )
                    processLocationUpdate(context, location, borrowingId)
                } else {
                    Log.w(TAG_LOCATION, "Last location is null, requesting single location update")
                    requestSingleLocationUpdate(context, fusedLocationClient, borrowingId)
                }
            }.addOnFailureListener { exception ->
                Log.e(TAG_LOCATION, "Failed to get last location: ${exception.message}")
            }
        } catch (e: SecurityException) {
            Log.e(TAG_LOCATION, "Security exception while accessing location: ${e.message}")
            Toast.makeText(context, "Location access denied", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG_LOCATION, "Unexpected error while accessing location: ${e.message}")
        }
    }

    private fun requestSingleLocationUpdate(
        context: Context,
        fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
        borrowingId: String
    ) {
        if (!hasLocationPermission(context)) {
            Log.w(TAG_PERMISSION, "Cannot request location update - permission not granted")
            return
        }

        Log.d(TAG_LOCATION, "Requesting single location update for borrowing: $borrowingId")

        try {
            val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 0
            ).setMaxUpdates(1).build()

            val singleUpdateCallback = object : com.google.android.gms.location.LocationCallback() {
                override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                    val loc = result.lastLocation
                    if (loc != null) {
                        Log.d(
                            TAG_LOCATION,
                            "Single location update received: ${loc.latitude}, ${loc.longitude}"
                        )
                        processLocationUpdate(context, loc, borrowingId)
                    } else {
                        Log.w(TAG_LOCATION, "Single location update returned null")
                    }
                    fusedLocationClient.removeLocationUpdates(this)
                    Log.d(TAG_LOCATION, "Single location update callback removed")
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                singleUpdateCallback,
                null
            )
            Log.d(TAG_LOCATION, "Single location update requested successfully")
        } catch (e: SecurityException) {
            Log.e(TAG_LOCATION, "Security exception during single location update: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG_LOCATION, "Unexpected error during single location update: ${e.message}")
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

    private fun checkDistanceToTarget(
        context: Context,
        currentLocation: Location,
        borrowingId: String
    ) {
        val targetLatitude = 0.4801978305934569
        val targetLongitude = 101.37665907336893
        val radiusInMeters = 50.0f

        Log.d(TAG_LOCATION, "Checking distance to target for borrowing: $borrowingId")

        val targetLocation = Location("Target").apply {
            latitude = targetLatitude
            longitude = targetLongitude
        }

        val distanceInMeters = currentLocation.distanceTo(targetLocation)
        Log.d(
            TAG_LOCATION,
            "Distance calculated: ${distanceInMeters}m (limit: ${radiusInMeters}m) for borrowing: $borrowingId"
        )

        val borrowingRef = FirebaseDatabase.getInstance().getReference("borrowing")
            .child(borrowingId)

        borrowingRef.child("out_of_range")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val wasOutOfRange = snapshot.getValue(Boolean::class.java) == true
                    Log.d(
                        TAG_LOCATION,
                        "Previous out_of_range status: $wasOutOfRange for borrowing: $borrowingId"
                    )

                    if (distanceInMeters > radiusInMeters && !wasOutOfRange) {
                        Log.w(
                            TAG_LOCATION,
                            "User moved outside radius - triggering out of range notification"
                        )
                        val intent = Intent(context, ReminderReceiver::class.java).apply {
                            action = ACTION_OUT_OF_RANGE
                            putExtra("BORROWING_ID", borrowingId)
                        }
                        context.sendBroadcast(intent)
                    } else if (distanceInMeters <= radiusInMeters && wasOutOfRange) {
                        Log.i(
                            TAG_LOCATION,
                            "User returned to permitted radius - cancelling out of range notification"
                        )
                        cancelOutOfRangeNotification(context)
                        updateOutOfRangeStatus(borrowingId, false)
                    } else {
                        Log.d(
                            TAG_LOCATION,
                            "No change in range status - current: ${distanceInMeters <= radiusInMeters}, previous: ${!wasOutOfRange}"
                        )
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG_DATABASE, "Failed to read out_of_range status: ${error.message}")
                }
            })
    }

    private fun cancelOutOfRangeNotification(context: Context) {
        try {
            Log.d(TAG_NOTIFICATION, "Cancelling out of range notification")
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(LOCATION_ALERT_NOTIFICATION_ID)
            Log.d(TAG_NOTIFICATION, "Out of range notification cancelled successfully")
        } catch (e: Exception) {
            Log.e(TAG_NOTIFICATION, "Failed to cancel out of range notification: ${e.message}")
        }
    }

    private fun scheduleNextLocationSend(context: Context, borrowingId: String) {
        val nextTime = System.currentTimeMillis() + (1 * 60 * 1000)
        Log.d(
            TAG_LOCATION,
            "Scheduling next location send at $nextTime for borrowing: $borrowingId"
        )

        try {
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
                getPendingIntentFlags()
            )

            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            nextTime,
                            pendingIntent
                        )
                        Log.d(
                            TAG_LOCATION,
                            "Exact alarm scheduled for next location send (API 31+)"
                        )
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, nextTime, pendingIntent)
                        Log.d(
                            TAG_LOCATION,
                            "Non-exact alarm scheduled for next location send (API 31+)"
                        )
                    }
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextTime,
                        pendingIntent
                    )
                    Log.d(TAG_LOCATION, "Exact alarm scheduled for next location send (API 23+)")
                }

                else -> {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, nextTime, pendingIntent)
                    Log.d(TAG_LOCATION, "Alarm scheduled for next location send (API < 23)")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG_LOCATION, "Failed to schedule next location send: ${e.message}")
        }
    }
}