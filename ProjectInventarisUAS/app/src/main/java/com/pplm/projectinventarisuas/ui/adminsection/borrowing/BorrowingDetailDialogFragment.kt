package com.pplm.projectinventarisuas.ui.adminsection.borrowing

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.pplm.projectinventarisuas.R
import com.pplm.projectinventarisuas.data.model.Borrowing
import com.pplm.projectinventarisuas.databinding.DialogBorrowingDetailBinding

class BorrowingDetailDialogFragment(private val borrowing: Borrowing) : DialogFragment() {

    private var _binding: DialogBorrowingDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        _binding = DialogBorrowingDetailBinding.inflate(layoutInflater)

        with(binding) {
            tvBorrowingCode.text = " ${borrowing.borrowing_id}"
            tvDateBorrowed.text = " ${borrowing.date_borrowed}"
            tvStartHours.text = "Star: ${borrowing.start_hour}"
            tvEndHours.text = "End:${borrowing.end_hour}"
            tvStudentName.text = "${borrowing.student_name}"
            tvAdminName.text = "${borrowing.admin_name}"
            tvLastLocation.text = "${borrowing.last_location}"
            tvReturnTime.text = "${borrowing.return_time}"
            tvStatus.text = "${borrowing.status}"

            btnClose.setOnClickListener { dismiss() }
        }

        builder.setView(binding.root)
        return builder.create()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawableResource(R.drawable.dialog_rounded_background)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
