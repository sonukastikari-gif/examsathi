package com.example.data.remote.gemini

/**
 * Clean architectural abstraction for AI question generation services.
 * Decouples the UI and repository layers from direct Gemini client-side calls,
 * allowing a secure backend proxy (or alternative AI provider) to replace
 * the direct client API call in production without changing UI or business logic.
 */
interface AiQuestionService {

    /**
     * Generates structured exam questions for a given syllabus subject, topic, and difficulty.
     *
     * @param subject Target subject (e.g., General Studies, Quantitative Aptitude)
     * @param topic Target syllabus topic
     * @param difficulty "Easy", "Medium", or "Hard"
     * @param count Number of questions requested (1 to 10)
     */
    suspend fun generateQuestions(
        subject: String,
        topic: String,
        difficulty: String,
        count: Int
    ): Result<List<GeminiGeneratedQuestion>>
}
