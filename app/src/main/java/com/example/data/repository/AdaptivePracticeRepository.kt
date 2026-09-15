package com.example.data.repository

import android.content.Context
import com.example.data.local.ExamSathiDatabase
import com.example.domain.model.AdaptiveConfig
import com.example.domain.model.Question
import com.example.domain.model.TestResult
import com.example.domain.model.WeakArea
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Clean architectural interface for the Adaptive Engine.
 * Enables zero-refactoring migration to Gemini AI in later stages.
 */
interface AdaptiveEngine {
    suspend fun analyzePerformance(results: List<TestResult>): List<WeakArea>
    suspend fun generatePracticeQuestions(config: AdaptiveConfig, candidateQuestions: List<Question>): List<Question>
}

/**
 * Production-ready offline implementation of Adaptive Engine.
 * Analyzes local performance and adapts questions based on accuracy, difficulty, and weak areas.
 */
class RuleBasedAdaptiveEngine : AdaptiveEngine {

    override suspend fun analyzePerformance(results: List<TestResult>): List<WeakArea> {
        if (results.isEmpty()) {
            return listOf(
                WeakArea(
                    subject = "Quantitative Aptitude",
                    topic = "Percentages & Algebra",
                    accuracy = 50.0,
                    totalAttempted = 0,
                    recommendedAction = "Start diagnostic practice to establish baseline strength"
                ),
                WeakArea(
                    subject = "General Studies",
                    topic = "Indian Polity & Constitution",
                    accuracy = 50.0,
                    totalAttempted = 0,
                    recommendedAction = "Practice core constitutional articles and amendments"
                )
            )
        }

        val topicStats = mutableMapOf<Pair<String, String>, Pair<Int, Int>>() // (Subject, Topic) -> (Correct, Attempted)

        for (result in results) {
            for (snap in result.responses) {
                if (snap.selectedOptionIndex != null) {
                    val key = Pair(snap.subject, snap.topic)
                    val current = topicStats.getOrDefault(key, Pair(0, 0))
                    val newCorrect = current.first + if (snap.isCorrect) 1 else 0
                    val newAttempted = current.second + 1
                    topicStats[key] = Pair(newCorrect, newAttempted)
                }
            }
        }

        val weakAreas = mutableListOf<WeakArea>()

        topicStats.forEach { (subTopic, stats) ->
            val correct = stats.first
            val attempted = stats.second
            val accuracy = if (attempted > 0) (correct.toDouble() / attempted.toDouble()) * 100.0 else 0.0

            if (accuracy < 70.0 || attempted < 3) {
                val recommendation = when {
                    accuracy < 40.0 -> "High priority: Review fundamental concepts and formulas"
                    accuracy < 65.0 -> "Moderate priority: Practice medium-difficulty drills"
                    else -> "Consolidate speed and timing"
                }
                weakAreas.add(
                    WeakArea(
                        subject = subTopic.first,
                        topic = subTopic.second,
                        accuracy = String.format("%.1f", accuracy).toDouble(),
                        totalAttempted = attempted,
                        recommendedAction = recommendation
                    )
                )
            }
        }

        // Sort by lowest accuracy first
        return weakAreas.sortedBy { it.accuracy }
    }

    override suspend fun generatePracticeQuestions(
        config: AdaptiveConfig,
        candidateQuestions: List<Question>
    ): List<Question> {
        var filtered = candidateQuestions

        // Filter by subject
        if (config.subject != "All Subjects") {
            filtered = filtered.filter { it.subject.equals(config.subject, ignoreCase = true) }
        }

        // Filter by topic
        if (config.topic != "All Topics") {
            filtered = filtered.filter { it.topic.contains(config.topic, ignoreCase = true) }
        }

        // Filter by difficulty if explicitly chosen
        if (config.difficulty != "Adaptive") {
            val matchingDiff = filtered.filter { it.difficulty.equals(config.difficulty, ignoreCase = true) }
            if (matchingDiff.isNotEmpty()) {
                filtered = matchingDiff
            }
        }

        // Shuffle to provide varied practice
        val shuffled = filtered.shuffled()
        return if (shuffled.size > config.questionCount) {
            shuffled.take(config.questionCount)
        } else {
            shuffled
        }
    }
}

/**
 * Architectural placeholder contract for future Gemini AI integration.
 * In later stages, this class will implement AdaptiveEngine using the server-side Gemini API.
 */
class GeminiAdaptiveEngineContract : AdaptiveEngine {
    override suspend fun analyzePerformance(results: List<TestResult>): List<WeakArea> {
        // Contract ready for Gemini AI prompt generation based on subject/topic/weak areas
        return RuleBasedAdaptiveEngine().analyzePerformance(results)
    }

    override suspend fun generatePracticeQuestions(
        config: AdaptiveConfig,
        candidateQuestions: List<Question>
    ): List<Question> {
        // Contract ready for dynamic Gemini AI question generation
        return RuleBasedAdaptiveEngine().generatePracticeQuestions(config, candidateQuestions)
    }
}

/**
 * Repository orchestrating adaptive practice and student diagnostics.
 */
class AdaptivePracticeRepository(
    private val database: ExamSathiDatabase,
    private val engine: AdaptiveEngine = RuleBasedAdaptiveEngine(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun getAvailableSubjects(): List<String> = withContext(ioDispatcher) {
        val dbSubjects = database.questionDao().getDistinctSubjects()
        val defaultSubjects = listOf(
            "All Subjects",
            "General Studies",
            "Quantitative Aptitude",
            "Logical Reasoning",
            "General Science"
        )
        (listOf("All Subjects") + dbSubjects).distinct()
    }

    suspend fun getTopicsForSubject(subject: String): List<String> = withContext(ioDispatcher) {
        if (subject == "All Subjects") {
            listOf("All Topics")
        } else {
            val topics = database.questionDao().getDistinctTopicsForSubject(subject)
            listOf("All Topics") + topics
        }
    }

    suspend fun getWeakAreas(): List<WeakArea> = withContext(ioDispatcher) {
        val results = database.testResultDao().getAllResults().first().map { entity ->
            TestResult(
                id = entity.id,
                testId = entity.testId,
                testTitle = entity.testTitle,
                testCategory = entity.testCategory,
                timestamp = entity.timestamp,
                durationMinutes = entity.durationMinutes,
                timeTakenSeconds = entity.timeTakenSeconds,
                totalQuestions = entity.totalQuestions,
                correctAnswers = entity.correctAnswers,
                wrongAnswers = entity.wrongAnswers,
                skippedAnswers = entity.skippedAnswers,
                score = entity.score,
                maxScore = entity.maxScore,
                accuracyPercentage = entity.accuracyPercentage,
                responses = com.example.data.local.JsonHelper.deserializeResponses(entity.responsesJson),
                subjectBreakdown = com.example.data.local.JsonHelper.deserializeSubjectScores(entity.subjectBreakdownJson)
            )
        }
        engine.analyzePerformance(results)
    }

    suspend fun createPracticeSession(config: AdaptiveConfig): List<Question> = withContext(ioDispatcher) {
        val allEntities = database.questionDao().getActiveQuestions()
        val allQuestions = allEntities.map {
            Question(
                id = it.id,
                testId = it.testId,
                questionNumber = it.questionNumber,
                subject = it.subject,
                topic = it.topic,
                questionText = it.questionText,
                options = listOf(it.optionA, it.optionB, it.optionC, it.optionD),
                correctOptionIndex = it.correctOptionIndex,
                marks = it.marks,
                negativeMarks = it.negativeMarks,
                explanation = it.explanation,
                difficulty = it.difficulty,
                isActive = it.isActive
            )
        }
        engine.generatePracticeQuestions(config, allQuestions)
    }

    companion object {
        @Volatile
        private var INSTANCE: AdaptivePracticeRepository? = null

        fun getInstance(context: Context): AdaptivePracticeRepository {
            return INSTANCE ?: synchronized(this) {
                val db = ExamSathiDatabase.getDatabase(context)
                val geminiEngine = GeminiAdaptiveEngine(
                    geminiService = com.example.data.remote.gemini.GeminiService(),
                    ruleBasedEngine = RuleBasedAdaptiveEngine(),
                    questionRepository = QuestionRepository.getInstance(context)
                )
                val instance = AdaptivePracticeRepository(db, geminiEngine)
                INSTANCE = instance
                instance
            }
        }
    }
}
