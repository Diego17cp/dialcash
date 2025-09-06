package com.dialcadev.dialcash.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.dialcadev.dialcash.data.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DeleteAccountViewModel @Inject constructor(private val repo: AppRepository): ViewModel() {
    suspend fun deleteData() {
        repo.wipeDatabase()
    }
}