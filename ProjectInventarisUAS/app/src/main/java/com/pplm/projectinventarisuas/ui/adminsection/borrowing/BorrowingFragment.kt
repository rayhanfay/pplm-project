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
import com.pplm.projectinventarisuas.R

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
        Log.d("BorrowingFragment", "onCreateView dipanggil.")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("BorrowingFragment", "onViewCreated dipanggil.")

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
        Log.d("BorrowingFragment", "setupSwipeRefresh dipanggil.")
        binding.swipeRefreshLayout.setOnRefreshListener {
            Log.d(
                "BorrowingFragment",
                "Refresh swipe terpicu. Memuat data..."
            )
            viewModel.loadBorrowingData()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d("BorrowingFragment", "isLoading diamati: $isLoading")
            binding.swipeRefreshLayout.isRefreshing = isLoading
        }
    }

    private fun showBorrowingOptionsDialog(borrowing: Borrowing) {
        if (isDialogVisible) return
        Log.d(
            "BorrowingFragment",
            "Menampilkan opsi untuk ID peminjaman: ${borrowing.borrowing_id}"
        )

        isDialogVisible = true
        CustomDialog.options(
            context = requireContext(),
            title = getString(R.string.select_option),
            onView = {
                Log.d(
                    "BorrowingFragment",
                    "Opsi: Lihat, ID Peminjaman: ${borrowing.borrowing_id}"
                )
                showBorrowingDetailDialog(borrowing, false)
                isDialogVisible = false
            },
            onEdit = {
                Log.d(
                    "BorrowingFragment",
                    "Opsi: Edit, ID Peminjaman: ${borrowing.borrowing_id}"
                )
                showBorrowingDetailDialog(borrowing, true)
                isDialogVisible = false
            },
            onDelete = {
                Log.d(
                    "BorrowingFragment",
                    "Opsi: Hapus, ID Peminjaman: ${borrowing.borrowing_id}"
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
            message = getString(R.string.delet_confimation),
            onConfirm = {
                viewModel.deleteBorrowing(borrowing)
                CustomDialog.alert(
                    context = requireContext(),
                    message = getString(R.string.delet_succes),
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
        Log.d("BorrowingFragment", "setupViewModel dipanggil.")
        val borrowingRepository = BorrowingRepository()
        val userRepository = UserRepository()
        val itemRepository = ItemRepository()
        val viewModelFactory =
            ViewModelFactory(itemRepository, borrowingRepository, userRepository)
        viewModel = ViewModelProvider(this, viewModelFactory)[BorrowingViewModel::class.java]

        viewModel.borrowingList.observe(viewLifecycleOwner) { borrowingList ->
            Log.d(
                "BorrowingFragment",
                "borrowingList diamati. Ukuran daftar: ${borrowingList.size}"
            )
            if (borrowingList.isEmpty()) {
                Log.d(
                    "BorrowingFragment",
                    "Daftar peminjaman kosong di observer."
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
                "summaryBorrowingStatus diamati. Ukuran daftar: ${itemList.size}"
            )
            val summaryMap = itemList
                .groupingBy { it.status }
                .eachCount()
                .map { BorrowingSummary(it.key, it.value) }

            summaryAdapter = BorrowingSummaryAdapter(summaryMap)
            binding.rvBorrowingSummary.layoutManager = GridLayoutManager(requireContext(), 2)
            binding.rvBorrowingSummary.adapter = summaryAdapter
        }

        Log.d("BorrowingFragment", "Memanggil viewModel.loadBorrowingData().")
        viewModel.loadBorrowingData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("BorrowingFragment", "onDestroyView dipanggil.")
        _binding = null
    }

    fun showBorrowingDetailDialog(borrowing: Borrowing, isEditMode: Boolean) {
        Log.d(
            "BorrowingFragment",
            "Mencoba menampilkan dialog untuk ID peminjaman: ${borrowing.borrowing_id}, Mode Edit: $isEditMode"
        )
        val dialog = BorrowingDetailDialogFragment.newInstance(borrowing, isEditMode)
        dialog.show(childFragmentManager, "BorrowingDetailDialog")
    }
}