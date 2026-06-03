package com.dialcadev.dialcash.features.transactions.domain.usecases

import com.dialcadev.dialcash.features.transactions.domain.repositories.TransactionRepository
import javax.inject.Inject

class AddIncomeUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(
        accountId: Int?,
        amount: Double,
        description: String,
        date: Long? = null
    ): Result<Unit> {
        if (accountId == null) return Result.failure(IllegalArgumentException("Please select an account."))
        if (amount <= 0) return Result.failure(IllegalArgumentException("Amount must be valid and greater than 0."))
        if (description.isBlank()) return Result.failure(IllegalArgumentException("Description cannot be empty."))

        return try {
            transactionRepository.insertTransaction(
                amount = amount,
                type = "income",
                description = description,
                date = date ?: System.currentTimeMillis(),
                accountId = accountId,
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}