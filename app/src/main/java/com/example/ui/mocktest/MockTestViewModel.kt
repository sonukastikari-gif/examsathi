package com.example.ui.mocktest

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.TestRepository
import com.example.domain.model.MockTest
import com.example.domain.model.Question
import com.example.domain.model.StudentResponse
import com.example.domain.model.TestResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ActiveTestUiState(
    val isLoading: Boolean = true,
    val test: MockTest? = null,
    val questions: List<Question> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val responses: Map<String, StudentResponse> = emptyMap(),
    val remainingSeconds: Int = 0,
    val totalSeconds: Int = 0,
    val isTimerRunning: Boolean = false,
    val showSubmitDialog: Boolean = false,
    val showPaletteSheet: Boolean = false,
    val isSubmitting: Boolean = false,
    val isTimeUpAutoSubmitted: Boolean = false
)

sealed class ActiveTestEvent {
    data class TestSubmitted(val resultId: String) : ActiveTestEvent()
    data class ShowToast(val message: String) : ActiveTestEvent()
}

class MockTestViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TestRepository.getInstance(application)

    private val _uiState = MutableStateFlow(ActiveTestUiState())
    val uiState: StateFlow<ActiveTestUiState> = _uiState.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _events = MutableSharedFlow<ActiveTestEvent>()
    val events: SharedFlow<ActiveTestEvent> = _events.asSharedFlow()

    private var timerJob: Job? = null

    fun loadTest(testId: String, forceReload: Boolean = false) {
        if (!forceReload && _uiState.value.test?.id == testId && _uiState.value.questions.isNotEmpty()) {
            if ((timerJob == null || timerJob?.isActive != true) && _uiState.value.isTimerRunning && !_uiState.value.isSubmitting) {
                startTimer()
            }
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val mockTest = repository.getMockTestById(testId)
                val questions = repository.getQuestionsForTest(testId)

                if (mockTest != null && questions.isNotEmpty()) {
                    val durationSecs = mockTest.durationMinutes * 60
                    val initialResponses = questions.associate { q ->
                        q.id to StudentResponse(questionId = q.id)
                    }

                    _remainingSeconds.value = durationSecs
                    _uiState.value = ActiveTestUiState(
                        isLoading = false,
                        test = mockTest,
                        questions = questions,
                        currentQuestionIndex = 0,
                        responses = initialResponses,
                        remainingSeconds = durationSecs,
                        totalSeconds = durationSecs,
                        isTimerRunning = true
                    )

                    startTimer()
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _events.emit(ActiveTestEvent.ShowToast("Test not found or no questions available."))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_remainingSeconds.value > 0 && _uiState.value.isTimerRunning) {
                delay(1000L)
                val newRemaining = _remainingSeconds.value - 1
                _remainingSeconds.value = newRemaining

                if (newRemaining <= 0) {
                    _uiState.value = _uiState.value.copy(remainingSeconds = 0)
                    // Auto-submit when timer reaches zero!
                    handleAutoSubmit()
                    break
                }
            }
        }
    }

    private fun handleAutoSubmit() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isTimerRunning = false,
                isTimeUpAutoSubmitted = true,
                showSubmitDialog = false,
                showPaletteSheet = false
            )
            _events.emit(ActiveTestEvent.ShowToast("Time's up! Automatically submitting your test..."))
            submitCurrentTest()
        }
    }

    fun selectOption(optionIndex: Int) {
        val currentQ = currentQuestion ?: return
        val currentResp = _uiState.value.responses[currentQ.id] ?: StudentResponse(questionId = currentQ.id)
        val updated = currentResp.copy(selectedOptionIndex = optionIndex)

        val updatedMap = _uiState.value.responses.toMutableMap()
        updatedMap[currentQ.id] = updated
        _uiState.value = _uiState.value.copy(responses = updatedMap)
    }

    fun clearResponse() {
        val currentQ = currentQuestion ?: return
        val currentResp = _uiState.value.responses[currentQ.id] ?: StudentResponse(questionId = currentQ.id)
        val updated = currentResp.copy(selectedOptionIndex = null)

        val updatedMap = _uiState.value.responses.toMutableMap()
        updatedMap[currentQ.id] = updated
        _uiState.value = _uiState.value.copy(responses = updatedMap)
    }

    fun toggleMarkForReview() {
        val currentQ = currentQuestion ?: return
        val currentResp = _uiState.value.responses[currentQ.id] ?: StudentResponse(questionId = currentQ.id)
        val updated = currentResp.copy(isMarkedForReview = !currentResp.isMarkedForReview)

        val updatedMap = _uiState.value.responses.toMutableMap()
        updatedMap[currentQ.id] = updated
        _uiState.value = _uiState.value.copy(responses = updatedMap)
    }

    fun navigateToNext() {
        val currentIndex = _uiState.value.currentQuestionIndex
        if (currentIndex < _uiState.value.questions.size - 1) {
            _uiState.value = _uiState.value.copy(currentQuestionIndex = currentIndex + 1)
        }
    }

    fun navigateToPrevious() {
        val currentIndex = _uiState.value.currentQuestionIndex
        if (currentIndex > 0) {
            _uiState.value = _uiState.value.copy(currentQuestionIndex = currentIndex - 1)
        }
    }

    fun jumpToQuestion(index: Int) {
        if (index in 0 until _uiState.value.questions.size) {
            _uiState.value = _uiState.value.copy(currentQuestionIndex = index)
        }
    }

    fun showSubmitConfirmation(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSubmitDialog = show)
    }

    fun showPalette(show: Boolean) {
        _uiState.value = _uiState.value.copy(showPaletteSheet = show)
    }

    fun submitCurrentTest() {
        val test = _uiState.value.test ?: return
        if (_uiState.value.isSubmitting) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, isTimerRunning = false)
            timerJob?.cancel()

            val timeSpentSecs = _uiState.value.totalSeconds - _remainingSeconds.value
            val result = repository.submitTest(
                test = test,
                responses = _uiState.value.responses,
                timeTakenSeconds = if (timeSpentSecs > 0) timeSpentSecs else 1
            )

            _uiState.value = _uiState.value.copy(isSubmitting = false, showSubmitDialog = false)
            _events.emit(ActiveTestEvent.TestSubmitted(result.id))
        }
    }

    val currentQuestion: Question?
        get() {
            val questions = _uiState.value.questions
            val index = _uiState.value.currentQuestionIndex
            return if (index in questions.indices) questions[index] else null
        }

    val answeredCount: Int
        get() = _uiState.value.responses.values.count { it.selectedOptionIndex != null }

    val unansweredCount: Int
        get() = _uiState.value.questions.size - answeredCount

    val reviewCount: Int
        get() = _uiState.value.responses.values.count { it.isMarkedForReview }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
