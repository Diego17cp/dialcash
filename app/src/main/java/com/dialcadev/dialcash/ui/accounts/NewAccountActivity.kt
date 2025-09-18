package com.dialcadev.dialcash.ui.accounts

import com.dialcadev.dialcash.R
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.dialcadev.dialcash.databinding.NewAccountActivityBinding
import com.dialcadev.dialcash.domain.AccountType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NewAccountActivity : AppCompatActivity() {
    private lateinit var binding: NewAccountActivityBinding
    private val viewModel: NewAccountViewModel by viewModels()

    private val accountTypes = AccountType.entries.toList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = NewAccountActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        setupListeners()
        observeViewModel()
    }
    private fun setupViews() {
        setupDropdownMenu()
        setupValidation()
        binding.actvAccountType.setText(getString(accountTypes.first().labelRes), false)
    }
    private fun setupDropdownMenu() {
        val labels = accountTypes.map { getString(it.labelRes) }
        val accountTypeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            labels
        )
        binding.actvAccountType.setAdapter(accountTypeAdapter)
        binding.actvAccountType.setOnItemClickListener { _, _, _, _ ->
            validateForm()
        }
    }
    private fun setupValidation() {
        binding.etAccountName.addTextChangedListener {
            validateForm()
        }
        binding.etInitialBalance.addTextChangedListener {
            validateForm()
        }
    }
    private fun setupListeners() {
        binding.btnCreateAccount.setOnClickListener { createAccount() }
    }
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when {
                    state.isLoading -> {
                        binding.btnCreateAccount.isEnabled = false
                        binding.btnCreateAccount.text = getString(R.string.creating)
                    }
                    state.isSuccess -> {
                        Toast.makeText(
                            this@NewAccountActivity,
                            getString(R.string.account_created_successfully),
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                    state.errorMessage != null -> {
                        binding.btnCreateAccount.isEnabled = true
                        binding.btnCreateAccount.text = getString(R.string.create_account)
                        Toast.makeText(
                            this@NewAccountActivity,
                            "Error ${state.errorMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }
    private fun validateForm() : Boolean {
        val accountName = binding.etAccountName.text?.toString()?.trim()
        val labelSelected = binding.actvAccountType.text?.toString()?.trim()
        val initialBalance = binding.etInitialBalance.text?.toString()?.trim()

        var isValid = true

        if (accountName.isNullOrEmpty()) {
            binding.tilAccountName.error = getString(R.string.account_name_required)
            isValid = false
        } else {
            binding.tilAccountName.error = null
        }

        val accountType = accountTypes.firstOrNull() { getString(it.labelRes) == labelSelected }

        if (accountType == null) {
            binding.tilAccountType.error = getString(R.string.account_type_required)
            isValid = false
        } else {
            binding.tilAccountType.error = null
        }

        if (initialBalance.isNullOrEmpty()) {
            binding.tilInitialBalance.error = getString(R.string.initial_balance_required)
            isValid = false
        } else {
            try {
                val balance = initialBalance.toDoubleOrNull()
                if (balance == null) {
                    binding.tilInitialBalance.error = getString(R.string.enter_valid_amount)
                    isValid = false
                } else {
                    binding.tilInitialBalance.error = null
                }
            } catch (e: NumberFormatException) {
                binding.tilInitialBalance.error = getString(R.string.enter_valid_amount)
                isValid = false
            }
        }

        binding.btnCreateAccount.isEnabled = isValid
        return isValid
    }
    private fun createAccount() {
        if (!validateForm()) return

        val accountName = binding.etAccountName.text?.toString()?.trim().orEmpty()
        val labelSelected = binding.actvAccountType.text?.toString()
        val accountType = accountTypes.first { getString(it.labelRes) == labelSelected }
        val initialBalance = binding.etInitialBalance.text?.toString()?.toDoubleOrNull() ?: 0.0

        viewModel.createAccount(accountName, accountType.code, initialBalance)
    }
}