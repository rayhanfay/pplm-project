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
                    message = "Item tidak ditemukan atau tidak tersedia"
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
                    message = "Data peminjaman disimpan"
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
        // Opsional: Untuk mereset dropdown jika fokus hilang
        binding.etAdminName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                // Untuk admin, mungkin tidak perlu di-reset, karena pilihan seringkali disimpan
                // Jika ingin reset tampilan, bisa panggil adapter.filter.filter("") atau adapter.notifyDataSetInvalidated()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupDatePicker() {
        binding.etDateBorrowed.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                R.style.AppTheme_DatePicker,
                { _, year, month, day ->
                    binding.etDateBorrowed.setText("$day/${month + 1}/$year")
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun generateHourOptions(): List<String> {
        return (7..18).map { it.toString().padStart(2, '0') }
    }

    // Fungsi untuk menghasilkan opsi menit berdasarkan jam yang dipilih dan tipe (start/end)
    private fun generateMinuteOptions(selectedHour: Int, isStart: Boolean): List<String> {
        return if (isStart) { // Logika untuk waktu MULAI
            when (selectedHour) {
                7 -> (30..59).map { it.toString().padStart(2, '0') }
                18 -> listOf("00")
                else -> (0..59).map { it.toString().padStart(2, '0') }
            }
        } else { // Logika untuk waktu AKHIR
            when (selectedHour) {
                7 -> (30..59).map { it.toString().padStart(2, '0') }
                18 -> listOf("00")
                else -> (0..59).map { it.toString().padStart(2, '0') }
            }
        }
    }

    private fun setupTimeDropdowns() {
        val hourOptions = generateHourOptions()

        // Inisialisasi adapters dengan semua menit (atau menit default)
        startHourAdapter =
            ArrayAdapter(this, R.layout.custom_dropdown_item, R.id.text1, hourOptions)
        // Awalnya, menit akan menunjukkan semua pilihan (isStart = true, selectedHour = -1)
        startMinuteAdapter = ArrayAdapter(
            this,
            R.layout.custom_dropdown_item,
            R.id.text1,
            generateMinuteOptions(-1, true)
        )

        endHourAdapter = ArrayAdapter(this, R.layout.custom_dropdown_item, R.id.text1, hourOptions)
        // Awalnya, menit akan menunjukkan semua pilihan (isStart = false, selectedHour = -1)
        endMinuteAdapter = ArrayAdapter(
            this,
            R.layout.custom_dropdown_item,
            R.id.text1,
            generateMinuteOptions(-1, false)
        )

        binding.etStartHour.setAdapter(startHourAdapter)
        binding.etStartMinute.setAdapter(startMinuteAdapter)

        binding.etEndHour.setAdapter(endHourAdapter)
        binding.etEndMinute.setAdapter(endMinuteAdapter)

        // Mengatur dropDownHeight
        val dropdownHeightPx = (resources.displayMetrics.density * 200).toInt() // Contoh 200dp
        binding.etStartHour.dropDownHeight = dropdownHeightPx
        binding.etStartMinute.dropDownHeight = dropdownHeightPx
        binding.etEndHour.dropDownHeight = dropdownHeightPx
        binding.etEndMinute.dropDownHeight = dropdownHeightPx

        binding.etStartHour.threshold = 0
        binding.etStartMinute.threshold = 0
        binding.etEndHour.threshold = 0
        binding.etEndMinute.threshold = 0

        // Listeners for showing dropdowns
        binding.etStartHour.setOnClickListener { binding.etStartHour.showDropDown() }
        binding.etStartMinute.setOnClickListener { binding.etStartMinute.showDropDown() }
        binding.etEndHour.setOnClickListener { binding.etEndHour.showDropDown() }
        binding.etEndMinute.setOnClickListener { binding.etEndMinute.showDropDown() }

        // Logic for filtering minutes based on selected hour for START TIME
        binding.etStartHour.setOnItemClickListener { parent, _, position, _ ->
            val selectedHourString = parent.getItemAtPosition(position).toString()
            val selectedHour = selectedHourString.toIntOrNull() ?: -1

            val filteredMinuteOptions =
                generateMinuteOptions(selectedHour, true) // Pass true for isStart
            startMinuteAdapter.clear()
            startMinuteAdapter.addAll(filteredMinuteOptions)
            startMinuteAdapter.notifyDataSetChanged()

            // Clear selected minute if it's no longer valid
            val currentMinute = binding.etStartMinute.text.toString().toIntOrNull() ?: -1
            if ((selectedHour == 7 && currentMinute < 30) || (selectedHour == 18 && currentMinute != 0)) {
                binding.etStartMinute.setText("")
            }
        }

        // Logic for filtering minutes based on selected hour for END TIME
        binding.etEndHour.setOnItemClickListener { parent, _, position, _ ->
            val selectedHourString = parent.getItemAtPosition(position).toString()
            val selectedHour = selectedHourString.toIntOrNull() ?: -1

            val filteredMinuteOptions =
                generateMinuteOptions(selectedHour, false) // Pass false for isStart
            endMinuteAdapter.clear()
            endMinuteAdapter.addAll(filteredMinuteOptions)
            endMinuteAdapter.notifyDataSetChanged()

            // Clear selected minute if it's no longer valid
            val currentMinute = binding.etEndMinute.text.toString().toIntOrNull() ?: -1
            if ((selectedHour == 7 && currentMinute < 30) || (selectedHour == 18 && currentMinute != 0)) {
                binding.etEndMinute.setText("")
            }
        }

        // Logic to reset minute dropdown when focus is lost (dropdown closes)
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

        // Perubahan 1: Validasi awal semua field harus diisi
        if (admin.isEmpty() || date.isEmpty() || startHour.isEmpty() || startMinute.isEmpty() || endHour.isEmpty() || endMinute.isEmpty()) {
            CustomDialog.alert(
                context = this,
                message = "Semua field harus diisi"
            )
            return false
        }

        val startHourInt = startHour.toIntOrNull()
        val startMinuteInt = startMinute.toIntOrNull()
        val endHourInt = endHour.toIntOrNull()
        val endMinuteInt = endMinute.toIntOrNull()

        if (startHourInt == null || startMinuteInt == null) {
            CustomDialog.alert(context = this, message = "Jam mulai tidak valid")
            return false
        }
        if (endHourInt == null || endMinuteInt == null) {
            CustomDialog.alert(context = this, message = "Jam akhir tidak valid")
            return false
        }

        val startTotalMinutes = startHourInt * 60 + startMinuteInt
        val endTotalMinutes = endHourInt * 60 + endMinuteInt
        val minBorrowMinutesTotal = 7 * 60 + 30 // 07:30 dalam menit
        val maxBorrowMinutesTotal = 18 * 60 // 18:00 dalam menit

        // Validasi rentang jam & menit untuk start
        if (startHourInt == 7 && startMinuteInt < 30) {
            CustomDialog.alert(context = this, message = "Jam mulai paling cepat 07:30")
            return false
        }
        if (startHourInt == 18 && startMinuteInt != 0) {
            CustomDialog.alert(
                context = this,
                message = "Jam mulai paling lambat 18:00 (hanya 18:00)"
            )
            return false
        }
        if (startTotalMinutes < minBorrowMinutesTotal || startTotalMinutes > maxBorrowMinutesTotal) {
            CustomDialog.alert(
                context = this,
                message = "Jam mulai harus antara 07:30 hingga 18:00"
            )
            return false
        }

        // Validasi rentang jam & menit untuk end
        if (endHourInt == 7 && endMinuteInt < 30) {
            CustomDialog.alert(context = this, message = "Jam akhir paling cepat 07:30")
            return false
        }
        if (endHourInt == 18 && endMinuteInt != 0) {
            CustomDialog.alert(
                context = this,
                message = "Jam akhir paling lambat 18:00 (hanya 18:00)"
            )
            return false
        }
        if (endTotalMinutes < minBorrowMinutesTotal || endTotalMinutes > maxBorrowMinutesTotal) {
            CustomDialog.alert(
                context = this,
                message = "Jam akhir harus antara 07:30 hingga 18:00"
            )
            return false
        }

        // Perubahan 2 & 3: Validasi jam akhir harus lebih dari jam mulai, dan minimal 30 menit
        if (endTotalMinutes <= startTotalMinutes) {
            CustomDialog.alert(context = this, message = "Jam akhir harus lebih dari jam mulai")
            return false
        }
        if (endTotalMinutes - startTotalMinutes < 30) {
            CustomDialog.alert(context = this, message = "Durasi peminjaman minimal 30 menit")
            return false
        }

        return true
    }
}