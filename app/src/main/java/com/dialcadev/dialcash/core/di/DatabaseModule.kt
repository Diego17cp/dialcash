package com.dialcadev.dialcash.core.di

import android.content.Context
import com.dialcadev.dialcash.core.database.AppDB
import com.dialcadev.dialcash.features.accounts.data.dao.AccountDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDB {
        return AppDB.getInstance(context)
    }
    @Provides
    fun provideAccountDao(db: AppDB): AccountDao {
        return db.accountDao()
    }
}