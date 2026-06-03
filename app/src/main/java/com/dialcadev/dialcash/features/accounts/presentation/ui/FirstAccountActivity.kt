package com.dialcadev.dialcash.features.accounts.presentation.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dialcadev.dialcash.MainActivity
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.databinding.FirstAccountActivityBinding
import com.dialcadev.dialcash.features.accounts.presentation.model.FirstAccountTypeUI
import com.dialcadev.dialcash.features.accounts.presentation.provider.FirstAccountTypeProvider
import com.dialcadev.dialcash.features.accounts.presentation.viewmodels.CreateAccountViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FirstAccountActivity : AppCompatActivity() {
    private lateinit var binding: FirstAccountActivityBinding
    private val viewModel: CreateAccountViewModel by viewModels()
    private lateinit var accountTypes: List<FirstAccountTypeUI>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FirstAccountActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupViews()
        setupListeners()
        observeViewModel()
        setupBackPressedHandler()
    }
    private fun setupViews() {
        accountTypes = FirstAccountTypeProvider.items
        val labels = accountTypes.map { getString(it.labelRes) }
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            labels
        )
        binding.actvAccountType.setAdapter(adapter)
        if (labels.isNotEmpty()) {
            val firstItem = accountTypes.first()
            binding.actvAccountType.setText(
                getString(firstItem.labelRes),
                false
            )
            viewModel.onTypeChanged(firstItem.type.code)

        }
    }
    private fun setupListeners() {
        binding.etAccountName.addTextChangedListener { text ->
            viewModel.onNameChanged(text?.toString()?.trim() ?: "")
        }
        binding.etInitialBalance.addTextChangedListener { text ->
            viewModel.onBalanceChanged(text?.toString()?.trim() ?: "")
        }
        binding.actvAccountType.setOnItemClickListener { _, _, position, _ ->
            val selected = accountTypes[position]
            viewModel.onTypeChanged(selected.type.code)
        }
        binding.btnCreateAccount.setOnClickListener { viewModel.createAccount() }
    }
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.tilAccountName.error = state.nameError?.let { getString(it) }
                    binding.tilInitialBalance.error = state.balanceError?.let { getString(it) }
                    binding.tilInitialBalance.error = state.balanceError?.let { getString(it) }
                    binding.btnCreateAccount.isEnabled = state.isFormValid
                    binding.btnCreateAccount.text = getString(
                        if (state.isLoading) R.string.creating else R.string.create_account
                    )
                    if (state.isSuccess) {
                        Toast.makeText(
                            this@FirstAccountActivity,
                            R.string.account_created_successfully,
                            Toast.LENGTH_SHORT
                        ).show()
                        navigateToMainActivity()
                    }
                    if (state.errorMessage != null && !state.isLoading) {
                        Toast.makeText(
                            this@FirstAccountActivity,
                            "Error: ${state.errorMessage}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(
                    this@FirstAccountActivity,
                    R.string.must_create_acc_to_proceed,
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
    private fun navigateToMainActivity() {
        val intent = Intent(
            this,
            MainActivity::class.java
        )
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}