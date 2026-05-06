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
        val userId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val prefsResult    = settingsRepository.getUserPreferences(userId)
                val settingsResult = settingsRepository.getAppSettings()
                _uiState.update {
                    it.copy(
                        isLoading       = false,
                        userPreferences = if (prefsResult    is Resource.Success) prefsResult.data    else null,
                        appSettings     = if (settingsResult is Resource.Success) settingsResult.data else null,
                        errorMessage    = if (prefsResult    is Resource.Error)   prefsResult.message
                        else if (settingsResult is Resource.Error) settingsResult.message
                        else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load settings")
                }
            }
        }
    }

    fun savePreferences(preferences: UserPreferences) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = settingsRepository.saveUserPreferences(preferences)) {
                is Resource.Success -> _uiState.update {
                    it.copy(isLoading = false, userPreferences = preferences, actionSuccess = true)
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                Resource.Loading -> Unit
            }
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        val userId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            // STEP 1: SharedPreferences mein save karo (local, instant)
            settingsRepository.setDarkMode(enabled)

            // STEP 2: MainActivity ka StateFlow update karo
            // Yahi asal fix hai — theme tabhi change hogi jab yeh line chalegi
            // Pehle yeh line nahi thi isliye toggle kaam nahi karta tha
            MainActivity.darkModeFlow.value = enabled

            // STEP 3: Firestore mein bhi save karo (cross-device sync)
            val fields = mapOf("isDarkMode" to enabled)
            when (val result = settingsRepository.updateUserPreferences(userId, fields)) {
                is Resource.Success -> _uiState.update {
                    it.copy(userPreferences = it.userPreferences?.copy(isDarkMode = enabled))
                }
                is Resource.Error -> _uiState.update {
                    it.copy(errorMessage = result.message)
                }
                Resource.Loading -> Unit
            }
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        val userId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
            val current = _uiState.value.userPreferences ?: return@launch
            val updated = if (enabled) current.withDefaultNotifications()
            else current.withAllNotificationsDisabled()
            val fields = mapOf(
                "notifyBookingUpdates" to updated.notifyBookingUpdates,
                "notifyMessages"       to updated.notifyMessages,
                "notifyPayments"       to updated.notifyPayments,
                "notifyPromotions"     to updated.notifyPromotions,
                "notifyAdminAlerts"    to updated.notifyAdminAlerts
            )
            when (val result = settingsRepository.updateUserPreferences(userId, fields)) {
                is Resource.Success -> _uiState.update { it.copy(userPreferences = updated) }
                is Resource.Error   -> _uiState.update { it.copy(errorMessage = result.message) }
                Resource.Loading    -> Unit
            }
        }
    }

    fun updateNotificationChannel(channel: String, enabled: Boolean) {
        val userId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            val fields = mapOf(channel to enabled)
            when (val result = settingsRepository.updateUserPreferences(userId, fields)) {
                is Resource.Success -> {
                    val current = _uiState.value.userPreferences ?: return@launch
                    val updated = when (channel) {
                        "notifyBookingUpdates" -> current.copy(notifyBookingUpdates = enabled)
                        "notifyMessages"       -> current.copy(notifyMessages       = enabled)
                        "notifyPayments"       -> current.copy(notifyPayments       = enabled)
                        "notifyPromotions"     -> current.copy(notifyPromotions     = enabled)
                        "notifyAdminAlerts"    -> current.copy(notifyAdminAlerts    = enabled)
                        else -> current
                    }
                    _uiState.update { it.copy(userPreferences = updated) }
                }
                is Resource.Error -> _uiState.update { it.copy(errorMessage = result.message) }
                Resource.Loading  -> Unit
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, actionSuccess = false) }
    }
}