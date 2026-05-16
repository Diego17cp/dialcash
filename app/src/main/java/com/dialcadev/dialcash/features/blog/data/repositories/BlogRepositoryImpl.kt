package com.dialcadev.dialcash.features.blog.data.repositories

import com.dialcadev.dialcash.features.blog.data.mappers.toDomain
import com.dialcadev.dialcash.features.blog.data.remote.BlogRemoteDataSource
import com.dialcadev.dialcash.features.blog.domain.models.BlogPost
import com.dialcadev.dialcash.features.blog.domain.repositories.BlogRepository
import javax.inject.Inject

class BlogRepositoryImpl @Inject constructor(
    private val remoteDataSource: BlogRemoteDataSource
) : BlogRepository {
    override suspend fun getBlogPosts(): Result<List<BlogPost>> {
        return try {
            val dtoList = remoteDataSource.fetchBlogPosts()
            Result.success(dtoList.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}