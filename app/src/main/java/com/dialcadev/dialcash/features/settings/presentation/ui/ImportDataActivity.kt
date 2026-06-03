package com.dialcadev.dialcash.features.settings.presentation.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.databinding.ImportDataActivityBinding
import com.dialcadev.dialcash.features.settings.presentation.viewmodels.ImportDataViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ImportDataActivity : AppCompatActivity() {
    private lateinit var binding: ImportDataActivityBinding
    private val viewModel: ImportDataViewModel by viewModels()

    private val documentPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.startImport(uri.toString())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ImportDataActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.import_data_layout)) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
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
        binding.btnImport.setOnClickListener {
            if (!viewModel.uiState.value.isCheckboxChecked) {
                Toast.makeText(
                    this,
                    getString(R.string.please_confirm_to_proceed),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            openFilePicker()
        }
    }
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        documentPicker.launch(intent)
    }
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.confirmCheckbox.isChecked = state.isCheckboxChecked
                    binding.btnImport.isEnabled = !state.isLoading
                    if (state.isLoading) binding.btnImport.text = getString(R.string.importing)
                    else binding.btnImport.text = getString(R.string.import_data)
                    if (state.isSuccess) {
                        setResult(RESULT_OK)
                        Toast.makeText(
                            this@ImportDataActivity,
                            getString(R.string.data_imported_successfully),
                            Toast.LENGTH_LONG
                        ).show()
                        viewModel.onCompleteHandled()
                        finish()
                    }
                    state.errorMessage?.let { errorMsg ->
                        val friendlyError = if (errorMsg.contains("is not a valid backup file") || errorMsg.contains("Invalid backup format")) {
                            getString(R.string.selected_file_error)
                        } else {
                            getString(R.string.failed_open_file) + ": " + errorMsg
                        }

                        Toast.makeText(this@ImportDataActivity, friendlyError, Toast.LENGTH_LONG).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }
}