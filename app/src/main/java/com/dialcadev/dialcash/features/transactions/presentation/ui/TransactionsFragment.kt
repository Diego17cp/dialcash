package com.dialcadev.dialcash.features.transactions.presentation.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.core.datastore.UserDataStore
import com.dialcadev.dialcash.core.models.UserPreferences
import com.dialcadev.dialcash.core.ui.components.showTransactionDetailsBottomSheet
import com.dialcadev.dialcash.core.ui.components.showTransactionFiltersBottomSheet
import com.dialcadev.dialcash.databinding.FragmentTransactionsBinding
import com.dialcadev.dialcash.features.transactions.presentation.adapters.TransactionsAdapter
import com.dialcadev.dialcash.features.transactions.presentation.viewmodels.AllTransactionsViewModel
import com.dialcadev.dialcash.ui.transactions.ChartsActivity
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TransactionsFragment : Fragment() {
    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AllTransactionsViewModel by viewModels()
    private lateinit var transactionsAdapter: TransactionsAdapter

    @Inject
    lateinit var userDataStore: UserDataStore
    private var preferences: UserPreferences? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()
        setupUI()
        setupRecyclerView()
        setupSwipeToRefresh()
        observeState()
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.chart_menu, menu)
                menuInflater.inflate(R.menu.filters_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_filters -> {
                        val state = viewModel.uiState.value
                        requireContext().showTransactionFiltersBottomSheet(
                            currentFilters = state.currentFilters,
                            accounts = state.accounts,
                            onApplyFilters = { newFilters ->
                                viewModel.updateFilters(newFilters)
                            }
                        )
                        true
                    }

                    R.id.action_chart -> {
                        val intent = Intent(
                            requireContext(),
                            // Todo: Change this class when refactorize it in the new arch
                            ChartsActivity::class.java
                        )
                        startActivity(intent)
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun setupUI() {
        binding.btnClearFilters.setOnClickListener {
            viewModel.clearFilters()
        }
    }

    private fun setupRecyclerView() {
        transactionsAdapter = TransactionsAdapter(
            onTransactionClick = { transaction ->
                val currentState = viewModel.uiState.value
                requireContext().showTransactionDetailsBottomSheet(
                    transaction = transaction,
                    accounts = currentState.accounts,
                    incomeGroups = currentState.incomeGroups,
                    currencySymbol = preferences?.currencySymbol ?: "$",
                    onUpdate = { updated -> viewModel.updateTransaction(updated) },
                    onDelete = { toDelete -> viewModel.deleteTransaction(toDelete) },
                    onValidateBalance = { transactionId, type, accountId, amount, accountToId, incomeGroupId, onResult ->
                        viewModel.validateTransactionBalance(
                            transactionId,
                            type,
                            accountId,
                            amount,
                            accountToId,
                            incomeGroupId,
                            onResult
                        )
                    }
                )
            },
            currencySymbol = preferences?.currencySymbol ?: "$"
        )
        binding.recyclerViewTransactions.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = transactionsAdapter
        }
    }

    private fun updateEmptyState(transactionsEmpty: Boolean, isFiltered: Boolean) {
        binding.layoutTransactions.isVisible = !transactionsEmpty
        binding.layoutNoTransactions.isVisible = transactionsEmpty
        if (transactionsEmpty) {
            binding.tvEmptyTitle.text =
                if (isFiltered) getString(R.string.no_results) else getString(R.string.no_transactions_yet)
            binding.tvEmptySubtitle.text =
                if (isFiltered) getString(R.string.try_adjusting_filters) else getString(R.string.add_your_first_transaction)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            userDataStore.getUserData().collectLatest { userPreferences ->
                preferences = userPreferences
                transactionsAdapter.updateCurrencySymbol(preferences?.currencySymbol ?: "$")
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading && state.errorMessage == null
                    if (!state.isLoading) binding.swipeRefreshLayout.isRefreshing = false
                    transactionsAdapter.submitList(state.filteredTransactions)
                    binding.btnClearFilters.isVisible = state.isFiltered
                    updateEmptyState(state.filteredTransactions.isEmpty(), state.isFiltered)
                    state.errorMessage?.let { error ->
                        Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG)
                            .setAction(getString(R.string.retry)) { viewModel.clearError() }
                            .show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}