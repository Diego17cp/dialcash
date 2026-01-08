package com.dialcadev.dialcash.ui.transactions

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
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
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.dialcadev.dialcash.data.UserData
import com.dialcadev.dialcash.data.UserDataStore
import com.dialcadev.dialcash.ui.shared.BottomSheetManager
import com.dialcadev.dialcash.utils.toReadableDate
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TransactionsFragment : Fragment() {
    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TransactionsViewModel by viewModels()
    private lateinit var transactionsAdapter: TransactionsAdapter
    private var selectedAccountsChips: List<String> = emptyList()

    @Inject
    lateinit var userDataStore: UserDataStore
    var userData: UserData? = null

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
                menuInflater.inflate(R.menu.chart_menu, menu)
                menuInflater.inflate(R.menu.filters_menu, menu)
            }
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_filters -> {
                        showFiltersBottomSheet()
                        true
                    }
                    R.id.action_chart -> {
                        val intent = Intent(requireContext(), ChartsActivity::class.java)
                        startActivity(intent)
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

        val cardIncome = bottomSheetView.findViewById<MaterialCardView>(R.id.incomeSelectorCard)
        val cardExpense = bottomSheetView.findViewById<MaterialCardView>(R.id.expenseSelectorCard)
        val cardTransfer = bottomSheetView.findViewById<MaterialCardView>(R.id.transferSelectorCard)

        val checkIncome = bottomSheetView.findViewById<ImageView>(R.id.checkIncomeSelector)
        val checkExpense = bottomSheetView.findViewById<ImageView>(R.id.checkExpenseSelector)
        val checkTransfer = bottomSheetView.findViewById<ImageView>(R.id.checkTransferSelector)

        val titleIncome = bottomSheetView.findViewById<TextView>(R.id.titleIncomeSelector)
        val titleExpense = bottomSheetView.findViewById<TextView>(R.id.titleExpenseSelector)
        val titleTransfer = bottomSheetView.findViewById<TextView>(R.id.titleTransferSelector)

        val currentTypes = viewModel.getTypesFilter()
        val selectedTypes = mutableSetOf<String>().apply { addAll(currentTypes) }

        fun updateCardState(card: MaterialCardView, check: ImageView, title: TextView, isSelected: Boolean) {
            if (isSelected) {
                val primaryColor = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
                val transparentBg = ColorUtils.setAlphaComponent(primaryColor, (0.10f * 255).toInt())
                card.setCardBackgroundColor(transparentBg)
                card.strokeColor = primaryColor
                card.strokeWidth = 5
                check.visibility = View.VISIBLE
                title.setTextColor(primaryColor)
                check.scaleX = 0f
                check.scaleY = 0f
                check.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(140)
                    .setInterpolator(OvershootInterpolator())
                    .start()
            } else {
                card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background))
                val typedValue = TypedValue()
                requireContext().theme.resolveAttribute(
                    com.google.android.material.R.attr.colorOutline,
                    typedValue,
                    true
                )
                val colorOutline = typedValue.data
                card.strokeColor = colorOutline
                card.strokeWidth = 4
                check.visibility = View.GONE
                title.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            }
        }
        fun updateTypesFilter() {
            viewModel.setTypesFilter(selectedTypes.toList())
        }
        cardIncome.setOnClickListener {
            val isSelected = selectedTypes.contains("INCOME")
            if (isSelected) selectedTypes.remove("INCOME")
            else selectedTypes.add("INCOME")
            updateCardState(cardIncome, checkIncome, titleIncome, !isSelected)
            updateTypesFilter()
        }
        cardExpense.setOnClickListener {
            val isSelected = selectedTypes.contains("EXPENSE")
            if (isSelected) selectedTypes.remove("EXPENSE")
            else selectedTypes.add("EXPENSE")
            updateCardState(cardExpense, checkExpense, titleExpense, !isSelected)
            updateTypesFilter()
        }
        cardTransfer.setOnClickListener {
            val isSelected = selectedTypes.contains("TRANSFER")
            if (isSelected) selectedTypes.remove("TRANSFER")
            else selectedTypes.add("TRANSFER")
            updateCardState(cardTransfer, checkTransfer, titleTransfer, !isSelected)
            updateTypesFilter()
        }

        val startDateCard = bottomSheetView.findViewById<MaterialCardView>(R.id.startDateCard)
        val endDateCard = bottomSheetView.findViewById<MaterialCardView>(R.id.endDateCard)
        val etStartDate = bottomSheetView.findViewById<TextInputEditText>(R.id.et_start_date)
        val etEndDate = bottomSheetView.findViewById<TextInputEditText>(R.id.et_end_date)
        val tvStartDateValue = bottomSheetView.findViewById<TextView>(R.id.tvStartDateValue)
        val tvEndDateValue = bottomSheetView.findViewById<TextView>(R.id.tvEndDateValue)
        val startDateChevron = bottomSheetView.findViewById<ImageView>(R.id.chevronStartDate)
        val endDateChevron = bottomSheetView.findViewById<ImageView>(R.id.chevronEndDate)
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
                        tvStartDateValue.text = formattedDate.toReadableDate()
                        startDateChevron.visibility = View.GONE
                        etStartDate.setText(formattedDate)
                        viewModel.setStartDate(selectedTimestamp)
                    } else {
                        tvEndDateValue.text = formattedDate.toReadableDate()
                        endDateChevron.visibility = View.GONE
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
        startDateCard.setOnClickListener { openDatePicker(true) }
        endDateCard.setOnClickListener { openDatePicker(false) }
        etEndDate.addTextChangedListener { date ->
            if (date.isNullOrEmpty()) {
                endDateChevron.visibility = View.VISIBLE
                tvEndDateValue.text = "DD/MM/YYYY"
            }
            else {
                val parsedDate = date.toString()
                tvEndDateValue.text = parsedDate.toReadableDate()
                endDateChevron.visibility = View.GONE
            }
        }
        etStartDate.addTextChangedListener { date ->
            if (date.isNullOrEmpty()) {
                startDateChevron.visibility = View.VISIBLE
                tvStartDateValue.text = "DD/MM/YYYY"
            }
            else {
                val parsedDate = date.toString()
                tvStartDateValue.text = parsedDate.toReadableDate()
                startDateChevron.visibility = View.GONE
            }
        }
        val chipGroup = bottomSheetView.findViewById<ChipGroup>(R.id.chipGroupAccounts)
        viewModel.accounts.observe(viewLifecycleOwner) { accounts ->
            val currentSelection = try {
                viewModel._accountNames.value ?: selectedAccountsChips
            } catch (e: Exception) {
                selectedAccountsChips
            }
            populateAccountsChips(chipGroup, accounts, currentSelection)
        }
        val btnApplyFilters = bottomSheetView.findViewById<MaterialButton>(R.id.btnApplyFilters)
        btnApplyFilters.setOnClickListener {
            bottomSheetDialog.dismiss()
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
        transactionsAdapter = TransactionsAdapter(
            onTransactionClick = { transaction ->
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
                            userData?.currencySymbol ?: "$",
                            { updated -> viewModel.updateTransaction(updated) },
                            { toDelete -> viewModel.deleteTransaction(toDelete) },
                            { id, type, accountId, amount, accountToId, incomeGroupId, onResult: (Boolean, String?) -> Unit ->
                                viewModel.validateTransactionBalance(
                                    id,
                                    type,
                                    accountId,
                                    amount,
                                    accountToId,
                                    incomeGroupId,
                                    onResult
                                )
                            }
                        )
                    }
                }
            },
            currencySymbol = userData?.currencySymbol ?: "$"
        )

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
            binding.tvEmptyTitle.text =
                if (filtered) getString(R.string.no_results) else getString(R.string.no_transactions_yet)
            binding.tvEmptySubtitle.text =
                if (filtered) getString(R.string.try_adjusting_filters)
                else getString(R.string.add_your_first_transaction)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            userDataStore.getUserData().collect { user ->
                userData = user
                user.currencySymbol.let { symbol ->
                    transactionsAdapter.updateCurrencySymbol(symbol)
                }
            }
        }
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