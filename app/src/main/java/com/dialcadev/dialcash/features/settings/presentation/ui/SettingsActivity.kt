package com.dialcadev.dialcash.features.settings.presentation.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.RadioGroup
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.dialcadev.dialcash.DialCashApp
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.core.datastore.UserDataStore
import com.dialcadev.dialcash.core.models.UserPreferences
import com.dialcadev.dialcash.core.theme.AppTypography
import com.dialcadev.dialcash.core.theme.DialCashTheme
import com.dialcadev.dialcash.core.ui.components.LiquidAppBar
import com.dialcadev.dialcash.features.settings.presentation.ui.components.SettingsScreen
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    @Inject
    lateinit var userDataStore: UserDataStore
    private var preferences: UserPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            DialCashTheme (typography = AppTypography) {
                val hazeState = remember { HazeState() }
                var topBarHeight by remember { mutableStateOf(0.dp) }
                val density = LocalDensity.current

                val userPreferences by userDataStore.getUserData()
                    .collectAsStateWithLifecycle(initialValue = null)

                preferences = userPreferences

                val userName = userPreferences?.name?.takeIf { it.isNotBlank() }
                    ?: getString(R.string.username)
                val photoUri = userPreferences?.photoUri?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                val themeSummary = when (userPreferences?.themeMode) {
                    AppCompatDelegate.MODE_NIGHT_NO -> getString(R.string.light)
                    AppCompatDelegate.MODE_NIGHT_YES -> getString(R.string.dark)
                    else -> getString(R.string.system_default)
                }

                Box(Modifier.fillMaxSize()) {
                    SettingsScreen(
                        hazeState = hazeState,
                        topBarHeight = topBarHeight,
                        userName = userName,
                        photoUri = photoUri,
                        themeSummary = themeSummary,
                        onEditProfileClick = { navigateToEditProfile() },
                        onThemeClick = { openThemeSelector() },
                        onExportDataClick = { navigateToExportData() },
                        onImportDataClick = { navigateToImportData() },
                        onDeleteAccountClick = { navigateToDeleteAccount() }
                    )

                    LiquidAppBar(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .onGloballyPositioned {
                                topBarHeight = with(density) { it.size.height.toDp() }
                            },
                        hazeState = hazeState,
                        title = getString(R.string.settings),
                        onBackClick = { onBackPressedDispatcher.onBackPressed() }
                    )
                }
            }
        }
    }

    private fun navigateToEditProfile() {
        startActivity(Intent(this, EditProfileActivity::class.java))
    }
    private fun navigateToDeleteAccount() {
        startActivity(Intent(this, DeleteAccountActivity::class.java))
    }
    private fun navigateToExportData() {
        startActivity(Intent(this, DownloadDataActivity::class.java))
    }
    private fun navigateToImportData() {
        startActivity(Intent(this, ImportDataActivity::class.java))
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