package com.pplm.projectinventarisuas.ui.adminsection.borrowing

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import android.util.Log

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
        Log.d("BorrowingFragment", "onCreateView called.")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("BorrowingFragment", "onViewCreated called.")

        setupViewModel()
        setupRecyclerView()
        setupSwipeRefresh()
    }

    private fun setupRecyclerView() {
        adapter =
            BorrowingAdapter(emptyList()) { selectedBorrowing ->
                showBorrowingOptionsDialog(selectedBorrowing)
            }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        Log.d("BorrowingFragment", "setupSwipeRefresh called.")
        binding.swipeRefreshLayout.setOnRefreshListener {
            Log.d(
                "BorrowingFragment",
                "Swipe refresh triggered. Loading data..."
            )
            viewModel.loadBorrowingData()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d("BorrowingFragment", "isLoading observed: $isLoading")
            binding.swipeRefreshLayout.isRefreshing = isLoading
        }
    }

    private fun showBorrowingOptionsDialog(borrowing: Borrowing) {
        if (isDialogVisible) return
        Log.d(
            "BorrowingFragment",
            "Showing options for borrowing ID: ${borrowing.borrowing_id}"
        )

        isDialogVisible = true
        CustomDialog.options(
            context = requireContext(),
            title = "Pilih Aksi",
            onView = {
                Log.d(
                    "BorrowingFragment",
                    "Option: View, Borrowing ID: ${borrowing.borrowing_id}"
                )
                showBorrowingDetailDialog(borrowing, false)
                isDialogVisible = false
            },
            onEdit = {
                Log.d(
                    "BorrowingFragment",
                    "Option: Edit, Borrowing ID: ${borrowing.borrowing_id}"
                )
                showBorrowingDetailDialog(borrowing, true)
                isDialogVisible = false
            },
            onDelete = {
                Log.d(
                    "BorrowingFragment",
                    "Option: Delete, Borrowing ID: ${borrowing.borrowing_id}"
                )
                deleteBorrowing(borrowing)
            },
            onDismiss = {
                isDialogVisible = false
            }
        )
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
        Log.d("BorrowingFragment", "setupViewModel called.")
        val borrowingRepository = BorrowingRepository()
        val userRepository = UserRepository()
        val itemRepository = ItemRepository()
        val viewModelFactory =
            ViewModelFactory(itemRepository, borrowingRepository, userRepository)
        viewModel = ViewModelProvider(this, viewModelFactory)[BorrowingViewModel::class.java]

        viewModel.borrowingList.observe(viewLifecycleOwner) { borrowingList ->
            Log.d(
                "BorrowingFragment",
                "borrowingList observed. List size: ${borrowingList.size}"
            )
            if (borrowingList.isEmpty()) {
                Log.d(
                    "BorrowingFragment",
                    "Borrowing list is empty in observer."
                )
            }
            adapter = BorrowingAdapter(borrowingList) { selectedBorrowing ->
                showBorrowingOptionsDialog(selectedBorrowing)
            }
            binding.recyclerView.adapter = adapter
        }

        viewModel.summaryBorrowingStatus.observe(viewLifecycleOwner) { itemList ->
            Log.d(
                "BorrowingFragment",
                "summaryBorrowingStatus observed. List size: ${itemList.size}"
            )
            val summaryMap = itemList
                .groupingBy { it.status }
                .eachCount()
                .map { BorrowingSummary(it.key, it.value) }

            summaryAdapter = BorrowingSummaryAdapter(summaryMap)
            binding.rvBorrowingSummary.layoutManager = GridLayoutManager(requireContext(), 2)
            binding.rvBorrowingSummary.adapter = summaryAdapter
        }

        Log.d("BorrowingFragment", "Calling viewModel.loadBorrowingData().")
        viewModel.loadBorrowingData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("BorrowingFragment", "onDestroyView called.")
        _binding = null
    }

    fun showBorrowingDetailDialog(borrowing: Borrowing, isEditMode: Boolean) {
        Log.d(
            "BorrowingFragment",
            "Attempting to show dialog for borrowing ID: ${borrowing.borrowing_id}, Edit Mode: $isEditMode"
        )
        val dialog = BorrowingDetailDialogFragment.newInstance(borrowing, isEditMode)
        dialog.show(childFragmentManager, "BorrowingDetailDialog")
    }
}