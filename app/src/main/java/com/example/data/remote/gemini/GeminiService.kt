package com.example.data.remote.gemini

import com.example.BuildConfig
import com.example.util.SecureLog
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Isolated Gemini API Service for ExamSathi implementing [AiQuestionService].
 * Handles API key retrieval, network requests, error handling,
 * and structured JSON response parsing.
 *
 * Security Enhancements:
 * - API key transmitted exclusively via the secure 'x-goog-api-key' HTTPS header (never in URL query params).
 * - Sensitive responses/payloads are never leaked to logs.
 * - Suppresses verbose and debug outputs in release builds.
 */
class GeminiService(
    private val apiKeyProvider: () -> String = { getSecretApiKey() },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val client: OkHttpClient = sharedHttpClient
) : AiQuestionService {

    companion object {
        private const val TAG = "GeminiService"
        // Recommended Gemini model for general reasoning & structured generation
        private const val MODEL_NAME = "gemini-2.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

        val sharedHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        }

        fun getSecretApiKey(): String {
            return try {
                val key = BuildConfig.GEMINI_API_KEY
                if (key.isBlank() || key == "MY_GEMINI_API_KEY" || key.startsWith("YOUR_") || key.equals("null", ignoreCase = true)) {
                    ""
                } else {
                    key.trim()
                }
            } catch (e: Throwable) {
                ""
            }
        }
    }

    /**
     * Generates structured exam questions via Gemini API.
     *
     * @param subject Subject domain (e.g., General Studies, Quantitative Aptitude)
     * @param topic Specific syllabus topic (e.g., Indian Polity, Percentage)
     * @param difficulty "Easy", "Medium", or "Hard"
     * @param count Number of questions requested (1 to 10)
     */
    override suspend fun generateQuestions(
        subject: String,
        topic: String,
        difficulty: String,
        count: Int
    ): Result<List<GeminiGeneratedQuestion>> = withContext(ioDispatcher) {
        val apiKey = apiKeyProvider()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                GeminiError.MissingApiKey(
                    "Gemini API key is not configured. Please set GEMINI_API_KEY in the AI Studio Secrets panel or .env file."
                )
            )
        }

        val prompt = buildGenerationPrompt(subject, topic, difficulty, count)
        val requestBodyJson = buildRequestBody(prompt)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        // Transmit API key securely via header rather than query param to avoid exposure in URLs and proxy logs
        val request = Request.Builder()
            .url(BASE_URL)
            .addHeader("x-goog-api-key", apiKey)
            .post(requestBodyJson.toRequestBody(mediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    val error = mapHttpError(response.code, responseBody)
                    SecureLog.e(TAG, "Gemini API HTTP error code: ${response.code}")
                    return@withContext Result.failure(error)
                }

                if (responseBody.isBlank()) {
                    return@withContext Result.failure(
                        GeminiError.InvalidResponse("Empty response received from Gemini AI.")
                    )
                }

                val questions = parseGeminiResponse(responseBody, subject, topic, difficulty)
                if (questions.isEmpty()) {
                    return@withContext Result.failure(
                        GeminiError.InvalidResponse("No valid questions could be extracted from Gemini AI response.")
                    )
                }

                Result.success(questions)
            }
        } catch (e: UnknownHostException) {
            SecureLog.e(TAG, "Network connection failure", e)
            Result.failure(GeminiError.NetworkError("No internet connection. Please check your network and try again."))
        } catch (e: SocketTimeoutException) {
            SecureLog.e(TAG, "Gemini request timed out", e)
            Result.failure(GeminiError.NetworkError("Request timed out while generating questions. Please try again."))
        } catch (e: IOException) {
            SecureLog.e(TAG, "I/O failure during Gemini API call", e)
            Result.failure(GeminiError.NetworkError("Network error while connecting to Gemini: ${e.message}"))
        } catch (e: Throwable) {
            SecureLog.e(TAG, "Unexpected error in Gemini API call", e)
            Result.failure(GeminiError.Unknown("Unexpected error: ${e.localizedMessage ?: "Unknown failure"}"))
        }
    }

    private fun buildGenerationPrompt(
        subject: String,
        topic: String,
        difficulty: String,
        count: Int
    ): String {
        return """
You are an expert exam question creator for Indian competitive examinations (UPSC, State PSC, SSC, Banking).
Generate exactly $count high-quality multiple choice questions with the following parameters:
- Subject: $subject
- Topic: $topic
- Difficulty Level: $difficulty

STRICT REQUIREMENTS:
1. Each question must be rigorous, factually accurate, and relevant to competitive exams.
2. Provide EXACTLY 4 distinct, plausible options for each question (indexed 0, 1, 2, 3).
3. Specify the 0-based integer index of the correct option (0 for first, 1 for second, 2 for third, 3 for fourth).
4. Provide a clear, thorough explanation citing relevant facts, articles, laws, or formulas.
5. Return ONLY a valid JSON object matching the exact structure below. Do not include markdown code block formatting, introductory text, or closing text.

Required JSON Structure:
{
  "questions": [
    {
      "questionText": "Full question statement here?",
      "subject": "$subject",
      "topic": "$topic",
      "difficulty": "$difficulty",
      "options": [
        "Option 1",
        "Option 2",
        "Option 3",
        "Option 4"
      ],
      "correctOptionIndex": 0,
      "explanation": "Detailed explanation of why the correct option is right and others are incorrect."
    }
  ]
}
""".trimIndent()
    }

    internal fun buildRequestBody(prompt: String): String {
        val root = JSONObject()

        val contents = JSONArray()
        val contentObj = JSONObject()
        val parts = JSONArray()
        val partObj = JSONObject()
        partObj.put("text", prompt)
        parts.put(partObj)
        contentObj.put("parts", parts)
        contents.put(contentObj)
        root.put("contents", contents)

        val genConfig = JSONObject()
        genConfig.put("responseMimeType", "application/json")
        genConfig.put("temperature", 0.7)
        root.put("generationConfig", genConfig)

        return root.toString()
    }

    internal fun mapHttpError(statusCode: Int, body: String): GeminiError {
        return when (statusCode) {
            400 -> GeminiError.InvalidResponse("Bad request to Gemini API. Please check query parameters.")
            401, 403 -> GeminiError.AuthenticationError("Invalid API key or unauthorized access to Gemini API.")
            429 -> GeminiError.QuotaExceeded("Gemini API rate limit reached. Please wait a few moments and try again.")
            in 500..599 -> GeminiError.ServerError("Gemini servers are currently experiencing issues. Please try again.")
            else -> GeminiError.Unknown("Gemini API returned error HTTP $statusCode.")
        }
    }

    /**
     * Parses the Gemini generateContent JSON response into a list of [GeminiGeneratedQuestion].
     */
    fun parseGeminiResponse(
        responseJson: String,
        fallbackSubject: String,
        fallbackTopic: String,
        fallbackDifficulty: String
    ): List<GeminiGeneratedQuestion> {
        val questionsList = mutableListOf<GeminiGeneratedQuestion>()
        try {
            val root = JSONObject(responseJson)
            val candidates = root.optJSONArray("candidates") ?: return emptyList()
            if (candidates.length() == 0) return emptyList()

            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content") ?: return emptyList()
            val parts = content.optJSONArray("parts") ?: return emptyList()
            if (parts.length() == 0) return emptyList()

            val rawText = parts.getJSONObject(0).optString("text", "").trim()
            if (rawText.isBlank()) return emptyList()

            // Remove markdown backticks if present
            val cleanedJson = cleanJsonString(rawText)

            val parsedJson = JSONObject(cleanedJson)
            val questionsArray = parsedJson.optJSONArray("questions") ?: return emptyList()

            for (i in 0 until questionsArray.length()) {
                val qObj = questionsArray.optJSONObject(i) ?: continue

                val qText = qObj.optString("questionText", "").trim()
                val subj = qObj.optString("subject", fallbackSubject).ifBlank { fallbackSubject }
                val top = qObj.optString("topic", fallbackTopic).ifBlank { fallbackTopic }
                val diff = qObj.optString("difficulty", fallbackDifficulty).ifBlank { fallbackDifficulty }
                val explanation = qObj.optString("explanation", "").trim()
                val correctIndex = qObj.optInt("correctOptionIndex", -1)

                val optionsArray = qObj.optJSONArray("options")
                val options = mutableListOf<String>()
                if (optionsArray != null) {
                    for (j in 0 until optionsArray.length()) {
                        options.add(optionsArray.optString(j, "").trim())
                    }
                }

                if (qText.isNotBlank() && options.isNotEmpty()) {
                    questionsList.add(
                        GeminiGeneratedQuestion(
                            questionText = qText,
                            subject = subj,
                            topic = top,
                            difficulty = diff,
                            options = options,
                            correctOptionIndex = correctIndex,
                            explanation = explanation
                        )
                    )
                }
            }
        } catch (e: Exception) {
            SecureLog.w(TAG, "Error parsing Gemini response JSON", e)
        }
        return questionsList
    }

    private fun cleanJsonString(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```json")) {
            text = text.removePrefix("```json").trim()
        } else if (text.startsWith("```")) {
            text = text.removePrefix("```").trim()
        }
        if (text.endsWith("```")) {
            text = text.removeSuffix("```").trim()
        }
        return text
    }
}
