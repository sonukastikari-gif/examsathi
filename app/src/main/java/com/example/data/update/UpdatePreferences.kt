package com.example.data.update

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages local preferences for the update system, including configurable HTTPS endpoint URL.
 */
class UpdatePreferences(context: Context) {

    companion object {
        private const val PREFS_NAME = "examsathi_update_prefs"
        private const val KEY_ENDPOINT_URL = "update_endpoint_url"
        private const val KEY_AUTO_CHECK = "auto_check_on_startup"
        private const val KEY_LAST_CHECKED = "last_checked_timestamp"
        private const val KEY_IGNORED_VERSION = "ignored_version_code"

        // Clearly marked placeholder URL for configurable remote server
        const val DEFAULT_UPDATE_ENDPOINT = "https://example.com/examsathi/releases/version.json"

        @Volatile
        private var INSTANCE: UpdatePreferences? = null

        fun getInstance(context: Context): UpdatePreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UpdatePreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _endpointUrl = MutableStateFlow(getEndpointUrl())
    val endpointUrl: StateFlow<String> = _endpointUrl.asStateFlow()

    fun getEndpointUrl(): String {
        return prefs.getString(KEY_ENDPOINT_URL, DEFAULT_UPDATE_ENDPOINT) ?: DEFAULT_UPDATE_ENDPOINT
    }

    fun setEndpointUrl(url: String): Boolean {
        val trimmed = url.trim()
        // Strict HTTPS validation
        val validUrl = if (trimmed.startsWith("https://", ignoreCase = true)) {
            trimmed
        } else if (trimmed.startsWith("http://", ignoreCase = true)) {
            // Upgrade insecure HTTP to HTTPS
            "https://" + trimmed.substring(7)
        } else {
            "https://$trimmed"
        }

        prefs.edit().putString(KEY_ENDPOINT_URL, validUrl).apply()
        _endpointUrl.value = validUrl
        return true
    }

    fun resetEndpointUrl() {
        setEndpointUrl(DEFAULT_UPDATE_ENDPOINT)
    }

    fun isAutoCheckEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_CHECK, true)
    }

    fun setAutoCheckEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CHECK, enabled).apply()
    }

    fun getLastCheckedTime(): Long {
        return prefs.getLong(KEY_LAST_CHECKED, 0L)
    }

    fun setLastCheckedTime(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_CHECKED, timestamp).apply()
    }

    fun getIgnoredVersionCode(): Long {
        return prefs.getLong(KEY_IGNORED_VERSION, 0L)
    }

    fun setIgnoredVersionCode(versionCode: Long) {
        prefs.edit().putLong(KEY_IGNORED_VERSION, versionCode).apply()
    }
}
