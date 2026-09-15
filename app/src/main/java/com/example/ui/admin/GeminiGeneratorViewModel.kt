package com.example.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.gemini.GeminiError
import com.example.data.remote.gemini.GeminiService
import com.example.data.remote.gemini.ReviewableGeneratedQuestion
import com.example.data.repository.QuestionRepository
import com.example.domain.model.Question
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class GeminiGeneratorUiState(
    val selectedSubject: String = "General Studies",
    val customSubject: String = "",
    val selectedTopic: String = "Indian Constitution & Polity",
    val selectedDifficulty: String = "Medium",
    val questionCount: Int = 3,
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val importSuccessMessage: String? = null,
    val isApiKeyConfigured: Boolean = true,
    val generatedQuestions: List<ReviewableGeneratedQuestion> = emptyList(),
    val availableSubjects: List<String> = listOf(
        "General Studies",
        "Quantitative Aptitude",
        "Logical Reasoning",
        "General Science",
        "Indian History",
        "Geography"
    )
) {
    val effectiveSubject: String
        get() = if (selectedSubject == "Custom") customSubject.trim() else selectedSubject

    val selectedCount: Int
        get() = generatedQuestions.count { it.isSelected && it.isImportable }

    val totalImportableCount: Int
        get() = generatedQuestions.count { it.isImportable }
}

class GeminiGeneratorViewModel(
    application: Application,
    private val geminiService: GeminiService = GeminiService(),
    private val questionRepository: QuestionRepository = QuestionRepository.getInstance(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(GeminiGeneratorUiState())
    val uiState: StateFlow<GeminiGeneratorUiState> = _uiState.asStateFlow()

    init {
        checkApiKeyStatus()
        loadSubjectsFromDatabase()
    }

    private fun checkApiKeyStatus() {
        val key = GeminiService.getSecretApiKey()
        _uiState.update { it.copy(isApiKeyConfigured = key.isNotBlank()) }
    }

    private fun loadSubjectsFromDatabase() {
        viewModelScope.launch {
            try {
                val dbSubjects = questionRepository.getDistinctSubjects()
                if (dbSubjects.isNotEmpty()) {
                    val combined = (_uiState.value.availableSubjects + dbSubjects).distinct()
                    _uiState.update { it.copy(availableSubjects = combined) }
                }
            } catch (e: Exception) {
                // Keep default subjects
            }
        }
    }

    fun selectSubject(subject: String) {
        val defaultTopic = when (subject) {
            "General Studies" -> "Indian Constitution & Fundamental Rights"
            "Quantitative Aptitude" -> "Percentages, Profit & Loss"
            "Logical Reasoning" -> "Syllogisms & Analytical Reasoning"
            "General Science" -> "Newton's Laws & Thermodynamics"
            "Indian History" -> "Mughal Empire & Freedom Movement"
            "Geography" -> "Physical Features & River Systems of India"
            else -> "Core Concepts"
        }
        _uiState.update {
            it.copy(
                selectedSubject = subject,
                selectedTopic = defaultTopic,
                errorMessage = null,
                importSuccessMessage = null
            )
        }
    }

    fun setCustomSubject(custom: String) {
        _uiState.update { it.copy(customSubject = custom, errorMessage = null) }
    }

    fun setTopic(topic: String) {
        _uiState.update { it.copy(selectedTopic = topic, errorMessage = null) }
    }

    fun setDifficulty(difficulty: String) {
        _uiState.update { it.copy(selectedDifficulty = difficulty) }
    }

    fun setQuestionCount(count: Int) {
        _uiState.update { it.copy(questionCount = count.coerceIn(1, 10)) }
    }

    fun generateQuestions() {
        val currentState = _uiState.value
        val subject = currentState.effectiveSubject
        val topic = currentState.selectedTopic.trim()
        val difficulty = currentState.selectedDifficulty
        val count = currentState.questionCount

        if (subject.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter or select a valid subject.") }
            return
        }

        if (topic.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter a specific topic.") }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                importSuccessMessage = null,
                statusMessage = "Connecting to Gemini AI to create $count questions..."
            )
        }

        viewModelScope.launch {
            val result = geminiService.generateQuestions(
                subject = subject,
                topic = topic,
                difficulty = difficulty,
                count = count
            )

            result.fold(
                onSuccess = { rawQuestions ->
                    _uiState.update { it.copy(statusMessage = "Validating generated questions...") }

                    val reviewList = mutableListOf<ReviewableGeneratedQuestion>()
                    for ((index, raw) in rawQuestions.withIndex()) {
                        val questionId = "gemini_gen_${UUID.randomUUID().toString().take(8)}"
                        val domainQuestion = Question(
                            id = questionId,
                            testId = "gemini_bank",
                            questionNumber = index + 1,
                            subject = raw.subject.ifBlank { subject },
                            topic = raw.topic.ifBlank { topic },
                            questionText = raw.questionText,
                            options = raw.options,
                            correctOptionIndex = raw.correctOptionIndex,
                            marks = 2.0,
                            negativeMarks = 0.66,
                            explanation = raw.explanation,
                            difficulty = raw.difficulty.ifBlank { difficulty },
                            isActive = true
                        )

                        // Run validation against standard schema
                        val validation = questionRepository.validateQuestion(domainQuestion)
                        // Run duplicate check against local question bank
                        val isDuplicate = questionRepository.isDuplicateQuestion(domainQuestion.questionText)

                        reviewList.add(
                            ReviewableGeneratedQuestion(
                                id = questionId,
                                question = domainQuestion,
                                validationResult = validation,
                                isDuplicate = isDuplicate,
                                isSelected = validation.isValid && !isDuplicate
                            )
                        )
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            statusMessage = null,
                            generatedQuestions = reviewList,
                            errorMessage = if (reviewList.isEmpty()) "No questions returned by Gemini AI." else null
                        )
                    }
                },
                onFailure = { error ->
                    val userFriendlyError = when (error) {
                        is GeminiError.MissingApiKey -> error.message
                        is GeminiError.NetworkError -> error.message
                        is GeminiError.AuthenticationError -> error.message
                        is GeminiError.QuotaExceeded -> error.message
                        is GeminiError.ServerError -> error.message
                        is GeminiError.InvalidResponse -> error.message
                        else -> "Failed to generate questions: ${error.localizedMessage ?: "Unknown error"}"
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            statusMessage = null,
                            errorMessage = userFriendlyError
                        )
                    }
                }
            )
        }
    }

    fun toggleQuestionSelection(id: String) {
        _uiState.update { state ->
            val updated = state.generatedQuestions.map { item ->
                if (item.id == id && item.isImportable) {
                    item.copy(isSelected = !item.isSelected)
                } else {
                    item
                }
            }
            state.copy(generatedQuestions = updated)
        }
    }

    fun selectAll(select: Boolean) {
        _uiState.update { state ->
            val updated = state.generatedQuestions.map { item ->
                if (item.isImportable) {
                    item.copy(isSelected = select)
                } else {
                    item
                }
            }
            state.copy(generatedQuestions = updated)
        }
    }

    fun importSelectedQuestions() {
        val selectedItems = _uiState.value.generatedQuestions.filter { it.isSelected && it.isImportable }
        if (selectedItems.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "No valid questions selected for import.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = "Importing questions into offline Question Bank...") }

            val questionsToImport = selectedItems.map { it.question }
            val summary = questionRepository.importBatchQuestions(questionsToImport)

            _uiState.update { state ->
                // Remove imported questions from generation review list
                val importedIds = selectedItems.map { it.id }.toSet()
                val remaining = state.generatedQuestions.filterNot { importedIds.contains(it.id) }

                state.copy(
                    isLoading = false,
                    statusMessage = null,
                    generatedQuestions = remaining,
                    importSuccessMessage = "Successfully imported ${summary.successfullyImported} questions into the Question Bank!"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, importSuccessMessage = null) }
    }
}
