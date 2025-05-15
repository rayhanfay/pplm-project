package com.pplm.projectinventarisuas.ui.adminsection.item

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.pplm.projectinventarisuas.databinding.ActivityItemDetailBinding
import com.pplm.projectinventarisuas.data.model.Item
import com.pplm.projectinventarisuas.data.repository.BorrowingRepository
import com.pplm.projectinventarisuas.data.repository.ItemRepository
import com.pplm.projectinventarisuas.data.repository.UserRepository
import com.pplm.projectinventarisuas.utils.components.CustomDialog
import com.pplm.projectinventarisuas.utils.viewmodel.ItemViewModel
import com.pplm.projectinventarisuas.utils.viewmodel.ViewModelFactory

class ItemDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityItemDetailBinding
    private lateinit var viewModel: ItemViewModel
    private lateinit var item: Item
    private var isEditMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityItemDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()

        item = intent.getParcelableExtra("item")!!
        isEditMode = intent.getBooleanExtra("isEditMode", false)

        setupToolbar()
        setupUserPermission()
        populateFields(item)
        setupButtons()
    }

    private fun setupUserPermission() {
        val userRole = getUserRole()
        if (userRole == "admin") {
            setEditMode(isEditMode)
            binding.btnEdit.visibility = View.VISIBLE
        } else {
            setEditMode(false)
            binding.btnEdit.visibility = View.GONE
            binding.btnSave.visibility = View.GONE
        }
    }
    private fun setupViewModel() {
        val factory = ViewModelFactory(ItemRepository(), BorrowingRepository(), UserRepository())
        viewModel = ViewModelProvider(this, factory)[ItemViewModel::class.java]
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun populateFields(item: Item) {
        binding.etItemId.setText(item.item_id)
        binding.etItemName.setText(item.item_name)
        binding.etItemType.setText(item.item_type)
        binding.etItemStatus.setText(item.item_status)
        binding.etItemDesciption.setText(item.item_description)
    }

    private fun setupButtons() {
        binding.btnEdit.setOnClickListener {
            setEditMode(true)
        }

        binding.btnSave.setOnClickListener {
            val updatedItem = item.copy(
                item_name = binding.etItemName.text.toString(),
                item_type = binding.etItemType.text.toString(),
                item_status = binding.etItemStatus.text.toString(),
                item_description = binding.etItemDesciption.text.toString()
            )

            viewModel.updateItem(updatedItem)
            if (getUserRole() == "admin") {
                viewModel.updateItem(updatedItem)
                CustomDialog.alert(
                    context = this,
                    message = "Item berhasil diperbarui",
                    onDismiss = { finish() }
                )
                setEditMode(false)
                item = updatedItem
            } else {
                CustomDialog.alert(
                    context = this,
                    message = "Anda tidak memiliki izin untuk mengubah data"
                )
            }
        }
    }

    private fun setEditMode(enabled: Boolean) {
        isEditMode = enabled

        binding.etItemName.isEnabled = enabled
        binding.etItemType.isEnabled = enabled
        binding.etItemStatus.isEnabled = enabled
        binding.etItemDesciption.isEnabled = enabled

        binding.btnEdit.visibility = if (enabled) View.GONE else View.VISIBLE
        binding.btnSave.visibility = if (enabled) View.VISIBLE else View.GONE
    }

    private fun getUserRole(): String {
        val sharedPref = getSharedPreferences("LoginSession", MODE_PRIVATE)
        return sharedPref.getString("userRole", "") ?: ""
    }
}
