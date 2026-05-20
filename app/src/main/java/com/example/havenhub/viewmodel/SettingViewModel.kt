package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.MainActivity
import com.example.havenhub.data.AppSettings
import com.example.havenhub.data.UserPreferences
import com.example.havenhub.repository.AuthRepository
import com.example.havenhub.repository.SettingsRepository
import com.example.havenhub.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isLoading      : Boolean          = false,
    val userPreferences: UserPreferences? = null,
    val appSettings    : AppSettings?     = null,
    val errorMessage   : String?          = null,
    val actionSuccess  : Boolean          = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository    : AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        val userId = authRepository.currentUser?.uid

        if (userId.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    isLoading    = false,
                    errorMessage = "User not signed in. Please sign in and try again."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val prefsResult    = settingsRepository.getUserPreferences(userId)
                val settingsResult = settingsRepository.getAppSettings()

                val loadedPrefs = when (prefsResult) {
                    is Resource.Success -> prefsResult.data ?: UserPreferences(userId = userId)
                    else                -> UserPreferences(userId = userId)
                }

                val isNewUser = loadedPrefs.updatedAt == null
                if (isNewUser) {
                    settingsRepository.saveUserPreferences(loadedPrefs)
                }

                _uiState.update {
                    it.copy(
                        isLoading       = false,
                        userPreferences = loadedPrefs,
                        appSettings     = if (settingsResult is Resource.Success)
                            settingsResult.data else null,
                        errorMessage    = when {
                            prefsResult    is Resource.Error -> prefsResult.message
                            settingsResult is Resource.Error -> settingsResult.message
                            else                             -> null
                        }
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading       = false,
                        userPreferences = UserPreferences(userId = userId),
                        errorMessage    = e.message ?: "Failed to load settings"
                    )
                }
            }
        }
    }

    // ✅ FIXED: loadSettings() removed from success path
    fun savePreferences(preferences: UserPreferences) {
        if (preferences.userId.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Cannot save: user ID is missing.")
            }
            return
        }

        viewModelScope.launch {
            // Optimistic update — UI changes instantly
            _uiState.update {
                it.copy(userPreferences = preferences, errorMessage = null)
            }

            when (val result = settingsRepository.saveUserPreferences(preferences)) {
                is Resource.Success -> {
                    // ✅ Do NOT call loadSettings() here — it overwrites optimistic state
                    _uiState.update { it.copy(actionSuccess = true) }
                }
                is Resource.Error -> {
                    // Revert only on failure
                    _uiState.update { it.copy(errorMessage = result.message) }
                    loadSettings()
                }
                Resource.Loading -> Unit
            }
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        val userId = authRepository.currentUser?.uid
        if (userId.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "User not signed in.") }
            return
        }

        viewModelScope.launch {
            settingsRepository.setDarkMode(enabled)
            MainActivity.darkModeFlow.value = enabled

            val fields = mapOf("isDarkMode" to enabled)
            when (val result = settingsRepository.updateUserPreferences(userId, fields)) {
                is Resource.Success -> _uiState.update {
                    it.copy(
                        userPreferences = it.userPreferences?.copy(isDarkMode = enabled)
                    )
                }
                is Resource.Error -> _uiState.update { it.copy(errorMessage = result.message) }
                Resource.Loading  -> Unit
            }
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        val userId = authRepository.currentUser?.uid
        if (userId.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "User not signed in.") }
            return
        }

        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)

            val current = _uiState.value.userPreferences ?: return@launch
            val updated = if (enabled) current.withDefaultNotifications()
            else         current.withAllNotificationsDisabled()

            // Optimistic update
            _uiState.update { it.copy(userPreferences = updated) }

            val fields = mapOf(
                "notifyBookingUpdates" to updated.notifyBookingUpdates,
                "notifyMessages"       to updated.notifyMessages,
                "notifyPayments"       to updated.notifyPayments,
                "notifyPromotions"     to updated.notifyPromotions,
                "notifyAdminAlerts"    to updated.notifyAdminAlerts
            )

            when (val result = settingsRepository.updateUserPreferences(userId, fields)) {
                is Resource.Success -> Unit
                is Resource.Error   -> {
                    loadSettings()
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
                Resource.Loading -> Unit
            }
        }
    }

    fun updateNotificationChannel(channel: String, enabled: Boolean) {
        val userId = authRepository.currentUser?.uid
        if (userId.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "User not signed in.") }
            return
        }

        viewModelScope.launch {
            // Optimistic update first
            val current = _uiState.value.userPreferences ?: return@launch
            val updated = when (channel) {
                "notifyBookingUpdates" -> current.copy(notifyBookingUpdates = enabled)
                "notifyMessages"       -> current.copy(notifyMessages       = enabled)
                "notifyPayments"       -> current.copy(notifyPayments       = enabled)
                "notifyPromotions"     -> current.copy(notifyPromotions     = enabled)
                "notifyAdminAlerts"    -> current.copy(notifyAdminAlerts    = enabled)
                else                   -> current
            }
            _uiState.update { it.copy(userPreferences = updated) }

            val fields = mapOf(channel to enabled)
            when (val result = settingsRepository.updateUserPreferences(userId, fields)) {
                is Resource.Success -> Unit
                is Resource.Error   -> {
                    // Revert on failure
                    _uiState.update {
                        it.copy(
                            userPreferences = current,
                            errorMessage    = result.message
                        )
                    }
                }
                Resource.Loading -> Unit
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, actionSuccess = false) }
    }
}