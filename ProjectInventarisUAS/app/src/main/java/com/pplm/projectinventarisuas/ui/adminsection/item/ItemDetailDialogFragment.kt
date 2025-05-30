package com.pplm.projectinventarisuas.ui.adminsection.item

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.pplm.projectinventarisuas.R
import com.pplm.projectinventarisuas.databinding.DialogItemDetailBinding
import com.pplm.projectinventarisuas.data.model.Item
import com.pplm.projectinventarisuas.data.repository.BorrowingRepository
import com.pplm.projectinventarisuas.data.repository.ItemRepository
import com.pplm.projectinventarisuas.data.repository.UserRepository
import com.pplm.projectinventarisuas.utils.components.CustomDialog
import com.pplm.projectinventarisuas.utils.viewmodel.ItemViewModel
import com.pplm.projectinventarisuas.utils.viewmodel.ViewModelFactory

class ItemDetailDialogFragment : DialogFragment() {

    private var _binding: DialogItemDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ItemViewModel
    private lateinit var item: Item
    private lateinit var originalItem: Item
    private var isEditMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog_NoActionBar_MinWidth)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogItemDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()

        arguments?.let {
            item = it.getParcelable("item")!!
            originalItem = item.copy()
            isEditMode = it.getBoolean("isEditMode", false)
        }

        displayItemDetails(item)
        setupUserPermission()
        setupButtons()
    }

    private fun setupViewModel() {
        val factory = ViewModelFactory(ItemRepository(), BorrowingRepository(), UserRepository())
        viewModel = ViewModelProvider(this, factory)[ItemViewModel::class.java]
    }

    private fun displayItemDetails(itemToDisplay: Item) {
        binding.etItemName.setText(itemToDisplay.item_name)
        binding.etItemType.setText(itemToDisplay.item_type)
        binding.etItemStatus.setText(itemToDisplay.item_status)
        binding.etItemDesciption.setText(itemToDisplay.item_description)
    }

    private fun setupUserPermission() {
        val userRole = getUserRole()
        if (userRole == "admin") {
            setEditMode(isEditMode)
        } else {
            setEditMode(false)
            binding.btnEdit.visibility = View.GONE
            binding.btnSave.visibility = View.GONE
            binding.btnCancel.visibility = View.GONE
        }
    }

    private fun setupButtons() {
        binding.btnEdit.setOnClickListener {
            setEditMode(true)
        }

        binding.btnSave.setOnClickListener {
            saveItemDetails()
        }

        binding.btnCancel.setOnClickListener {
            confirmCancel()
        }
    }

    private fun saveItemDetails() {
        val userRole = getUserRole()
        if (userRole == "admin") {
            val updatedItem = Item(
                item_id = item.item_id,
                item_name = binding.etItemName.text.toString(),
                item_type = binding.etItemType.text.toString(),
                item_status = binding.etItemStatus.text.toString(),
                item_description = binding.etItemDesciption.text.toString()
            )

            CustomDialog.confirm(
                context = requireContext(),
                message = "Apakah Anda yakin ingin menyimpan perubahan?",
                onConfirm = {
                    viewModel.updateItem(updatedItem)
                    CustomDialog.success(
                        context = requireContext(),
                        message = "Item berhasil diperbarui",
                        onDismiss = {
                            item = updatedItem
                            originalItem = item.copy()
                            setEditMode(false)
                        }
                    )
                }
            )
        } else {
            CustomDialog.alert(
                context = requireContext(),
                message = "Anda tidak memiliki izin untuk mengubah data"
            )
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
        binding.btnCancel.visibility = if (enabled) View.VISIBLE else View.GONE
    }

    private fun getUserRole(): String {
        val sharedPref =
            requireActivity().getSharedPreferences("LoginSession", AppCompatActivity.MODE_PRIVATE)
        return sharedPref.getString("userRole", "") ?: ""
    }

    private fun hasUnsavedChanges(): Boolean {
        val currentItem = Item(
            item_id = item.item_id,
            item_name = binding.etItemName.text.toString(),
            item_type = binding.etItemType.text.toString(),
            item_status = binding.etItemStatus.text.toString(),
            item_description = binding.etItemDesciption.text.toString()
        )
        return currentItem != originalItem
    }

    private fun confirmCancel() {
        if (hasUnsavedChanges()) {
            CustomDialog.confirm(
                context = requireContext(),
                message = "Anda memiliki perubahan yang belum disimpan. Yakin ingin membatalkan?",
                onConfirm = {
                    displayItemDetails(originalItem)
                    setEditMode(false)
                }
            )
        } else {
            setEditMode(false)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawableResource(R.drawable.dialog_rounded_background)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(item: Item, isEditMode: Boolean = false): ItemDetailDialogFragment {
            val fragment = ItemDetailDialogFragment()
            val args = Bundle()
            args.putParcelable("item", item)
            args.putBoolean("isEditMode", isEditMode)
            fragment.arguments = args
            return fragment
        }
    }
}