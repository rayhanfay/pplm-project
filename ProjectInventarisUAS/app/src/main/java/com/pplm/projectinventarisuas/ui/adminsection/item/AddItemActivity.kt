package com.pplm.projectinventarisuas.ui.adminsection.item

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.pplm.projectinventarisuas.data.model.Item
import com.pplm.projectinventarisuas.data.repository.BorrowingRepository
import com.pplm.projectinventarisuas.data.repository.ItemRepository
import com.pplm.projectinventarisuas.data.repository.UserRepository
import com.pplm.projectinventarisuas.databinding.ActivityAddItemBinding
import com.pplm.projectinventarisuas.utils.components.CustomDialog
import com.pplm.projectinventarisuas.utils.viewmodel.AddItemViewModel
import com.pplm.projectinventarisuas.utils.viewmodel.ViewModelFactory

class AddItemActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddItemBinding
    private val viewModel: AddItemViewModel by viewModels {
        ViewModelFactory(ItemRepository(), BorrowingRepository(), UserRepository())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddItemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnSave.setOnClickListener {
            saveItem()
        }
    }

    private fun saveItem() {
        if (validateItemInput()) {
            val id = binding.etItemId.text.toString().trim()
            val name = binding.etItemName.text.toString().trim()
            val type = binding.etItemtype.text.toString().trim()
            val status = binding.etItemStatus.text.toString().trim()

            viewModel.itemExists(id) { exists ->
                if (exists) {
                    CustomDialog.alert(
                        context = this,
                        message = "Item dengan ID '$id' sudah ada",
                    )
                } else {
                    val item = Item(
                        item_id = id,
                        item_name = name,
                        item_type = type,
                        item_status = status
                    )

                    viewModel.addItem(item) { success ->
                        if (success) {
                            CustomDialog.alert(
                                context = this,
                                message = "Item berhasil disimpan",
                                onDismiss = { finish() }
                            )
                        } else {
                            CustomDialog.alert(
                                context = this,
                                message = "Gagal menyimpan item"
                            )
                        }
                    }
                }
            }
        }
    }

    private fun validateItemInput(): Boolean {
        val id = binding.etItemId.text.toString().trim()
        val name = binding.etItemName.text.toString().trim()
        val type = binding.etItemtype.text.toString().trim()
        val status = binding.etItemStatus.text.toString().trim()

        if (id.isEmpty() && name.isEmpty() && type.isEmpty() && status.isEmpty()) {
            CustomDialog.alert(
                context = this,
                message = "Semua field harus diisi"
            )
            return false
        }

        if (id.isEmpty()) {
            CustomDialog.alert(context = this, message = "ID item harus diisi")
            return false
        }

        if (name.isEmpty()) {
            CustomDialog.alert(context = this, message = "Nama item harus diisi")
            return false
        }

        if (type.isEmpty()) {
            CustomDialog.alert(context = this, message = "Tipe item harus diisi")
            return false
        }

        if (status.isEmpty()) {
            CustomDialog.alert(context = this, message = "Status item harus diisi")
            return false
        }

        return true
    }
}