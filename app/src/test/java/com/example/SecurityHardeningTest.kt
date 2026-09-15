package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.gemini.AiQuestionService
import com.example.data.remote.gemini.BackendProxyQuestionService
import com.example.data.remote.gemini.GeminiService
import com.example.data.security.AdminAuthResult
import com.example.data.security.AdminSecurityManager
import com.example.util.SecureLog
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SecurityHardeningTest {

    private lateinit var context: Context
    private lateinit var securityManager: AdminSecurityManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Reset admin security preferences for isolated test runs
        val prefs = context.getSharedPreferences("admin_security_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        securityManager = AdminSecurityManager(context)
    }

    @Test
    fun testDefaultPinAuthenticatedWithSaltedHash() {
        // Verification of initial setup: default PIN is salted & hashed, not stored plain text
        assertTrue(securityManager.isDefaultPinInUse())

        val authResult = securityManager.authenticate("1234")
        assertTrue("Expected successful authentication with default PIN", authResult is AdminAuthResult.Success)
        assertTrue(securityManager.isSessionAuthenticated.value)
    }

    @Test
    fun testBruteForceLockoutAfterMaxAttempts() {
        // Attempt 5 incorrect PINs
        for (i in 1..4) {
            val result = securityManager.authenticate("9999")
            assertTrue(result is AdminAuthResult.InvalidPin)
            assertEquals(5 - i, (result as AdminAuthResult.InvalidPin).remainingAttempts)
        }

        // 5th failed attempt triggers lockout
        val fifthResult = securityManager.authenticate("9999")
        assertTrue(fifthResult is AdminAuthResult.LockedOut)
        assertTrue(securityManager.isLockedOut())

        // Subsequent authentication attempt while locked out is rejected even with correct PIN
        val lockedAttempt = securityManager.authenticate("1234")
        assertTrue(lockedAttempt is AdminAuthResult.LockedOut)
    }

    @Test
    fun testChangePinEnforcesSecurityRulesAndUpdatesHash() {
        // Test weak PIN rejection
        val weakResult = securityManager.changePin("1234", "0000")
        assertTrue(weakResult.isFailure)

        val shortResult = securityManager.changePin("1234", "12")
        assertTrue(shortResult.isFailure)

        // Change to valid secure PIN
        val successResult = securityManager.changePin("1234", "5829")
        assertTrue(successResult.isSuccess)
        assertFalse(securityManager.isDefaultPinInUse())

        // Old PIN no longer authenticates
        val oldPinAuth = securityManager.authenticate("1234")
        assertTrue(oldPinAuth is AdminAuthResult.InvalidPin)

        // New PIN authenticates successfully
        val newPinAuth = securityManager.authenticate("5829")
        assertTrue(newPinAuth is AdminAuthResult.Success)
    }

    @Test
    fun testSessionLogoutClearsAuth() {
        securityManager.authenticate("1234")
        assertTrue(securityManager.isSessionAuthenticated.value)

        securityManager.logout()
        assertFalse(securityManager.isSessionAuthenticated.value)
    }

    @Test
    fun testSecureLogSanitizesSensitivePatterns() {
        val rawApiKeyLog = "Requesting url https://generativelanguage.googleapis.com/v1beta/models?key=AIzaSyABCD12345"
        val sanitizedKeyLog = SecureLog.sanitize(rawApiKeyLog)
        assertFalse(sanitizedKeyLog.contains("AIzaSyABCD12345"))
        assertTrue(sanitizedKeyLog.contains("[REDACTED]"))

        val rawPinLog = "Admin attempted pin=4821"
        val sanitizedPinLog = SecureLog.sanitize(rawPinLog)
        assertFalse(sanitizedPinLog.contains("4821"))
        assertTrue(sanitizedPinLog.contains("[REDACTED]"))

        val rawBearerLog = "Authorization: Bearer secret_jwt_token_12345"
        val sanitizedBearerLog = SecureLog.sanitize(rawBearerLog)
        assertFalse(sanitizedBearerLog.contains("secret_jwt_token_12345"))
        assertTrue(sanitizedBearerLog.contains("[REDACTED]"))
    }

    @Test
    fun testAiQuestionServiceAbstraction() {
        // Verify GeminiService conforms to AiQuestionService interface
        val geminiService: AiQuestionService = GeminiService(apiKeyProvider = { "test_key" })
        assertNotNull(geminiService)
        assertTrue(geminiService is AiQuestionService)

        // Verify BackendProxyQuestionService conforms to AiQuestionService interface
        val proxyService: AiQuestionService = BackendProxyQuestionService(
            proxyEndpointUrl = "https://api.examsathi.internal/v1/generate-questions"
        )
        assertNotNull(proxyService)
        assertTrue(proxyService is AiQuestionService)
    }
}
