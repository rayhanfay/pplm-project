package com.pplm.projectinventarisuas.ui.adminsection.borrowing

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pplm.projectinventarisuas.data.model.Borrowing
import com.pplm.projectinventarisuas.databinding.ActivityBorrowingDetailBinding

class BorrowingDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBorrowingDetailBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBorrowingDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val item = intent.getParcelableExtra<Borrowing>("borrowing")
        item?.let {
            supportActionBar?.title = it.borrowing_id

            binding.tvBorrowingCode.text = "Borrowing Code: ${it.borrowing_id}"
            binding.tvDateBorrowed.text = "Date Borrowed: ${it.date_borrowed}"
            binding.tvStartHours.text = "Start Hours: ${it.start_hour}"
            binding.tvEndHours.text = "End Hours: ${it.end_hour}"
            binding.tvStudentName.text = "Student Name: ${it.student_name}"
            binding.tvAdminName.text = "Admin Name: ${it.admin_name}"
            binding.tvLastLocation.text = "Last Location: ${it.last_location}"
            binding.tvReturnTime.text = "Return Time: ${it.return_time}"
            binding.tvStatus.text = "Status: ${it.status}"
        }
    }
}