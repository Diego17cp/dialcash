package com.dialcadev.dialcash

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.dialcadev.dialcash.data.UserDataStore
import com.dialcadev.dialcash.databinding.RegisterActivityBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: RegisterActivityBinding

    @Inject
    lateinit var userDataStore: UserDataStore
    private var selectedImageUri: Uri? = null

    private val selectImageLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (ignored: Exception) { }
            selectedImageUri = uri
            binding.ivProfilePicture.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = RegisterActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupViews()
        setupListeners()
    }

    private fun setupViews() {
        val currencyOptions = resources.getStringArray(R.array.currency_options)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, currencyOptions)
        binding.etCurrency.setAdapter(adapter)
        binding.etCurrency.setText("$", false)
        binding.etName.addTextChangedListener {
            validateForm()
        }
        binding.etCurrency.addTextChangedListener {
            validateForm()
        }
    }

    private fun setupListeners() {
        binding.tvChangePhoto.setOnClickListener {
            selectProfileImage()
        }
        binding.ivProfilePicture.setOnClickListener {
            selectProfileImage()
        }
        binding.btnRegister.setOnClickListener {
            registerUser()
        }
    }

    private fun selectProfileImage() {
        selectImageLauncher.launch(arrayOf("image/*"))
    }

    private fun validateForm(): Boolean {
        val name = binding.etName.text?.toString()?.trim()
        val currency = binding.etCurrency.text?.toString()?.trim()
        var isValid = true

        if (name.isNullOrEmpty()) {
            binding.tilName.error = getString(R.string.name_cannot_be_empty)
            isValid = false
        } else {
            binding.tilName.error = null
        }

        if (currency.isNullOrEmpty()) {
            binding.tilCurrency.error = getString(R.string.currency_cannot_be_empty)
            isValid = false
        } else {
            binding.tilCurrency.error = null
        }

        binding.btnRegister.isEnabled = isValid
        return isValid
    }

    private fun registerUser() {
        if (!validateForm()) return

        val name = binding.etName.text.toString().trim()
        val photoUriString = selectedImageUri?.toString()
        val currencySymbol = binding.etCurrency.text.toString().trim()
            .substringBefore(" -")

        lifecycleScope.launch {
            try {
                userDataStore.saveUserData(
                    name = name,
                    photoUri = photoUriString,
                    currencySymbol = currencySymbol
                )

                Toast.makeText(this@RegisterActivity, getString(R.string.registered_successfully), Toast.LENGTH_SHORT).show()

                val intent = Intent(this@RegisterActivity, FirstAccountActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()

            } catch (e: Exception) {
                Toast.makeText(
                    this@RegisterActivity,
                    "Error registering: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}