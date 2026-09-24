package com.dialcadev.dialcash.features.settings.presentation.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.core.theme.AppTypography
import com.dialcadev.dialcash.core.theme.DialCashTheme
import com.dialcadev.dialcash.core.ui.components.LiquidAppBar
import com.dialcadev.dialcash.features.settings.presentation.ui.components.EditProfileScreen
import com.dialcadev.dialcash.features.settings.presentation.viewmodels.EditProfileViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.haze.HazeState
import androidx.compose.ui.res.stringResource

@AndroidEntryPoint
class EditProfileActivity : AppCompatActivity() {

    private val viewModel: EditProfileViewModel by viewModels()

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
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            DialCashTheme (typography = AppTypography) {
                val hazeState = remember { HazeState() }
                var topBarHeight by remember { mutableStateOf(0.dp) }
                val density = LocalDensity.current

                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val currencyOptions = remember {
                    resources.getStringArray(R.array.currency_options).toList()
                }
                var name by remember(uiState.isInitialized) {
                    mutableStateOf(if (uiState.isInitialized) uiState.initialName else "")
                }
                var currency by remember(uiState.isInitialized) {
                    mutableStateOf(if (uiState.isInitialized) uiState.initialCurrency else "")
                }

                val photoUri = uiState.currentPhotoUri.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }

                LaunchedEffectMessages(
                    showSuccess = uiState.showSuccessMessage,
                    showError = uiState.showErrorMessage,
                    onShown = { viewModel.onMessagesShown() }
                )

                Box(Modifier.fillMaxSize()) {
                    EditProfileScreen(
                        hazeState = hazeState,
                        topBarHeight = topBarHeight,
                        photoUri = photoUri,
                        name = name,
                        onNameChanged = {
                            name = it
                            viewModel.onNameChanged(it)
                        },
                        nameError = if (uiState.isNameError) getString(R.string.name_cannot_be_empty) else null,
                        currency = currency,
                        onCurrencyChanged = {
                            currency = it
                            viewModel.onCurrencyChanged(it)
                        },
                        currencyOptions = currencyOptions,
                        currencyError = if (uiState.isCurrencyError) getString(R.string.currency_cannot_be_empty) else null,
                        isSaveEnabled = uiState.isSaveEnabled,
                        onSaveClick = { viewModel.saveChanges() },
                        onPickPhotoClick = { launcher.launch(arrayOf("image/*")) }
                    )

                    LiquidAppBar(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .onGloballyPositioned {
                                topBarHeight = with(density) { it.size.height.toDp() }
                            },
                        hazeState = hazeState,
                        title = getString(R.string.edit_profile),
                        onBackClick = { onBackPressedDispatcher.onBackPressed() }
                    )
                }
            }
        }
    }
}

@Composable
private fun LaunchedEffectMessages(
    showSuccess: Boolean,
    showError: Boolean,
    onShown: () -> Unit
) {
    val context = LocalContext.current
    val successText = stringResource(R.string.profile_updated)
    val errorText = stringResource(R.string.failed_to_update_profile)
    androidx.compose.runtime.LaunchedEffect(showSuccess, showError) {
        if (showSuccess) {
            android.widget.Toast.makeText(
                context,
                successText,
                android.widget.Toast.LENGTH_SHORT
            ).show()
            onShown()
        }
        if (showError) {
            android.widget.Toast.makeText(
                context,
                errorText,
                android.widget.Toast.LENGTH_SHORT
            ).show()
            onShown()
        }
    }
}