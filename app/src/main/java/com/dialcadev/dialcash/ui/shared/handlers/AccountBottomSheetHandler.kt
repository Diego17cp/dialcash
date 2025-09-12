package com.dialcadev.dialcash.ui.shared.handlers

import android.view.View
import android.widget.ArrayAdapter
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.data.dao.AccountBalanceWithOriginal
import com.dialcadev.dialcash.data.entities.Account
import com.dialcadev.dialcash.databinding.RecycleAccountItemBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

class AccountBottomSheetHandler(
    private val binding: RecycleAccountItemBinding,
    private val account: AccountBalanceWithOriginal,
    private val onUpdate: (Account) -> Unit,
    private val onDelete: (Account) -> Unit,
    private val dialog: BottomSheetDialog
) {
    private val accountTypeLabels =
        arrayOf("Bank", "Cash", "Card", "Wallet", "Debt", "Savings", "Other")
    private val accountTypeMapped = mapOf(
        "Bank" to "bank",
        "Cash" to "cash",
        "Card" to "card",
        "Wallet" to "wallet",
        "Debt" to "debt",
        "Savings" to "savings",
        "Other" to "other"
    )

    fun setup() {
        initializeViews()
        setupValidation()
        setupClickListeners()
    }

    private fun initializeViews() {
        binding.apply {
            tvAccountName.text = account.name
            etEditAccountName.setText(account.name)
            tvAccountType.text = account.type.replaceFirstChar { it.uppercase() }
            tvAccountBalance.text =
                root.context.getString(R.string.currency_format, account.originalBalance)
            etInitialBalance.setText(account.originalBalance.toString())
            tvAccountCurrentBalance.text =
                root.context.getString(R.string.currency_format, account.balance)
            tvCreatedAt.text = "Created at: ${account.createdAt}"
            val iconRes = when (account.type) {
                "bank" -> R.drawable.ic_bank
                "cash" -> R.drawable.ic_cash
                "card" -> R.drawable.ic_card
                "wallet" -> R.drawable.ic_accounts_outline
                else -> R.drawable.ic_account_default
            }
            imageAccountIcon.setImageResource(iconRes)
            val accountTypeAdapter = ArrayAdapter(
                root.context,
                android.R.layout.simple_dropdown_item_1line,
                accountTypeLabels
            )
            actvAccountType.setAdapter(accountTypeAdapter)
            val currentLabel = accountTypeMapped.entries.find { it.value == account.type }?.key
                ?: account.type.replaceFirstChar { it.uppercase() }
            actvAccountType.setText(currentLabel, false)
        }
    }

    private fun setupValidation() {
        binding.apply {
            etEditAccountName.addTextChangedListener { validateForm() }
            etInitialBalance.addTextChangedListener { validateForm() }
            actvAccountType.setOnItemClickListener { _, _, _, _ -> validateForm() }
            validateForm()
        }
    }

    private fun validateForm(): Boolean {
        binding.apply {
            val name = etEditAccountName.text.toString().trim()
            val balanceText = etInitialBalance.text.toString().trim()
            val typeText = actvAccountType.text.toString().trim()
            var isValid = true

            if (name.isEmpty()) {
                tilAccountName.error = "Name cannot be empty"
                isValid = false
            } else {
                tilAccountName.error = null
            }
            if (typeText.isEmpty() || !accountTypeMapped.containsKey(typeText)) {
                tilAccountType.error = "Select a valid account type"
                isValid = false
            } else {
                tilAccountType.error = null
            }
            val balance = balanceText.toDoubleOrNull()
            if (balanceText.isEmpty()) {
                tilInitialBalance.error = "Balance cannot be empty"
                isValid = false
            } else if (balance == null) {
                tilInitialBalance.error = "Enter a valid number"
                isValid = false
            } else {
                tilInitialBalance.error = null
            }
            btnSave.isEnabled = isValid
            return isValid
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            fun resetViews() {
                tvAccountName.visibility = View.VISIBLE
                tilAccountName.visibility = View.GONE
                tvAccountType.visibility = View.VISIBLE
                tilAccountType.visibility = View.GONE
                tvAccountBalance.visibility = View.VISIBLE
                tilInitialBalance.visibility = View.GONE
                actionsRow.visibility = View.VISIBLE
                editFooter.visibility = View.GONE
                deleteConfirmFooter.visibility = View.GONE
            }
            fun editAccount() {
                if (!validateForm()) return
                val newName = etEditAccountName.text.toString().trim()
                val newTypeLabel = actvAccountType.text.toString().trim()
                val newType = accountTypeMapped[newTypeLabel] ?: account.type
                val newBalance = etInitialBalance.text.toString().trim().toDoubleOrNull()
                    ?: account.originalBalance
                onUpdate(
                    Account(
                        id = account.id,
                        name = newName,
                        type = newType,
                        balance = newBalance,
                    )
                )
                resetViews()
                dialog.dismiss()
            }
            btnEdit.setOnClickListener {
                tvAccountName.visibility = View.GONE
                tilAccountName.visibility = View.VISIBLE
                tvAccountType.visibility = View.GONE
                tilAccountType.visibility = View.VISIBLE
                tvAccountBalance.visibility = View.GONE
                tilInitialBalance.visibility = View.VISIBLE
                actionsRow.visibility = View.GONE
                editFooter.visibility = View.VISIBLE
                etEditAccountName.setText(account.name)
                etInitialBalance.setText(account.originalBalance.toString())
                validateForm()
            }
            btnCancel.setOnClickListener {
                if (actionsRow.isGone && editFooter.isVisible) {
                    resetViews()
                } else {
                    dialog.dismiss()
                }
            }
            btnDelete.setOnClickListener {
                actionsRow.visibility = View.GONE
                deleteConfirmFooter.visibility = View.VISIBLE
            }
            btnCancelDelete.setOnClickListener {
                if (deleteConfirmFooter.isVisible && actionsRow.isGone) {
                    resetViews()
                } else {
                    dialog.dismiss()
                }
            }
            btnConfirmDelete.setOnClickListener {
                onDelete(
                    Account(
                        id = account.id,
                        name = account.name,
                        type = account.type,
                        balance = account.originalBalance,
                    )
                )
                dialog.dismiss()
            }
            btnSave.setOnClickListener { editAccount() }
        }
    }
}