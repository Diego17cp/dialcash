package com.dialcadev.dialcash.features.accounts.domain.usecases

import com.dialcadev.dialcash.features.transactions.domain.repositories.TransactionRepository
import javax.inject.Inject

class DeleteTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transactionId: Int): Result<Unit> {
        return try {
            val existingTransaction = repository.getTransactionById(transactionId)
                ?: return Result.failure(NoSuchElementException("Transaction with id=$transactionId not found"))
            repository.deleteTransaction(existingTransaction)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}