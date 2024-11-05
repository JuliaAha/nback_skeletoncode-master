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
import mobappdev.example.nback_cimpl.data.UserPreferencesRepository
import java.util.*

interface GameViewModel {
    val visualSequence: StateFlow<List<Int>>
    val gameState: StateFlow<GameState>
    val score: StateFlow<Int>
    val highscore: StateFlow<Int>
    val nBack: Int

    fun setGameType(gameType: GameType)
    fun startGame()
    fun checkAudioMatch()
    fun checkPlaceMatch()
}

class GameVM(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val context: Context,
    override val nBack: Int = 2
) : GameViewModel, ViewModel(), TextToSpeech.OnInitListener {

    private val _gameState = MutableStateFlow(GameState())
    override val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _score = MutableStateFlow(0)
    override val score: StateFlow<Int> = _score

    private val _highscore = MutableStateFlow(0)
    override val highscore: StateFlow<Int> = _highscore

    private val _visualSequence = MutableStateFlow<List<Int>>(emptyList())
    override val visualSequence: StateFlow<List<Int>> = _visualSequence.asStateFlow()

    private val _audioSequence = MutableStateFlow<List<Char>>(emptyList())
    private var job: Job? = null
    private val eventInterval: Long = 2000L

    private var textToSpeech: TextToSpeech? = null
    private var readyForMatchCheck = false // Flag to ensure stable match checking

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

    override fun startGame() {
        job?.cancel()
        readyForMatchCheck = false // Disable matching during sequence update
        val visualSequence = generatePositionSequence(30)
        val audioSequence = generateLetterSequence(30)

        _visualSequence.value = visualSequence
        _audioSequence.value = audioSequence
        Log.d("GameVM", "Visual sequence: $visualSequence")
        Log.d("GameVM", "Audio sequence: $audioSequence")

        job = viewModelScope.launch {
            for (i in visualSequence.indices) {
                if (textToSpeech == null) {
                    Log.e("GameVM", "TextToSpeech is not initialized.")
                    return@launch
                }
                _gameState.value = _gameState.value.copy(
                    currentPosition = visualSequence[i],
                    currentLetter = audioSequence[i]
                )
                playAudioLetter(audioSequence[i])
                delay(eventInterval)
            }
            readyForMatchCheck = true // Enable match checking after sequences are stable
        }
    }

    private fun generatePositionSequence(size: Int): List<Int> {
        val positions = mutableListOf<Int>()
        for (i in 0 until size) {
            if (i >= nBack && (0..1).random() == 0) {  // 50% chance of repeating the n-back position
                positions.add(positions[i - nBack])
            } else {
                positions.add((0..8).random())
            }
        }
        return positions
    }

    private fun generateLetterSequence(size: Int): List<Char> {
        val alphabet = ('A'..'Z').toList()
        val letters = mutableListOf<Char>()
        for (i in 0 until size) {
            if (i >= nBack && (0..1).random() == 0) {  // 50% chance of repeating the n-back letter
                letters.add(letters[i - nBack])
            } else {
                letters.add(alphabet.random())
            }
        }
        return letters
    }

    private fun playAudioLetter(letter: Char) {
        textToSpeech?.speak(letter.toString(), TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun checkAudioMatch() {
        if (!readyForMatchCheck) {
            Log.d("GameVM", "Audio match check attempted before sequence was ready.")
            _gameState.value = _gameState.value.copy(feedback = "No match!")
            return
        }

        val currentPos = _audioSequence.value.size - 1
        val matchPos = currentPos - nBack
        val audioMatch = matchPos >= 0 && _audioSequence.value.getOrNull(matchPos) == _audioSequence.value.getOrNull(currentPos)

        Log.d("GameVM", "Audio Match: $audioMatch at positions $matchPos and $currentPos")
        updateMatchFeedback(audioMatch, isAudio = true)
    }

    override fun checkPlaceMatch() {
        if (!readyForMatchCheck) {
            Log.d("GameVM", "Place match check attempted before sequence was ready.")
            _gameState.value = _gameState.value.copy(feedback = "No match!")
            return
        }

        val currentPos = _visualSequence.value.size - 1
        val matchPos = currentPos - nBack
        val visualMatch = matchPos >= 0 && _visualSequence.value.getOrNull(matchPos) == _visualSequence.value.getOrNull(currentPos)

        Log.d("GameVM", "Visual Match: $visualMatch at positions $matchPos and $currentPos")
        updateMatchFeedback(visualMatch, isAudio = false)
    }

    private fun updateMatchFeedback(isMatch: Boolean, isAudio: Boolean) {
        val feedbackMessage = if (isMatch) {
            _score.value += 1
            val matchType = if (isAudio) "Audio" else "Place"
            "$matchType Correct match!"
        } else {
            "No match!"
        }
        _gameState.value = _gameState.value.copy(feedback = feedbackMessage)
    }

    override fun setGameType(gameType: GameType) {
        _gameState.value = _gameState.value.copy(gameType = gameType)
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeech?.shutdown()
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

enum class GameType { Audio, Visual, AudioVisual }

data class GameState(
    val gameType: GameType = GameType.Visual,
    val currentPosition: Int = -1,
    val currentLetter: Char = ' ',
    val feedback: String = ""
)