package com.pplm.projectinventarisuas.ui.adminsection.borrowing

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.pplm.projectinventarisuas.R
import com.pplm.projectinventarisuas.data.model.Borrowing
import com.pplm.projectinventarisuas.data.repository.BorrowingRepository
import com.pplm.projectinventarisuas.data.repository.ItemRepository
import com.pplm.projectinventarisuas.data.repository.UserRepository
import com.pplm.projectinventarisuas.databinding.DialogBorrowingDetailBinding
import com.pplm.projectinventarisuas.utils.components.CustomDialog
import com.pplm.projectinventarisuas.utils.viewmodel.BorrowingViewModel
import com.pplm.projectinventarisuas.utils.viewmodel.ViewModelFactory
import android.util.Log
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build

class BorrowingDetailDialogFragment : DialogFragment() {

    private var _binding: DialogBorrowingDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: BorrowingViewModel
    private lateinit var borrowing: Borrowing
    private lateinit var originalBorrowing: Borrowing
    private var isEditMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog_NoActionBar_MinWidth)
        Log.d("BorrowingDetailDialog", "onCreate dipanggil.")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (_binding == null) {
            _binding = DialogBorrowingDetailBinding.inflate(inflater, container, false)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("BorrowingDetailDialog", "onViewCreated dipanggil.")

        setupViewModel()

        arguments?.let {
            borrowing = it.getParcelable("borrowing")!!
            originalBorrowing = borrowing.copy()
            isEditMode = it.getBoolean("isEditMode", false)
            Log.d(
                "BorrowingDetailDialog",
                "Argumen diterima. ID Peminjaman: ${borrowing.borrowing_id}, isEditMode: $isEditMode"
            )
        } ?: run {
            Log.e("BorrowingDetailDialog", "Argumen null! Objek peminjaman tidak ada.")
            dismiss()
            return
        }

        displayBorrowingDetails(borrowing)
        setupUserPermission()
        setupButtons()
        setupActionButtons()
    }

    private fun setupViewModel() {
        Log.d("BorrowingDetailDialog", "setupViewModel dipanggil.")
        val factory = ViewModelFactory(ItemRepository(), BorrowingRepository(), UserRepository())
        viewModel = ViewModelProvider(this, factory)[BorrowingViewModel::class.java]
    }

    private fun setupUserPermission() {
        val userRole = getUserRole()
        if (userRole == "admin") {
            setEditMode(isEditMode)
        } else {
            setEditMode(false)
            binding.btnEdit.visibility = View.GONE
            binding.btnSave.visibility = View.GONE
            binding.btnCancel.visibility = View.GONE
        }
    }

    private fun setupButtons() {
        binding.btnEdit.setOnClickListener {
            setEditMode(true)
        }

        binding.btnSave.setOnClickListener {
            saveBorrowingDetails()
        }

        binding.btnCancel.setOnClickListener {
            confirmCancel()
        }

        binding.btnClose.setOnClickListener {
            if (isEditMode && hasUnsavedChanges()) {
                CustomDialog.confirm(
                    context = requireContext(),
                    message = getString(R.string.unsaved_changes_close),
                    onConfirm = { dismiss() }
                )
            } else {
                dismiss()
            }
        }
    }

    private fun setupActionButtons() {
        binding.btnStudentPhone.setOnClickListener {
            val phoneNumber = binding.etStudentPhoneNumber.text.toString()
            if (phoneNumber.isNotEmpty() && phoneNumber != "-") {
                openPhoneChooser(phoneNumber)
            } else {
                CustomDialog.alert(requireContext(), message = getString(R.string.no_phone_number))
            }
        }

        binding.btnAdminPhone.setOnClickListener {
            val phoneNumber = binding.etAdminPhoneNumber.text.toString()
            if (phoneNumber.isNotEmpty() && phoneNumber != "-") {
                openPhoneChooser(phoneNumber)
            } else {
                CustomDialog.alert(requireContext(), message = getString(R.string.no_phone_number))
            }
        }

        binding.btnOpenMap.setOnClickListener {
            val coordinates = binding.etLastLocation.text.toString()
            if (coordinates.isNotEmpty() && coordinates != "-") {
                openMap(coordinates)
            } else {
                CustomDialog.alert(
                    requireContext(),
                    message = getString(R.string.last_location_not_available)
                )
            }
        }
    }

    private fun openPhoneChooser(phoneNumber: String) {
        val cleanedPhoneNumber = phoneNumber.replace("[^\\d]".toRegex(), "")

        val targetIntents: MutableList<Intent> = ArrayList()

        val callIntent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$cleanedPhoneNumber")
        }
        targetIntents.add(callIntent)

        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$cleanedPhoneNumber")
        }
        targetIntents.add(smsIntent)

        addWhatsAppIntents(cleanedPhoneNumber, targetIntents)

        if (targetIntents.isEmpty()) {
            CustomDialog.alert(requireContext(), message = getString(R.string.no_app_to_handle))
            return
        }

        val chooserIntent =
            Intent.createChooser(targetIntents[0], getString(R.string.select_contact_method))

        if (targetIntents.size > 1) {
            val remainingIntents = targetIntents.drop(1).toTypedArray()
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, remainingIntents)
        }

        try {
            startActivity(chooserIntent)
        } catch (e: Exception) {
            CustomDialog.alert(requireContext(), message = getString(R.string.no_app_to_handle))
            Log.e("BorrowingDetailDialog", "Failed to open phone/WhatsApp chooser: ${e.message}")
        }
    }

    private fun addWhatsAppIntents(phoneNumber: String, targetIntents: MutableList<Intent>) {
        val packageManager = requireContext().packageManager

        val whatsappPackages = listOf(
            "com.whatsapp",
            "com.whatsapp.w4b"
        )

        for (packageName in whatsappPackages) {
            try {
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                val whatsappIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber")
                    setPackage(packageName)
                }

                if (packageManager.resolveActivity(whatsappIntent, 0) != null) {
                    targetIntents.add(whatsappIntent)
                    Log.d(
                        "BorrowingDetailDialog",
                        "Added WhatsApp intent for package: $packageName"
                    )
                }
            } catch (e: Exception) {
                Log.d("BorrowingDetailDialog", "Package $packageName not found or not accessible")
            }
        }

        val whatsappBaseIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber")
        }

        val resolveInfos: List<ResolveInfo> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    whatsappBaseIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(
                    whatsappBaseIntent,
                    PackageManager.MATCH_DEFAULT_ONLY
                )
            }

        for (info in resolveInfos) {
            val packageName = info.activityInfo.packageName

            if (targetIntents.any { it.`package` == packageName }) {
                continue
            }

            val intent = Intent(whatsappBaseIntent).apply {
                setPackage(packageName)
            }
            targetIntents.add(intent)
            Log.d(
                "BorrowingDetailDialog",
                "Added additional WhatsApp-like intent for package: $packageName"
            )
        }
    }

    private fun openMap(coordinates: String) {
        val parts = coordinates.split(",")
        if (parts.size != 2) {
            CustomDialog.alert(requireContext(), message = getString(R.string.invalid_coordinates))
            Log.e("BorrowingDetailDialog", "Invalid coordinates format: $coordinates")
            return
        }

        val latitude = parts[0].trim()
        val longitude = parts[1].trim()

        try {
            val lat = latitude.toDouble()
            val lng = longitude.toDouble()

            if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
                throw NumberFormatException("Invalid coordinate range")
            }
        } catch (e: NumberFormatException) {
            CustomDialog.alert(requireContext(), message = getString(R.string.invalid_coordinates))
            Log.e("BorrowingDetailDialog", "Invalid coordinates values: $coordinates")
            return
        }

        val targetIntents: MutableList<Intent> = ArrayList()

        val geoIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
        }
        targetIntents.add(geoIntent)

        val googleMapsIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://maps.google.com/?q=$latitude,$longitude")
            setPackage("com.google.android.apps.maps")
        }

        val packageManager = requireContext().packageManager
        try {
            packageManager.getPackageInfo("com.google.android.apps.maps", 0)
            targetIntents.add(googleMapsIntent)
        } catch (e: Exception) {
            Log.d("BorrowingDetailDialog", "Google Maps not installed")
        }

        val resolveInfos: List<ResolveInfo> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    geoIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(geoIntent, PackageManager.MATCH_DEFAULT_ONLY)
            }

        for (info in resolveInfos) {
            val packageName = info.activityInfo.packageName

            if (targetIntents.any { it.`package` == packageName } ||
                packageName == requireContext().packageName) {
                continue
            }

            val intent = Intent(geoIntent).apply {
                setPackage(packageName)
            }
            targetIntents.add(intent)
        }

        if (targetIntents.isEmpty()) {
            CustomDialog.alert(requireContext(), message = getString(R.string.no_app_to_handle))
            return
        }

        val chooserIntent =
            Intent.createChooser(targetIntents[0], getString(R.string.open_map_with))

        if (targetIntents.size > 1) {
            val remainingIntents = targetIntents.drop(1).toTypedArray()
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, remainingIntents)
        }

        try {
            startActivity(chooserIntent)
        } catch (e: Exception) {
            CustomDialog.alert(requireContext(), message = getString(R.string.no_app_to_handle))
            Log.e("BorrowingDetailDialog", "Failed to open map chooser: ${e.message}")
        }
    }

    private fun saveBorrowingDetails() {
        Log.d("BorrowingDetailDialog", "saveBorrowingDetails dipanggil.")
        val userRole = getUserRole()
        if (userRole == "admin") {
            val updatedBorrowing = Borrowing(
                borrowing_id = binding.etBorrowingCode.text.toString(),
                item_id = borrowing.item_id,
                student_id = borrowing.student_id,
                admin_id = borrowing.admin_id,
                date_borrowed = binding.etDateBorrowed.text.toString(),
                start_hour = binding.etStartHours.text.toString(),
                end_hour = binding.etEndHours.text.toString(),
                last_location = binding.etLastLocation.text.toString(),
                return_time = binding.etReturnTime.text.toString(),
                status = binding.etStatus.text.toString(),
                student_name = binding.etStudentName.text.toString(),
                student_phone_number = binding.etStudentPhoneNumber.text.toString(),
                admin_name = binding.etAdminName.text.toString(),
                admin_phone_number = binding.etAdminPhoneNumber.text.toString(),
                item_name = borrowing.item_name
            )
            Log.d("BorrowingDetailDialog", "Memperbarui peminjaman: $updatedBorrowing")

            CustomDialog.confirm(
                context = requireContext(),
                message = getString(R.string.save_confirmation),
                onConfirm = {
                    viewModel.updateBorrowing(updatedBorrowing)
                    CustomDialog.success(
                        context = requireContext(),
                        message = getString(R.string.save_success),
                        onDismiss = {
                            borrowing = updatedBorrowing
                            originalBorrowing =
                                borrowing.copy()
                            setEditMode(false)
                        }
                    )
                }
            )
        } else {
            CustomDialog.alert(
                context = requireContext(),
                message = getString(R.string.no_permission_to_edit)
            )
        }
    }

    private fun setEditMode(enabled: Boolean) {
        Log.d("BorrowingDetailDialog", "setEditMode dipanggil dengan enabled: $enabled")
        isEditMode = enabled

        with(binding) {
            etBorrowingCode.isEnabled = false
            etItemName.isEnabled = false
            etDateBorrowed.isEnabled = enabled
            etStartHours.isEnabled = enabled
            etEndHours.isEnabled = enabled
            etStudentName.isEnabled = enabled
            etStudentPhoneNumber.isEnabled = enabled
            etAdminName.isEnabled = enabled
            etAdminPhoneNumber.isEnabled = enabled
            etLastLocation.isEnabled = enabled
            etReturnTime.isEnabled = enabled
            etStatus.isEnabled = enabled

            btnStudentPhone.visibility = if (enabled) View.GONE else View.VISIBLE
            btnAdminPhone.visibility = if (enabled) View.GONE else View.VISIBLE
            btnOpenMap.visibility = if (enabled) View.GONE else View.VISIBLE
        }

        binding.btnEdit.visibility = if (enabled) View.GONE else View.VISIBLE
        binding.btnSave.visibility = if (enabled) View.VISIBLE else View.GONE
        binding.btnCancel.visibility =
            if (enabled) View.VISIBLE else View.GONE
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState)
    }

    private fun displayBorrowingDetails(borrowingToDisplay: Borrowing) {
        Log.d(
            "BorrowingDetailDialog",
            "displayBorrowingDetails dipanggil. Objek peminjaman: $borrowingToDisplay"
        )
        with(binding) {
            etBorrowingCode.setText(borrowingToDisplay.borrowing_id)
            etItemName.setText(borrowingToDisplay.item_name)
            etDateBorrowed.setText(borrowingToDisplay.date_borrowed)
            etStartHours.setText(borrowingToDisplay.start_hour)
            etEndHours.setText(borrowingToDisplay.end_hour)
            etStudentName.setText(borrowingToDisplay.student_name)
            etStudentPhoneNumber.setText(borrowingToDisplay.student_phone_number)
            etAdminName.setText(borrowingToDisplay.admin_name)
            etAdminPhoneNumber.setText(borrowingToDisplay.admin_phone_number)
            etLastLocation.setText(borrowingToDisplay.last_location)
            etReturnTime.setText(borrowingToDisplay.return_time)
            etStatus.setText(borrowingToDisplay.status)
        }
        Log.d("BorrowingDetailDialog", "Detail peminjaman ditampilkan di UI.")
    }

    private fun getUserRole(): String {
        val sharedPref =
            requireActivity().getSharedPreferences("LoginSession", AppCompatActivity.MODE_PRIVATE)
        return sharedPref.getString("userRole", "") ?: ""
    }

    private fun hasUnsavedChanges(): Boolean {
        val currentBorrowing = Borrowing(
            borrowing_id = binding.etBorrowingCode.text.toString(),
            item_id = borrowing.item_id,
            student_id = borrowing.student_id,
            admin_id = borrowing.admin_id,
            date_borrowed = binding.etDateBorrowed.text.toString(),
            start_hour = binding.etStartHours.text.toString(),
            end_hour = binding.etEndHours.text.toString(),
            last_location = binding.etLastLocation.text.toString(),
            return_time = binding.etReturnTime.text.toString(),
            status = binding.etStatus.text.toString(),
            student_name = binding.etStudentName.text.toString(),
            student_phone_number = binding.etStudentPhoneNumber.text.toString(),
            admin_name = binding.etAdminName.text.toString(),
            admin_phone_number = binding.etAdminPhoneNumber.text.toString(),
            item_name = borrowing.item_name
        )
        return currentBorrowing != originalBorrowing
    }

    private fun confirmCancel() {
        if (hasUnsavedChanges()) {
            CustomDialog.confirm(
                context = requireContext(),
                message = getString(R.string.unsaved_changes_cancel),
                onConfirm = {
                    displayBorrowingDetails(originalBorrowing)
                    setEditMode(false)
                }
            )
        } else {
            setEditMode(false)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawableResource(R.drawable.dialog_rounded_background)
        }
        Log.d("BorrowingDetailDialog", "onStart dipanggil.")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("BorrowingDetailDialog", "onDestroyView dipanggil.")
        _binding = null
    }

    companion object {
        fun newInstance(
            borrowing: Borrowing,
            isEditMode: Boolean = false
        ): BorrowingDetailDialogFragment {
            val fragment = BorrowingDetailDialogFragment()
            val args = Bundle()
            args.putParcelable("borrowing", borrowing)
            args.putBoolean("isEditMode", isEditMode)
            fragment.arguments = args
            Log.d(
                "BorrowingDetailDialog",
                "newInstance dipanggil untuk ID peminjaman: ${borrowing.borrowing_id}"
            )
            return fragment
        }
    }
}