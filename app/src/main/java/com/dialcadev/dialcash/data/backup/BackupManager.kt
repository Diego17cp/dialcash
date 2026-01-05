package com.dialcadev.dialcash.data.backup

import android.util.Log
import com.dialcadev.dialcash.data.AppRepository
import com.dialcadev.dialcash.data.UserDataStore
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import javax.inject.Inject
import javax.inject.Singleton
import androidx.room.withTransaction

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
                        profilePicture = userData.photoUri.takeIf { it.isNotEmpty() },
                        currencySymbol = userData.currencySymbol
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
        withContext(Dispatchers.IO) {
            try {
                Log.d("BackupManager", "Starting db restore")

                repo.database.withTransaction {
                    repo.wipeDatabase()
                    val accountIdMap = mutableMapOf<Int, Int>()
                    val incomeGroupIdMap = mutableMapOf<Int, Int>()
                    bundle.db.accounts?.forEach { originalAccount ->
                        val accountCopy = originalAccount.copy(id = 0)
                        val newId = repo.createAccount(accountCopy)
                        accountIdMap[originalAccount.id] = newId.toInt()
                        Log.d("BackupManager", "Account remapped: ${originalAccount.id} -> $newId")
                    }
                    bundle.db.incomeGroups?.forEach { originalGroup ->
                        val groupCopy = originalGroup.copy(id = 0)
                        val newId = repo.createIncomeGroup(groupCopy)
                        incomeGroupIdMap[originalGroup.id] = newId.toInt()
                        Log.d(
                            "BackupManager",
                            "IncomeGroup remapped: ${originalGroup.id} -> $newId"
                        )
                    }
                    bundle.db.transactions?.forEach { originalTransaction ->
                        val newAccountId = accountIdMap[originalTransaction.accountId]
                        val newTransferAccountId = originalTransaction.transferAccountId?.let { accountIdMap[it] }
                        val newRelatedIncomeId = originalTransaction.relatedIncomeId?.let { incomeGroupIdMap[it] }
                        if (newAccountId != null) {
                            when (originalTransaction.type) {
                                "income" -> repo.addIncome(
                                    accountId = newAccountId,
                                    amount = originalTransaction.amount,
                                    description = originalTransaction.description ?: "",
                                    relatedIncomeId = newRelatedIncomeId,
                                    date = originalTransaction.date
                                )
                                "expense" -> repo.addExpense(
                                    accountId = newAccountId,
                                    amount = originalTransaction.amount,
                                    description = originalTransaction.description ?: "",
                                    relatedIncomeId = newRelatedIncomeId,
                                    date = originalTransaction.date
                                )
                                "transfer" -> {
                                    if (newTransferAccountId != null) {
                                        repo.makeTransfer(
                                            fromAccountId = newAccountId,
                                            toAccountId = newTransferAccountId,
                                            amount = originalTransaction.amount,
                                            description = originalTransaction.description ?: "",
                                            date = originalTransaction.date
                                        )
                                    }
                                }
                            }
                            Log.d(
                                "BackupManager",
                                "Transaction remapped: account ${originalTransaction.accountId} -> $newAccountId"
                            )
                        } else {
                            Log.w(
                                "BackupManager",
                                "Could not find mapped account for transaction ${originalTransaction.id}"
                            )
                        }
                    }

                    Log.d(
                        "BackupManager",
                        "Inserted ${bundle.db.accounts?.size ?: 0} accounts, ${bundle.db.incomeGroups?.size ?: 0} income groups, ${bundle.db.transactions?.size ?: 0} transactions"
                    )
                }
                bundle.datastore?.let { dataStore ->
                    userDataStore.updateUserData(
                        name = dataStore.username ?: "",
                        photoUri = dataStore.profilePicture ?: "",
                        currencySymbol = dataStore.currencySymbol ?: "$"
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