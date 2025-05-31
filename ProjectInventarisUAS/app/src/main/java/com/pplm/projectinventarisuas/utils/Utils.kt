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
import com.pplm.projectinventarisuas.R
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
        in 4..10 -> "Selamat Pagi $name"
        in 11..14 -> "Selamat Siang $name"
        in 15..18 -> "Selamat Sore $name"
        else -> "Selamat Malam $name"
    }
}

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val TIME_REMINDER_CHANNEL_ID = "time_reminder_channel"
        private const val TIME_REMINDER_CHANNEL_NAME = "Pengingat Waktu"
        private const val OVERDUE_REMINDER_CHANNEL_ID = "overdue_reminder_channel"
        private const val OVERDUE_REMINDER_CHANNEL_NAME = "Pengingat Keterlambatan"
        private const val LOCATION_ALERT_CHANNEL_ID = "location_alert_channel"
        private const val LOCATION_ALERT_CHANNEL_NAME = "Peringatan Lokasi"

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

        Log.d(TAG_MAIN, "onReceive dipicu - Aksi: $action, ID Peminjaman: $borrowingId")

        when (action) {
            ACTION_SEND_LOCATION -> {
                Log.d(TAG_LOCATION, "Memproses aksi pengiriman lokasi")
                sendLastLocation(context, borrowingId)
            }

            ACTION_OUT_OF_RANGE -> {
                Log.d(TAG_LOCATION, "Memproses aksi di luar jangkauan")
                if (hasNotificationPermission(context)) {
                    showOutOfRangeNotification(context, borrowingId)
                } else {
                    Log.w(
                        TAG_PERMISSION,
                        "Izin notifikasi tidak tersedia untuk peringatan di luar jangkauan"
                    )
                }
            }

            ACTION_TIME_REMINDER -> {
                val minutesRemaining = intent.getIntExtra("MINUTES_REMAINING", 0)
                Log.d(
                    TAG_TIME,
                    "Memproses aksi pengingat waktu - Sisa menit: $minutesRemaining"
                )
                if (hasNotificationPermission(context)) {
                    showTimeReminderNotification(context, minutesRemaining)
                } else {
                    Log.w(TAG_PERMISSION, "Izin notifikasi tidak tersedia untuk pengingat waktu")
                }
            }

            ACTION_OVERDUE_REMINDER -> {
                val minutesOverdue = intent.getIntExtra("MINUTES_OVERDUE", 0)
                Log.d(
                    TAG_OVERDUE,
                    "Memproses aksi pengingat keterlambatan - Menit terlambat: $minutesOverdue"
                )
                if (hasNotificationPermission(context)) {
                    showOverdueReminderNotification(context, minutesOverdue)
                } else {
                    Log.w(
                        TAG_PERMISSION,
                        "Izin notifikasi tidak tersedia untuk pengingat keterlambatan"
                    )
                }
            }

            ACTION_SEND_LOCATION_AND_CHECK_LATE -> {
                Log.d(TAG_LOCATION, "Memproses aksi pengiriman lokasi dan pemeriksaan terlambat")
                sendLastLocation(context, borrowingId)
                if (borrowingId != null) {
                    checkAndSetLateStatusFromAlarm(context, borrowingId)
                } else {
                    Log.e(
                        TAG_DATABASE,
                        "ID Peminjaman null untuk pemeriksaan status terlambat dari alarm."
                    )
                }
            }

            else -> {
                Log.w(TAG_MAIN, "Aksi tidak dikenal diterima: $action")
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
                            "Status peminjaman berhasil diperbarui menjadi 'Terlambat' melalui alarm untuk ID: $borrowingId"
                        )
                        if (hasNotificationPermission(context)) {
                            showNotification(
                                context = context,
                                title = context.getString(R.string.overdue_warning_title),
                                message = context.getString(R.string.borrowing_time_over_late_status),
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
                            "Gagal memperbarui status peminjaman menjadi 'Terlambat' melalui alarm: ${e.message}"
                        )
                    }
            } else {
                Log.d(
                    TAG_DATABASE,
                    "Status sudah '$currentStatus', tidak perlu memperbarui menjadi 'Terlambat'"
                )
            }
        }.addOnFailureListener { e ->
            Log.e(
                TAG_DATABASE,
                "Gagal mendapatkan status saat ini untuk pemeriksaan keterlambatan: ${e.message}"
            )
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
                Log.w(TAG_PERMISSION, "Tidak dapat menampilkan notifikasi - izin tidak diberikan")
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
                Log.w(
                    TAG_NOTIFICATION,
                    "Kelas BorrowingTimerActivity tidak ditemukan: ${e.message}"
                )
            }

            val notificationManager = NotificationManagerCompat.from(context)
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(notificationId, builder.build())
                Log.d(
                    TAG_NOTIFICATION,
                    "Notifikasi berhasil ditampilkan untuk ID: $notificationId"
                )
            } else {
                Log.w(TAG_NOTIFICATION, "Notifikasi dinonaktifkan untuk aplikasi ini")
            }
        } catch (e: Exception) {
            Log.e(TAG_NOTIFICATION, "Gagal menampilkan notifikasi: ${e.message}")
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
            Log.d(TAG_NOTIFICATION, "Membuat saluran notifikasi: $channelId")

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
            Log.d(TAG_NOTIFICATION, "Saluran notifikasi berhasil dibuat: $channelId")
        } else {
            Log.d(TAG_NOTIFICATION, "Saluran notifikasi tidak diperlukan untuk Android versi < O")
        }
    }

    private fun showTimeReminderNotification(context: Context, minutesRemaining: Int) {
        val title = context.getString(R.string.time_reminder_title)
        val message = when (minutesRemaining) {
            30 -> context.getString(R.string.time_reminder_30_min)
            15 -> context.getString(R.string.time_reminder_15_min)
            5 -> context.getString(R.string.time_reminder_5_min)
            else -> context.getString(R.string.time_reminder_soon)
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
        val title = context.getString(R.string.overdue_warning_title)
        val message = when (minutesOverdue) {
            5 -> context.getString(R.string.overdue_5_min)
            15 -> context.getString(R.string.overdue_15_min)
            30 -> context.getString(R.string.overdue_30_min)
            60 -> context.getString(R.string.overdue_60_min)
            else -> context.getString(R.string.overdue_generic)
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
            Log.w(
                TAG_LOCATION,
                "Tidak dapat menampilkan notifikasi di luar jangkauan - borrowingId null"
            )
            return
        }

        val title = context.getString(R.string.location_alert_title)
        val message = context.getString(R.string.location_out_of_range_message)

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
            "Memperbarui status out_of_range menjadi $isOutOfRange untuk peminjaman: $borrowingId"
        )

        val borrowingRef = FirebaseDatabase.getInstance().getReference("borrowing")
            .child(borrowingId)
        borrowingRef.child("out_of_range").setValue(isOutOfRange)
            .addOnSuccessListener {
                Log.d(
                    TAG_DATABASE,
                    "Berhasil memperbarui status out_of_range menjadi $isOutOfRange"
                )
            }
            .addOnFailureListener { exception ->
                Log.e(TAG_DATABASE, "Gagal memperbarui status out_of_range: ${exception.message}")
            }
    }

    private fun sendLastLocation(context: Context, borrowingId: String?) {
        if (borrowingId == null) {
            Log.w(TAG_LOCATION, "Tidak dapat mengirim lokasi - borrowingId null")
            return
        }

        if (!hasLocationPermission(context)) {
            Log.w(TAG_PERMISSION, context.getString(R.string.location_permission_not_granted))
            Toast.makeText(
                context,
                context.getString(R.string.location_permission_not_granted),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        Log.d(TAG_LOCATION, "Mencoba mengirim lokasi untuk peminjaman: $borrowingId")

        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            Log.d(TAG_LOCATION, "Izin lokasi diberikan, mendapatkan lokasi terakhir")

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    Log.d(
                        TAG_LOCATION,
                        "Lokasi terakhir diperoleh: ${location.latitude}, ${location.longitude}"
                    )
                    processLocationUpdate(context, location, borrowingId)
                } else {
                    Log.w(TAG_LOCATION, "Lokasi terakhir null, meminta pembaruan lokasi tunggal")
                    requestSingleLocationUpdate(context, fusedLocationClient, borrowingId)
                }
            }.addOnFailureListener { exception ->
                Log.e(TAG_LOCATION, "Gagal mendapatkan lokasi terakhir: ${exception.message}")
            }
        } catch (e: SecurityException) {
            Log.e(TAG_LOCATION, "Pengecualian keamanan saat mengakses lokasi: ${e.message}")
            Toast.makeText(
                context,
                context.getString(R.string.location_access_denied),
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Log.e(TAG_LOCATION, "Kesalahan tak terduga saat mengakses lokasi: ${e.message}")
        }
    }

    private fun requestSingleLocationUpdate(
        context: Context,
        fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
        borrowingId: String
    ) {
        if (!hasLocationPermission(context)) {
            Log.w(TAG_PERMISSION, "Tidak dapat meminta pembaruan lokasi - izin tidak diberikan")
            return
        }

        Log.d(TAG_LOCATION, "Meminta pembaruan lokasi tunggal untuk peminjaman: $borrowingId")

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
                            "Pembaruan lokasi tunggal diterima: ${loc.latitude}, ${loc.longitude}"
                        )
                        processLocationUpdate(context, loc, borrowingId)
                    } else {
                        Log.w(TAG_LOCATION, "Pembaruan lokasi tunggal mengembalikan null")
                    }
                    fusedLocationClient.removeLocationUpdates(this)
                    Log.d(TAG_LOCATION, "Callback pembaruan lokasi tunggal dihapus")
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                singleUpdateCallback,
                null
            )
            Log.d(TAG_LOCATION, "Permintaan pembaruan lokasi tunggal berhasil")
        } catch (e: SecurityException) {
            Log.e(
                TAG_LOCATION,
                "Pengecualian keamanan selama pembaruan lokasi tunggal: ${e.message}"
            )
        } catch (e: Exception) {
            Log.e(
                TAG_LOCATION,
                "Kesalahan tak terduga selama pembaruan lokasi tunggal: ${e.message}"
            )
        }
    }

    private fun processLocationUpdate(context: Context, location: Location, borrowingId: String) {
        val latLng = "${location.latitude},${location.longitude}"
        Log.d(TAG_LOCATION, "Memproses pembaruan lokasi: $latLng untuk peminjaman: $borrowingId")

        val borrowingRef = FirebaseDatabase.getInstance().getReference("borrowing")
            .child(borrowingId)
        borrowingRef.child("last_location").setValue(latLng)
            .addOnSuccessListener {
                Log.d(TAG_DATABASE, "Lokasi berhasil disimpan ke database: $latLng")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG_DATABASE, "Gagal menyimpan lokasi ke database: ${exception.message}")
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

        Log.d(TAG_LOCATION, "Memeriksa jarak ke target untuk peminjaman: $borrowingId")

        val targetLocation = Location("Target").apply {
            latitude = targetLatitude
            longitude = targetLongitude
        }

        val distanceInMeters = currentLocation.distanceTo(targetLocation)
        Log.d(
            TAG_LOCATION,
            "Jarak terhitung: ${distanceInMeters}m (batas: ${radiusInMeters}m) untuk peminjaman: $borrowingId"
        )

        val borrowingRef = FirebaseDatabase.getInstance().getReference("borrowing")
            .child(borrowingId)

        borrowingRef.child("out_of_range")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val wasOutOfRange = snapshot.getValue(Boolean::class.java) == true
                    Log.d(
                        TAG_LOCATION,
                        "Status out_of_range sebelumnya: $wasOutOfRange untuk peminjaman: $borrowingId"
                    )

                    if (distanceInMeters > radiusInMeters && !wasOutOfRange) {
                        Log.w(
                            TAG_LOCATION,
                            "Pengguna bergerak di luar radius - memicu notifikasi di luar jangkauan"
                        )
                        val intent = Intent(context, ReminderReceiver::class.java).apply {
                            action = ACTION_OUT_OF_RANGE
                            putExtra("BORROWING_ID", borrowingId)
                        }
                        context.sendBroadcast(intent)
                    } else if (distanceInMeters <= radiusInMeters && wasOutOfRange) {
                        Log.i(
                            TAG_LOCATION,
                            "Pengguna kembali ke radius yang diizinkan - membatalkan notifikasi di luar jangkauan"
                        )
                        cancelOutOfRangeNotification(context)
                        updateOutOfRangeStatus(borrowingId, false)
                    } else {
                        Log.d(
                            TAG_LOCATION,
                            "Tidak ada perubahan status jangkauan - saat ini: ${distanceInMeters <= radiusInMeters}, sebelumnya: ${!wasOutOfRange}"
                        )
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG_DATABASE, "Gagal membaca status out_of_range: ${error.message}")
                }
            })
    }

    private fun cancelOutOfRangeNotification(context: Context) {
        try {
            Log.d(TAG_NOTIFICATION, "Membatalkan notifikasi di luar jangkauan")
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(LOCATION_ALERT_NOTIFICATION_ID)
            Log.d(TAG_NOTIFICATION, "Notifikasi di luar jangkauan berhasil dibatalkan")
        } catch (e: Exception) {
            Log.e(TAG_NOTIFICATION, "Gagal membatalkan notifikasi di luar jangkauan: ${e.message}")
        }
    }

    private fun scheduleNextLocationSend(context: Context, borrowingId: String) {
        val nextTime = System.currentTimeMillis() + (1 * 60 * 1000)
        Log.d(
            TAG_LOCATION,
            "Menjadwalkan pengiriman lokasi berikutnya pada $nextTime untuk peminjaman: $borrowingId"
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
                            "Alarm tepat dijadwalkan untuk pengiriman lokasi berikutnya (API 31+)"
                        )
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, nextTime, pendingIntent)
                        Log.d(
                            TAG_LOCATION,
                            "Alarm non-tepat dijadwalkan untuk pengiriman lokasi berikutnya (API 31+)"
                        )
                    }
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextTime,
                        pendingIntent
                    )
                    Log.d(
                        TAG_LOCATION,
                        "Alarm tepat dijadwalkan untuk pengiriman lokasi berikutnya (API 23+)"
                    )
                }

                else -> {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, nextTime, pendingIntent)
                    Log.d(
                        TAG_LOCATION,
                        "Alarm dijadwalkan untuk pengiriman lokasi berikutnya (API < 23)"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG_LOCATION, "Gagal menjadwalkan pengiriman lokasi berikutnya: ${e.message}")
        }
    }
}