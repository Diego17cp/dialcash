package com.dialcadev.dialcash.ui.home

import android.content.Intent
import android.icu.text.DateFormat
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
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
import com.dialcadev.dialcash.databinding.RecycleAccountItemBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.map
import com.dialcadev.dialcash.data.entities.Account
import com.dialcadev.dialcash.data.entities.IncomeGroup
import com.dialcadev.dialcash.data.entities.Transaction
import com.dialcadev.dialcash.databinding.RecycleTransactionItemBinding
import com.dialcadev.dialcash.ui.shared.BottomSheetManager
import kotlinx.coroutines.launch
import kotlin.toString

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var accountsAdapter: MainAccountsAdapter
    private lateinit var transactionsAdapter: RecentTransactionsAdapter

    private val dateFormat = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault())

    private val accountTypeLabels = arrayOf(
        "Bank", "Card", "Cash", "Wallet", "Debt", "Savings", "Other"
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
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
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
        val manager = BottomSheetManager(requireContext(), layoutInflater)
        accountsAdapter = MainAccountsAdapter { account ->
            manager.showAccountBottomSheet(
                account,
                { updated -> viewModel.updateAccount(updated) },
                { toDelete -> viewModel.deleteAccount(toDelete) }
            )
        }
        binding.recyclerViewAccounts.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = accountsAdapter
        }

        transactionsAdapter = RecentTransactionsAdapter { transaction ->
            fun <T> LiveData<T>.observeOnce(owner: LifecycleOwner, onChange: (T) -> Unit) {
                val observer = object : Observer<T> {
                    override fun onChanged(value: T) {
                        onChange(value)
                        removeObserver(this)
                    }
                }
                observe(owner, observer)
            }
            viewModel.loadAccounts()
            viewModel.loadIncomeGroups()
            viewModel.accounts.observeOnce(viewLifecycleOwner) { accounts ->
                viewModel.incomeGroups.observeOnce(viewLifecycleOwner) { groups ->
                    manager.showTransactionBottomSheet(
                        transaction,
                        accounts,
                        groups,
                        { updated -> viewModel.updateTransaction(updated) },
                        { toDelete -> viewModel.deleteTransaction(toDelete) },
                        { id, type, accountId, amount, accountToId, incomeGroupId, onResult: (Boolean, String?) -> Unit ->
                            viewModel.validateTransactionBalance(
                                id, type, accountId, amount, accountToId, incomeGroupId, onResult
                            )
                        }
                    )
                }
            }
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
            val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav)
            bottomNav.selectedItemId = R.id.accountsFragment
        }
    }

    private fun navigateToTransactionType(transactionType: String) {
        val intent = Intent(this.context, NewTransactionActivity::class.java)
        intent.putExtra("transaction_type", transactionType)
        startActivity(intent)
    }

    private fun updateEmptyState() {
        val isAccountsEmpty = accountsAdapter.currentList.isEmpty()
        val isTransactionsEmpty = transactionsAdapter.currentList.isEmpty()
        if (isAccountsEmpty) {
            binding.btnQuickIncome.isEnabled = false
            binding.btnQuickExpense.isEnabled = false
            binding.btnQuickTransfer.isEnabled = false
        } else {
            binding.btnQuickIncome.isEnabled = true
            binding.btnQuickExpense.isEnabled = true
            binding.btnQuickTransfer.isEnabled = true
        }
        binding.layoutNoInfo.visibility =
            if (isAccountsEmpty && isTransactionsEmpty) View.VISIBLE else View.GONE
    }

    private fun observeViewModel() {
        viewModel.totalBalance.observe(viewLifecycleOwner) { total ->
            binding.textTotalBalance.text = getString(R.string.currency_format, total)
        }
        viewModel.mainAccounts.observe(viewLifecycleOwner) { accounts ->
            accountsAdapter.submitList(accounts)
            updateEmptyState()
            binding.layoutMainAccounts.visibility =
                if (accounts.isEmpty()) View.GONE else View.VISIBLE
        }
        viewModel.recentTransactions.observe(viewLifecycleOwner) { transactions ->
            transactionsAdapter.submitList(transactions)
            updateEmptyState()
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
                    .setAction("Retry") { viewModel.refreshData() }.show()
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