package com.dialcadev.dialcash.features.transactions.domain.usecases

import com.dialcadev.dialcash.core.domain.DatabaseTransactionRunner
import com.dialcadev.dialcash.features.accounts.data.repositories.AccountRepository
import com.dialcadev.dialcash.features.incomegroups.data.repositories.IncomeGroupRepository
import com.dialcadev.dialcash.features.transactions.domain.repositories.TransactionRepository
import javax.inject.Inject

class AddExpenseUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val incomeGroupRepository: IncomeGroupRepository,
    private val dbTransaction: DatabaseTransactionRunner,
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(
        accountId: Int?,
        amount: Double,
        description: String,
        date: Long? = null,
        relatedIncomeId: Int? = null
    ) : Result<Unit> {
        if (accountId == null) return Result.failure(IllegalArgumentException("Please select an account."))
        if (amount <= 0) return Result.failure(IllegalArgumentException("Amount must be valid and greater than 0."))
        if (description.isBlank()) return Result.failure(IllegalArgumentException("Description cannot be empty."))
        return try {
            dbTransaction {
                val accountBalance = accountRepository.getAccountBalance(accountId) ?: 0.0
                if (accountBalance < amount) throw IllegalArgumentException("Insufficient funds in the selected account. Available: $accountBalance")
                if (relatedIncomeId != null) {
                    val remaining = incomeGroupRepository.getRemainingForIncomeGroup(relatedIncomeId)
                    if (remaining < amount) throw IllegalArgumentException("Insufficient funds in income group. Required: $amount, Available: $remaining")
                }
                transactionRepository.insertTransaction(
                    amount = amount,
                    type = "expense",
                    description = description,
                    date = date ?: System.currentTimeMillis(),
                    accountId = accountId,
                    relatedIncomeId = relatedIncomeId
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}