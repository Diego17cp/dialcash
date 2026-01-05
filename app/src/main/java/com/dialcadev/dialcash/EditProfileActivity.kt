package com.dialcadev.dialcash

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.dialcadev.dialcash.data.UserDataStore
import com.dialcadev.dialcash.databinding.EditProfileActivityBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint

class EditProfileActivity : AppCompatActivity() {
    private lateinit var binding: EditProfileActivityBinding
    private lateinit var userName: String
    private lateinit var userPhotoUri: String
    private var newPhotoUri: Uri? = null
    private lateinit var userCurrency: String
    private val launcher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (ignored: Exception) {
                }
                newPhotoUri = uri
                binding.ivProfilePicture.setImageURI(uri)
            }
        }

    @Inject
    lateinit var userDataStore: UserDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = EditProfileActivityBinding.inflate(layoutInflater)
        userName = intent.getStringExtra("userName") ?: ""
        userPhotoUri = intent.getStringExtra("userPhotoUri") ?: ""
        userCurrency = intent.getStringExtra("userCurrency") ?: ""
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
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, currencyOptions)
        binding.etCurrency.setAdapter(adapter)
        binding.etCurrency.setText(userCurrency, false)
        binding.etName.setText(userName)
        val uri = userPhotoUri.takeIf { it.isNotBlank() }?.toUri()
        if (uri != null) {
            try {
                binding.ivProfilePicture.setImageURI(uri)
            } catch (e: SecurityException) {
                binding.ivProfilePicture.setImageResource(R.drawable.ic_account_circle)
            } catch (e: Exception) {
                binding.ivProfilePicture.setImageResource(R.drawable.ic_account_circle)
            }
        }
    }

    private fun setupListeners() {
        binding.overlay.setOnClickListener {
            launcher.launch(arrayOf("image/*"))
        }
        binding.etName.addTextChangedListener { validateName() }
        binding.etCurrency.addTextChangedListener { validateCurrency() }
        binding.btnSaveChanges.setOnClickListener { updateInfo() }
    }

    private fun validateName(): Boolean {
        val name = binding.etName.text.toString().trim()
        return if (name.isEmpty()) {
            binding.tilName.error = getString(R.string.name_cannot_be_empty)
            binding.btnSaveChanges.isEnabled = false
            false
        } else {
            binding.tilName.error = null
            binding.btnSaveChanges.isEnabled = true
            true
        }
    }

    private fun validateCurrency(): Boolean {
        val currency = binding.etCurrency.text.toString().trim()
        return if (currency.isEmpty()) {
            binding.tilCurrency.error = getString(R.string.currency_cannot_be_empty)
            binding.btnSaveChanges.isEnabled = false
            false
        } else {
            binding.tilCurrency.error = null
            binding.btnSaveChanges.isEnabled = true
            true
        }
    }

    private fun updateInfo() {
        if (!validateName()) return
        val updatedName = binding.etName.text.toString().trim()
        val updatedPhotoUri = newPhotoUri?.toString() ?: userPhotoUri
        val updatedCurrency = binding.etCurrency.text.toString().trim()
            .substringBefore(" -")
        lifecycleScope.launch {
            try {
                userDataStore.updateUserData(
                    name = updatedName,
                    photoUri = updatedPhotoUri.takeIf { it.isNotBlank() },
                    currencySymbol = updatedCurrency
                )
                Toast.makeText(this@EditProfileActivity, getString(R.string.profile_updated), Toast.LENGTH_SHORT)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@EditProfileActivity,
                    getString(R.string.failed_to_update_profile),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}