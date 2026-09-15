package com.example.domain.model

data class Question(
    val id: String = "",
    val testId: String = "bank",
    val questionNumber: Int = 1,
    val subject: String,
    val topic: String,
    val questionText: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val marks: Double = 2.0,
    val negativeMarks: Double = 0.5,
    val explanation: String,
    val difficulty: String = "Medium",
    val isActive: Boolean = true
)

sealed class QuestionValidationError {
    object BlankQuestionText : QuestionValidationError()
    object BlankSubject : QuestionValidationError()
    object BlankTopic : QuestionValidationError()
    data class IncompleteOption(val index: Int) : QuestionValidationError()
    object InvalidCorrectOption : QuestionValidationError()
    object BlankExplanation : QuestionValidationError()
    object DuplicateQuestion : QuestionValidationError()
}

data class QuestionValidationResult(
    val isValid: Boolean,
    val errors: List<QuestionValidationError> = emptyList(),
    val errorMessage: String? = null
)

data class MockTest(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val durationMinutes: Int,
    val totalQuestions: Int,
    val totalMarks: Double,
    val passingMarks: Double,
    val positiveMarksPerQuestion: Double = 2.0,
    val negativeMarksPerQuestion: Double = 0.5,
    val difficulty: String = "Medium",
    val subjectsCovered: List<String> = emptyList(),
    val isAvailableOffline: Boolean = true
)

data class StudentResponse(
    val questionId: String,
    val selectedOptionIndex: Int? = null,
    val isMarkedForReview: Boolean = false,
    val timeSpentSeconds: Int = 0
)

data class StudentResponseSnapshot(
    val questionId: String,
    val questionNumber: Int,
    val questionText: String,
    val subject: String,
    val topic: String,
    val options: List<String>,
    val selectedOptionIndex: Int?,
    val correctOptionIndex: Int,
    val isCorrect: Boolean,
    val marksEarned: Double,
    val explanation: String,
    val isMarkedForReview: Boolean = false
)

data class SubjectScore(
    val subject: String,
    val total: Int,
    val correct: Int,
    val wrong: Int,
    val score: Double
)

data class TestResult(
    val id: String,
    val testId: String,
    val testTitle: String,
    val testCategory: String,
    val timestamp: Long,
    val durationMinutes: Int,
    val timeTakenSeconds: Int,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val wrongAnswers: Int,
    val skippedAnswers: Int,
    val score: Double,
    val maxScore: Double,
    val accuracyPercentage: Double,
    val responses: List<StudentResponseSnapshot>,
    val subjectBreakdown: Map<String, SubjectScore> = emptyMap()
)

data class StudentOverallStats(
    val totalTestsTaken: Int = 0,
    val averageScorePercentage: Double = 0.0,
    val averageAccuracyPercentage: Double = 0.0,
    val totalQuestionsAttempted: Int = 0,
    val totalCorrect: Int = 0,
    val totalWrong: Int = 0,
    val weakSubjects: List<String> = emptyList(),
    val strongSubjects: List<String> = emptyList()
)

data class AdaptiveConfig(
    val subject: String = "All Subjects",
    val topic: String = "All Topics",
    val difficulty: String = "Adaptive", // "Easy", "Medium", "Hard", "Adaptive"
    val questionCount: Int = 10,
    val focusOnWeakAreas: Boolean = true,
    val useAiGeneration: Boolean = false
)

data class WeakArea(
    val subject: String,
    val topic: String,
    val accuracy: Double,
    val totalAttempted: Int,
    val recommendedAction: String
)
