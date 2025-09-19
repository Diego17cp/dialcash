package com.dialcadev.dialcash.data

import android.util.Log
import androidx.room.RoomOpenDelegate
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
import kotlin.compareTo
import kotlin.text.insert

class AppRepository(private val db: AppDB) {
    private val accountDao = db.accountDao()
    private val transactionDao = db.transactionDao()
    private val incomeGroupDao = db.incomeGroupDao()
    private val checkpointDao = db.checkpointDao()
    val database: AppDB = db

    // ==================== ACCOUNT OPERATIONS ====================
    suspend fun createAccount(account: Account) =
        accountDao.insert(account.name, account.type, account.balance)

    suspend fun updateAccount(account: Account) =
        accountDao.updateAccount(account.id, account.name, account.type, account.balance)

    suspend fun deleteAccount(account: Account) = accountDao.delete(account)
    fun getAllAccounts(): Flow<List<Account>> = accountDao.getAllAccounts()
    fun getAllAccountBalances(): Flow<List<AccountBalanceWithOriginal>> =
        accountDao.getAccountBalances()

    fun getMainAccounts(): Flow<List<AccountBalanceWithOriginal>> =
        accountDao.getMainAccountBalances()

    suspend fun getAccountById(accountId: Int): Account? = accountDao.getAccountById(accountId)
    suspend fun getAccountByName(name: String): Account? = accountDao.getAccountByName(name)
    suspend fun getAccountCount(): Int = accountDao.getAccountCount()
    suspend fun insertAccountsBatch(accounts: List<Account>) {
        db.withTransaction {
            accounts.forEach { account ->
                accountDao.insert(account.name, account.type, account.balance)
            }
        }
    }

    suspend fun getAccountBalance(accountId: Int): Double? {
        return accountDao.getAccountBalance(accountId)
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
                val remaining = incomeGroupDao.getRemainingAmount(relatedIncomeId) ?: 0.0
                if (remaining < amount) {
                    throw IllegalArgumentException("Insufficient funds in the selected income group.")
                }
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

    suspend fun updateTransaction(transaction: Transaction) = transactionDao.update(
        transaction.id,
        transaction.amount,
        transaction.type,
        transaction.description!!,
        transaction.date,
        transaction.accountId,
        transaction.transferAccountId,
        transaction.relatedIncomeId
    )

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

    suspend fun getTransactionById(transactionId: Int): Transaction? =
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
        incomeGroupDao.insert(incomeGroup.name, incomeGroup.amount)

    suspend fun updateIncomeGroup(incomeGroup: IncomeGroup) = incomeGroupDao.update(
        incomeGroup.id,
        incomeGroup.name,
        incomeGroup.amount
    )

    suspend fun deleteIncomeGroup(incomeGroup: IncomeGroup) = incomeGroupDao.delete(incomeGroup)

    fun getAllIncomeGroupsWithRemaining(): Flow<List<IncomeGroupRemaining>> =
        incomeGroupDao.getIncomeGroupsRemaining()

    fun getAllIncomeGroups(): Flow<List<IncomeGroup>> = incomeGroupDao.getAllIncomeGroups()

    suspend fun getRemainingForIncomeGroup(incomeGroupId: Int): Double {
        return incomeGroupDao.getRemainingAmount(incomeGroupId) ?: 0.0
    }

    suspend fun getIncomeGroupById(incomeGroupId: Int): IncomeGroup? =
        incomeGroupDao.getIncomeGroupById(incomeGroupId)

    suspend fun insertIncomeGroupsBatch(incomeGroups: List<IncomeGroup>) {
        db.withTransaction {
            incomeGroups.forEach { group ->
                incomeGroupDao.insert(group.name, group.amount)
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
    suspend fun getCheckpointById(checkpointId: Int): Checkpoint? =
        checkpointDao.getCheckpointById(checkpointId)

    fun getCheckpointsBetween(startDate: Long, endDate: Long): Flow<List<Checkpoint>> =
        checkpointDao.getCheckpointsBetween(startDate, endDate)

    suspend fun getLastCheckpointBefore(date: Long): Checkpoint? =
        checkpointDao.getLastCheckpointBefore(date)

    // ==================== UTILITY OPERATIONS====================
    suspend fun wipeDatabase() {
        db.clearAllTables()
    }

    suspend fun validateExpenseEdit(
        transactionId: Int,
        accountId: Int,
        amount: Double,
        incomeGroupId: Int?
    ): ValidationResult {
        val currentTransaction = transactionDao.getTransactionById(transactionId)
        val currentAmount = currentTransaction?.amount ?: 0.0
        val currentAccountId = currentTransaction?.accountId
        if (currentAccountId != accountId) {
            val accountBalance = getAccountBalance(accountId)
            if ((accountBalance ?: 0.0) < amount) {
                return ValidationResult(
                    false,
                    "Insufficient funds in the selected account."
                )
            }
        } else {
            val amountDiff = amount - currentAmount
            if (amountDiff > 0) {
                val accountBalance = getAccountBalance(accountId)
                if ((accountBalance ?: 0.0) < amountDiff) {
                    return ValidationResult(
                        false,
                        "Insufficient funds in the selected account."
                    )
                }
            }
        }

        if (incomeGroupId != null) {
            val currentIncomeId = currentTransaction?.relatedIncomeId
            if (currentIncomeId != incomeGroupId) {
                val remaining = getRemainingForIncomeGroup(incomeGroupId)
                if (remaining < amount) {
                    return ValidationResult(
                        false,
                        "Insufficient funds in the selected income group."
                    )
                }
            } else {
                val amountDiff = amount - currentAmount
                if (amountDiff > 0) {
                    val remaining = getRemainingForIncomeGroup(incomeGroupId)
                    if (remaining < amountDiff) {
                        return ValidationResult(
                            false,
                            "Insufficient funds in the selected income group."
                        )
                    }
                }
            }
        }
        return ValidationResult(true, "")
    }

    suspend fun validateTransferEdit(
        transactionId: Int,
        fromAccountId: Int,
        toAccountId: Int,
        amount: Double
    ): ValidationResult {
        val currentTransaction = transactionDao.getTransactionById(transactionId)
        val currentAmount = currentTransaction?.amount ?: 0.0
        val currentFromAccountId = currentTransaction?.accountId

        if (fromAccountId == toAccountId) {
            return ValidationResult(
                false,
                "Source and destination accounts must be different."
            )
        }

        if (currentFromAccountId != fromAccountId) {
            val fromAccountBalance = getAccountBalance(fromAccountId)
            if ((fromAccountBalance ?: 0.0) < amount) {
                return ValidationResult(
                    false,
                    "Insufficient funds in the source account."
                )
            }
        } else {
            val amountDiff = amount - currentAmount
            if (amountDiff > 0) {
                val fromAccountBalance = getAccountBalance(fromAccountId)
                if ((fromAccountBalance ?: 0.0) < amountDiff) {
                    return ValidationResult(
                        false,
                        "Insufficient funds in the source account."
                    )
                }
            }
        }
        return ValidationResult(true, "")
    }
}

data class ValidationResult(val isValid: Boolean, val message: String)