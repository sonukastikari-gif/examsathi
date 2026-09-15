package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import com.example.util.SecureLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom

sealed class AdminAuthResult {
    data object Success : AdminAuthResult()
    data class InvalidPin(val remainingAttempts: Int) : AdminAuthResult()
    data class LockedOut(val remainingSeconds: Long) : AdminAuthResult()
}

/**
 * Security-hardened Admin Credential & Session Manager.
 *
 * Highlights:
 * - Salted cryptographic hashing (SHA-256 with 5,000 rounds and 16-byte SecureRandom salt).
 * - Plaintext credentials are NEVER persisted to disk or logs.
 * - Configurable credential handling: Admins can update/change their PIN.
 * - Brute-force protection: Rate-limited with automatic 30-second lockout after 5 consecutive failures.
 * - Session state enforcement: Inaccessible from Student View unless actively authenticated.
 * - Timing-attack resistant constant-time hash comparisons.
 */
class AdminSecurityManager internal constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _isSessionAuthenticated = MutableStateFlow(false)
    val isSessionAuthenticated: StateFlow<Boolean> = _isSessionAuthenticated.asStateFlow()

    private var failedAttempts: Int = 0
    private var lockoutUntilTimestamp: Long = 0L

    init {
        ensureInitialized()
    }

    companion object {
        private const val TAG = "AdminSecurityManager"
        private const val PREFS_NAME = "admin_security_prefs"
        private const val KEY_SALT_HEX = "admin_pin_salt_hex"
        private const val KEY_HASH_HEX = "admin_pin_hash_hex"
        private const val KEY_IS_DEFAULT_PIN = "admin_is_default_pin"

        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MS = 30_000L // 30 seconds
        const val INITIAL_DEFAULT_PIN = "1234"

        @Volatile
        private var INSTANCE: AdminSecurityManager? = null

        fun getInstance(context: Context): AdminSecurityManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AdminSecurityManager(context).also { INSTANCE = it }
            }
        }

        /**
         * Computes salted multi-round SHA-256 hash.
         */
        internal fun computeSaltedHash(pin: String, saltHex: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            var combined = (saltHex + pin).toByteArray(Charsets.UTF_8)
            for (i in 0 until 5000) {
                md.update(combined)
                combined = md.digest()
            }
            return combined.joinToString("") { "%02x".format(it) }
        }

        /**
         * Constant-time comparison to protect against timing analysis attacks.
         */
        internal fun constantTimeEquals(a: String, b: String): Boolean {
            if (a.length != b.length) return false
            var diff = 0
            for (i in a.indices) {
                diff = diff or (a[i].code xor b[i].code)
            }
            return diff == 0
        }

        private fun generateRandomSaltHex(): String {
            val random = SecureRandom()
            val saltBytes = ByteArray(16)
            random.nextBytes(saltBytes)
            return saltBytes.joinToString("") { "%02x".format(it) }
        }
    }

    /**
     * Ensures an initial salted hash exists in secure SharedPreferences on first launch.
     */
    private fun ensureInitialized() {
        val storedHash = prefs.getString(KEY_HASH_HEX, null)
        val storedSalt = prefs.getString(KEY_SALT_HEX, null)

        if (storedHash == null || storedSalt == null) {
            val newSalt = generateRandomSaltHex()
            val initialHash = computeSaltedHash(INITIAL_DEFAULT_PIN, newSalt)
            prefs.edit()
                .putString(KEY_SALT_HEX, newSalt)
                .putString(KEY_HASH_HEX, initialHash)
                .putBoolean(KEY_IS_DEFAULT_PIN, true)
                .apply()
            SecureLog.i(TAG, "Initialized default administrator salted credential store.")
        }
    }

    /**
     * Checks whether the administrator is still using the initial default PIN.
     */
    fun isDefaultPinInUse(): Boolean {
        return prefs.getBoolean(KEY_IS_DEFAULT_PIN, false)
    }

    /**
     * Checks whether the authentication is currently locked out due to excessive failures.
     */
    fun isLockedOut(): Boolean {
        val now = System.currentTimeMillis()
        return now < lockoutUntilTimestamp
    }

    /**
     * Returns remaining lockout seconds if locked out, 0 otherwise.
     */
    fun getRemainingLockoutSeconds(): Long {
        val diff = lockoutUntilTimestamp - System.currentTimeMillis()
        return if (diff > 0) (diff + 999) / 1000 else 0L
    }

    /**
     * Authenticates an entered PIN against the salted hash.
     * Enforces rate-limiting and brute-force protection.
     */
    fun authenticate(enteredPin: String): AdminAuthResult {
        if (isLockedOut()) {
            val remainingSec = getRemainingLockoutSeconds()
            SecureLog.w(TAG, "Admin authentication attempted while locked out.")
            return AdminAuthResult.LockedOut(remainingSec)
        }

        val storedSalt = prefs.getString(KEY_SALT_HEX, null) ?: ""
        val storedHash = prefs.getString(KEY_HASH_HEX, null) ?: ""

        val computedHash = computeSaltedHash(enteredPin.trim(), storedSalt)
        val isValid = constantTimeEquals(computedHash, storedHash)

        return if (isValid) {
            failedAttempts = 0
            lockoutUntilTimestamp = 0L
            _isSessionAuthenticated.value = true
            SecureLog.i(TAG, "Admin authenticated successfully.")
            AdminAuthResult.Success
        } else {
            failedAttempts++
            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                lockoutUntilTimestamp = System.currentTimeMillis() + LOCKOUT_DURATION_MS
                val remainingSec = LOCKOUT_DURATION_MS / 1000
                SecureLog.w(TAG, "Excessive failed admin attempts. Lockout triggered.")
                AdminAuthResult.LockedOut(remainingSec)
            } else {
                val remaining = MAX_FAILED_ATTEMPTS - failedAttempts
                SecureLog.w(TAG, "Failed admin login attempt.")
                AdminAuthResult.InvalidPin(remaining)
            }
        }
    }

    /**
     * Changes the administrator PIN after verifying the current PIN.
     * Validates that new PIN meets security criteria (4 to 8 digits, not trivial).
     */
    fun changePin(currentPin: String, newPin: String): Result<Unit> {
        val storedSalt = prefs.getString(KEY_SALT_HEX, null) ?: ""
        val storedHash = prefs.getString(KEY_HASH_HEX, null) ?: ""

        val computedCurrentHash = computeSaltedHash(currentPin.trim(), storedSalt)
        if (!constantTimeEquals(computedCurrentHash, storedHash)) {
            return Result.failure(IllegalArgumentException("Current PIN is incorrect."))
        }

        val trimmedNew = newPin.trim()
        if (!trimmedNew.matches(Regex("^\\d{4,8}$"))) {
            return Result.failure(IllegalArgumentException("New PIN must be between 4 and 8 digits."))
        }

        if (trimmedNew == currentPin.trim()) {
            return Result.failure(IllegalArgumentException("New PIN must be different from current PIN."))
        }

        if (trimmedNew in listOf("0000", "1111", "2222", "1234", "4321")) {
            return Result.failure(IllegalArgumentException("New PIN is too simple. Please choose a stronger PIN."))
        }

        val freshSalt = generateRandomSaltHex()
        val freshHash = computeSaltedHash(trimmedNew, freshSalt)

        prefs.edit()
            .putString(KEY_SALT_HEX, freshSalt)
            .putString(KEY_HASH_HEX, freshHash)
            .putBoolean(KEY_IS_DEFAULT_PIN, false)
            .apply()

        failedAttempts = 0
        lockoutUntilTimestamp = 0L
        _isSessionAuthenticated.value = true
        SecureLog.i(TAG, "Admin PIN successfully updated.")

        return Result.success(Unit)
    }

    /**
     * Terminates the active administrator session and returns to locked state.
     */
    fun logout() {
        _isSessionAuthenticated.value = false
        SecureLog.i(TAG, "Admin logged out. Session locked.")
    }

    /**
     * Resets credential to default for unit test or emergency recovery scenarios.
     */
    fun resetToDefault() {
        val newSalt = generateRandomSaltHex()
        val initialHash = computeSaltedHash(INITIAL_DEFAULT_PIN, newSalt)
        prefs.edit()
            .putString(KEY_SALT_HEX, newSalt)
            .putString(KEY_HASH_HEX, initialHash)
            .putBoolean(KEY_IS_DEFAULT_PIN, true)
            .apply()
        failedAttempts = 0
        lockoutUntilTimestamp = 0L
        _isSessionAuthenticated.value = false
    }
}
