package com.dialcadev.dialcash.ui.accounts

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.data.entities.Account
import com.dialcadev.dialcash.databinding.FragmentAccountsBinding
import com.dialcadev.dialcash.databinding.RecycleAccountItemBinding
import com.dialcadev.dialcash.ui.home.adapters.MainAccountsAdapter
import com.dialcadev.dialcash.ui.shared.BottomSheetManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AccountsFragment : Fragment() {
    private var _binding: FragmentAccountsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AccountsViewModel by viewModels()
    private lateinit var accountsAdapter: AccountsAdapter

    private val accountTypeLabels = arrayOf(
        "Bank",
        "Card",
        "Cash",
        "Wallet",
        "Debt",
        "Savings",
        "Other"
    )
    private val accountTypeMapped = mapOf(
        "Bank" to "bank",
        "Card" to "card",
        "Cash" to "cash",
        "Wallet" to "wallet",
        "Debt" to "debt",
        "Savings" to "savings",
        "Other" to "other"
    )

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
        observeViewModel()
        setupSwipeToRefresh()
        setupListeners()
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshAccounts()
        }
    }

    private fun setupRecyclerView() {
        val manager = BottomSheetManager(requireContext(), layoutInflater)
        accountsAdapter = AccountsAdapter { account ->
            manager.showAccountBottomSheet(
                account,
                { updated -> viewModel.updateAccount(updated) },
                { toDelete -> viewModel.deleteAccount(toDelete) }
            )
        }
        binding.recyclerViewAccounts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = accountsAdapter
        }
    }
    private fun setupListeners() {
        binding.btnNewAccount.setOnClickListener {
            val intent = Intent(requireContext(), NewAccountActivity::class.java)
            startActivity(intent)
        }
    }
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.layoutAccounts.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.layoutNoAccounts.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }
    private fun observeViewModel() {
        viewModel.accounts.observe(viewLifecycleOwner) { accounts ->
            accountsAdapter.submitList(accounts)
            updateEmptyState(accounts.isEmpty())
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefreshLayout.isRefreshing = isLoading
            binding.progressBar.visibility =
                if (isLoading && !binding.swipeRefreshLayout.isRefreshing) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG)
                    .setAction("Retry") { viewModel.refreshAccounts() }
                    .show()
                viewModel.clearError()
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