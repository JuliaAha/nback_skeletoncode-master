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

    fun setGameType(gameType: GameType)
    fun startGame()
    fun checkAudioMatch()
    fun checkPlaceMatch()
    fun stopAudio()
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
    private var events: MutableList<Int> = mutableListOf()
    private var lettersSequence: MutableList<Char> = mutableListOf()

    private var textToSpeech: TextToSpeech? = null
    private var readyForMatchCheck = false
    private var currentIndex: Int = -1

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
        job?.cancel()
        readyForMatchCheck = false
        currentIndex = -1

        events.clear()
        lettersSequence.clear()

        // Generate independent random positions and letters using nBackHelper
        val generatedEvents = nBackHelper.generateNBackString(30, 9, 40, nBack).toList()
        val generatedLetters = nBackHelper.generateNBackString(30, 26, 40, nBack).map { ('A' + it) }.toList()

        events.addAll(generatedEvents)
        lettersSequence.addAll(generatedLetters)

        Log.d("GameVM", "Generated Position Sequence: $events")
        Log.d("GameVM", "Generated Letters Sequence: $lettersSequence")

        job = viewModelScope.launch {
            delay(500) // Delay to allow TextToSpeech to be ready

            for (i in events.indices) {
                currentIndex = i
                val currentPosition = events[i]
                val currentLetter = lettersSequence[i]

                Log.d("GameVM", "Current Index: $i, Current Position: $currentPosition, Current Letter: $currentLetter")

                _gameState.value = _gameState.value.copy(
                    currentPosition = currentPosition,
                    currentLetter = currentLetter
                )

                _activatedPositions.value = setOf(currentPosition)
                playAudioLetter(currentLetter)

                delay(eventInterval)

                _activatedPositions.value = emptySet()
                _gameState.value = _gameState.value.copy(currentPosition = -1)

                delay(eventInterval)

                if (i >= nBack) {
                    readyForMatchCheck = true
                }
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
        val expectedLetter = lettersSequence[matchIndex]
        val currentLetter = lettersSequence[currentIndex]
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
        val expectedPosition = events[matchIndex]
        val currentPosition = events[currentIndex]
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

    private fun updateMatchFeedback(isMatch: Boolean, isAudio: Boolean) {
        val feedbackMessage = if (isMatch) {
            _score.value += 1
            val matchType = if (isAudio) "Audio" else "Place"
            "$matchType Correct match!"
        } else {
            "No match!"
        }
        Log.d("GameVM", "Updating Match Feedback: $feedbackMessage")
        _gameState.value = _gameState.value.copy(feedback = feedbackMessage)
    }

    override fun setGameType(gameType: GameType) {
        _gameState.value = _gameState.value.copy(gameType = gameType)
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeech?.shutdown()
        Log.d("GameVM", "TextToSpeech shutdown.")
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
