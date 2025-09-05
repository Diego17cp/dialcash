package com.dialcadev.dialcash.ui.incomegroup

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.dialcadev.dialcash.NewIncomeActivity
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.databinding.FragmentIncomesBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class IncomesFragment : Fragment() {

    private var _binding: FragmentIncomesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: IncomesViewModel by viewModels()
    private lateinit var incomesAdapter: IncomesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentIncomesBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSwipeToRefresh()
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }
    private fun setupSwipeToRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshIncomes()
        }
    }
    private fun setupRecyclerView() {
        incomesAdapter = IncomesAdapter { income ->

        }
        binding.recyclerViewIncomes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = incomesAdapter
        }
    }
    private fun setupListeners() {
        binding.btnNewIncome.setOnClickListener {
            val intent = Intent(requireContext(), NewIncomeActivity::class.java)
            startActivity(intent)
        }
    }
    private fun updateEmptyState(isEmpty: Boolean){
        binding.layoutIncomes.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.layoutNoIncomes.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }
    private fun observeViewModel() {
        viewModel.incomes.observe(viewLifecycleOwner) { incomes ->
            incomesAdapter.submitList(incomes)
            updateEmptyState(incomes.isEmpty())
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefreshLayout.isRefreshing = isLoading
            binding.progressBar.visibility = if (isLoading && !binding.swipeRefreshLayout.isRefreshing) View.VISIBLE else View.GONE
        }
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG)
                    .setAction("Retry") { viewModel.refreshIncomes() }
                    .show()
                viewModel.clearError()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshIncomes()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}