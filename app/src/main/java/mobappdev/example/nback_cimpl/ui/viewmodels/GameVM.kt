package mobappdev.example.nback_cimpl.ui.viewmodels

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mobappdev.example.nback_cimpl.GameApplication
import mobappdev.example.nback_cimpl.NBackHelper
import mobappdev.example.nback_cimpl.data.UserPreferencesRepository
import java.util.*

interface GameViewModel {
    val gameState: StateFlow<GameState>
    val score: StateFlow<Int>
    val highscore: StateFlow<Int>
    val activatedPositions: StateFlow<Set<Int>>
    val nBack: Int

    fun startGame()
    fun checkAudioMatch()
    fun checkPlaceMatch()
    fun stopAudio()
    fun stopGame()
}

class GameVM(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val context: Context,
    override val nBack: Int = 1
) : ViewModel(), GameViewModel, TextToSpeech.OnInitListener {

    private val _activatedPositions = MutableStateFlow<Set<Int>>(emptySet())
    override val activatedPositions: StateFlow<Set<Int>> get() = _activatedPositions

    private val _gameState = MutableStateFlow(GameState())
    override val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _score = MutableStateFlow(0)
    override val score: StateFlow<Int> = _score

    private val _highscore = MutableStateFlow(0)
    override val highscore: StateFlow<Int> = _highscore

    private val nBackHelper = NBackHelper()
    private var job: Job? = null
    private val eventInterval: Long = 2000L
    private var positionSequence: List<Int> = listOf()
    private var letterSequence: List<Char> = listOf()

    private var textToSpeech: TextToSpeech? = null
    private var currentIndex: Int = -1
    private var readyForMatchCheck = false

    init {
        textToSpeech = TextToSpeech(context, this)
        viewModelScope.launch {
            userPreferencesRepository.highscore.collect { _highscore.value = it }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.language = Locale.US
            Log.d("GameVM", "TextToSpeech initialized successfully")
        } else {
            Log.e("GameVM", "Text-to-Speech initialization failed with status: $status")
        }
    }

    private fun playAudioLetter(letter: Char) {
        textToSpeech?.speak(letter.toString(), TextToSpeech.QUEUE_FLUSH, null, null)
        Log.d("GameVM", "Playing audio for letter: $letter")
    }

    override fun startGame() {
        stopGame() // Stop any ongoing game activities before starting a new one
        _score.value = 0
        readyForMatchCheck = false
        currentIndex = -1

        // Generate new sequences using NBackHelper for both positions and letters
        positionSequence = nBackHelper.generateNBackString(30, 9, 40, nBack).toList()
        letterSequence = generateLetterSequenceWithRepeat(30)

        Log.d("GameVM", "Generated Position Sequence: $positionSequence")
        Log.d("GameVM", "Generated Letters Sequence: $letterSequence")

        job = viewModelScope.launch {
            delay(500) // Allow time for TextToSpeech to be ready

            for (i in positionSequence.indices) {
                currentIndex = i
                val currentPosition = positionSequence[i]
                val currentLetter = letterSequence[i]

                Log.d("GameVM", "Current Index: $i, Current Position: $currentPosition, Current Letter: $currentLetter")

                // Activate the current tile
                _activatedPositions.value = setOf(currentPosition)
                _gameState.value = _gameState.value.copy(
                    currentPosition = currentPosition,
                    currentLetter = currentLetter
                )

                // Play the audio letter
                playAudioLetter(currentLetter)

                // Keep the tile activated for the event interval
                delay(eventInterval)

                // Deactivate the tile after the event interval
                _activatedPositions.value = emptySet()
                _gameState.value = _gameState.value.copy(currentPosition = -1)

                // Allow a small delay for better visual clarity
                delay(600)

                // Set ready for match check after processing enough elements
                if (i == nBack - 1) {
                    readyForMatchCheck = true
                    Log.d("GameVM", "Ready for match checks")
                }
            }
        }
    }

    private fun generateLetterSequenceWithRepeat(size: Int): List<Char> {
        val tempSequence = nBackHelper.generateNBackString(size, 26, 40, nBack).toList().map { index ->
            ('A' + (index % 26))
        }
        val random = Random()

        return tempSequence.mapIndexed { index, char ->
            if (index > 0 && random.nextFloat() < 0.5) {
                // 50% chance to repeat the previous letter
                tempSequence[index - 1]
            } else {
                char
            }
        }
    }

    override fun checkAudioMatch() {
        if (!readyForMatchCheck || currentIndex < nBack) {
            Log.d("GameVM", "Audio match check attempted before sequence was ready or not enough data.")
            _gameState.value = _gameState.value.copy(feedback = "No match!")
            return
        }

        val matchIndex = currentIndex - nBack
        val expectedLetter = letterSequence[matchIndex]
        val currentLetter = letterSequence[currentIndex]
        val audioMatch = expectedLetter == currentLetter

        Log.d("GameVM", "Checking Audio Match: Expected Letter: $expectedLetter, Current Letter: $currentLetter, Match: $audioMatch")
        updateMatchFeedback(audioMatch, isAudio = true)
    }

    override fun checkPlaceMatch() {
        if (!readyForMatchCheck || currentIndex < nBack) {
            Log.d("GameVM", "Place match check attempted before sequence was ready or not enough data.")
            _gameState.value = _gameState.value.copy(feedback = "No match!")
            return
        }

        val matchIndex = currentIndex - nBack
        val expectedPosition = positionSequence[matchIndex]
        val currentPosition = positionSequence[currentIndex]
        val visualMatch = expectedPosition == currentPosition

        Log.d("GameVM", "Checking Place Match: Expected Position: $expectedPosition, Current Position: $currentPosition, Match: $visualMatch")
        updateMatchFeedback(visualMatch, isAudio = false)
    }

    override fun stopAudio() {
        if (textToSpeech?.isSpeaking == true) {
            textToSpeech?.stop()
            Log.d("GameVM", "TextToSpeech stopped.")
        }
    }

    override fun stopGame() {
        job?.cancel() // Cancel any ongoing game actions
        stopAudio() // Stop any audio that might be playing
        _activatedPositions.value = emptySet() // Reset activated positions
        _gameState.value = _gameState.value.copy(currentPosition = -1, currentLetter = ' ', feedback = "")
        Log.d("GameVM", "Game stopped.")
    }

    private fun updateMatchFeedback(isMatch: Boolean, isAudio: Boolean) {
        val feedbackMessage = if (isMatch) {
            _score.value += 1
            val matchType = if (isAudio) "Audio" else "Place"
            "$matchType Correct match!"
        } else {
            "No match!"
        }

        // Check if the current score exceeds the high score and update it
        if (_score.value > _highscore.value) {
            _highscore.value = _score.value

            // Persist the new high score
            viewModelScope.launch {
                userPreferencesRepository.saveHighScore(_highscore.value)
            }
        }

        Log.d("GameVM", "Updating Match Feedback: $feedbackMessage")
        _gameState.value = _gameState.value.copy(feedback = feedbackMessage)
    }

    override fun onCleared() {
        super.onCleared()
        stopGame() // Ensure all ongoing tasks are stopped
        textToSpeech?.shutdown()
        Log.d("GameVM", "Game stopped and TextToSpeech shutdown.")
    }

    companion object {
        fun provideFactory(application: GameApplication): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(GameVM::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return GameVM(application.userPreferencesRepository, application) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}


data class GameState(
    val currentPosition: Int = -1,
    val currentLetter: Char = ' ',
    val feedback: String = ""
)
