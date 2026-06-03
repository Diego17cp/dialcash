package com.dialcadev.dialcash.features.settings.domain.usecases

import com.dialcadev.dialcash.core.datastore.UserDataStore
import com.dialcadev.dialcash.features.settings.data.repositories.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DeleteAccountUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val userDataStore: UserDataStore
) {
    suspend operator fun invoke() = withContext(Dispatchers.IO) {
        userDataStore.clearUserProfile()
        settingsRepository.wipeDatabase()
    }
}