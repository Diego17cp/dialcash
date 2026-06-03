package com.dialcadev.dialcash.features.onboarding.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dialcadev.dialcash.core.datastore.UserDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userDataStore: UserDataStore
) : ViewModel() {

    val isSeen = userDataStore.isOnboardingSeen()
        .map { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun markSeen() {
        viewModelScope.launch {
            userDataStore.setOnboardingSeen(true)
        }
    }
}