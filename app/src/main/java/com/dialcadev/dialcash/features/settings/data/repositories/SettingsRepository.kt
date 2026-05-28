package com.dialcadev.dialcash.features.settings.data.repositories

import com.dialcadev.dialcash.core.database.AppDB
import javax.inject.Inject

class SettingsRepository @Inject constructor(
    private val db: AppDB
) {
    fun wipeDatabase() {
        db.clearAllTables()
    }
}