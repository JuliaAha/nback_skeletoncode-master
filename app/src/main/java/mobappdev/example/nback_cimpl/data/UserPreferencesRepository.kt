package mobappdev.example.nback_cimpl.data

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

data class Settings(
    val numberOfEvents: Int = 20,
    val eventInterval: Int = 1000,
    val nBackLevel: Int = 1,
    val gridSize: Int = 3,
    val audioLetterCount: Int = 5
)

class UserPreferencesRepository (
    private val dataStore: DataStore<Preferences>
) {
    private companion object {
        val HIGHSCORE = intPreferencesKey("highscore")
        val NUMBER_OF_EVENTS = intPreferencesKey("number_of_events")
        val EVENT_INTERVAL = intPreferencesKey("event_interval")
        val N_BACK_LEVEL = intPreferencesKey("n_back_level")
        val GRID_SIZE = intPreferencesKey("grid_size")  // New key for grid size
        val AUDIO_LETTER_COUNT = intPreferencesKey("audio_letter_count")  // New key for audio letters count
        const val TAG = "UserPreferencesRepo"
    }

    val highscore: Flow<Int> = dataStore.data
        .catch {
            if (it is IOException) {
                Log.e(TAG, "Error reading preferences", it)
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            preferences[HIGHSCORE] ?: 0
        }

    val settings: Flow<Settings> = dataStore.data
        .catch {
            if (it is IOException) {
                Log.e(TAG, "Error reading preferences", it)
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            Settings(
                numberOfEvents = preferences[NUMBER_OF_EVENTS] ?: 20,
                eventInterval = preferences[EVENT_INTERVAL] ?: 1000,
                nBackLevel = preferences[N_BACK_LEVEL] ?: 1,
                gridSize = preferences[GRID_SIZE] ?: 3,  // Default grid size 3x3
                audioLetterCount = preferences[AUDIO_LETTER_COUNT] ?: 5  // Default to 5 letters
            )
        }

//    suspend fun saveHighScore(score: Int) {
//        dataStore.edit { preferences ->
//            preferences[HIGHSCORE] = score
//        }
//    }

    suspend fun updateNumberOfEvents(events: Int) {
        dataStore.edit { preferences ->
            preferences[NUMBER_OF_EVENTS] = events
        }
    }

    suspend fun updateEventInterval(interval: Int) {
        dataStore.edit { preferences ->
            preferences[EVENT_INTERVAL] = interval
        }
    }

    suspend fun updateNBackLevel(level: Int) {
        dataStore.edit { preferences ->
            preferences[N_BACK_LEVEL] = level
        }
    }

    // New methods to update grid size and audio letter count
    suspend fun updateGridSize(size: Int) {
        dataStore.edit { preferences ->
            preferences[GRID_SIZE] = size
        }
    }

    suspend fun updateAudioLetterCount(count: Int) {
        dataStore.edit { preferences ->
            preferences[AUDIO_LETTER_COUNT] = count
        }
    }
}
