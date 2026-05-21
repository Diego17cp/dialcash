package com.dialcadev.dialcash.features.accounts.presentation.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.core.datastore.UserDataStore
import com.dialcadev.dialcash.core.models.UserPreferences
import com.dialcadev.dialcash.core.utils.extensions.toCurrencyFormat
import com.dialcadev.dialcash.databinding.NewAccountActivityBinding
import com.dialcadev.dialcash.features.accounts.presentation.adapter.AccountTypeAdapter
import com.dialcadev.dialcash.features.accounts.presentation.provider.AccountTypeUIProvider
import com.dialcadev.dialcash.features.accounts.presentation.viewmodels.CreateAccountViewModel
import com.dialcadev.dialcash.ui.shared.GridSpacingItemDecoration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NewAccountActivity: AppCompatActivity() {
    private lateinit var binding: NewAccountActivityBinding
    private val viewModel: CreateAccountViewModel by viewModels()
    @Inject
    lateinit var userDataStore: UserDataStore
    var userPreferences: UserPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = NewAccountActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        setupViews()
        setupListeners()
        observeViewModel()
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(android.R.drawable.ic_menu_close_clear_cancel)
        }
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
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