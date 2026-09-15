package com.example.ui.update

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.update.UpdateManager
import com.example.data.update.UpdatePreferences
import com.example.data.update.model.AppVersionInfo
import com.example.data.update.model.DownloadProgress
import com.example.data.update.model.RemoteUpdateMetadata
import com.example.data.update.model.UpdateStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * State object for the Update System UI.
 */
data class UpdateUiState(
    val currentVersion: AppVersionInfo,
    val status: UpdateStatus = UpdateStatus.Idle,
    val endpointUrl: String = "",
    val isAutoCheckEnabled: Boolean = true,
    val showDialog: Boolean = false,
    val isSimulatedDemo: Boolean = false,
    val toastMessage: String? = null
)

class UpdateViewModel(
    application: Application,
    private val updateManager: UpdateManager = UpdateManager.getInstance(application),
    private val preferences: UpdatePreferences = UpdatePreferences.getInstance(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        UpdateUiState(
            currentVersion = updateManager.getCurrentVersionInfo(),
            endpointUrl = preferences.getEndpointUrl(),
            isAutoCheckEnabled = preferences.isAutoCheckEnabled()
        )
    )
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    private var downloadJob: Job? = null

    init {
        // Observe endpoint URL changes
        viewModelScope.launch {
            preferences.endpointUrl.collect { newUrl ->
                _uiState.value = _uiState.value.copy(endpointUrl = newUrl)
            }
        }
    }

    /**
     * Trigger manual or automated update check.
     */
    fun checkForUpdates(showDialogOnResult: Boolean = true) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                status = UpdateStatus.Checking,
                showDialog = showDialogOnResult,
                isSimulatedDemo = false
            )

            val result = updateManager.checkForUpdates()
            _uiState.value = _uiState.value.copy(
                status = result,
                showDialog = showDialogOnResult || (result is UpdateStatus.Available)
            )
        }
    }

    /**
     * Start downloading the APK from remote or simulated repository.
     */
    fun startDownload(metadata: RemoteUpdateMetadata) {
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                status = UpdateStatus.Downloading(metadata, DownloadProgress())
            )

            if (_uiState.value.isSimulatedDemo) {
                // Smooth simulation for offline demonstration
                for (p in 1..100) {
                    delay(25L)
                    val simulatedDownloaded = (metadata.apkSizeBytes * p) / 100
                    _uiState.value = _uiState.value.copy(
                        status = UpdateStatus.Downloading(
                            metadata = metadata,
                            progress = DownloadProgress(
                                bytesDownloaded = simulatedDownloaded,
                                totalBytes = metadata.apkSizeBytes,
                                progressPercent = p
                            )
                        )
                    )
                }

                // Create placeholder APK file for demonstration
                val cacheDir = getApplication<Application>().cacheDir
                val demoFile = File(cacheDir, "updates/ExamSathi-demo.apk").apply {
                    parentFile?.mkdirs()
                    if (!exists()) {
                        writeBytes(ByteArray(1024) { 0 })
                    }
                }

                _uiState.value = _uiState.value.copy(
                    status = UpdateStatus.ReadyToInstall(metadata, demoFile)
                )
            } else {
                val downloadResult = updateManager.downloadApk(metadata) { progress ->
                    _uiState.value = _uiState.value.copy(
                        status = UpdateStatus.Downloading(metadata, progress)
                    )
                }

                downloadResult.fold(
                    onSuccess = { apkFile ->
                        _uiState.value = _uiState.value.copy(
                            status = UpdateStatus.ReadyToInstall(metadata, apkFile)
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            status = UpdateStatus.Failed(
                                message = "Download failed: ${error.localizedMessage ?: "Network error"}",
                                isNetworkUnavailable = true,
                                metadata = metadata
                            )
                        )
                    }
                )
            }
        }
    }

    /**
     * Launch Android installation flow.
     */
    fun installApk(context: Context, apkFile: File): Boolean {
        if (!updateManager.canInstallApks()) {
            val permissionIntent = updateManager.createInstallPermissionIntent()
            context.startActivity(permissionIntent)
            return false
        }

        val installResult = updateManager.createInstallIntent(apkFile)
        return installResult.fold(
            onSuccess = { intent ->
                context.startActivity(intent)
                true
            },
            onFailure = { error ->
                _uiState.value = _uiState.value.copy(
                    toastMessage = "Unable to start installer: ${error.localizedMessage}"
                )
                false
            }
        )
    }

    /**
     * Simulate an update release for UI and workflow testing without live remote server.
     */
    fun simulateDemoUpdate() {
        val simulatedMetadata = updateManager.createSimulatedUpdateMetadata()
        _uiState.value = _uiState.value.copy(
            status = UpdateStatus.Available(simulatedMetadata, isMandatory = false),
            showDialog = true,
            isSimulatedDemo = true
        )
    }

    fun saveEndpointUrl(newUrl: String) {
        val success = preferences.setEndpointUrl(newUrl)
        if (success) {
            _uiState.value = _uiState.value.copy(
                endpointUrl = preferences.getEndpointUrl(),
                toastMessage = "Update URL updated successfully"
            )
        }
    }

    fun resetEndpointUrl() {
        preferences.resetEndpointUrl()
        _uiState.value = _uiState.value.copy(
            endpointUrl = preferences.getEndpointUrl(),
            toastMessage = "Update URL restored to default"
        )
    }

    fun toggleAutoCheck(enabled: Boolean) {
        preferences.setAutoCheckEnabled(enabled)
        _uiState.value = _uiState.value.copy(isAutoCheckEnabled = enabled)
    }

    fun showUpdateDialog() {
        _uiState.value = _uiState.value.copy(showDialog = true)
        if (_uiState.value.status is UpdateStatus.Idle) {
            checkForUpdates(showDialogOnResult = true)
        }
    }

    fun dismissDialog() {
        // If mandatory update, do not dismiss
        val currentStatus = _uiState.value.status
        if (currentStatus is UpdateStatus.Available && currentStatus.isMandatory) {
            return
        }
        _uiState.value = _uiState.value.copy(showDialog = false)
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }

    fun clearCache(): Long {
        val freed = updateManager.clearUpdateCache()
        _uiState.value = _uiState.value.copy(
            toastMessage = "Cleared ${(freed / (1024.0 * 1024.0)).toInt()} MB from update cache"
        )
        return freed
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val app = context.applicationContext as Application
            return UpdateViewModel(app) as T
        }
    }
}
