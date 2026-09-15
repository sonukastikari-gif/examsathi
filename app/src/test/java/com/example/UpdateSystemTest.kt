package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.update.UpdateManager
import com.example.data.update.UpdatePreferences
import com.example.data.update.model.DownloadProgress
import com.example.data.update.model.RemoteUpdateMetadata
import com.example.data.update.model.UpdateStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UpdateSystemTest {

    private lateinit var context: Context
    private lateinit var preferences: UpdatePreferences
    private lateinit var updateManager: UpdateManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val prefs = context.getSharedPreferences("examsathi_update_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        preferences = UpdatePreferences(context)
        updateManager = UpdateManager(context, preferences)
    }

    @Test
    fun testCurrentVersionInfoRetrieved() {
        val versionInfo = updateManager.getCurrentVersionInfo()
        assertNotNull(versionInfo)
        assertTrue(versionInfo.versionCode >= 1)
        assertTrue(versionInfo.versionName.isNotBlank())
        assertEquals(context.packageName, versionInfo.packageName)
    }

    @Test
    fun testHttpsEnforcement() = runBlocking {
        // Insecure HTTP endpoint should be rejected
        val insecureResult = updateManager.checkForUpdates("http://example.com/update.json")
        assertTrue("Insecure endpoint must fail", insecureResult is UpdateStatus.Failed)
        val failure = insecureResult as UpdateStatus.Failed
        assertTrue(failure.message.contains("HTTPS", ignoreCase = true))
    }

    @Test
    fun testUrlSanitizationAndUpgrade() {
        // Entering http should be safely upgraded to https
        preferences.setEndpointUrl("http://institute.edu/repo/version.json")
        assertEquals("https://institute.edu/repo/version.json", preferences.getEndpointUrl())

        // Plain domain should be prefixed with https://
        preferences.setEndpointUrl("github.com/org/repo/version.json")
        assertEquals("https://github.com/org/repo/version.json", preferences.getEndpointUrl())

        // Reset should return default placeholder
        preferences.resetEndpointUrl()
        assertEquals(UpdatePreferences.DEFAULT_UPDATE_ENDPOINT, preferences.getEndpointUrl())
    }

    @Test
    fun testVersionComparisonAndMandatoryLogic() {
        val currentVersion = updateManager.getCurrentVersionInfo()

        // 1. Remote version is newer and above minSupported -> Update available (optional)
        val newerVersion = RemoteUpdateMetadata(
            versionCode = currentVersion.versionCode + 1,
            versionName = "1.2.0",
            minSupportedVersionCode = currentVersion.versionCode,
            title = "Optional Feature Release",
            releaseDate = "2026-09-15",
            changelog = listOf("New Question Categories", "Minor UI improvements"),
            apkUrl = "https://example.com/ExamSathi-1.2.0.apk",
            apkSizeBytes = 15_000_000L
        )
        val isNewerMandatory = currentVersion.versionCode < newerVersion.minSupportedVersionCode
        assertFalse(isNewerMandatory)

        // 2. Remote version requires a higher minimum version -> Mandatory update
        val obsoleteVersion = newerVersion.copy(
            minSupportedVersionCode = currentVersion.versionCode + 1
        )
        val isObsoleteMandatory = currentVersion.versionCode < obsoleteVersion.minSupportedVersionCode
        assertTrue(isObsoleteMandatory)

        // 3. Remote version is same or older -> No update
        val sameVersion = newerVersion.copy(versionCode = currentVersion.versionCode)
        val isUpdateAvailable = sameVersion.versionCode > currentVersion.versionCode
        assertFalse(isUpdateAvailable)
    }

    @Test
    fun testSimulatedUpdateMetadataIntegrity() {
        val simulated = updateManager.createSimulatedUpdateMetadata()
        assertNotNull(simulated)
        assertTrue(simulated.versionCode > updateManager.getCurrentVersionInfo().versionCode)
        assertTrue(simulated.title.contains("ExamSathi"))
        assertTrue(simulated.changelog.isNotEmpty())
        assertTrue(simulated.apkUrl.startsWith("https://"))
        assertTrue(simulated.apkSizeBytes > 0)
        assertNotNull(simulated.sha256Checksum)
    }

    @Test
    fun testProgressAndSizeFormatting() {
        val metadata = RemoteUpdateMetadata(
            versionCode = 2L,
            versionName = "1.1.0",
            minSupportedVersionCode = 1L,
            title = "Test",
            releaseDate = "Today",
            changelog = listOf("Test"),
            apkUrl = "https://example.com/test.apk",
            apkSizeBytes = 15_728_640L // exactly 15.0 MB
        )
        assertEquals("15.0 MB", metadata.formatSizeMb())

        val progress = DownloadProgress(
            bytesDownloaded = 7_864_320L,
            totalBytes = 15_728_640L,
            progressPercent = 50
        )
        assertEquals(50, progress.percent)
        val formattedProgress = progress.formatProgressText()
        assertTrue(formattedProgress.contains("50%"))
        assertTrue(formattedProgress.contains("7.5 / 15.0 MB"))
    }

    @Test
    fun testRollbackSafetyAndCacheManagement() {
        val updatesDir = File(context.cacheDir, "updates")
        updatesDir.mkdirs()

        val sampleApk = File(updatesDir, "ExamSathi-test.apk")
        sampleApk.writeBytes(ByteArray(2048) { 1 })
        assertTrue(sampleApk.exists())

        // Ensure clearUpdateCache cleans downloaded packages safely without touching databases
        val freed = updateManager.clearUpdateCache()
        assertTrue(freed > 0L)
        assertFalse(sampleApk.exists())
    }
}
