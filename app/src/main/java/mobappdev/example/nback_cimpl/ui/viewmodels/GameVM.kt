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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mobappdev.example.nback_cimpl.GameApplication
import mobappdev.example.nback_cimpl.NBackHelper
import mobappdev.example.nback_cimpl.data.UserPreferencesRepository
import java.util.Locale
import java.util.Random

interface GameViewModel {
    val gameState: StateFlow<GameState>
    val score: StateFlow<Int>
    val highscore: StateFlow<Int>
    val activatedPositions: StateFlow<Set<Int>>
    val gameSettings: StateFlow<GameSettings>  // Expose game settings to observe
    val nBack: Int
    val currentIndex: StateFlow<Int>

    fun startGame()
    fun checkAudioMatch()
    fun checkPlaceMatch()
    fun stopAudio()
    fun stopGame()
    fun setGameType(gameType: GameType)
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
    override val score: StateFlow<Int> get() = _score.asStateFlow()

    private val _highscore = MutableStateFlow(0)
    override val highscore: StateFlow<Int> get() = _highscore.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    override val currentIndex: StateFlow<Int> get() = _currentIndex.asStateFlow()

    private val _gameSettings = MutableStateFlow(GameSettings())
    override val gameSettings: StateFlow<GameSettings> get() = _gameSettings.asStateFlow()

    private val nBackHelper = NBackHelper()
    private var job: Job? = null
    private var positionSequence: List<Int> = listOf()
    private var letterSequence: List<Char> = listOf()
    private var textToSpeech: TextToSpeech? = null

    private val matchCheckedPositions = mutableSetOf<Int>()
    private val matchCheckedAudio = mutableSetOf<Int>()

    init {
        textToSpeech = TextToSpeech(context, this)
        viewModelScope.launch {
            // Load initial settings from the repository
            userPreferencesRepository.settings.collect { settings ->
            _gameSettings.value = GameSettings(
                stimuliType = _gameState.value.gameType,
                nValue = settings.nBackLevel,
                eventInterval = settings.eventInterval,
                numberOfEvents = settings.numberOfEvents
            )
        }
    }
        viewModelScope.launch {
            // Collect highscore to update in real-time
            userPreferencesRepository.highscore.collect { highScore ->
                _highscore.value = highScore
            }
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

    override fun setGameType(gameType: GameType) {
        _gameSettings.value = _gameSettings.value.copy(stimuliType = gameType)
        _gameState.value = _gameState.value.copy(gameType = gameType)
    }

    private fun playAudioLetter(letter: Char) {
        textToSpeech?.let {
            if (it.isSpeaking) it.stop()
            it.speak(letter.toString(), TextToSpeech.QUEUE_FLUSH, null, null)
            Log.d("GameVM", "Playing audio for letter: $letter")
        }
    }

    override fun startGame() {
        stopGame()
        _score.value = 0
        matchCheckedPositions.clear()
        matchCheckedAudio.clear()
        _currentIndex.value = -1

        viewModelScope.launch {
            val settings = userPreferencesRepository.settings.first()
            if (settings.numberOfEvents <= 0 || settings.nBackLevel <= 0) {
                Log.e("GameVM", "Invalid settings: Number of Events = ${settings.numberOfEvents}, N-Back Level = ${settings.nBackLevel}")
                return@launch
            }

            _gameState.value = _gameState.value.copy(gridSize = settings.gridSize)
            _gameSettings.value = _gameSettings.value.copy(
                nValue = settings.nBackLevel,
                eventInterval = settings.eventInterval,
                numberOfEvents = settings.numberOfEvents
            )

            val gridRange = settings.gridSize * settings.gridSize
            positionSequence = nBackHelper.generateNBackString(settings.numberOfEvents, gridRange, 40, settings.nBackLevel).toList()
            letterSequence = generateLimitedLetterSequence(settings.numberOfEvents, settings.audioLetterCount)

            job = viewModelScope.launch {
                if (!waitForTTSReady()) return@launch

                for (i in positionSequence.indices) {
                    _currentIndex.value = i
                    val currentPosition = positionSequence[i]
                    val currentLetter = letterSequence[i]

                    Log.d("GameVM", "Activating tile at position $currentPosition with letter $currentLetter")

                    // Deactivate the current position (turn off)
                    _activatedPositions.value = emptySet()
                    delay(100)

                    _activatedPositions.value = setOf(currentPosition)
                    _gameState.value = _gameState.value.copy(
                        currentPosition = currentPosition,
                        currentLetter = currentLetter
                    )
                    delay(50)

                    if (gameSettings.value.stimuliType == GameType.AudioVisual || gameSettings.value.stimuliType == GameType.Audio) {
                        playAudioLetter(currentLetter)
                    }

                    delay(settings.eventInterval.toLong())

                    _activatedPositions.value = emptySet()
                    Log.d("GameVM", "Deactivating tile at position $currentPosition")
                    delay(100)
                }
            }
        }
    }

    private suspend fun waitForTTSReady(): Boolean {
        repeat(10) {
            if (textToSpeech != null && textToSpeech?.isSpeaking == false) return true
            delay(100)
        }
        Log.e("GameVM", "TextToSpeech not ready, cancelling game start")
        return false
    }

    private fun generateLimitedLetterSequence(size: Int, maxUniqueLetters: Int): List<Char> {
        val letterPool = ('A' until 'A' + maxUniqueLetters).toList()
        val random = Random()
        return List(size) {
            letterPool[random.nextInt(letterPool.size)]
        }
    }

    override fun checkAudioMatch() {
        if (currentIndex.value >= nBack) {
            val matchIndex = currentIndex.value - nBack
            if (letterSequence[matchIndex] == letterSequence[currentIndex.value] && matchCheckedAudio.add(currentIndex.value)) {
                _score.value += 1
                Log.d("GameVM", "Audio Match confirmed at index ${currentIndex.value}, score incremented.")
                _gameState.value = _gameState.value.copy(feedback = "Audio Correct match!")
            } else {
                _gameState.value = _gameState.value.copy(feedback = "No match!")
            }
        }
    }

    override fun checkPlaceMatch() {
        if (currentIndex.value >= nBack) {
            val matchIndex = currentIndex.value - nBack
            if (positionSequence[matchIndex] == positionSequence[currentIndex.value] && matchCheckedPositions.add(currentIndex.value)) {
                _score.value += 1
                Log.d("GameVM", "Place Match confirmed at index ${currentIndex.value}, score incremented.")
                _gameState.value = _gameState.value.copy(feedback = "Place Correct match!")
            } else {
                _gameState.value = _gameState.value.copy(feedback = "No match!")
            }
        }
    }

    override fun stopAudio() {
        textToSpeech?.let {
            if (it.isSpeaking) it.stop()
            Log.d("GameVM", "TextToSpeech stopped.")
        }
    }

    override fun stopGame() {
        job?.cancel()
        stopAudio()
        _activatedPositions.value = emptySet()
        _gameState.value = _gameState.value.copy(currentPosition = -1, currentLetter = ' ', feedback = "")
        Log.d("GameVM", "Game stopped.")
    }

    override fun onCleared() {
        super.onCleared()
        stopGame()
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

enum class GameType {
    Audio,
    Visual,
    AudioVisual
}

data class GameSettings(
    val stimuliType: GameType = GameType.AudioVisual,
    val nValue: Int = 1,
    val eventInterval: Int = 1000,
    val numberOfEvents: Int = 10
)

data class GameState(
    val currentPosition: Int = -1,
    val currentLetter: Char = ' ',
    val feedback: String = "",
    val gridSize: Int = 3,
    val gameType: GameType = GameType.AudioVisual
)




//package mobappdev.example.nback_cimpl.ui.viewmodels
//
//import android.content.Context
//import android.speech.tts.TextToSpeech
//import android.util.Log
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.viewModelScope
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.flow.first
//import kotlinx.coroutines.launch
//import mobappdev.example.nback_cimpl.GameApplication
//import mobappdev.example.nback_cimpl.NBackHelper
//import mobappdev.example.nback_cimpl.data.UserPreferencesRepository
//import java.util.Locale
//import java.util.Random
//
//interface GameViewModel {
//    val gameState: StateFlow<GameState>
//    val score: StateFlow<Int>
//    val highscore: StateFlow<Int>
//    val activatedPositions: StateFlow<Set<Int>>
//    val nBack: Int
//    val currentIndex: StateFlow<Int>
//
//    fun startGame()
//    fun checkAudioMatch()
//    fun checkPlaceMatch()
//    fun stopAudio()
//    fun stopGame()
//    fun setGameType(gameType: GameType)
//    fun gameSettings()
//}
//
//class GameVM(
//    private val userPreferencesRepository: UserPreferencesRepository,
//    private val context: Context,
//    override val nBack: Int = 1
//) : ViewModel(), GameViewModel, TextToSpeech.OnInitListener {
//
//    private val _activatedPositions = MutableStateFlow<Set<Int>>(emptySet())
//    override val activatedPositions: StateFlow<Set<Int>> get() = _activatedPositions
//
//    private val _gameState = MutableStateFlow(GameState())
//    override val gameState: StateFlow<GameState> = _gameState.asStateFlow()
//
//    private val _score = MutableStateFlow(0)
//    override val score: StateFlow<Int> get() = _score.asStateFlow()
//
//    private val _highscore = MutableStateFlow(0)
//    override val highscore: StateFlow<Int> get() = _highscore.asStateFlow()
//
//    private val _currentIndex = MutableStateFlow(-1)
//    override val currentIndex: StateFlow<Int> get() = _currentIndex.asStateFlow()
//
//    private val nBackHelper = NBackHelper()
//    private var job: Job? = null
//    private var positionSequence: List<Int> = listOf()
//    private var letterSequence: List<Char> = listOf()
//    private var textToSpeech: TextToSpeech? = null
//
//    private val _gameSettings = MutableStateFlow(GameSettings())
//    val gameSettings: StateFlow<GameSettings> = _gameSettings.asStateFlow()
//
//
//    private val matchCheckedPositions = mutableSetOf<Int>()
//    private val matchCheckedAudio = mutableSetOf<Int>()
//
//    override fun setGameType(gameType: GameType) {
//        _gameState.value = _gameState.value.copy(gameType = gameType)
//    }
//
//    init {
//        textToSpeech = TextToSpeech(context, this)
//        viewModelScope.launch {
//            val settings = userPreferencesRepository.settings.first()
//            _gameSettings.value = GameSettings(
//                stimuliType = _gameSettings.value.stimuliType,
//                nValue = settings.nBackLevel,
//                eventInterval = settings.eventInterval,
//                numberOfEvents = settings.numberOfEvents
//            )
//            //userPreferencesRepository.highscore.collect { _highscore.value = it }
//        }
//    }
//
//
//
//    override fun onInit(status: Int) {
//        if (status == TextToSpeech.SUCCESS) {
//            textToSpeech?.language = Locale.US
//            Log.d("GameVM", "TextToSpeech initialized successfully")
//        } else {
//            Log.e("GameVM", "Text-to-Speech initialization failed with status: $status")
//        }
//    }
//
//    private fun playAudioLetter(letter: Char) {
//        textToSpeech?.let {
//            if (it.isSpeaking) it.stop()
//            it.speak(letter.toString(), TextToSpeech.QUEUE_FLUSH, null, null)
//            Log.d("GameVM", "Playing audio for letter: $letter")
//        }
//    }
//
//    override fun startGame() {
//        stopGame()
//        _score.value = 0
//        matchCheckedPositions.clear()
//        matchCheckedAudio.clear()
//        _currentIndex.value = -1
//
//        viewModelScope.launch {
//            val settings = userPreferencesRepository.settings.first()
//            if (settings.numberOfEvents <= 0 || settings.nBackLevel <= 0) {
//                Log.e("GameVM", "Invalid settings: Number of Events = ${settings.numberOfEvents}, N-Back Level = ${settings.nBackLevel}")
//                return@launch
//            }
//
//            _gameState.value = _gameState.value.copy(gridSize = settings.gridSize)
//
//            val gridRange = settings.gridSize * settings.gridSize
//            positionSequence = nBackHelper.generateNBackString(settings.numberOfEvents, gridRange, 40, settings.nBackLevel).toList()
//            letterSequence = generateLimitedLetterSequence(settings.numberOfEvents, settings.audioLetterCount)
//
//            job = viewModelScope.launch {
//                if (!waitForTTSReady()) return@launch
//
//                for (i in positionSequence.indices) {
//                    _currentIndex.value = i
//                    val currentPosition = positionSequence[i]
//                    val currentLetter = letterSequence[i]
//
//                    Log.d("GameVM", "Activating tile at position $currentPosition with letter $currentLetter")
//
//                    // Deactivate the current position (turn off)
//                    _activatedPositions.value = emptySet()
//                    delay(100)  // Short delay for the "off" state
//
//
//                    _activatedPositions.value = setOf(currentPosition)
//                    _gameState.value = _gameState.value.copy(
//                        currentPosition = currentPosition,
//                        currentLetter = currentLetter
//                    )
//                    delay(50)
//
//                    if(gameState.value.gameType == GameType.AudioVisual || gameState.value.gameType == GameType.Audio){
//                        playAudioLetter(currentLetter)
//                    }
//
//                    delay(settings.eventInterval.toLong())
//
//                    _activatedPositions.value = emptySet()
//                    Log.d("GameVM", "Deactivating tile at position $currentPosition")
//                    delay(100)
//                }
//            }
//        }
//    }
//
//    private suspend fun waitForTTSReady(): Boolean {
//        repeat(10) {
//            if (textToSpeech != null && textToSpeech?.isSpeaking == false) return true
//            delay(100)
//        }
//        Log.e("GameVM", "TextToSpeech not ready, cancelling game start")
//        return false
//    }
//
//    fun setGameType(gameType: GameType) {
//        _gameSettings.value = _gameSettings.value.copy(stimuliType = gameType)
//    }
//    private fun generateLimitedLetterSequence(size: Int, maxUniqueLetters: Int): List<Char> {
//        val letterPool = ('A' until 'A' + maxUniqueLetters).toList()
//        val random = Random()
//        return List(size) {
//            letterPool[random.nextInt(letterPool.size)]
//        }
//    }
//
//
//    override fun checkAudioMatch() {
//        if (currentIndex.value >= nBack) {
//            val matchIndex = currentIndex.value - nBack
//            if (letterSequence[matchIndex] == letterSequence[currentIndex.value] && matchCheckedAudio.add(currentIndex.value)) {
//                _score.value += 1
//                Log.d("GameVM", "Audio Match confirmed at index ${currentIndex.value}, score incremented.")
//                _gameState.value = _gameState.value.copy(feedback = "Audio Correct match!")
//            } else {
//                _gameState.value = _gameState.value.copy(feedback = "No match!")
//            }
//        }
//    }
//
//    override fun checkPlaceMatch() {
//        if (currentIndex.value >= nBack) {
//            val matchIndex = currentIndex.value - nBack
//            if (positionSequence[matchIndex] == positionSequence[currentIndex.value] && matchCheckedPositions.add(currentIndex.value)) {
//                _score.value += 1
//                Log.d("GameVM", "Place Match confirmed at index ${currentIndex.value}, score incremented.")
//                _gameState.value = _gameState.value.copy(feedback = "Place Correct match!")
//            } else {
//                _gameState.value = _gameState.value.copy(feedback = "No match!")
//            }
//        }
//    }
//
//    override fun stopAudio() {
//        textToSpeech?.let {
//            if (it.isSpeaking) it.stop()
//            Log.d("GameVM", "TextToSpeech stopped.")
//        }
//    }
//
//    override fun stopGame() {
//        job?.cancel()
//        stopAudio()
//        _activatedPositions.value = emptySet()
//        _gameState.value = _gameState.value.copy(currentPosition = -1, currentLetter = ' ', feedback = "")
//        Log.d("GameVM", "Game stopped.")
//    }
//
//    override fun onCleared() {
//        super.onCleared()
//        stopGame()
//        textToSpeech?.shutdown()
//        Log.d("GameVM", "Game stopped and TextToSpeech shutdown.")
//    }
//
//    companion object {
//        fun provideFactory(application: GameApplication): ViewModelProvider.Factory {
//            return object : ViewModelProvider.Factory {
//                override fun <T : ViewModel> create(modelClass: Class<T>): T {
//                    if (modelClass.isAssignableFrom(GameVM::class.java)) {
//                        @Suppress("UNCHECKED_CAST")
//                        return GameVM(application.userPreferencesRepository, application) as T
//                    }
//                    throw IllegalArgumentException("Unknown ViewModel class")
//                }
//            }
//        }
//    }
//}
//
//enum class GameType{
//    Audio,
//    Visual,
//    AudioVisual
//}
//data class GameState(
//    val currentPosition: Int = -1,
//    val currentLetter: Char = ' ',
//    val feedback: String = "",
//    val gridSize: Int = 3,
//    val gameType: GameType = GameType.AudioVisual
//)