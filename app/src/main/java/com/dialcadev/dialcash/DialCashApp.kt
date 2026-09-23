package com.dialcadev.dialcash

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DialCashApp : Application() {
    companion object {
        const val THEME_PREFS_NAME = "theme_prefs"
        const val KEY_NIGHT_MODE = "night_mode"
    }

    override fun onCreate() {
        super.onCreate()
        applyCachedTheme()
    }

    private fun applyCachedTheme() {
        val prefs = getSharedPreferences(THEME_PREFS_NAME, MODE_PRIVATE)
        val cachedMode = prefs.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(cachedMode)
    }
}