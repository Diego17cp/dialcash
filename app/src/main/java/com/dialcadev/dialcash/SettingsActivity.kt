package com.dialcadev.dialcash

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.dialcadev.dialcash.data.UserDataStore
import com.dialcadev.dialcash.databinding.SettingsActivityBinding
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import com.dialcadev.dialcash.data.UserData

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: SettingsActivityBinding
    private lateinit var userDataStore: UserDataStore
    var userData: UserData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = SettingsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        userDataStore = UserDataStore.getInstance(this)
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
        lifecycleScope.launch {
            userDataStore.getUserData().collect { user ->
                userData = user
                binding.tvUsername.text = user.name
                val uri = user.photoUri
                    .takeIf { it.isNotBlank() }?.toUri()
                binding.imageProfile.setImageURI(uri)
            }
        }
    }

    private fun setupListeners() {
        binding.tvEditProfile.setOnClickListener { navigateToEditProfile() }
    }

    private fun navigateToEditProfile() {
        val intent = Intent(this, EditProfileActivity::class.java)
        intent.putExtra("userName", userData?.name)
        intent.putExtra("userPhotoUri", userData?.photoUri)
        startActivity(intent)
    }
}