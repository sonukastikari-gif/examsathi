package com.example.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.QuestionRepository
import com.example.data.security.AdminAuthResult
import com.example.data.security.AdminSecurityManager
import com.example.domain.model.Question
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class QuestionStatusFilter(val displayName: String) {
    ALL("All Status"),
    ACTIVE_ONLY("Active Only"),
    INACTIVE_ONLY("Inactive Only")
}

data class AdminUiState(
    val isLoading: Boolean = true,
    val allQuestions: List<Question> = emptyList(),
    val filteredQuestions: List<Question> = emptyList(),
    val searchQuery: String = "",
    val selectedSubject: String = "All Subjects",
    val selectedTopic: String = "All Topics",
    val selectedDifficulty: String = "All Difficulties",
    val statusFilter: QuestionStatusFilter = QuestionStatusFilter.ALL,
    val availableSubjects: List<String> = emptyList(),
    val availableTopics: List<String> = emptyList(),
    val totalCount: Int = 0,
    val activeCount: Int = 0,
    val inactiveCount: Int = 0,
    val questionPendingDelete: Question? = null,
    val isAdminAuthenticated: Boolean = false,
    val isDefaultPinInUse: Boolean = false,
    val toastMessage: String? = null
)

class AdminViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = QuestionRepository.getInstance(application)
    private val securityManager = AdminSecurityManager.getInstance(application)

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        observeQuestions()
        loadSubjects()
        observeSecurityState()
    }

    private fun observeSecurityState() {
        viewModelScope.launch {
            securityManager.isSessionAuthenticated.collectLatest { isAuthenticated ->
                _uiState.value = _uiState.value.copy(
                    isAdminAuthenticated = isAuthenticated,
                    isDefaultPinInUse = securityManager.isDefaultPinInUse()
                )
            }
        }
    }

    private fun observeQuestions() {
        viewModelScope.launch {
            repository.getAllQuestionsFlow().collectLatest { questions ->
                val total = questions.size
                val active = questions.count { it.isActive }
                val inactive = total - active

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    allQuestions = questions,
                    totalCount = total,
                    activeCount = active,
                    inactiveCount = inactive
                )
                applyFilters()
            }
        }
    }

    private fun loadSubjects() {
        viewModelScope.launch {
            val subjects = repository.getDistinctSubjects()
            _uiState.value = _uiState.value.copy(
                availableSubjects = listOf("All Subjects") + subjects
            )
        }
    }

    /**
     * Authenticates the admin using salted cryptographic hash matching and rate-limiting.
     */
    fun authenticateAdmin(pin: String): Boolean {
        val result = securityManager.authenticate(pin)
        return result is AdminAuthResult.Success
    }

    fun authenticateWithResult(pin: String): AdminAuthResult {
        return securityManager.authenticate(pin)
    }

    /**
     * Securely updates the administrator PIN with salted hashing.
     */
    fun changeAdminPin(currentPin: String, newPin: String): Result<Unit> {
        val result = securityManager.changePin(currentPin, newPin)
        if (result.isSuccess) {
            _uiState.value = _uiState.value.copy(
                isDefaultPinInUse = securityManager.isDefaultPinInUse(),
                toastMessage = "Admin PIN successfully updated."
            )
        }
        return result
    }

    fun logoutAdmin() {
        securityManager.logout()
    }

    fun searchQuestions(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun filterSubject(subject: String) {
        viewModelScope.launch {
            val topics = if (subject == "All Subjects") {
                emptyList()
            } else {
                repository.getDistinctTopicsForSubject(subject)
            }
            _uiState.value = _uiState.value.copy(
                selectedSubject = subject,
                selectedTopic = "All Topics",
                availableTopics = listOf("All Topics") + topics
            )
            applyFilters()
        }
    }

    fun filterTopic(topic: String) {
        _uiState.value = _uiState.value.copy(selectedTopic = topic)
        applyFilters()
    }

    fun filterDifficulty(difficulty: String) {
        _uiState.value = _uiState.value.copy(selectedDifficulty = difficulty)
        applyFilters()
    }

    fun filterStatus(status: QuestionStatusFilter) {
        _uiState.value = _uiState.value.copy(statusFilter = status)
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        var result = state.allQuestions

        // Search query filter
        if (state.searchQuery.isNotBlank()) {
            val query = state.searchQuery.trim().lowercase()
            result = result.filter { q ->
                q.questionText.lowercase().contains(query) ||
                q.subject.lowercase().contains(query) ||
                q.topic.lowercase().contains(query) ||
                q.options.any { it.lowercase().contains(query) }
            }
        }

        // Subject filter
        if (state.selectedSubject != "All Subjects") {
            result = result.filter { it.subject.equals(state.selectedSubject, ignoreCase = true) }
        }

        // Topic filter
        if (state.selectedTopic != "All Topics") {
            result = result.filter { it.topic.equals(state.selectedTopic, ignoreCase = true) }
        }

        // Difficulty filter
        if (state.selectedDifficulty != "All Difficulties") {
            result = result.filter { it.difficulty.equals(state.selectedDifficulty, ignoreCase = true) }
        }

        // Status filter
        when (state.statusFilter) {
            QuestionStatusFilter.ACTIVE_ONLY -> result = result.filter { it.isActive }
            QuestionStatusFilter.INACTIVE_ONLY -> result = result.filter { !it.isActive }
            QuestionStatusFilter.ALL -> {} // No filtering
        }

        _uiState.value = state.copy(filteredQuestions = result)
    }

    fun toggleQuestionStatus(question: Question) {
        if (!_uiState.value.isAdminAuthenticated) {
            _uiState.value = _uiState.value.copy(toastMessage = "Admin authentication required.")
            return
        }
        viewModelScope.launch {
            val newStatus = !question.isActive
            val updateResult = repository.setQuestionActive(question.id, newStatus)
            if (updateResult.isSuccess) {
                val action = if (newStatus) "activated" else "deactivated"
                _uiState.value = _uiState.value.copy(
                    toastMessage = "Question $action successfully."
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    toastMessage = "Failed to update question status."
                )
            }
        }
    }

    fun promptDeleteQuestion(question: Question) {
        if (!_uiState.value.isAdminAuthenticated) {
            _uiState.value = _uiState.value.copy(toastMessage = "Admin authentication required.")
            return
        }
        _uiState.value = _uiState.value.copy(questionPendingDelete = question)
    }

    fun cancelDelete() {
        _uiState.value = _uiState.value.copy(questionPendingDelete = null)
    }

    fun confirmDelete() {
        if (!_uiState.value.isAdminAuthenticated) {
            _uiState.value = _uiState.value.copy(
                questionPendingDelete = null,
                toastMessage = "Admin authentication required to delete questions."
            )
            return
        }
        val target = _uiState.value.questionPendingDelete ?: return
        viewModelScope.launch {
            val deleteResult = repository.deleteQuestion(target.id)
            if (deleteResult.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    questionPendingDelete = null,
                    toastMessage = "Question deleted permanently."
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    questionPendingDelete = null,
                    toastMessage = "Failed to delete question."
                )
            }
        }
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }
}
