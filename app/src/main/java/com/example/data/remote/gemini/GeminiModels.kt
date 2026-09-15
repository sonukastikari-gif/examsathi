package com.example.data.remote.gemini

import com.example.domain.model.Question
import com.example.domain.model.QuestionValidationResult

/**
 * Domain representation of a raw question returned by the Gemini API
 * before conversion and validation into [Question].
 */
data class GeminiGeneratedQuestion(
    val questionText: String,
    val subject: String,
    val topic: String,
    val difficulty: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanation: String
)

/**
 * Item shown in the Admin AI Question Generator review list.
 */
data class ReviewableGeneratedQuestion(
    val id: String,
    val question: Question,
    val validationResult: QuestionValidationResult,
    val isDuplicate: Boolean,
    val isSelected: Boolean = true
) {
    val isImportable: Boolean
        get() = validationResult.isValid && !isDuplicate
}

/**
 * Sealed class representing possible errors during Gemini API operations.
 */
sealed class GeminiError(override val message: String) : Exception(message) {
    data class MissingApiKey(override val message: String = "Gemini API key is not configured. Please add GEMINI_API_KEY in the Secrets panel.") : GeminiError(message)
    data class NetworkError(override val message: String) : GeminiError(message)
    data class AuthenticationError(override val message: String = "Invalid or unauthorized Gemini API key.") : GeminiError(message)
    data class QuotaExceeded(override val message: String = "Gemini API rate limit or quota exceeded. Please try again shortly.") : GeminiError(message)
    data class ServerError(override val message: String = "Gemini AI service error. Please try again.") : GeminiError(message)
    data class InvalidResponse(override val message: String = "Unable to parse valid question structure from AI response.") : GeminiError(message)
    data class Unknown(override val message: String) : GeminiError(message)
}
