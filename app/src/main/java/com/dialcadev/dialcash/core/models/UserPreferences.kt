package com.dialcadev.dialcash.core.models

import androidx.appcompat.app.AppCompatDelegate

data class UserPreferences(
    val name: String,
    val photoUri: String,
    val isRegistered: Boolean,
    val currencySymbol: String,
    val isBalanceVisible: Boolean = true,
    val themeMode: Int = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
)
