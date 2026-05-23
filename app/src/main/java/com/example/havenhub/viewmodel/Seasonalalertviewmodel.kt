package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.SeasonalAlert
import com.example.havenhub.repository.SeasonalAlertRepository
import com.example.havenhub.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// SeasonalAlertViewModel.kt
//
// Manages seasonal alert state for:
//   1. NotificationsScreen  — shows seasonal alerts section at the top
//   2. HomeScreen           — optional banner/card for active alerts
//   3. Admin screens        — create/manage alerts
//
// Flow:
//   loadAlertsForRole(userRole) → repository.observeActiveAlerts(role)
//   → UI collects uiState.alerts and displays them
// ─────────────────────────────────────────────────────────────────────────────

// ── UI State ──────────────────────────────────────────────────────────────────
data class SeasonalAlertUiState(
    val alerts        : List<SeasonalAlert> = emptyList(),  // Active alerts for current user
    val allAlerts     : List<SeasonalAlert> = emptyList(),  // Admin: all alerts
    val isLoading     : Boolean             = false,
    val errorMessage  : String?             = null,
    val successMessage: String?             = null
)

@HiltViewModel
class SeasonalAlertViewModel @Inject constructor(
    private val repository: SeasonalAlertRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeasonalAlertUiState())
    val uiState: StateFlow<SeasonalAlertUiState> = _uiState.asStateFlow()

    // ─────────────────────────────────────────────────────────────────────────
    // loadAlertsForRole
    //
    // Called from NotificationsScreen / HomeScreen when user role is known.
    // Starts a real-time Firestore listener so alerts update automatically
    // when Admin adds/removes alerts — no manual refresh needed.
    //
    // @param userRole  "landlord" | "tenant" | "admin"
    // ─────────────────────────────────────────────────────────────────────────
    fun loadAlertsForRole(userRole: String) {
        // Admin sees all alerts in their dashboard, not the user-facing ones
        if (userRole == "admin" || userRole == "sub_admin") return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Collect the real-time Flow from repository
                repository.observeActiveAlerts(userRole).collect { alerts ->
                    _uiState.update {
                        it.copy(
                            alerts    = alerts,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading    = false,
                        errorMessage = e.localizedMessage ?: "Failed to load seasonal alerts"
                    )
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // loadAllAlertsForAdmin
    //
    // Admin-only: fetches ALL alerts (active + inactive) for the
    // manage alerts screen / admin dashboard.
    // ─────────────────────────────────────────────────────────────────────────
    fun loadAllAlertsForAdmin() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = repository.getAllAlerts()) {
                is Resource.Success -> _uiState.update {
                    it.copy(allAlerts = result.data, isLoading = false)
                }
                is Resource.Error   -> _uiState.update {
                    it.copy(errorMessage = result.message, isLoading = false)
                }
                is Resource.Loading -> { /* handled by isLoading flag above */ }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createAlert — Admin creates a new seasonal alert
    // ─────────────────────────────────────────────────────────────────────────
    fun createAlert(alert: SeasonalAlert) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = repository.createAlert(alert)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading      = false,
                            successMessage = "Seasonal alert created successfully!"
                        )
                    }
                    loadAllAlertsForAdmin() // Refresh the admin list
                }
                is Resource.Error   -> _uiState.update {
                    it.copy(errorMessage = result.message, isLoading = false)
                }
                is Resource.Loading -> { }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // toggleAlertActive — Admin enables/disables an alert quickly
    // ─────────────────────────────────────────────────────────────────────────
    fun toggleAlertActive(alertId: String, isActive: Boolean) {
        viewModelScope.launch {
            when (val result = repository.toggleAlertActive(alertId, isActive)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            successMessage = if (isActive) "Alert activated" else "Alert deactivated"
                        )
                    }
                    loadAllAlertsForAdmin()
                }
                is Resource.Error   -> _uiState.update { it.copy(errorMessage = result.message) }
                is Resource.Loading -> { }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteAlert — Admin permanently deletes an alert
    // ─────────────────────────────────────────────────────────────────────────
    fun deleteAlert(alertId: String) {
        viewModelScope.launch {
            when (val result = repository.deleteAlert(alertId)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(successMessage = "Alert deleted successfully")
                    }
                    loadAllAlertsForAdmin()
                }
                is Resource.Error   -> _uiState.update { it.copy(errorMessage = result.message) }
                is Resource.Loading -> { }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // clearMessages — called after Snackbar shows to reset messages
    // ─────────────────────────────────────────────────────────────────────────
    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
