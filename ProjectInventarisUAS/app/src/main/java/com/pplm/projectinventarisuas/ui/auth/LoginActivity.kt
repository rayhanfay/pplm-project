package com.pplm.projectinventarisuas.ui.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log // Import kelas Log
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.pplm.projectinventarisuas.R
import com.pplm.projectinventarisuas.data.repository.BorrowingRepository
import com.pplm.projectinventarisuas.data.repository.ItemRepository
import com.pplm.projectinventarisuas.data.repository.UserRepository
import com.pplm.projectinventarisuas.databinding.ActivityLoginBinding
import com.pplm.projectinventarisuas.ui.adminsection.AdminSectionActivity
import com.pplm.projectinventarisuas.ui.studentsection.StudentSectionActivity
import com.pplm.projectinventarisuas.utils.components.CustomDialog
import com.pplm.projectinventarisuas.utils.viewmodel.UserViewModel
import com.pplm.projectinventarisuas.utils.viewmodel.ViewModelFactory

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: UserViewModel
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupButtonLogin()
        setupShowPassword()
        setupObserver()
    }

    private fun setupButtonLogin() {
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // Log input username dan password sebelum validasi
            Log.d("LoginActivity", "Input Username: $username")
            Log.d("LoginActivity", "Input Password (raw): $password") // Jangan log password di produksi! Ini hanya untuk debugging.

            var isValid = true

            if (username.isEmpty()) {
                binding.etUsername.error = "Username tidak boleh kosong"
                isValid = false
                Log.d("LoginActivity", "Validasi Gagal: Username kosong")
            }

            if (password.isEmpty()) {
                binding.etPassword.error = "Password tidak boleh kosong"
                isValid = false
                Log.d("LoginActivity", "Validasi Gagal: Password kosong")
            }

            if (!isValid) {
                Log.d("LoginActivity", "Login dibatalkan karena validasi gagal.")
                return@setOnClickListener
            }

            CustomDialog.showLoading(this, "Sedang login...")
            // Log data yang akan dikirim ke ViewModel
            Log.d("LoginActivity", "Mengirim data login ke ViewModel: Username=$username")
            viewModel.login(username, password)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupShowPassword() {
        binding.etPassword.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (binding.etPassword.right - binding.etPassword.compoundDrawables[2].bounds.width())) {
                    isPasswordVisible = !isPasswordVisible
                    if (isPasswordVisible) {
                        binding.etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                        binding.etPassword.setCompoundDrawablesWithIntrinsicBounds(
                            0, 0, R.drawable.ic_eye, 0
                        )
                        Log.d("LoginActivity", "Password terlihat.")
                    } else {
                        binding.etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                        binding.etPassword.setCompoundDrawablesWithIntrinsicBounds(
                            0, 0, R.drawable.ic_eye_closed, 0
                        )
                        Log.d("LoginActivity", "Password tersembunyi.")
                    }
                    binding.etPassword.post {
                        binding.etPassword.setSelection(binding.etPassword.text.length)
                    }
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun setupObserver() {
        viewModel.user.observe(this) { user ->
            CustomDialog.dismissLoading()
            // Log hasil observasi dari ViewModel
            Log.d("LoginActivity", "Menerima hasil login dari ViewModel. User: $user")

            if (user != null) {
                Log.d("LoginActivity", "Login berhasil untuk peran: ${user.role}, ID: ${user.id}, Nama: ${user.name}, Password Changed: ${user.isPasswordChanged}")

                val sharedPref = getSharedPreferences("LoginSession", MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putBoolean("isLoggedIn", true)
                    putString("userRole", user.role)
                    putString("userName", user.name)
                    Log.e("Auth", "User Name: ${user.name}") // Log ini sudah ada
                    user.id?.let { putString("studentId", it) }
                    apply()
                    Log.d("LoginActivity", "Session disimpan: isLoggedIn=true, userRole=${user.role}, userName=${user.name}, studentId=${user.id}")
                }

                if (!user.isPasswordChanged) {
                    Log.d("LoginActivity", "Mengarahkan ke ChangePasswordActivity karena password belum diubah.")
                    CustomDialog.alert(
                        context = this,
                        title = getString(R.string.need_change_password),
                        message = getString(R.string.first_login)
                    ) {
                        val intent = Intent(this, ChangePasswordActivity::class.java)
                        intent.putExtra("userId", user.id)
                        intent.putExtra("userRole", user.role)
                        Log.d("LoginActivity", "Mengirim userId=${user.id} dan userRole=${user.role} ke ChangePasswordActivity.")
                        startActivity(intent)
                        finish()
                    }
                } else {
                    val intent = when (user.role) {
                        "admin" -> Intent(this, AdminSectionActivity::class.java)
                        "student" -> Intent(this, StudentSectionActivity::class.java)
                        else -> null
                    }
                    intent?.let {
                        Log.d("LoginActivity", "Login berhasil, mengarahkan ke ${user.role} section.")
                        CustomDialog.success(
                            context = this,
                            title = getString(R.string.login_success),
                            message = getString(R.string.find_your_stuff),
                        ) {
                            startActivity(it)
                            finish()
                        }
                    }
                }
            } else {
                Log.d("LoginActivity", "Login gagal: Username atau password salah.")
                CustomDialog.alert(
                    context = this,
                    title = getString(R.string.login_failed),
                    message = getString(R.string.failed_login)
                )
            }
        }
    }

    private fun setupViewModel() {
        val factory = ViewModelFactory(
            ItemRepository(),
            BorrowingRepository(),
            UserRepository()
        )
        viewModel = ViewModelProvider(this, factory)[UserViewModel::class.java]
        Log.d("LoginActivity", "ViewModel berhasil diinisialisasi.")
    }
}
