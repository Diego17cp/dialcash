package com.dialcadev.dialcash.ui.blog

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dialcadev.dialcash.data.BlogService
import com.dialcadev.dialcash.data.dto.BlogPostPreview
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BlogViewModel @Inject constructor(
    private val blogService: BlogService
) : ViewModel() {
    private val _posts = MutableLiveData<List<BlogPostPreview>>()
    val posts: LiveData<List<BlogPostPreview>> = _posts
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadBlogFeed()
    }

    fun loadBlogFeed() {
        _isLoading.postValue(true)
        _error.postValue(null)
        blogService.fetchBlogFeed(
            onSuccess = { _posts.postValue(it); _isLoading.postValue(false); _error.postValue(null) },
            onError = { it.printStackTrace(); _isLoading.postValue(false); _error.postValue(it.message ?: "Error") }
        )
    }

    fun refreshBlogFeed() {
        loadBlogFeed()
    }
}