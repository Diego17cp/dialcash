package com.dialcadev.dialcash.features.transactions.presentation.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.core.ui.components.showAccountSelector
import com.dialcadev.dialcash.core.ui.components.showIncomeGroupsSelector
import com.dialcadev.dialcash.core.utils.extensions.toCurrencyFormat
import com.dialcadev.dialcash.core.utils.extensions.toReadableDate
import com.dialcadev.dialcash.databinding.NewTransactionActivityBinding
import com.dialcadev.dialcash.features.transactions.domain.models.TransactionType
import com.dialcadev.dialcash.features.transactions.presentation.viewmodels.CreateTransactionViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.DateFormat
import java.util.Locale
import java.util.TimeZone
import com.google.android.material.R.*
import kotlinx.coroutines.launch
import java.util.Calendar


@AndroidEntryPoint
class NewTransactionActivity : AppCompatActivity() {
    private lateinit var binding: NewTransactionActivityBinding
    private val viewModel: CreateTransactionViewModel by viewModels()
    private lateinit var transactionType: TransactionType
    private val dateFormat: DateFormat =
        DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = NewTransactionActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val typeString = intent.getStringExtra("transaction_type")
        transactionType = TransactionType.fromString(typeString)

        setupToolbar()
        setupUI()
        setupListeners()
        observeViewModel()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(android.R.drawable.ic_menu_close_clear_cancel)
        }
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupUI() {
        when (transactionType) {
            TransactionType.INCOME -> {
                binding.toolbar.title = getString(R.string.new_income)
                binding.btnSave.text = getString(R.string.register_income)
            }

            TransactionType.EXPENSE -> {
                binding.toolbar.title = getString(R.string.new_expense)
                binding.btnSave.text = getString(R.string.register_expense)
                binding.incomeGroupItem.visibility = View.VISIBLE
                binding.incomeGroupDivider.visibility = View.VISIBLE
            }

            TransactionType.TRANSFER -> {
                binding.toolbar.title = getString(R.string.new_transfer)
                binding.btnSave.text = getString(R.string.register_transfer)
                binding.tvFrom.visibility = View.VISIBLE
                binding.toAccountItem.visibility = View.VISIBLE
                binding.toAccountDivider.visibility = View.VISIBLE
            }
        }
    }

    private fun setupListeners() {
        binding.etAmount.addTextChangedListener { text ->
            val value = text?.toString()?.trim() ?: ""
            viewModel.onAmountChanged(value)
            if (value.isEmpty()) {
                binding.tvAmount.text = "0.00"
                binding.tvAmount.setTextColor(getThemeColor(attr.colorOutline))
            } else {
                val formatted = value.toDoubleOrNull()?.toCurrencyFormat() ?: value
                binding.tvAmount.text = formatted
                binding.tvAmount.setTextColor(getThemeColor(attr.colorOnSurface))
            }
        }
        binding.etDescription.addTextChangedListener {
            viewModel.onDescriptionChanged(it?.toString()?.trim() ?: "")
        }
        binding.etDate.setOnClickListener { showDatePicker() }
        binding.dateItem.setOnClickListener { showDatePicker() }
        binding.tvAmount.setOnClickListener {
            binding.etAmount.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.etAmount, InputMethodManager.SHOW_IMPLICIT)
        }
        binding.fromAccountItem.setOnClickListener {
            showAccountSelector(
                accountsList = viewModel.uiState.value.accounts,
                currencySymbol = viewModel.uiState.value.currencySymbol
            ) { selected ->
                viewModel.onAccountFromSelected(selected)
            }
        }
        binding.toAccountItem.setOnClickListener {
            showAccountSelector(
                accountsList = viewModel.uiState.value.accounts,
                currencySymbol = viewModel.uiState.value.currencySymbol
            ) { selected ->
                viewModel.onAccountToSelected(selected)
            }
        }
        binding.incomeGroupItem.setOnClickListener {
            showIncomeGroupsSelector(
                incomeGroupsList = viewModel.uiState.value.incomeGroups,
                currencySymbol = viewModel.uiState.value.currencySymbol,
                onClearBtnClicked = {
                    viewModel.onIncomeGroupSelected(null)
                    binding.tvIncomeGroup.text = getString(R.string.hint_ic_g_optional)
                    binding.tvIncomeGroupRemaining.text = getString(R.string.balance)
                },
                onIncomeGroupSelected = { selected ->
                    viewModel.onIncomeGroupSelected(selected)
                }
            )
        }
        binding.btnSave.setOnClickListener {
            viewModel.saveTransaction(transactionType)
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                viewModel.onDateSelected(calendar.timeInMillis)
                binding.etDate.setText(dateFormat.format(calendar.time))
                binding.tvDateValue.text = calendar.timeInMillis.toString().toReadableDate()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val currency = state.currencySymbol
                    binding.tvAmountError.apply {
                        visibility = if (state.amountError != null) View.VISIBLE else View.GONE
                        if (state.amountError != null) text = getString(state.amountError)
                    }
                    binding.tvDescriptionError.apply {
                        visibility = if (state.descriptionError != null) View.VISIBLE else View.GONE
                        if (state.descriptionError != null) text = getString(state.descriptionError)
                    }
                    state.selectedAccountFrom?.let { account ->
                        binding.tvAccountFrom.text = account.name
                        binding.tvFromAccountBalance.text =
                            "${getString(R.string.balance)}: $currency ${account.balance.toCurrencyFormat()}"
                    }
                    state.selectedAccountTo?.let { account ->
                        binding.tvAccountTo.text = account.name
                        binding.tvToAccountBalance.text =
                            "${getString(R.string.balance)}: $currency ${account.balance.toCurrencyFormat()}"
                    }
                    state.selectedIncomeGroup?.let { group ->
                        binding.tvIncomeGroup.text = group.name
                        binding.tvIncomeGroupRemaining.text =
                            "${getString(R.string.remaining)}: $currency ${group.remaining.toCurrencyFormat()}"
                    }
                    binding.btnSave.isEnabled = !state.isLoading
                    if (state.isSuccess) {
                        Toast.makeText(
                            this@NewTransactionActivity,
                            getString(R.string.transaction_created_successfully),
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                    if (state.errorMessage != null) {
                        Toast.makeText(
                            this@NewTransactionActivity,
                            state.errorMessage,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun getThemeColor(attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }
}