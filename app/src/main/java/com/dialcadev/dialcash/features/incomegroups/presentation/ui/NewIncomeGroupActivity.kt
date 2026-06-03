package com.dialcadev.dialcash.features.incomegroups.presentation.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dialcadev.dialcash.R
import com.google.android.material.R.attr
import com.dialcadev.dialcash.core.datastore.UserDataStore
import com.dialcadev.dialcash.core.models.UserPreferences
import com.dialcadev.dialcash.core.utils.extensions.toCurrencyFormat
import com.dialcadev.dialcash.databinding.NewIncomeActivityBinding
import com.dialcadev.dialcash.features.incomegroups.presentation.viewmodels.CreateIncomeGroupViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NewIncomeGroupActivity : AppCompatActivity() {
    private lateinit var binding: NewIncomeActivityBinding
    @Inject
    lateinit var userDataStore: UserDataStore
    var preferences: UserPreferences? = null
    private val viewModel: CreateIncomeGroupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = NewIncomeActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
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
    private fun showKeyboard(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }
    private fun getThemeColor(attr: Int): Int {
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }
    private fun validateForm(): Boolean {
        val name = binding.etIncomeName.text?.toString()?.trim()
        val amountText = binding.etAmount.text?.toString()?.trim()
        var isValid = true
        if (name.isNullOrEmpty()) {
            binding.tvNameError.visibility = View.VISIBLE
            binding.etIncomeName.background = getDrawable(R.drawable.bg_input_error)
            isValid = false
        } else {
            binding.etIncomeName.background = getDrawable(R.drawable.bg_input_rounded)
            binding.tvNameError.visibility = View.GONE
        }
        if (amountText.isNullOrEmpty() || amountText == "0.00" || amountText.toDoubleOrNull() == null || amountText.toDouble() < 0.0) {
            binding.tvAmountError.visibility = View.VISIBLE
            isValid = false
        } else binding.tvAmountError.visibility = View.GONE
        return isValid
    }
    private fun setupListeners() {
        binding.tvAmount.setOnClickListener {
            binding.etAmount.requestFocus()
            showKeyboard(binding.etAmount)
        }
        binding.etAmount.addTextChangedListener { text ->
            val value = text?.toString()?.filter { it.isDigit() } ?: ""
            if (value.isEmpty()) {
                binding.tvAmount.text = "0.00"
                binding.tvAmount.setTextColor(getThemeColor(attr.colorOutline))
            } else {
                val formattedValue = try {
                    val number = value.toDouble()
                    number.toCurrencyFormat()
                } catch (e: NumberFormatException) {
                    value
                }
                binding.tvAmount.text = formattedValue
                binding.tvAmount.setTextColor(getThemeColor(attr.colorOnSurface))
                binding.tvAmountError.visibility = View.GONE
            }
        }
        binding.etIncomeName.addTextChangedListener { validateForm() }
        binding.etAmount.addTextChangedListener { validateForm() }
        binding.btnCreateIncome.setOnClickListener {
            if (!validateForm()) return@setOnClickListener
            val amount = binding.etAmount.text.toString().filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
            viewModel.createIncomeGroup(
                name = binding.etIncomeName.text.toString().trim(),
                amount = amount
            )
        }
    }
    private fun observeViewModel() {
        lifecycleScope.launch {
            userDataStore.getUserData().collect { userPreferences ->
                preferences = userPreferences
                binding.tvCurrency.text = userPreferences.currencySymbol
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.btnCreateIncome.isEnabled = !state.isLoading
                    binding.btnCreateIncome.text = if (state.isLoading) getString(R.string.creating) else getString(R.string.create)
                    if (state.isSuccess) {
                        Toast.makeText(
                            this@NewIncomeGroupActivity,
                            getString(R.string.income_group_created_successfully),
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                    if (state.errorMessage != null && !state.isLoading) {
                        Toast.makeText(
                            this@NewIncomeGroupActivity,
                            "Error ${state.errorMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }
}