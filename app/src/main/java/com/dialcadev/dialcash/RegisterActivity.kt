package com.dialcadev.dialcash

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.dialcadev.dialcash.data.UserDataStore
import com.dialcadev.dialcash.databinding.RegisterActivityBinding
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: RegisterActivityBinding
    private lateinit var userDataStore: UserDataStore
    private var selectedImageUri: Uri? = null

    private val selectImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                binding.ivProfilePicture.setImageURI(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = RegisterActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userDataStore = UserDataStore.getInstance(this)
        setupViews()
        setupListeners()
    }

    private fun setupViews() {
        binding.etName.addTextChangedListener {
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
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        selectImageLauncher.launch(intent)
    }

    private fun validateForm(): Boolean {
        val name = binding.etName.text?.toString()?.trim()
        var isValid = true

        if (name.isNullOrEmpty()) {
            binding.tilName.error = "The name is required"
            isValid = false
        } else {
            binding.tilName.error = null
        }

        binding.btnRegister.isEnabled = isValid
        return isValid
    }

    private fun registerUser() {
        if (!validateForm()) return

        val name = binding.etName.text.toString().trim()
        val photoUriString = selectedImageUri?.toString()

        lifecycleScope.launch {
            try {
                userDataStore.saveUserData(
                    name = name,
                    photoUri = photoUriString
                )

                Toast.makeText(this@RegisterActivity, "Registered successful", Toast.LENGTH_SHORT).show()

                val intent = Intent(this@RegisterActivity, MainActivity::class.java)
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