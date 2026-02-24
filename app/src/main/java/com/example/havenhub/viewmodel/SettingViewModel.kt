package com.example.havenhub.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.havenhub.data.model.AppSettings
import com.havenhub.data.repository.SettingsRepository
import com.havenhub.utils.Resource
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

    private val _settings = MutableStateFlow<Resource<AppSettings>>(Resource.Idle())
    val settings: StateFlow<Resource<AppSettings>> = _settings.asStateFlow()

    private val _darkMode = MutableStateFlow(false)
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _language = MutableStateFlow("en")
    val language: StateFlow<String> = _language.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val result = settingsRepository.getSettings()
            _settings.value = result
            if (result is Resource.Success) {
                result.data?.let { s ->
                    _darkMode.value = s.darkMode
                    _notificationsEnabled.value = s.notificationsEnabled
                    _language.value = s.language
                }
            }
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        _darkMode.value = enabled
        viewModelScope.launch { settingsRepository.updateDarkMode(enabled) }
    }

    fun toggleNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        viewModelScope.launch { settingsRepository.updateNotifications(enabled) }
    }

    fun setLanguage(lang: String) {
        _language.value = lang
        viewModelScope.launch { settingsRepository.updateLanguage(lang) }
    }

    fun deleteAccount() {
        viewModelScope.launch { settingsRepository.deleteAccount() }
    }
}
