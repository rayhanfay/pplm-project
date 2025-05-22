package com.pplm.projectinventarisuas.ui.adminsection.item

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
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
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun saveItem() {
        if (validateItemInput()) {
            val code = binding.etItemCode.text.toString().trim()
            val name = binding.etToolName.text.toString().trim()
            val type = binding.etType.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()
            val status = "Available"

            viewModel.itemExists(code) { exists ->
                if (exists) {
                    CustomDialog.alert(
                        context = requireContext(),
                        message = "Item dengan kode '$code' sudah ada",
                    )
                } else {
                    val item = Item(
                        item_id = code,
                        item_name = name,
                        item_type = type,
                        item_status = status,
                        item_description = description
                    )

                    viewModel.addItem(item) { success ->
                        if (success) {
                            CustomDialog.alert(
                                context = requireContext(),
                                message = "Item berhasil disimpan",
                                onDismiss = { dismiss() }
                            )
                        } else {
                            CustomDialog.alert(
                                context = requireContext(),
                                message = "Gagal menyimpan item"
                            )
                        }
                    }
                }
            }
        }
    }

    private fun validateItemInput(): Boolean {
        val code = binding.etItemCode.text.toString().trim()
        val name = binding.etToolName.text.toString().trim()
        val type = binding.etType.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        if (code.isEmpty() && name.isEmpty() && type.isEmpty() && description.isEmpty()) {
            CustomDialog.alert(
                context = requireContext(),
                message = "Semua field harus diisi"
            )
            return false
        }

        if (code.isEmpty()) {
            CustomDialog.alert(context = requireContext(), message = "Kode item harus diisi")
            return false
        }

        if (name.isEmpty()) {
            CustomDialog.alert(context = requireContext(), message = "Nama tool harus diisi")
            return false
        }

        if (type.isEmpty()) {
            CustomDialog.alert(context = requireContext(), message = "Tipe harus diisi")
            return false
        }

        if (description.isEmpty()) {
            CustomDialog.alert(context = requireContext(), message = "Deskripsi harus diisi")
            return false
        }

        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}