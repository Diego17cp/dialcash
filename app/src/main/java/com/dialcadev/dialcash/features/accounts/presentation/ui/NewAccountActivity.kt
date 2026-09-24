package com.dialcadev.dialcash.features.accounts.presentation.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.core.datastore.UserDataStore
import com.dialcadev.dialcash.core.models.UserPreferences
import com.dialcadev.dialcash.core.theme.AppTypography
import com.dialcadev.dialcash.core.ui.components.LiquidAppBar
import com.dialcadev.dialcash.core.utils.extensions.toCurrencyFormat
import com.dialcadev.dialcash.databinding.NewAccountActivityBinding
import com.dialcadev.dialcash.features.accounts.presentation.adapter.AccountTypeAdapter
import com.dialcadev.dialcash.features.accounts.presentation.provider.AccountTypeUIProvider
import com.dialcadev.dialcash.features.accounts.presentation.viewmodels.CreateAccountViewModel
import com.dialcadev.dialcash.core.ui.shared.GridSpacingItemDecoration
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NewAccountActivity: AppCompatActivity() {
    private lateinit var binding: NewAccountActivityBinding
    private val hazeState = HazeState()
    private val viewModel: CreateAccountViewModel by viewModels()
    @Inject
    lateinit var userDataStore: UserDataStore
    var userPreferences: UserPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = NewAccountActivityBinding.inflate(layoutInflater)
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
                            title = getString(R.string.new_acc_title),
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
        observeViewModel()
    }
    private fun setupViews() {
        val spacing = resources.getDimensionPixelSize(R.dimen.spacing_12)
        binding.rvAccountTypes.addItemDecoration(
            GridSpacingItemDecoration(
                spanCount = 2,
                spacing = spacing
            )
        )
        binding.rvAccountTypes.adapter = AccountTypeAdapter(AccountTypeUIProvider.getItems()) { selected ->
            viewModel.onTypeChanged(selected.id)
        }
        lifecycleScope.launch {
            userDataStore.getUserData().collect { preferences ->
                userPreferences = preferences
                binding.tvCurrency.text = preferences.currencySymbol
            }
        }
    }
    private fun setupListeners() {
        binding.etAccountName.addTextChangedListener { text ->
            viewModel.onNameChanged(text?.toString()?.trim() ?: "")
        }
        binding.etInitialBalance.addTextChangedListener { text ->
            val value = text?.toString()?.trim() ?: ""
            viewModel.onBalanceChanged(value)
            if (value.isEmpty()) {
                binding.tvInitialBalance.text = "0.00"
                binding.tvInitialBalance.setTextColor(getThemeColor(com.google.android.material.R.attr.colorOutline))
            } else {
                val formattedValue = try {
                    value.toDouble().toCurrencyFormat()
                } catch (e: NumberFormatException) {
                    value
                }
                binding.tvInitialBalance.text = formattedValue
                binding.tvInitialBalance.setTextColor(getThemeColor(com.google.android.material.R.attr.colorOnSurface))
                binding.tvInitialBalanceError.visibility = View.GONE
            }
        }
        binding.tvInitialBalance.setOnClickListener {
            binding.etInitialBalance.requestFocus()
            showKeyboard(binding.etInitialBalance)
        }
        binding.btnCreateAccount.setOnClickListener { viewModel.createAccount() }
    }
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.tvNameError.apply {
                        visibility = if (state.nameError != null) View.VISIBLE else View.GONE
                        if (state.nameError != null) text = getString(state.nameError)
                    }
                    binding.etAccountName.background = AppCompatResources.getDrawable(
                        this@NewAccountActivity,
                        if (state.nameError != null) R.drawable.bg_input_error else R.drawable.bg_input_rounded
                    )
                    binding.tvTypeAccountError.visibility = if (state.typeError != null) View.VISIBLE else View.GONE
                    if (state.typeError != null) binding.tvTypeAccountError.text = getString(state.typeError)
                    binding.tvInitialBalanceError.visibility = if (state.balanceError != null) View.VISIBLE else View.GONE
                    if (state.balanceError != null) binding.tvInitialBalanceError.text = getString(state.balanceError)
                    binding.btnCreateAccount.isEnabled = state.isFormValid && !state.isLoading
                    binding.btnCreateAccount.text = if (state.isLoading) getString(R.string.creating) else getString(R.string.create_account)
                    if (state.isSuccess) {
                        Toast.makeText(
                            this@NewAccountActivity,
                            getString(R.string.account_created_successfully),
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                    if (state.errorMessage != null && !state.isLoading) {
                        Toast.makeText(
                            this@NewAccountActivity,
                            "Error ${state.errorMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }
    private fun showKeyboard(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }
    private fun getThemeColor(attr: Int): Int {
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }
}