package com.pplm.projectinventarisuas.ui.adminsection

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupMenu
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.pplm.projectinventarisuas.R
import com.pplm.projectinventarisuas.databinding.ActivityAdminSectionBinding
import com.pplm.projectinventarisuas.ui.adminsection.borrowing.BorrowingFragment
import com.pplm.projectinventarisuas.ui.adminsection.borrowing.ScanReturnActivity
import com.pplm.projectinventarisuas.ui.adminsection.item.AddItemDialogFragment
import com.pplm.projectinventarisuas.ui.adminsection.item.ItemFragment
import com.pplm.projectinventarisuas.ui.auth.ChangePasswordActivity
import com.pplm.projectinventarisuas.ui.auth.ChangePhoneNumberActivity
import com.pplm.projectinventarisuas.ui.auth.LoginActivity
import com.pplm.projectinventarisuas.utils.components.CustomDialog
import com.pplm.projectinventarisuas.utils.getGreetingWithName

class AdminSectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminSectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminSectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkPhoneNumberSet()
        setupGreeting()
        setupDefaultFragment()
        setupBottomNavigation()
        setupFabButtons()
        setupDropdownMenu()
    }

    private fun checkPhoneNumberSet() {
        val sharedPref = getSharedPreferences("LoginSession", MODE_PRIVATE)
        val userRole = sharedPref.getString("userRole", "")
        val userId = sharedPref.getString("studentId", "")
        val userName = sharedPref.getString("userName", "") ?: ""
        val isPhoneNumberSet = sharedPref.getBoolean("isPhoneNumberSet", false)

        if (userRole == "admin" && !isPhoneNumberSet) {
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

    private fun setupDefaultFragment() {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, BorrowingFragment())
            .commit()
        binding.fabAddItem.hide()
        binding.fabScanReturn.show()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.borrowing_menu -> {
                    supportFragmentManager.beginTransaction()
                        .replace(binding.fragmentContainer.id, BorrowingFragment())
                        .commit()
                    binding.fabAddItem.hide()
                    binding.fabScanReturn.show()
                    true
                }
                R.id.item_menu -> {
                    supportFragmentManager.beginTransaction()
                        .replace(binding.fragmentContainer.id, ItemFragment())
                        .commit()
                    binding.fabAddItem.show()
                    binding.fabScanReturn.hide()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupFabButtons() {
        binding.fabAddItem.setOnClickListener {
            checkPhoneNumberAndProceed {
                val dialog = AddItemDialogFragment()
                dialog.show(supportFragmentManager, "AddItemDialog")
            }
        }

        binding.fabScanReturn.setOnClickListener {
            checkPhoneNumberAndProceed {
                val intent = Intent(this, ScanReturnActivity::class.java)
                startActivity(intent)
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

    private fun checkPhoneNumberAndProceed(onProceed: () -> Unit) {
        val sharedPref = getSharedPreferences("LoginSession", MODE_PRIVATE)
        val userRole = sharedPref.getString("userRole", "")
        val isPhoneNumberSet = sharedPref.getBoolean("isPhoneNumberSet", false)
        val userId = sharedPref.getString("studentId", "")
        val userName = sharedPref.getString("userName", "") ?: ""

        if (userRole == "admin" && !isPhoneNumberSet) {
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
