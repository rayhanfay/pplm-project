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
import androidx.recyclerview.widget.LinearLayoutManager
import com.pplm.projectinventarisuas.data.model.Borrowing
import com.pplm.projectinventarisuas.data.repository.BorrowingRepository
import com.pplm.projectinventarisuas.data.repository.ItemRepository
import com.pplm.projectinventarisuas.data.repository.UserRepository
import com.pplm.projectinventarisuas.databinding.FragmentBorrowingBinding
import com.pplm.projectinventarisuas.utils.adapter.BorrowingAdapter
import com.pplm.projectinventarisuas.utils.components.CustomDialog
import com.pplm.projectinventarisuas.utils.viewmodel.BorrowingViewModel
import com.pplm.projectinventarisuas.utils.viewmodel.ViewModelFactory

class BorrowingFragment : Fragment() {

    private var _binding: FragmentBorrowingBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: BorrowingViewModel
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

        setupRecyclerView()
        setupViewModel()
    }

    private fun setupRecyclerView() {
        adapter = BorrowingAdapter(emptyList()) { selectedBorrowing ->
            showBorrowingOptionsDialog(selectedBorrowing)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun showBorrowingOptionsDialog(borrowing: Borrowing) {
        val options = arrayOf("View Borrowing", "Edit Borrowing", "Delete Borrowing")

        CustomDialog.options(
            context = requireContext(),
            title = "Pilih Aksi",
            options = listOf("View Borrowing", "Edit Borrowing", "Delete Borrowing")
        ) { which ->
            when (which) {
                0 -> viewBorrowing(borrowing)
                1 -> editBorrowing(borrowing)
                2 -> deleteBorrowing(borrowing)
            }
        }
    }

    private fun viewBorrowing(borrowing: Borrowing) {
        val intent = Intent(requireContext(), BorrowingDetailActivity::class.java)
        intent.putExtra("borrowing", borrowing)
        startActivity(intent)
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
        val viewModelFactory =
            ViewModelFactory(ItemRepository(), borrowingRepository, userRepository)
        viewModel = ViewModelProvider(this, viewModelFactory)[BorrowingViewModel::class.java]

        viewModel.borrowingList.observe(viewLifecycleOwner) { borrowingList ->
            adapter = BorrowingAdapter(borrowingList) { selectedItem ->
                showBorrowingOptionsDialog(selectedItem)
            }
            binding.recyclerView.adapter = adapter
            adapter.notifyDataSetChanged()
        }

        viewModel.loadBorrowingData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}