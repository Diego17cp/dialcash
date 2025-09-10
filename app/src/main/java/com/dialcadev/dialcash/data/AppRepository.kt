package com.dialcadev.dialcash.data

import androidx.room.withTransaction
import com.dialcadev.dialcash.data.dao.AccountBalanceWithOriginal
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
import kotlin.text.insert

class AppRepository(private val db: AppDB) {
    private val accountDao = db.accountDao()
    private val transactionDao = db.transactionDao()
    private val incomeGroupDao = db.incomeGroupDao()
    private val checkpointDao = db.checkpointDao()

    // ==================== ACCOUNT OPERATIONS ====================
    suspend fun createAccount(account: Account) =
        accountDao.insert(account.name, account.type, account.balance)

    suspend fun updateAccount(account: Account) =
        accountDao.updateAccount(account.id, account.name, account.type, account.balance)

    suspend fun deleteAccount(account: Account) = accountDao.delete(account)
    fun getAllAccounts(): Flow<List<Account>> = accountDao.getAllAccounts()
    fun getAllAccountBalances(): Flow<List<AccountBalanceWithOriginal>> =
        accountDao.getAccountBalances()

    fun getMainAccounts(): Flow<List<AccountBalanceWithOriginal>> = accountDao.getMainAccountBalances()
    suspend fun getAccountById(accountId: Long): Account? = accountDao.getAccountById(accountId)
    suspend fun getAccountByName(name: String): Account? = accountDao.getAccountByName(name)
    suspend fun getAccountCount(): Int = accountDao.getAccountCount()
    suspend fun insertAccountsBatch(accounts: List<Account>) {
        db.withTransaction {
            accounts.forEach { account ->
                accountDao.insert(account.name, account.type, account.balance)
            }
        }
    }

    // Transaction  operations

    suspend fun addIncome(
        accountId: Int,
        amount: Double,
        description: String,
        relatedIncomeId: Int? = null,
        date: Long? = null
    ) {
        transactionDao.insert(
            accountId = accountId,
            type = "income",
            amount = amount,
            date = date ?: System.currentTimeMillis(),
            description = description,
            relatedIncomeId = relatedIncomeId,
            transferAccountId = null
        )
    }

    suspend fun addExpense(
        accountId: Int,
        amount: Double,
        description: String,
        relatedIncomeId: Int? = null,
        date: Long? = null
    ) {
        db.withTransaction {
            if (relatedIncomeId != null) {
                val incomeGroup = incomeGroupDao.getIncomeGroupById(relatedIncomeId.toLong())
                    ?: throw IllegalArgumentException("Income group with ID $relatedIncomeId not found")
                val remaining = incomeGroupDao.getRemainingForGroup(incomeGroup.id.toInt())
                val updatedRemaining = (remaining ?: 0.0) - amount

                if (updatedRemaining < 0) {
                    throw IllegalArgumentException("Insufficient funds in income group. Available: $remaining, Required: $amount")
                }

                val updatedIncomeGroup = incomeGroup.copy(remaining = updatedRemaining)
                incomeGroupDao.update(
                    id = relatedIncomeId,
                    name = updatedIncomeGroup.name,
                    amount = updatedIncomeGroup.amount,
                    remaining = updatedIncomeGroup.remaining
                )
            }
            transactionDao.insert(
                accountId = accountId,
                type = "expense",
                amount = amount,
                date = date ?: System.currentTimeMillis(),
                description = description,
                relatedIncomeId = relatedIncomeId,
                transferAccountId = null
            )
        }
    }

    suspend fun makeTransfer(
        fromAccountId: Int,
        toAccountId: Int,
        amount: Double,
        description: String,
        date: Long? = null
    ) {
        db.withTransaction {
            transactionDao.insert(
                amount = amount,
                type = "transfer",
                date = date ?: System.currentTimeMillis(),
                description = description,
                accountId = fromAccountId,
                transferAccountId = toAccountId,
                relatedIncomeId = null
            )
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

    suspend fun insertTransactionsBatch(transactions: List<Transaction>) {
        db.withTransaction {
            transactions.forEach { transaction ->
                transactionDao.insert(
                    transaction.amount,
                    transaction.type,
                    transaction.description!!,
                    transaction.date,
                    transaction.accountId,
                    transaction.transferAccountId,
                    transaction.relatedIncomeId
                )
            }
        }
    }

    // ==================== SPECIAL INCOMES OPERATIONS ====================
    suspend fun createIncomeGroup(incomeGroup: IncomeGroup) =
        incomeGroupDao.insert(incomeGroup.name, incomeGroup.amount, incomeGroup.remaining)

    suspend fun updateIncomeGroup(incomeGroup: IncomeGroup) = incomeGroupDao.update(
        incomeGroup.id,
        incomeGroup.name,
        incomeGroup.amount,
        incomeGroup.remaining
    )

    suspend fun deleteIncomeGroup(incomeGroup: IncomeGroup) = incomeGroupDao.delete(incomeGroup)
    fun getAllIncomeGroupsWithRemaining(): Flow<List<IncomeGroupRemaining>> =
        incomeGroupDao.getIncomeGroupsRemaining()

    fun getAllIncomeGroups(): Flow<List<IncomeGroup>> = incomeGroupDao.getAllIncomeGroups()

    suspend fun getIncomeGroupById(incomeGroupId: Long): IncomeGroup? =
        incomeGroupDao.getIncomeGroupById(incomeGroupId)

    suspend fun insertIncomeGroupsBatch(incomeGroups: List<IncomeGroup>) {
        db.withTransaction {
            incomeGroups.forEach { group ->
                incomeGroupDao.insert(group.name, group.amount, group.remaining)
            }
        }
    }

    // ==================== CHECKPOINT OPERATIONS ====================
    suspend fun createCheckpoint(checkpoint: Checkpoint) =
        checkpointDao.insert(checkpoint.date, checkpoint.balanceSnapshot)

    suspend fun updateCheckpoint(checkpoint: Checkpoint) = checkpointDao.update(
        checkpoint.id,
        checkpoint.date,
        checkpoint.balanceSnapshot
    )

    suspend fun deleteCheckpoint(checkpoint: Checkpoint) = checkpointDao.delete(checkpoint)
    suspend fun getAllCheckpoints(): List<Checkpoint> = checkpointDao.getAllCheckpoints()
    suspend fun getCheckpointById(checkpointId: Long): Checkpoint? =
        checkpointDao.getCheckpointById(checkpointId)

    fun getCheckpointsBetween(startDate: Long, endDate: Long): Flow<List<Checkpoint>> =
        checkpointDao.getCheckpointsBetween(startDate, endDate)

    suspend fun getLastCheckpointBefore(date: Long): Checkpoint? =
        checkpointDao.getLastCheckpointBefore(date)

    // ==================== UTILITY OPERATIONS====================
    suspend fun wipeDatabase() {
        db.clearAllTables()
    }
}