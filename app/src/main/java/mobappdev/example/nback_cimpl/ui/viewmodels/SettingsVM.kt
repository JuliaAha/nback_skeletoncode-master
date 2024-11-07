package mobappdev.example.nback_cimpl.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mobappdev.example.nback_cimpl.GameApplication
import mobappdev.example.nback_cimpl.data.Settings
import mobappdev.example.nback_cimpl.data.UserPreferencesRepository

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    // MutableStateFlow to hold settings and make it observable
    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings

    init {
        // Collect settings from the repository into the state flow
        viewModelScope.launch {
            userPreferencesRepository.settings.collect {
                _settings.value = it
            }
        }
    }

    fun setNumberOfEvents(events: Int) {
        viewModelScope.launch {
            userPreferencesRepository.updateNumberOfEvents(events)
        }
    }

    fun setEventInterval(interval: Int) {
        viewModelScope.launch {
            userPreferencesRepository.updateEventInterval(interval)
        }
    }

    fun setNBackLevel(level: Int) {
        viewModelScope.launch {
            userPreferencesRepository.updateNBackLevel(level)
        }
    }

    fun setGridSize(size: Int) {
        viewModelScope.launch {
            userPreferencesRepository.updateGridSize(size)
        }
    }

    fun setAudioLetterCount(count: Int) {
        viewModelScope.launch {
            userPreferencesRepository.updateAudioLetterCount(count)
        }
    }

    // Factory for instantiating SettingsViewModel
    companion object {
        fun provideFactory(application: GameApplication): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return SettingsViewModel(application.userPreferencesRepository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}
