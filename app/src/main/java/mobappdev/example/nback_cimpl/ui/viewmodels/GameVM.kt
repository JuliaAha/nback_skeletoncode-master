package mobappdev.example.nback_cimpl.ui.viewmodels

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
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

interface GameViewModel {
    val eventValues: StateFlow<List<Int>>
    val gameState: StateFlow<GameState>
    val score: StateFlow<Int>
    val highscore: StateFlow<Int>
    val nBack: Int

    fun setGameType(gameType: GameType)
    fun startGame()
    fun checkMatch()
    fun playSound()
}

class GameVM(
    private val userPreferencesRepository: UserPreferencesRepository
) : GameViewModel, ViewModel() {

    private val _gameState = MutableStateFlow(GameState())
    override val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _score = MutableStateFlow(0)
    override val score: StateFlow<Int> = _score

    private val _highscore = MutableStateFlow(0)
    override val highscore: StateFlow<Int> = _highscore

    override val nBack: Int = 1

    private val _eventValues = MutableStateFlow<List<Int>>(emptyList())
    override val eventValues: StateFlow<List<Int>> = _eventValues.asStateFlow()

    private var job: Job? = null
    private val eventInterval: Long = 2000L
    private val nBackHelper = NBackHelper()
    private var events = emptyArray<Int>()
    private var audioTrack: AudioTrack? = null

    init {
        initializeAudio()
        viewModelScope.launch {
            userPreferencesRepository.highscore.collect { _highscore.value = it }
        }
    }

    private fun initializeAudio() {
        val sampleRate = 44100
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .build()
    }

  override fun playSound() {
        audioTrack?.apply {
            val soundData = generateBeepSound()
            if (playState != AudioTrack.PLAYSTATE_PLAYING) {
                play() // Start playing only if not already in play state
            }
            write(soundData, 0, soundData.size)
        }
    }

    private fun generateBeepSound(): ShortArray {
        val duration = 0.5
        val sampleRate = 44100
        val numSamples = (duration * sampleRate).toInt()
        val sound = ShortArray(numSamples)
        val freq = 440.0
        for (i in sound.indices) {
            sound[i] = (Short.MAX_VALUE * Math.sin(2 * Math.PI * i / (sampleRate / freq))).toInt().toShort()
        }
        return sound
    }

    override fun setGameType(gameType: GameType) {
        _gameState.value = _gameState.value.copy(gameType = gameType)
    }

    override fun startGame() {
        job?.cancel()
        events = nBackHelper.generateNBackString(10, 9, 30, nBack).toList().toTypedArray()
        Log.d("GameVM", "Generated sequence: ${events.contentToString()}")

        job = viewModelScope.launch {
            when (gameState.value.gameType) {
                GameType.Audio -> runAudioGame()
                GameType.AudioVisual -> runAudioVisualGame()
                GameType.Visual -> runVisualGame(events)
            }
        }
    }

    override fun checkMatch() {
        val currentPos = gameState.value.eventValue
        val matchCondition = currentPos >= 0 && events.isNotEmpty() && events[currentPos] == currentPos
        if (matchCondition) {
            _score.value += 1
            _gameState.value = gameState.value.copy(feedback = "Correct match!")
        } else {
            _gameState.value = gameState.value.copy(feedback = "No match!")
        }
    }

    private fun runAudioGame() {
        Log.d("GameVM", "Audio game started")
        playSound()
    }

    private suspend fun runVisualGame(events: Array<Int>) {
        for (value in events) {
            _gameState.value = gameState.value.copy(eventValue = value)
            _eventValues.value = _eventValues.value + value
            delay(eventInterval)
        }
    }

    private fun runAudioVisualGame() {
        Log.d("GameVM", "Audio-visual game started")
        playSound()
    }

    override fun onCleared() {
        super.onCleared()
        audioTrack?.release()
        audioTrack = null
    }

    companion object {
        fun provideFactory(application: GameApplication): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(GameVM::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return GameVM(application.userPreferencesRepository) as T
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
    val eventValue: Int = -1,
    val feedback: String = ""
)
