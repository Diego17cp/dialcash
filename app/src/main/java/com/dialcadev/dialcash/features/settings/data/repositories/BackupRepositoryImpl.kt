package com.dialcadev.dialcash.features.settings.data.repositories

import android.content.Context
import android.net.Uri
import android.util.Log
import com.dialcadev.dialcash.core.database.AppDB
import com.dialcadev.dialcash.core.datastore.UserDataStore
import com.dialcadev.dialcash.features.settings.domain.repositories.BackupRepository
import com.google.gson.GsonBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri
import androidx.room.withTransaction
import com.dialcadev.dialcash.BuildConfig
import com.dialcadev.dialcash.features.settings.data.models.backup.BackupBundleDto
import com.dialcadev.dialcash.features.settings.data.models.backup.BackupMetadata
import com.dialcadev.dialcash.features.settings.data.models.backup.DataStoreBackup
import com.dialcadev.dialcash.features.settings.data.models.backup.DatabaseBackup
import com.google.gson.stream.JsonWriter
import java.io.BufferedInputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter

@Singleton
class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDB,
    private val userDataStore: UserDataStore
): BackupRepository {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    override suspend fun exportBackup(uriString: String, start: Long, end: Long) {
        try {
            val uri = uriString.toUri()
            val accounts = db.accountDao().getAllAccounts().first()
            val incomeGroups = db.incomeGroupDao().getAllIncomeGroups().first()
            val transactions = db.transactionDao().getTransactionsBetween(start, end).first()
            val userData = userDataStore.getUserData().first()

            val bundle = BackupBundleDto(
                metadata = BackupMetadata(
                    schemaVersion = 1,
                    appVersion = BuildConfig.VERSION_NAME,
                    exportedAt = System.currentTimeMillis(),
                    totalAccounts = accounts.size,
                    totalTransactions = transactions.size,
                    totalIncomeGroups = incomeGroups.size
                ),
                datastore = DataStoreBackup(
                    username = userData.name,
                    profilePicture = userData.photoUri.takeIf { it.isNotEmpty() },
                    currencySymbol = userData.currencySymbol,
                    isBalanceVisible = userData.isBalanceVisible
                ),
                db = DatabaseBackup(
                    accounts = accounts,
                    transactions = transactions,
                    incomeGroups = incomeGroups
                )
            )

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { osw ->
                    JsonWriter(osw).use { jw ->
                        gson.toJson(bundle, BackupBundleDto::class.java, jw)
                        jw.flush()
                    }
                }
            } ?: throw Exception("Could not open content stream")
            Log.d("BackupRepository", "Export completed successfully")
        } catch (e: Exception) {
            Log.e("BackupRepository", "Error during export: ${e.message}", e)
            throw e
        }
    }

    override suspend fun importBackup(uriString: String) {
        val uri = uriString.toUri()
        context.contentResolver.openInputStream(uri)?.use { rawStream ->
            val bis = if (rawStream is BufferedInputStream) rawStream else BufferedInputStream(rawStream)
            bis.mark(2048)
            val headerBytes = ByteArray(1024)
            val read = bis.read(headerBytes)
            val header = if (read > 0) String(headerBytes, 0, read, Charsets.UTF_8).trimStart() else ""
            bis.reset()
            if (!header.startsWith("{") && !header.startsWith("[")) throw IllegalArgumentException("Selected file is not a valid backup file.")
            InputStreamReader(bis, Charsets.UTF_8).use { isr ->
                val bundle = gson.fromJson(isr, BackupBundleDto::class.java)
                if (bundle?.metadata == null || bundle.db == null || bundle.datastore == null) {
                    throw IllegalArgumentException("Invalid backup format")
                }
                db.withTransaction {
                    db.clearAllTables()
                    val accountIdMap = mutableMapOf<Int, Int>()
                    val incomeGroupIdMap = mutableMapOf<Int, Int>()

                    bundle.db.accounts?.forEach { originalAccount ->
                        val newId = db.accountDao().insert(
                            originalAccount.name,
                            originalAccount.type,
                            originalAccount.balance
                        )
                        accountIdMap[originalAccount.id] = newId.toInt()
                    }
                    bundle.db.incomeGroups?.forEach { originalGroup ->
                        val newId = db.incomeGroupDao().insert(
                            originalGroup.name,
                            originalGroup.amount
                        )
                        incomeGroupIdMap[originalGroup.id] = newId.toInt()
                    }
                    bundle.db.transactions?.forEach { originalTransaction ->
                        val newAccountId = accountIdMap[originalTransaction.accountId]
                        val newTransferAccountId = originalTransaction.transferAccountId?.let { accountIdMap[it] }
                        val newRelatedIncomeGroupId = originalTransaction.relatedIncomeId?.let { incomeGroupIdMap[it] }

                        if (newAccountId != null) {
                            db.transactionDao().insert(
                                accountId = newAccountId,
                                type = originalTransaction.type ?: "expense",
                                amount = originalTransaction.amount,
                                date = originalTransaction.date ?: System.currentTimeMillis(),
                                description = originalTransaction.description ?: "",
                                relatedIncomeId = newRelatedIncomeGroupId,
                                transferAccountId = if (originalTransaction.type == "transfer") newTransferAccountId else null
                            )
                        }
                    }
                }
                bundle.datastore.let { dataStoreBackup ->
                    userDataStore.updateUserData(
                        name = dataStoreBackup.username ?: "User",
                        photoUri = dataStoreBackup.profilePicture ?: "",
                        currencySymbol = dataStoreBackup.currencySymbol ?: "$",
                    )
                    userDataStore.updateBalanceVisibility(
                        isVisible = dataStoreBackup.isBalanceVisible
                    )
                }
            }
        } ?: throw Exception("Could not open content stream")
    }
}