package com.dialcadev.dialcash.features.settings.presentation.ui

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
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
import com.dialcadev.dialcash.databinding.DownloadDataActivityBinding
import com.dialcadev.dialcash.features.settings.presentation.viewmodels.DownloadDataViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DownloadDataActivity : AppCompatActivity() {
    private lateinit var binding: DownloadDataActivityBinding
    private val hazeState = HazeState()

    private val viewModel: DownloadDataViewModel by viewModels()

    private val createDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
            uri?.let { viewModel.startExport(it.toString()) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = DownloadDataActivityBinding.inflate(layoutInflater)
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
                            title = getString(R.string.download_data),
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
            if (binding.confirmCheckbox.isChecked != viewModel.uiState.value.isCheckboxChecked) {
                viewModel.setCheckboxChecked(isChecked)
            }
        }
        binding.btnDownload.setOnClickListener {
            if (!viewModel.uiState.value.isCheckboxChecked) {
                Toast.makeText(
                    this,
                    getString(R.string.please_confirm_to_proceed),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            launchStoragePicker()
        }
    }

    private fun launchStoragePicker() {
        val fileName = "dialcash_data_${System.currentTimeMillis()}.backup"
        try {
            createDocumentLauncher.launch(fileName)
        } catch (e: Exception) {
            Log.e("DownloadDataActivity", "Error launching file picker", e)
            Toast.makeText(this, "Error starting file picker", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.confirmCheckbox.isChecked = state.isCheckboxChecked
                    binding.btnDownload.isEnabled = !state.isLoading
                    if (state.isLoading) binding.btnDownload.text =
                        getString(R.string.generating_backup)
                    else binding.btnDownload.text = getString(R.string.download_data)
                    if (state.isSuccess) {
                        Toast.makeText(
                            this@DownloadDataActivity,
                            getString(R.string.backup_saved_successfully),
                            Toast.LENGTH_LONG
                        ).show()
                        viewModel.onCompleteHandled()
                        onBackPressedDispatcher.onBackPressed()
                    }

                    state.errorMessage?.let { error ->
                        Toast.makeText(
                            this@DownloadDataActivity,
                            "${getString(R.string.failed_to_generate_backup)}: $error",
                            Toast.LENGTH_LONG
                        ).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }
}