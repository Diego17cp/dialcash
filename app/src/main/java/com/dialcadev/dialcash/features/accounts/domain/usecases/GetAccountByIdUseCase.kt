package com.dialcadev.dialcash.features.accounts.domain.usecases

import com.dialcadev.dialcash.features.accounts.data.repositories.AccountRepository
import javax.inject.Inject

class GetAccountByIdUseCase @Inject constructor(
    private val repository: AccountRepository
) {
    suspend operator fun invoke(accountId: Int) = repository.getAccountById(accountId)
}