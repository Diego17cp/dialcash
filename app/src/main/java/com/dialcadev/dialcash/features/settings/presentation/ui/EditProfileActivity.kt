package com.dialcadev.dialcash.features.settings.presentation.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.databinding.EditProfileActivityBinding
import com.dialcadev.dialcash.features.settings.presentation.viewmodels.EditProfileViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditProfileActivity : AppCompatActivity() {
    private lateinit var binding: EditProfileActivityBinding
    private val viewModel: EditProfileViewModel by viewModels()
    private var isInitialDataBound = false
    private val launcher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                try {
                    contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (ignored: Exception) {
                }
                viewModel.onPhotoPicked(it.toString())
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = EditProfileActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        setupViews()
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

    private fun setupViews() {
        val currencyOptions = resources.getStringArray(R.array.currency_options)
        val adapter =
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, currencyOptions)
        binding.etCurrency.setAdapter(adapter)
    }

    private fun setupListeners() {
        binding.overlay.setOnClickListener { launcher.launch(arrayOf("image/*")) }
        binding.etName.addTextChangedListener { viewModel.onNameChanged(it.toString()) }
        binding.etCurrency.addTextChangedListener { viewModel.onCurrencyChanged(it.toString()) }
        binding.btnSaveChanges.setOnClickListener { viewModel.saveChanges() }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.isInitialized && !isInitialDataBound) {
                        binding.etName.setText(state.initialName)
                        binding.etCurrency.setText(state.initialCurrency, false)
                        isInitialDataBound = true
                    }
                    binding.tilName.error =
                        if (state.isNameError) getString(R.string.name_cannot_be_empty) else null
                    binding.tilCurrency.error =
                        if (state.isCurrencyError) getString(R.string.currency_cannot_be_empty) else null
                    binding.btnSaveChanges.isEnabled = state.isSaveEnabled

                    val uri = state.currentPhotoUri.takeIf { it.isNotBlank() }?.toUri()
                    if (uri != null) {
                        try {
                            binding.ivProfilePicture.setImageURI(uri)
                        } catch (e: Exception) {
                            binding.ivProfilePicture.setImageResource(R.drawable.ic_account_circle)
                        }
                    } else {
                        binding.ivProfilePicture.setImageResource(R.drawable.ic_account_circle)
                    }

                    if (state.showSuccessMessage) {
                        Toast.makeText(
                            this@EditProfileActivity,
                            getString(R.string.profile_updated),
                            Toast.LENGTH_SHORT
                        ).show()
                        viewModel.onMessagesShown()
                    }
                    if (state.showErrorMessage) {
                        Toast.makeText(
                            this@EditProfileActivity,
                            getString(R.string.failed_to_update_profile),
                            Toast.LENGTH_SHORT
                        ).show()
                        viewModel.onMessagesShown()
                    }
                }
            }
        }
    }
}