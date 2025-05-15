package com.pplm.projectinventarisuas.ui.studentsection.borrowing

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.*
import com.pplm.projectinventarisuas.databinding.ActivityBorrowingTimerBinding
import com.pplm.projectinventarisuas.ui.studentsection.StudentSectionActivity
import com.pplm.projectinventarisuas.utils.ReminderReceiver
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

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Permission denied for notifications", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBorrowingTimerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        borrowingId = intent.getStringExtra("BORROWING_ID")
        endHour = intent.getStringExtra("END_HOUR")
        if (borrowingId == null) {
            Toast.makeText(this, "Borrowing ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        preventBackNavigation()
        checkAndRequestPermissions()
        observeTimerState()
        observeBorrowingStatus()
        startTimerService()
        setReminderForBorrowingEnd()
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasExactAlarmPermission()) {
                requestExactAlarmPermission()
            }
        }
        checkNotificationPermission()
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent().apply {
                action = android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
            }
            startActivity(intent)
        }
    }

    private fun hasExactAlarmPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else true
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setReminderForBorrowingEnd() {
        val borrowingEndTime = getBorrowingEndTimeInMillis()
        val prefs = getSharedPreferences("BorrowingSession", MODE_PRIVATE)
        val alarmSetKey = "alarmSet_$borrowingId"

        if (prefs.getBoolean(alarmSetKey, false)) return

        scheduleReminderNotifications(borrowingEndTime)
        scheduleLocationSend(borrowingEndTime)
        prefs.edit().putBoolean(alarmSetKey, true).apply()
    }

    private fun scheduleReminderNotifications(borrowingEndTime: Long) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val reminders = listOf(30 * 60 * 1000, 15 * 60 * 1000, 5 * 60 * 1000)

        reminders.forEachIndexed { index, delay ->
            val reminderTime = borrowingEndTime - delay
            val intent = Intent(this, ReminderReceiver::class.java).apply {
                action = "REMINDER_ACTION"
                putExtra("BORROWING_ID", borrowingId)
            }

            val requestCode = (borrowingId.hashCode() + index) and 0xFFFFFFF
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            Log.d("BorrowingLog", "Scheduling reminder $index at $reminderTime ms for ID: $borrowingId")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
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
            }
        }
    }

    private fun getBorrowingEndTimeInMillis(): Long {
        val calendar = Calendar.getInstance()
        if (!endHour.isNullOrEmpty()) {
            val parts = endHour!!.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toIntOrNull() ?: 0
                val minute = parts[1].toIntOrNull() ?: 0
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
        }
        return calendar.timeInMillis
    }

    private fun scheduleLocationSend(borrowingEndTime: Long) {
        val sendLocationTime = borrowingEndTime + (1 * 60 * 1000)
        val intent = Intent(this, ReminderReceiver::class.java).apply {
            action = "SEND_LOCATION_ACTION"
            putExtra("BORROWING_ID", borrowingId)
        }

        val requestCode = (borrowingId.hashCode() + 999) and 0xFFFFFFF
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        Log.d("BorrowingLog", "Scheduling location send at $sendLocationTime ms for ID: $borrowingId")

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, sendLocationTime, pendingIntent)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    sendLocationTime,
                    pendingIntent
                )
            }
        }
    }

    private fun startTimerService() {
        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_START
        }
        Log.d("BorrowingLog", "Starting TimerService")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopTimerService() {
        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
        }
        Log.d("BorrowingLog", "Stopping TimerService")
        startService(intent)
    }

    private fun observeTimerState() {
        lifecycleScope.launch {
            TimerState.timeInSeconds.collectLatest { seconds ->
                val formatted = formatTime(seconds)
                binding.tvTimer.text = formatted
            }
        }

        val updateIntent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_UPDATE
        }
        startService(updateIntent)
    }

    private fun observeBorrowingStatus() {
        databaseRef = FirebaseDatabase.getInstance().getReference("borrowing").child(borrowingId!!)
        databaseRef.child("status").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.getValue(String::class.java)
                Log.d("BorrowingLog", "Status changed: $status")
                if (status == "Returned") {
                    val prefs = getSharedPreferences("BorrowingSession", MODE_PRIVATE)
                    prefs.edit() { remove("activeBorrowingId") }
                    prefs.edit() { remove("alarmSet_$borrowingId") }

                    stopTimerService()
                    cancelReminderNotifications()
                    cancelLocationSend()

                    val intent = Intent(
                        this@BorrowingTimerActivity,
                        StudentSectionActivity::class.java
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@BorrowingTimerActivity,
                    "Failed to read status",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("BorrowingLog", "Failed to read status: ${error.message}")
            }
        })
    }

    private fun cancelReminderNotifications() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val reminders = listOf(30 * 60 * 1000, 15 * 60 * 1000, 5 * 60 * 1000)
        reminders.forEachIndexed { index, _ ->
            val intent = Intent(this, ReminderReceiver::class.java)
            val requestCode = (borrowingId.hashCode() + index) and 0xFFFFFFF
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            Log.d("BorrowingLog", "Cancelled reminder $index for ID: $borrowingId")
        }
    }

    private fun cancelLocationSend() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java).apply {
            action = "SEND_LOCATION_ACTION"
            putExtra("BORROWING_ID", borrowingId)
        }
        val requestCode = (borrowingId.hashCode() + 999) and 0xFFFFFFF
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d("BorrowingLog", "Cancelled location send for ID: $borrowingId")
    }

    private fun preventBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(
                    this@BorrowingTimerActivity,
                    "You cannot leave until the item is returned",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
}