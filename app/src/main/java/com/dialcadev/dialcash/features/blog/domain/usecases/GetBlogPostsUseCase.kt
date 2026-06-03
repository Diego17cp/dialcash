package com.dialcadev.dialcash.features.blog.domain.usecases

import com.dialcadev.dialcash.features.blog.domain.models.BlogPost
import com.dialcadev.dialcash.features.blog.domain.repositories.BlogRepository
import javax.inject.Inject

class GetBlogPostsUseCase @Inject constructor(
    private val repository: BlogRepository
) {
    suspend operator fun invoke(): Result<List<BlogPost>> {
        return repository.getBlogPosts()
    }
}