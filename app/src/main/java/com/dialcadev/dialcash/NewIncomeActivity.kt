package com.dialcadev.dialcash

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.dialcadev.dialcash.databinding.NewIncomeActivityBinding
import com.dialcadev.dialcash.ui.incomegroup.NewIncomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NewIncomeActivity : AppCompatActivity() {
    private lateinit var binding: NewIncomeActivityBinding
    private val viewModel: NewIncomeViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = NewIncomeActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupValidation()
        setupListeners()
        observeViewModel()
    }

    private fun setupValidation() {
        binding.etIncomeName.addTextChangedListener {
            validateForm()
        }
        binding.etAmount.addTextChangedListener {
            validateForm()
        }
    }

    private fun setupListeners() {
        binding.btnCreateIncome.setOnClickListener { createIncome() }
        binding.btnCancel.setOnClickListener { finish() }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when {
                    state.isLoading -> {
                        binding.btnCreateIncome.isEnabled = false
                        binding.btnCreateIncome.text = "Creating..."
                    }

                    state.isSuccess -> {
                        Toast.makeText(
                            this@NewIncomeActivity,
                            "Income group created successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }

                    state.errorMessage != null -> {
                        binding.btnCreateIncome.isEnabled = true
                        binding.btnCreateIncome.text = "Create Income"
                        Toast.makeText(
                            this@NewIncomeActivity,
                            state.errorMessage,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun validateForm(): Boolean {
        val name = binding.etIncomeName.text?.toString()?.trim()
        val amountText = binding.etAmount.text?.toString()?.trim()
        var isValid = true
        if (name.isNullOrEmpty()) {
            binding.tilIncomeName.error = "Name is required"
            isValid = false
        } else binding.tilIncomeName.error = null

        if (amountText.isNullOrEmpty()) {
            binding.tilAmount.error = "Amount is required"
            isValid = false
        } else binding.tilAmount.error = null

        if (amountText.isNullOrEmpty()) {
            binding.tilAmount.error = "Amount is required"
            isValid = false
        } else {
            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                binding.tilAmount.error = "Enter a valid amount"
                isValid = false
            } else {
                binding.tilAmount.error = null
            }
        }
        binding.btnCreateIncome.isEnabled = isValid
        return isValid
    }

    private fun createIncome() {
        if (!validateForm()) return
        val name = binding.etIncomeName.text?.toString()?.trim() ?: ""
        val amount = binding.etAmount.text?.toString()?.trim()?.toDoubleOrNull() ?: 0.0
        val remaining = binding.etRemaining.text?.toString()?.trim()?.toDoubleOrNull()
        viewModel.createIncomeGroup(name, amount, remaining)
    }
}