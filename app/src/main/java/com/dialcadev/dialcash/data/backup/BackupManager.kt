package com.dialcadev.dialcash.data.backup

import android.util.Log
import com.dialcadev.dialcash.data.AppRepository
import com.dialcadev.dialcash.data.UserDataStore
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.io.OutputStreamWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    private val repo: AppRepository,
    private val userDataStore: UserDataStore,
) {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun exportToOutputStream(
        output: OutputStream,
        start: Long = 0L,
        end: Long = Long.MAX_VALUE
    ) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("BackupManager", "Starting export to output stream")
                val accounts = repo.getAllAccounts().first()
                val incomeGroups = repo.getAllIncomeGroups().first()
                val transactions = repo.getTransactionBetween(start, end).first()
                val userData = userDataStore.getUserData().first()

                val bundle = BackupBundleDto(
                    metadata = BackupMetadata(
                        schemaVersion = 1,
                        appVersion = "1.0.0",
                        exportedAt = System.currentTimeMillis(),
                        totalAccounts = accounts.size,
                        totalTransactions = transactions.size,
                        totalIncomeGroups = incomeGroups.size
                    ),
                    datastore = DataStoreBackup(
                        username = userData.name,
                        profilePicture = userData.photoUri.takeIf { it.isNotEmpty() }
                    ),
                    db = DatabaseBackup(
                        accounts = accounts,
                        transactions = transactions,
                        incomeGroups = incomeGroups
                    )
                )

                OutputStreamWriter(output, Charsets.UTF_8).use { osw ->
                    JsonWriter(osw).use { jw ->
                        gson.toJson(bundle, BackupBundleDto::class.java, jw)
                        jw.flush()
                    }
                }
                Log.d("BackupManager", "Export completed successfully")
            } catch (e: Exception) {
                Log.e("BackupManager", "Error during export: ${e.message}", e)
                throw e
            }
        }
    }
}