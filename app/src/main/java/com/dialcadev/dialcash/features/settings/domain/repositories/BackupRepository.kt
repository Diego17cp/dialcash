package com.dialcadev.dialcash.features.settings.domain.repositories

interface BackupRepository {
    suspend fun exportBackup(
        uriString: String,
        start: Long = 0L,
        end: Long = Long.MAX_VALUE
    )
    suspend fun importBackup(uriString: String)
}