package com.dialcadev.dialcash.features.settings.di

import com.dialcadev.dialcash.features.settings.data.repositories.BackupRepositoryImpl
import com.dialcadev.dialcash.features.settings.domain.repositories.BackupRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {
    @Binds
    @Singleton
    abstract fun bindBackupRepository(
        backupRepositoryImpl: BackupRepositoryImpl
    ): BackupRepository
}