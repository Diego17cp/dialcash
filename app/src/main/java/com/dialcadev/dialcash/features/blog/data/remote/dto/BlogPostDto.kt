package com.dialcadev.dialcash.features.blog.data.remote.dto

data class BlogPostDto(
    val title: String,
    val description: String,
    val portrait: String,
    val publishedAt: String,
    val category: String,
    val slug: String
)
