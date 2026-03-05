package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ✅ Onboarding ke liye UI State define ki
data class OnboardingUiState(
    val currentPage: Int = 0,
    val totalPages: Int = 3,
    val isOnboardingComplete: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun nextPage() {
        _uiState.update { state ->
            if (state.currentPage < state.totalPages - 1) {
                state.copy(currentPage = state.currentPage + 1)
            } else {
                // Last page par next dabane se onboarding khatam
                state.copy(isOnboardingComplete = true)
            }
        }
    }

    fun previousPage() {
        _uiState.update { state ->
            if (state.currentPage > 0) {
                state.copy(currentPage = state.currentPage - 1)
            } else {
                state
            }
        }
    }

    fun skipOnboarding() {
        _uiState.update { it.copy(isOnboardingComplete = true) }
    }

    // Agar future mein repo mein flag save karna ho
    private fun completeOnboarding() {
        viewModelScope.launch {
            // authRepository.saveOnboardingStatus(true) // Future use ke liye
            _uiState.update { it.copy(isOnboardingComplete = true) }
        }
    }
}