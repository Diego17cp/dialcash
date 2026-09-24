package com.dialcadev.dialcash.features.settings.presentation.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.core.theme.AppTypography
import com.dialcadev.dialcash.core.ui.components.LiquidAppBar
import com.dialcadev.dialcash.databinding.ImportDataActivityBinding
import com.dialcadev.dialcash.features.settings.presentation.viewmodels.ImportDataViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ImportDataActivity : AppCompatActivity() {
    private lateinit var binding: ImportDataActivityBinding
    private val hazeState = HazeState()
    private val viewModel: ImportDataViewModel by viewModels()

    private val documentPicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    viewModel.startImport(uri.toString())
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ImportDataActivityBinding.inflate(layoutInflater)
        val composeView = ComposeView(this).apply {
            setContent {
                MaterialTheme(
                    typography = AppTypography
                ) {
                    var appBarHeightPx by remember { mutableIntStateOf(0) }
                    val extraTopPaddingPx = (12 * resources.displayMetrics.density).toInt()
                    Box(Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { binding.root },
                            modifier = Modifier
                                .fillMaxSize()
                                .hazeSource(hazeState),
                            update = {
                                val nested = binding.nestedScrollView
                                val desiredTop = appBarHeightPx + extraTopPaddingPx
                                if (nested.paddingTop != desiredTop) {
                                    nested.updatePadding(top = desiredTop)
                                }
                            }
                        )
                        LiquidAppBar(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .onGloballyPositioned { coordinates ->
                                    val measured = coordinates.size.height
                                    if (measured != appBarHeightPx) {
                                        appBarHeightPx = measured
                                    }
                                },
                            hazeState = hazeState,
                            title = getString(R.string.import_data),
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

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.confirmText.setOnClickListener { viewModel.toggleCheckbox() }
        binding.confirmCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (binding.confirmCheckbox.isChecked != viewModel.uiState.value.isCheckboxChecked) viewModel.setCheckboxChecked(
                isChecked
            )
        }
        binding.btnImport.setOnClickListener {
            if (!viewModel.uiState.value.isCheckboxChecked) {
                Toast.makeText(
                    this,
                    getString(R.string.please_confirm_to_proceed),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            openFilePicker()
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        documentPicker.launch(intent)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.confirmCheckbox.isChecked = state.isCheckboxChecked
                    binding.btnImport.isEnabled = !state.isLoading
                    if (state.isLoading) binding.btnImport.text = getString(R.string.importing)
                    else binding.btnImport.text = getString(R.string.import_data)
                    if (state.isSuccess) {
                        setResult(RESULT_OK)
                        Toast.makeText(
                            this@ImportDataActivity,
                            getString(R.string.data_imported_successfully),
                            Toast.LENGTH_LONG
                        ).show()
                        viewModel.onCompleteHandled()
                        finish()
                    }
                    state.errorMessage?.let { errorMsg ->
                        val friendlyError =
                            if (errorMsg.contains("is not a valid backup file") || errorMsg.contains(
                                    "Invalid backup format"
                                )
                            ) {
                                getString(R.string.selected_file_error)
                            } else {
                                getString(R.string.failed_open_file) + ": " + errorMsg
                            }

                        Toast.makeText(this@ImportDataActivity, friendlyError, Toast.LENGTH_LONG)
                            .show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }
}