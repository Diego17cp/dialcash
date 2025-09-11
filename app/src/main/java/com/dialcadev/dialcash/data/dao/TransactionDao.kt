package com.dialcadev.dialcash.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

import com.dialcadev.dialcash.data.dto.TransactionWithDetails
import com.dialcadev.dialcash.data.dto.TransferHistory
import com.dialcadev.dialcash.data.entities.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("INSERT INTO transactions (amount, type, description, date, account_id, transfer_account_id, related_income_id) VALUES (:amount, :type, :description, :date, :accountId, :transferAccountId, :relatedIncomeId)")
    suspend fun insert(
        amount: Double,
        type: String,
        description: String,
        date: Long,
        accountId: Int,
        transferAccountId: Int?,
        relatedIncomeId: Int?
    ): Long

    @Query("UPDATE transactions SET amount = :amount, type = :type, description = :description, date = :date, account_id = :accountId, transfer_account_id = :transferAccountId, related_income_id = :relatedIncomeId WHERE id = :id")
    suspend fun update(
        id: Int,
        amount: Double,
        type: String,
        description: String,
        date: Long,
        accountId: Int,
        transferAccountId: Int?,
        relatedIncomeId: Int?
    )

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :transactionId LIMIT 1")
    suspend fun getTransactionById(transactionId: Int): Transaction?

    @Query("SELECT * FROM transactions WHERE account_id = :accountId ORDER BY date DESC")
    fun getTransactionsForAccount(accountId: Int): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'income'")
    fun getTotalIncome(): Flow<Double>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'expense'")
    fun getTotalExpense(): Flow<Double>

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int

    @Query("SELECT * FROM transactions WHERE description LIKE :query ORDER BY date DESC")
    fun searchTransactions(query: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE related_income_id = :incomeGroupId AND type = 'expense' ORDER BY date DESC")
    fun getExpensesByIncomeGroup(incomeGroupId: Int): Flow<List<Transaction>>

    @Query(
        """
        SELECT t.id, t.amount, t.type, t.date, t.description,
               a1.name AS accountName,
               a2.name AS accountToName,
               ig.name AS incomeGroupName
        FROM transactions t
        JOIN accounts a1 ON t.account_id = a1.id
        LEFT JOIN accounts a2 ON t.transfer_account_id = a2.id
        LEFT JOIN income_groups ig ON t.related_income_id = ig.id
        ORDER BY t.date DESC
        LIMIT :limit
    """
    )
    fun getRecentTransactions(limit: Int): Flow<List<TransactionWithDetails>>

    @Query(
        """
        SELECT t.id, t.amount, t.type, t.date, t.description,
               a1.name AS accountName,
               a2.name AS accountToName,
               ig.name AS incomeGroupName
        FROM transactions t
        JOIN accounts a1 ON t.account_id = a1.id
        LEFT JOIN accounts a2 ON t.transfer_account_id = a2.id
        LEFT JOIN income_groups ig ON t.related_income_id = ig.id
        ORDER BY t.date DESC
    """
    )
    fun getTransactionWithDetails(): Flow<List<TransactionWithDetails>>

    @Query(
        """
        SELECT  t.id, t.amount, t.date, t.description,
        a1.name AS fromAccount,
        a2.name AS toAccount
        FROM transactions t
        JOIN accounts a1 ON t.account_id = a1.id
        JOIN accounts a2 ON t.transfer_account_id = a2.id
        WHERE t.type = 'transfer'
        ORDER BY t.date DESC
    """
    )
    fun getTransferTransactions(): Flow<List<TransferHistory>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE (account_id = :accountId OR transfer_account_id = :accountId)
        AND date BETWEEN :startDate AND :endDate
        ORDER BY date ASC
    """
    )
    fun getAccountTransactionsBetween(
        accountId: Int,
        startDate: Long,
        endDate: Long
    ): Flow<List<Transaction>>
}