package com.example.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.TestRepository
import com.example.domain.model.TestResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class HistoryUiState(
    val isLoading: Boolean = true,
    val results: List<TestResult> = emptyList()
)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TestRepository.getInstance(application)

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getAllResults()
                .catch { e ->
                    e.printStackTrace()
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .collect { list ->
                    _uiState.value = HistoryUiState(
                        isLoading = false,
                        results = list
                    )
                }
        }
    }
}
