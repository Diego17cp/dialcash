package com.dialcadev.dialcash.features.blog.domain.models

data class BlogPost(
    val title: String,
    val description: String,
    val portrait: String,
    val publishedAt: String,
    val category: String,
    val slug: String,
)
