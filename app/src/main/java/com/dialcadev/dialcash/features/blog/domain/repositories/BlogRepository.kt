package com.dialcadev.dialcash.features.blog.domain.repositories

import com.dialcadev.dialcash.features.blog.domain.models.BlogPost

interface BlogRepository {
    suspend fun getBlogPosts(): Result<List<BlogPost>>
}