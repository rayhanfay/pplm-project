package com.pplm.projectinventarisuas.utils.permission

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.pplm.projectinventarisuas.utils.components.CustomDialog

object PermissionManager {

    private const val REQUEST_CODE = 1001

    fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return permissions
    }

    fun allPermissionsGranted(context: Context): Boolean {
        return getRequiredPermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun checkExactAlarmPermission(context: Context, onNotGranted: () -> Unit = {}) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                onNotGranted()
                val intent = Intent().apply {
                    action = android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                }
                context.startActivity(intent)
            }
        }
    }

    fun requestPermissions(activity: Activity) {
        val permissionsToRequest = getRequiredPermissions().filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, permissionsToRequest.toTypedArray(), REQUEST_CODE)
        }
    }

    fun handlePermissionResult(
        activity: Activity,
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onAllGranted: () -> Unit
    ) {
        if (requestCode != REQUEST_CODE) return

        val deniedPermission = permissions.indices
            .map { permissions[it] to grantResults[it] }
            .firstOrNull { it.second != PackageManager.PERMISSION_GRANTED }

        if (deniedPermission == null) {
            onAllGranted()
            return
        }

        val (permission, _) = deniedPermission
        val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)

        if (shouldShowRationale) {
            CustomDialog.alert(
                context = activity,
                message = "Aplikasi membutuhkan izin ${permission.substringAfterLast('.')} untuk melanjutkan.",
                onDismiss = {
                    requestPermissions(activity)
                }
            )
        } else {
            CustomDialog.alert(
                context = activity,
                message = "Izin ${permission.substringAfterLast('.')} dibutuhkan agar aplikasi berjalan dengan baik. Aktifkan secara manual di Pengaturan.",
                onDismiss = {
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", activity.packageName, null)
                    }
                    activity.startActivity(intent)
                }
            )
        }
    }
}