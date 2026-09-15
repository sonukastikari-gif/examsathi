package com.example.data.update.model

import java.io.File

/**
 * Information about currently installed app version.
 */
data class AppVersionInfo(
    val versionCode: Long,
    val versionName: String,
    val packageName: String
)

/**
 * Parsed metadata fetched from the HTTPS version endpoint.
 */
data class RemoteUpdateMetadata(
    val versionCode: Long,
    val versionName: String,
    val minSupportedVersionCode: Long = 1L,
    val title: String,
    val releaseDate: String,
    val changelog: List<String>,
    val apkUrl: String,
    val apkSizeBytes: Long = 0L,
    val sha256Checksum: String? = null,
    val isMandatory: Boolean = false
) {
    fun formatSizeMb(): String {
        return if (apkSizeBytes > 0) {
            String.format("%.1f MB", apkSizeBytes / (1024.0 * 1024.0))
        } else {
            "Standard APK"
        }
    }
}

/**
 * Live download progress tracking.
 */
data class DownloadProgress(
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val progressPercent: Int = 0
) {
    val percent: Int get() = progressPercent

    fun formatProgressText(): String {
        return if (totalBytes > 0) {
            val downloadedMb = bytesDownloaded / (1024.0 * 1024.0)
            val totalMb = totalBytes / (1024.0 * 1024.0)
            String.format("%.1f / %.1f MB (%d%%)", downloadedMb, totalMb, progressPercent)
        } else {
            "$progressPercent%"
        }
    }
}

/**
 * Sealed class representing comprehensive update status for UI consumption.
 */
sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    data class UpToDate(val versionName: String, val checkedAt: Long) : UpdateStatus()
    data class Available(val metadata: RemoteUpdateMetadata, val isMandatory: Boolean) : UpdateStatus()
    data class Downloading(val metadata: RemoteUpdateMetadata, val progress: DownloadProgress) : UpdateStatus()
    data class ReadyToInstall(val metadata: RemoteUpdateMetadata, val apkFile: File) : UpdateStatus()
    data class Failed(val message: String, val isNetworkUnavailable: Boolean, val metadata: RemoteUpdateMetadata? = null) : UpdateStatus()
}
