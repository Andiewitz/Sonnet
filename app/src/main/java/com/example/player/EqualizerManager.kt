package com.example.player

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log
import com.example.data.datastore.UserPreferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("DEPRECATION")
enum class EqualizerPreset(
    val displayName: String,
    val bassStrength: Short,        // 0 to 1000 (hardware effect strength)
    val virtualizerStrength: Short, // 0 to 1000 (3D / spatializer strength)
    val bandGains: List<Int>        // Gains in dB * 10 (-120 to +120 = -12.0dB to +12.0dB) for [60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz]
) {
    FLAT("Flat", 0, 0, listOf(0, 0, 0, 0, 0)),
    BASS("Bass Boost", 800, 150, listOf(90, 50, -20, 20, 40)),       // Rich +9dB sub-bass and 80% hardware BassBoost
    ROCK("Rock", 450, 300, listOf(70, 30, -35, 55, 80)),             // Classic rock V-curve for heavy riffs & crisp cymbals
    POP("Pop", 350, 400, listOf(40, 20, 50, 60, 50)),                // Punchy radio presence and bright vocal clarity
    VOCAL_BOOST("Vocal Clarity", 0, 200, listOf(-60, -30, 70, 85, 40)), // Low cut (-6dB), prominent vocal core (+7 to +8.5dB)
    ELECTRONIC("Electronic", 850, 450, listOf(100, 50, -30, 50, 90)),// Thunderous 10dB sub kick & sizzling synth highs
    ACOUSTIC("Acoustic", 250, 350, listOf(40, 35, 15, 50, 70)),      // Natural warm low-end and airy string shimmer
    THREE_D("3D Surround", 400, 850, listOf(50, 20, 30, 50, 70)),    // Immersive 85% spatial widening & dynamic soundstage
    CUSTOM("Custom", 0, 0, listOf(0, 0, 0, 0, 0))
}

@Suppress("DEPRECATION")
class EqualizerManager(
    private val context: Context,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private val _isEnabled = MutableStateFlow(true)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _currentPreset = MutableStateFlow(EqualizerPreset.FLAT)
    val currentPreset: StateFlow<EqualizerPreset> = _currentPreset.asStateFlow()

    private val _bassLevel = MutableStateFlow(0) // 0 to 100 %
    val bassLevel: StateFlow<Int> = _bassLevel.asStateFlow()

    private val _virtualizerLevel = MutableStateFlow(0) // 0 to 100 % (3D)
    val virtualizerLevel: StateFlow<Int> = _virtualizerLevel.asStateFlow()

    // 5 frequency bands: [60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz] in dB * 10 (-120 to +120)
    private val _bandGains = MutableStateFlow(listOf(0, 0, 0, 0, 0))
    val bandGains: StateFlow<List<Int>> = _bandGains.asStateFlow()

    private val _bandFrequencyLabels = MutableStateFlow(listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz"))
    val bandFrequencyLabels: StateFlow<List<String>> = _bandFrequencyLabels.asStateFlow()

    private var currentAudioSessionId: Int = 0

    init {
        // Restore saved equalizer preferences asynchronously
        coroutineScope.launch {
            try {
                val snapshot = userPreferencesDataStore.getInitialEqualizerSnapshot()
                _isEnabled.value = snapshot.enabled
                _bassLevel.value = snapshot.bass
                _virtualizerLevel.value = snapshot.virtualizer
                _bandGains.value = snapshot.bandGains
                _currentPreset.value = EqualizerPreset.entries.find { it.name == snapshot.presetName }
                    ?: EqualizerPreset.CUSTOM

                withContext(Dispatchers.Main) {
                    if (equalizer != null) {
                        applyAllSettings()
                    }
                }
            } catch (e: Exception) {
                Log.e("EqualizerManager", "Error restoring equalizer settings: ${e.message}")
            }
        }
    }

    fun bindAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        if (currentAudioSessionId == audioSessionId && equalizer != null) {
            applyAllSettings()
            return
        }

        release()
        currentAudioSessionId = audioSessionId

        try {
            // Inform Android audio framework and DSP of active audio effect session
            val openIntent = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
            context.sendBroadcast(openIntent)

            // Priority 1000 ensures application takes priority over standard system background effects
            equalizer = Equalizer(1000, audioSessionId).apply {
                enabled = _isEnabled.value
            }
            bassBoost = BassBoost(1000, audioSessionId).apply {
                enabled = _isEnabled.value
                if (strengthSupported) {
                    setStrength((_bassLevel.value * 10).coerceIn(0, 1000).toShort())
                }
            }
            virtualizer = Virtualizer(1000, audioSessionId).apply {
                enabled = _isEnabled.value
                if (strengthSupported) {
                    setStrength((_virtualizerLevel.value * 10).coerceIn(0, 1000).toShort())
                }
            }

            updateBandFrequencies()
            applyAllSettings()
            Log.d("EqualizerManager", "Bound audio session $audioSessionId. Eq control=${equalizer?.hasControl()}")
        } catch (e: Exception) {
            Log.e("EqualizerManager", "Error binding audio effects: ${e.message}")
        }
    }

    private fun updateBandFrequencies() {
        val eq = equalizer ?: return
        try {
            val count = eq.numberOfBands.toInt()
            val labels = mutableListOf<String>()
            for (i in 0 until count) {
                val centerMhz = eq.getCenterFreq(i.toShort()) // in mHz
                val hz = centerMhz / 1000
                val label = if (hz >= 1000) {
                    val khz = hz / 1000f
                    if (khz % 1f == 0f) "${khz.toInt()} kHz" else String.format(java.util.Locale.US, "%.1f kHz", khz)
                } else {
                    "$hz Hz"
                }
                labels.add(label)
            }
            if (labels.isNotEmpty()) {
                _bandFrequencyLabels.value = labels
            }
        } catch (e: Exception) {
            Log.e("EqualizerManager", "Error fetching center frequencies: ${e.message}")
        }
    }

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
            if (enabled) {
                applyAllSettings()
            }
        } catch (e: Exception) {
            Log.e("EqualizerManager", "Error toggling equalizer: ${e.message}")
        }
        persistSettings(immediate = true)
    }

    fun selectPreset(preset: EqualizerPreset) {
        _currentPreset.value = preset
        if (preset != EqualizerPreset.CUSTOM) {
            _bassLevel.value = (preset.bassStrength / 10).coerceIn(0, 100)
            _virtualizerLevel.value = (preset.virtualizerStrength / 10).coerceIn(0, 100)
            _bandGains.value = preset.bandGains
        }
        applyAllSettings()
        persistSettings(immediate = true)
    }

    fun setBass(level: Int) {
        val clamped = level.coerceIn(0, 100)
        _bassLevel.value = clamped
        checkIfCustom()
        try {
            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength((clamped * 10).coerceIn(0, 1000).toShort())
            }
        } catch (e: Exception) {
            Log.e("EqualizerManager", "Error setting bass: ${e.message}")
        }
        persistSettings()
    }

    fun setVirtualizer(level: Int) {
        val clamped = level.coerceIn(0, 100)
        _virtualizerLevel.value = clamped
        checkIfCustom()
        try {
            if (virtualizer?.strengthSupported == true) {
                virtualizer?.setStrength((clamped * 10).coerceIn(0, 1000).toShort())
            }
        } catch (e: Exception) {
            Log.e("EqualizerManager", "Error setting 3D virtualizer: ${e.message}")
        }
        persistSettings()
    }

    fun setBandGain(bandIndex: Int, gain: Int) {
        val current = _bandGains.value.toMutableList()
        if (bandIndex in current.indices) {
            current[bandIndex] = gain.coerceIn(-120, 120)
            _bandGains.value = current
            checkIfCustom()
            applyBandGains()
            persistSettings()
        }
    }

    private fun checkIfCustom() {
        val currentPreset = _currentPreset.value
        if (currentPreset != EqualizerPreset.CUSTOM) {
            val matches = currentPreset.bandGains == _bandGains.value &&
                    (currentPreset.bassStrength / 10) == _bassLevel.value &&
                    (currentPreset.virtualizerStrength / 10) == _virtualizerLevel.value
            if (!matches) {
                _currentPreset.value = EqualizerPreset.CUSTOM
            }
        }
    }

    private fun applyAllSettings() {
        try {
            bassBoost?.let {
                if (it.strengthSupported) {
                    it.setStrength((_bassLevel.value * 10).coerceIn(0, 1000).toShort())
                }
            }
            virtualizer?.let {
                if (it.strengthSupported) {
                    it.setStrength((_virtualizerLevel.value * 10).coerceIn(0, 1000).toShort())
                }
            }
            applyBandGains()
        } catch (e: Exception) {
            Log.e("EqualizerManager", "Error applying settings: ${e.message}")
        }
    }

    private fun applyBandGains() {
        val eq = equalizer ?: return
        try {
            val minRange = eq.bandLevelRange?.getOrNull(0) ?: -1500
            val maxRange = eq.bandLevelRange?.getOrNull(1) ?: 1500

            val numBands = eq.numberOfBands.toInt()
            val gains = _bandGains.value

            for (i in 0 until numBands) {
                val gainIndex = (i * gains.size / numBands).coerceIn(0, gains.size - 1)
                val gain = gains[gainIndex] // e.g. -120 to +120 (-12.0dB to +12.0dB)
                // 1 dB = 100 millibels (mB). So gain * 10 accurately converts dB*10 into millibels.
                val targetMb = (gain * 10).coerceIn(minRange.toInt(), maxRange.toInt()).toShort()
                eq.setBandLevel(i.toShort(), targetMb)
            }
        } catch (e: Exception) {
            Log.e("EqualizerManager", "Error applying band gains: ${e.message}")
        }
    }

    private var persistJob: kotlinx.coroutines.Job? = null

    private fun persistSettings(immediate: Boolean = false) {
        val enabled = _isEnabled.value
        val presetName = _currentPreset.value.name
        val bass = _bassLevel.value
        val virtualizer = _virtualizerLevel.value
        val gains = _bandGains.value

        persistJob?.cancel()
        persistJob = coroutineScope.launch {
            if (!immediate) {
                kotlinx.coroutines.delay(300)
            }
            try {
                userPreferencesDataStore.saveEqualizerSettings(
                    enabled = enabled,
                    presetName = presetName,
                    bass = bass,
                    virtualizer = virtualizer,
                    bandGains = gains
                )
            } catch (e: Exception) {
                Log.e("EqualizerManager", "Error persisting equalizer settings: ${e.message}")
            }
        }
    }

    fun release() {
        if (currentAudioSessionId > 0) {
            try {
                val closeIntent = Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                    putExtra(AudioEffect.EXTRA_AUDIO_SESSION, currentAudioSessionId)
                    putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                }
                context.sendBroadcast(closeIntent)
            } catch (e: Exception) {
                Log.e("EqualizerManager", "Error broadcasting close session: ${e.message}")
            }
        }
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
        } catch (e: Exception) {
            Log.e("EqualizerManager", "Error releasing audio effects: ${e.message}")
        } finally {
            equalizer = null
            bassBoost = null
            virtualizer = null
            currentAudioSessionId = 0
        }
    }
}
