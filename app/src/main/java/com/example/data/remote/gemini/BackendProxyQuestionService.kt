package com.example.data.remote.gemini

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
 * Production-ready Backend Proxy implementation of [AiQuestionService].
 *
 * In production environments, client-side API keys in APKs are vulnerable
 * to extraction via reverse-engineering (decompilation, strings analysis).
 * This service forwards requests to an authorized enterprise/backend proxy endpoint
 * where the Gemini API key is securely stored server-side.
 */
class BackendProxyQuestionService(
    private val proxyEndpointUrl: String,
    private val sessionAuthTokenProvider: (() -> String?)? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val client: OkHttpClient = defaultHttpClient()
) : AiQuestionService {

    companion object {
        private const val TAG = "BackendProxyService"

        private fun defaultHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build()
        }
    }

    override suspend fun generateQuestions(
        subject: String,
        topic: String,
        difficulty: String,
        count: Int
    ): Result<List<GeminiGeneratedQuestion>> = withContext(ioDispatcher) {
        if (proxyEndpointUrl.isBlank()) {
            return@withContext Result.failure(
                GeminiError.ServerError("Backend proxy endpoint URL is not configured.")
            )
        }

        val requestPayload = JSONObject().apply {
            put("subject", subject)
            put("topic", topic)
            put("difficulty", difficulty)
            put("count", count)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBuilder = Request.Builder()
            .url(proxyEndpointUrl)
            .post(requestPayload.toString().toRequestBody(mediaType))

        // Attach server authentication / Firebase App Check token if available
        sessionAuthTokenProvider?.invoke()?.let { token ->
            if (token.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
        }

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    SecureLog.e(TAG, "Backend proxy returned HTTP status code: ${response.code}")
                    return@withContext Result.failure(
                        GeminiError.ServerError("Backend proxy returned error ${response.code}")
                    )
                }

                val body = response.body?.string().orEmpty()
                if (body.isBlank()) {
                    return@withContext Result.failure(
                        GeminiError.InvalidResponse("Empty response from backend proxy.")
                    )
                }

                val questions = parseProxyResponse(body, subject, topic, difficulty)
                if (questions.isEmpty()) {
                    return@withContext Result.failure(
                        GeminiError.InvalidResponse("Failed to parse questions from backend proxy.")
                    )
                }

                Result.success(questions)
            }
        } catch (e: UnknownHostException) {
            SecureLog.e(TAG, "Network host unreachable for backend proxy", e)
            Result.failure(GeminiError.NetworkError("No internet connection to question service."))
        } catch (e: SocketTimeoutException) {
            SecureLog.e(TAG, "Backend proxy request timed out", e)
            Result.failure(GeminiError.NetworkError("Question generation request timed out."))
        } catch (e: IOException) {
            SecureLog.e(TAG, "I/O failure while communicating with backend proxy", e)
            Result.failure(GeminiError.NetworkError("Network error: ${e.message}"))
        } catch (e: Throwable) {
            SecureLog.e(TAG, "Unexpected error in backend proxy", e)
            Result.failure(GeminiError.Unknown("Unexpected failure: ${e.localizedMessage}"))
        }
    }

    private fun parseProxyResponse(
        jsonString: String,
        fallbackSubject: String,
        fallbackTopic: String,
        fallbackDifficulty: String
    ): List<GeminiGeneratedQuestion> {
        val result = mutableListOf<GeminiGeneratedQuestion>()
        try {
            val root = JSONObject(jsonString)
            val questionsArray = root.optJSONArray("questions") ?: return emptyList()

            for (i in 0 until questionsArray.length()) {
                val qObj = questionsArray.optJSONObject(i) ?: continue
                val text = qObj.optString("questionText", "").trim()
                val subj = qObj.optString("subject", fallbackSubject)
                val top = qObj.optString("topic", fallbackTopic)
                val diff = qObj.optString("difficulty", fallbackDifficulty)
                val explanation = qObj.optString("explanation", "").trim()
                val correctIndex = qObj.optInt("correctOptionIndex", -1)

                val optionsArray = qObj.optJSONArray("options")
                val options = mutableListOf<String>()
                if (optionsArray != null) {
                    for (j in 0 until optionsArray.length()) {
                        options.add(optionsArray.optString(j, "").trim())
                    }
                }

                if (text.isNotBlank() && options.size == 4 && correctIndex in 0..3) {
                    result.add(
                        GeminiGeneratedQuestion(
                            questionText = text,
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
            SecureLog.w(TAG, "Failed to parse backend proxy response", e)
        }
        return result
    }
}
