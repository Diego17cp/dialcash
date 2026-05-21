package com.dialcadev.dialcash.core.ui.components

import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.core.utils.extensions.toCurrencyFormat
import com.dialcadev.dialcash.core.utils.extensions.toReadableDate
import com.dialcadev.dialcash.databinding.RecycleAccountItemBinding
import com.dialcadev.dialcash.domain.AccountType
import com.dialcadev.dialcash.features.accounts.domain.dtos.AccountBalanceWithOriginal
import com.dialcadev.dialcash.features.accounts.domain.models.Account
import com.google.android.material.bottomsheet.BottomSheetDialog

fun Context.showAccountDetailsBottomSheet(
    account: AccountBalanceWithOriginal,
    currencySymbol: String,
    onUpdate: (Account) -> Unit,
    onDelete: (Account) -> Unit
) {
    val dialog = BottomSheetDialog(this)
    val binding = RecycleAccountItemBinding.inflate(LayoutInflater.from(this))
    val accountTypes = AccountType.entries.toList()

    fun validateForm(): Boolean {
        binding.apply {
            val name = etEditAccountName.text.toString().trim()
            val balanceText = etInitialBalance.text.toString().trim()
            val typeText = actvAccountType.text.toString().trim()
            var isValid = true

            if (name.isEmpty()) {
                tilAccountName.error = getString(R.string.name_cannot_be_empty)
                isValid = false
            } else tilAccountName.error = null

            val selectedType = accountTypes.firstOrNull { getString(it.labelRes) == typeText }
            if (selectedType == null) {
                tilAccountType.error = getString(R.string.select_valid_acc_type)
                isValid = false
            } else tilAccountType.error = null

            val balance = balanceText.toDoubleOrNull()
            if (balanceText.isEmpty()) {
                tilInitialBalance.error = getString(R.string.balance_cannot_be_empty)
                isValid = false
            } else if (balance == null) {
                tilInitialBalance.error = getString(R.string.enter_valid_amount)
                isValid = false
            } else tilInitialBalance.error = null

            btnSave.isEnabled = isValid
            return isValid
        }
    }

    fun resetViews() {
        binding.apply {
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
    }
    binding.apply {
        tvAccountName.text = account.name
        etEditAccountName.setText(account.name)

        val enumType = AccountType.byCode(account.type)
        tvAccountType.text = getString(enumType.labelRes)

        tvAccountBalance.text = "$currencySymbol ${account.originalBalance.toCurrencyFormat()}"
        etInitialBalance.setText(account.originalBalance.toString())

        tvAccountCurrentBalance.text = "$currencySymbol ${account.balance.toCurrencyFormat()}"
        tvAccountCurrentBalance.post {
            val paint = tvAccountCurrentBalance.paint
            val width = paint.measureText(tvAccountCurrentBalance.text.toString())
            val textShader = LinearGradient(
                0f,
                0f,
                width,
                0f,
                intArrayOf(Color.parseColor("#60A5FA"), Color.parseColor("#cbd5e4")),
                null,
                Shader.TileMode.CLAMP
            )
            binding.tvAccountCurrentBalance.paint.shader = textShader
            binding.tvAccountCurrentBalance.invalidate()
        }
        tilInitialBalance.prefixText = "$currencySymbol "
        tvCreatedAt.text = getString(R.string.created_at, account.createdAt?.toReadableDate())

        val iconRes = when (enumType) {
            AccountType.BANK -> R.drawable.ic_bank
            AccountType.CASH -> R.drawable.ic_cash
            AccountType.CARD -> R.drawable.ic_card
            AccountType.WALLET -> R.drawable.ic_accounts_outline
            else -> R.drawable.ic_account_default
        }
        imageAccountIcon.setImageResource(iconRes)

        val labels = accountTypes.map { getString(it.labelRes) }
        val accountTypeAdapter = ArrayAdapter(
            this@showAccountDetailsBottomSheet,
            android.R.layout.simple_list_item_1,
            labels
        )
        actvAccountType.setAdapter(accountTypeAdapter)
        actvAccountType.setText(getString(enumType.labelRes), false)

        etEditAccountName.addTextChangedListener { validateForm() }
        etInitialBalance.addTextChangedListener { validateForm() }
        actvAccountType.setOnItemClickListener { _, _, _, _ -> validateForm() }
        validateForm()

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
            if (actionsRow.isGone && editFooter.isVisible) resetViews()
            else dialog.dismiss()
        }
        btnDelete.setOnClickListener {
            actionsRow.visibility = View.GONE
            deleteConfirmFooter.visibility = View.VISIBLE
        }
        btnCancelDelete.setOnClickListener {
            if (deleteConfirmFooter.isVisible && actionsRow.isGone) resetViews()
            else dialog.dismiss()
        }
        btnConfirmDelete.setOnClickListener {
            onDelete(
                Account(
                    id = account.id,
                    name = account.name,
                    type = account.type,
                    balance = account.originalBalance
                )
            )
            dialog.dismiss()
        }
        btnSave.setOnClickListener {
            if (!validateForm()) return@setOnClickListener
            val newName = etEditAccountName.text.toString().trim()
            val newTypeLabel = actvAccountType.text.toString().trim()
            val newType = accountTypes.first { getString(it.labelRes) == newTypeLabel }
            val newBalance =
                etInitialBalance.text.toString().trim().toDoubleOrNull() ?: account.originalBalance
            onUpdate(
                Account(
                    id = account.id,
                    name = newName,
                    type = newType.code,
                    balance = newBalance
                )
            )
            resetViews()
            dialog.dismiss()
        }
    }
    dialog.setContentView(binding.root)
    dialog.show()
}