package com.dialcadev.dialcash.features.transactions.domain.usecases

import com.dialcadev.dialcash.features.transactions.domain.repositories.TransactionRepository
import javax.inject.Inject

class GetTransactionsForAccountBetweenUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(accountId: Int, startDate: Long, endDate: Long) =
        transactionRepository.getTransactionsForAccountBetween(accountId, startDate, endDate)
}