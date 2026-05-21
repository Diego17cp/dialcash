package com.dialcadev.dialcash.features.transactions.domain.usecases

import com.dialcadev.dialcash.features.accounts.data.repositories.AccountRepository
import com.dialcadev.dialcash.features.transactions.domain.repositories.TransactionRepository
import javax.inject.Inject

class ValidateTransferEditUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(
        transactionId: Int,
        fromAccountId: Int,
        toAccountId: Int,
        amount: Double
    ) : Result<Unit> {
        val currentTransaction = transactionRepository.getTransactionById(transactionId)
        val currentAmount = currentTransaction?.amount ?: 0.0
        val currentFromAccountId = currentTransaction?.accountId

        if (fromAccountId == toAccountId) return Result.failure(IllegalArgumentException("Source and destination accounts cannot be the same."))
        if (currentFromAccountId != fromAccountId) {
            val fromAccountBalance = accountRepository.getAccountBalance(fromAccountId) ?: 0.0
            if (fromAccountBalance < amount) return Result.failure(IllegalArgumentException("Insufficient funds in the source account. Available: $fromAccountBalance"))
        } else {
            val amountDiff = amount - currentAmount
            if (amountDiff > 0) {
                val fromAccountBalance = accountRepository.getAccountBalance(fromAccountId) ?: 0.0
                if (fromAccountBalance < amountDiff) return Result.failure(IllegalArgumentException("Insufficient funds in the source account. Available: $fromAccountBalance"))
            }
        }
        return Result.success(Unit)
    }
}