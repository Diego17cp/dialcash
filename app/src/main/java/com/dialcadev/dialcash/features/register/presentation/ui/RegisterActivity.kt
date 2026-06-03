package com.dialcadev.dialcash.features.register.presentation.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.databinding.RegisterActivityBinding
import com.dialcadev.dialcash.features.accounts.presentation.ui.FirstAccountActivity
import com.dialcadev.dialcash.features.register.presentation.viewmodels.RegisterViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: RegisterActivityBinding
    private val viewModel: RegisterViewModel by viewModels()

    private val selectImageLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (ignored: Exception) { }
            viewModel.onPhotoSelected(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = RegisterActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupViews()
        setupListeners()
        observeViewModel()
    }
    private fun setupViews() {
        val currencyOptions = resources.getStringArray(R.array.currency_options)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, currencyOptions)
        binding.etCurrency.setAdapter(adapter)
        binding.etCurrency.setText("$", false)
    }
    private fun setupListeners() {
        binding.etName.addTextChangedListener { text ->
            viewModel.onNameChanged(text?.toString() ?: "")
        }
        binding.etCurrency.addTextChangedListener { text ->
            viewModel.onCurrencyChanged(text?.toString() ?: "")
        }
        binding.tvChangePhoto.setOnClickListener { selectProfileImage() }
        binding.ivProfilePicture.setOnClickListener { selectProfileImage() }
        binding.btnRegister.setOnClickListener { viewModel.registerUser() }
    }
    private fun selectProfileImage() {
        selectImageLauncher.launch(arrayOf("image/*"))
    }
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.photoUri != null) binding.ivProfilePicture.setImageURI(state.photoUri)
                    binding.tilName.error = state.nameError?.let { getString(it) }
                    binding.tilCurrency.error = state.currencyError?.let { getString(it) }
                    binding.btnRegister.isEnabled = state.isFormValid && !state.isLoading
                    if (state.isSuccess) {
                        Toast.makeText(
                            this@RegisterActivity,
                            getString(R.string.registered_successfully),
                            Toast.LENGTH_SHORT
                        ).show()
                        navigateToFirstAccount()
                    }
                    if (state.errorMessage != null && !state.isLoading) {
                        Toast.makeText(
                            this@RegisterActivity,
                            "Error: ${state.errorMessage}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
    private fun navigateToFirstAccount() {
        val intent = Intent(
            this,
            FirstAccountActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}