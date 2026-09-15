package com.example.ui.admin

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.branding.BrandingConfig
import com.example.data.branding.BrandingManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AdminBrandingUiState(
    val brandNameInput: String = "",
    val taglineInput: String = "",
    val subTaglineInput: String = "",
    val organizationInput: String = "",
    val showRemoveConfirmationDialog: Boolean = false,
    val toastMessage: String? = null
)

class AdminBrandingViewModel(
    private val brandingManager: BrandingManager
) : ViewModel() {

    val brandingConfig: StateFlow<BrandingConfig> = brandingManager.brandingConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), brandingManager.brandingConfig.value)

    private val _uiState = MutableStateFlow(
        AdminBrandingUiState(
            brandNameInput = brandingManager.brandingConfig.value.brandName,
            taglineInput = brandingManager.brandingConfig.value.tagline,
            subTaglineInput = brandingManager.brandingConfig.value.subTagline,
            organizationInput = brandingManager.brandingConfig.value.organizationName
        )
    )
    val uiState: StateFlow<AdminBrandingUiState> = _uiState.asStateFlow()

    fun onBrandNameChanged(value: String) {
        _uiState.value = _uiState.value.copy(brandNameInput = value)
    }

    fun onTaglineChanged(value: String) {
        _uiState.value = _uiState.value.copy(taglineInput = value)
    }

    fun onSubTaglineChanged(value: String) {
        _uiState.value = _uiState.value.copy(subTaglineInput = value)
    }

    fun onOrganizationChanged(value: String) {
        _uiState.value = _uiState.value.copy(organizationInput = value)
    }

    fun saveBrandDetails() {
        val state = _uiState.value
        brandingManager.updateBrandDetails(
            brandName = state.brandNameInput,
            tagline = state.taglineInput,
            subTagline = state.subTaglineInput,
            organizationName = state.organizationInput
        )
        _uiState.value = _uiState.value.copy(toastMessage = "Branding details updated successfully.")
    }

    fun selectPresetLogo(preset: String) {
        brandingManager.setPresetLogo(preset)
        _uiState.value = _uiState.value.copy(toastMessage = "Preset emblem updated.")
    }

    fun replaceLogoWithUri(uri: Uri) {
        viewModelScope.launch {
            val success = brandingManager.saveCustomLogoFromUri(uri)
            if (success) {
                _uiState.value = _uiState.value.copy(toastMessage = "Custom logo uploaded and applied.")
            } else {
                _uiState.value = _uiState.value.copy(toastMessage = "Failed to load custom logo image.")
            }
        }
    }

    fun replaceLogoWithBitmap(bitmap: Bitmap) {
        val success = brandingManager.saveCustomLogo(bitmap)
        if (success) {
            _uiState.value = _uiState.value.copy(toastMessage = "Custom logo replaced successfully.")
        } else {
            _uiState.value = _uiState.value.copy(toastMessage = "Failed to save custom logo.")
        }
    }

    fun showRemoveConfirmation() {
        _uiState.value = _uiState.value.copy(showRemoveConfirmationDialog = true)
    }

    fun dismissRemoveConfirmation() {
        _uiState.value = _uiState.value.copy(showRemoveConfirmationDialog = false)
    }

    fun confirmRemoveCustomLogo() {
        _uiState.value = _uiState.value.copy(showRemoveConfirmationDialog = false)
        val success = brandingManager.removeCustomLogo()
        if (success) {
            _uiState.value = _uiState.value.copy(toastMessage = "Custom logo removed. Reverted to default ExamSathi emblem.")
        } else {
            _uiState.value = _uiState.value.copy(toastMessage = "Failed to remove custom logo.")
        }
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val brandingManager = BrandingManager.getInstance(context)
            return AdminBrandingViewModel(brandingManager) as T
        }
    }
}
