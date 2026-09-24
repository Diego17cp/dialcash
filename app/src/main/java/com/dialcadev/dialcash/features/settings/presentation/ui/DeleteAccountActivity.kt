package com.dialcadev.dialcash.features.settings.presentation.ui

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.databinding.DeleteAccountActivityBinding
import com.dialcadev.dialcash.features.settings.presentation.viewmodels.DeleteAccountViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch
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
import androidx.core.view.WindowCompat
import androidx.core.view.updatePadding
import com.dialcadev.dialcash.core.theme.AppTypography
import com.dialcadev.dialcash.core.ui.components.LiquidAppBar
import dev.chrisbanes.haze.hazeSource

@AndroidEntryPoint
class DeleteAccountActivity : AppCompatActivity() {
    lateinit var binding: DeleteAccountActivityBinding
    private val hazeState = HazeState()

    private val viewModel: DeleteAccountViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = DeleteAccountActivityBinding.inflate(layoutInflater)
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
                            title = getString(R.string.delete_account),
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
        binding.btnDelete.setOnClickListener {
            if (!viewModel.uiState.value.isCheckboxChecked) {
                Toast.makeText(
                    this,
                    getString(R.string.please_confirm_to_proceed),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            viewModel.deleteAccount()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.confirmCheckbox.isChecked = state.isCheckboxChecked
                    binding.btnDelete.isEnabled = !state.isLoading
                    if (state.isSuccess) {
                        Toast.makeText(
                            this@DeleteAccountActivity,
                            getString(R.string.account_deleted_successfully),
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }

                    state.errorMessage?.let { error ->
                        Toast.makeText(this@DeleteAccountActivity, error, Toast.LENGTH_LONG).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }
}