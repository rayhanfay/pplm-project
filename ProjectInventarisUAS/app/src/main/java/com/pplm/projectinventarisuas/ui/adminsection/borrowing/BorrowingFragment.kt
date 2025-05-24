package com.pplm.projectinventarisuas.ui.adminsection.borrowing

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.pplm.projectinventarisuas.data.model.Borrowing
import com.pplm.projectinventarisuas.data.model.BorrowingSummary
import com.pplm.projectinventarisuas.data.repository.BorrowingRepository
import com.pplm.projectinventarisuas.data.repository.ItemRepository
import com.pplm.projectinventarisuas.data.repository.UserRepository
import com.pplm.projectinventarisuas.databinding.FragmentBorrowingBinding
import com.pplm.projectinventarisuas.utils.adapter.BorrowingAdapter
import com.pplm.projectinventarisuas.utils.adapter.BorrowingSummaryAdapter
import com.pplm.projectinventarisuas.utils.components.CustomDialog
import com.pplm.projectinventarisuas.utils.viewmodel.BorrowingViewModel
import com.pplm.projectinventarisuas.utils.viewmodel.ViewModelFactory

class BorrowingFragment : Fragment() {

    private var _binding: FragmentBorrowingBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: BorrowingViewModel
    private lateinit var summaryAdapter: BorrowingSummaryAdapter
    private lateinit var adapter: BorrowingAdapter
    private var isDialogVisible = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBorrowingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupRecyclerView()
        setupSwipeRefresh()
    }

    private fun setupRecyclerView() {
        val itemRepository = ItemRepository()
        adapter = BorrowingAdapter(emptyList(), itemRepository) { selectedBorrowing ->
            showBorrowingOptionsDialog(selectedBorrowing)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadBorrowingData()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefreshLayout.isRefreshing = isLoading
        }
    }

    private fun showBorrowingOptionsDialog(borrowing: Borrowing) {
        if (isDialogVisible) return

        isDialogVisible = true
        CustomDialog.options(
            context = requireContext(),
            title = "Pilih Aksi",
            onView = {
                viewBorrowing(borrowing)
            },
            onEdit = {
                editBorrowing(borrowing)
            },
            onDelete = {
                deleteBorrowing(borrowing)
            },
            onDismiss = {
                isDialogVisible = false
            }
        )
    }

    private fun viewBorrowing(borrowing: Borrowing) {
        val dialogFragment = BorrowingDetailDialogFragment(borrowing)
        dialogFragment.show(childFragmentManager, "BorrowingDetailDialog")
    }

    private fun editBorrowing(borrowing: Borrowing) {
        Toast.makeText(requireContext(), "Edit: ${borrowing.borrowing_id}", Toast.LENGTH_SHORT)
            .show()
        // TODO: Navigate to AddBorrowingActivity with edit mode
    }

    private fun deleteBorrowing(borrowing: Borrowing) {
        if (isDialogVisible) return

        isDialogVisible = true
        CustomDialog.confirm(
            context = requireContext(),
            message = "Yakin ingin menghapus peminjaman ini?",
            onConfirm = {
                viewModel.deleteBorrowing(borrowing)
                CustomDialog.alert(
                    context = requireContext(),
                    message = "Peminjaman berhasil dihapus",
                    onDismiss = { isDialogVisible = false }
                )
            },
            onCancel = {
                isDialogVisible = false
            }
        )
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupViewModel() {
        val borrowingRepository = BorrowingRepository()
        val userRepository = UserRepository()
        val itemRepository = ItemRepository()
        val viewModelFactory =
            ViewModelFactory(itemRepository, borrowingRepository, userRepository)
        viewModel = ViewModelProvider(this, viewModelFactory)[BorrowingViewModel::class.java]

        viewModel.borrowingList.observe(viewLifecycleOwner) { borrowingList ->
            adapter = BorrowingAdapter(borrowingList, itemRepository) { selectedItem ->
                showBorrowingOptionsDialog(selectedItem)
            }
            binding.recyclerView.adapter = adapter
        }

        viewModel.summaryBorrowingStatus.observe(viewLifecycleOwner) { itemList ->
            val summaryMap = itemList
                .groupingBy { it.status }
                .eachCount()
                .map { BorrowingSummary(it.key, it.value) }

            summaryAdapter = BorrowingSummaryAdapter(summaryMap)
            binding.rvBorrowingSummary.layoutManager = GridLayoutManager(requireContext(), 2)
            binding.rvBorrowingSummary.adapter = summaryAdapter
        }

        viewModel.loadBorrowingData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}