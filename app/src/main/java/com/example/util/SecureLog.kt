package com.example.util

import android.util.Log
import com.example.BuildConfig

/**
 * Security-hardened logger utility.
 * - Strips sensitive credentials, API keys, tokens, and PIN patterns.
 * - Suppresses debug, info, and verbose logs in release builds (when BuildConfig.DEBUG is false).
 * - Minimizes error details logged to release Logcat.
 */
object SecureLog {

    private val SENSITIVE_PATTERNS = listOf(
        Regex("AIza[0-9A-Za-z-_]{35}"), // Google API key
        Regex("key=[^&\\s]+", RegexOption.IGNORE_CASE),
        Regex("pin=\\d+", RegexOption.IGNORE_CASE),
        Regex("password=[^&\\s]+", RegexOption.IGNORE_CASE),
        Regex("bearer\\s+[a-zA-Z0-9._~+/-]+", RegexOption.IGNORE_CASE),
        Regex("salt=[a-fA-F0-9]+", RegexOption.IGNORE_CASE),
        Regex("hash=[a-fA-F0-9]+", RegexOption.IGNORE_CASE)
    )

    fun sanitize(message: String): String {
        var result = message
        for (pattern in SENSITIVE_PATTERNS) {
            result = result.replace(pattern, "[REDACTED]")
        }
        return result
    }

    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, sanitize(message))
        }
    }

    fun i(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, sanitize(message))
        }
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            if (throwable != null) {
                Log.w(tag, sanitize(message), throwable)
            } else {
                Log.w(tag, sanitize(message))
            }
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val sanitized = sanitize(message)
        if (BuildConfig.DEBUG) {
            if (throwable != null) {
                Log.e(tag, sanitized, throwable)
            } else {
                Log.e(tag, sanitized)
            }
        } else {
            // In release builds, log high-level sanitized status only without stack trace
            Log.e(tag, sanitized)
        }
    }
}
