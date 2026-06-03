package com.dialcadev.dialcash.core.updates

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import androidx.core.net.toUri
import com.dialcadev.dialcash.BuildConfig
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.core.models.GithubReleaseDto
import com.dialcadev.dialcash.core.updates.data.GithubApi
import com.dialcadev.dialcash.core.utils.extensions.hasInternet
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton


sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    object UpToDate : UpdateState()
    data class UpdateAvailable(val release: GithubReleaseDto) : UpdateState()
    data class Downloading(val progress: Int) : UpdateState()
    data class ReadyToInstall(val apkFile: File) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

@Singleton
class AppUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val githubApi: GithubApi
) {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state = _state.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var downloadJob: Job? = null

    fun checkForUpdates() {
        if (!context.hasInternet()) {
            _state.value = UpdateState.UpToDate
            return
        }
        _state.value = UpdateState.Checking
        coroutineScope.launch {
            val release = githubApi.fetchLatestRelease()
            if (release != null) {
                val latestVersion = release.tag_name.removePrefix("v")
                if (isNewerVersion(latestVersion, BuildConfig.VERSION_NAME)) _state.value = UpdateState.UpdateAvailable(release)
                else _state.value = UpdateState.UpToDate
            } else _state.value = UpdateState.UpToDate
        }
    }
    fun downloadUpdate(apkUrl: String) {
        val request = DownloadManager.Request(apkUrl.toUri())
            .setTitle(context.getString(R.string.updating_app))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "dialcash_latest.apk")
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val oldFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "dialcash_latest.apk")
        if (oldFile.exists()) oldFile.delete()

        val downloadId = manager.enqueue(request)
        downloadJob?.cancel()
        downloadJob = coroutineScope.launch {
            _state.value = UpdateState.Downloading(0)
            while (isActive) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = manager.query(query)

                if (cursor != null && cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "dialcash_latest.apk")
                        if (isValidApk(file)) _state.value = UpdateState.ReadyToInstall(file)
                        else _state.value = UpdateState.Error("Downloaded file is invalid.")
                        cursor.close()
                        break
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        _state.value = UpdateState.Error("Download failed.")
                        cursor.close()
                        break
                    } else {
                        if (total > 0) {
                            val progress = ((downloaded * 100) / total).toInt()
                            _state.value = UpdateState.Downloading(progress)
                        }
                    }
                    cursor.close()
                }
                delay(400)
            }
        }
    }
    fun resetState() {
        _state.value = UpdateState.Idle
    }
    private fun isValidApk(file: File): Boolean {
        val pm = context.packageManager
        val info = pm.getPackageArchiveInfo(file.absolutePath, 0)
        return info != null
    }
    private fun isNewerVersion(remote: String, current: String): Boolean {
        val remoteParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLength = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLength) {
            val remotePart = remoteParts.getOrNull(i) ?: 0
            val currentPart = currentParts.getOrNull(i) ?: 0
            if (remotePart > currentPart) return true
            if (remotePart < currentPart) return false
        }
        return false
    }
}