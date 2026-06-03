package com.dialcadev.dialcash.features.accounts.domain.usecases

import com.dialcadev.dialcash.features.accounts.data.repositories.AccountRepository
import com.dialcadev.dialcash.features.accounts.domain.models.Account
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllAccountsUseCase @Inject constructor(
    private val repository: AccountRepository
) {
    operator fun invoke(): Flow<List<Account>> {
        return repository.getAllAccounts()
    }
}