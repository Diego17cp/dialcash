package com.dialcadev.dialcash.features.settings.data.models.backup

import com.dialcadev.dialcash.features.accounts.domain.models.Account
import com.dialcadev.dialcash.features.incomegroups.domain.models.IncomeGroup
import com.dialcadev.dialcash.features.transactions.domain.models.Transaction

data class DatabaseBackup(
    val accounts: List<Account>,
    val transactions: List<Transaction>,
    val incomeGroups: List<IncomeGroup>
)