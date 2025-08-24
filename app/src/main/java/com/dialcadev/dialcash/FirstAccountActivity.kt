package com.dialcadev.dialcash

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.dialcadev.dialcash.databinding.FirstAccountActicityBinding
import com.dialcadev.dialcash.ui.viewmodel.FirstAccountViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FirstAccountActivity : AppCompatActivity() {
    private lateinit var binding: FirstAccountActicityBinding
    private val viewModel: FirstAccountViewModel by viewModels()

    private val accountTypeLabels = arrayOf(
        "Checking Account",
        "Savings Account",
        "Credit Card",
        "Cash",
        "Investment Account"
    )

    private val accountTypeMapped = mapOf(
        "Checking Account" to "bank",
        "Savings Account" to "bank",
        "Credit Card" to "card",
        "Cash" to "cash",
        "Investment Account" to "wallet"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FirstAccountActicityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        setupListeners()
        observeViewModel()
        setupBackPressedHandler()
    }

    private fun setupViews() {
        setupDropdownMenu()
        setupValidation()
        binding.actvAccountType.setText(accountTypeLabels[0], false)
    }

    private fun setupDropdownMenu() {
        val accountTypeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            accountTypeLabels
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
        binding.btnCreateAccount.setOnClickListener {
            createAccount()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when {
                    state.isLoading -> {
                        binding.btnCreateAccount.isEnabled = false
                        binding.btnCreateAccount.text = "Creating..."
                    }

                    state.isSuccess -> {
                        Toast.makeText(
                            this@FirstAccountActivity,
                            "Account created successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        navigateToMainActivity()
                    }

                    state.errorMessage != null -> {
                        binding.btnCreateAccount.isEnabled = true
                        binding.btnCreateAccount.text = "Create Account"
                        Toast.makeText(
                            this@FirstAccountActivity,
                            "Error: ${state.errorMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(
                    this@FirstAccountActivity,
                    "You must create an account to proceed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun validateForm(): Boolean {
        val accountName = binding.etAccountName.text?.toString()?.trim()
        val accountType = binding.actvAccountType.text?.toString()?.trim()
        val initialBalance = binding.etInitialBalance.text?.toString()?.trim()

        var isValid = true

        if (accountName.isNullOrEmpty()) {
            binding.tilAccountName.error = "Account name is required"
            isValid = false
        } else {
            binding.tilAccountName.error = null
        }

        if (accountType.isNullOrEmpty() || !accountTypeMapped.containsKey(accountType)) {
            binding.tilAccountType.error = "Account type is required"
            isValid = false
        } else {
            binding.tilAccountType.error = null
        }

        if (initialBalance.isNullOrEmpty()) {
            binding.tilInitialBalance.error = "Initial balance is required"
            isValid = false
        } else {
            try {
                val balance = initialBalance.toDoubleOrNull()
                if (balance == null) {
                    binding.tilInitialBalance.error = "Enter a valid amount"
                    isValid = false
                } else {
                    binding.tilInitialBalance.error = null
                }
            } catch (e: NumberFormatException) {
                binding.tilInitialBalance.error = "Enter a valid amount"
                isValid = false
            }
        }

        binding.btnCreateAccount.isEnabled = isValid
        return isValid
    }

    private fun createAccount() {
        if (!validateForm()) return

        val accountName = binding.etAccountName.text?.toString()?.trim() ?: ""
        val accountTypeLabel = binding.actvAccountType.text?.toString()?.trim() ?: ""
        val initialBalanceText = binding.etInitialBalance.text?.toString()?.trim() ?: "0"
        val initialBalance = initialBalanceText.toDoubleOrNull() ?: 0.0
        val accountTypeValue = accountTypeMapped[accountTypeLabel] ?: "bank"

        viewModel.createAccount(accountName, accountTypeValue, initialBalance)
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}