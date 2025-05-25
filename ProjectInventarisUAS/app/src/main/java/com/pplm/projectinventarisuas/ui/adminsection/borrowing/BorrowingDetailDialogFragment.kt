package com.pplm.projectinventarisuas.ui.adminsection.borrowing

import android.app.AlertDialog
import android.app.Dialog
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

class BorrowingDetailDialogFragment : DialogFragment() {

    private var _binding: DialogBorrowingDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: BorrowingViewModel
    private lateinit var borrowing: Borrowing
    private var isEditMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog_NoActionBar_MinWidth)
        Log.d("BorrowingDetailDialog", "onCreate called.")
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
        Log.d("BorrowingDetailDialog", "onViewCreated called.")

        setupViewModel()

        arguments?.let {
            borrowing = it.getParcelable("borrowing")!!
            isEditMode = it.getBoolean("isEditMode", false)
            Log.d(
                "BorrowingDetailDialog",
                "Arguments received. Borrowing ID: ${borrowing.borrowing_id}, isEditMode: $isEditMode"
            )
        } ?: run {
            Log.e("BorrowingDetailDialog", "Arguments are null! Borrowing object missing.")
            dismiss()
            return
        }

        displayBorrowingDetails()
        setupUserPermission()
        setupButtons()
    }

    private fun setupViewModel() {
        Log.d("BorrowingDetailDialog", "setupViewModel called.")
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
        }
    }

    private fun setupButtons() {
        binding.btnEdit.setOnClickListener {
            setEditMode(true)
        }

        binding.btnSave.setOnClickListener {
            saveBorrowingDetails()
        }
    }

    private fun saveBorrowingDetails() {
        Log.d("BorrowingDetailDialog", "saveBorrowingDetails called.")
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
                admin_name = binding.etAdminName.text.toString(),
                item_name = borrowing.item_name
            )
            Log.d("BorrowingDetailDialog", "Updating borrowing: $updatedBorrowing")

            viewModel.updateBorrowing(updatedBorrowing)
            CustomDialog.alert(
                context = requireContext(),
                message = "Borrowing berhasil diperbarui",
                onDismiss = {
                    dismiss()
                }
            )
            setEditMode(false)
            borrowing = updatedBorrowing
        } else {
            CustomDialog.alert(
                context = requireContext(),
                message = "Anda tidak memiliki izin untuk mengubah data"
            )
        }
    }

    private fun setEditMode(enabled: Boolean) {
        Log.d("BorrowingDetailDialog", "setEditMode called with enabled: $enabled")
        isEditMode = enabled

        with(binding) {
            etBorrowingCode.isEnabled = false
            etItemName.isEnabled = false
            etDateBorrowed.isEnabled = enabled
            etStartHours.isEnabled = enabled
            etEndHours.isEnabled = enabled
            etStudentName.isEnabled = enabled
            etAdminName.isEnabled = enabled
            etLastLocation.isEnabled = enabled
            etReturnTime.isEnabled = enabled
            etStatus.isEnabled = enabled

            btnClose.setOnClickListener { dismiss() }
        }

        binding.btnEdit.visibility = if (enabled) View.GONE else View.VISIBLE
        binding.btnSave.visibility = if (enabled) View.VISIBLE else View.GONE
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogBorrowingDetailBinding.inflate(layoutInflater)
        val builder = AlertDialog.Builder(requireContext())
        builder.setView(binding.root)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_rounded_background)
        return dialog
    }

    private fun displayBorrowingDetails() {
        Log.d(
            "BorrowingDetailDialog",
            "displayBorrowingDetails called. Borrowing object: $borrowing"
        )
        with(binding) {
            etBorrowingCode.setText(borrowing.borrowing_id)
            etItemName.setText(borrowing.item_name)
            etDateBorrowed.setText(borrowing.date_borrowed)
            etStartHours.setText(borrowing.start_hour)
            etEndHours.setText(borrowing.end_hour)
            etStudentName.setText(borrowing.student_name)
            etAdminName.setText(borrowing.admin_name)
            etLastLocation.setText(borrowing.last_location)
            etReturnTime.setText(borrowing.return_time)
            etStatus.setText(borrowing.status)

            btnClose.setOnClickListener { dismiss() }
        }
        Log.d("BorrowingDetailDialog", "Borrowing details displayed on UI.")
    }

    private fun getUserRole(): String {
        val sharedPref =
            requireActivity().getSharedPreferences("LoginSession", AppCompatActivity.MODE_PRIVATE)
        return sharedPref.getString("userRole", "") ?: ""
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
        Log.d("BorrowingDetailDialog", "onStart called.")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("BorrowingDetailDialog", "onDestroyView called.")
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
                "newInstance called for borrowing ID: ${borrowing.borrowing_id}"
            )
            return fragment
        }
    }
}