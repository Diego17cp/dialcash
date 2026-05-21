package com.dialcadev.dialcash.features.accounts.domain.usecases

import com.dialcadev.dialcash.features.accounts.data.repositories.AccountRepository
import com.dialcadev.dialcash.features.accounts.domain.models.Account
import javax.inject.Inject

class UpdateAccountUseCase @Inject constructor(
    private val repository: AccountRepository
) {
    suspend operator fun invoke(
        accountId: Int,
        name: String,
        type: String,
        balance: Double
    ): Result<Unit> {
        if (name.isBlank()) return Result.failure(IllegalArgumentException("Account name cannot be empty"))
        if (type.isBlank()) return Result.failure(IllegalArgumentException("Account type cannot be empty"))
        if (balance < 0) return Result.failure(IllegalArgumentException("Balance cannot be negative"))

        val existingAccount = repository.getAccountById(accountId)
            ?: return Result.failure(NoSuchElementException("Account with id=$accountId not found"))

        val account = Account(
            id = existingAccount.id,
            name = name.trim(),
            type = type,
            balance = balance
        )

        return try {
            repository.updateAccount(account)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}