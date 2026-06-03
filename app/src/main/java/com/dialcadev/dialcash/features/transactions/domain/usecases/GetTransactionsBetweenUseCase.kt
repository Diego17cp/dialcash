package com.dialcadev.dialcash.features.transactions.domain.usecases

import com.dialcadev.dialcash.features.transactions.domain.repositories.TransactionRepository
import javax.inject.Inject

class GetTransactionsBetweenUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(startDate: Long, endDate: Long) =
        transactionRepository.getTransactionBetween(startDate, endDate)
}