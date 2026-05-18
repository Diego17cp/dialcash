package com.dialcadev.dialcash.core.di

import com.dialcadev.dialcash.core.database.RoomTransactionRunner
import com.dialcadev.dialcash.core.domain.DatabaseTransactionRunner
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class CoreBindsModule {
    @Binds
    abstract fun bindDatabaseTransactionRunner(
        impl: RoomTransactionRunner
    ): DatabaseTransactionRunner
}