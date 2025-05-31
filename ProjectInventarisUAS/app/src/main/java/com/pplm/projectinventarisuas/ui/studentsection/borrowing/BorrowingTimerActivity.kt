package com.pplm.projectinventarisuas.ui.studentsection.borrowing

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import com.google.firebase.database.*
import com.pplm.projectinventarisuas.R
import com.pplm.projectinventarisuas.databinding.ActivityBorrowingTimerBinding
import com.pplm.projectinventarisuas.ui.studentsection.StudentSectionActivity
import com.pplm.projectinventarisuas.utils.ReminderReceiver
import com.pplm.projectinventarisuas.utils.components.CustomDialog
import com.pplm.projectinventarisuas.utils.formatTime
import com.pplm.projectinventarisuas.utils.timer.TimerService
import com.pplm.projectinventarisuas.utils.timer.TimerState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

class BorrowingTimerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBorrowingTimerBinding
    private lateinit var databaseRef: DatabaseReference
    private var borrowingId: String? = null
    private var endHour: String? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geofenceClient: GeofencingClient
    private lateinit var locationCallback: LocationCallback
    private val targetLatitude = 0.4801978305934569
    private val targetLongitude = 101.37665907336893
    private val radiusInMeters = 50.0f
    private var locationPermissionGranted = false
    private var isOutsideRadius = false

    companion object {
        private const val TAG_MAIN = "BorrowingTimerMain"
        private const val TAG_LOCATION = "BorrowingLocation"
        private const val TAG_PERMISSION = "BorrowingPermission"
        private const val TAG_NOTIFICATION = "BorrowingNotification"
        private const val TAG_TIMER = "BorrowingTimer"
        private const val TAG_DATABASE = "BorrowingDatabase"
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG_PERMISSION, "Izin notifikasi diberikan")
        } else {
            Log.w(TAG_PERMISSION, "Izin notifikasi ditolak")
            CustomDialog.alert(
                this,
                getString(R.string.permission_denied_title),
                getString(R.string.notification_permission_denied)
            )
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        locationPermissionGranted = permissions.entries.all { it.value }
        if (locationPermissionGranted) {
            Log.d(TAG_PERMISSION, "Semua izin lokasi diberikan")
            startLocationMonitoring()
        } else {
            Log.w(TAG_PERMISSION, "Beberapa izin lokasi ditolak: $permissions")
            CustomDialog.alert(
                this,
                getString(R.string.permission_required_title),
                getString(R.string.location_permission_required)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG_MAIN, "onCreate: Aktivitas dimulai")

        binding = ActivityBorrowingTimerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        borrowingId = intent.getStringExtra("BORROWING_ID")
        endHour = intent.getStringExtra("END_HOUR")

        Log.d(TAG_MAIN, "onCreate: borrowingId=$borrowingId, endHour=$endHour")

        if (borrowingId == null) {
            Log.e(TAG_MAIN, "onCreate: ID Peminjaman null, mengakhiri aktivitas")
            CustomDialog.alert(
                this,
                getString(R.string.error_title),
                getString(R.string.borrowing_id_not_found)
            )
            finish()
            return
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        geofenceClient = LocationServices.getGeofencingClient(this)
        Log.d(TAG_LOCATION, "Layanan lokasi diinisialisasi")

        preventBackNavigation()
        checkAndRequestPermissions()

        checkBorrowingExists()
    }

    private fun checkBorrowingExists() {
        Log.d(TAG_DATABASE, "Memeriksa apakah ID peminjaman ada: $borrowingId")

        databaseRef = FirebaseDatabase.getInstance().getReference("borrowing").child(borrowingId!!)
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    Log.d(TAG_DATABASE, "ID Peminjaman ada, melanjutkan inisialisasi normal")
                    observeTimerState()
                    observeBorrowingStatus()
                    startTimerService()
                    setReminderForBorrowingEnd()
                } else {
                    Log.w(
                        TAG_DATABASE,
                        "ID Peminjaman tidak ada di database, mengakhiri aktivitas"
                    )
                    CustomDialog.alert(
                        this@BorrowingTimerActivity,
                        getString(R.string.error_title),
                        getString(R.string.borrowing_data_not_found)
                    ) {
                        cleanupAndFinish()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG_DATABASE, "Gagal memeriksa keberadaan peminjaman: ${error.message}")
                CustomDialog.alert(
                    this@BorrowingTimerActivity,
                    getString(R.string.error_title),
                    getString(R.string.failed_to_verify_borrowing_data)
                ) {
                    cleanupAndFinish()
                }
            }
        })
    }

    private fun cleanupAndFinish() {
        Log.d(TAG_MAIN, "Membersihkan dan mengakhiri aktivitas")

        val prefs = getSharedPreferences("BorrowingSession", MODE_PRIVATE)
        prefs.edit() {
            remove("activeBorrowingId")
            remove("alarmSet_$borrowingId")
        }

        stopTimerService()
        stopLocationMonitoring()
        cancelAllNotifications()

        val intent = Intent(this, StudentSectionActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun checkAndRequestPermissions() {
        Log.d(TAG_PERMISSION, "Memulai pemeriksaan izin")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasExactAlarmPermission()) {
                Log.d(TAG_PERMISSION, "Izin alarm tepat tidak diberikan, meminta izin")
                requestExactAlarmPermission()
            } else {
                Log.d(TAG_PERMISSION, "Izin alarm tepat sudah diberikan")
            }
        }
        checkNotificationPermission()
        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        Log.d(TAG_PERMISSION, "Memeriksa izin lokasi")

        val fineLocationPermission = Manifest.permission.ACCESS_FINE_LOCATION
        val coarseLocationPermission = Manifest.permission.ACCESS_COARSE_LOCATION
        val backgroundLocationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        } else {
            null
        }

        val permissionsToRequest = mutableListOf(fineLocationPermission, coarseLocationPermission)
        backgroundLocationPermission?.let {
            permissionsToRequest.add(it)
            Log.d(TAG_PERMISSION, "Izin lokasi latar belakang ditambahkan ke daftar permintaan")
        }

        when {
            permissionsToRequest.all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            } -> {
                Log.d(TAG_PERMISSION, "Semua izin lokasi sudah diberikan")
                locationPermissionGranted = true
                startLocationMonitoring()
            }

            else -> {
                Log.d(TAG_PERMISSION, "Meminta izin lokasi: $permissionsToRequest")
                locationPermissionLauncher.launch(permissionsToRequest.toTypedArray())
            }
        }
    }

    private fun startLocationMonitoring() {
        if (!locationPermissionGranted) {
            Log.w(TAG_LOCATION, "Tidak dapat memulai pemantauan lokasi - izin tidak diberikan")
            return
        }

        Log.d(TAG_LOCATION, "Memulai pemantauan lokasi")

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(5000)
            .setMaxUpdateDelayMillis(15000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                Log.d(
                    TAG_LOCATION,
                    "Pembaruan lokasi diterima dengan ${result.locations.size} lokasi"
                )
                for (location in result.locations) {
                    checkUserDistance(location)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            Log.d(TAG_LOCATION, "Permintaan pembaruan lokasi berhasil")
        } catch (e: SecurityException) {
            Log.e(TAG_LOCATION, "Pengecualian keamanan saat meminta pembaruan lokasi: ${e.message}")
        }
    }

    private fun checkUserDistance(currentLocation: Location) {
        val targetLocation = Location("Target").apply {
            latitude = targetLatitude
            longitude = targetLongitude
        }

        val distanceInMeters = currentLocation.distanceTo(targetLocation)
        Log.d(
            TAG_LOCATION,
            "Jarak saat ini ke target: ${distanceInMeters}m (batas: ${radiusInMeters}m)"
        )

        if (distanceInMeters > radiusInMeters && !isOutsideRadius) {
            Log.w(
                TAG_LOCATION,
                "Pengguna bergerak di luar radius yang diizinkan - memicu peringatan di luar jangkauan"
            )
            isOutsideRadius = true
            showOutOfRangeReminder()
        } else if (distanceInMeters <= radiusInMeters && isOutsideRadius) {
            Log.i(TAG_LOCATION, "Pengguna kembali ke radius yang diizinkan")
            isOutsideRadius = false
        }
    }

    private fun showOutOfRangeReminder() {
        Log.d(
            TAG_NOTIFICATION,
            "Mengirim notifikasi di luar jangkauan untuk peminjaman: $borrowingId"
        )

        val intent = Intent(this, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_OUT_OF_RANGE
            putExtra("BORROWING_ID", borrowingId)
        }

        val requestCode =
            (borrowingId.hashCode() + ReminderReceiver.REQUEST_CODE_OUT_OF_RANGE) and 0xFFFFFFF
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            pendingIntent.send()
            Log.d(TAG_NOTIFICATION, "Notifikasi di luar jangkauan berhasil dikirim")
        } catch (e: PendingIntent.CanceledException) {
            Log.e(TAG_NOTIFICATION, "Gagal mengirim notifikasi di luar jangkauan: ${e.message}")
        }
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.d(TAG_PERMISSION, "Meminta izin alarm tepat")
            val intent = Intent().apply {
                action = android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
            }
            startActivity(intent)
        }
    }

    private fun hasExactAlarmPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val canSchedule = alarmManager.canScheduleExactAlarms()
            Log.d(TAG_PERMISSION, "Status izin alarm tepat: $canSchedule")
            canSchedule
        } else {
            Log.d(TAG_PERMISSION, "Izin alarm tepat tidak diperlukan untuk versi Android ini")
            true
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!isGranted) {
                Log.d(TAG_PERMISSION, "Meminta izin notifikasi")
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                Log.d(TAG_PERMISSION, "Izin notifikasi sudah diberikan")
            }
        } else {
            Log.d(TAG_PERMISSION, "Izin notifikasi tidak diperlukan untuk versi Android ini")
        }
    }

    private fun setReminderForBorrowingEnd() {
        Log.d(TAG_NOTIFICATION, "Menyiapkan pengingat akhir peminjaman untuk ID: $borrowingId")

        val borrowingEndTime = getBorrowingEndTimeInMillis()
        val prefs = getSharedPreferences("BorrowingSession", MODE_PRIVATE)
        val alarmSetKey = "alarmSet_$borrowingId"

        if (prefs.getBoolean(alarmSetKey, false)) {
            Log.d(TAG_NOTIFICATION, "Pengingat sudah diatur untuk peminjaman: $borrowingId")
            return
        }

        scheduleReminderNotifications(borrowingEndTime)
        scheduleOverdueNotifications(borrowingEndTime)
        scheduleLocationSend(borrowingEndTime)
        prefs.edit().putBoolean(alarmSetKey, true).apply()

        Log.d(
            TAG_NOTIFICATION,
            "Semua pengingat berhasil dijadwalkan untuk peminjaman: $borrowingId"
        )
    }

    private fun scheduleReminderNotifications(borrowingEndTime: Long) {
        Log.d(TAG_NOTIFICATION, "Menjadwalkan notifikasi pengingat")

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val reminders = listOf(30 * 60 * 1000, 15 * 60 * 1000, 5 * 60 * 1000)

        reminders.forEachIndexed { index, delay ->
            val reminderTime = borrowingEndTime - delay
            val intent = Intent(this, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_TIME_REMINDER
                putExtra("BORROWING_ID", borrowingId)
                putExtra("MINUTES_REMAINING", delay / (60 * 1000))
            }

            val requestCode =
                (borrowingId.hashCode() + ReminderReceiver.REQUEST_CODE_TIME_REMINDER + index) and 0xFFFFFFF
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            Log.d(
                TAG_NOTIFICATION,
                "Menjadwalkan pengingat waktu ${index + 1}/3 pada $reminderTime ms (${delay / (60 * 1000)} menit sebelum berakhir)"
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
                Log.d(TAG_NOTIFICATION, "Alarm non-tepat dijadwalkan untuk pengingat ${index + 1}")
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminderTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
                }
                Log.d(TAG_NOTIFICATION, "Alarm tepat dijadwalkan untuk pengingat ${index + 1}")
            }
        }
    }

    private fun scheduleOverdueNotifications(borrowingEndTime: Long) {
        Log.d(TAG_NOTIFICATION, "Menjadwalkan notifikasi keterlambatan")

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val overdueIntervals = listOf(
            5 * 60 * 1000,
            15 * 60 * 1000,
            30 * 60 * 1000,
            60 * 60 * 1000
        ) // 5min, 15min, 30min, 1hour after end time

        overdueIntervals.forEachIndexed { index, delay ->
            val overdueTime = borrowingEndTime + delay
            val intent = Intent(this, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_OVERDUE_REMINDER
                putExtra("BORROWING_ID", borrowingId)
                putExtra("MINUTES_OVERDUE", delay / (60 * 1000))
            }

            val requestCode =
                (borrowingId.hashCode() + ReminderReceiver.REQUEST_CODE_OVERDUE_REMINDER + index) and 0xFFFFFFF
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            Log.d(
                TAG_NOTIFICATION,
                "Menjadwalkan pengingat keterlambatan ${index + 1}/4 pada $overdueTime ms (${delay / (60 * 1000)} menit setelah berakhir)"
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, overdueTime, pendingIntent)
                Log.d(
                    TAG_NOTIFICATION,
                    "Alarm non-tepat dijadwalkan untuk pengingat keterlambatan ${index + 1}"
                )
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        overdueTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, overdueTime, pendingIntent)
                }
                Log.d(
                    TAG_NOTIFICATION,
                    "Alarm tepat dijadwalkan untuk pengingat keterlambatan ${index + 1}"
                )
            }
        }
    }

    private fun getBorrowingEndTimeInMillis(): Long {
        val calendar = Calendar.getInstance()
        Log.d(TAG_NOTIFICATION, "Menghitung waktu akhir peminjaman dari endHour: $endHour")

        if (!endHour.isNullOrEmpty()) {
            val parts = endHour!!.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toIntOrNull() ?: 0
                val minute = parts[1].toIntOrNull() ?: 0
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Log.d(TAG_NOTIFICATION, "Waktu akhir peminjaman diatur ke: ${calendar.time}")
            } else {
                Log.w(TAG_NOTIFICATION, "Format endHour tidak valid: $endHour")
            }
        } else {
            Log.w(TAG_NOTIFICATION, "endHour adalah null atau kosong")
        }
        return calendar.timeInMillis
    }

    private fun scheduleLocationSend(borrowingEndTime: Long) {
        val sendLocationTime = borrowingEndTime + (1 * 60 * 1000)
        Log.d(
            TAG_NOTIFICATION,
            "Menjadwalkan pengiriman lokasi dan pemeriksaan status terlambat pada $sendLocationTime ms (1 menit setelah berakhirnya peminjaman)"
        )

        val intent = Intent(this, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_SEND_LOCATION_AND_CHECK_LATE
            putExtra("BORROWING_ID", borrowingId)
        }

        val requestCode =
            (borrowingId.hashCode() + ReminderReceiver.REQUEST_CODE_LOCATION_SEND) and 0xFFFFFFF // Reuse or create a new request code
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, sendLocationTime, pendingIntent)
            Log.d(
                TAG_NOTIFICATION,
                "Alarm non-tepat dijadwalkan untuk pengiriman lokasi dan pemeriksaan status terlambat"
            )
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    sendLocationTime,
                    pendingIntent
                )
            }
            Log.d(
                TAG_NOTIFICATION,
                "Alarm tepat dijadwalkan untuk pengiriman lokasi dan pemeriksaan status terlambat"
            )
        }
    }

    private fun startTimerService() {
        Log.d(TAG_TIMER, "Memulai TimerService")
        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
            Log.d(TAG_TIMER, "TimerService dimulai sebagai foreground service")
        } else {
            startService(intent)
            Log.d(TAG_TIMER, "TimerService dimulai sebagai layanan biasa")
        }
    }

    private fun stopTimerService() {
        Log.d(TAG_TIMER, "Menghentikan TimerService")
        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
        }
        startService(intent)
    }

    private fun observeTimerState() {
        Log.d(TAG_TIMER, "Menyiapkan pengamatan status timer")

        lifecycleScope.launch {
            TimerState.timeInSeconds.collectLatest { seconds ->
                val formatted = formatTime(seconds)
                binding.tvTimer.text = formatted
                if (seconds % 60 == 0) { // Log setiap menit
                    Log.d(TAG_TIMER, "Timer diperbarui: $formatted")
                }
            }
        }

        val updateIntent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_UPDATE
        }
        startService(updateIntent)
        Log.d(TAG_TIMER, "Permintaan pembaruan timer dikirim")
    }

    private fun observeBorrowingStatus() {
        Log.d(TAG_DATABASE, "Menyiapkan pengamatan status peminjaman untuk ID: $borrowingId")

        databaseRef = FirebaseDatabase.getInstance().getReference("borrowing").child(borrowingId!!)

        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Log.w(TAG_DATABASE, "Data peminjaman telah dihapus dari database")
                    CustomDialog.alert(
                        this@BorrowingTimerActivity,
                        getString(R.string.data_deleted_title),
                        getString(R.string.borrowing_data_deleted)
                    ) {
                        cleanupAndFinish()
                    }
                    return
                }

                val status = snapshot.child("status").getValue(String::class.java)
                Log.d(
                    TAG_DATABASE,
                    "Status peminjaman berubah menjadi: $status untuk ID: $borrowingId"
                )

                if (status == "Returned") {
                    Log.d(
                        TAG_DATABASE,
                        "Item dikembalikan - membersihkan dan menavigasi ke bagian siswa"
                    )

                    stopTimerService()
                    stopLocationMonitoring()
                    cancelAllNotifications()

                    val prefs = getSharedPreferences("BorrowingSession", MODE_PRIVATE)
                    prefs.edit {
                        remove("activeBorrowingId")
                        remove("alarmSet_$borrowingId")
                        apply()
                    }

                    CustomDialog.success(
                        context = this@BorrowingTimerActivity,
                        title = getString(R.string.success_title),
                        message = getString(R.string.item_returned_successfully),
                        onDismiss = {
                            val intent = Intent(
                                this@BorrowingTimerActivity,
                                StudentSectionActivity::class.java
                            ).apply {
                                flags =
                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                            finish()
                        }
                    )
                } else {
                    checkAndSetLateStatus(snapshot)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG_DATABASE, "Gagal membaca status peminjaman: ${error.message}")
                CustomDialog.alert(
                    this@BorrowingTimerActivity,
                    getString(R.string.error_title),
                    getString(R.string.failed_to_check_borrowing_status)
                )
            }
        })
    }

    private fun checkAndSetLateStatus(snapshot: DataSnapshot) {
        val currentStatus = snapshot.child("status").getValue(String::class.java)
        if (currentStatus != "Returned" && currentStatus != "Late") {
            val borrowingEndTime = getBorrowingEndTimeInMillis()
            val currentTime = System.currentTimeMillis()

            if (currentTime > borrowingEndTime) {
                Log.d(
                    TAG_DATABASE,
                    "Peminjaman terlambat. Mengatur status menjadi 'Late' untuk ID: $borrowingId"
                )
                databaseRef.child("status").setValue("Late")
                    .addOnSuccessListener {
                        Log.d(TAG_DATABASE, "Status peminjaman berhasil diperbarui menjadi 'Late'")
                        CustomDialog.alert(
                            this,
                            getString(R.string.late_status_title),
                            getString(R.string.borrowing_time_over_late_status)
                        )
                    }
                    .addOnFailureListener { e ->
                        Log.e(
                            TAG_DATABASE,
                            "Gagal memperbarui status peminjaman menjadi 'Late': ${e.message}"
                        )
                    }
            }
        }
    }

    private fun stopLocationMonitoring() {
        try {
            if (::locationCallback.isInitialized) {
                fusedLocationClient.removeLocationUpdates(locationCallback)
                Log.d(TAG_LOCATION, "Pemantauan lokasi berhasil dihentikan")
            }
        } catch (e: Exception) {
            Log.e(TAG_LOCATION, "Kesalahan menghentikan pembaruan lokasi: ${e.message}")
        }
    }

    private fun cancelAllNotifications() {
        cancelReminderNotifications()
        cancelOverdueNotifications()
        cancelLocationSend()
    }

    private fun cancelReminderNotifications() {
        Log.d(TAG_NOTIFICATION, "Membatalkan notifikasi pengingat untuk peminjaman: $borrowingId")

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val reminders = listOf(30 * 60 * 1000, 15 * 60 * 1000, 5 * 60 * 1000)

        reminders.forEachIndexed { index, _ ->
            val intent = Intent(this, ReminderReceiver::class.java)
            val requestCode =
                (borrowingId.hashCode() + ReminderReceiver.REQUEST_CODE_TIME_REMINDER + index) and 0xFFFFFFF
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            Log.d(TAG_NOTIFICATION, "Pengingat waktu ${index + 1}/3 dibatalkan")
        }
    }

    private fun cancelOverdueNotifications() {
        Log.d(
            TAG_NOTIFICATION,
            "Membatalkan notifikasi keterlambatan untuk peminjaman: $borrowingId"
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val overdueIntervals = listOf(5 * 60 * 1000, 15 * 60 * 1000, 30 * 60 * 1000, 60 * 60 * 1000)

        overdueIntervals.forEachIndexed { index, _ ->
            val intent = Intent(this, ReminderReceiver::class.java)
            val requestCode =
                (borrowingId.hashCode() + ReminderReceiver.REQUEST_CODE_OVERDUE_REMINDER + index) and 0xFFFFFFF
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            Log.d(TAG_NOTIFICATION, "Pengingat keterlambatan ${index + 1}/4 dibatalkan")
        }
    }

    private fun cancelLocationSend() {
        Log.d(
            TAG_NOTIFICATION,
            "Membatalkan alarm pengiriman lokasi untuk peminjaman: $borrowingId"
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_SEND_LOCATION
            putExtra("BORROWING_ID", borrowingId)
        }
        val requestCode =
            (borrowingId.hashCode() + ReminderReceiver.REQUEST_CODE_LOCATION_SEND) and 0xFFFFFFF
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d(TAG_NOTIFICATION, "Alarm pengiriman lokasi dibatalkan")
    }

    private fun preventBackNavigation() {
        Log.d(TAG_MAIN, "Menyiapkan pencegahan navigasi kembali")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(TAG_MAIN, "Navigasi kembali diblokir - sesi peminjaman aktif")
                CustomDialog.alert(
                    this@BorrowingTimerActivity,
                    getString(R.string.blocked_action_title),
                    getString(R.string.cannot_leave_until_returned)
                )
            }
        })
    }

    override fun onDestroy() {
        Log.d(TAG_MAIN, "onDestroy: Membersihkan sumber daya")
        super.onDestroy()
        stopLocationMonitoring()
    }
}