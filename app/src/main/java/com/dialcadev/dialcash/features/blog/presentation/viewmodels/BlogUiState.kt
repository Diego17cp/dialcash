package com.dialcadev.dialcash.features.blog.presentation.viewmodels

import com.dialcadev.dialcash.features.blog.domain.models.BlogPost

data class BlogUiState(
    val posts: List<BlogPost> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
