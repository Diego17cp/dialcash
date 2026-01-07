package com.dialcadev.dialcash.data.backup

import kotlinx.serialization.Serializable
import com.dialcadev.dialcash.data.entities.Account
import com.dialcadev.dialcash.data.entities.IncomeGroup
import com.dialcadev.dialcash.data.entities.Transaction

data class BackupBundleDto(
    val metadata: BackupMetadata,
    val datastore: DataStoreBackup,
    val db: DatabaseBackup
)

data class DataStoreBackup(
    val username: String,
    val profilePicture: String?, // Uri as String
    val currencySymbol: String, // e.g. "$", "€"
    val isBalanceVisible: Boolean = true
)

data class DatabaseBackup(
    val accounts: List<Account>,
    val transactions: List<Transaction>,
    val incomeGroups: List<IncomeGroup>
)

data class BackupMetadata(
    val schemaVersion: Int,
    val appVersion: String,
    val exportedAt: Long,
    val totalAccounts: Int,
    val totalTransactions: Int,
    val totalIncomeGroups: Int
)
