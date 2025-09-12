package com.dialcadev.dialcash.ui.transactions

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.MenuProvider
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.data.entities.Account
import com.dialcadev.dialcash.databinding.FragmentTransactionsBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.dialcadev.dialcash.data.entities.IncomeGroup
import com.dialcadev.dialcash.data.entities.Transaction
import com.dialcadev.dialcash.databinding.RecycleTransactionItemBinding
import kotlinx.coroutines.launch
import kotlin.collections.any
import kotlin.collections.find
import kotlin.collections.orEmpty

@AndroidEntryPoint
class TransactionsFragment : Fragment() {
    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TransactionsViewModel by viewModels()
    private lateinit var transactionsAdapter: TransactionsAdapter
    private var selectedAccountsChips: List<String> = emptyList()

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.filters_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_filters -> {
                        showFiltersBottomSheet()
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
        setupUI()
        setupRecyclerView()
        observeViewModel()
        setupSwipeToRefresh()
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshTransactions()
        }
    }

    private fun showFiltersBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val bottomSheetView = LayoutInflater.from(requireContext())
            .inflate(R.layout.bottom_sheet_filters, null)

        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()

        val etSearch = bottomSheetView.findViewById<TextInputEditText>(R.id.etSearch)
        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                val query = etSearch.text.toString()
                viewModel.setSearchQuery(query)
                bottomSheetDialog.dismiss()
                true
            } else false
        }

        val cbIncome = bottomSheetView.findViewById<MaterialCheckBox>(R.id.cbIncome)
        val cbExpense = bottomSheetView.findViewById<MaterialCheckBox>(R.id.cbExpense)
        val cbTransfer = bottomSheetView.findViewById<MaterialCheckBox>(R.id.cbTransfer)
        val cardIncome = bottomSheetView.findViewById<MaterialCardView>(R.id.cardIncome)
        val cardExpense = bottomSheetView.findViewById<MaterialCardView>(R.id.cardExpense)
        val cardTransfer = bottomSheetView.findViewById<MaterialCardView>(R.id.cardTransfer)

        fun updateCardBg(card: MaterialCardView, isChecked: Boolean) {
            if (isChecked) {
                val primaryColor = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
                val transparentBg =
                    ColorUtils.setAlphaComponent(primaryColor, (0.10f * 255).toInt())
                card.setCardBackgroundColor(transparentBg)
                card.strokeColor = primaryColor
            } else {
                card.setCardBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.surface
                    )
                )
                card.strokeColor = ContextCompat.getColor(requireContext(), R.color.surface)
            }
        }
        cardIncome.setOnClickListener {
            cbIncome.isChecked = !cbIncome.isChecked
            updateCardBg(cardIncome, cbIncome.isChecked)
        }
        cardExpense.setOnClickListener {
            cbExpense.isChecked = !cbExpense.isChecked
            updateCardBg(cardExpense, cbExpense.isChecked)
        }
        cardTransfer.setOnClickListener {
            cbTransfer.isChecked = !cbTransfer.isChecked
            updateCardBg(cardTransfer, cbTransfer.isChecked)
        }


        fun updateTypesFilter() {
            val selected = mutableListOf<String>()
            if (cbIncome.isChecked) selected.add("INCOME")
            if (cbExpense.isChecked) selected.add("EXPENSE")
            if (cbTransfer.isChecked) selected.add("TRANSFER")
            viewModel.setTypesFilter(selected)
        }

        cbIncome.setOnCheckedChangeListener { _, _ -> updateTypesFilter() }
        cbExpense.setOnCheckedChangeListener { _, _ -> updateTypesFilter() }
        cbTransfer.setOnCheckedChangeListener { _, _ -> updateTypesFilter() }

        val etStartDate = bottomSheetView.findViewById<TextInputEditText>(R.id.etStartDate)
        val etEndDate = bottomSheetView.findViewById<TextInputEditText>(R.id.etEndDate)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        fun openDatePicker(isStart: Boolean) {
            val calendar = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    if (isStart) {
                        calendar.set(Calendar.HOUR_OF_DAY, 0)
                        calendar.set(Calendar.MINUTE, 0)
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)
                    } else {
                        calendar.set(Calendar.HOUR_OF_DAY, 23)
                        calendar.set(Calendar.MINUTE, 59)
                        calendar.set(Calendar.SECOND, 59)
                        calendar.set(Calendar.MILLISECOND, 999)
                    }
                    val selectedTimestamp = calendar.timeInMillis
                    val formattedDate = dateFormat.format(calendar.time)
                    if (isStart) {
                        etStartDate.setText(formattedDate)
                        viewModel.setStartDate(selectedTimestamp)
                    } else {
                        etEndDate.setText(formattedDate)
                        viewModel.setEndDate(selectedTimestamp)
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }
        etStartDate.setOnClickListener { openDatePicker(true) }
        etEndDate.setOnClickListener { openDatePicker(false) }

        val chipGroup = bottomSheetView.findViewById<ChipGroup>(R.id.chipGroupAccounts)
        viewModel.accounts.observe(viewLifecycleOwner) { accounts ->
            val currentSelection = try {
                viewModel._accountNames.value ?: selectedAccountsChips
            } catch (e: Exception) {
                selectedAccountsChips
            }
            populateAccountsChips(chipGroup, accounts, currentSelection)
        }
    }
    private fun populateAccountsChips(chipGroup: ChipGroup, accounts: List<Account>, selectedAccounts: List<String>) {
        chipGroup.removeAllViews()
        val allChip = Chip(requireContext()).apply {
            text = "All"
            isCheckable = true
            isChecked = selectedAccounts.isEmpty()
            id = View.generateViewId()
            setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_text_color))
            chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), R.color.chip_background)
        }
        chipGroup.addView(allChip)
        accounts.forEach { account ->
            val chip = Chip(requireContext()).apply {
                text = account.name
                isCheckable = true
                isChecked = selectedAccounts.contains(account.name)
                tag = account.name
                id = View.generateViewId()
                setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_text_color))
                chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), R.color.chip_background)
            }
            chipGroup.addView(chip)
        }
        allChip.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                for (i in 0 until chipGroup.childCount) {
                    val child = chipGroup.getChildAt(i)
                    if (child is Chip && child != allChip) child.isChecked = false
                }
                selectedAccountsChips = emptyList()
                viewModel.setAccountFilter(emptyList())
            }
        }
        for (i in 0 until chipGroup.childCount) {
            val child = chipGroup.getChildAt(i)
            if (child is Chip && child != allChip) {
                child.setOnCheckedChangeListener { _, _ ->
                    if (child.isChecked) allChip.isChecked = false

                    val selected = mutableListOf<String>()
                    for (j in 0 until chipGroup.childCount) {
                        val c = chipGroup.getChildAt(j)
                        if (c is Chip && c != allChip && c.isChecked) selected.add(c.tag as String)
                    }

                    if (selected.isEmpty()) {
                        allChip.isChecked = true
                        selectedAccountsChips = emptyList()
                        viewModel.setAccountFilter(emptyList())
                    } else {
                        selectedAccountsChips = selected.toList()
                        viewModel.setAccountFilter(selected)
                    }
                }
            }
        }
    }
    private fun setupRecyclerView() {
        transactionsAdapter = TransactionsAdapter { transaction ->
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
                viewModel.fetchAccounts()
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

    private fun setupUI() {
        binding.btnClearFilters.setOnClickListener {
            viewModel.clearFilters()
            selectedAccountsChips = emptyList()
        }
        viewModel.isFiltered.observe(viewLifecycleOwner) { filtered ->
            binding.btnClearFilters.visibility = if (filtered) View.VISIBLE else View.GONE
        }
    }
    private fun updateEmptyState(transactionsEmpty: Boolean, filtered: Boolean) {
        binding.layoutTransactions.visibility = if (transactionsEmpty) View.GONE else View.VISIBLE
        binding.layoutNoTransactions.visibility = if (transactionsEmpty) View.VISIBLE else View.GONE
        if (transactionsEmpty) {
            binding.tvEmptyTitle.text = if (filtered) "No results found" else "No transactions yet"
            binding.tvEmptySubtitle.text = if (filtered) "Try adjusting your filters or clear them to see all transactions."
            else "Add your first transaction to get started."
        }
    }
    private fun observeViewModel() {
        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            transactionsAdapter.submitList(transactions)
            val filtered = viewModel.isFiltered.value ?: false
            updateEmptyState(transactions.isEmpty(), filtered)
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefreshLayout.isRefreshing = isLoading
            binding.progressBar.visibility =
                if (isLoading && !binding.swipeRefreshLayout.isRefreshing) View.VISIBLE else View.GONE
        }
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG)
                    .setAction("Retry") { viewModel.refreshTransactions() }
                    .show()
                viewModel.clearError()
            }
        }
        viewModel.isFiltered.observe(viewLifecycleOwner) { filtered ->
            binding.btnClearFilters.visibility = if (filtered) View.VISIBLE else View.GONE
            if (binding.layoutNoTransactions.isVisible) {
                binding.tvEmptyTitle.text = if (filtered) "No results found" else "No transactions yet"
                binding.tvEmptySubtitle.text = if (filtered) "Try adjusting your filters or clear them to see all transactions."
                else "Add your first transaction to get started."
            }
        }
        try {
            viewModel._accountNames.observe(viewLifecycleOwner) { selected ->
                selectedAccountsChips = selected ?: emptyList()
            }
        } catch (_: Exception) {}
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshTransactions()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}