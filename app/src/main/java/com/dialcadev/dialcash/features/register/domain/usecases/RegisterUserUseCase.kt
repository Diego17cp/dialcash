package com.dialcadev.dialcash.features.register.domain.usecases

import com.dialcadev.dialcash.core.datastore.UserDataStore
import javax.inject.Inject

class RegisterUserUseCase @Inject constructor(
    private val userDataStore: UserDataStore
) {
    suspend operator fun invoke(name: String, currency: String, profilePictureUri: String?): Result<Unit> {
        if (name.isBlank()) return Result.failure(IllegalArgumentException("Name cannot be empty"))
        if (currency.isBlank()) return Result.failure(IllegalArgumentException("Currency cannot be empty"))
        val currencySymbol = currency.trim().substringBefore(" -")
        return try {
            userDataStore.saveUserData(
                name = name.trim(),
                currencySymbol = currencySymbol,
                photoUri = profilePictureUri
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}