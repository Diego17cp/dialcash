package com.dialcadev.dialcash.features.settings.domain.usecases

import com.dialcadev.dialcash.features.settings.domain.repositories.BackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ExportBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(uriString: String) = withContext(Dispatchers.IO) {
        backupRepository.exportBackup(uriString)
    }
}