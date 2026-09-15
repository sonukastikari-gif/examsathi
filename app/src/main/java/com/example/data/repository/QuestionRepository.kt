package com.example.data.repository

import android.content.Context
import com.example.data.local.ExamSathiDatabase
import com.example.data.local.entity.QuestionEntity
import com.example.domain.model.Question
import com.example.domain.model.QuestionValidationError
import com.example.domain.model.QuestionValidationResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

data class BatchImportSummary(
    val totalProcessed: Int,
    val successfullyImported: Int,
    val duplicatesSkipped: Int,
    val invalidSkipped: Int
)

class QuestionRepository(
    private val database: ExamSathiDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val questionDao = database.questionDao()

    fun getAllQuestionsFlow(): Flow<List<Question>> {
        return questionDao.getAllQuestionsFlow().map { list ->
            list.map { it.toDomain() }
        }.flowOn(ioDispatcher)
    }

    suspend fun getQuestionById(id: String): Question? = withContext(ioDispatcher) {
        questionDao.getQuestionById(id)?.toDomain()
    }

    suspend fun getDistinctSubjects(): List<String> = withContext(ioDispatcher) {
        val dbSubjects = questionDao.getDistinctSubjects()
        val baseSubjects = listOf(
            "General Studies",
            "Quantitative Aptitude",
            "Logical Reasoning",
            "General Science",
            "Indian Polity",
            "History & Culture",
            "Geography & Environment"
        )
        (baseSubjects + dbSubjects).distinct()
    }

    suspend fun getDistinctTopicsForSubject(subject: String): List<String> = withContext(ioDispatcher) {
        questionDao.getDistinctTopicsForSubject(subject)
    }

    suspend fun validateQuestion(
        question: Question,
        isEditing: Boolean = false
    ): QuestionValidationResult = withContext(ioDispatcher) {
        val errors = mutableListOf<QuestionValidationError>()

        if (question.questionText.trim().isEmpty()) {
            errors.add(QuestionValidationError.BlankQuestionText)
        }

        if (question.subject.trim().isEmpty()) {
            errors.add(QuestionValidationError.BlankSubject)
        }

        if (question.topic.trim().isEmpty()) {
            errors.add(QuestionValidationError.BlankTopic)
        }

        if (question.options.size != 4) {
            errors.add(QuestionValidationError.IncompleteOption(0))
        } else {
            question.options.forEachIndexed { index, option ->
                if (option.trim().isEmpty()) {
                    errors.add(QuestionValidationError.IncompleteOption(index))
                }
            }
        }

        if (question.correctOptionIndex !in 0..3) {
            errors.add(QuestionValidationError.InvalidCorrectOption)
        }

        if (question.explanation.trim().isEmpty()) {
            errors.add(QuestionValidationError.BlankExplanation)
        }

        // Check for duplicate question text
        val normalizedTarget = normalizeText(question.questionText)
        if (normalizedTarget.isNotEmpty()) {
            val allQuestions = questionDao.getAllQuestions()
            val duplicate = allQuestions.any { existing ->
                val isDifferentId = if (isEditing) existing.id != question.id else true
                isDifferentId && normalizeText(existing.questionText) == normalizedTarget
            }
            if (duplicate) {
                errors.add(QuestionValidationError.DuplicateQuestion)
            }
        }

        val errorMessage = when {
            errors.contains(QuestionValidationError.BlankQuestionText) -> "Question text cannot be empty."
            errors.contains(QuestionValidationError.BlankSubject) -> "Please specify a subject."
            errors.contains(QuestionValidationError.BlankTopic) -> "Please specify a topic."
            errors.any { it is QuestionValidationError.IncompleteOption } -> "All 4 options must be non-empty."
            errors.contains(QuestionValidationError.InvalidCorrectOption) -> "Select a valid correct option."
            errors.contains(QuestionValidationError.BlankExplanation) -> "Please provide a solution explanation."
            errors.contains(QuestionValidationError.DuplicateQuestion) -> "A question with identical content already exists."
            else -> null
        }

        QuestionValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            errorMessage = errorMessage
        )
    }

    suspend fun saveQuestion(question: Question): Result<String> = withContext(ioDispatcher) {
        try {
            val validation = validateQuestion(question, isEditing = false)
            if (!validation.isValid) {
                return@withContext Result.failure(IllegalArgumentException(validation.errorMessage ?: "Validation failed"))
            }

            val questionId = if (question.id.isBlank()) "q_bank_${UUID.randomUUID().toString().take(8)}" else question.id
            val entity = question.copy(id = questionId).toEntity()
            questionDao.insertQuestion(entity)
            Result.success(questionId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateQuestion(question: Question): Result<Unit> = withContext(ioDispatcher) {
        try {
            val validation = validateQuestion(question, isEditing = true)
            if (!validation.isValid) {
                return@withContext Result.failure(IllegalArgumentException(validation.errorMessage ?: "Validation failed"))
            }

            questionDao.updateQuestion(question.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setQuestionActive(questionId: String, isActive: Boolean): Result<Unit> = withContext(ioDispatcher) {
        try {
            questionDao.updateQuestionStatus(questionId, isActive)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteQuestion(questionId: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            questionDao.deleteQuestionById(questionId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Architecture ready for future Gemini-generated question ingestion.
     */
    suspend fun importBatchQuestions(questions: List<Question>): BatchImportSummary = withContext(ioDispatcher) {
        var imported = 0
        var duplicates = 0
        var invalid = 0

        val existingNormalized = questionDao.getAllQuestions()
            .map { normalizeText(it.questionText) }
            .toMutableSet()

        val validEntities = mutableListOf<QuestionEntity>()

        for (q in questions) {
            val normalized = normalizeText(q.questionText)
            // Strict sanitization & validation of external / AI-generated question payloads
            if (normalized.isEmpty() ||
                q.questionText.length > 3000 ||
                q.options.size != 4 ||
                q.options.any { it.isBlank() || it.length > 500 } ||
                q.correctOptionIndex !in 0..3 ||
                q.explanation.isBlank() ||
                q.explanation.length > 5000 ||
                q.subject.isBlank() ||
                q.topic.isBlank()
            ) {
                invalid++
                continue
            }

            if (existingNormalized.contains(normalized)) {
                duplicates++
                continue
            }

            existingNormalized.add(normalized)
            val assignedId = if (q.id.isBlank()) "q_import_${UUID.randomUUID().toString().take(8)}" else q.id
            validEntities.add(q.copy(id = assignedId).toEntity())
            imported++
        }

        if (validEntities.isNotEmpty()) {
            questionDao.insertQuestions(validEntities)
        }

        BatchImportSummary(
            totalProcessed = questions.size,
            successfullyImported = imported,
            duplicatesSkipped = duplicates,
            invalidSkipped = invalid
        )
    }

    suspend fun isDuplicateQuestion(questionText: String, excludeId: String? = null): Boolean = withContext(ioDispatcher) {
        val normalizedTarget = normalizeText(questionText)
        if (normalizedTarget.isEmpty()) return@withContext false
        val allQuestions = questionDao.getAllQuestions()
        allQuestions.any { existing ->
            val isDifferentId = if (excludeId != null) existing.id != excludeId else true
            isDifferentId && normalizeText(existing.questionText) == normalizedTarget
        }
    }

    private fun normalizeText(text: String): String {
        return text.trim().lowercase().replace(Regex("[^a-zA-Z0-9]"), "")
    }

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

    private fun Question.toEntity(): QuestionEntity = QuestionEntity(
        id = id,
        testId = testId,
        questionNumber = questionNumber,
        subject = subject.trim(),
        topic = topic.trim(),
        questionText = questionText.trim(),
        optionA = options.getOrElse(0) { "" }.trim(),
        optionB = options.getOrElse(1) { "" }.trim(),
        optionC = options.getOrElse(2) { "" }.trim(),
        optionD = options.getOrElse(3) { "" }.trim(),
        correctOptionIndex = correctOptionIndex,
        marks = marks,
        negativeMarks = negativeMarks,
        explanation = explanation.trim(),
        difficulty = difficulty,
        isActive = isActive
    )

    companion object {
        @Volatile
        private var INSTANCE: QuestionRepository? = null

        fun getInstance(context: Context): QuestionRepository {
            return INSTANCE ?: synchronized(this) {
                val db = ExamSathiDatabase.getDatabase(context)
                val instance = QuestionRepository(db)
                INSTANCE = instance
                instance
            }
        }
    }
}
