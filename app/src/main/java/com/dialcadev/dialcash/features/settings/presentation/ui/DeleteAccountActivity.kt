package com.dialcadev.dialcash.features.settings.presentation.ui

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.databinding.DeleteAccountActivityBinding
import com.dialcadev.dialcash.features.settings.presentation.viewmodels.DeleteAccountViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DeleteAccountActivity : AppCompatActivity() {
    lateinit var binding: DeleteAccountActivityBinding
    private val viewModel: DeleteAccountViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DeleteAccountActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.delete_account)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
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
    private fun setupListeners() {
        binding.btnCancel.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.confirmText.setOnClickListener { viewModel.toggleCheckbox() }
        binding.confirmCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (binding.confirmCheckbox.isChecked != viewModel.uiState.value.isCheckboxChecked) viewModel.setCheckboxChecked(isChecked)
        }
        binding.btnDelete.setOnClickListener {
            if (!viewModel.uiState.value.isCheckboxChecked) {
                Toast.makeText(
                    this,
                    getString(R.string.please_confirm_to_proceed),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            viewModel.deleteAccount()
        }
    }
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.confirmCheckbox.isChecked = state.isCheckboxChecked
                    binding.btnDelete.isEnabled = !state.isLoading
                    if (state.isSuccess) {
                        Toast.makeText(
                            this@DeleteAccountActivity,
                            getString(R.string.account_deleted_successfully),
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }

                    state.errorMessage?.let { error ->
                        Toast.makeText(this@DeleteAccountActivity, error, Toast.LENGTH_LONG).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }
}