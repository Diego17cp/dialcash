package com.dialcadev.dialcash.data

import com.dialcadev.dialcash.data.dto.GithubReleaseDto
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import javax.inject.Inject

class GithubApi @Inject constructor(
    private val client: OkHttpClient
) {
    private val GITHUB_USER = "Diego17cp"
    private val GITHUB_REPO = "dialcash"
    private val RELEASES_API_URL = "https://api.github.com/repos/$GITHUB_USER/$GITHUB_REPO/releases/latest"

    fun fetchLatestRelease(
        onResult: (GithubReleaseDto?) -> Unit,
    ) {
        val req = Request.Builder()
            .url(RELEASES_API_URL)
            .get()
            .build()
        client.newCall(req).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    onResult(null)
                    return
                }
                val json = response.body?.string()
                val dto = Gson().fromJson(json, GithubReleaseDto::class.java)
                onResult(dto)
            }
        })
    }
}