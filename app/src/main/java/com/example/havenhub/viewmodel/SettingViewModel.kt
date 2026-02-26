package com.example.havenhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.havenhub.data.AppSettings
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

    private val _settings = MutableStateFlow<Resource<AppSettings>>(Resource.Loading)
    val settings: StateFlow<Resource<AppSettings>> = _settings.asStateFlow()

    // FIX: load from local SharedPreferences via repository (non-suspend functions)
    private val _darkMode = MutableStateFlow(settingsRepository.isDarkMode())
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(settingsRepository.areNotificationsEnabled())
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _language = MutableStateFlow(settingsRepository.getLanguage())
    val language: StateFlow<String> = _language.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            // FIX: getSettings() → getAppSettings()
            _settings.value = Resource.Loading
            _settings.value = settingsRepository.getAppSettings()
        }
    }

    // FIX: updateDarkMode() → setDarkMode() (non-suspend, local SharedPreferences)
    fun toggleDarkMode(enabled: Boolean) {
        _darkMode.value = enabled
        settingsRepository.setDarkMode(enabled)
    }

    // FIX: updateNotifications() → setNotificationsEnabled()
    fun toggleNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        settingsRepository.setNotificationsEnabled(enabled)
    }

    // FIX: updateLanguage() → setLanguage()
    fun setLanguage(lang: String) {
        _language.value = lang
        settingsRepository.setLanguage(lang)
    }

    // FIX: deleteAccount() doesn't exist in SettingsRepository — removed
    // Add this to AuthRepository if needed
}