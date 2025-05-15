package com.pplm.projectinventarisuas.ui.studentsection

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.pplm.projectinventarisuas.data.model.Item
import com.pplm.projectinventarisuas.data.repository.BorrowingRepository
import com.pplm.projectinventarisuas.data.repository.ItemRepository
import com.pplm.projectinventarisuas.data.repository.UserRepository
import com.pplm.projectinventarisuas.databinding.ActivityStudentSectionBinding
import com.pplm.projectinventarisuas.ui.adminsection.item.ItemDetailActivity
import com.pplm.projectinventarisuas.ui.auth.LoginActivity
import com.pplm.projectinventarisuas.ui.studentsection.scancode.ScanCodeActivity
import com.pplm.projectinventarisuas.utils.adapter.ItemAdapter
import com.pplm.projectinventarisuas.utils.components.CustomDialog
import com.pplm.projectinventarisuas.utils.getGreetingWithName
import com.pplm.projectinventarisuas.utils.viewmodel.ItemViewModel
import com.pplm.projectinventarisuas.utils.viewmodel.ViewModelFactory

class StudentSectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentSectionBinding
    private lateinit var viewModel: ItemViewModel
    private lateinit var adapter: ItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentSectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGreeting()
        setupRecyclerView()
        setupViewModel()
        setupLogoutButton()
        setupScanCodeButton()
    }

    private fun setupGreeting() {
        val sharedPref = getSharedPreferences("LoginSession", MODE_PRIVATE)
        val userName = sharedPref.getString("userName", "") ?: ""
        val greeting = getGreetingWithName(userName)
        binding.tvTitle.text = greeting
    }

    private fun setupRecyclerView() {
        adapter = ItemAdapter(emptyList()) { selectedItem -> viewItem(selectedItem) }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupViewModel() {
        val borrowingRepository = BorrowingRepository()
        val userRepository = UserRepository()
        val viewModelFactory = ViewModelFactory(ItemRepository(), borrowingRepository, userRepository)
        viewModel = ViewModelProvider(this, viewModelFactory)[ItemViewModel::class.java]

        viewModel.items.observe(this) { itemList ->
            adapter = ItemAdapter(itemList) { selectedItem -> viewItem(selectedItem) }
            binding.recyclerView.adapter = adapter
            adapter.notifyDataSetChanged()
        }

        viewModel.loadItems()
    }

    private fun viewItem(item: Item) {
        val intent = Intent(this, ItemDetailActivity::class.java).apply {
            putExtra("item", item)
        }
        startActivity(intent)
    }

    private fun setupScanCodeButton() {
        binding.fabScanCode.setOnClickListener {
            startActivity(Intent(this, ScanCodeActivity::class.java))
        }
    }

    private fun setupLogoutButton() {
        binding.btnLogout.setOnClickListener {
            CustomDialog.confirm(
                context = this,
                message = "Yakin ingin logout?",
                onConfirm = {
                    getSharedPreferences("LoginSession", MODE_PRIVATE).edit { clear() }
                    startActivity(Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                }
            )
        }
    }
}
