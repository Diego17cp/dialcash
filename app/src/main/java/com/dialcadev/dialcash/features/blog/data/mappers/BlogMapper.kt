package com.dialcadev.dialcash.features.blog.data.mappers

import com.dialcadev.dialcash.features.blog.data.remote.dto.BlogPostDto
import com.dialcadev.dialcash.features.blog.domain.models.BlogPost

private const val WEB_URL = "https://dialcash.vercel.app"

fun BlogPostDto.toDomain(): BlogPost {
    val fullPortraitUrl = if (this.portrait.startsWith("http")) {
        this.portrait
    } else {
        "$WEB_URL${this.portrait}"
    }

    return BlogPost(
        title = this.title,
        description = this.description,
        portrait = fullPortraitUrl,
        publishedAt = this.publishedAt,
        category = this.category,
        slug = this.slug
    )
}