package com.dialcadev.dialcash.features.transactions.domain.usecases

import com.dialcadev.dialcash.features.transactions.domain.dtos.TransactionWithDetails
import com.dialcadev.dialcash.features.transactions.domain.repositories.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecentTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(limit: Int = 5): Flow<List<TransactionWithDetails>> = repository.getRecentTransactions(limit)
}