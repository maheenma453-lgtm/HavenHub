package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class OnboardingUiState(
    val currentPage         : Int     = 0,
    val totalPages          : Int     = 3,
    val isOnboardingComplete: Boolean = false
)

// ✅ AuthRepository hataya — zaroorat nahi thi
@HiltViewModel
class OnboardingViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun nextPage() {
        _uiState.update { state ->
            if (state.currentPage < state.totalPages - 1) {
                state.copy(currentPage = state.currentPage + 1)
            } else {
                state.copy(isOnboardingComplete = true)
            }
        }
    }

    fun previousPage() {
        _uiState.update { state ->
            if (state.currentPage > 0)
                state.copy(currentPage = state.currentPage - 1)
            else
                state
        }
    }

    fun skipOnboarding() {
        _uiState.update { it.copy(isOnboardingComplete = true) }
    }
}