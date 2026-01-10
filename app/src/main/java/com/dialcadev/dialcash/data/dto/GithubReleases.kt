package com.dialcadev.dialcash.data.dto

data class GithubReleaseDto(
    val tag_name: String,
    val name: String,
    val body: String,
    val assets: List<GithubAssetDto>
)

data class GithubAssetDto(
    val name: String,
    val browser_download_url: String
)

