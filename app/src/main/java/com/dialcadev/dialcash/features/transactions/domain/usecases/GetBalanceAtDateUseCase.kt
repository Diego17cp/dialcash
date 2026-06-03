package com.dialcadev.dialcash.features.transactions.domain.usecases

import com.dialcadev.dialcash.features.accounts.data.repositories.AccountRepository
import javax.inject.Inject

class GetBalanceAtDateUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(accountId: Int, targetDate: Long): Double {
        return accountRepository.getBalanceAtDate(accountId, targetDate)
    }
}