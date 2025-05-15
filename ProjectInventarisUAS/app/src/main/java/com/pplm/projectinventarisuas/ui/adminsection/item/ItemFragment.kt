package com.pplm.projectinventarisuas.ui.adminsection.item

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.pplm.projectinventarisuas.data.model.Item
import com.pplm.projectinventarisuas.data.repository.BorrowingRepository
import com.pplm.projectinventarisuas.data.repository.ItemRepository
import com.pplm.projectinventarisuas.data.repository.UserRepository
import com.pplm.projectinventarisuas.databinding.FragmentItemBinding
import com.pplm.projectinventarisuas.utils.adapter.ItemAdapter
import com.pplm.projectinventarisuas.utils.components.CustomDialog
import com.pplm.projectinventarisuas.utils.viewmodel.ItemViewModel
import com.pplm.projectinventarisuas.utils.viewmodel.ViewModelFactory

class ItemFragment : Fragment() {

    private var _binding: FragmentItemBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ItemViewModel
    private lateinit var adapter: ItemAdapter
    private var isDialogVisible = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupViewModel()
    }

    private fun setupRecyclerView() {
        adapter = ItemAdapter(emptyList()) { selectedItem ->
            showItemOptionsDialog(selectedItem)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun setupViewModel() {
        val borrowingRepository = BorrowingRepository()
        val userRepository = UserRepository()
        val viewModelFactory = ViewModelFactory(ItemRepository(), borrowingRepository, userRepository)
        viewModel = ViewModelProvider(this, viewModelFactory)[ItemViewModel::class.java]

        viewModel.items.observe(viewLifecycleOwner) { itemList ->
            adapter.updateItems(itemList)
        }

        viewModel.loadItems()
    }

    private fun showItemOptionsDialog(item: Item) {
        if (isDialogVisible) return

        isDialogVisible = true
        CustomDialog.options(
            context = requireContext(),
            title = "Pilih Aksi",
            options = listOf("View Item", "Edit Item", "Delete Item")
        ) { which ->
            isDialogVisible = false
            when (which) {
                0 -> viewItem(item)
                1 -> editItem(item)
                2 -> deleteItem(item)
            }
        }
    }

    private fun viewItem(item: Item) {
        val intent = Intent(requireContext(), ItemDetailActivity::class.java)
        intent.putExtra("item", item)
        intent.putExtra("isEditMode", false)
        startActivity(intent)
    }

    private fun editItem(item: Item) {
        val intent = Intent(requireContext(), ItemDetailActivity::class.java)
        intent.putExtra("item", item)
        intent.putExtra("isEditMode", true)
        startActivity(intent)
    }

    private fun deleteItem(item: Item) {
        viewModel.deleteItem(item)
        if (!isDialogVisible) {
            isDialogVisible = true
            CustomDialog.alert(
                context = requireContext(),
                message = "Item deleted successfully",
                onDismiss = { isDialogVisible = false }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
