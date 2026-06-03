package com.dialcadev.dialcash.features.settings.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dialcadev.dialcash.core.datastore.UserDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userDataStore: UserDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState = _uiState.asStateFlow()

    private var currentName = ""
    private var currentCurrency = ""
    private var isInitialized = false

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val user = userDataStore.getUserData().first()
            if (!isInitialized) {
                currentName = user.name ?: ""
                currentCurrency = user.currencySymbol ?: ""

                _uiState.update {
                    it.copy(
                        isInitialized = true,
                        initialName = currentName,
                        initialCurrency = currentCurrency,
                        currentPhotoUri = user.photoUri ?: ""
                    )
                }
                isInitialized = true
            }
        }
    }
    fun onNameChanged(name: String) {
        currentName = name
        validateFields()
    }
    fun onCurrencyChanged(currency: String) {
        currentCurrency = currency
        validateFields()
    }
    fun onPhotoPicked(uri: String) {
        _uiState.update { it.copy(currentPhotoUri = uri) }
    }
    private fun validateFields() {
        val isNameEmpty = currentName.trim().isEmpty()
        val isCurrencyEmpty = currentCurrency.trim().isEmpty()

        _uiState.update {
            it.copy(
                isNameError = isNameEmpty,
                isCurrencyError = isCurrencyEmpty,
                isSaveEnabled = !isNameEmpty && !isCurrencyEmpty
            )
        }
    }
    fun saveChanges() {
        if (currentName.trim().isEmpty() || currentCurrency.trim().isEmpty()) return

        _uiState.update { it.copy(isLoading = true) }

        val updatedCurrency = currentCurrency.substringBefore(" -").trim()

        viewModelScope.launch {
            try {
                userDataStore.updateUserData(
                    name = currentName.trim(),
                    photoUri = _uiState.value.currentPhotoUri.takeIf { it.isNotBlank() },
                    currencySymbol = updatedCurrency
                )
                _uiState.update { it.copy(isLoading = false, showSuccessMessage = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, showErrorMessage = true) }
            }
        }
    }
    fun onMessagesShown() {
        _uiState.update { it.copy(showSuccessMessage = false, showErrorMessage = false) }
    }
}