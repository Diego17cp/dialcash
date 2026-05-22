package com.dialcadev.dialcash.features.transactions.domain.usecases

import com.dialcadev.dialcash.features.transactions.domain.dtos.TransactionWithDetails
import com.dialcadev.dialcash.features.transactions.domain.repositories.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(): Flow<List<TransactionWithDetails>> {
        return transactionRepository.getAllTransactions()
    }
}