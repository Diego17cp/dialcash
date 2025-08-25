package com.dialcadev.dialcash.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.databinding.FragmentHomeBinding
import com.dialcadev.dialcash.ui.home.adapters.MainAccountsAdapter
import com.dialcadev.dialcash.ui.home.adapters.RecentTransactionsAdapter
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dialcadev.dialcash.NewTransactionActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var accountsAdapter: MainAccountsAdapter
    private lateinit var transactionsAdapter: RecentTransactionsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupOnClickListeners()
        setupSwipeRefresh()
        observeViewModel()
    }
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshData()
        }
    }
    private fun setupRecyclerViews() {
        accountsAdapter = MainAccountsAdapter { account ->
            // Navegación temporal hasta que se genere HomeFragmentDirections
            findNavController().navigate(R.id.accountsFragment)
        }
        binding.recyclerViewAccounts.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = accountsAdapter
        }

        transactionsAdapter = RecentTransactionsAdapter { transaction ->
            // Navegación temporal
            findNavController().navigate(R.id.transactionsFragment)
        }
        binding.recyclerViewTransactions.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = transactionsAdapter
        }
    }

    private fun setupOnClickListeners() {
        binding.btnQuickIncome.setOnClickListener {
            navigateToTransactionType("income")
        }
        binding.btnQuickExpense.setOnClickListener {
            navigateToTransactionType("expense")
        }
        binding.btnQuickTransfer.setOnClickListener {
            navigateToTransactionType("transfer")
        }
        binding.btnViewAllTransactions.setOnClickListener {
            val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav)
            bottomNav.selectedItemId = R.id.transactionsFragment
        }
        binding.btnViewAllAccounts.setOnClickListener {
            findNavController().navigate(R.id.accountsFragment)
        }
    }
    private fun navigateToTransactionType(transactionType: String) {
        val intent = Intent(this.context, NewTransactionActivity::class.java)
        intent.putExtra("transaction_type", transactionType)
        startActivity(intent)
    }

    private fun observeViewModel() {
        viewModel.totalBalance.observe(viewLifecycleOwner) { total ->
            binding.textTotalBalance.text = getString(R.string.currency_format, total)
        }
        viewModel.mainAccounts.observe(viewLifecycleOwner) { accounts ->
            accountsAdapter.submitList(accounts)
            binding.layoutMainAccounts.visibility =
                if (accounts.isEmpty()) View.GONE else View.VISIBLE
        }
        viewModel.recentTransactions.observe(viewLifecycleOwner) { transactions ->
            transactionsAdapter.submitList(transactions)
            binding.layoutRecentTransactions.visibility =
                if (transactions.isEmpty()) View.GONE else View.VISIBLE
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefreshLayout.isRefreshing = isLoading
            binding.progressBar.visibility =
                if (isLoading && !binding.swipeRefreshLayout.isRefreshing) View.VISIBLE else View.GONE
        }
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG)
                    .setAction("Retry") { viewModel.refreshData() }
                    .show()
                viewModel.clearError()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}