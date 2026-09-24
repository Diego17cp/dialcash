package com.dialcadev.dialcash.features.transactions.presentation.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.core.theme.AppTypography
import com.dialcadev.dialcash.core.ui.components.LiquidAppBar
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
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import java.util.Calendar


@AndroidEntryPoint
class NewTransactionActivity : AppCompatActivity() {
    private lateinit var binding: NewTransactionActivityBinding
    private val hazeState = HazeState()
    private val viewModel: CreateTransactionViewModel by viewModels()
    private lateinit var transactionType: TransactionType
    private val dateFormat: DateFormat =
        DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = NewTransactionActivityBinding.inflate(layoutInflater)

        val typeString = intent.getStringExtra("transaction_type")
        transactionType = TransactionType.fromString(typeString)
        val appBarTitle = when (transactionType) {
            TransactionType.INCOME -> getString(R.string.new_income)
            TransactionType.EXPENSE -> getString(R.string.new_expense)
            TransactionType.TRANSFER -> getString(R.string.new_transfer)
        }

        val composeView = ComposeView(this).apply {
            setContent {
                MaterialTheme(
                    typography = AppTypography
                ) {
                    Box(Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { binding.root },
                            modifier = Modifier.fillMaxSize().hazeSource(hazeState)
                        )
                        LiquidAppBar(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .onGloballyPositioned {
                                    binding.nestedScrollView.updatePadding(top = it.size.height)
                                },
                            hazeState = hazeState,
                            title = appBarTitle,
                            onBackClick = { onBackPressedDispatcher.onBackPressed() }
                        )
                    }
                }
            }
        }
        setContentView(composeView)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }
        setupUI()
        setupListeners()
        observeViewModel()
    }

    private fun setupUI() {
        when (transactionType) {
            TransactionType.INCOME -> {
                binding.btnSave.text = getString(R.string.register_income)
            }

            TransactionType.EXPENSE -> {
                binding.btnSave.text = getString(R.string.register_expense)
                binding.incomeGroupItem.visibility = View.VISIBLE
                binding.incomeGroupDivider.visibility = View.VISIBLE
            }

            TransactionType.TRANSFER -> {
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
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                viewModel.onDateSelected(calendar.timeInMillis)
                binding.etDate.setText(dateFormat.format(calendar.time))
                binding.tvDateValue.text = calendar.timeInMillis.toReadableDate()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
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