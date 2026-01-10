package com.dialcadev.dialcash.data.updates

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.dialcadev.dialcash.R
import com.dialcadev.dialcash.data.GithubApi
import com.dialcadev.dialcash.data.dto.GithubReleaseDto
import com.dialcadev.dialcash.utils.hasInternet
import com.dialcadev.dialcash.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val githubApi: GithubApi
) {
    private var downloadId: Long = -1
    private var progressDialog: AlertDialog? = null
    private var currentActivity: AppCompatActivity? = null
    private var downloadReceiver: BroadcastReceiver? = null
    private var pendingApkUrl: String? = null

    companion object {
        private const val TAG = "UpdateManager"
    }

    fun checkForUpdates(activity: AppCompatActivity, onUpToDate: () -> Unit) {
        currentActivity = activity
        pendingApkUrl?.let { url ->
            pendingApkUrl = null
            downloadApk(url)
            return
        }
        if (!context.hasInternet()) {
            onUpToDate()
            return
        }
        githubApi.fetchLatestRelease { release ->
            if (release == null) {
                Log.w(TAG, "No release found")
                onUpToDate()
                return@fetchLatestRelease
            }
            val latestVersion = release.tag_name.removePrefix("v")
            val currentVersion = BuildConfig.VERSION_NAME
            Log.d(TAG, "Latest version: $latestVersion, Current version: $currentVersion")

            if (isNewerVersion(latestVersion, currentVersion)) {
                Log.d(TAG, "Update available")
                showUpdateDialog(release)
            } else {
                Log.d(TAG, "App is up to date")
                onUpToDate()
            }
        }
    }
    private fun isNewerVersion(remote: String, current: String): Boolean {
        try {
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
        } catch (e: Exception) {
            Log.e(TAG, "Error comparing versions", e)
            return false
        }
    }
    private fun showUpdateDialog(release: GithubReleaseDto) {
        val activity = currentActivity ?: return
        val apkUrl = release.assets
            .firstOrNull { it.name.endsWith(".apk") }
            ?.browser_download_url
            ?: return

        Log.d(TAG, "Showing update dialog for: ${release.tag_name}")

        Handler(Looper.getMainLooper()).post {
            AlertDialog.Builder(activity)
                .setTitle(R.string.update_available)
                .setMessage("${activity.getString(R.string.new_version)} ${release.tag_name}\n\n${release.name}")
                .setCancelable(true)
                .setPositiveButton(R.string.update) { _, _ ->
                    checkInstallPermissionAndDownload(apkUrl)
                }
                .setNegativeButton(R.string.later, null)
                .show()
        }
    }

    private fun checkInstallPermissionAndDownload(apkUrl: String) {
        Log.d(TAG, "Checking install permissions")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canInstall = context.packageManager.canRequestPackageInstalls()
            Log.d(TAG, "Can request package installs: $canInstall")

            if (!canInstall) {
                pendingApkUrl = apkUrl
                showInstallPermissionDialog()
                return
            }
        }
        downloadApk(apkUrl)
    }

    private fun downloadApk(apkUrl: String) {
        Log.d(TAG, "Starting download: $apkUrl")
        cleanupOldDownload()
        val request = DownloadManager.Request(apkUrl.toUri())
            .setTitle(context.getString(R.string.updating_app))
            .setDescription(context.getString(R.string.downloading_new_version))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "dialcash_latest.apk"
            )
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = manager.enqueue(request)
        Log.d(TAG, "Download enqueued with ID: $downloadId")

        showProgressDialog()
        registerDownloadReceiver()
        startDownloadProgressMonitor()
    }

    private fun cleanupOldDownload() {
        try {
            val oldFile = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "dialcash_latest.apk"
            )
            if (oldFile.exists()) {
                val deleted = oldFile.delete()
                Log.d(TAG, "Old APK deleted: $deleted")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up old download", e)
        }
    }

    private fun showProgressDialog() {
        val activity = currentActivity ?: return
        Handler(Looper.getMainLooper()).post {
            progressDialog?.dismiss()
            progressDialog = AlertDialog.Builder(activity)
                .setTitle(R.string.downloading_update)
                .setMessage("0%")
                .setCancelable(false)
                .create()
            progressDialog?.show()
            Log.d(TAG, "Progress dialog shown")
        }
    }

    private fun updateProgressDialog(progress: Int) {
        Handler(Looper.getMainLooper()).post {
            progressDialog?.setMessage("$progress%")
        }
    }

    private fun dismissProgressDialog() {
        Handler(Looper.getMainLooper()).post {
            progressDialog?.dismiss()
            progressDialog = null
            Log.d(TAG, "Progress dialog dismissed")
        }
    }

    private fun startDownloadProgressMonitor() {
        Log.d(TAG, "Starting download progress monitor")
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                try {
                    val manager =
                        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = manager.query(query)

                    if (cursor != null && cursor.moveToFirst()) {
                        val bytesDownloaded = cursor.getLong(
                            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        )
                        val bytesTotal = cursor.getLong(
                            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        )
                        val status = cursor.getInt(
                            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                        )

                        val statusText = when (status) {
                            DownloadManager.STATUS_PENDING -> "PENDING"
                            DownloadManager.STATUS_RUNNING -> "RUNNING"
                            DownloadManager.STATUS_PAUSED -> "PAUSED"
                            DownloadManager.STATUS_SUCCESSFUL -> "SUCCESSFUL"
                            DownloadManager.STATUS_FAILED -> "FAILED"
                            else -> "UNKNOWN($status)"
                        }
                        if (bytesTotal > 0) {
                            val progress = ((bytesDownloaded * 100) / bytesTotal).toInt()
                            updateProgressDialog(progress)
                            Log.d(
                                TAG,
                                "Download progress: $progress% ($bytesDownloaded/$bytesTotal bytes) - Status: $statusText"
                            )
                        } else {
                            Log.d(TAG, "Download status: $statusText - Total bytes unknown")
                        }
                        when (status) {
                            DownloadManager.STATUS_RUNNING,
                            DownloadManager.STATUS_PENDING,
                            DownloadManager.STATUS_PAUSED -> {
                                handler.postDelayed(this, 300)
                            }

                            DownloadManager.STATUS_SUCCESSFUL -> {
                                Log.d(TAG, "Download status is SUCCESSFUL, verifying file...")
                                cursor.close()
                                verifyAndInstall()
                            }

                            DownloadManager.STATUS_FAILED -> {
                                val reason = cursor.getInt(
                                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)
                                )
                                Log.e(TAG, "Download failed with reason: $reason")
                                cursor.close()
                                dismissProgressDialog()
                                showDownloadErrorDialog()
                                unregisterDownloadReceiver()
                            }
                        }
                        if (status != DownloadManager.STATUS_SUCCESSFUL &&
                            status != DownloadManager.STATUS_FAILED
                        ) {
                            cursor.close()
                        }
                    } else {
                        Log.w(TAG, "Cursor is null or empty")
                        cursor?.close()
                        handler.postDelayed(this, 300)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in progress monitor", e)
                    dismissProgressDialog()
                    showDownloadErrorDialog()
                    unregisterDownloadReceiver()
                }
            }
        }
        handler.post(runnable)
    }

    private fun verifyAndInstall() {
        Log.d(TAG, "Verifying downloaded file before installation...")
        val apkFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "dialcash_latest.apk"
        )
        if (!apkFile.exists()) {
            Log.e(TAG, "APK file does not exist: ${apkFile.absolutePath}")
            dismissProgressDialog()
            showDownloadErrorDialog()
            unregisterDownloadReceiver()
            return
        }
        val fileSize = apkFile.length()
        Log.d(TAG, "APK file exists: ${apkFile.absolutePath}, size: $fileSize bytes")
        if (!isValidApk(apkFile)) {
            Log.e(TAG, "Downloaded file is not a valid APK")
            dismissProgressDialog()
            showDownloadErrorDialog()
            unregisterDownloadReceiver()
            return
        }

        Log.d(TAG, "APK validation passed, proceeding to install")
        updateProgressDialog(100)

        Handler(Looper.getMainLooper()).postDelayed({
            dismissProgressDialog()
            showInstallDialog(apkFile)
            unregisterDownloadReceiver()
        }, 500)
    }

    private fun isValidApk(file: File): Boolean {
        return try {
            Log.d(TAG, "Validating APK at: ${file.absolutePath}")
            val pm = context.packageManager
            val info = pm.getPackageArchiveInfo(file.absolutePath, 0)
            val isValid = info != null

            if (isValid) {
                Log.d(
                    TAG,
                    "APK is valid - Package: ${info.packageName}, Version: ${info.versionName}"
                )
            } else {
                Log.e(TAG, "APK validation failed - getPackageArchiveInfo returned null")
            }

            isValid
        } catch (e: Exception) {
            Log.e(TAG, "Error validating APK", e)
            false
        }
    }

    private fun registerDownloadReceiver() {
        Log.d(TAG, "Registering download receiver")
        unregisterDownloadReceiver()

        downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: -1
                Log.d(TAG, "BroadcastReceiver triggered - Download ID: $id (expected: $downloadId)")

                if (id == downloadId) {
                    Log.d(TAG, "Download IDs match - calling onDownloadComplete")
                    onDownloadComplete()
                } else {
                    Log.w(TAG, "Download ID mismatch - ignoring")
                }
            }
        }

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            Log.d(TAG, "Receiver registered with RECEIVER_NOT_EXPORTED")
        } else {
            ContextCompat.registerReceiver(
                context,
                downloadReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            Log.d(TAG, "Receiver registered without flags")
        }
    }

    private fun onDownloadComplete() {
        Log.d(TAG, "onDownloadComplete called")
        try {
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = manager.query(query)

            if (cursor != null && cursor.moveToFirst()) {
                val status =
                    cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                Log.d(TAG, "Download status in onDownloadComplete: $status")

                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    val apkFile = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        "dialcash_latest.apk"
                    )
                    Log.d(
                        TAG,
                        "APK file path: ${apkFile.absolutePath}, exists: ${apkFile.exists()}, size: ${apkFile.length()} bytes"
                    )

                    updateProgressDialog(100)
                    Log.d(TAG, "Updated progress to 100%")

                    Handler(Looper.getMainLooper()).postDelayed({
                        dismissProgressDialog()
                        if (apkFile.exists()) {
                            Log.d(TAG, "Showing install dialog")
                            showInstallDialog(apkFile)
                        } else {
                            Log.e(TAG, "APK file does not exist after download")
                            showDownloadErrorDialog()
                        }
                    }, 500)
                } else {
                    Log.w(TAG, "Download not successful, status: $status")
                }
                cursor.close()
            } else {
                Log.e(TAG, "Cursor is null or empty in onDownloadComplete")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDownloadComplete", e)
            dismissProgressDialog()
            showDownloadErrorDialog()
        } finally {
            unregisterDownloadReceiver()
        }
    }

    private fun unregisterDownloadReceiver() {
        downloadReceiver?.let {
            try {
                context.unregisterReceiver(it)
                Log.d(TAG, "Download receiver unregistered")
            } catch (e: Exception) {
                Log.w(TAG, "Error unregistering receiver", e)
            }
            downloadReceiver = null
        }
    }

    private fun showInstallDialog(apkFile: File) {
        val activity = currentActivity ?: return
        Log.d(TAG, "Showing install dialog")
        Handler(Looper.getMainLooper()).post {
            AlertDialog.Builder(activity)
                .setTitle(R.string.download_complete)
                .setMessage(R.string.install_update_message)
                .setCancelable(true)
                .setPositiveButton(R.string.install) { _, _ ->
                    Log.d(TAG, "User tapped Install")
                    installApk(apkFile)
                }
                .setNegativeButton(R.string.later) { _, _ ->
                    Log.d(TAG, "User tapped Later")
                }
                .show()
        }
    }

    private fun showDownloadErrorDialog() {
        val activity = currentActivity ?: return
        Log.e(TAG, "Showing download error dialog")
        Handler(Looper.getMainLooper()).post {
            AlertDialog.Builder(activity)
                .setTitle(R.string.error)
                .setMessage(R.string.download_failed)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    private fun installApk(apkFile: File) {
        Log.d(TAG, "Installing APK: ${apkFile.absolutePath}")
        if (!apkFile.exists()) {
            Log.e(TAG, "APK file does not exist at install time")
            showDownloadErrorDialog()
            return
        }
        try {
            val uri =
                FileProvider.getUriForFile(context, "${context.packageName}.provider", apkFile)
            Log.d(TAG, "FileProvider URI: $uri")

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.d(TAG, "Install intent launched")
        } catch (e: Exception) {
            Log.e(TAG, "Error launching installer", e)
            showDownloadErrorDialog()
        }
    }

    private fun showInstallPermissionDialog() {
        val activity = currentActivity ?: return
        Log.d(TAG, "Showing install permission dialog")
        Handler(Looper.getMainLooper()).post {
            AlertDialog.Builder(activity)
                .setTitle(R.string.permission_required)
                .setMessage(R.string.install_permission_message)
                .setCancelable(false)
                .setPositiveButton(R.string.grant_permission) { _, _ ->
                    Log.d(TAG, "User granted permission, opening settings")
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = "package:${context.packageName}".toUri()
                    }
                    activity.startActivity(intent)
                    Handler(Looper.getMainLooper()).postDelayed({
                        Log.d(TAG, "Closing app to apply permission changes")
                        activity.finishAffinity()
                    }, 300)
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    Log.d(TAG, "User cancelled permission request")
                    pendingApkUrl = null
                }
                .show()
        }
    }

    fun onActivityDestroyed() {
        Log.d(TAG, "Activity destroyed, cleaning up")
        currentActivity = null
        progressDialog?.dismiss()
        progressDialog = null
        unregisterDownloadReceiver()
    }
}