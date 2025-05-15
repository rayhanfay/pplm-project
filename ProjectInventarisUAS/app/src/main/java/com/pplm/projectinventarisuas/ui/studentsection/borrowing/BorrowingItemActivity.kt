package com.pplm.projectinventarisuas.ui.studentsection.borrowing

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.core.content.edit
import com.pplm.projectinventarisuas.data.repository.BorrowingRepository
import com.pplm.projectinventarisuas.data.repository.ItemRepository
import com.pplm.projectinventarisuas.data.repository.UserRepository
import com.pplm.projectinventarisuas.databinding.ActivityBorrowingItemBinding
import com.pplm.projectinventarisuas.utils.components.CustomDialog
import com.pplm.projectinventarisuas.utils.generateBorrowingId
import com.pplm.projectinventarisuas.utils.viewmodel.BorrowingViewModel
import com.pplm.projectinventarisuas.utils.viewmodel.ViewModelFactory
import java.util.Calendar

class BorrowingItemActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBorrowingItemBinding
    private lateinit var borrowingViewModel: BorrowingViewModel
    private var itemCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBorrowingItemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupObservers()

        setupToolbar()
        itemCode = intent.getStringExtra("ITEM_CODE")
        itemCode?.let {
            borrowingViewModel.fetchItemName(it)
        }

        borrowingViewModel.loadAdminMap()
        setupDatePicker()
        setupTimePickers()

        binding.btnBowrowItem.setOnClickListener {
            if (validateInput()) {
                saveBorrowingData()
            }
        }
    }

    private fun setupViewModel() {
        val borrowingRepository = BorrowingRepository()
        val itemRepository = ItemRepository()
        val userRepository = UserRepository()

        val factory = ViewModelFactory(itemRepository, borrowingRepository, userRepository)
        borrowingViewModel = ViewModelProvider(this, factory)[BorrowingViewModel::class.java]
    }

    private fun setupObservers() {
        borrowingViewModel.itemName.observe(this) { name ->
            binding.etItemName.setText(name)
        }

        borrowingViewModel.adminMap.observe(this) { adminMap ->
            setupAdminNameDropdown(adminMap.keys.toList())
        }

        borrowingViewModel.saveStatus.observe(this) { (success, message) ->
            if (success) {
                val borrowingId = borrowingViewModel.lastBorrowingId.value ?: return@observe

                CustomDialog.alert(
                    context = this,
                    message = "Data peminjaman disimpan"
                ) {
                    val prefs = getSharedPreferences("BorrowingSession", MODE_PRIVATE)
                    prefs.edit { putString("activeBorrowingId", borrowingId) }

                    startActivity(Intent(this, BorrowingTimerActivity::class.java).apply {
                        putExtra("BORROWING_ID", borrowingId)
                        putExtra("END_HOUR", binding.etEndHours.text.toString())
                    })
                }
            } else {
                CustomDialog.alert(
                    context = this,
                    message = "Gagal menyimpan: $message"
                )
                Log.e("SaveError", "Error saving: $message")
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setupAdminNameDropdown(names: List<String>) {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            names
        )
        binding.etAdminName.setAdapter(adapter)
        binding.etAdminName.setOnClickListener {
            binding.etAdminName.showDropDown()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupDatePicker() {
        binding.etDateBorrowed.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    binding.etDateBorrowed.setText("$day/${month + 1}/$year")
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    @SuppressLint("DefaultLocale")
    private fun setupTimePickers() {
        val timeListener = { field: EditText ->
            val cal = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                field.setText(String.format("%02d:%02d", hour, minute))
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        binding.etStartHours.setOnClickListener { timeListener(binding.etStartHours) }
        binding.etEndHours.setOnClickListener { timeListener(binding.etEndHours) }
    }

    private fun saveBorrowingData() {
        val itemId = itemCode ?: "UNKNOWN"

        if (validateInput()) {
            val sharedPref = getSharedPreferences("LoginSession", MODE_PRIVATE)
            val studentId = sharedPref.getString("studentId", "") ?: ""
            val borrowingId = generateBorrowingId()
            val adminName = binding.etAdminName.text.toString()
            val adminId = borrowingViewModel.getAdminIdByName(adminName) ?: "UNKNOWN"
            val dateBorrowed = binding.etDateBorrowed.text.toString()
            val startHours = binding.etStartHours.text.toString()
            val endHours = binding.etEndHours.text.toString()

            val borrowingData = mapOf(
                "borrowing_id" to borrowingId,
                "student_id" to studentId,
                "admin_id" to adminId,
                "item_id" to itemId,
                "date_borrowed" to dateBorrowed,
                "start_hour" to startHours,
                "end_hour" to endHours,
                "status" to "On Borrow",
                "return_time" to "-",
                "last_location" to "-"
            )

            borrowingViewModel.saveBorrowing(borrowingData, itemId)
        }
    }

    private fun validateInput(): Boolean {
        val admin = binding.etAdminName.text.toString().trim()
        val date = binding.etDateBorrowed.text.toString().trim()
        val start = binding.etStartHours.text.toString().trim()
        val end = binding.etEndHours.text.toString().trim()

        if (admin.isEmpty() && date.isEmpty() && start.isEmpty() && end.isEmpty()) {
            CustomDialog.alert(
                context = this,
                message = "Semua field harus diisi"
            )
            return false
        }

        if (admin.isEmpty()) {
            CustomDialog.alert(context = this, message = "Nama admin harus diisi")
            return false
        }

        if (date.isEmpty()) {
            CustomDialog.alert(context = this, message = "Tanggal peminjaman harus diisi")
            return false
        }

        if (start.isEmpty()) {
            CustomDialog.alert(context = this, message = "Jam mulai harus diisi")
            return false
        }

        if (end.isEmpty()) {
            CustomDialog.alert(context = this, message = "Jam akhir harus diisi")
            return false
        }

        return true
    }
}