package com.dialcadev.dialcash.ui.shared.handlers

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.data.dao.AccountBalanceWithOriginal
import com.dialcadev.dialcash.data.entities.Account
import com.dialcadev.dialcash.databinding.RecycleAccountItemBinding
import com.dialcadev.dialcash.domain.AccountType
import com.dialcadev.dialcash.utils.toReadableDate
import com.google.android.material.bottomsheet.BottomSheetDialog

class AccountBottomSheetHandler(
    private val binding: RecycleAccountItemBinding,
    private val account: AccountBalanceWithOriginal,
    private val currencySymbol: String,
    private val onUpdate: (Account) -> Unit,
    private val onDelete: (Account) -> Unit,
    private val dialog: BottomSheetDialog
) {
    private val accountTypes = AccountType.entries.toList()

    fun setup() {
        initializeViews()
        setupValidation()
        setupClickListeners()
    }

    private fun initializeViews() {
        binding.apply {
            tvAccountName.text = account.name
            etEditAccountName.setText(account.name)
            val enumType = AccountType.byCode(account.type)
            tvAccountType.text = root.context.getString(enumType.labelRes)
            "$currencySymbol ${String.format("%.2f", account.originalBalance)}".also { tvAccountBalance.text = it }
            etInitialBalance.setText(account.originalBalance.toString())
            "$currencySymbol ${String.format("%.2f", account.balance)}".also { tvAccountCurrentBalance.text = it }
            binding.tvAccountCurrentBalance.post {
                val paint = binding.tvAccountCurrentBalance.paint
                val width = paint.measureText(binding.tvAccountCurrentBalance.text.toString())
                    val textShader = LinearGradient(
                        0f, 0f, width, 0f,
                        intArrayOf(
                            Color.parseColor("#60A5FA"),
                            Color.parseColor("#cbd5e4")
                        ),
                        null,
                        Shader.TileMode.CLAMP
                    )
                    binding.tvAccountCurrentBalance.paint.shader = textShader
                    binding.tvAccountCurrentBalance.invalidate()
            }

            tilInitialBalance.prefixText = "$currencySymbol "

            tvCreatedAt.text = root.context.getString(
                R.string.created_at,
                account.createdAt?.toReadableDate()
            )
            val iconRes = when (enumType) {
                AccountType.BANK -> R.drawable.ic_bank
                AccountType.CASH -> R.drawable.ic_cash
                AccountType.CARD -> R.drawable.ic_card
                AccountType.WALLET -> R.drawable.ic_accounts_outline
                else -> R.drawable.ic_account_default
            }
            imageAccountIcon.setImageResource(iconRes)
            val labels = accountTypes.map { root.context.getString(it.labelRes) }
            val accountTypeAdapter = ArrayAdapter(
                root.context,
                android.R.layout.simple_dropdown_item_1line,
                labels
            )
            actvAccountType.setAdapter(accountTypeAdapter)
            actvAccountType.setText(root.context.getString(enumType.labelRes), false)
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
                tilAccountName.error = root.context.getString(R.string.name_cannot_be_empty)
                isValid = false
            } else {
                tilAccountName.error = null
            }
            val selectedType = accountTypes.firstOrNull { root.context.getString(it.labelRes) == typeText }
            if (selectedType == null) {
                tilAccountType.error = root.context.getString(R.string.select_valid_acc_type)
                isValid = false
            } else {
                tilAccountType.error = null
            }
            val balance = balanceText.toDoubleOrNull()
            if (balanceText.isEmpty()) {
                tilInitialBalance.error = root.context.getString(R.string.balance_cannot_be_empty)
                isValid = false
            } else if (balance == null) {
                tilInitialBalance.error = root.context.getString(R.string.enter_valid_amount)
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
                val newType = accountTypes.first { root.context.getString(it.labelRes) == newTypeLabel }.code
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