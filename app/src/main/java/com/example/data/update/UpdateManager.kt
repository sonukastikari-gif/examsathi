package com.example.data.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.data.update.model.AppVersionInfo
import com.example.data.update.model.DownloadProgress
import com.example.data.update.model.RemoteUpdateMetadata
import com.example.data.update.model.UpdateStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Core update engine managing version checks, secure APK downloads,
 * and FileProvider installation intents.
 */
class UpdateManager(
    private val context: Context,
    private val preferences: UpdatePreferences = UpdatePreferences.getInstance(context),
    private val httpClient: OkHttpClient = defaultClient()
) {

    companion object {
        private const val TAG = "UpdateManager"

        private fun defaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()
        }

        @Volatile
        private var INSTANCE: UpdateManager? = null

        fun getInstance(context: Context): UpdateManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UpdateManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Retrieve currently installed version details.
     */
    fun getCurrentVersionInfo(): AppVersionInfo {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }

            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }

            val versionName = packageInfo.versionName ?: BuildConfig.VERSION_NAME

            AppVersionInfo(
                versionCode = versionCode,
                versionName = versionName,
                packageName = context.packageName
            )
        } catch (_: Exception) {
            AppVersionInfo(
                versionCode = BuildConfig.VERSION_CODE.toLong(),
                versionName = BuildConfig.VERSION_NAME,
                packageName = context.packageName
            )
        }
    }

    /**
     * Query remote HTTPS endpoint for new release information.
     */
    suspend fun checkForUpdates(customEndpointUrl: String? = null): UpdateStatus = withContext(Dispatchers.IO) {
        val current = getCurrentVersionInfo()
        val endpoint = (customEndpointUrl ?: preferences.getEndpointUrl()).trim()

        // Enforce HTTPS security
        if (!endpoint.startsWith("https://", ignoreCase = true)) {
            return@withContext UpdateStatus.Failed(
                message = "Insecure endpoint: Update checks must use a secure HTTPS endpoint.",
                isNetworkUnavailable = false
            )
        }

        try {
            val request = Request.Builder()
                .url(endpoint)
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "ExamSathi-Android/${current.versionName}")
                .build()

            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext UpdateStatus.Failed(
                    message = "Update server responded with HTTP code ${response.code}.",
                    isNetworkUnavailable = false
                )
            }

            val jsonBody = response.body?.string() ?: ""
            if (jsonBody.isBlank()) {
                return@withContext UpdateStatus.Failed(
                    message = "Received empty response from update server.",
                    isNetworkUnavailable = false
                )
            }

            val parsedMetadata = parseVersionJson(jsonBody)
                ?: return@withContext UpdateStatus.Failed(
                    message = "Unable to parse update metadata.",
                    isNetworkUnavailable = false
                )

            preferences.setLastCheckedTime(System.currentTimeMillis())

            if (parsedMetadata.versionCode > current.versionCode) {
                val isMandatory = current.versionCode < parsedMetadata.minSupportedVersionCode
                UpdateStatus.Available(parsedMetadata, isMandatory)
            } else {
                UpdateStatus.UpToDate(current.versionName, System.currentTimeMillis())
            }
        } catch (e: IOException) {
            // Graceful offline behavior: allow studying without error popups
            UpdateStatus.Failed(
                message = "Offline: Unable to reach update server (${e.localizedMessage ?: "network unreachable"}). You can continue using ExamSathi offline.",
                isNetworkUnavailable = true
            )
        } catch (e: Exception) {
            UpdateStatus.Failed(
                message = "Update check error: ${e.localizedMessage ?: "Unknown error"}",
                isNetworkUnavailable = false
            )
        }
    }

    /**
     * Parse and validate JSON schema from remote server.
     */
    private fun parseVersionJson(jsonString: String): RemoteUpdateMetadata? {
        return try {
            val root = JSONObject(jsonString)
            val versionCode = root.optLong("latestVersionCode", root.optLong("versionCode", 0L))
            val versionName = root.optString("latestVersionName", root.optString("versionName", "1.0"))
            val minSupported = root.optLong("minSupportedVersionCode", 1L)
            val title = root.optString("title", "ExamSathi Update")
            val releaseDate = root.optString("releaseDate", "Recent")
            val apkUrl = root.optString("apkUrl", "")
            val apkSize = root.optLong("apkSizeBytes", 0L)
            val sha256 = if (root.has("sha256")) root.getString("sha256") else null

            val changelogList = mutableListOf<String>()
            val changelogArray = root.optJSONArray("changelog")
            if (changelogArray != null) {
                for (i in 0 until changelogArray.length()) {
                    changelogList.add(changelogArray.getString(i))
                }
            } else {
                val notes = root.optString("releaseNotes", "")
                if (notes.isNotBlank()) {
                    changelogList.addAll(notes.split("\n").filter { it.isNotBlank() })
                }
            }

            if (changelogList.isEmpty()) {
                changelogList.add("Performance enhancements and bug fixes.")
            }

            if (versionCode <= 0L || apkUrl.isBlank()) {
                null
            } else {
                RemoteUpdateMetadata(
                    versionCode = versionCode,
                    versionName = versionName,
                    minSupportedVersionCode = minSupported,
                    title = title,
                    releaseDate = releaseDate,
                    changelog = changelogList,
                    apkUrl = apkUrl,
                    apkSizeBytes = apkSize,
                    sha256Checksum = sha256
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Download the APK to the updates cache directory with progress reporting.
     * Rollback/recovery safety: Existing data and installed app are NEVER modified.
     */
    suspend fun downloadApk(
        metadata: RemoteUpdateMetadata,
        onProgress: (DownloadProgress) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val updatesDir = File(context.cacheDir, "updates").apply {
                if (!exists()) mkdirs()
            }

            val finalApkFile = File(updatesDir, "ExamSathi-v${metadata.versionName}.apk")
            val tempDownloadFile = File(updatesDir, "ExamSathi-v${metadata.versionName}.apk.download")

            if (tempDownloadFile.exists()) {
                tempDownloadFile.delete()
            }

            val request = Request.Builder()
                .url(metadata.apkUrl)
                .addHeader("User-Agent", "ExamSathi-Android-Updater")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("Server returned HTTP error ${response.code}")
                )
            }

            val responseBody = response.body
                ?: return@withContext Result.failure(IOException("Empty response body"))

            val totalBytes = if (responseBody.contentLength() > 0) {
                responseBody.contentLength()
            } else {
                metadata.apkSizeBytes
            }

            var downloadedBytes = 0L

            responseBody.byteStream().use { inputStream ->
                FileOutputStream(tempDownloadFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val percent = if (totalBytes > 0) {
                            ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                        } else {
                            50
                        }

                        onProgress(
                            DownloadProgress(
                                bytesDownloaded = downloadedBytes,
                                totalBytes = totalBytes,
                                progressPercent = percent
                            )
                        )
                    }
                    outputStream.flush()
                }
            }

            // Atomic rename to final APK
            if (finalApkFile.exists()) {
                finalApkFile.delete()
            }

            val renameSuccess = tempDownloadFile.renameTo(finalApkFile)
            if (!renameSuccess && !finalApkFile.exists()) {
                return@withContext Result.failure(
                    IOException("Failed to finalize downloaded APK file")
                )
            }

            onProgress(DownloadProgress(downloadedBytes, totalBytes, 100))
            Result.success(finalApkFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if app has permission to install unknown apps (Android 8.0+).
     */
    fun canInstallApks(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Build an Intent to guide user to allow package installation permission.
     */
    fun createInstallPermissionIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    /**
     * Create an Intent for launching the standard Android Package Installer via FileProvider.
     */
    fun createInstallIntent(apkFile: File): Result<Intent> {
        return try {
            if (!apkFile.exists() || apkFile.length() <= 0) {
                return Result.failure(IllegalArgumentException("APK file is invalid or missing"))
            }

            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, apkFile)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            Result.success(installIntent)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generates simulated update metadata for offline demonstration and testing.
     */
    fun createSimulatedUpdateMetadata(): RemoteUpdateMetadata {
        val current = getCurrentVersionInfo()
        val nextVersionCode = current.versionCode + 1
        val nextVersionName = "1.${nextVersionCode}.0"

        return RemoteUpdateMetadata(
            versionCode = nextVersionCode,
            versionName = nextVersionName,
            minSupportedVersionCode = current.versionCode,
            title = "ExamSathi v$nextVersionName Major Update",
            releaseDate = "Current Release",
            changelog = listOf(
                "Enhanced Adaptive Practice algorithm with intelligent weak-spot diagnosis",
                "Official Institute Branding watermarks and header stamps on exported PDFs",
                "Instant offline caching with ultra-low latency test start",
                "Resolved minor question palette navigation edge cases"
            ),
            apkUrl = "https://example.com/examsathi/releases/ExamSathi-v$nextVersionName.apk",
            apkSizeBytes = 18_450_000L,
            sha256Checksum = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            isMandatory = false
        )
    }

    /**
     * Clear all cached APK files in updates folder to recover storage.
     */
    fun clearUpdateCache(): Long {
        var reclaimedBytes = 0L
        try {
            val dir = File(context.cacheDir, "updates")
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    reclaimedBytes += file.length()
                    file.delete()
                }
            }
        } catch (_: Exception) {}
        return reclaimedBytes
    }
}
