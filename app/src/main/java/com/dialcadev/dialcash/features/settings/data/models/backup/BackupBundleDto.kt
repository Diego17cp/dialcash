package com.dialcadev.dialcash.features.settings.data.models.backup

data class BackupBundleDto(
    val metadata: BackupMetadata,
    val datastore: DataStoreBackup,
    val db: DatabaseBackup
)