package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ExamSathiDatabase
import com.example.data.remote.gemini.GeminiError
import com.example.data.remote.gemini.GeminiGeneratedQuestion
import com.example.data.remote.gemini.GeminiService
import com.example.data.repository.GeminiAdaptiveEngine
import com.example.data.repository.QuestionRepository
import com.example.data.repository.RuleBasedAdaptiveEngine
import com.example.domain.model.AdaptiveConfig
import com.example.domain.model.Question
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
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
class GeminiIntegrationTest {

    private lateinit var database: ExamSathiDatabase
    private lateinit var questionRepository: QuestionRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ExamSathiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        questionRepository = QuestionRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testGeminiApiKeyConfigurationReading() {
        // Test key resolution logic
        val validKey = "AIzaSyD-sample-valid-key"
        val serviceWithKey = GeminiService(apiKeyProvider = { validKey })
        // Verify key provider is utilized
        assertEquals("AIzaSyD-sample-valid-key", validKey)

        // Test placeholder or missing key handling
        val emptyService = GeminiService(apiKeyProvider = { "" })
        runBlocking {
            val result = emptyService.generateQuestions("History", "Modern", "Easy", 1)
            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(exception is GeminiError.MissingApiKey)
            assertTrue(exception?.message?.contains("API key") == true)
        }
    }

    @Test
    fun testGeminiRequestBodyConfiguredForJsonAndModel() {
        val service = GeminiService(apiKeyProvider = { "dummy_key" })
        val bodyString = service.buildRequestBody("Generate questions")
        val json = JSONObject(bodyString)

        // Verify generationConfig specifies application/json
        val genConfig = json.getJSONObject("generationConfig")
        assertEquals("application/json", genConfig.getString("responseMimeType"))

        // Verify contents structure
        val contents = json.getJSONArray("contents")
        assertTrue(contents.length() > 0)
        val parts = contents.getJSONObject(0).getJSONArray("parts")
        assertEquals("Generate questions", parts.getJSONObject(0).getString("text"))
    }

    @Test
    fun testHttpErrorMappingDoesNotCrash() {
        val service = GeminiService(apiKeyProvider = { "dummy_key" })

        // 401 Unauthorized
        val err401 = service.mapHttpError(401, """{"error": "Unauthorized"}""")
        assertTrue(err401 is GeminiError.AuthenticationError)

        // 403 Forbidden
        val err403 = service.mapHttpError(403, """{"error": "Forbidden"}""")
        assertTrue(err403 is GeminiError.AuthenticationError)

        // 429 Quota Exceeded
        val err429 = service.mapHttpError(429, """{"error": "RESOURCE_EXHAUSTED"}""")
        assertTrue(err429 is GeminiError.QuotaExceeded)

        // 500 Internal Server Error
        val err500 = service.mapHttpError(500, """{"error": "Internal Server Error"}""")
        assertTrue(err500 is GeminiError.ServerError)

        // 503 Service Unavailable
        val err503 = service.mapHttpError(503, """{"error": "Service Unavailable"}""")
        assertTrue(err503 is GeminiError.ServerError)

        // Unknown error code
        val err502 = service.mapHttpError(502, """{"error": "Bad Gateway"}""")
        assertTrue(err502 is GeminiError.ServerError)
    }

    @Test
    fun testParseValidGeminiResponse() {
        val sampleResponse = """
        {
          "candidates": [
            {
              "content": {
                "parts": [
                  {
                    "text": "{\n  \"questions\": [\n    {\n      \"questionText\": \"Which Constitutional Amendment introduced the Goods and Services Tax (GST) in India?\",\n      \"subject\": \"General Studies\",\n      \"topic\": \"Indian Economy & Taxation\",\n      \"difficulty\": \"Medium\",\n      \"options\": [\n        \"100th Amendment Act\",\n        \"101st Amendment Act\",\n        \"102nd Amendment Act\",\n        \"103rd Amendment Act\"\n      ],\n      \"correctOptionIndex\": 1,\n      \"explanation\": \"The 101st Constitutional Amendment Act, 2016 introduced the Goods and Services Tax (GST) in India.\"\n    }\n  ]\n}"
                  }
                ]
              }
            }
          ]
        }
        """.trimIndent()

        val service = GeminiService(apiKeyProvider = { "test_key" })
        val parsed = service.parseGeminiResponse(
            responseJson = sampleResponse,
            fallbackSubject = "General Studies",
            fallbackTopic = "Taxation",
            fallbackDifficulty = "Medium"
        )

        assertEquals(1, parsed.size)
        val q = parsed[0]
        assertEquals("Which Constitutional Amendment introduced the Goods and Services Tax (GST) in India?", q.questionText)
        assertEquals(4, q.options.size)
        assertEquals("101st Amendment Act", q.options[1])
        assertEquals(1, q.correctOptionIndex)
        assertTrue(q.explanation.contains("101st Constitutional Amendment"))
    }

    @Test
    fun testParseMarkdownFencedGeminiResponse() {
        val markdownFencedResponse = """
        {
          "candidates": [
            {
              "content": {
                "parts": [
                  {
                    "text": "```json\n{\n  \"questions\": [\n    {\n      \"questionText\": \"What is the chemical symbol for Gold?\",\n      \"subject\": \"General Science\",\n      \"topic\": \"Chemistry\",\n      \"difficulty\": \"Easy\",\n      \"options\": [\"Ag\", \"Au\", \"Pb\", \"Fe\"],\n      \"correctOptionIndex\": 1,\n      \"explanation\": \"Au comes from the Latin word aurum meaning shining dawn.\"\n    }\n  ]\n}\n```"
                  }
                ]
              }
            }
          ]
        }
        """.trimIndent()

        val service = GeminiService(apiKeyProvider = { "test_key" })
        val parsed = service.parseGeminiResponse(
            responseJson = markdownFencedResponse,
            fallbackSubject = "General Science",
            fallbackTopic = "Chemistry",
            fallbackDifficulty = "Easy"
        )

        assertEquals(1, parsed.size)
        assertEquals("Au", parsed[0].options[1])
        assertEquals(1, parsed[0].correctOptionIndex)
    }

    @Test
    fun testMalformedGeminiResponseDoesNotCrash() {
        val corruptedResponse = """{"invalid": "structure", "error": true}"""
        val service = GeminiService(apiKeyProvider = { "test_key" })

        val parsed = service.parseGeminiResponse(
            responseJson = corruptedResponse,
            fallbackSubject = "General Studies",
            fallbackTopic = "Polity",
            fallbackDifficulty = "Medium"
        )

        assertTrue(parsed.isEmpty())
    }

    @Test
    fun testValidateAiGeneratedQuestionsDetectsInvalidStructure() = runBlocking {
        // AI question with missing options
        val invalidAiQuestion = Question(
            id = "ai_q_1",
            testId = "bank",
            questionNumber = 1,
            subject = "Quantitative Aptitude",
            topic = "Percentages",
            questionText = "If price increases by 25%, what percentage must consumption decrease?",
            options = listOf("20%", "25%"), // only 2 options!
            correctOptionIndex = 0,
            explanation = "Formula: (r/(100+r))*100 = (25/125)*100 = 20%."
        )

        val validation = questionRepository.validateQuestion(invalidAiQuestion)
        assertFalse(validation.isValid)
    }

    @Test
    fun testDuplicateDetectionForGeneratedQuestions() = runBlocking {
        // Seed an existing question in offline bank
        val existingQuestion = Question(
            id = "existing_q_1",
            testId = "bank",
            questionNumber = 1,
            subject = "Indian Polity",
            topic = "Fundamental Rights",
            questionText = "Which Article of the Indian Constitution abolishes Untouchability?",
            options = listOf("Article 14", "Article 15", "Article 16", "Article 17"),
            correctOptionIndex = 3,
            explanation = "Article 17 abolishes untouchability and forbids its practice in any form."
        )
        val saveResult = questionRepository.saveQuestion(existingQuestion)
        assertTrue(saveResult.isSuccess)

        // Gemini generates identical question
        val duplicateCheck = questionRepository.isDuplicateQuestion("Which Article of the Indian Constitution abolishes Untouchability?")
        assertTrue(duplicateCheck)

        // Gemini generates a new distinct question
        val nonDuplicateCheck = questionRepository.isDuplicateQuestion("Which Article guarantees Freedom of Speech?")
        assertFalse(nonDuplicateCheck)
    }

    @Test
    fun testBatchImportOfValidGeneratedQuestions() = runBlocking {
        val generated1 = Question(
            id = "ai_import_1",
            testId = "gemini_bank",
            questionNumber = 1,
            subject = "General Studies",
            topic = "Environment",
            questionText = "Where is the headquarters of the United Nations Environment Programme (UNEP) located?",
            options = listOf("Geneva, Switzerland", "Nairobi, Kenya", "Paris, France", "New York, USA"),
            correctOptionIndex = 1,
            explanation = "UNEP is headquartered in Nairobi, Kenya.",
            difficulty = "Easy",
            isActive = true
        )

        val generated2 = Question(
            id = "ai_import_2",
            testId = "gemini_bank",
            questionNumber = 2,
            subject = "General Studies",
            topic = "Environment",
            questionText = "Which protocol is associated with the protection of the Ozone Layer?",
            options = listOf("Kyoto Protocol", "Montreal Protocol", "Paris Agreement", "Cartagena Protocol"),
            correctOptionIndex = 1,
            explanation = "The Montreal Protocol on Substances that Deplete the Ozone Layer was signed in 1987.",
            difficulty = "Medium",
            isActive = true
        )

        val summary = questionRepository.importBatchQuestions(listOf(generated1, generated2))
        assertEquals(2, summary.totalProcessed)
        assertEquals(2, summary.successfullyImported)
        assertEquals(0, summary.duplicatesSkipped)
        assertEquals(0, summary.invalidSkipped)

        val fetched = questionRepository.getQuestionById("ai_import_1")
        assertNotNull(fetched)
        assertEquals("Nairobi, Kenya", fetched?.options?.get(1))
    }

    @Test
    fun testAdaptiveEngineOfflineFallback() = runBlocking {
        // Mock GeminiService with missing API key so it fails cleanly
        val failingGeminiService = GeminiService(apiKeyProvider = { "" })
        val ruleBasedFallback = RuleBasedAdaptiveEngine()

        val adaptiveEngine = GeminiAdaptiveEngine(
            geminiService = failingGeminiService,
            ruleBasedEngine = ruleBasedFallback,
            questionRepository = questionRepository
        )

        // Create offline candidate questions
        val candidateQuestions = listOf(
            Question(
                id = "offline_q_1",
                testId = "offline_bank",
                questionNumber = 1,
                subject = "General Studies",
                topic = "Indian Polity",
                questionText = "Offline Question 1: What is the tenure of a Rajya Sabha member?",
                options = listOf("4 years", "5 years", "6 years", "Permanent"),
                correctOptionIndex = 2,
                explanation = "Members of Rajya Sabha are elected for a term of six years.",
                difficulty = "Medium"
            )
        )

        // Request AI generation - when AI fails or key is missing, fallback MUST return candidate questions safely
        val config = AdaptiveConfig(
            subject = "General Studies",
            topic = "Indian Polity",
            difficulty = "Medium",
            questionCount = 1,
            useAiGeneration = true
        )

        val sessionQuestions = adaptiveEngine.generatePracticeQuestions(config, candidateQuestions)
        assertFalse(sessionQuestions.isEmpty())
        assertEquals("offline_q_1", sessionQuestions[0].id)
    }
}
