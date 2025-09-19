package com.dialcadev.dialcash.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserDataStore @Inject constructor(private val context: Context) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

    companion object {
        private val USER_NAME = stringPreferencesKey("user_name")
        private val USER_PHOTO_URI = stringPreferencesKey("user_photo_uri")
        private val IS_REGISTERED = booleanPreferencesKey("is_registered")
        private val SEEN_ONBOARDING = booleanPreferencesKey("seen_onboarding")
    }

    suspend fun saveUserData(
        name: String,
        photoUri: String? = null
    ) {
        context.dataStore.edit { prefs ->
            prefs[USER_NAME] = name
            prefs[USER_PHOTO_URI] = photoUri ?: ""
            prefs[IS_REGISTERED] = true
        }
    }
    suspend fun updateUserData(
        name: String,
        photoUri: String? = null
    ) {
        context.dataStore.edit { prefs ->
            prefs[USER_NAME] = name
            if (photoUri != null) {
                prefs[USER_PHOTO_URI] = photoUri
            }
        }
    }

    fun getUserData(): Flow<UserData> {
        return context.dataStore.data.map { prefs ->
            UserData(
                name = prefs[USER_NAME] ?: "",
                photoUri = prefs[USER_PHOTO_URI] ?: "",
                isRegistered = prefs[IS_REGISTERED] ?: false
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

data class UserData(
    val name: String,
    val photoUri: String,
    val isRegistered: Boolean
)