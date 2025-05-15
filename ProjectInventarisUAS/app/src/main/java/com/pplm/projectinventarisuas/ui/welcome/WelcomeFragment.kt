package com.pplm.projectinventarisuas.ui.welcome

import android.Manifest
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
import com.pplm.projectinventarisuas.databinding.FragmentWelcomeBinding
import com.pplm.projectinventarisuas.utils.components.CustomDialog

class WelcomeFragment : Fragment() {

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_TEXT = "text"
        private const val ARG_IS_LAST = "is_last"
        private const val REQUEST_CODE_PERMISSIONS = 100

        fun getRequiredPermissions(): Array<String> {
            val basePermissions = mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                basePermissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            return basePermissions.toTypedArray()
        }

        fun newInstance(text: String, isLast: Boolean = false): WelcomeFragment {
            return WelcomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TEXT, text)
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

        val text = arguments?.getString(ARG_TEXT) ?: ""
        val isLast = arguments?.getBoolean(ARG_IS_LAST) ?: false

        binding.tvWelcome.text = text
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
            if (allPermissionsGranted()) {
                (requireActivity() as WelcomeActivity).goToLogin()
            } else {
                requestPermissions(getRequiredPermissions(), REQUEST_CODE_PERMISSIONS)
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

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                (requireActivity() as WelcomeActivity).goToLogin()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Semua permission harus diberikan untuk melanjutkan.",
                    Toast.LENGTH_SHORT
                ).show()

                val shouldShowRequestPermissionRationale = permissions.any {
                    shouldShowRequestPermissionRationale(it)
                }

                if (!shouldShowRequestPermissionRationale) {
                    CustomDialog.alert(
                        context = requireContext(),
                        message = "Permissions are required for the app to work properly. Please grant them in the settings.",
                        onDismiss = {
                            val intent =
                                Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri: Uri =
                                Uri.fromParts("package", requireContext().packageName, null)
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