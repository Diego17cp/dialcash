package com.dialcadev.dialcash.ui.shared.contracts

import com.dialcadev.dialcash.data.entities.Account
import com.dialcadev.dialcash.data.entities.Transaction

interface TransactionsOperations {
    fun updateTransaction(transaction: Transaction)
    fun deleteTransaction(transaction: Transaction)
    fun validateTransactionBalance(
        transactionId: Int, type: String, accountId: Int, amount: Double,
        accountToId: Int?, incomeGroupId: Int?, onResult: (Boolean, String?) -> Unit
    )
}
interface AccountOperations {
    fun updateAccount(account: Account)
    fun deleteAccount(account: Account)
}