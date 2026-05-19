package com.dialcadev.dialcash.features.transactions.domain.repositories

import com.dialcadev.dialcash.features.transactions.domain.dtos.TransactionWithDetails
import com.dialcadev.dialcash.features.transactions.domain.dtos.TransferHistory
import com.dialcadev.dialcash.features.transactions.domain.models.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    suspend fun insertTransaction(
        amount: Double,
        type: String,
        description: String,
        date: Long,
        accountId: Int,
        transferAccountId: Int? = null,
        relatedIncomeId: Int? = null
    )

    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
    suspend fun getTransactionById(transactionId: Int): Transaction?
    suspend fun getTotalTransactions(): Int
    suspend fun insertTransactionsBatch(transactions: List<Transaction>)

    fun getAllTransactions(): Flow<List<TransactionWithDetails>>
    fun getTransferTransactions(): Flow<List<TransferHistory>>
    fun getTransactionBetween(startDate: Long, endDate: Long): Flow<List<Transaction>>
    fun searchTransactions(query: String): Flow<List<Transaction>>
    fun getTransactionsForIncomeGroup(incomeGroupId: Int): Flow<List<Transaction>>
    fun getTransactionsForAccount(accountId: Int): Flow<List<Transaction>>
    fun getTotalIncome(): Flow<Double>
    fun getTotalExpense(): Flow<Double>
    fun getRecentTransactions(limit: Int): Flow<List<TransactionWithDetails>>
}