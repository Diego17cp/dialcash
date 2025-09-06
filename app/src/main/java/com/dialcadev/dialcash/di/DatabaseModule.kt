package com.dialcadev.dialcash.di

import android.content.Context
import com.dialcadev.dialcash.data.AppRepository
import com.dialcadev.dialcash.data.UserDataStore
import com.dialcadev.dialcash.data.db.AppDB
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule{
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDB {
        return AppDB.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideAppRepository(db: AppDB): AppRepository {
        return AppRepository(db)
    }

    @Provides
    @Singleton
    fun provideUserDataStore(@ApplicationContext context: Context): UserDataStore {
        return UserDataStore(context)
    }
}