package com.dialcadev.dialcash.data

import androidx.room.withTransaction
import com.dialcadev.dialcash.data.db.AppDB
import com.dialcadev.dialcash.data.dto.AccountBalance
import com.dialcadev.dialcash.data.dto.IncomeGroupRemaining
import com.dialcadev.dialcash.data.dto.TransactionWithDetails
import com.dialcadev.dialcash.data.dto.TransferHistory
import com.dialcadev.dialcash.data.entities.Account
import com.dialcadev.dialcash.data.entities.Checkpoint
import com.dialcadev.dialcash.data.entities.IncomeGroup
import com.dialcadev.dialcash.data.entities.Transaction
import kotlinx.coroutines.flow.Flow

class AppRepository(private val db: AppDB) {
    private val accountDao = db.accountDao()
    private val transactionDao = db.transactionDao()
    private val incomeGroupDao = db.incomeGroupDao()
    private val checkpointDao = db.checkpointDao()

    // ==================== ACCOUNT OPERATIONS ====================
    suspend fun createAccount(account: Account) = accountDao.insert(account)
    suspend fun updateAccount(account: Account) = accountDao.updateAccount(account)
    suspend fun deleteAccount(account: Account) = accountDao.delete(account)
    fun getAllAccounts(): Flow<List<Account>> = accountDao.getAllAccounts()
    fun getAllAccountBalances(): Flow<List<AccountBalance>> = accountDao.getAccountBalances()
    fun getMainAccounts(): Flow<List<AccountBalance>> = accountDao.getMainAccountBalances()
    suspend fun getAccountById(accountId: Long): Account? = accountDao.getAccountById(accountId)
    suspend fun getAccountByName(name: String): Account? = accountDao.getAccountByName(name)
    suspend fun getAccountCount(): Int = accountDao.getAccountCount()

    // Transaction  operations

    suspend fun addIncome(
        accountId: Int,
        amount: Double,
        description: String?,
        relatedIncomeId: Int? = null,
        date: Long? = null
    ) {
        val transaction = Transaction(
            accountId = accountId,
            type = "income",
            amount = amount,
            date = date ?: System.currentTimeMillis(),
            description = description,
            relatedIncomeId = relatedIncomeId
        )
        transactionDao.insert(transaction)
    }

    suspend fun addExpense(
        accountId: Int,
        amount: Double,
        description: String?,
        relatedIncomeId: Int? = null,
        date: Long? = null
    ) {
        val transaction = Transaction(
            accountId = accountId,
            type = "expense",
            amount = amount,
            date = date ?: System.currentTimeMillis(),
            description = description,
            relatedIncomeId = relatedIncomeId
        )
        transactionDao.insert(transaction)
    }

    suspend fun makeTransfer(
        fromAccount: Account,
        toAccount: Account,
        amount: Double,
        description: String?,
        date: Long? = null
    ) {
        db.withTransaction {
            val updatedFrom = fromAccount.copy(balance = fromAccount.balance - amount)
            val updatedTo = toAccount.copy(balance = toAccount.balance + amount)
            accountDao.updateAccount(updatedFrom)
            accountDao.updateAccount(updatedTo)

            val transfer = Transaction(
                amount = amount,
                type = "transfer",
                date = date ?: System.currentTimeMillis(),
                description = description,
                accountId = fromAccount.id,
                transferAccountId = toAccount.id
            )
            transactionDao.insert(transfer)
        }
    }

    suspend fun deleteTransaction(transaction: Transaction) = transactionDao.delete(transaction)
    fun getAllTransactions(): Flow<List<TransactionWithDetails>> =
        transactionDao.getTransactionWithDetails()

    fun getTransferTransactions(): Flow<List<TransferHistory>> =
        transactionDao.getTransferTransactions()

    fun getTransactionBetween(startDate: Long, endDate: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsBetween(startDate, endDate)

    fun getTransactionsForAccountBetween(
        accountId: Int,
        startDate: Long,
        endDate: Long
    ): Flow<List<Transaction>> =
        transactionDao.getAccountTransactionsBetween(accountId, startDate, endDate)

    suspend fun searchTransactions(query: String): Flow<List<Transaction>> =
        transactionDao.searchTransactions(query)

    fun getTransactionsForIncomeGroup(incomeGroupId: Int): Flow<List<Transaction>> =
        transactionDao.getExpensesByIncomeGroup(incomeGroupId)

    fun getTransactionsForAccount(accountId: Int): Flow<List<Transaction>> =
        transactionDao.getTransactionsForAccount(accountId)

    suspend fun getTransactionById(transactionId: Long): Transaction? =
        transactionDao.getTransactionById(transactionId)

    suspend fun getTotalTransactions(): Int = transactionDao.getTransactionCount()
    fun getTotalIncome(): Flow<Double> = transactionDao.getTotalIncome()
    fun getTotalExpense(): Flow<Double> = transactionDao.getTotalExpense()
    fun getRecentTransactions(limit: Int): Flow<List<TransactionWithDetails>> =
        transactionDao.getRecentTransactions(limit)

    // ==================== ACCOUNT OPERATIONS ====================
    suspend fun createIncomeGroup(incomeGroup: IncomeGroup) = incomeGroupDao.insert(incomeGroup)
    suspend fun updateIncomeGroup(incomeGroup: IncomeGroup) = incomeGroupDao.update(incomeGroup)
    suspend fun deleteIncomeGroup(incomeGroup: IncomeGroup) = incomeGroupDao.delete(incomeGroup)
    fun getAllIncomeGroups(): Flow<List<IncomeGroupRemaining>> =
        incomeGroupDao.getIncomeGroupsRemaining()

    suspend fun getIncomeGroupById(incomeGroupId: Long): IncomeGroup? =
        incomeGroupDao.getIncomeGroupById(incomeGroupId)

    // ==================== CHECKPOINT OPERATIONS ====================
    suspend fun createCheckpoint(checkpoint: Checkpoint) = checkpointDao.insert(checkpoint)
    suspend fun updateCheckpoint(checkpoint: Checkpoint) = checkpointDao.update(checkpoint)
    suspend fun deleteCheckpoint(checkpoint: Checkpoint) = checkpointDao.delete(checkpoint)
    suspend fun getAllCheckpoints(): List<Checkpoint> = checkpointDao.getAllCheckpoints()
    suspend fun getCheckpointById(checkpointId: Long): Checkpoint? =
        checkpointDao.getCheckpointById(checkpointId)

    fun getCheckpointsBetween(startDate: Long, endDate: Long): Flow<List<Checkpoint>> =
        checkpointDao.getCheckpointsBetween(startDate, endDate)

    suspend fun getLastCheckpointBefore(date: Long): Checkpoint? =
        checkpointDao.getLastCheckpointBefore(date)
}