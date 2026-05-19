package com.dialcadev.dialcash.features.transactions.domain.usecases

import com.dialcadev.dialcash.core.domain.DatabaseTransactionRunner
import com.dialcadev.dialcash.features.accounts.data.repositories.AccountRepository
import com.dialcadev.dialcash.features.transactions.domain.repositories.TransactionRepository
import javax.inject.Inject

class MakeTransferUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val dbTransaction: DatabaseTransactionRunner,
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(
        fromAccountId: Int?,
        toAccountId: Int?,
        amount: Double,
        description: String,
        date: Long? = null,
    ) : Result<Unit> {
        if (fromAccountId == null) return Result.failure(IllegalArgumentException("Please select source account."))
        if (toAccountId == null) return Result.failure(IllegalArgumentException("Please select destination account."))
        if (fromAccountId == toAccountId) return Result.failure(IllegalArgumentException("Source and destination accounts must be different."))
        if (amount <= 0) return Result.failure(IllegalArgumentException("Amount must be valid and greater than 0."))
        if (description.isBlank()) return Result.failure(IllegalArgumentException("Description cannot be empty."))
        return try {
            dbTransaction {
                val fromBalance = accountRepository.getAccountBalance(fromAccountId) ?: 0.0
                if (fromBalance < amount) {
                    throw IllegalArgumentException("Insufficient funds in source account. Available: $fromBalance")
                }
                transactionRepository.insertTransaction(
                    accountId = fromAccountId,
                    transferAccountId = toAccountId,
                    type = "transfer",
                    amount = amount,
                    description = description,
                    date = date ?: System.currentTimeMillis()
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}