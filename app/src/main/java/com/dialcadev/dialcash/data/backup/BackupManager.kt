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
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.Charset
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
    suspend fun importFromInputStream(input: InputStream): BackupBundleDto {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("BackupManager", "Starting import from input stream")
                InputStreamReader(input, Charsets.UTF_8).use { isr ->
                    val bundle = gson.fromJson(isr, BackupBundleDto::class.java)
                    if (bundle?.metadata == null || bundle.db == null || bundle.datastore == null) {
                        throw IllegalArgumentException("Invalid backup format")
                    }
                    Log.d("BackupManager", "Import completed successfully")
                    bundle
                }
            } catch (e: Exception) {
                Log.e("BackupManager", "Error during import: ${e.message}", e)
                throw e
            }
        }
    }
    suspend fun restoreFromBundle(bundle: BackupBundleDto) {
        withContext(Dispatchers.IO){
            try {
                Log.d("BackupManager", "Starting db restore")

                // Clear existing data
                repo.wipeDatabase()
                bundle.db.accounts?.let { accounts ->
                    repo.insertAccountsBatch(accounts)
                    Log.d("BackupManager", "Inserted ${accounts.size} accounts")
                }
                bundle.db.incomeGroups?.let { groups ->
                    repo.insertIncomeGroupsBatch(groups)
                    Log.d("BackupManager", "Inserted ${groups.size} income groups")
                }
                bundle.db.transactions?.let { transactions ->
                    repo.insertTransactionsBatch(transactions)
                    Log.d("BackupManager", "Inserted ${transactions.size} transactions")
                }
                bundle.datastore?.let { dataStore ->
                    userDataStore.updateUserData(
                        name = dataStore.username ?: "",
                        photoUri = dataStore.profilePicture ?: ""
                    )
                    Log.d("BackupManager", "User data restored")
                }
                Log.d("BackupManager", "Database restore completed successfully")
            } catch (e: Exception) {
                Log.e("BackupManager", "Error during db restore: ${e.message}", e)
                throw e
            }
        }
    }
}