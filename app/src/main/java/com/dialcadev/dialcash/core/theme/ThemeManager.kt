package com.dialcadev.dialcash.core.theme

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.dialcadev.dialcash.DialCashApp
import com.dialcadev.dialcash.core.datastore.UserDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class ThemeManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userDataStore: UserDataStore
) {
    suspend fun setThemeMode(mode: Int) {
        context.getSharedPreferences(DialCashApp.THEME_PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putInt(DialCashApp.KEY_NIGHT_MODE, mode)
            }
        userDataStore.updateThemeMode(mode)
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}