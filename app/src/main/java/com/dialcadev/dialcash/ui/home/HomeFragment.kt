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

                val accountTypeAdapter = ArrayAdapter(
                    requireContext(), android.R.layout.simple_dropdown_item_1line, accountTypeLabels
                )
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
                    val newBalance = etInitialBalance.text.toString().trim().toDoubleOrNull()
                        ?: account.originalBalance
                    viewModel.updateAccount(
                        Account(
                            id = account.id, name = newName, type = newType, balance = newBalance
                        )
                    )
                    resetView()
                    bottomSheetDialog.dismiss()
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
                    viewModel.deleteAccount(
                        Account(
                            id = account.id,
                            name = account.name,
                            type = account.type,
                            balance = account.originalBalance
                        )
                    )
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
            val bottomSheetDialog = BottomSheetDialog(requireContext())
            val binding = RecycleTransactionItemBinding.inflate(layoutInflater)
            binding.apply {
                val iconRes = when (transaction.type) {
                    "income" -> R.drawable.ic_income
                    "expense" -> R.drawable.ic_expense
                    else -> R.drawable.ic_transactions_outline
                }
                val color = when (transaction.type) {
                    "income" -> R.color.positive_amount
                    "transfer" -> R.color.colorPrimary
                    else -> R.color.negative_amount
                }
                val amount = if (transaction.type == "income") "+${
                    root.context.getString(
                        R.string.currency_format, transaction.amount
                    )
                }" else "-${root.context.getString(R.string.currency_format, transaction.amount)}"
                ivTransactionType.setImageResource(iconRes)
                ivTransactionType.setColorFilter(root.context.getColor(color))
                tvTransactionAmount.text = amount
                tvTransactionAmount.setTextColor(root.context.getColor(color))
                etTransactionAmount.setText(transaction.amount.toString())
                tvTransactionDescription.text = transaction.description
                etTransactionDescription.setText(transaction.description)
                tvAccountName.text = transaction.accountName
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
                fun setupAccountAdapters(accounts: List<Account>) {
                    val accountNames = accounts.map { it.name }
                    val accountAdapter = ArrayAdapter(
                        requireContext(), android.R.layout.simple_dropdown_item_1line, accountNames
                    )
                    actvAccountName.setAdapter(accountAdapter)
                    actvAccountToName.setAdapter(accountAdapter)
                    actvAccountName.setText(transaction.accountName, false)
                    actvAccountToName.setText(transaction.accountToName ?: "", false)
                }

                fun setupIncomeGroupAdapter(groups: List<IncomeGroup>) {
                    val groupNames = groups.map { it.name }
                    val groupAdapter = ArrayAdapter(
                        requireContext(), android.R.layout.simple_dropdown_item_1line, groupNames
                    )
                    actvIncomeGroupName.setAdapter(groupAdapter)
                    actvIncomeGroupName.setText(transaction.incomeGroupName ?: "", false)
                }
                viewModel.accounts.observeOnce(viewLifecycleOwner) { accounts ->
                    setupAccountAdapters(accounts)
                }
                viewModel.incomeGroups.observeOnce(viewLifecycleOwner) { groups ->
                    setupIncomeGroupAdapter(groups)
                }

                var selectedAccount: Account? = null
                var selectedAccountTo: Account? = null
                var selectedIncomeGroup: IncomeGroup? = null

                fun resetView() {
                    tvTransactionAmount.visibility = View.VISIBLE
                    tilTransactionAmount.visibility = View.GONE
                    tvTransactionDescription.visibility = View.VISIBLE
                    tilTransactionDescription.visibility = View.GONE
                    tvAccountName.visibility = View.VISIBLE
                    tilAccountName.visibility = View.GONE
                    if (transaction.type == "transfer" && transaction.accountToName != null) {
                        tvAccountToName.visibility = View.VISIBLE
                        tilAccountToName.visibility = View.GONE
                    }
                    if (transaction.type == "expense" && transaction.incomeGroupName != null) {
                        tvIncomeGroupName.visibility = View.VISIBLE
                        tilIncomeGroupName.visibility = View.GONE
                    }
                    actionsRow.visibility = View.VISIBLE
                    editFooter.visibility = View.GONE
                    deleteConfirmFooter.visibility = View.GONE
                }
                fun handleValidationResult(balanceValid: Boolean?, message: String?) {
                    if (balanceValid == true) {
                        tilAccountName.error = null
                        tilIncomeGroupName.error = null
                        btnSave.isEnabled = true
                    } else {
                        when (transaction.type) {
                            "expense" -> {
                                if (message?.contains("account") == true) {
                                    tilAccountName.error = message
                                } else {
                                    tilIncomeGroupName.error = message
                                }
                            }
                            "transfer" -> {
                                tilAccountName.error = message
                            }
                        }
                        btnSave.isEnabled = false
                    }
                }

                fun validateForm(): Boolean {
                    val amountText = etTransactionAmount.text.toString().trim()
                    val descriptionText = etTransactionDescription.text.toString().trim()
                    val accountText = actvAccountName.text.toString().trim()
                    var isValid = true

                    if (amountText.isEmpty()) {
                        tilTransactionAmount.error = "Amount cannot be empty"
                        isValid = false
                    } else if (amountText.toDoubleOrNull() == null || amountText.toDouble() <= 0) {
                        tilTransactionAmount.error = "Enter a valid number"
                        isValid = false
                    } else {
                        tilTransactionAmount.error = null
                    }
                    if (descriptionText.isEmpty()) {
                        tilTransactionDescription.error = "Description cannot be empty"
                        isValid = false
                    } else {
                        tilTransactionDescription.error = null
                    }
                    if (accountText.isEmpty() || !viewModel.accounts.value.orEmpty().any { it.name == accountText }) {
                        tilAccountName.error = "Select a valid account"
                        isValid = false
                    } else {
                        tilAccountName.error = null
                    }
                    if (transaction.type == "transfer") {
                        val toAccountText = actvAccountToName.text.toString().trim()
                        if (toAccountText.isEmpty() || !viewModel.accounts.value.orEmpty().any { it.name == toAccountText }) {
                            tilAccountToName.error = "Select a valid account"
                            isValid = false
                        } else if ((selectedAccount != null && selectedAccountTo != null) && (selectedAccount!!.id == selectedAccountTo!!.id)) {
                            tilAccountToName.error = "Cannot transfer to the same account"
                            isValid = false
                        } else {
                            tilAccountToName.error = null
                        }
                    }
                    if (transaction.type == "expense") {
                        val groupText = actvIncomeGroupName.text.toString().trim()
                        if (groupText.isNotEmpty() && !viewModel.incomeGroups.value.orEmpty()
                                .any { it.name == groupText }
                        ) {
                            tilIncomeGroupName.error = "Select a valid income group"
                            isValid = false
                        } else {
                            tilIncomeGroupName.error = null
                        }
                    }

                    if (isValid && (transaction.type == "expense" || transaction.type == "transfer")) {
                        val amount = amountText.toDouble()
                        val accountId = viewModel.accounts.value?.find { it.name == accountText }?.id ?: return false
                        val accountToId = if (transaction.type == "transfer") {
                            val toAccountText = actvAccountToName.text.toString().trim()
                            viewModel.accounts.value?.find { it.name == toAccountText }?.id
                        } else null
                        val incomeGroupId = if (transaction.type == "expense") {
                            val groupText = actvIncomeGroupName.text.toString().trim()
                            if (groupText.isNotEmpty()) {
                                viewModel.incomeGroups.value?.find { it.name == groupText }?.id
                            } else null
                        } else null
                        lifecycleScope.launch {
                            viewModel.validateTransactionBalance(
                                transactionId = transaction.id,
                                type = transaction.type,
                                accountId = accountId,
                                amount = amount,
                                accountToId = accountToId,
                                incomeGroupId = incomeGroupId
                            ) { balanceValid, message ->
                                handleValidationResult(balanceValid, message)
                            }
                        }
                    } else {
                        btnSave.isEnabled = isValid
                    }
                    return isValid
                }
                etTransactionAmount.addTextChangedListener { validateForm() }
                etTransactionDescription.addTextChangedListener { validateForm() }
                actvAccountName.setOnItemClickListener { _, _, position, _ ->
                    val selectedName = actvAccountName.adapter.getItem(position) as String
                    selectedAccount =
                        viewModel.accounts.value.orEmpty().find { it.name == selectedName }
                    validateForm()
                }
                actvAccountToName.setOnItemClickListener { _, _, position, _ ->
                    val selectedName = actvAccountToName.adapter.getItem(position) as String
                    selectedAccountTo =
                        viewModel.accounts.value.orEmpty().find { it.name == selectedName }
                    validateForm()
                }
                actvIncomeGroupName.setOnItemClickListener { _, _, position, _ ->
                    val selectedName = actvIncomeGroupName.adapter.getItem(position) as String
                    selectedIncomeGroup =
                        viewModel.incomeGroups.value.orEmpty().find { it.name == selectedName }
                    validateForm()
                }

                fun editTransaction() {
                    if (!validateForm()) return

                    val newAmount = etTransactionAmount.text.toString().trim().toDouble()
                    val newDescription = etTransactionDescription.text.toString().trim()
                    val selectedAccountName = actvAccountName.text.toString().trim()
                    val newAccountId = viewModel.accounts.value?.find { it.name == selectedAccountName }?.id ?: return
                    val newAccountToId = if (transaction.type == "transfer") {
                        val selectedToAccountName = actvAccountToName.text.toString().trim()
                        viewModel.accounts.value?.find { it.name == selectedToAccountName }?.id
                    } else null
                    val newIncomeGroupId = if (transaction.type == "expense") {
                        val selectedGroupName = actvIncomeGroupName.text.toString().trim()
                        viewModel.incomeGroups.value?.find { it.name == selectedGroupName }?.id
                    } else null
                    if (transaction.type == "income") {
                        viewModel.updateTransaction(
                            Transaction(
                                id = transaction.id,
                                amount = newAmount,
                                type = transaction.type,
                                date = transaction.date,
                                description = newDescription,
                                accountId = newAccountId,
                                transferAccountId = newAccountToId,
                                relatedIncomeId = newIncomeGroupId
                            )
                        )
                        resetView()
                        bottomSheetDialog.dismiss()
                        return
                    }
                    lifecycleScope.launch {
                        viewModel.validateTransactionBalance(
                            transactionId = transaction.id,
                            type = transaction.type,
                            accountId = newAccountId,
                            amount = newAmount,
                            accountToId = newAccountToId,
                            incomeGroupId = newIncomeGroupId
                        ) { balanceValid, message ->
                            if (balanceValid == true) {
                                viewModel.updateTransaction(
                                    Transaction(
                                        id = transaction.id,
                                        amount = newAmount,
                                        type = transaction.type,
                                        date = transaction.date,
                                        description = newDescription,
                                        accountId = newAccountId,
                                        transferAccountId = newAccountToId,
                                        relatedIncomeId = newIncomeGroupId
                                    )
                                )
                                resetView()
                                bottomSheetDialog.dismiss()
                            }
                        }
                    }
                }
                fun deleteTransaction() {
                    viewModel.deleteTransaction(
                        Transaction(
                            id = transaction.id,
                            amount = transaction.amount,
                            type = transaction.type,
                            date = transaction.date,
                            description = transaction.description,
                            accountId = transaction.id,
                            transferAccountId = transaction.id,
                            relatedIncomeId = transaction.id
                        )
                    )
                    bottomSheetDialog.dismiss()
                }
                if (transaction.type == "transfer" && transaction.accountToName != null) layoutTransferTo.visibility =
                    View.VISIBLE
                tvAccountToName.text = transaction.accountToName ?: "N/A"
                if (transaction.type == "expense" && transaction.incomeGroupName != null) layoutIncomeGroup.visibility =
                    View.VISIBLE
                tvIncomeGroupName.text = transaction.incomeGroupName ?: "N/A"
                tvTransactionDate.text = dateFormat.format(transaction.date)

                btnEdit.setOnClickListener {
                    tvTransactionAmount.visibility = View.GONE
                    tilTransactionAmount.visibility = View.VISIBLE
                    tvTransactionDescription.visibility = View.GONE
                    tilTransactionDescription.visibility = View.VISIBLE
                    tvAccountName.visibility = View.GONE
                    tilAccountName.visibility = View.VISIBLE
                    if (transaction.type == "transfer" && transaction.accountToName != null) {
                        tvAccountToName.visibility = View.GONE
                        tilAccountToName.visibility = View.VISIBLE
                    }
                    if (transaction.type == "expense" && transaction.incomeGroupName != null) {
                        tvIncomeGroupName.visibility = View.GONE
                        tilIncomeGroupName.visibility = View.VISIBLE
                    }
                    actionsRow.visibility = View.GONE
                    editFooter.visibility = View.VISIBLE
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
                    deleteTransaction()
                }
                btnSave.setOnClickListener {
                    editTransaction()
                }
            }
            bottomSheetDialog.setContentView(binding.root)
            bottomSheetDialog.show()
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