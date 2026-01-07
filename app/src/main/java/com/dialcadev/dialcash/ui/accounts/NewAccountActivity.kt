package com.dialcadev.dialcash.ui.accounts

import com.dialcadev.dialcash.R
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.dialcadev.dialcash.data.UserData
import com.dialcadev.dialcash.data.UserDataStore
import com.dialcadev.dialcash.databinding.NewAccountActivityBinding
import com.dialcadev.dialcash.domain.AccountType
import com.dialcadev.dialcash.ui.shared.GridSpacingItemDecoration
import com.google.android.material.R.attr
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NewAccountActivity : AppCompatActivity() {
    private lateinit var binding: NewAccountActivityBinding
    private val viewModel: NewAccountViewModel by viewModels()

    private val accountTypes = AccountType.entries.toList()

    @Inject
    lateinit var userDatStore: UserDataStore
    var userDataStore: UserData? = null


    private var selectedAccountTypeId: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = NewAccountActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(android.R.drawable.ic_menu_close_clear_cancel)
        }
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val spacing = resources.getDimensionPixelSize(R.dimen.spacing_12)
        binding.rvAccountTypes.addItemDecoration(
            GridSpacingItemDecoration(
                spanCount = 2,
                spacing = spacing
            )
        )
        lifecycleScope.launch {
            userDatStore.getUserData().collect { user ->
                userDataStore = user
                binding.tvCurrency.text = user.currencySymbol
            }
        }
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
    private fun showKeyboard(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }
    private fun getThemeColor(attr: Int): Int {
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }
    private fun setupViews() {
        setupDropdownMenu()
        setupValidation()
        binding.rvAccountTypes.adapter = AccountTypeAdapter(com.dialcadev.dialcash.domain.accountTypes) { selected ->
            selectedAccountTypeId = selected.id
            validateForm()
        }
    }
    private fun setupDropdownMenu() {
        val labels = accountTypes.map { getString(it.labelRes) }
        val accountTypeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            labels
        )
    }
    private fun setupValidation() {
        binding.etInitialBalance.addTextChangedListener { validateForm() }
        binding.etAccountName.addTextChangedListener {
            validateForm()
        }
    }
    private fun setupListeners() {
        binding.tvInitialBalance.setOnClickListener {
            binding.etInitialBalance.requestFocus()
            showKeyboard(binding.etInitialBalance)
        }
        binding.etInitialBalance.addTextChangedListener { text ->
            val value = text?.toString() ?: ""
            if (value.isEmpty()) {
                binding.tvInitialBalance.text = "0.00"
                binding.tvInitialBalance.setTextColor(getThemeColor(attr.colorOutline))
            } else {
                val formattedValue = try {
                    val number = value.toDouble()
                    String.format("%.2f", number)
                } catch (e: NumberFormatException) {
                    value
                }
                binding.tvInitialBalance.text = formattedValue
                binding.tvInitialBalance.setTextColor(getThemeColor(attr.colorOnSurface))
                binding.tvInitialBalanceError.visibility = View.GONE
            }
        }
        binding.btnCreateAccount.setOnClickListener { createAccount() }
    }
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when {
                    state.isLoading -> {
                        binding.btnCreateAccount.isEnabled = false
                        binding.btnCreateAccount.text = getString(R.string.creating)
                    }
                    state.isSuccess -> {
                        Toast.makeText(
                            this@NewAccountActivity,
                            getString(R.string.account_created_successfully),
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                    state.errorMessage != null -> {
                        binding.btnCreateAccount.isEnabled = true
                        binding.btnCreateAccount.text = getString(R.string.create_account)
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
    private fun validateForm() : Boolean {
        val accountName = binding.etAccountName.text?.toString()?.trim()
        val initialBalance = binding.etInitialBalance.text?.toString()?.trim()

        var isValid = true

        if (accountName.isNullOrEmpty()) {
            binding.tvNameError.visibility = View.VISIBLE
            binding.etAccountName.background = getDrawable(R.drawable.bg_input_error)
            isValid = false
        } else {
            binding.etAccountName.background = getDrawable(R.drawable.bg_input_rounded)
            binding.tvNameError.visibility = View.GONE
        }

        if (selectedAccountTypeId == null) {
            binding.tvTypeAccountError.visibility = View.VISIBLE
            isValid = false
        } else {
            binding.tvTypeAccountError.visibility = View.GONE
        }

        if (initialBalance.isNullOrEmpty() || initialBalance == "0.00" || initialBalance.toDoubleOrNull() == null) {
            binding.tvInitialBalanceError.visibility = View.VISIBLE
            isValid = false
        } else {
            binding.tvInitialBalanceError.visibility = View.GONE
        }
        binding.btnCreateAccount.isEnabled = isValid
        return isValid
    }
    private fun createAccount() {
        if (!validateForm()) return
        val accountName = binding.etAccountName.text?.toString()?.trim().orEmpty()
        val initialBalance = binding.etInitialBalance.text?.toString()?.toDoubleOrNull() ?: 0.0

        viewModel.createAccount(accountName, selectedAccountTypeId ?: "bank", initialBalance)
    }
}