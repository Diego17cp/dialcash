package com.dialcadev.dialcash.features.blog.data.remote

import com.dialcadev.dialcash.features.blog.data.remote.dto.BlogPostDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class BlogRemoteDataSource @Inject constructor(
    private val client: OkHttpClient
) {
    private val WEB_URL = "https://dialcash.vercel.app"
    private val BLOG_API_URL = "$WEB_URL/blog-feed.json"
    private val gson = Gson()
    suspend fun fetchBlogPosts(): List<BlogPostDto> = suspendCancellableCoroutine { continuation ->
        val request = Request.Builder()
            .url(BLOG_API_URL)
            .get()
            .build()
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!continuation.isCancelled) continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        continuation.resumeWithException(IOException("HTTP ${response.code}: ${response.message}"))
                        return
                    }
                    val body = response.body?.string() ?: run {
                        continuation.resumeWithException(IOException("Empty response body"))
                        return
                    }
                    try {
                        val type = object : TypeToken<List<BlogPostDto>>() {}.type
                        val blogPosts: List<BlogPostDto> = gson.fromJson(body, type)
                        if (continuation.isActive) continuation.resume(blogPosts)
                    } catch (e: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(e)
                    }
                }
            }
        })
    }
}