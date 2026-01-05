package com.dialcadev.dialcash.ui.shared

import android.content.Context
import android.view.LayoutInflater
import com.dialcadev.dialcash.data.dao.AccountBalanceWithOriginal
import com.dialcadev.dialcash.data.dto.TransactionWithDetails
import com.dialcadev.dialcash.data.entities.Account
import com.dialcadev.dialcash.data.entities.IncomeGroup
import com.dialcadev.dialcash.data.entities.Transaction
import com.dialcadev.dialcash.databinding.RecycleAccountItemBinding
import com.dialcadev.dialcash.databinding.RecycleTransactionItemBinding
import com.dialcadev.dialcash.ui.shared.handlers.AccountBottomSheetHandler
import com.dialcadev.dialcash.ui.shared.handlers.TransactionBottomSheetHandler
import com.google.android.material.bottomsheet.BottomSheetDialog

class BottomSheetManager(private val context: Context, private val layoutInflater: LayoutInflater) {
    fun showAccountBottomSheet(
        account: AccountBalanceWithOriginal,
        currencySymbol: String,
        onUpdate: (Account) -> Unit,
        onDelete: (Account) -> Unit
    ) {
        val dialog = BottomSheetDialog(context)
        val binding = RecycleAccountItemBinding.inflate(layoutInflater)

        AccountBottomSheetHandler(binding, account, currencySymbol, onUpdate, onDelete, dialog).setup()
        dialog.setContentView(binding.root)
        dialog.show()
    }

    fun showTransactionBottomSheet(
        transaction: TransactionWithDetails,
        accounts: List<Account>,
        incomeGroups: List<IncomeGroup>,
        currencySymbol: String,
        onUpdate: (Transaction) -> Unit,
        onDelete: (Transaction) -> Unit,
        onValidateBalance: (Int, String, Int, Double, Int?, Int?, (Boolean, String?) -> Unit) -> Unit
    ) {
        val dialog = BottomSheetDialog(context)
        val binding = RecycleTransactionItemBinding.inflate(layoutInflater)
        TransactionBottomSheetHandler(
            binding,
            transaction,
            currencySymbol,
            accounts,
            incomeGroups,
            onUpdate,
            onDelete,
            onValidateBalance,
            dialog
        ).setup()
        dialog.setContentView(binding.root)
        dialog.show()
    }
}