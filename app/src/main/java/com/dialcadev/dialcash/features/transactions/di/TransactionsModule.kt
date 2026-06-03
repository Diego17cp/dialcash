package com.dialcadev.dialcash.features.transactions.di

import com.dialcadev.dialcash.features.transactions.data.repositories.TransactionRepositoryImpl
import com.dialcadev.dialcash.features.transactions.domain.repositories.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class TransactionsModule {
    @Binds
    abstract fun bindTransactionsRepository(
        impl: TransactionRepositoryImpl
    ): TransactionRepository
}