package com.dialcadev.dialcash.features.settings.data.models.backup

data class BackupMetadata(
    val schemaVersion: Int,
    val appVersion: String,
    val exportedAt: Long,
    val totalAccounts: Int,
    val totalTransactions: Int,
    val totalIncomeGroups: Int
)