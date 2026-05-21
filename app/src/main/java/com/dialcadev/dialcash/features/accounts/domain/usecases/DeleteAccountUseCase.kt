package com.dialcadev.dialcash.features.accounts.domain.usecases

import com.dialcadev.dialcash.features.accounts.data.repositories.AccountRepository
import javax.inject.Inject

class DeleteAccountUseCase @Inject constructor(
    private val repository: AccountRepository
) {
    suspend operator fun invoke(accountId: Int): Result<Unit> {
        return try {
            val account = repository.getAccountById(accountId) ?: return Result.failure(
                NoSuchElementException("Account with ID $accountId not found")
            )
            repository.deleteAccount(account)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}