package com.dialcadev.dialcash.data

import com.dialcadev.dialcash.data.dto.BlogPostPreview
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import javax.inject.Inject

class BlogService @Inject constructor(
    private val client: OkHttpClient,
) {
    private val WEB_URL = "https://dialcash.vercel.app"
    private val BLOG_API_URL = "$WEB_URL/blog-feed.json"
    private val gson: Gson = Gson()

    fun fetchBlogFeed(
        onSuccess: (List<BlogPostPreview>) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val request = Request.Builder()
            .url(BLOG_API_URL)
            .get()
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        onError(IOException("HTTP ${response.code}: ${response.message}"))
                        return
                    }
                    val body = response.body?.string() ?: run {
                        onError(IOException("Empty response body"))
                        return
                    }
                    val type = object : TypeToken<List<BlogPostPreview>>() {}.type
                    val blogPosts: List<BlogPostPreview> = gson.fromJson(body, type)
                    val mappedPosts = blogPosts.map { post ->
                        post.copy(
                            portrait = if (post.portrait.startsWith("http")) post.portrait
                            else "$WEB_URL${post.portrait}"
                        )
                    }
                    onSuccess(mappedPosts)
                }
            }
        })
    }
}