package com.pplm.projectinventarisuas.ui.welcome

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.pplm.projectinventarisuas.R
import com.pplm.projectinventarisuas.databinding.FragmentWelcomeBinding
import com.pplm.projectinventarisuas.utils.components.CustomDialog

class WelcomeFragment : Fragment() {

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

    private var currentPermissionIndex = 0
    private val permissionList by lazy { getRequiredPermissions().toList() }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_DESCRIPTION = "description"
        private const val ARG_IS_LAST = "is_last"
        private const val ARG_IMAGE_RES = "image_res"
        private const val REQUEST_CODE_PERMISSIONS = 100

        fun getRequiredPermissions(): Array<String> {
            val basePermissions = mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                basePermissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                basePermissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            return basePermissions.toTypedArray()
        }

        fun newInstance(title: String, description: String, imageRes: Int, isLast: Boolean = false): WelcomeFragment {
            return WelcomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_DESCRIPTION, description)
                    putInt(ARG_IMAGE_RES, imageRes)
                    putBoolean(ARG_IS_LAST, isLast)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = arguments?.getString(ARG_TITLE) ?: ""
        val description = arguments?.getString(ARG_DESCRIPTION) ?: ""
        val imageRes = arguments?.getInt(ARG_IMAGE_RES) ?: 0
        val isLast = arguments?.getBoolean(ARG_IS_LAST) ?: false

        binding.tvTitle.text = title
        if (imageRes != 0) {
            binding.ivWelcomeImage.setImageResource(imageRes)
        }
        binding.tvDescription.text = description
        binding.btnSkip.visibility = if (isLast) View.GONE else View.VISIBLE
        binding.btnFinish.visibility = if (isLast) View.VISIBLE else View.GONE

        binding.btnSkip.setOnClickListener {
            if (isLast) {
                (requireActivity() as WelcomeActivity).goToLogin()
            } else {
                (requireActivity() as WelcomeActivity).goToNextFragment()
            }
        }

        binding.btnFinish.setOnClickListener {
            checkExactAlarmPermission()
            if (allPermissionsGranted()) {
                (requireActivity() as WelcomeActivity).goToLogin()
            } else {
                currentPermissionIndex = 0
                requestNextPermission()
            }
        }
    }

    private fun allPermissionsGranted(): Boolean {
        return getRequiredPermissions().all {
            ContextCompat.checkSelfPermission(
                requireContext(),
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent().apply {
                    action = android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                }
                startActivity(intent)
            }
        }
    }

    private fun requestNextPermission() {
        if (currentPermissionIndex >= permissionList.size) {
            if (allPermissionsGranted()) {
                (requireActivity() as WelcomeActivity).goToLogin()
            }
            return
        }

        val permission = permissionList[currentPermissionIndex]
        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            currentPermissionIndex++
            requestNextPermission()
        } else {
            requestPermissions(arrayOf(permission), REQUEST_CODE_PERMISSIONS)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            val permission = permissions.firstOrNull() ?: return
            val granted = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED

            if (granted) {
                currentPermissionIndex++
                requestNextPermission()
            } else {
                val shouldShowRationale = shouldShowRequestPermissionRationale(permission)
                if (shouldShowRationale) {
                    CustomDialog.alert(
                        context = requireContext(),
                        message = getString(R.string.permission_required_message, permission.substringAfterLast('.')),
                        onDismiss = {
                            requestPermissions(arrayOf(permission), REQUEST_CODE_PERMISSIONS)
                        }
                    )
                } else {
                    CustomDialog.alert(
                        context = requireContext(),
                        message = getString(R.string.permission_manual_enable_message, permission.substringAfterLast('.')),
                        onDismiss = {
                            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri: Uri = Uri.fromParts("package", requireContext().packageName, null)
                            intent.data = uri
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
