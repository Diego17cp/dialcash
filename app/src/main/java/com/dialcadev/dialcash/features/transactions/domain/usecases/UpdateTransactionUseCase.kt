package com.dialcadev.dialcash.features.transactions.domain.usecases

import com.dialcadev.dialcash.features.transactions.domain.repositories.TransactionRepository
import javax.inject.Inject

class UpdateTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(
        transactionId: Int,
        toAccountId: Int?,
        transferAccountId: Int?,
        amount: Double,
        description: String,
        date: Long?,
        relatedIncomeId: Int?
    ) : Result<Unit> {
        val existingTransaction = repository.getTransactionById(transactionId)
            ?: return Result.failure(NoSuchElementException("Transaction with id=$transactionId not found"))
        if (toAccountId == null) return Result.failure(IllegalArgumentException("Please select an account."))
        if (existingTransaction.type == "transfer" && transferAccountId == null) {
            return Result.failure(IllegalArgumentException("Please select a transfer account for transfer transactions."))
        }
        if (amount <= 0) return Result.failure(IllegalArgumentException("Amount must be valid and greater than 0."))
        if (description.isBlank()) return Result.failure(IllegalArgumentException("Description cannot be empty."))

        val updatedTransaction = existingTransaction.copy(
            accountId = toAccountId,
            transferAccountId = if (existingTransaction.type == "transfer") transferAccountId else null,
            amount = amount,
            description = description.trim(),
            date = date ?: existingTransaction.date,
            relatedIncomeId = relatedIncomeId
        )
        return try {
            repository.updateTransaction(updatedTransaction)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}