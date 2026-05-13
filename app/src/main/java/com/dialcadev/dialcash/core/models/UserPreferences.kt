package com.dialcadev.dialcash.core.models

data class UserPreferences(
    val name: String,
    val photoUri: String,
    val isRegistered: Boolean,
    val currencySymbol: String,
    val isBalanceVisible: Boolean = true
)
