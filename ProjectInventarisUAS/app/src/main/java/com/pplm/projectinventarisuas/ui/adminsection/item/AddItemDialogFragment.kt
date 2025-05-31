package com.pplm.projectinventarisuas.ui.adminsection.item

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.pplm.projectinventarisuas.R
import com.pplm.projectinventarisuas.data.model.Item
import com.pplm.projectinventarisuas.data.repository.BorrowingRepository
import com.pplm.projectinventarisuas.data.repository.ItemRepository
import com.pplm.projectinventarisuas.data.repository.UserRepository
import com.pplm.projectinventarisuas.databinding.DialogAddItemBinding
import com.pplm.projectinventarisuas.utils.components.CustomDialog
import com.pplm.projectinventarisuas.utils.viewmodel.AddItemViewModel
import com.pplm.projectinventarisuas.utils.viewmodel.ViewModelFactory

class AddItemDialogFragment : DialogFragment() {

    private var _binding: DialogAddItemBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddItemViewModel by viewModels {
        ViewModelFactory(ItemRepository(), BorrowingRepository(), UserRepository())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnAdd.setOnClickListener {
            saveItem()
        }

        binding.btnCancel.setOnClickListener {
            confirmCancel()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawableResource(R.drawable.dialog_rounded_background)
    }

    private fun saveItem() {
        if (validateItemInput()) {
            val code = binding.etItemCode.text.toString().trim()
            val name = binding.etToolName.text.toString().trim()
            val type = binding.etType.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()
            val status = "Tersedia"

            viewModel.itemExists(code) { exists ->
                if (exists) {
                    CustomDialog.alert(
                        context = requireContext(),
                        message = getString(R.string.item_already_exists, code),
                    )
                } else {
                    CustomDialog.confirm(
                        context = requireContext(),
                        message = getString(R.string.add_item_confirmation),
                        onConfirm = {
                            val item = Item(
                                item_id = code,
                                item_name = name,
                                item_type = type,
                                item_status = status,
                                item_description = description
                            )

                            viewModel.addItem(item) { success ->
                                if (success) {
                                    CustomDialog.success(
                                        context = requireContext(),
                                        message = getString(R.string.item_save_success),
                                        onDismiss = { dismiss() }
                                    )
                                } else {
                                    CustomDialog.alert(
                                        context = requireContext(),
                                        message = getString(R.string.item_save_failed)
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    private fun validateItemInput(): Boolean {
        val code = binding.etItemCode.text.toString().trim()
        val name = binding.etToolName.text.toString().trim()
        val type = binding.etType.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        if (code.isEmpty() || name.isEmpty() || type.isEmpty() || description.isEmpty()) {
            CustomDialog.alert(
                context = requireContext(),
                message = getString(R.string.all_fields_required)
            )
            return false
        }
        return true
    }

    private fun confirmCancel() {
        val code = binding.etItemCode.text.toString().trim()
        val name = binding.etToolName.text.toString().trim()
        val type = binding.etType.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        if (code.isNotEmpty() || name.isNotEmpty() || type.isNotEmpty() || description.isNotEmpty()) {
            CustomDialog.confirm(
                context = requireContext(),
                message = getString(R.string.unsaved_changes_discard),
                onConfirm = { dismiss() }
            )
        } else {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}