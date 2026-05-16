package com.dialcadev.dialcash.features.blog.presentation.viewmodels

import androidx.lifecycle.ViewModel
import com.dialcadev.dialcash.features.blog.domain.usecases.GetBlogPostsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

@HiltViewModel
class BlogViewModel @Inject constructor(
    private val getBlogPostsUseCase: GetBlogPostsUseCase
): ViewModel() {
    private val _uiState = MutableStateFlow(BlogUiState())
    val uiState: StateFlow<BlogUiState> = _uiState.asStateFlow()
    init {
        loadBlogFeed()
    }
    fun loadBlogFeed() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = getBlogPostsUseCase()
            result.fold(
                onSuccess = { posts ->
                    _uiState.value = _uiState.value.copy(
                        posts = posts,
                        isLoading = false,
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "An unknown error occurred"
                    )
                }
            )
        }
    }
}