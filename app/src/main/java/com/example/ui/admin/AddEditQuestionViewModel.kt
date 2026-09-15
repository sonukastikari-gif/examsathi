package com.example.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.QuestionRepository
import com.example.domain.model.Question
import com.example.domain.model.QuestionValidationError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddEditQuestionUiState(
    val questionId: String? = null,
    val subject: String = "",
    val topic: String = "",
    val difficulty: String = "Medium",
    val questionText: String = "",
    val optionA: String = "",
    val optionB: String = "",
    val optionC: String = "",
    val optionD: String = "",
    val correctOptionIndex: Int = 0,
    val explanation: String = "",
    val isActive: Boolean = true,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaveCompleted: Boolean = false,
    val errorMessage: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val availableSubjects: List<String> = emptyList()
)

class AddEditQuestionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = QuestionRepository.getInstance(application)

    private val _uiState = MutableStateFlow(AddEditQuestionUiState())
    val uiState: StateFlow<AddEditQuestionUiState> = _uiState.asStateFlow()

    init {
        loadSubjects()
    }

    private fun loadSubjects() {
        viewModelScope.launch {
            val subjects = repository.getDistinctSubjects()
            _uiState.value = _uiState.value.copy(availableSubjects = subjects)
        }
    }

    fun loadQuestion(questionId: String?) {
        if (questionId.isNullOrBlank() || questionId == "new") {
            _uiState.value = AddEditQuestionUiState(availableSubjects = _uiState.value.availableSubjects)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val question = repository.getQuestionById(questionId)
            if (question != null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    questionId = question.id,
                    subject = question.subject,
                    topic = question.topic,
                    difficulty = question.difficulty,
                    questionText = question.questionText,
                    optionA = question.options.getOrElse(0) { "" },
                    optionB = question.options.getOrElse(1) { "" },
                    optionC = question.options.getOrElse(2) { "" },
                    optionD = question.options.getOrElse(3) { "" },
                    correctOptionIndex = question.correctOptionIndex,
                    explanation = question.explanation,
                    isActive = question.isActive
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Question not found."
                )
            }
        }
    }

    fun onSubjectChange(value: String) {
        _uiState.value = _uiState.value.copy(
            subject = value,
            fieldErrors = _uiState.value.fieldErrors - "subject"
        )
    }

    fun onTopicChange(value: String) {
        _uiState.value = _uiState.value.copy(
            topic = value,
            fieldErrors = _uiState.value.fieldErrors - "topic"
        )
    }

    fun onDifficultyChange(value: String) {
        _uiState.value = _uiState.value.copy(difficulty = value)
    }

    fun onQuestionTextChange(value: String) {
        _uiState.value = _uiState.value.copy(
            questionText = value,
            fieldErrors = _uiState.value.fieldErrors - "questionText"
        )
    }

    fun onOptionAChange(value: String) {
        _uiState.value = _uiState.value.copy(
            optionA = value,
            fieldErrors = _uiState.value.fieldErrors - "optionA"
        )
    }

    fun onOptionBChange(value: String) {
        _uiState.value = _uiState.value.copy(
            optionB = value,
            fieldErrors = _uiState.value.fieldErrors - "optionB"
        )
    }

    fun onOptionCChange(value: String) {
        _uiState.value = _uiState.value.copy(
            optionC = value,
            fieldErrors = _uiState.value.fieldErrors - "optionC"
        )
    }

    fun onOptionDChange(value: String) {
        _uiState.value = _uiState.value.copy(
            optionD = value,
            fieldErrors = _uiState.value.fieldErrors - "optionD"
        )
    }

    fun onCorrectOptionSelect(index: Int) {
        if (index in 0..3) {
            _uiState.value = _uiState.value.copy(correctOptionIndex = index)
        }
    }

    fun onExplanationChange(value: String) {
        _uiState.value = _uiState.value.copy(
            explanation = value,
            fieldErrors = _uiState.value.fieldErrors - "explanation"
        )
    }

    fun onActiveChange(value: Boolean) {
        _uiState.value = _uiState.value.copy(isActive = value)
    }

    fun saveQuestion(onSuccess: () -> Unit) {
        val state = _uiState.value
        val isEditing = !state.questionId.isNullOrBlank()

        val questionToValidate = Question(
            id = state.questionId ?: "",
            testId = "bank",
            questionNumber = 1,
            subject = state.subject.trim(),
            topic = state.topic.trim(),
            questionText = state.questionText.trim(),
            options = listOf(
                state.optionA.trim(),
                state.optionB.trim(),
                state.optionC.trim(),
                state.optionD.trim()
            ),
            correctOptionIndex = state.correctOptionIndex,
            explanation = state.explanation.trim(),
            difficulty = state.difficulty,
            isActive = state.isActive
        )

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)

            val validation = repository.validateQuestion(questionToValidate, isEditing = isEditing)
            if (!validation.isValid) {
                val newFieldErrors = mutableMapOf<String, String>()
                validation.errors.forEach { err ->
                    when (err) {
                        is QuestionValidationError.BlankQuestionText -> newFieldErrors["questionText"] = "Question text is required."
                        is QuestionValidationError.BlankSubject -> newFieldErrors["subject"] = "Subject is required."
                        is QuestionValidationError.BlankTopic -> newFieldErrors["topic"] = "Topic is required."
                        is QuestionValidationError.IncompleteOption -> {
                            when (err.index) {
                                0 -> newFieldErrors["optionA"] = "Option A cannot be empty."
                                1 -> newFieldErrors["optionB"] = "Option B cannot be empty."
                                2 -> newFieldErrors["optionC"] = "Option C cannot be empty."
                                3 -> newFieldErrors["optionD"] = "Option D cannot be empty."
                            }
                        }
                        is QuestionValidationError.BlankExplanation -> newFieldErrors["explanation"] = "Explanation is required."
                        is QuestionValidationError.DuplicateQuestion -> newFieldErrors["questionText"] = "A question with identical content already exists."
                        else -> {}
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = validation.errorMessage ?: "Please fix the highlighted errors.",
                    fieldErrors = newFieldErrors
                )
                return@launch
            }

            val result = if (isEditing) {
                repository.updateQuestion(questionToValidate)
            } else {
                repository.saveQuestion(questionToValidate).map { Unit }
            }

            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    isSaveCompleted = true,
                    errorMessage = null
                )
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to save question."
                )
            }
        }
    }
}
