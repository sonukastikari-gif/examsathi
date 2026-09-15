package com.example.ui.practice

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AdaptivePracticeRepository
import com.example.domain.model.AdaptiveConfig
import com.example.domain.model.Question
import com.example.domain.model.WeakArea
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdaptiveUiState(
    val isLoading: Boolean = true,
    val availableSubjects: List<String> = emptyList(),
    val availableTopics: List<String> = emptyList(),
    val weakAreas: List<WeakArea> = emptyList(),
    val selectedSubject: String = "All Subjects",
    val selectedTopic: String = "All Topics",
    val selectedDifficulty: String = "Adaptive",
    val selectedCount: Int = 5,
    val useAiGeneration: Boolean = false,
    // Active session state
    val sessionQuestions: List<Question> = emptyList(),
    val sessionCurrentIndex: Int = 0,
    val selectedOptionIndex: Int? = null,
    val isAnswerRevealed: Boolean = false,
    val sessionScore: Int = 0
)

class AdaptiveViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AdaptivePracticeRepository.getInstance(application)

    private val _uiState = MutableStateFlow(AdaptiveUiState())
    val uiState: StateFlow<AdaptiveUiState> = _uiState.asStateFlow()

    init {
        loadAdaptiveFoundation()
    }

    fun loadAdaptiveFoundation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val subjects = repository.getAvailableSubjects()
            val topics = repository.getTopicsForSubject("All Subjects")
            val weak = repository.getWeakAreas()

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                availableSubjects = subjects,
                availableTopics = topics,
                weakAreas = weak
            )
        }
    }

    fun selectSubject(subject: String) {
        viewModelScope.launch {
            val topics = repository.getTopicsForSubject(subject)
            _uiState.value = _uiState.value.copy(
                selectedSubject = subject,
                availableTopics = topics,
                selectedTopic = "All Topics"
            )
        }
    }

    fun selectTopic(topic: String) {
        _uiState.value = _uiState.value.copy(selectedTopic = topic)
    }

    fun selectDifficulty(diff: String) {
        _uiState.value = _uiState.value.copy(selectedDifficulty = diff)
    }

    fun selectCount(count: Int) {
        _uiState.value = _uiState.value.copy(selectedCount = count)
    }

    fun setUseAiGeneration(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(useAiGeneration = enabled)
    }

    fun startPracticeSession(subject: String, topic: String, difficulty: String, forceReload: Boolean = false) {
        if (!forceReload && _uiState.value.sessionQuestions.isNotEmpty() &&
            _uiState.value.selectedSubject == subject &&
            _uiState.value.selectedTopic == topic
        ) {
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val config = AdaptiveConfig(
                subject = subject,
                topic = topic,
                difficulty = difficulty,
                questionCount = _uiState.value.selectedCount,
                useAiGeneration = _uiState.value.useAiGeneration
            )
            val questions = repository.createPracticeSession(config)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                selectedSubject = subject,
                selectedTopic = topic,
                selectedDifficulty = difficulty,
                sessionQuestions = questions,
                sessionCurrentIndex = 0,
                selectedOptionIndex = null,
                isAnswerRevealed = false,
                sessionScore = 0
            )
        }
    }

    fun resetPracticeSession() {
        _uiState.value = _uiState.value.copy(
            sessionQuestions = emptyList(),
            sessionCurrentIndex = 0,
            selectedOptionIndex = null,
            isAnswerRevealed = false,
            sessionScore = 0
        )
    }

    fun selectPracticeOption(index: Int) {
        if (_uiState.value.isAnswerRevealed) return
        _uiState.value = _uiState.value.copy(selectedOptionIndex = index)
    }

    fun checkPracticeAnswer() {
        val current = currentPracticeQuestion ?: return
        val selected = _uiState.value.selectedOptionIndex ?: return
        val isCorrect = selected == current.correctOptionIndex
        val newScore = _uiState.value.sessionScore + (if (isCorrect) 1 else 0)

        _uiState.value = _uiState.value.copy(
            isAnswerRevealed = true,
            sessionScore = newScore
        )
    }

    fun nextPracticeQuestion() {
        val nextIdx = _uiState.value.sessionCurrentIndex + 1
        _uiState.value = _uiState.value.copy(
            sessionCurrentIndex = nextIdx,
            selectedOptionIndex = null,
            isAnswerRevealed = false
        )
    }

    val currentPracticeQuestion: Question?
        get() {
            val qs = _uiState.value.sessionQuestions
            val idx = _uiState.value.sessionCurrentIndex
            return if (idx in qs.indices) qs[idx] else null
        }
}
