package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.AppSettings
import com.example.havenhub.data.UserPreferences
import com.example.havenhub.repository.SettingsRepository
import com.example.havenhub.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _userPreferences = MutableStateFlow<Resource<UserPreferences>?>(null)
    val userPreferences: StateFlow<Resource<UserPreferences>?> = _userPreferences.asStateFlow()

    private val _appSettings = MutableStateFlow<Resource<AppSettings>?>(null)
    val appSettings: StateFlow<Resource<AppSettings>?> = _appSettings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadSettings()
    }

    // ✅ Load Settings
    private fun loadSettings() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val prefsResult = settingsRepository.getUserPreferences()
                _userPreferences.value = prefsResult

                val settingsResult = settingsRepository.getAppSettings()
                _appSettings.value = settingsResult

            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ✅ Update Preferences
    fun updatePreferences(preferences: UserPreferences) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val result = settingsRepository.updateUserPreferences(preferences)
                _userPreferences.value = result
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ✅ Toggle Notifications
    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.updateNotificationSettings(enabled)
                loadSettings()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    // ✅ Toggle Dark Mode
    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.updateThemeSettings(enabled)
                loadSettings()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}