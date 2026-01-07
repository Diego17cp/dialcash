package com.dialcadev.dialcash

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.dialcadev.dialcash.data.UserData
import com.dialcadev.dialcash.data.UserDataStore
import com.dialcadev.dialcash.databinding.NewIncomeActivityBinding
import com.dialcadev.dialcash.ui.incomegroup.NewIncomeViewModel
import com.google.android.material.R.attr
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NewIncomeActivity : AppCompatActivity() {
    private lateinit var binding: NewIncomeActivityBinding
    @Inject
    lateinit var userDataStore: UserDataStore
    var userData: UserData? = null
    private val viewModel: NewIncomeViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = NewIncomeActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(android.R.drawable.ic_menu_close_clear_cancel)
        }
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        lifecycleScope.launch {
            userDataStore.getUserData().collect { user ->
                userData = user
                binding.tvCurrency.text = user.currencySymbol
            }
        }
        setupValidation()
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
    private fun setupValidation() {
        binding.etIncomeName.addTextChangedListener { validateForm() }
        binding.etAmount.addTextChangedListener { validateForm() }
    }
    private fun setupListeners() {
        binding.tvAmount.setOnClickListener {
            binding.etAmount.requestFocus()
            showKeyboard(binding.etAmount)
        }
        binding.etAmount.addTextChangedListener { text ->
            val value = text?.toString() ?: ""
            if (value.isEmpty()) {
                binding.tvAmount.text = "0.00"
                binding.tvAmount.setTextColor(getThemeColor(attr.colorOutline))
            } else {
                val formattedValue = try {
                    val number = value.toDouble()
                    String.format("%.2f", number)
                } catch (e: NumberFormatException) {
                    value
                }
                binding.tvAmount.text = formattedValue
                binding.tvAmount.setTextColor(getThemeColor(attr.colorOnSurface))
                binding.tvAmountError.visibility = View.GONE
            }
        }
        binding.btnCreateIncome.setOnClickListener { createIncome() }
    }
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when {
                    state.isLoading -> {
                        binding.btnCreateIncome.isEnabled = false
                        binding.btnCreateIncome.text = getString(R.string.creating)
                    }
                    state.isSuccess -> {
                        Toast.makeText(
                            this@NewIncomeActivity,
                            getString(R.string.income_group_created_successfully),
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                    state.errorMessage != null -> {
                        binding.btnCreateIncome.isEnabled = true
                        binding.btnCreateIncome.text = getString(R.string.create)
                        Toast.makeText(
                            this@NewIncomeActivity,
                            state.errorMessage,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
    private fun validateForm(): Boolean {
        val name = binding.etIncomeName.text?.toString()?.trim()
        val amountText = binding.etAmount.text?.toString()?.trim()
        var isValid = true
        if (name.isNullOrEmpty()) {
            binding.tvNameError.visibility = View.VISIBLE
            isValid = false
        } else binding.tvNameError.visibility = View.GONE
        if (amountText.isNullOrEmpty() || amountText == "0.00" || amountText.toDoubleOrNull() == null) {
            binding.tvAmountError.visibility = View.VISIBLE
            isValid = false
        } else binding.tvAmountError.visibility = View.GONE
        binding.btnCreateIncome.isEnabled = isValid
        return isValid
    }
    private fun createIncome() {
        if (!validateForm()) return
        val name = binding.etIncomeName.text?.toString()?.trim() ?: ""
        val amount = binding.etAmount.text?.toString()?.trim()?.toDoubleOrNull() ?: 0.0
        viewModel.createIncomeGroup(name, amount)
    }
}