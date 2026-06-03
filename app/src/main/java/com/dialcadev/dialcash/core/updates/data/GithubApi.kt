package com.dialcadev.dialcash.core.updates.data

import com.dialcadev.dialcash.core.models.GithubReleaseDto
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GithubApi @Inject constructor(
    private val client: OkHttpClient
) {
    private val GITHUB_USER = "Diego17cp"
    private val GITHUB_REPO = "dialcash"
    private val RELEASES_API_URL = "https://api.github.com/repos/$GITHUB_USER/$GITHUB_REPO/releases/latest"

    suspend fun fetchLatestRelease(): GithubReleaseDto? = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(RELEASES_API_URL)
            .get()
            .build()
        try {
            val response = client.newCall(req).execute()
            if (response.isSuccessful) Gson().fromJson(response.body?.string(), GithubReleaseDto::class.java)
            else null
        } catch (e: Exception) {
            null
        }
    }
}