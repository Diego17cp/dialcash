package com.dialcadev.dialcash.features.settings.data.models.backup

data class DataStoreBackup(
    val username: String,
    val profilePicture: String?, // Uri as String
    val currencySymbol: String, // e.g. "$", "€"
    val isBalanceVisible: Boolean = true
)