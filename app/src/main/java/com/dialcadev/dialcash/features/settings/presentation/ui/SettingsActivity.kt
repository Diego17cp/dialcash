package com.dialcadev.dialcash.features.settings.presentation.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.RadioGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.dialcadev.dialcash.DialCashApp
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.core.datastore.UserDataStore
import com.dialcadev.dialcash.core.models.UserPreferences
import com.dialcadev.dialcash.databinding.SettingsActivityBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.core.view.updatePadding
import com.dialcadev.dialcash.core.theme.AppTypography
import com.dialcadev.dialcash.core.ui.components.LiquidAppBar
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: SettingsActivityBinding
    private val hazeState = HazeState()

    @Inject
    lateinit var userDataStore: UserDataStore
    var preferences: UserPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = SettingsActivityBinding.inflate(layoutInflater)
        val composeView = ComposeView(this).apply {
            setContent {
                MaterialTheme(
                    typography = AppTypography
                ) {
                    Box(Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { binding.root },
                            modifier = Modifier.fillMaxSize().hazeSource(hazeState)
                        )
                        LiquidAppBar(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .onGloballyPositioned {
                                    binding.nestedScrollView.updatePadding(top = it.size.height)
                                },
                            hazeState = hazeState,
                            title = getString(R.string.settings),
                            onBackClick = { onBackPressedDispatcher.onBackPressed() }
                        )
                    }
                }
            }
        }
        setContentView(composeView)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }
        setupViews()
        setupListeners()
    }
    private fun setupViews() {
        lifecycleScope.launch {
            userDataStore.getUserData().collect { userPreferences ->
                preferences = userPreferences
                binding.tvUsername.text = userPreferences.name?.takeIf { it.isNotBlank() } ?: "User"
                val uri = userPreferences.photoUri.takeIf { it.isNotBlank() }?.toUri()
                if (uri != null) {
                    try {
                        binding.imageProfile.setImageURI(uri)
                    } catch (e: Exception) {
                        binding.imageProfile.setImageResource(R.drawable.ic_account_circle)
                    }
                } else {
                    binding.imageProfile.setImageResource(R.drawable.ic_account_circle)
                }
                val themeTextRes = when (userPreferences.themeMode) {
                    AppCompatDelegate.MODE_NIGHT_NO -> R.string.light
                    AppCompatDelegate.MODE_NIGHT_YES -> R.string.dark
                    else -> R.string.system_default
                }
                binding.textCurrentTheme.text = getString(themeTextRes)
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
        val currentTheme = preferences?.themeMode ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        when (currentTheme) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> radioGroup.check(R.id.rb_system)
            AppCompatDelegate.MODE_NIGHT_NO -> radioGroup.check(R.id.rb_light)
            AppCompatDelegate.MODE_NIGHT_YES -> radioGroup.check(R.id.rb_dark)
            else -> radioGroup.check(R.id.rb_system)
        }
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.select_theme))
            .setView(view)
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Ok") { dialog, _ ->
                val selectedTheme = when (radioGroup.checkedRadioButtonId) {
                    R.id.rb_system -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    R.id.rb_light -> AppCompatDelegate.MODE_NIGHT_NO
                    R.id.rb_dark -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                lifecycleScope.launch {
                    userDataStore.updateThemeMode(selectedTheme)
                    getSharedPreferences(DialCashApp.THEME_PREFS_NAME, MODE_PRIVATE)
                        .edit(commit = true) {
                            putInt(DialCashApp.KEY_NIGHT_MODE, selectedTheme)
                        }
                    AppCompatDelegate.setDefaultNightMode(selectedTheme)
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