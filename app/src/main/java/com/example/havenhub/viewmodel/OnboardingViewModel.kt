package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// FIX: com.havenhub → com.example.havenhub
import com.example.havenhub.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _onboardingComplete = MutableStateFlow(false)
    val onboardingComplete: StateFlow<Boolean> = _onboardingComplete.asStateFlow()

    val totalPages = 3

    fun nextPage() {
        if (_currentPage.value < totalPages - 1) {
            _currentPage.value++
        } else {
            completeOnboarding()
        }
    }

    fun previousPage() {
        if (_currentPage.value > 0) {
            _currentPage.value--
        }
    }

    fun skipOnboarding() {
        completeOnboarding()
    }

    private fun completeOnboarding() {
        // TODO: setOnboardingComplete() does not exist in AuthRepository
        // Option A: Add it to AuthRepository (uses SharedPreferences or Firestore flag)
        // Option B: Remove this call and handle navigation purely from _onboardingComplete state
        _onboardingComplete.value = true
    }
}