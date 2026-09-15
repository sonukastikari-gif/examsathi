package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.data.branding.BrandingConfig
import com.example.data.branding.BrandingManager
import com.example.data.pdf.ExamPdfGenerator
import com.example.data.pdf.PdfShareManager
import com.example.domain.model.StudentResponseSnapshot
import com.example.domain.model.SubjectScore
import com.example.domain.model.TestResult
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
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], shadows = [ShadowPdfDocument::class])
class BrandingAndPdfTest {

    private lateinit var context: Context
    private lateinit var brandingManager: BrandingManager
    private lateinit var pdfGenerator: ExamPdfGenerator

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        brandingManager = BrandingManager.getInstance(context)
        brandingManager.resetToDefaults()
        ExamPdfGenerator.resetForTesting()
        pdfGenerator = ExamPdfGenerator.getInstance(context)
    }

    @Test
    fun testDefaultBrandingConfiguration() {
        val config = brandingManager.brandingConfig.value
        assertEquals("ExamSathi", config.brandName)
        assertEquals("आपकी परीक्षा, आपका साथी", config.tagline)
        assertFalse(config.hasCustomLogo)
        assertEquals(BrandingConfig.PRESET_EMBLEM, config.presetLogo)

        val defaultBitmap = brandingManager.getLogoBitmap(100)
        assertNotNull(defaultBitmap)
        assertEquals(100, defaultBitmap.width)
        assertEquals(100, defaultBitmap.height)
    }

    @Test
    fun testUpdateBrandDetails() {
        brandingManager.updateBrandDetails(
            brandName = "Apex Academy ExamSathi",
            tagline = "Aim High, Score High",
            subTagline = "Official Test Performance Series",
            organizationName = "Apex Educational Services"
        )

        val updated = brandingManager.brandingConfig.value
        assertEquals("Apex Academy ExamSathi", updated.brandName)
        assertEquals("Aim High, Score High", updated.tagline)
        assertEquals("Official Test Performance Series", updated.subTagline)
        assertEquals("Apex Educational Services", updated.organizationName)
    }

    @Test
    fun testPresetEmblemStyles() {
        brandingManager.setPresetLogo(BrandingConfig.PRESET_TROPHY)
        assertEquals(BrandingConfig.PRESET_TROPHY, brandingManager.brandingConfig.value.presetLogo)

        brandingManager.setPresetLogo(BrandingConfig.PRESET_SHIELD)
        assertEquals(BrandingConfig.PRESET_SHIELD, brandingManager.brandingConfig.value.presetLogo)
    }

    @Test
    fun testCustomLogoSaveAndRemovalCycle() {
        // Create small test bitmap
        val testBitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        testBitmap.eraseColor(Color.BLUE)

        val saved = brandingManager.saveCustomLogo(testBitmap)
        assertTrue(saved)
        assertTrue(brandingManager.brandingConfig.value.hasCustomLogo)

        // Custom logo file exists
        val customLogoPath = brandingManager.brandingConfig.value.customLogoPath
        assertNotNull(customLogoPath)
        val logoFile = File(customLogoPath!!)
        assertTrue(logoFile.exists())
        assertTrue(logoFile.length() > 0)

        // Retrieval gives custom bitmap
        val customLoaded = brandingManager.getLogoBitmap(80)
        assertNotNull(customLoaded)
        assertEquals(64, customLoaded.width)

        // Remove custom logo
        val removed = brandingManager.removeCustomLogo()
        assertTrue(removed)
        assertFalse(brandingManager.brandingConfig.value.hasCustomLogo)
        assertFalse(logoFile.exists())
    }

    @Test
    fun testPdfScorecardGeneration() = runBlocking {
        val sampleResponses = listOf(
            StudentResponseSnapshot(
                questionId = "q1",
                questionNumber = 1,
                questionText = "What is the primary function of ribosomes in a cell?",
                subject = "General Science",
                topic = "Cell Biology",
                options = listOf("Protein synthesis", "Lipid metabolism", "Photosynthesis", "Cellular respiration"),
                selectedOptionIndex = 0,
                correctOptionIndex = 0,
                isCorrect = true,
                marksEarned = 1.0,
                explanation = "Ribosomes are macromolecular machines responsible for protein synthesis in all living cells."
            ),
            StudentResponseSnapshot(
                questionId = "q2",
                questionNumber = 2,
                questionText = "Which Indian state has the longest coastline?",
                subject = "General Studies",
                topic = "Indian Geography",
                options = listOf("Maharashtra", "Gujarat", "Tamil Nadu", "Andhra Pradesh"),
                selectedOptionIndex = 2,
                correctOptionIndex = 1,
                isCorrect = false,
                marksEarned = -0.33,
                explanation = "Gujarat has the longest mainland coastline in India, stretching over 1,600 kilometers."
            ),
            StudentResponseSnapshot(
                questionId = "q3",
                questionNumber = 3,
                questionText = "In which year was the Reserve Bank of India established?",
                subject = "Economics",
                topic = "Banking System",
                options = listOf("1935", "1947", "1950", "1921"),
                selectedOptionIndex = null,
                correctOptionIndex = 0,
                isCorrect = false,
                marksEarned = 0.0,
                explanation = "The Reserve Bank of India was established on April 1, 1935 in accordance with the RBI Act, 1934."
            )
        )

        val testResult = TestResult(
            id = "result_unit_test_101",
            testId = "mock_test_1",
            testTitle = "BPSC Prelims Full Mock Test 01",
            testCategory = "Full Mock",
            timestamp = 1718000000000L,
            durationMinutes = 60,
            timeTakenSeconds = 145,
            totalQuestions = 3,
            correctAnswers = 1,
            wrongAnswers = 1,
            skippedAnswers = 1,
            score = 0.67,
            maxScore = 3.0,
            accuracyPercentage = 50.0,
            responses = sampleResponses,
            subjectBreakdown = mapOf(
                "General Science" to SubjectScore("General Science", 1, 1, 0, 1.0),
                "General Studies" to SubjectScore("General Studies", 1, 0, 1, -0.33)
            )
        )

        val pdfResult = pdfGenerator.generateScorecardPdf(testResult)
        if (pdfResult.isFailure) {
            val ex = pdfResult.exceptionOrNull()
            println("DEBUG_PDF_ERROR: ${ex?.javaClass?.name} - ${ex?.message}")
            ex?.printStackTrace()
        }
        assertTrue("PDF generation must succeed: ${pdfResult.exceptionOrNull()?.message}", pdfResult.isSuccess)

        val pdfFile = pdfResult.getOrThrow()
        assertTrue("PDF file must exist", pdfFile.exists())
        assertTrue("PDF file must be non-empty", pdfFile.length() > 0)
        assertTrue("PDF filename must have .pdf extension", pdfFile.name.endsWith(".pdf"))

        // Verify FileProvider URI resolution
        val uri = PdfShareManager.getContentUri(context, pdfFile)
        assertNotNull(uri)
        assertTrue("URI scheme must be content://", uri.scheme == "content")
    }
}
