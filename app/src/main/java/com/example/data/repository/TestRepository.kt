package com.example.data.repository

import android.content.Context
import com.example.data.local.ExamSathiDatabase
import com.example.data.local.InitialDataSeed
import com.example.data.local.JsonHelper
import com.example.data.local.entity.MockTestEntity
import com.example.data.local.entity.QuestionEntity
import com.example.data.local.entity.TestResultEntity
import com.example.domain.model.MockTest
import com.example.domain.model.Question
import com.example.domain.model.StudentOverallStats
import com.example.domain.model.StudentResponse
import com.example.domain.model.StudentResponseSnapshot
import com.example.domain.model.SubjectScore
import com.example.domain.model.TestResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class TestRepository(
    private val database: ExamSathiDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun initializeSeedDataIfNeeded() = withContext(ioDispatcher) {
        val testCount = database.mockTestDao().getMockTestCount()
        if (testCount == 0) {
            database.mockTestDao().insertMockTests(InitialDataSeed.getInitialMockTests())
            database.questionDao().insertQuestions(InitialDataSeed.getInitialQuestions())
        }
    }

    fun getAllMockTests(): Flow<List<MockTest>> {
        return database.mockTestDao().getAllMockTests().map { entities ->
            entities.map { it.toDomain() }
        }.flowOn(ioDispatcher)
    }

    suspend fun getMockTestById(testId: String): MockTest? = withContext(ioDispatcher) {
        database.mockTestDao().getMockTestById(testId)?.toDomain()
    }

    suspend fun getQuestionsForTest(testId: String): List<Question> = withContext(ioDispatcher) {
        database.questionDao().getQuestionsForTest(testId).map { it.toDomain() }
    }

    suspend fun submitTest(
        test: MockTest,
        responses: Map<String, StudentResponse>,
        timeTakenSeconds: Int
    ): TestResult = withContext(ioDispatcher) {
        val questions = database.questionDao().getQuestionsForTest(test.id).map { it.toDomain() }

        var correctCount = 0
        var wrongCount = 0
        var skippedCount = 0
        var totalScore = 0.0

        val snapshots = mutableListOf<StudentResponseSnapshot>()
        val subjectMap = mutableMapOf<String, MutableList<Boolean?>>() // true = correct, false = wrong, null = skipped

        for (q in questions) {
            val resp = responses[q.id]
            val selected = resp?.selectedOptionIndex
            val isMarked = resp?.isMarkedForReview ?: false

            val isCorrect: Boolean
            val marksEarned: Double

            if (selected == null) {
                skippedCount++
                isCorrect = false
                marksEarned = 0.0
                subjectMap.getOrPut(q.subject) { mutableListOf() }.add(null)
            } else if (selected == q.correctOptionIndex) {
                correctCount++
                isCorrect = true
                marksEarned = q.marks
                totalScore += q.marks
                subjectMap.getOrPut(q.subject) { mutableListOf() }.add(true)
            } else {
                wrongCount++
                isCorrect = false
                marksEarned = -q.negativeMarks
                totalScore -= q.negativeMarks
                subjectMap.getOrPut(q.subject) { mutableListOf() }.add(false)
            }

            snapshots.add(
                StudentResponseSnapshot(
                    questionId = q.id,
                    questionNumber = q.questionNumber,
                    questionText = q.questionText,
                    subject = q.subject,
                    topic = q.topic,
                    options = q.options,
                    selectedOptionIndex = selected,
                    correctOptionIndex = q.correctOptionIndex,
                    isCorrect = isCorrect,
                    marksEarned = marksEarned,
                    explanation = q.explanation,
                    isMarkedForReview = isMarked
                )
            )
        }

        // Prevent negative final score if desired, or keep actual negative score per exam rules
        val finalScore = String.format("%.2f", totalScore).toDouble()
        val totalAttempted = correctCount + wrongCount
        val accuracy = if (totalAttempted > 0) {
            String.format("%.1f", (correctCount.toDouble() / totalAttempted.toDouble()) * 100).toDouble()
        } else {
            0.0
        }

        val subjectBreakdown = subjectMap.mapValues { (subject, results) ->
            val correct = results.count { it == true }
            val wrong = results.count { it == false }
            val sScore = (correct * test.positiveMarksPerQuestion) - (wrong * test.negativeMarksPerQuestion)
            SubjectScore(
                subject = subject,
                total = results.size,
                correct = correct,
                wrong = wrong,
                score = sScore
            )
        }

        val resultId = "res_" + UUID.randomUUID().toString().take(8)
        val result = TestResult(
            id = resultId,
            testId = test.id,
            testTitle = test.title,
            testCategory = test.category,
            timestamp = System.currentTimeMillis(),
            durationMinutes = test.durationMinutes,
            timeTakenSeconds = timeTakenSeconds,
            totalQuestions = questions.size,
            correctAnswers = correctCount,
            wrongAnswers = wrongCount,
            skippedAnswers = skippedCount,
            score = finalScore,
            maxScore = test.totalMarks,
            accuracyPercentage = accuracy,
            responses = snapshots,
            subjectBreakdown = subjectBreakdown
        )

        // Save locally to Room DB
        val entity = TestResultEntity(
            id = result.id,
            testId = result.testId,
            testTitle = result.testTitle,
            testCategory = result.testCategory,
            timestamp = result.timestamp,
            durationMinutes = result.durationMinutes,
            timeTakenSeconds = result.timeTakenSeconds,
            totalQuestions = result.totalQuestions,
            correctAnswers = result.correctAnswers,
            wrongAnswers = result.wrongAnswers,
            skippedAnswers = result.skippedAnswers,
            score = result.score,
            maxScore = result.maxScore,
            accuracyPercentage = result.accuracyPercentage,
            responsesJson = JsonHelper.serializeResponses(result.responses),
            subjectBreakdownJson = JsonHelper.serializeSubjectScores(result.subjectBreakdown)
        )
        database.testResultDao().insertResult(entity)

        result
    }

    fun getAllResults(): Flow<List<TestResult>> {
        return database.testResultDao().getAllResults().map { entities ->
            entities.map { it.toDomain() }
        }.flowOn(ioDispatcher)
    }

    suspend fun getResultById(id: String): TestResult? = withContext(ioDispatcher) {
        database.testResultDao().getResultById(id)?.toDomain()
    }

    suspend fun getStudentStats(): StudentOverallStats = withContext(ioDispatcher) {
        val results = database.testResultDao().getAllResults().first().map { it.toDomain() }
        if (results.isEmpty()) {
            return@withContext StudentOverallStats()
        }

        var totalScorePctSum = 0.0
        var totalAccuracySum = 0.0
        var totalCorrect = 0
        var totalWrong = 0
        var totalQuestions = 0

        val subjectPerformances = mutableMapOf<String, Pair<Int, Int>>() // subject -> (correct, totalAttempted)

        for (r in results) {
            val scorePct = if (r.maxScore > 0) (r.score / r.maxScore) * 100.0 else 0.0
            totalScorePctSum += scorePct
            totalAccuracySum += r.accuracyPercentage
            totalCorrect += r.correctAnswers
            totalWrong += r.wrongAnswers
            totalQuestions += (r.correctAnswers + r.wrongAnswers + r.skippedAnswers)

            for (snap in r.responses) {
                if (snap.selectedOptionIndex != null) {
                    val current = subjectPerformances.getOrDefault(snap.subject, Pair(0, 0))
                    val newCorrect = current.first + if (snap.isCorrect) 1 else 0
                    val newAttempted = current.second + 1
                    subjectPerformances[snap.subject] = Pair(newCorrect, newAttempted)
                }
            }
        }

        val weakList = mutableListOf<String>()
        val strongList = mutableListOf<String>()

        subjectPerformances.forEach { (sub, stats) ->
            if (stats.second > 0) {
                val acc = (stats.first.toDouble() / stats.second.toDouble()) * 100.0
                if (acc < 60.0) weakList.add(sub)
                else if (acc >= 75.0) strongList.add(sub)
            }
        }

        StudentOverallStats(
            totalTestsTaken = results.size,
            averageScorePercentage = String.format("%.1f", totalScorePctSum / results.size).toDouble(),
            averageAccuracyPercentage = String.format("%.1f", totalAccuracySum / results.size).toDouble(),
            totalQuestionsAttempted = totalCorrect + totalWrong,
            totalCorrect = totalCorrect,
            totalWrong = totalWrong,
            weakSubjects = weakList,
            strongSubjects = strongList
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: TestRepository? = null

        fun getInstance(context: Context): TestRepository {
            return INSTANCE ?: synchronized(this) {
                val db = ExamSathiDatabase.getDatabase(context)
                val instance = TestRepository(db)
                INSTANCE = instance
                instance
            }
        }
    }
}

// Mapper extensions
private fun MockTestEntity.toDomain(): MockTest = MockTest(
    id = id,
    title = title,
    description = description,
    category = category,
    durationMinutes = durationMinutes,
    totalQuestions = totalQuestions,
    totalMarks = totalMarks,
    passingMarks = passingMarks,
    positiveMarksPerQuestion = positiveMarksPerQuestion,
    negativeMarksPerQuestion = negativeMarksPerQuestion,
    difficulty = difficulty,
    subjectsCovered = subjectsCovered.split(",").map { it.trim() }.filter { it.isNotEmpty() },
    isAvailableOffline = isAvailableOffline
)

private fun QuestionEntity.toDomain(): Question = Question(
    id = id,
    testId = testId,
    questionNumber = questionNumber,
    subject = subject,
    topic = topic,
    questionText = questionText,
    options = listOf(optionA, optionB, optionC, optionD),
    correctOptionIndex = correctOptionIndex,
    marks = marks,
    negativeMarks = negativeMarks,
    explanation = explanation,
    difficulty = difficulty,
    isActive = isActive
)

private fun TestResultEntity.toDomain(): TestResult = TestResult(
    id = id,
    testId = testId,
    testTitle = testTitle,
    testCategory = testCategory,
    timestamp = timestamp,
    durationMinutes = durationMinutes,
    timeTakenSeconds = timeTakenSeconds,
    totalQuestions = totalQuestions,
    correctAnswers = correctAnswers,
    wrongAnswers = wrongAnswers,
    skippedAnswers = skippedAnswers,
    score = score,
    maxScore = maxScore,
    accuracyPercentage = accuracyPercentage,
    responses = JsonHelper.deserializeResponses(responsesJson),
    subjectBreakdown = JsonHelper.deserializeSubjectScores(subjectBreakdownJson)
)
