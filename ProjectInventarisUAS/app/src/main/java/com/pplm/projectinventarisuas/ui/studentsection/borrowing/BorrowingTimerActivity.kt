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
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import com.google.firebase.database.*
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
            Log.d(TAG_PERMISSION, "Notification permission granted")
        } else {
            Log.w(TAG_PERMISSION, "Notification permission denied")
            Toast.makeText(this, "Permission denied for notifications", Toast.LENGTH_SHORT).show()
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        locationPermissionGranted = permissions.entries.all { it.value }
        if (locationPermissionGranted) {
            Log.d(TAG_PERMISSION, "All location permissions granted")
            startLocationMonitoring()
        } else {
            Log.w(TAG_PERMISSION, "Some location permissions denied: $permissions")
            Toast.makeText(
                this,
                "Location permission required for area monitoring",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG_MAIN, "onCreate: Activity started")

        binding = ActivityBorrowingTimerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        borrowingId = intent.getStringExtra("BORROWING_ID")
        endHour = intent.getStringExtra("END_HOUR")

        Log.d(TAG_MAIN, "onCreate: borrowingId=$borrowingId, endHour=$endHour")

        if (borrowingId == null) {
            Log.e(TAG_MAIN, "onCreate: Borrowing ID is null, finishing activity")
            Toast.makeText(this, "Borrowing ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        geofenceClient = LocationServices.getGeofencingClient(this)
        Log.d(TAG_LOCATION, "Location services initialized")

        preventBackNavigation()
        checkAndRequestPermissions()

        // Check if borrowing exists first, then proceed with other initialization
        checkBorrowingExists()
    }

    private fun checkBorrowingExists() {
        Log.d(TAG_DATABASE, "Checking if borrowing ID exists: $borrowingId")

        databaseRef = FirebaseDatabase.getInstance().getReference("borrowing").child(borrowingId!!)
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    Log.d(TAG_DATABASE, "Borrowing ID exists, proceeding with initialization")
                    // Proceed with normal initialization
                    observeTimerState()
                    observeBorrowingStatus()
                    startTimerService()
                    setReminderForBorrowingEnd()
                } else {
                    Log.w(
                        TAG_DATABASE,
                        "Borrowing ID does not exist in database, finishing activity"
                    )
                    Toast.makeText(
                        this@BorrowingTimerActivity,
                        "Data peminjaman tidak ditemukan",
                        Toast.LENGTH_SHORT
                    ).show()
                    cleanupAndFinish()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG_DATABASE, "Failed to check borrowing existence: ${error.message}")
                Toast.makeText(
                    this@BorrowingTimerActivity,
                    "Gagal memverifikasi data peminjaman",
                    Toast.LENGTH_SHORT
                ).show()
                cleanupAndFinish()
            }
        })
    }

    private fun cleanupAndFinish() {
        Log.d(TAG_MAIN, "Cleaning up and finishing activity")

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
        Log.d(TAG_PERMISSION, "Starting permission checks")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasExactAlarmPermission()) {
                Log.d(TAG_PERMISSION, "Exact alarm permission not granted, requesting")
                requestExactAlarmPermission()
            } else {
                Log.d(TAG_PERMISSION, "Exact alarm permission already granted")
            }
        }
        checkNotificationPermission()
        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        Log.d(TAG_PERMISSION, "Checking location permissions")

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
            Log.d(TAG_PERMISSION, "Background location permission added to request list")
        }

        when {
            permissionsToRequest.all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            } -> {
                Log.d(TAG_PERMISSION, "All location permissions already granted")
                locationPermissionGranted = true
                startLocationMonitoring()
            }

            else -> {
                Log.d(TAG_PERMISSION, "Requesting location permissions: $permissionsToRequest")
                locationPermissionLauncher.launch(permissionsToRequest.toTypedArray())
            }
        }
    }

    private fun startLocationMonitoring() {
        if (!locationPermissionGranted) {
            Log.w(TAG_LOCATION, "Cannot start location monitoring - permission not granted")
            return
        }

        Log.d(TAG_LOCATION, "Starting location monitoring")

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(5000)
            .setMaxUpdateDelayMillis(15000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                Log.d(
                    TAG_LOCATION,
                    "Location update received with ${result.locations.size} locations"
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
            Log.d(TAG_LOCATION, "Location updates requested successfully")
        } catch (e: SecurityException) {
            Log.e(TAG_LOCATION, "Security exception when requesting location updates: ${e.message}")
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
            "Current distance to target: ${distanceInMeters}m (limit: ${radiusInMeters}m)"
        )

        if (distanceInMeters > radiusInMeters && !isOutsideRadius) {
            Log.w(
                TAG_LOCATION,
                "User moved outside permitted radius - triggering out of range alert"
            )
            isOutsideRadius = true
            showOutOfRangeReminder()
        } else if (distanceInMeters <= radiusInMeters && isOutsideRadius) {
            Log.i(TAG_LOCATION, "User returned to permitted radius")
            isOutsideRadius = false
        }
    }

    private fun showOutOfRangeReminder() {
        Log.d(TAG_NOTIFICATION, "Sending out of range notification for borrowing: $borrowingId")

        val intent = Intent(this, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_OUT_OF_RANGE
            putExtra("BORROWING_ID", borrowingId)
        }

        val requestCode = (borrowingId.hashCode() + ReminderReceiver.REQUEST_CODE_OUT_OF_RANGE) and 0xFFFFFFF
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            pendingIntent.send()
            Log.d(TAG_NOTIFICATION, "Out of range notification sent successfully")
        } catch (e: PendingIntent.CanceledException) {
            Log.e(TAG_NOTIFICATION, "Failed to send out of range notification: ${e.message}")
        }
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.d(TAG_PERMISSION, "Requesting exact alarm permission")
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
            Log.d(TAG_PERMISSION, "Exact alarm permission status: $canSchedule")
            canSchedule
        } else {
            Log.d(TAG_PERMISSION, "Exact alarm permission not required for this Android version")
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
                Log.d(TAG_PERMISSION, "Requesting notification permission")
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                Log.d(TAG_PERMISSION, "Notification permission already granted")
            }
        } else {
            Log.d(TAG_PERMISSION, "Notification permission not required for this Android version")
        }
    }

    private fun setReminderForBorrowingEnd() {
        Log.d(TAG_NOTIFICATION, "Setting up borrowing end reminders for ID: $borrowingId")

        val borrowingEndTime = getBorrowingEndTimeInMillis()
        val prefs = getSharedPreferences("BorrowingSession", MODE_PRIVATE)
        val alarmSetKey = "alarmSet_$borrowingId"

        if (prefs.getBoolean(alarmSetKey, false)) {
            Log.d(TAG_NOTIFICATION, "Reminders already set for borrowing: $borrowingId")
            return
        }

        scheduleReminderNotifications(borrowingEndTime)
        scheduleOverdueNotifications(borrowingEndTime) // New function for overdue notifications
        scheduleLocationSend(borrowingEndTime)
        prefs.edit().putBoolean(alarmSetKey, true).apply()

        Log.d(TAG_NOTIFICATION, "All reminders scheduled successfully for borrowing: $borrowingId")
    }

    private fun scheduleReminderNotifications(borrowingEndTime: Long) {
        Log.d(TAG_NOTIFICATION, "Scheduling reminder notifications")

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
                "Scheduling time reminder ${index + 1}/3 at $reminderTime ms (${delay / (60 * 1000)} minutes before end)"
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
                Log.d(TAG_NOTIFICATION, "Non-exact alarm scheduled for reminder ${index + 1}")
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
                Log.d(TAG_NOTIFICATION, "Exact alarm scheduled for reminder ${index + 1}")
            }
        }
    }

    private fun scheduleOverdueNotifications(borrowingEndTime: Long) {
        Log.d(TAG_NOTIFICATION, "Scheduling overdue notifications")

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
                "Scheduling overdue reminder ${index + 1}/4 at $overdueTime ms (${delay / (60 * 1000)} minutes after end)"
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, overdueTime, pendingIntent)
                Log.d(
                    TAG_NOTIFICATION,
                    "Non-exact alarm scheduled for overdue reminder ${index + 1}"
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
                Log.d(TAG_NOTIFICATION, "Exact alarm scheduled for overdue reminder ${index + 1}")
            }
        }
    }

    private fun getBorrowingEndTimeInMillis(): Long {
        val calendar = Calendar.getInstance()
        Log.d(TAG_NOTIFICATION, "Calculating borrowing end time from endHour: $endHour")

        if (!endHour.isNullOrEmpty()) {
            val parts = endHour!!.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toIntOrNull() ?: 0
                val minute = parts[1].toIntOrNull() ?: 0
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Log.d(TAG_NOTIFICATION, "Borrowing end time set to: ${calendar.time}")
            } else {
                Log.w(TAG_NOTIFICATION, "Invalid endHour format: $endHour")
            }
        } else {
            Log.w(TAG_NOTIFICATION, "endHour is null or empty")
        }
        return calendar.timeInMillis
    }

    private fun scheduleLocationSend(borrowingEndTime: Long) {
        val sendLocationTime = borrowingEndTime + (1 * 60 * 1000)
        Log.d(TAG_NOTIFICATION, "Scheduling location send and late status check at $sendLocationTime ms (1 minute after borrowing end)")

        val intent = Intent(this, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_SEND_LOCATION_AND_CHECK_LATE
            putExtra("BORROWING_ID", borrowingId)
        }

        val requestCode = (borrowingId.hashCode() + ReminderReceiver.REQUEST_CODE_LOCATION_SEND) and 0xFFFFFFF // Reuse or create a new request code
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, sendLocationTime, pendingIntent)
            Log.d(TAG_NOTIFICATION, "Non-exact alarm scheduled for location send and late status check")
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    sendLocationTime,
                    pendingIntent
                )
            }
            Log.d(TAG_NOTIFICATION, "Exact alarm scheduled for location send and late status check")
        }
    }

    private fun startTimerService() {
        Log.d(TAG_TIMER, "Starting TimerService")
        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
            Log.d(TAG_TIMER, "TimerService started as foreground service")
        } else {
            startService(intent)
            Log.d(TAG_TIMER, "TimerService started as regular service")
        }
    }

    private fun stopTimerService() {
        Log.d(TAG_TIMER, "Stopping TimerService")
        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
        }
        startService(intent)
    }

    private fun observeTimerState() {
        Log.d(TAG_TIMER, "Setting up timer state observation")

        lifecycleScope.launch {
            TimerState.timeInSeconds.collectLatest { seconds ->
                val formatted = formatTime(seconds)
                binding.tvTimer.text = formatted
                if (seconds % 60 == 0) { // Log every minute
                    Log.d(TAG_TIMER, "Timer updated: $formatted")
                }
            }
        }

        val updateIntent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_UPDATE
        }
        startService(updateIntent)
        Log.d(TAG_TIMER, "Timer update request sent")
    }

    private fun observeBorrowingStatus() {
        Log.d(TAG_DATABASE, "Setting up borrowing status observation for ID: $borrowingId")

        databaseRef = FirebaseDatabase.getInstance().getReference("borrowing").child(borrowingId!!)

        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Log.w(TAG_DATABASE, "Borrowing data has been deleted from database")
                    Toast.makeText(
                        this@BorrowingTimerActivity,
                        "Data peminjaman telah dihapus",
                        Toast.LENGTH_SHORT
                    ).show()
                    cleanupAndFinish()
                    return
                }

                val status = snapshot.child("status").getValue(String::class.java)
                Log.d(TAG_DATABASE, "Borrowing status changed to: $status for ID: $borrowingId")

                if (status == "Returned") {
                    Log.d(
                        TAG_DATABASE,
                        "Item returned - cleaning up and navigating to student section"
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
                        title = "Sukses",
                        message = "Barang telah berhasil dikembalikan",
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
                Log.e(TAG_DATABASE, "Failed to read borrowing status: ${error.message}")
                Toast.makeText(
                    this@BorrowingTimerActivity,
                    "Gagal memeriksa status peminjaman",
                    Toast.LENGTH_SHORT
                ).show()
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
                    "Borrowing is overdue. Setting status to 'Late' for ID: $borrowingId"
                )
                databaseRef.child("status").setValue("Late")
                    .addOnSuccessListener {
                        Log.d(TAG_DATABASE, "Borrowing status successfully updated to 'Late'")
                        Toast.makeText(
                            this,
                            "Waktu peminjaman telah habis. Status diperbarui menjadi Terlambat.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        Log.e(
                            TAG_DATABASE,
                            "Failed to update borrowing status to 'Late': ${e.message}"
                        )
                    }
            }
        }
    }

    private fun stopLocationMonitoring() {
        try {
            if (::locationCallback.isInitialized) {
                fusedLocationClient.removeLocationUpdates(locationCallback)
                Log.d(TAG_LOCATION, "Location monitoring stopped successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG_LOCATION, "Error stopping location updates: ${e.message}")
        }
    }

    private fun cancelAllNotifications() {
        cancelReminderNotifications()
        cancelOverdueNotifications()
        cancelLocationSend()
    }

    private fun cancelReminderNotifications() {
        Log.d(TAG_NOTIFICATION, "Cancelling reminder notifications for borrowing: $borrowingId")

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
            Log.d(TAG_NOTIFICATION, "Cancelled time reminder ${index + 1}/3")
        }
    }

    private fun cancelOverdueNotifications() {
        Log.d(TAG_NOTIFICATION, "Cancelling overdue notifications for borrowing: $borrowingId")

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
            Log.d(TAG_NOTIFICATION, "Cancelled overdue reminder ${index + 1}/4")
        }
    }

    private fun cancelLocationSend() {
        Log.d(TAG_NOTIFICATION, "Cancelling location send alarm for borrowing: $borrowingId")

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
        Log.d(TAG_NOTIFICATION, "Location send alarm cancelled")
    }

    private fun preventBackNavigation() {
        Log.d(TAG_MAIN, "Setting up back navigation prevention")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(TAG_MAIN, "Back navigation blocked - borrowing session active")
                Toast.makeText(
                    this@BorrowingTimerActivity,
                    "You cannot leave until the item is returned",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    override fun onDestroy() {
        Log.d(TAG_MAIN, "onDestroy: Cleaning up resources")
        super.onDestroy()
        stopLocationMonitoring()
    }
}