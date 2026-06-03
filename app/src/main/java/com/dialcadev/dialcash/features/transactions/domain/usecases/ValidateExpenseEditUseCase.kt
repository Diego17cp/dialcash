package com.dialcadev.dialcash.features.transactions.domain.usecases

import com.dialcadev.dialcash.features.accounts.data.repositories.AccountRepository
import com.dialcadev.dialcash.features.incomegroups.data.repositories.IncomeGroupRepository
import com.dialcadev.dialcash.features.transactions.domain.repositories.TransactionRepository
import javax.inject.Inject

class ValidateExpenseEditUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val incomeGroupRepository: IncomeGroupRepository
) {
    suspend operator fun invoke(
        transactionId: Int,
        accountId: Int,
        amount: Double,
        incomeGroupId: Int?
    ): Result<Unit> {
        val currentTransaction = transactionRepository.getTransactionById(transactionId)
        val currentAmount = currentTransaction?.amount ?: 0.0
        val currentAccountId = currentTransaction?.accountId

        if (currentAccountId != accountId) {
            val accountBalance = accountRepository.getAccountBalance(accountId) ?: 0.0
            if (accountBalance < amount) return Result.failure(IllegalArgumentException("Insufficient funds in the selected account. Available: $accountBalance"))
        } else {
            val amountDiff = amount - currentAmount
            if (amountDiff > 0) {
                val accountBalance = accountRepository.getAccountBalance(accountId) ?: 0.0
                if (accountBalance < amountDiff) return Result.failure(IllegalArgumentException("Insufficient funds in the selected account. Available: $accountBalance"))
            }
        }

        if (incomeGroupId != null) {
            val relatedIncomeId = currentTransaction?.relatedIncomeId
            if (relatedIncomeId != incomeGroupId) {
                val remaining = incomeGroupRepository.getRemainingForIncomeGroup(incomeGroupId)
                if (remaining < amount) return Result.failure(IllegalArgumentException("Insufficient funds in income group. Required: $amount, Available: $remaining"))
            } else {
                val amountDiff = amount - currentAmount
                if (amountDiff > 0) {
                    val remaining = incomeGroupRepository.getRemainingForIncomeGroup(incomeGroupId)
                    if (remaining < amountDiff) return Result.failure(IllegalArgumentException("Insufficient funds in income group. Required: $amountDiff, Available: $remaining"))
                }
            }
        }
        return Result.success(Unit)
    }
}