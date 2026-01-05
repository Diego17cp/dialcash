package com.dialcadev.dialcash.ui.shared.handlers

import android.view.View
import android.widget.ArrayAdapter
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.data.dto.TransactionWithDetails
import com.dialcadev.dialcash.data.entities.Account
import com.dialcadev.dialcash.data.entities.IncomeGroup
import com.dialcadev.dialcash.data.entities.Transaction
import com.dialcadev.dialcash.databinding.RecycleTransactionItemBinding
import com.dialcadev.dialcash.utils.toReadableDate
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.collections.find

class TransactionBottomSheetHandler(
    private val binding: RecycleTransactionItemBinding,
    private val transaction: TransactionWithDetails,
    private val currencySymbol: String,
    private val accounts: List<Account>,
    private val incomeGroups: List<IncomeGroup>,
    private val onUpdate: (Transaction) -> Unit,
    private val onDelete: (Transaction) -> Unit,
    private val onValidateBalance: (Int, String, Int, Double, Int?, Int?, (Boolean, String?) -> Unit) -> Unit,
    private val dialog: BottomSheetDialog
) {
    private var selectedAccount: Account? = null
    private var selectedAccountTo: Account? = null
    private var selectedIncomeGroup: IncomeGroup? = null
    fun setup() {
        initializeViews()
        setupAdapters()
        setupValidation()
        setupClickListeners()
    }

    private fun initializeViews() {
        binding.apply {
            val iconRes = when (transaction.type) {
                "income" -> R.drawable.ic_income
                "expense" -> R.drawable.ic_expense
                else -> R.drawable.ic_transactions_outline
            }
            val color = when (transaction.type) {
                "income" -> R.color.positive_amount
                "transfer" -> R.color.colorPrimary
                else -> R.color.negative_amount
            }
            val amountText = if (transaction.type == "income") "+$currencySymbol ${transaction.amount}"
            else "-$currencySymbol ${transaction.amount}"
            ivTransactionType.setImageResource(iconRes)
            ivTransactionType.setColorFilter(root.context.getColor(color))
            tvTransactionAmount.text = amountText
            tvTransactionAmount.setTextColor(root.context.getColor(color))
            etTransactionAmount.setText(transaction.amount.toString())
            tilTransactionAmount.prefixText = "$currencySymbol "
            tvTransactionDescription.text = transaction.description
            etTransactionDescription.setText(transaction.description)
            tvAccountName.text = transaction.accountName
            if (transaction.type == "transfer" && transaction.accountToName != null) layoutTransferTo.visibility =
                View.VISIBLE
            tvAccountToName.text = transaction.accountToName ?: "N/A"
            if (transaction.type == "expense" && transaction.incomeGroupName != null) layoutIncomeGroup.visibility =
                View.VISIBLE
            tvIncomeGroupName.text = transaction.incomeGroupName ?: "N/A"
            tvTransactionDate.text = transaction.date.toReadableDate()
        }
    }

    private fun setupAdapters() {
        binding.apply {
            val accountNames = accounts.map { it.name }
            val accountAdapter = ArrayAdapter(
                root.context,
                android.R.layout.simple_dropdown_item_1line,
                accountNames
            )
            actvAccountName.setAdapter(accountAdapter)
            actvAccountToName.setAdapter(accountAdapter)
            actvAccountName.setText(transaction.accountName, false)
            actvAccountToName.setText(transaction.accountToName ?: "", false)

            val incomeGroupNames = incomeGroups.map { it.name }
            val incomeGroupAdapter = ArrayAdapter(
                root.context,
                android.R.layout.simple_dropdown_item_1line,
                incomeGroupNames
            )
            actvIncomeGroupName.setAdapter(incomeGroupAdapter)
            actvIncomeGroupName.setText(transaction.incomeGroupName ?: "", false)
        }
    }

    private fun setupValidation() {
        binding.apply {
            etTransactionAmount.addTextChangedListener { validateForm() }
            etTransactionDescription.addTextChangedListener { validateForm() }
            actvAccountName.setOnItemClickListener { _, _, position, _ ->
                val selectedName = actvAccountName.adapter.getItem(position) as String
                selectedAccount =
                    accounts.find { it.name == selectedName }
                validateForm()
            }
            actvAccountToName.setOnItemClickListener { _, _, position, _ ->
                val selectedName = actvAccountToName.adapter.getItem(position) as String
                selectedAccountTo =
                    accounts.find { it.name == selectedName }
                validateForm()
            }
            actvIncomeGroupName.setOnItemClickListener { _, _, position, _ ->
                val selectedName = actvIncomeGroupName.adapter.getItem(position) as String
                selectedIncomeGroup =
                    incomeGroups.find { it.name == selectedName }
                validateForm()
            }
            validateForm()
        }
    }

    private fun validateForm(): Boolean {
        binding.apply {
            val amountText = etTransactionAmount.text.toString().trim()
            val descriptionText = etTransactionDescription.text.toString().trim()
            val accountText = actvAccountName.text.toString().trim()
            var isValid = true
            if (amountText.isEmpty()) {
                tilTransactionAmount.error = root.context.getString(R.string.amount_required)
                isValid = false
            } else if (amountText.toDoubleOrNull() == null || amountText.toDouble() <= 0) {
                tilTransactionAmount.error = root.context.getString(R.string.enter_valid_amount)
                isValid = false
            } else {
                tilTransactionAmount.error = null
            }
            if (descriptionText.isEmpty()) {
                tilTransactionDescription.error = root.context.getString(R.string.description_cannot_be_empty)
                isValid = false
            } else {
                tilTransactionDescription.error = null
            }
            if (accountText.isEmpty() || accounts.none { it.name == accountText }) {
                tilAccountName.error = root.context.getString(R.string.select_valid_acc)
                isValid = false
            } else {
                tilAccountName.error = null
            }
            if (transaction.type == "transfer") {
                val toAccountText = actvAccountToName.text.toString().trim()
                if (toAccountText.isEmpty() || accounts.none { it.name == toAccountText }) {
                    tilAccountToName.error = root.context.getString(R.string.select_valid_acc)
                    isValid = false
                } else if ((selectedAccount != null && selectedAccountTo != null) && (selectedAccount!!.id == selectedAccountTo!!.id)) {
                    tilAccountToName.error = root.context.getString(R.string.cannot_transfer_same_acc)
                    isValid = false
                } else {
                    tilAccountToName.error = null
                }
            }
            if (transaction.type == "expense") {
                val groupText = actvIncomeGroupName.text.toString().trim()
                if (groupText.isNotEmpty() && incomeGroups.none { it.name == groupText }) {
                    tilIncomeGroupName.error = root.context.getString(R.string.select_valid_income_group)
                    isValid = false
                } else {
                    tilIncomeGroupName.error = null
                }
            }
            if (isValid && (transaction.type == "expense" || transaction.type == "transfer")) {
                val amount = amountText.toDouble()
                val accountId = accounts.find { it.name == accountText }?.id ?: return false
                val accountToId = if (transaction.type == "transfer") {
                    val toAccountText = actvAccountToName.text.toString().trim()
                    accounts.find { it.name == toAccountText }?.id
                } else null
                val incomeGroupId = if (transaction.type == "expense") {
                    val groupText = actvIncomeGroupName.text.toString().trim()
                    if (groupText.isNotEmpty()) incomeGroups.find { it.name == groupText }?.id else null
                } else null
                btnSave.isEnabled = false
                onValidateBalance(
                    transaction.id,
                    transaction.type,
                    accountId,
                    amount,
                    accountToId,
                    incomeGroupId
                ) { valid, msg ->
                    if (valid) {
                        tilAccountName.error = null
                        tilIncomeGroupName.error = null
                        tilAccountToName.error = null
                        btnSave.isEnabled = true
                    } else {
                        when (transaction.type) {
                            "expense" -> {
                                if (msg?.contains("account", ignoreCase = true) == true) {
                                    tilAccountName.error = msg
                                } else {
                                    tilIncomeGroupName.error = msg
                                }
                            }

                            "transfer" -> {
                                tilAccountName.error = msg
                            }
                        }
                        btnSave.isEnabled = false
                    }
                }
            } else {
                btnSave.isEnabled = isValid
            }
            return isValid
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            fun resetView() {
                tvTransactionAmount.visibility = View.VISIBLE
                tilTransactionAmount.visibility = View.GONE
                tvTransactionDescription.visibility = View.VISIBLE
                tilTransactionDescription.visibility = View.GONE
                tvAccountName.visibility = View.VISIBLE
                tilAccountName.visibility = View.GONE
                if (transaction.type == "transfer" && transaction.accountToName != null) {
                    tvAccountToName.visibility = View.VISIBLE
                    tilAccountToName.visibility = View.GONE
                }
                if (transaction.type == "expense" && transaction.incomeGroupName != null) {
                    tvIncomeGroupName.visibility = View.VISIBLE
                    tilIncomeGroupName.visibility = View.GONE
                }
                actionsRow.visibility = View.VISIBLE
                editFooter.visibility = View.GONE
                deleteConfirmFooter.visibility = View.GONE
            }

            fun editTransaction() {
                if (!validateForm()) return
                val newAmount = etTransactionAmount.text.toString().trim().toDouble()
                val newDescription = etTransactionDescription.text.toString().trim()
                val selectedAccountName = actvAccountName.text.toString().trim()
                val newAccountId = accounts.find { it.name == selectedAccountName }?.id ?: return
                val newAccountToId = if (transaction.type == "transfer") {
                    val selectedToAccountName = actvAccountToName.text.toString().trim()
                    accounts.find { it.name == selectedToAccountName }?.id
                } else null
                val newIncomeGroupId = if (transaction.type == "expense") {
                    val selectedGroupName = actvIncomeGroupName.text.toString().trim()
                    incomeGroups.find { it.name == selectedGroupName }?.id
                } else null
                if (transaction.type == "income") {
                    onUpdate(
                        Transaction(
                            id = transaction.id,
                            amount = newAmount,
                            type = transaction.type,
                            date = transaction.date,
                            description = newDescription,
                            accountId = newAccountId,
                            transferAccountId = newAccountToId,
                            relatedIncomeId = newIncomeGroupId
                        )
                    )
                    resetView()
                    dialog.dismiss()
                    return
                }
                btnSave.isEnabled = false
                onValidateBalance(
                    transaction.id,
                    transaction.type,
                    newAccountId,
                    newAmount,
                    newAccountToId,
                    newIncomeGroupId
                ) { valid, _ ->
                    if (valid) {
                        onUpdate(
                            Transaction(
                                id = transaction.id,
                                amount = newAmount,
                                type = transaction.type,
                                date = transaction.date,
                                description = newDescription,
                                accountId = newAccountId,
                                transferAccountId = newAccountToId,
                                relatedIncomeId = newIncomeGroupId
                            )
                        )
                        resetView()
                        dialog.dismiss()
                    }
                }

            }

            fun deleteTransaction() {
                val txId = transaction.id ?: run {
                    dialog.dismiss()
                    return
                }
                val accountId = accounts.find { it.name == transaction.accountName }?.id ?: run {
                    dialog.dismiss()
                    return
                }
                val accountToId = if (transaction.type == "transfer" && transaction.accountToName != null) {
                    accounts.find { it.name == transaction.accountToName }?.id
                } else null
                val incomeGroupId = if (transaction.type == "expense" && transaction.incomeGroupName != null) {
                    incomeGroups.find { it.name == transaction.incomeGroupName }?.id
                } else null
                onDelete(
                    Transaction(
                        id = txId,
                        amount = transaction.amount,
                        type = transaction.type ?: "expense",
                        date = transaction.date,
                        description = transaction.description ?: "",
                        accountId = accountId,
                        transferAccountId = accountToId,
                        relatedIncomeId = incomeGroupId
                    )
                )
                dialog.dismiss()
            }
            btnEdit.setOnClickListener {
                tvTransactionAmount.visibility = View.GONE
                tilTransactionAmount.visibility = View.VISIBLE
                tvTransactionDescription.visibility = View.GONE
                tilTransactionDescription.visibility = View.VISIBLE
                tvAccountName.visibility = View.GONE
                tilAccountName.visibility = View.VISIBLE
                if (transaction.type == "transfer" && transaction.accountToName != null) {
                    tvAccountToName.visibility = View.GONE
                    tilAccountToName.visibility = View.VISIBLE
                }
                if (transaction.type == "expense" && transaction.incomeGroupName != null) {
                    tvIncomeGroupName.visibility = View.GONE
                    tilIncomeGroupName.visibility = View.VISIBLE
                }
                actionsRow.visibility = View.GONE
                editFooter.visibility = View.VISIBLE
            }
            btnCancel.setOnClickListener {
                if (actionsRow.isGone && editFooter.isVisible) {
                    resetView()
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
                    resetView()
                } else {
                    dialog.dismiss()
                }
            }
            btnConfirmDelete.setOnClickListener { deleteTransaction() }
            btnSave.setOnClickListener { editTransaction() }
        }
    }
}