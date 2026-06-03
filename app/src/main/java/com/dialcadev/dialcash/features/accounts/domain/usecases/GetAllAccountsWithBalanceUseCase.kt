package com.dialcadev.dialcash.features.accounts.domain.usecases

import com.dialcadev.dialcash.features.accounts.data.repositories.AccountRepository
import com.dialcadev.dialcash.features.accounts.domain.dtos.AccountBalanceWithOriginal
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllAccountsWithBalanceUseCase @Inject constructor(
    private val repository: AccountRepository
) {
    operator fun invoke(): Flow<List<AccountBalanceWithOriginal>> {
        return repository.getAllAccountBalances()
    }
}