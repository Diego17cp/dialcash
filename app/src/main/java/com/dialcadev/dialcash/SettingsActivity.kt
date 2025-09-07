package com.dialcadev.dialcash

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.WindowManager
import android.widget.RadioGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.dialcadev.dialcash.data.UserDataStore
import com.dialcadev.dialcash.databinding.SettingsActivityBinding
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import com.dialcadev.dialcash.data.UserData
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: SettingsActivityBinding

    @Inject
    lateinit var userDataStore: UserDataStore
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
                binding.tvUsername.text = user.name?.takeIf { it.isNotBlank() } ?: "User"
                val uri = user.photoUri
                    .takeIf { it.isNotBlank() }?.toUri()
                if (uri != null) {
                    try {
                        binding.imageProfile.setImageURI(uri)
                    } catch (e: Exception) {
                        binding.imageProfile.setImageResource(R.drawable.ic_account_circle)
                    } catch (e: Exception) {
                        binding.imageProfile.setImageResource(R.drawable.ic_account_circle)
                    }
                } else {
                    binding.imageProfile.setImageResource(R.drawable.ic_account_circle)
                }
            }
        }
    }

    private fun setupListeners() {
        binding.tvEditProfile.setOnClickListener { navigateToEditProfile() }
        binding.layoutThemeSelector.setOnClickListener { openThemeSelector() }
        binding.layoutDeleteAccount.setOnClickListener { navigateToDeleteAccount() }
        binding.layoutExportData.setOnClickListener { navigateToExportData() }
        binding.layoutImportData.setOnClickListener { navigateToImportData() }
    }

    private fun navigateToEditProfile() {
        val intent = Intent(this, EditProfileActivity::class.java)
        intent.putExtra("userName", userData?.name)
        intent.putExtra("userPhotoUri", userData?.photoUri)
        startActivity(intent)
    }

    private fun navigateToDeleteAccount() {
        val intent = Intent(this, DeleteAccountActivity::class.java)
        startActivity(intent)
    }
    private fun navigateToExportData() {
        val intent = Intent(this, DownloadDataActivity::class.java)
        startActivity(intent)
    }
    private fun navigateToImportData() {
        val intent = Intent(this, ImportDataActivity::class.java)
        startActivity(intent)
    }

    private fun openThemeSelector() {
        val view = layoutInflater.inflate(R.layout.theme_picker_sheet, null)
        val radioGroup = view.findViewById<RadioGroup>(R.id.rg_theme)
        when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> radioGroup.check(R.id.rb_system)
            AppCompatDelegate.MODE_NIGHT_NO -> radioGroup.check(R.id.rb_light)
            AppCompatDelegate.MODE_NIGHT_YES -> radioGroup.check(R.id.rb_dark)
            else -> radioGroup.check(R.id.rb_system)
        }
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Select Theme")
            .setView(view)
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Ok") { dialog, _ ->
                when (radioGroup.checkedRadioButtonId) {
                    R.id.rb_system -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    R.id.rb_light -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    R.id.rb_dark -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
                dialog.dismiss()
            }
            .create()

        dialog.window?.apply {
            setBackgroundDrawableResource(R.drawable.dialog_background)
            val params = attributes
            params.width = (resources.displayMetrics.widthPixels * 0.8).toInt()
            attributes = params
        }
        dialog.show()
    }
}