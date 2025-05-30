package com.pplm.projectinventarisuas.ui.adminsection.item

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import com.pplm.projectinventarisuas.data.model.ItemSummary
import com.pplm.projectinventarisuas.utils.adapter.ItemSummaryAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.pplm.projectinventarisuas.R
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
    private lateinit var summaryAdapter: ItemSummaryAdapter
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

        setupViewModel()
        setupSearchBar()
        setupSwipeToRefresh()
        setupRecyclerView()
    }

    private fun setupSearchBar() {
        binding.etSearch.doOnTextChanged { text, _, _, _ ->
            val query = text.toString()
            viewModel.searchItems(query)
        }
    }

    private fun setupRecyclerView() {
        adapter = ItemAdapter(emptyList(), onItemClick = { selectedItem ->
            showItemOptionsDialog(selectedItem)
        }, isStudentMode = false)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun setupViewModel() {
        val borrowingRepository = BorrowingRepository()
        val userRepository = UserRepository()
        val viewModelFactory =
            ViewModelFactory(ItemRepository(), borrowingRepository, userRepository)
        viewModel = ViewModelProvider(this, viewModelFactory)[ItemViewModel::class.java]

        viewModel.items.observe(viewLifecycleOwner) { itemList ->
            adapter.updateItems(itemList)
            binding.swipeRefresh.isRefreshing = false
        }

        viewModel.summaryItems.observe(viewLifecycleOwner) { itemList ->
            val summaryMap = itemList
                .groupingBy { it.item_type }
                .eachCount()
                .map { ItemSummary(it.key, it.value) }

            summaryAdapter = ItemSummaryAdapter(summaryMap)
            binding.rvItemSummary.layoutManager = GridLayoutManager(requireContext(), 2)
            binding.rvItemSummary.adapter = summaryAdapter
        }

        viewModel.loadItems()
    }

    private fun showItemOptionsDialog(item: Item) {
        if (isDialogVisible) return

        isDialogVisible = true
        CustomDialog.options(
            context = requireContext(),
            title = getString(R.string.select_option),
            onView = {
                showItemDetailDialog(item, false)
                isDialogVisible = false
            },
            onEdit = {
                showItemDetailDialog(item, true)
                isDialogVisible = false
            },
            onDelete = {
                deleteItem(item)
            },
            onDismiss = {
                isDialogVisible = false
            }
        )
    }

    private fun deleteItem(item: Item) {
        CustomDialog.confirm(
            context = requireContext(),
            message =  getString(R.string.confirm_delet),
            onConfirm = {
                viewModel.deleteItem(item)
                CustomDialog.alert(
                    context = requireContext(),
                    message = getString(R.string.delet_item_succes),
                    onDismiss = { isDialogVisible = false }
                )
            },
            onCancel = {
                isDialogVisible = false
            }
        )
    }

    private fun setupSwipeToRefresh() {
        binding.etSearch.setText("")
        binding.etSearch.clearFocus()

        val imm =
            requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)

        viewModel.loadItems()

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadItems()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun showItemDetailDialog(item: Item, isEditMode: Boolean) {
        val dialog = ItemDetailDialogFragment.newInstance(item, isEditMode)
        dialog.show(childFragmentManager, "ItemDetailDialog")
    }
}