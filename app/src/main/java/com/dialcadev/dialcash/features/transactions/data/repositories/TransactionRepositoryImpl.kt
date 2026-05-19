package com.dialcadev.dialcash.features.transactions.data.repositories

import com.dialcadev.dialcash.features.transactions.data.dao.TransactionDao
import com.dialcadev.dialcash.features.transactions.domain.dtos.TransactionWithDetails
import com.dialcadev.dialcash.features.transactions.domain.dtos.TransferHistory
import com.dialcadev.dialcash.features.transactions.domain.models.Transaction
import com.dialcadev.dialcash.features.transactions.domain.repositories.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
): TransactionRepository {
    override suspend fun insertTransaction(
        amount: Double,
        type: String,
        description: String,
        date: Long,
        accountId: Int,
        transferAccountId: Int?,
        relatedIncomeId: Int?
    ) {
        transactionDao.insert(
            accountId = accountId,
            type = type,
            amount = amount,
            date = date,
            description = description,
            relatedIncomeId = relatedIncomeId,
            transferAccountId = transferAccountId
        )
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.update(
            transaction.id,
            transaction.amount,
            transaction.type,
            transaction.description ?: "",
            transaction.date,
            transaction.accountId,
            transaction.transferAccountId,
            transaction.relatedIncomeId
        )
    }
    override suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.delete(transaction)
    }
    override suspend fun getTransactionById(transactionId: Int): Transaction? = transactionDao.getTransactionById(transactionId)
    override suspend fun getTotalTransactions(): Int = transactionDao.getTransactionCount()
    override suspend fun insertTransactionsBatch(transactions: List<Transaction>) {
        transactions.forEach { transaction ->
            transactionDao.insert(
                accountId = transaction.accountId,
                type = transaction.type,
                amount = transaction.amount,
                date = transaction.date,
                description = transaction.description ?: "",
                relatedIncomeId = transaction.relatedIncomeId,
                transferAccountId = transaction.transferAccountId
            )
        }
    }
    override fun getAllTransactions(): Flow<List<TransactionWithDetails>> = transactionDao.getTransactionWithDetails()
    override fun getTransferTransactions(): Flow<List<TransferHistory>> = transactionDao.getTransferTransactions()
    override fun getTransactionBetween(startDate: Long, endDate: Long): Flow<List<Transaction>> = transactionDao.getTransactionsBetween(startDate, endDate)
    override fun searchTransactions(query: String): Flow<List<Transaction>> = transactionDao.searchTransactions(query)
    override fun getTransactionsForIncomeGroup(incomeGroupId: Int): Flow<List<Transaction>> = transactionDao.getExpensesByIncomeGroup(incomeGroupId)
    override fun getTransactionsForAccount(accountId: Int): Flow<List<Transaction>> = transactionDao.getTransactionsForAccount(accountId)
    override fun getTotalIncome(): Flow<Double> = transactionDao.getTotalIncome()
    override fun getTotalExpense(): Flow<Double> = transactionDao.getTotalExpense()
    override fun getRecentTransactions(limit: Int): Flow<List<TransactionWithDetails>> = transactionDao.getRecentTransactions(limit)
}