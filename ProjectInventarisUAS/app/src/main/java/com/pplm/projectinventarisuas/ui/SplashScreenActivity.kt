package com.pplm.projectinventarisuas.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.pplm.projectinventarisuas.ui.auth.LoginActivity
import com.pplm.projectinventarisuas.databinding.ActivitySplashScreenBinding
import com.pplm.projectinventarisuas.ui.adminsection.AdminSectionActivity
import com.pplm.projectinventarisuas.ui.studentsection.StudentSectionActivity
import com.pplm.projectinventarisuas.ui.studentsection.borrowing.BorrowingTimerActivity
import com.pplm.projectinventarisuas.ui.welcome.WelcomeActivity

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
            finish()
        }, 2000)
    }

    private fun navigateToNextScreen() {
        val onboardingPref = getSharedPreferences("OnboardingPrefs", MODE_PRIVATE)
        val isOnboardingCompleted = onboardingPref.getBoolean("isOnboardingCompleted", false)

        val loginPref = getSharedPreferences("LoginSession", MODE_PRIVATE)
        val isLoggedIn = loginPref.getBoolean("isLoggedIn", false)
        val userRole = loginPref.getString("userRole", "")
        val borrowingPrefs = getSharedPreferences("BorrowingSession", MODE_PRIVATE)
        val activeBorrowingId = borrowingPrefs.getString("activeBorrowingId", null)

        when {
            !isOnboardingCompleted -> {
                startActivity(Intent(this, WelcomeActivity::class.java))
            }
            isLoggedIn -> {
                when {
                    activeBorrowingId != null -> {
                        val intent = Intent(this, BorrowingTimerActivity::class.java).apply {
                            putExtra("BORROWING_ID", activeBorrowingId)
                        }
                        startActivity(intent)
                    }
                    userRole == "admin" -> {
                        startActivity(Intent(this, AdminSectionActivity::class.java))
                    }
                    userRole == "student" -> {
                        startActivity(Intent(this, StudentSectionActivity::class.java))
                    }
                }
            }
            else -> {
                startActivity(Intent(this, LoginActivity::class.java))
            }
        }
    }
}