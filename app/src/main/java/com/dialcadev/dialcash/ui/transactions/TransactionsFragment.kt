package com.dialcadev.dialcash.ui.transactions

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.MenuProvider
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
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.dialcadev.dialcash.ui.shared.BottomSheetManager

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

    private fun populateAccountsChips(
        chipGroup: ChipGroup,
        accounts: List<Account>,
        selectedAccounts: List<String>
    ) {
        chipGroup.removeAllViews()
        val allChip = Chip(requireContext()).apply {
            text = requireContext().getString(R.string.all)
            isCheckable = true
            isChecked = selectedAccounts.isEmpty()
            id = View.generateViewId()
            setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_text_color))
            chipBackgroundColor =
                ContextCompat.getColorStateList(requireContext(), R.color.chip_background)
        }
        chipGroup.addView(allChip)
        accounts.forEach { account ->
            val chip = Chip(requireContext()).apply {
                text = account.name
                isCheckable = true
                isChecked = selectedAccounts.contains(account.name)
                tag = account.name
                id = View.generateViewId()
                setTextColor(
                    ContextCompat.getColorStateList(
                        requireContext(),
                        R.color.chip_text_color
                    )
                )
                chipBackgroundColor =
                    ContextCompat.getColorStateList(requireContext(), R.color.chip_background)
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
        val manager = BottomSheetManager(requireContext(), layoutInflater)
        transactionsAdapter = TransactionsAdapter { transaction ->
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
            binding.tvEmptyTitle.text = if (filtered) getString(R.string.no_results) else getString(R.string.no_transactions_yet)
            binding.tvEmptySubtitle.text =
                if (filtered) getString(R.string.try_adjusting_filters)
                else getString(R.string.add_your_first_transaction)
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
                    .setAction(getString(R.string.retry)) { viewModel.refreshTransactions() }
                    .show()
                viewModel.clearError()
            }
        }
        viewModel.isFiltered.observe(viewLifecycleOwner) { filtered ->
            binding.btnClearFilters.visibility = if (filtered) View.VISIBLE else View.GONE
            if (binding.layoutNoTransactions.isVisible) {
                binding.tvEmptyTitle.text =
                    if (filtered) getString(R.string.no_results) else getString(R.string.no_transactions_yet)
                binding.tvEmptySubtitle.text =
                    if (filtered) getString(R.string.try_adjusting_filters)
                    else getString(R.string.add_your_first_transaction)
            }
        }
        try {
            viewModel._accountNames.observe(viewLifecycleOwner) { selected ->
                selectedAccountsChips = selected ?: emptyList()
            }
        } catch (_: Exception) {
        }
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