package com.pplm.projectinventarisuas.ui.welcome

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.pplm.projectinventarisuas.databinding.ActivityWelcomeBinding
import com.pplm.projectinventarisuas.ui.auth.LoginActivity
import com.pplm.projectinventarisuas.utils.adapter.WelcomePagerAdapter

class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fragments = listOf(
            WelcomeFragment.newInstance("First Welcome Message"),
            WelcomeFragment.newInstance("Second Welcome Message"),
            WelcomeFragment.newInstance("Third Welcome Message", isLast = true)
        )

        val adapter = WelcomePagerAdapter(this, fragments)
        binding.viewPager.adapter = adapter
        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
    }

    fun goToNextFragment() {
        val currentItem = binding.viewPager.currentItem
        if (currentItem < 2) {
            binding.viewPager.currentItem = currentItem + 1
        } else {
            goToLogin()
        }
    }

    fun goToLogin() {
        val sharedPref = getSharedPreferences("OnboardingPrefs", AppCompatActivity.MODE_PRIVATE)
        sharedPref.edit().putBoolean("isOnboardingCompleted", true).apply()

        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}