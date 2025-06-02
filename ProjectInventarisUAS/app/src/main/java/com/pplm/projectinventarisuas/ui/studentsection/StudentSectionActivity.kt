package com.pplm.projectinventarisuas.ui.studentsection

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.PopupMenu
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.pplm.projectinventarisuas.R
import com.pplm.projectinventarisuas.data.model.Item
import com.pplm.projectinventarisuas.data.model.ItemSummary
import com.pplm.projectinventarisuas.data.repository.BorrowingRepository
import com.pplm.projectinventarisuas.data.repository.ItemRepository
import com.pplm.projectinventarisuas.data.repository.UserRepository
import com.pplm.projectinventarisuas.databinding.ActivityStudentSectionBinding
import com.pplm.projectinventarisuas.ui.adminsection.item.ItemDetailDialogFragment
import com.pplm.projectinventarisuas.ui.auth.ChangePasswordActivity
import com.pplm.projectinventarisuas.ui.auth.ChangePhoneNumberActivity
import com.pplm.projectinventarisuas.ui.auth.LoginActivity
import com.pplm.projectinventarisuas.ui.studentsection.scancode.ScanCodeActivity
import com.pplm.projectinventarisuas.utils.adapter.ItemAdapter
import com.pplm.projectinventarisuas.utils.adapter.ItemSummaryAdapter
import com.pplm.projectinventarisuas.utils.components.CustomDialog
import com.pplm.projectinventarisuas.utils.getGreetingWithName
import com.pplm.projectinventarisuas.utils.viewmodel.ItemViewModel
import com.pplm.projectinventarisuas.utils.viewmodel.ViewModelFactory

class StudentSectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentSectionBinding
    private lateinit var viewModel: ItemViewModel
    private lateinit var itemAdapter: ItemAdapter
    private lateinit var summaryAdapter: ItemSummaryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentSectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkPhoneNumberSet()
        setupSwipeToRefresh()
        setupGreeting()
        setupRecyclerViews()
        setupViewModel()
        setupSearchBar()
        setupDropdownMenu()
        setupScanCodeButton()
    }

    private fun checkPhoneNumberSet() {
        val sharedPref = getSharedPreferences("LoginSession", MODE_PRIVATE)
        val userRole = sharedPref.getString("userRole", "") ?: ""
        val userId = sharedPref.getString("studentId", "")
        val userName = sharedPref.getString("userName", "") ?: ""
        val isPhoneNumberSet = sharedPref.getBoolean("isPhoneNumberSet", false)

        if (userRole == "student" && !isPhoneNumberSet) {
            CustomDialog.alert(
                context = this,
                title = getString(R.string.need_phone_number),
                message = getString(R.string.phone_number_required)
            ) {
                val intent = Intent(this, ChangePhoneNumberActivity::class.java)
                intent.putExtra("userId", userId)
                intent.putExtra("userRole", userRole)
                intent.putExtra("userName", userName)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun setupGreeting() {
        val sharedPref = getSharedPreferences("LoginSession", MODE_PRIVATE)
        val userName = sharedPref.getString("userName", "") ?: ""
        val greeting = getGreetingWithName(userName)
        binding.tvTitle.text = greeting
    }

    private fun setupRecyclerViews() {
        itemAdapter = ItemAdapter(emptyList(), onItemClick = { selectedItem ->
            showItemDetailDialog(selectedItem, false)
        }, isStudentMode = true)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = itemAdapter

        summaryAdapter = ItemSummaryAdapter(emptyList())
        binding.rvItemSummary.apply {
            layoutManager = GridLayoutManager(this@StudentSectionActivity, 2)
            adapter = summaryAdapter
        }
    }

    private fun setupSearchBar() {
        binding.etSearch.doOnTextChanged { text, _, _, _ ->
            val query = text.toString()
            viewModel.searchItemsAvailable(query)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupViewModel() {
        val viewModelFactory =
            ViewModelFactory(ItemRepository(), BorrowingRepository(), UserRepository())
        viewModel = ViewModelProvider(this, viewModelFactory)[ItemViewModel::class.java]

        viewModel.items.observe(this) { itemList ->
            itemAdapter.updateItems(itemList)
            binding.swipeRefresh.isRefreshing = false
        }

        viewModel.summaryItems.observe(this) { itemList ->
            val summaryMap = itemList
                .groupingBy { it.item_type }
                .eachCount()
                .map { ItemSummary(it.key, it.value) }

            summaryAdapter.updateItems(summaryMap)
        }

        viewModel.loadItemsAvailable()
    }

    private fun setupScanCodeButton() {
        binding.fabScanCode.setOnClickListener {
            checkPhoneNumberAndProceed {
                startActivity(Intent(this, ScanCodeActivity::class.java))
            }
        }
    }

//    private fun setupLogoutButton() {
//        binding.btnLogout.setOnClickListener {
//            CustomDialog.confirm(
//                context = this,
//                message = getString(R.string.logout_message),
//                onConfirm = {
//                    getSharedPreferences("LoginSession", MODE_PRIVATE).edit { clear() }
//                    startActivity(Intent(this, LoginActivity::class.java).apply {
//                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//                    })
//                    finish()
//                }
//            )
//        }
//    }

    private fun setupDropdownMenu() {
        binding.btnDropdownMenu.setOnClickListener { view ->
            showCustomDropdownMenu(view)
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun showCustomDropdownMenu(anchorView: View) {
        val popupView = LayoutInflater.from(this).inflate(R.layout.dialog_custom_dropdown, null)

        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        popupWindow.setBackgroundDrawable(resources.getDrawable(android.R.color.transparent))
        popupWindow.elevation = 8f
        val tvChangePassword = popupView.findViewById<TextView>(R.id.tvChangePassword)
        val tvChangePhoneNumber = popupView.findViewById<TextView>(R.id.tvChangePhoneNumber)
        val btnLogout = popupView.findViewById<Button>(R.id.btnLogout)

        tvChangePassword.setOnClickListener {
            popupWindow.dismiss()
            val intent = Intent(this, ChangePasswordActivity::class.java)
            val sharedPref = getSharedPreferences("LoginSession", MODE_PRIVATE)
            intent.putExtra("userId", sharedPref.getString("studentId", ""))
            intent.putExtra("userRole", sharedPref.getString("userRole", ""))
            intent.putExtra("userName", sharedPref.getString("userName", ""))
            startActivity(intent)
        }

        tvChangePhoneNumber.setOnClickListener {
            popupWindow.dismiss()
            checkPhoneNumberAndProceed {
                val sharedPref = getSharedPreferences("LoginSession", MODE_PRIVATE)
                val userId = sharedPref.getString("studentId", "")
                val userRole = sharedPref.getString("userRole", "")
                val userName = sharedPref.getString("userName", "") ?: ""

                val intent = Intent(this, ChangePhoneNumberActivity::class.java)
                intent.putExtra("userId", userId)
                intent.putExtra("userRole", userRole)
                intent.putExtra("userName", userName)
                startActivity(intent)
            }
        }

        btnLogout.setOnClickListener {
            popupWindow.dismiss()
            CustomDialog.confirm(
                context = this,
                message = getString(R.string.logout_message),
                onConfirm = {
                    getSharedPreferences("LoginSession", MODE_PRIVATE).edit { clear() }
                    startActivity(Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                }
            )
        }

        val xOffset = anchorView.width - popupView.measuredWidth
        val yOffset = anchorView.height

        popupWindow.showAsDropDown(anchorView, xOffset, 0, Gravity.END)
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            binding.etSearch.setText("")
            binding.etSearch.clearFocus()

            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)

            viewModel.loadItemsAvailable()
        }
    }

    override fun onResume() {
        super.onResume()
        val sharedPref = getSharedPreferences("LoginSession", MODE_PRIVATE)
        val isPhoneNumberSet = sharedPref.getBoolean("isPhoneNumberSet", false)
        val userRole = sharedPref.getString("userRole", "") ?: ""

        if (userRole != "student" || isPhoneNumberSet) {
            viewModel.loadItemsAvailable()
        }
    }

    fun showItemDetailDialog(item: Item, isEditMode: Boolean) {
        checkPhoneNumberAndProceed {
            val dialog = ItemDetailDialogFragment.newInstance(item, isEditMode)
            dialog.show(supportFragmentManager, "ItemDetailDialog")
        }
    }

    private fun checkPhoneNumberAndProceed(onProceed: () -> Unit) {
        val sharedPref = getSharedPreferences("LoginSession", MODE_PRIVATE)
        val userRole = sharedPref.getString("userRole", "") ?: ""
        val isPhoneNumberSet = sharedPref.getBoolean("isPhoneNumberSet", false)
        val userId = sharedPref.getString("studentId", "")
        val userName = sharedPref.getString("userName", "") ?: ""

        if (userRole == "student" && !isPhoneNumberSet) {
            CustomDialog.alert(
                context = this,
                title = getString(R.string.need_phone_number),
                message = getString(R.string.phone_number_required)
            ) {
                val intent = Intent(this, ChangePhoneNumberActivity::class.java)
                intent.putExtra("userId", userId)
                intent.putExtra("userRole", userRole)
                intent.putExtra("userName", userName)
                startActivity(intent)
            }
        } else {
            onProceed()
        }
    }
}
