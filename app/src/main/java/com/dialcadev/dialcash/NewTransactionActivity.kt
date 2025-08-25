package com.dialcadev.dialcash

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dialcadev.dialcash.data.AppRepository
import com.dialcadev.dialcash.data.db.AppDB
import com.dialcadev.dialcash.data.dto.AccountBalance
import com.dialcadev.dialcash.data.entities.Account
import com.dialcadev.dialcash.data.entities.IncomeGroup
import com.dialcadev.dialcash.databinding.NewTransactionActivityBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar

class NewTransactionActivity : AppCompatActivity() {
    private lateinit var binding: NewTransactionActivityBinding
    private lateinit var repository: AppRepository
    private lateinit var transactionType: String

    private var selectedDate: Long? = null
    private var selectedAccountFrom: AccountBalance? = null
    private var selectedAccountTo: AccountBalance? = null
    private var selectedIncomeGroup: IncomeGroup? = null

    private var accountsList: List<AccountBalance> = emptyList()
    private var incomeGroupsList: List<IncomeGroup> = emptyList()

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = NewTransactionActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AppRepository(AppDB.getInstance(this))
        transactionType = intent.getStringExtra("transaction_type") ?: "expense"

        setupUI()
        setupListeners()
        loadData()
    }
    private fun setupUI() {
        when (transactionType) {
            "income" -> {
                binding.tvTransactionTitle.text = "New Income"
                binding.tilAccountFrom.hint = "Account"
            }
            "expense" -> {
                binding.tvTransactionTitle.text = "New Expense"
                binding.tilAccountFrom.hint = "Account"
                binding.tilIncomeGroup.visibility = View.VISIBLE
            }
            "transfer" -> {
                binding.tvTransactionTitle.text = "New Transfer"
                binding.tilAccountFrom.hint = "From Account"
                binding.tilAccountTo.visibility = View.VISIBLE
            }
        }
    }
    private fun setupListeners() {
        binding.etDate.setOnClickListener { showDatePicker() }

        // Usar setOnItemClickListener para AutoCompleteTextView
        binding.dropdownAccountFrom.setOnItemClickListener { _, _, position, _ ->
            val selectedName = binding.dropdownAccountFrom.adapter.getItem(position) as String
            selectedAccountFrom = accountsList.find { it.name == selectedName }
        }

        binding.dropdownAccountTo.setOnItemClickListener { _, _, position, _ ->
            val selectedName = binding.dropdownAccountTo.adapter.getItem(position) as String
            selectedAccountTo = accountsList.find { it.name == selectedName }
        }

        binding.dropdownIncomeGroup.setOnItemClickListener { _, _, position, _ ->
            val selectedName = binding.dropdownIncomeGroup.adapter.getItem(position) as String
            selectedIncomeGroup = incomeGroupsList.find { it.name == selectedName }
        }

        binding.btnSave.setOnClickListener { saveTransaction() }
        binding.btnCancel.setOnClickListener { finish() }
    }
    private fun loadData() {
        lifecycleScope.launch {
            repository.getAllAccountBalances().collect { accounts ->
                accountsList = accounts
                setupAccountDropdowns(accounts)
            }
        }
        if (transactionType == "expense") {
            lifecycleScope.launch {
                repository.getAllIncomeGroups().collect { incomeGroupsRemaining ->
                    incomeGroupsList = incomeGroupsRemaining.map {
                        IncomeGroup(
                            id = it.id,
                            name = it.name,
                            amount = it.amount
                        )
                    }
                    setupIncomeGroupDropdown(incomeGroupsList)
                }
            }
        }
    }
    private fun setupAccountDropdowns(accounts: List<AccountBalance>) {
        val accountNames = accounts.map { it.name }
        val fromAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, accountNames)
        binding.dropdownAccountFrom.setAdapter(fromAdapter)
        if (transactionType == "transfer") {
            val toAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, accountNames)
            binding.dropdownAccountTo.setAdapter(toAdapter)
        }
    }
    private fun setupIncomeGroupDropdown(incomeGroups: List<IncomeGroup>) {
        val incomesNames = incomeGroups.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, incomesNames)
        binding.dropdownIncomeGroup.setAdapter(adapter)
    }
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(this, {_, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            selectedDate = calendar.timeInMillis
            binding.etDate.setText(dateFormat.format(calendar.time))
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        datePickerDialog.show()
    }
    private fun saveTransaction() {
        val amountText = binding.etAmount.text?.toString()?.trim()
        val description = binding.etDescription.text?.toString()?.trim()
        if (description.isNullOrEmpty()) {
            binding.tilDescription.error = "Description is required"
            return
        }
        if (amountText.isNullOrEmpty()) {
            binding.tilAmount.error = "Amount is required"
            return
        }
        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            binding.tilAmount.error = "Enter a valid amount"
            return
        }
        if (selectedAccountFrom == null) {
            Toast.makeText(this, "Please select an account", Toast.LENGTH_SHORT).show()
            return
        }
        if (transactionType == "transfer") {
            if (selectedAccountTo == null) {
                Toast.makeText(this, "Please select destination account", Toast.LENGTH_SHORT).show()
                return
            }

            if (selectedAccountFrom?.id == selectedAccountTo?.id) {
                Toast.makeText(this, "Source and destination accounts must be different", Toast.LENGTH_SHORT).show()
                return
            }
        }
        if (transactionType == "expense" || transactionType == "transfer") {
            if (selectedAccountFrom!!.balance < amount) {
                Toast.makeText(this, "Insufficient funds. Available: ${selectedAccountFrom!!.balance}", Toast.LENGTH_SHORT).show()
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
                            fromAccount = Account(
                                id = selectedAccountFrom!!.id,
                                name = selectedAccountFrom!!.name,
                                type = selectedAccountFrom!!.type,
                                balance = selectedAccountFrom!!.balance
                            ),
                            toAccount = Account(
                                id = selectedAccountTo!!.id,
                                name = selectedAccountTo!!.name,
                                type = selectedAccountTo!!.type,
                                balance = selectedAccountTo!!.balance
                            ),
                            amount = amount,
                            description = description,
                            date = finalDate
                        )
                    }
                }
                Toast.makeText(this@NewTransactionActivity, "Transaction saved successfully", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@NewTransactionActivity, "Error saving transaction: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}