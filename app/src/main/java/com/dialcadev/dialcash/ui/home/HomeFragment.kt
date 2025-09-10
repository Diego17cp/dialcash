package com.dialcadev.dialcash.ui.home

import android.content.Intent
import android.icu.text.DateFormat
import android.media.Image
import android.os.Bundle
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
import com.dialcadev.dialcash.data.entities.Account

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var accountsAdapter: MainAccountsAdapter
    private lateinit var transactionsAdapter: RecentTransactionsAdapter

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
            val bottomSheetDialog = BottomSheetDialog(requireContext())
            val binding = RecycleAccountItemBinding.inflate(layoutInflater)
            binding.apply {
                tvAccountName.text = account.name
                etEditAccountName.setText(account.name)
                tvAccountType.text = account.type.replaceFirstChar { it.uppercase() }
                tvAccountBalance.text = getString(R.string.currency_format, account.originalBalance)
                etInitialBalance.setText(account.originalBalance.toString())
                tvAccountCurrentBalance.text = getString(R.string.currency_format, account.balance)
                tvCreatedAt.text = "Created at: ${account.createdAt}"
                val iconRes = when (account.type) {
                    "bank" -> R.drawable.ic_bank
                    "cash" -> R.drawable.ic_cash
                    "card" -> R.drawable.ic_card
                    "wallet" -> R.drawable.ic_accounts_outline
                    else -> R.drawable.ic_account_default
                }
                imageAccountIcon.setImageResource(iconRes)

                val accountTypeAdapter = ArrayAdapter(requireContext(),
                    android.R.layout.simple_dropdown_item_1line, accountTypeLabels)
                actvAccountType.setAdapter(accountTypeAdapter)
                actvAccountType.setText(account.type.replaceFirstChar { it.uppercase() }, false)

                fun resetView() {
                    tvAccountName.visibility = View.VISIBLE
                    tilAccountName.visibility = View.GONE
                    tvAccountType.visibility = View.VISIBLE
                    tilAccountType.visibility = View.GONE
                    tvAccountBalance.visibility = View.VISIBLE
                    tilInitialBalance.visibility = View.GONE
                    actionsRow.visibility = View.VISIBLE
                    editFooter.visibility = View.GONE
                    deleteConfirmFooter.visibility = View.GONE
                }
                fun validateForm(): Boolean {
                    val name = etEditAccountName.text.toString().trim()
                    val balanceText = etInitialBalance.text.toString().trim()
                    val typeText = actvAccountType.text.toString().trim()
                    var isValid = true

                    if (name.isEmpty()) {
                        tilAccountName.error = "Name cannot be empty"
                        isValid = false
                    } else {
                        tilAccountName.error = null
                    }

                    if (typeText.isEmpty() || !accountTypeMapped.containsKey(typeText)) {
                        tilAccountType.error = "Select a valid account type"
                        isValid = false
                    } else {
                        tilAccountType.error = null
                    }

                    val balance = balanceText.toDoubleOrNull()
                    if (balanceText.isEmpty()) {
                        tilInitialBalance.error = "Balance cannot be empty"
                        isValid = false
                    } else if (balance == null) {
                        tilInitialBalance.error = "Enter a valid number"
                        isValid = false
                    } else {
                        tilInitialBalance.error = null
                    }

                    btnSave.isEnabled = isValid
                    return isValid
                }
                fun editAccount() {
                    if (!validateForm()) return
                    val newName = etEditAccountName.text.toString().trim()
                    val newTypeLabel = actvAccountType.text.toString().trim()
                    val newType = accountTypeMapped[newTypeLabel] ?: account.type
                    val newBalance = etInitialBalance.text.toString().trim().toDoubleOrNull() ?: account.originalBalance
                    viewModel.updateAccount(Account(
                        id = account.id,
                        name = newName,
                        type = newType,
                        balance = newBalance
                    ))
                    resetView()
                }
                etEditAccountName.addTextChangedListener { validateForm() }
                actvAccountType.setOnItemClickListener { _, _, _, _ -> validateForm() }
                etInitialBalance.addTextChangedListener { validateForm() }
                btnEdit.setOnClickListener {
                    tvAccountName.visibility = View.GONE
                    tilAccountName.visibility = View.VISIBLE
                    tvAccountType.visibility = View.GONE
                    tilAccountType.visibility = View.VISIBLE
                    tvAccountBalance.visibility = View.GONE
                    tilInitialBalance.visibility = View.VISIBLE
                    actionsRow.visibility = View.GONE
                    editFooter.visibility = View.VISIBLE
                    etEditAccountName.setText(account.name)
                    etInitialBalance.setText(account.originalBalance.toString())
                }
                btnCancel.setOnClickListener {
                    if (actionsRow.isGone && editFooter.isVisible) {
                        resetView()
                    } else {
                        bottomSheetDialog.dismiss()
                    }
                }
                btnDelete.setOnClickListener {
                    actionsRow.visibility = View.GONE
                    deleteConfirmFooter.visibility = View.VISIBLE
                }
                btnCancelDelete.setOnClickListener {
                    if (deleteConfirmFooter.isVisible && actionsRow.isGone) {
                        resetView()
                    } else {
                        bottomSheetDialog.dismiss()
                    }
                }
                btnConfirmDelete.setOnClickListener {
                    viewModel.deleteAccount(Account(
                        id = account.id,
                        name = account.name,
                        type = account.type,
                        balance = account.originalBalance
                    ))
                    bottomSheetDialog.dismiss()
                }
                btnSave.setOnClickListener {
                    editAccount()
                }
            }
            bottomSheetDialog.setContentView(binding.root)
            bottomSheetDialog.show()
        }
        binding.recyclerViewAccounts.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = accountsAdapter
        }

        transactionsAdapter = RecentTransactionsAdapter { transaction ->
            // aqui deberia mostrar un fragment o activity con la info del elemento seleccionado
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