package com.dialcadev.dialcash.features.blog.di

import com.dialcadev.dialcash.features.blog.data.repositories.BlogRepositoryImpl
import com.dialcadev.dialcash.features.blog.domain.repositories.BlogRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BlogModule {

    @Binds
    @Singleton
    abstract fun bindBlogRepository(
        blogRepositoryImpl: BlogRepositoryImpl
    ): BlogRepository
}