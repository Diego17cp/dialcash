package com.dialcadev.dialcash.features.accounts.data.repositories

import com.dialcadev.dialcash.features.accounts.data.dao.AccountDao
import com.dialcadev.dialcash.features.accounts.domain.dtos.AccountBalanceWithOriginal
import com.dialcadev.dialcash.features.accounts.domain.models.Account
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AccountRepository @Inject constructor(
    private val accountDao: AccountDao
) {
    suspend fun createAccount(account: Account) = accountDao.insert(account.name, account.type, account.balance)
    suspend fun updateAccount(account: Account) = accountDao.updateAccount(account.id, account.name, account.type, account.balance)
    suspend fun deleteAccount(account: Account) = accountDao.delete(account)
    fun getAllAccounts(): Flow<List<Account>> = accountDao.getAllAccounts()
    fun getAllAccountBalances(): Flow<List<AccountBalanceWithOriginal>> = accountDao.getAccountBalances()
    fun getMainAccounts(): Flow<List<AccountBalanceWithOriginal>> = accountDao.getMainAccountBalances()
    suspend fun getAccountById(accountId: Int): Account? = accountDao.getAccountById(accountId)
    suspend fun getAccountByName(name: String): Account? = accountDao.getAccountByName(name)
    suspend fun getAccountCount(): Int = accountDao.getAccountCount()
    suspend fun insertAccountsBatch(accounts: List<Account>) = accountDao.insertBatch(accounts)
    suspend fun getAccountBalance(accountId: Int): Double? {
        return accountDao.getAccountBalance(accountId)
    }
}