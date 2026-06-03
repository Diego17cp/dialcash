package com.dialcadev.dialcash.features.settings.presentation.ui

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.databinding.DownloadDataActivityBinding
import com.dialcadev.dialcash.features.settings.presentation.viewmodels.DownloadDataViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DownloadDataActivity : AppCompatActivity() {
    private lateinit var binding: DownloadDataActivityBinding
    private val viewModel: DownloadDataViewModel by viewModels()

    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        uri?.let { viewModel.startExport(it.toString()) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DownloadDataActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.download_data_layout)) { v, insets ->
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
            if (binding.confirmCheckbox.isChecked != viewModel.uiState.value.isCheckboxChecked) {
                viewModel.setCheckboxChecked(isChecked)
            }
        }
        binding.btnDownload.setOnClickListener {
            if (!viewModel.uiState.value.isCheckboxChecked) {
                Toast.makeText(
                    this,
                    getString(R.string.please_confirm_to_proceed),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            launchStoragePicker()
        }
    }
    private fun launchStoragePicker() {
        val fileName = "dialcash_data_${System.currentTimeMillis()}.backup"
        try {
            createDocumentLauncher.launch(fileName)
        } catch (e: Exception) {
            Log.e("DownloadDataActivity", "Error launching file picker", e)
            Toast.makeText(this, "Error starting file picker", Toast.LENGTH_SHORT).show()
        }
    }
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.confirmCheckbox.isChecked = state.isCheckboxChecked
                    binding.btnDownload.isEnabled = !state.isLoading
                    if (state.isLoading) binding.btnDownload.text = getString(R.string.generating_backup)
                    else binding.btnDownload.text = getString(R.string.download_data)
                    if (state.isSuccess) {
                        Toast.makeText(this@DownloadDataActivity, getString(R.string.backup_saved_successfully), Toast.LENGTH_LONG).show()
                        viewModel.onCompleteHandled()
                        onBackPressedDispatcher.onBackPressed()
                    }

                    state.errorMessage?.let { error ->
                        Toast.makeText(this@DownloadDataActivity, "${getString(R.string.failed_to_generate_backup)}: $error", Toast.LENGTH_LONG).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }
}