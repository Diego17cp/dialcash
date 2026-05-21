package com.dialcadev.dialcash.features.accounts.presentation.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.core.datastore.UserDataStore
import com.dialcadev.dialcash.core.models.UserPreferences
import com.dialcadev.dialcash.core.ui.components.showAccountDetailsBottomSheet
import com.dialcadev.dialcash.databinding.FragmentAccountsBinding
import com.dialcadev.dialcash.features.accounts.presentation.adapter.AccountsAdapter
import com.dialcadev.dialcash.features.accounts.presentation.viewmodels.AllAccountsViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AccountsFragment : Fragment() {
    private var _binding: FragmentAccountsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AllAccountsViewModel by viewModels()
    private lateinit var accountsAdapter: AccountsAdapter

    @Inject
    lateinit var userDataStore: UserDataStore
    private var preferences: UserPreferences? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSwipeToRefresh()
        setupListeners()
        observeState()
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshAccounts()
        }
    }

    private fun setupRecyclerView() {
        accountsAdapter = AccountsAdapter(
            onAccountClick = { account ->
                requireContext().showAccountDetailsBottomSheet(
                    account = account,
                    currencySymbol = preferences?.currencySymbol ?: "$",
                    onUpdate = { updated -> viewModel.updateAccount(updated) },
                    onDelete = { toDelete -> viewModel.deleteAccount(toDelete) }
                )
            },
            currencySymbol = preferences?.currencySymbol ?: "$"
        )
        binding.recyclerViewAccounts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = accountsAdapter
        }
    }

    private fun setupListeners() {
        binding.btnNewAccount.setOnClickListener {
            val intent = Intent(
                requireContext(),
                NewAccountActivity::class.java
            )
            startActivity(intent)
        }
    }

    private fun updateEmptyState(accountsEmpty: Boolean) {
        if (accountsEmpty) {
            binding.layoutAccounts.visibility = View.GONE
            binding.layoutNoAccounts.visibility = View.VISIBLE
        } else {
            binding.layoutAccounts.visibility = View.VISIBLE
            binding.layoutNoAccounts.visibility = View.GONE
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            userDataStore.getUserData().collect { userPreferences ->
                preferences = userPreferences
                accountsAdapter.updateCurrencySymbol(preferences?.currencySymbol ?: "$")
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.swipeRefreshLayout.isRefreshing = state.isLoading
                    binding.progressBar.visibility =
                        if (state.isLoading && !binding.swipeRefreshLayout.isRefreshing) View.VISIBLE else View.GONE
                    accountsAdapter.submitList(state.accounts)
                    updateEmptyState(state.accounts.isEmpty())
                    state.errorMessage?.let { error ->
                        Snackbar.make(
                            binding.root, error, Snackbar.LENGTH_LONG
                        ).setAction(getString(R.string.retry)) { viewModel.refreshAccounts() }
                            .show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshAccounts()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}