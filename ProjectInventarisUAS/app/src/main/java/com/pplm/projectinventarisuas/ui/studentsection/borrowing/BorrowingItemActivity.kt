package com.pplm.projectinventarisuas.ui.studentsection.borrowing

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.core.content.edit
import com.pplm.projectinventarisuas.R
import com.pplm.projectinventarisuas.data.repository.BorrowingRepository
import com.pplm.projectinventarisuas.data.repository.ItemRepository
import com.pplm.projectinventarisuas.data.repository.UserRepository
import com.pplm.projectinventarisuas.databinding.ActivityBorrowingItemBinding
import com.pplm.projectinventarisuas.ui.studentsection.StudentSectionActivity
import com.pplm.projectinventarisuas.utils.components.CustomDialog
import com.pplm.projectinventarisuas.utils.generateBorrowingId
import com.pplm.projectinventarisuas.utils.viewmodel.BorrowingViewModel
import com.pplm.projectinventarisuas.utils.viewmodel.ViewModelFactory
import java.util.Calendar

class BorrowingItemActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBorrowingItemBinding
    private lateinit var borrowingViewModel: BorrowingViewModel
    private var itemCode: String? = null

    private lateinit var startHourAdapter: ArrayAdapter<String>
    private lateinit var startMinuteAdapter: ArrayAdapter<String>
    private lateinit var endHourAdapter: ArrayAdapter<String>
    private lateinit var endMinuteAdapter: ArrayAdapter<String>

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
        setupTimeDropdowns()

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
            if (name == null || name.isEmpty()) {
                CustomDialog.alert(
                    context = this,
                    message = getString(R.string.cant_find_item)
                ) {
                    val intent = Intent(this, StudentSectionActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                }
            } else {
                binding.etItemName.setText(name)
                supportActionBar?.title = name
            }
        }

        borrowingViewModel.adminMap.observe(this) { adminMap ->
            setupAdminNameDropdown(adminMap.keys.toList())
        }

        borrowingViewModel.saveStatus.observe(this) { (success, message) ->
            if (success) {
                val borrowingId = borrowingViewModel.lastBorrowingId.value ?: return@observe

                CustomDialog.success(
                    context = this,
                    message = getString(R.string.success_save_borrow)
                ) {
                    val prefs = getSharedPreferences("BorrowingSession", MODE_PRIVATE)
                    prefs.edit { putString("activeBorrowingId", borrowingId) }

                    startActivity(Intent(this, BorrowingTimerActivity::class.java).apply {
                        putExtra("BORROWING_ID", borrowingId)
                        val endTime = "${binding.etEndHour.text}:${binding.etEndMinute.text}"
                        putExtra("END_TIME", endTime)
                    })
                }
            } else {
                CustomDialog.alert(
                    context = this,
                    message = getString(R.string.failed_save_borrow, message)
                )
                Log.e("SaveError", "Error saving: $message")
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val navigationIcon = binding.toolbar.navigationIcon
        if (navigationIcon != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                navigationIcon.setTint(resources.getColor(android.R.color.white, theme))
            } else {
                navigationIcon.setTint(resources.getColor(android.R.color.white))
            }
        }
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setupAdminNameDropdown(names: List<String>) {
        val adapter = ArrayAdapter(
            this,
            R.layout.custom_dropdown_item,
            R.id.text1,
            names
        )
        binding.etAdminName.setAdapter(adapter)
        binding.etAdminName.threshold = 0
        binding.etAdminName.setOnClickListener {
            binding.etAdminName.showDropDown()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupDatePicker() {
        binding.etDateBorrowed.setOnClickListener {
            val cal = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                this,
                R.style.AppTheme_DatePicker,
                { _, year, month, day ->
                    val selectedCalendar = Calendar.getInstance()
                    selectedCalendar.set(year, month, day)
                    val dayOfWeek = selectedCalendar.get(Calendar.DAY_OF_WEEK)

                    if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                        CustomDialog.alert(
                            context = this,
                            message = getString(R.string.cant_borrow_weekend)
                        )
                    } else {
                        binding.etDateBorrowed.setText("$day/${month + 1}/$year")
                    }
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }
    }

    private fun generateHourOptions(): List<String> {
        return (7..18).map { it.toString().padStart(2, '0') }
    }

    private fun generateMinuteOptions(selectedHour: Int, isStart: Boolean): List<String> {
        return when (selectedHour) {
            7 -> listOf(
                "30",
                "31",
                "32",
                "33",
                "34",
                "35",
                "36",
                "37",
                "38",
                "39",
                "40",
                "41",
                "42",
                "43",
                "44",
                "45",
                "46",
                "47",
                "48",
                "49",
                "50",
                "51",
                "52",
                "53",
                "54",
                "55",
                "56",
                "57",
                "58",
                "59"
            )

            18 -> listOf("00")
            else -> (0..59).map { it.toString().padStart(2, '0') }
        }
    }

    private fun setupTimeDropdowns() {
        val hourOptions = generateHourOptions()

        startHourAdapter =
            ArrayAdapter(this, R.layout.custom_dropdown_item, R.id.text1, hourOptions)
        startMinuteAdapter = ArrayAdapter(
            this,
            R.layout.custom_dropdown_item,
            R.id.text1,
            (0..59).map { it.toString().padStart(2, '0') }
        )

        endHourAdapter = ArrayAdapter(this, R.layout.custom_dropdown_item, R.id.text1, hourOptions)
        endMinuteAdapter = ArrayAdapter(
            this,
            R.layout.custom_dropdown_item,
            R.id.text1,
            (0..59).map { it.toString().padStart(2, '0') }
        )

        binding.etStartHour.setAdapter(startHourAdapter)
        binding.etStartMinute.setAdapter(startMinuteAdapter)
        binding.etEndHour.setAdapter(endHourAdapter)
        binding.etEndMinute.setAdapter(endMinuteAdapter)

        val dropdownHeightPx = (resources.displayMetrics.density * 200).toInt()
        binding.etStartHour.dropDownHeight = dropdownHeightPx
        binding.etStartMinute.dropDownHeight = dropdownHeightPx
        binding.etEndHour.dropDownHeight = dropdownHeightPx
        binding.etEndMinute.dropDownHeight = dropdownHeightPx

        binding.etStartHour.threshold = 0
        binding.etStartMinute.threshold = 0
        binding.etEndHour.threshold = 0
        binding.etEndMinute.threshold = 0

        binding.etStartHour.setOnClickListener { binding.etStartHour.showDropDown() }
        binding.etStartMinute.setOnClickListener { binding.etStartMinute.showDropDown() }
        binding.etEndHour.setOnClickListener { binding.etEndHour.showDropDown() }
        binding.etEndMinute.setOnClickListener { binding.etEndMinute.showDropDown() }

        binding.etStartHour.setOnItemClickListener { parent, _, position, _ ->
            val selectedHourString = parent.getItemAtPosition(position).toString()
            val selectedHour = selectedHourString.toIntOrNull() ?: -1

            val filteredMinuteOptions = generateMinuteOptions(selectedHour, true)
            startMinuteAdapter.clear()
            startMinuteAdapter.addAll(filteredMinuteOptions)
            startMinuteAdapter.notifyDataSetChanged()

            val currentMinute = binding.etStartMinute.text.toString().toIntOrNull() ?: -1
            if ((selectedHour == 7 && currentMinute < 30) || (selectedHour == 18 && currentMinute != 0)) {
                binding.etStartMinute.setText("")
            }
        }

        binding.etEndHour.setOnItemClickListener { parent, _, position, _ ->
            val selectedHourString = parent.getItemAtPosition(position).toString()
            val selectedHour = selectedHourString.toIntOrNull() ?: -1

            val filteredMinuteOptions = generateMinuteOptions(selectedHour, false)
            endMinuteAdapter.clear()
            endMinuteAdapter.addAll(filteredMinuteOptions)
            endMinuteAdapter.notifyDataSetChanged()

            val currentMinute = binding.etEndMinute.text.toString().toIntOrNull() ?: -1
            if ((selectedHour == 7 && currentMinute < 30) || (selectedHour == 18 && currentMinute != 0)) {
                binding.etEndMinute.setText("")
            }
        }

        binding.etStartMinute.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val selectedHour = binding.etStartHour.text.toString().toIntOrNull() ?: -1
                val currentOptions = generateMinuteOptions(selectedHour, true)
                startMinuteAdapter.clear()
                startMinuteAdapter.addAll(currentOptions)
                startMinuteAdapter.notifyDataSetChanged()
            }
        }

        binding.etEndMinute.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val selectedHour = binding.etEndHour.text.toString().toIntOrNull() ?: -1
                val currentOptions = generateMinuteOptions(selectedHour, false)
                endMinuteAdapter.clear()
                endMinuteAdapter.addAll(currentOptions)
                endMinuteAdapter.notifyDataSetChanged()
            }
        }
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
            val startHour = binding.etStartHour.text.toString().padStart(2, '0')
            val startMinute = binding.etStartMinute.text.toString().padStart(2, '0')
            val endHour = binding.etEndHour.text.toString().padStart(2, '0')
            val endMinute = binding.etEndMinute.text.toString().padStart(2, '0')

            val startHours = "$startHour:$startMinute"
            val endHours = "$endHour:$endMinute"

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
        val startHour = binding.etStartHour.text.toString().trim()
        val startMinute = binding.etStartMinute.text.toString().trim()
        val endHour = binding.etEndHour.text.toString().trim()
        val endMinute = binding.etEndMinute.text.toString().trim()

        if (admin.isEmpty() || date.isEmpty() || startHour.isEmpty() || startMinute.isEmpty() || endHour.isEmpty() || endMinute.isEmpty()) {
            CustomDialog.alert(
                context = this,
                message = getString(R.string.all_fields_required)
            )
            return false
        }

        val startHourInt = startHour.toIntOrNull()
        val startMinuteInt = startMinute.toIntOrNull()
        val endHourInt = endHour.toIntOrNull()
        val endMinuteInt = endMinute.toIntOrNull()

        if (startHourInt == null || startMinuteInt == null) {
            CustomDialog.alert(
                context = this,
                message = getString(R.string.hour_star_fail)
            )
            return false
        }
        if (endHourInt == null || endMinuteInt == null) {
            CustomDialog.alert(
                context = this,
                message = getString(R.string.hour_end_fail)
            )
            return false
        }

        val startTotalMinutes = startHourInt * 60 + startMinuteInt
        val endTotalMinutes = endHourInt * 60 + endMinuteInt
        val minBorrowMinutesTotal = 7 * 60 + 30
        val maxBorrowMinutesTotal = 18 * 60

        if (startHourInt == 7 && startMinuteInt < 30) {
            CustomDialog.alert(
                context = this,
                message = getString(R.string.error_start_time_earliest)
            )
            return false
        }
        if (startHourInt == 18 && startMinuteInt != 0) {
            CustomDialog.alert(
                context = this,
                message = getString(R.string.error_start_time_latest)
            )
            return false
        }
        if (startTotalMinutes < minBorrowMinutesTotal || startTotalMinutes > maxBorrowMinutesTotal) {
            CustomDialog.alert(
                context = this,
                message = getString(R.string.error_start_time_range)
            )
            return false
        }

        if (endHourInt == 7 && endMinuteInt < 30) {
            CustomDialog.alert(
                context = this,
                message = getString(R.string.error_end_time_earliest)
            )
            return false
        }
        if (endHourInt == 18 && endMinuteInt != 0) {
            CustomDialog.alert(
                context = this,
                message = getString(R.string.error_end_time_latest)
            )
            return false
        }
        if (endTotalMinutes < minBorrowMinutesTotal || endTotalMinutes > maxBorrowMinutesTotal) {
            CustomDialog.alert(
                context = this,
                message = getString(R.string.error_end_time_range)
            )
            return false
        }

        if (endTotalMinutes <= startTotalMinutes) {
            CustomDialog.alert(
                context = this,
                message = getString(R.string.error_end_before_start)
            )
            return false
        }
        if (endTotalMinutes - startTotalMinutes < 30) {
            CustomDialog.alert(
                context = this,
                message = getString(R.string.error_min_duration)
            )
            return false
        }

        return true
    }
}