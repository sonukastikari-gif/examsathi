package com.example.data.repository

import com.example.data.remote.gemini.AiQuestionService
import com.example.data.remote.gemini.GeminiService
import com.example.domain.model.AdaptiveConfig
import com.example.domain.model.Question
import com.example.domain.model.TestResult
import com.example.domain.model.WeakArea
import com.example.util.SecureLog
import java.util.UUID

/**
 * Enhanced Adaptive Engine integrating Gemini AI for real-time question generation
 * while preserving complete, seamless offline fallback to [RuleBasedAdaptiveEngine].
 *
 * Security & Architecture:
 * - Uses [AiQuestionService] abstraction so direct Gemini calls can be substituted
 *   with an enterprise backend proxy without modifying business logic.
 * - Enforces data validation and sanitization on all incoming AI responses before persistence.
 * - Offline fallback is 100% resilient if the network or AI service fails.
 */
class GeminiAdaptiveEngine(
    private val aiQuestionService: AiQuestionService = GeminiService(),
    private val ruleBasedEngine: RuleBasedAdaptiveEngine = RuleBasedAdaptiveEngine(),
    private val questionRepository: QuestionRepository? = null
) : AdaptiveEngine {

    // Overload constructor to ensure seamless backward compatibility with existing tests
    constructor(
        geminiService: GeminiService,
        ruleBasedEngine: RuleBasedAdaptiveEngine = RuleBasedAdaptiveEngine(),
        questionRepository: QuestionRepository? = null
    ) : this(
        aiQuestionService = geminiService,
        ruleBasedEngine = ruleBasedEngine,
        questionRepository = questionRepository
    )

    companion object {
        private const val TAG = "GeminiAdaptiveEngine"
    }

    override suspend fun analyzePerformance(results: List<TestResult>): List<WeakArea> {
        // Leverages performance analysis based on historical accuracy and attempt patterns
        return ruleBasedEngine.analyzePerformance(results)
    }

    override suspend fun generatePracticeQuestions(
        config: AdaptiveConfig,
        candidateQuestions: List<Question>
    ): List<Question> {
        if (!config.useAiGeneration) {
            // Student selected offline bank practice
            return ruleBasedEngine.generatePracticeQuestions(config, candidateQuestions)
        }

        // Student requested Gemini AI generated practice
        val targetSubject = if (config.subject != "All Subjects") config.subject else "General Studies"
        val targetTopic = if (config.topic != "All Topics") config.topic else "General Knowledge & Aptitude"
        val targetDifficulty = if (config.difficulty != "Adaptive") config.difficulty else "Medium"
        val targetCount = config.questionCount.coerceIn(1, 15)

        try {
            SecureLog.d(TAG, "Requesting $targetCount questions from AI service for $targetSubject - $targetTopic ($targetDifficulty)")
            val geminiResult = aiQuestionService.generateQuestions(
                subject = targetSubject,
                topic = targetTopic,
                difficulty = targetDifficulty,
                count = targetCount
            )

            if (geminiResult.isSuccess) {
                val rawQuestions = geminiResult.getOrNull() ?: emptyList()
                val convertedQuestions = rawQuestions.mapIndexed { index, raw ->
                    Question(
                        id = "gemini_practice_${UUID.randomUUID().toString().take(8)}",
                        testId = "adaptive_ai_session",
                        questionNumber = index + 1,
                        subject = raw.subject.ifBlank { targetSubject },
                        topic = raw.topic.ifBlank { targetTopic },
                        questionText = raw.questionText,
                        options = raw.options,
                        correctOptionIndex = raw.correctOptionIndex,
                        marks = 2.0,
                        negativeMarks = 0.66,
                        explanation = raw.explanation,
                        difficulty = raw.difficulty.ifBlank { targetDifficulty },
                        isActive = true
                    )
                }

                // Validate questions to filter out any malformed items or malicious payloads
                val validQuestions = mutableListOf<Question>()
                for (q in convertedQuestions) {
                    if (q.questionText.isNotBlank() &&
                        q.questionText.length <= 2000 &&
                        q.options.size == 4 &&
                        q.options.all { it.isNotBlank() && it.length <= 500 } &&
                        q.correctOptionIndex in 0..3 &&
                        q.explanation.isNotBlank() &&
                        q.explanation.length <= 5000
                    ) {
                        validQuestions.add(q)
                    }
                }

                if (validQuestions.isNotEmpty()) {
                    SecureLog.d(TAG, "Successfully generated and validated ${validQuestions.size} AI practice questions.")

                    // Optionally persist into offline question bank cache for future offline reuse
                    questionRepository?.let { repo ->
                        try {
                            repo.importBatchQuestions(validQuestions)
                        } catch (e: Exception) {
                            SecureLog.w(TAG, "Error caching AI questions to local database", e)
                        }
                    }

                    return validQuestions
                } else {
                    SecureLog.w(TAG, "AI generated questions failed validation, falling back to offline bank")
                }
            } else {
                SecureLog.w(TAG, "AI generation failed. Falling back cleanly to offline bank.")
            }
        } catch (e: Throwable) {
            SecureLog.e(TAG, "Exception during AI practice generation, falling back cleanly to offline engine", e)
        }

        // Clean offline fallback: guarantee students can always practice regardless of AI/network status
        return ruleBasedEngine.generatePracticeQuestions(config, candidateQuestions)
    }
}
