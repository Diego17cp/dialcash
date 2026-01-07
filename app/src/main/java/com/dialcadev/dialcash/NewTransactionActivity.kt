package com.dialcadev.dialcash

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dialcadev.dialcash.data.AppRepository
import com.dialcadev.dialcash.data.UserData
import com.dialcadev.dialcash.data.UserDataStore
import com.dialcadev.dialcash.data.dao.AccountBalanceWithOriginal
import com.dialcadev.dialcash.data.db.AppDB
import com.dialcadev.dialcash.data.dto.IncomeGroupRemaining
import com.dialcadev.dialcash.databinding.NewTransactionActivityBinding
import com.dialcadev.dialcash.ui.accounts.SelectorAccountAdapter
import com.dialcadev.dialcash.ui.incomegroup.SelectorIncomeGroupAdapter
import com.dialcadev.dialcash.utils.toReadableDate
import com.google.android.material.R.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class NewTransactionActivity : AppCompatActivity() {
    private lateinit var binding: NewTransactionActivityBinding
    private lateinit var repository: AppRepository

    @Inject
    lateinit var userDataStore: UserDataStore
    var userData: UserData? = null
    private lateinit var transactionType: String

    private var selectedDate: Long? = null
    private var selectedAccountFrom: AccountBalanceWithOriginal? = null
    private var selectedAccountTo: AccountBalanceWithOriginal? = null
    private var selectedIncomeGroup: IncomeGroupRemaining? = null

    private var accountsList: List<AccountBalanceWithOriginal> = emptyList()
    private var incomeGroupsList: List<IncomeGroupRemaining> = emptyList()

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = NewTransactionActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(android.R.drawable.ic_menu_close_clear_cancel)
        }

        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        repository = AppRepository(AppDB.getInstance(this))
        transactionType = intent.getStringExtra("transaction_type") ?: "expense"

        setupUI()
        setupListeners()
        loadData()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showKeyboard(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }
    private fun getThemeColor(attr: Int): Int {
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }
    private fun setupUI() {
        when (transactionType) {
            "income" -> {
                binding.toolbar.title = getString(R.string.new_income)
                binding.btnSave.text = getString(R.string.register_income)
            }

            "expense" -> {
                binding.toolbar.title = getString(R.string.new_expense)
                binding.btnSave.text = getString(R.string.register_expense)
                binding.incomeGroupItem.visibility = View.VISIBLE
                binding.incomeGroupDivider.visibility = View.VISIBLE
            }

            "transfer" -> {
                binding.toolbar.title = getString(R.string.new_transfer)
                binding.btnSave.text = getString(R.string.register_transfer)
                binding.tvFrom.visibility = View.VISIBLE
                binding.toAccountItem.visibility = View.VISIBLE
                binding.toAccountDivider.visibility = View.VISIBLE
            }
        }
    }

    private fun setupListeners() {
        binding.etDate.setOnClickListener { showDatePicker() }
        binding.dateItem.setOnClickListener { showDatePicker() }

        binding.tvAmount.setOnClickListener {
            binding.etAmount.requestFocus()
            showKeyboard(binding.etAmount)
        }

        binding.toAccountItem.setOnClickListener {
            showAccountSelectionBottomSheet(isFrom = false)
        }
        binding.fromAccountItem.setOnClickListener {
            showAccountSelectionBottomSheet(isFrom = true)
        }
        binding.incomeGroupItem.setOnClickListener {
            showIncomeGroupSelectionBottomSheet()
        }

        binding.etAmount.addTextChangedListener { text ->
            val value = text?.toString() ?: ""
            if (value.isEmpty()) {
                binding.tvAmount.text = "0.00"
                binding.tvAmount.setTextColor(getThemeColor(attr.colorOutline))
            } else {
                val formattedValue = try {
                    val number = value.toDouble()
                    String.format("%.2f", number)
                } catch (e: NumberFormatException) {
                    value
                }
                binding.tvAmount.text = formattedValue
                binding.tvAmount.setTextColor(getThemeColor(attr.colorOnSurface))
                binding.tvAmountError.visibility = View.GONE
            }
        }

        binding.etDate.addTextChangedListener { date ->
            if (date.isNullOrEmpty()) binding.tvDateValue.text = getString(R.string.today)
            else {
                val parsedDate = date.toString()
                binding.tvDateValue.text = parsedDate.toReadableDate()
            }
        }

        binding.btnSave.setOnClickListener { saveTransaction() }
    }

    private fun loadData() {
        lifecycleScope.launch {
            userDataStore.getUserData().collect { user ->
                userData = user
                binding.tvCurrency.text = user.currencySymbol
            }
        }
        lifecycleScope.launch {
            repository.getAllAccountBalances().collect { accounts ->
                accountsList = accounts
            }
        }
        if (transactionType == "expense") {
            lifecycleScope.launch {
                repository.getAllIncomeGroupsWithRemaining().collect { incomeGroupsRemaining ->
                    incomeGroupsList = incomeGroupsRemaining
                }
            }
        }
    }
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedDate = calendar.timeInMillis
                binding.etDate.setText(dateFormat.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun showAccountSelectionBottomSheet(isFrom: Boolean) {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this)
            .inflate(R.layout.accounts_selector_bottomsheet, null)
        val rv = view.findViewById<RecyclerView>(R.id.rvAccounts)
        rv.layoutManager = LinearLayoutManager(this)
        val adapter = SelectorAccountAdapter(
            onClick = { selected ->
                if (isFrom) {
                    selectedAccountFrom = selected
                    binding.tvAccountFrom.text = selected.name
                    val formattedBalance = "${userData?.currencySymbol ?: "$"} ${"%.2f".format(selected.balance)}"
                    binding.tvFromAccountBalance.text = "${getString(R.string.balance)}: $formattedBalance"
                } else {
                    selectedAccountTo = selected
                    binding.tvAccountTo.text = selected.name
                    val formattedBalance = "${userData?.currencySymbol ?: "$"} ${"%.2f".format(selected.balance)}"
                    binding.tvToAccountBalance.text = "${getString(R.string.balance)}: $formattedBalance"
                }
                dialog.dismiss()
            },
            currencySymbol = userData?.currencySymbol ?: "$"
        )
        rv.adapter = adapter
        adapter.submitList(accountsList)
        dialog.setContentView(view)
        dialog.show()
    }
    private fun showIncomeGroupSelectionBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this)
            .inflate(R.layout.income_groups_selector_bottomsheet, null)
        val clearBtn = view.findViewById<View>(R.id.btnClearSelection)
        clearBtn.setOnClickListener {
            selectedIncomeGroup = null
            binding.tvIncomeGroup.text = getString(R.string.hint_ic_g_optional)
            binding.tvIncomeGroupRemaining.text = getString(R.string.balance)
            dialog.dismiss()
        }
        val rv = view.findViewById<RecyclerView>(R.id.rvIncomeGroups)
        rv.layoutManager = LinearLayoutManager(this)
        val adapter = SelectorIncomeGroupAdapter(
            onClick = { selected ->
                selectedIncomeGroup = selected
                binding.tvIncomeGroup.text = selected.name
                val formattedBalance = "${userData?.currencySymbol ?: "$"} ${"%.2f".format(selected.remaining)}"
                binding.tvIncomeGroupRemaining.text = "${getString(R.string.remaining)}: $formattedBalance"
                dialog.dismiss()
            },
            currencySymbol = userData?.currencySymbol ?: "$"
        )
        rv.adapter = adapter
        adapter.submitList(incomeGroupsList)
        dialog.setContentView(view)
        dialog.show()
    }

    private fun saveTransaction() {
        val amountText = binding.etAmount.text?.toString()?.trim()
        if (amountText.isNullOrEmpty() || amountText == "0.00" || amountText.toDoubleOrNull() == null) {
            binding.tvAmountError.visibility = View.VISIBLE
            binding.tvAmountError.text = getString(R.string.amount_required)
            return
        }
        val amount = amountText.toDouble()
        if (amount <= 0) {
            binding.tvAmountError.visibility = View.VISIBLE
            binding.tvAmountError.text = getString(R.string.enter_valid_amount)
            return
        }
        val description = binding.etDescription.text?.toString()?.trim()
        if (description.isNullOrEmpty()) {
            binding.tvDescriptionError.visibility = View.VISIBLE
            binding.tvDescriptionError.text = getString(R.string.description_required)
            return
        }

        if (selectedAccountFrom == null) {
            Toast.makeText(this, getString(R.string.please_select_acc), Toast.LENGTH_SHORT).show()
            return
        }
        if (transactionType == "expense" && selectedIncomeGroup != null) {
            if (selectedIncomeGroup!!.remaining < amount) {
                Toast.makeText(
                    this,
                    "${getString(R.string.insuficient_funds_income_group_available)}: ${selectedIncomeGroup!!.remaining}, Required: $amount",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        }
        if (transactionType == "transfer") {
            if (selectedAccountTo == null) {
                Toast.makeText(this, getString(R.string.please_select_to_acc), Toast.LENGTH_SHORT)
                    .show()
                return
            }
            if (selectedAccountFrom?.id == selectedAccountTo?.id) {
                Toast.makeText(
                    this,
                    getString(R.string.source_and_dest_acc_must_be_different),
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        }
        if (transactionType == "expense" || transactionType == "transfer") {
            if (selectedAccountFrom!!.balance < amount) {
                Toast.makeText(
                    this,
                    "${getString(R.string.insuficient_funds_account_available)}: ${selectedAccountFrom!!.balance}",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        }
        val finalDate = selectedDate ?: System.currentTimeMillis()
        lifecycleScope.launch {
            try {
                when (transactionType) {
                    "income" -> {
                        repository.addIncome(
                            accountId = selectedAccountFrom!!.id,
                            amount = amount,
                            description = description,
                            relatedIncomeId = selectedIncomeGroup?.id,
                            date = finalDate
                        )
                    }

                    "expense" -> {
                        repository.addExpense(
                            accountId = selectedAccountFrom!!.id,
                            amount = amount,
                            description = description,
                            relatedIncomeId = selectedIncomeGroup?.id,
                            date = finalDate
                        )
                    }

                    "transfer" -> {
                        repository.makeTransfer(
                            fromAccountId = selectedAccountFrom!!.id,
                            toAccountId = selectedAccountTo!!.id,
                            amount = amount,
                            description = description,
                            date = finalDate
                        )
                    }
                }
                Toast.makeText(
                    this@NewTransactionActivity,
                    getString(R.string.transaction_created_successfully),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            } catch (e: Exception) {
                Log.d("NewTransactionActivity", "saveTransaction: ${e.message}")
            }
        }
    }
}