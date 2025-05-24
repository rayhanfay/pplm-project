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
import com.pplm.projectinventarisuas.ui.studentsection.scancode.ScanCodeActivity
import com.pplm.projectinventarisuas.utils.components.CustomDialog
import com.pplm.projectinventarisuas.utils.viewmodel.ItemViewModel
import com.pplm.projectinventarisuas.utils.viewmodel.ViewModelFactory

class ItemDetailDialogFragment : DialogFragment() {

    private var _binding: DialogItemDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ItemViewModel
    private lateinit var item: Item
    private var isEditMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set style for the dialog to be fullscreen or as needed
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

        // Retrieve item data from arguments
        arguments?.let {
            item = it.getParcelable("item")!!
            // isEditMode from arguments is only a suggestion, actual edit mode depends on user role
            isEditMode = it.getBoolean("isEditMode", false)
        }

        displayItemDetails()
        // Call setupUserPermission first to determine actual editability and button visibility
        setupUserPermission()
        setupButtons() // Setup button click listeners after their visibility is determined
    }

    private fun setupViewModel() {
        val factory = ViewModelFactory(ItemRepository(), BorrowingRepository(), UserRepository())
        viewModel = ViewModelProvider(this, factory)[ItemViewModel::class.java]
    }

//    private fun setupViewModel() {
//        val itemRepository = ItemRepository()
//        val userRepository = UserRepository()
//        val borrowingRepository = BorrowingRepository()
//        val factory = ViewModelFactory(requireContext(), itemRepository, userRepository, borrowingRepository)
//        viewModel = ViewModelProvider(this, factory).get(ItemViewModel::class.java)
//    }

    private fun displayItemDetails() {
        binding.etItemName.setText(item.item_name)
        binding.etItemType.setText(item.item_type)
        binding.etItemStatus.setText(item.item_status)
        binding.etItemDesciption.setText(item.item_description)
    }

    private fun setupUserPermission() {
        val userRole = getUserRole()
        if (userRole == "admin") {
            // Admin can edit, so enable fields and show/hide buttons based on initial isEditMode
            setEditMode(isEditMode) // Use the isEditMode from arguments for admin
        } else {
            // Student cannot edit, so disable fields and hide edit/save buttons
            setEditMode(false) // Force disable edit mode for students
            binding.btnEdit.visibility = View.GONE
            binding.btnSave.visibility = View.GONE
        }
    }

    private fun setupButtons() {
        binding.btnEdit.setOnClickListener {
            setEditMode(true)
        }

        binding.btnSave.setOnClickListener {
            saveItemDetails()
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

            viewModel.updateItem(updatedItem)
            CustomDialog.alert(
                context = requireContext(),
                message = "Item berhasil diperbarui",
                onDismiss = {
                    dismiss() // Close dialog after item successfully updated
                }
            )
            setEditMode(false)
            item = updatedItem
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
    }

    private fun getUserRole(): String {
        val sharedPref = requireActivity().getSharedPreferences("LoginSession", AppCompatActivity.MODE_PRIVATE)
        return sharedPref.getString("userRole", "") ?: ""
    }

    override fun onStart() {
        super.onStart()
        // Set rounded corners background for dialog
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
