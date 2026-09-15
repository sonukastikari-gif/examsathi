package com.example.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AdaptivePracticeRepository
import com.example.data.repository.TestRepository
import com.example.domain.model.MockTest
import com.example.domain.model.StudentOverallStats
import com.example.domain.model.TestResult
import com.example.domain.model.WeakArea
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val featuredTests: List<MockTest> = emptyList(),
    val stats: StudentOverallStats = StudentOverallStats(),
    val recentResults: List<TestResult> = emptyList(),
    val weakAreas: List<WeakArea> = emptyList()
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val testRepository = TestRepository.getInstance(application)
    private val adaptiveRepository = AdaptivePracticeRepository.getInstance(application)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                testRepository.initializeSeedDataIfNeeded()

                // Collect tests
                testRepository.getAllMockTests()
                    .catch { e -> e.printStackTrace() }
                    .collect { tests ->
                        val stats = testRepository.getStudentStats()
                        val weak = adaptiveRepository.getWeakAreas()

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            featuredTests = tests,
                            stats = stats,
                            weakAreas = weak
                        )
                    }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}
