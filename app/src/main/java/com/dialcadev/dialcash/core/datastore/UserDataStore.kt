package com.dialcadev.dialcash.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dialcadev.dialcash.core.models.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserDataStore @Inject constructor(private val context: Context) {

    companion object {
        private val USER_NAME = stringPreferencesKey("user_name")
        private val USER_PHOTO_URI = stringPreferencesKey("user_photo_uri")
        private val IS_REGISTERED = booleanPreferencesKey("is_registered")
        private val SEEN_ONBOARDING = booleanPreferencesKey("seen_onboarding")
        private val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        private val IS_BALANCE_VISIBLE = booleanPreferencesKey("is_balance_visible")
    }

    suspend fun saveUserData(
        name: String,
        photoUri: String? = null,
        currencySymbol: String? = "$"
    ) {
        context.dataStore.edit { prefs ->
            prefs[USER_NAME] = name
            prefs[USER_PHOTO_URI] = photoUri ?: ""
            prefs[IS_REGISTERED] = true
            prefs[CURRENCY_SYMBOL] = currencySymbol ?: "$"
        }
    }
    suspend fun updateUserData(
        name: String,
        photoUri: String? = null,
        currencySymbol: String? = "$"
    ) {
        context.dataStore.edit { prefs ->
            prefs[USER_NAME] = name
            if (photoUri != null) {
                prefs[USER_PHOTO_URI] = photoUri
            }
            prefs[CURRENCY_SYMBOL] = currencySymbol ?: "$"
        }
    }

    suspend fun toggleBalanceVisibility() {
        context.dataStore.edit { prefs ->
            val current = prefs[IS_BALANCE_VISIBLE] ?: true
            prefs[IS_BALANCE_VISIBLE] = !current
        }
    }
    suspend fun updateBalanceVisibility(isVisible: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_BALANCE_VISIBLE] = isVisible
        }
    }

    fun getUserData(): Flow<UserPreferences> {
        return context.dataStore.data.map { prefs ->
            UserPreferences(
                name = prefs[USER_NAME] ?: "",
                photoUri = prefs[USER_PHOTO_URI] ?: "",
                isRegistered = prefs[IS_REGISTERED] ?: false,
                currencySymbol = prefs[CURRENCY_SYMBOL] ?: "$",
                isBalanceVisible = prefs[IS_BALANCE_VISIBLE] ?: true
            )
        }
    }

    fun isUserRegistered(): Flow<Boolean> {
        return context.dataStore.data.map {  prefs ->
            prefs[IS_REGISTERED] ?: false
        }
    }
    suspend fun clearUserProfile() {
        context.dataStore.edit { prefs ->
            prefs[USER_NAME] = ""
            prefs[USER_PHOTO_URI] = ""
            prefs[CURRENCY_SYMBOL] = "$"
            prefs[IS_BALANCE_VISIBLE] = true
        }
    }
    suspend fun clearUserData() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
    fun isOnboardingSeen(): Flow<Boolean> {
        return context.dataStore.data.map { prefs ->
            prefs[SEEN_ONBOARDING] ?: false
        }
    }
    suspend fun setOnboardingSeen(seen: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SEEN_ONBOARDING] = seen
        }
    }
}