package com.example.player

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
    val bassStrength: Short,     // 0 to 1000 (hardware effect strength)
    val virtualizerStrength: Short, // 0 to 1000 (3D / spatializer strength)
    val bandGains: List<Int>     // Gains in dB * 10 (-100 to +100 = -10.0dB to +10.0dB) for [60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz]
) {
    FLAT("Flat", 0, 0, listOf(0, 0, 0, 0, 0)),
    BASS("Bass Boost", 220, 0, listOf(35, -15, 0, 10, 15)), // -1.5dB at 230Hz eliminates boxy mud, +3.5dB at 60Hz gives clean punch
    ROCK("Rock", 120, 80, listOf(30, 10, -15, 20, 30)),
    POP("Pop", 100, 90, listOf(15, 5, 20, 25, 20)),
    VOCAL_BOOST("Vocal Clarity", 0, 60, listOf(-20, -10, 30, 30, 10)), // Removes low rumble & boominess, clarifies voices
    ELECTRONIC("Electronic", 200, 120, listOf(35, 5, -10, 20, 30)),
    ACOUSTIC("Acoustic", 0, 80, listOf(15, 10, 10, 15, 25)),
    THREE_D("3D Surround", 100, 220, listOf(15, 0, 10, 15, 25)), // Controlled 22% spatializer without comb-filtering or hollow phasing
    CUSTOM("Custom", 0, 0, listOf(0, 0, 0, 0, 0))
}

@Suppress("DEPRECATION")
class EqualizerManager(
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

    // 5 frequency bands: [60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz] in dB * 10 (-100 to +100)
    private val _bandGains = MutableStateFlow(listOf(0, 0, 0, 0, 0))
    val bandGains: StateFlow<List<Int>> = _bandGains.asStateFlow()

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
        if (currentAudioSessionId == audioSessionId && equalizer != null) return

        release()
        currentAudioSessionId = audioSessionId

        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = _isEnabled.value
            }
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = _isEnabled.value
                if (strengthSupported) {
                    setStrength((_bassLevel.value * 10).coerceIn(0, 1000).toShort())
                }
            }
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = _isEnabled.value
                if (strengthSupported) {
                    setStrength((_virtualizerLevel.value * 10).coerceIn(0, 1000).toShort())
                }
            }
            applyAllSettings()
        } catch (e: Exception) {
            Log.e("EqualizerManager", "Error binding audio effects: ${e.message}")
        }
    }

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
        } catch (e: Exception) {
            Log.e("EqualizerManager", "Error toggling equalizer: ${e.message}")
        }
        persistSettings()
    }

    fun selectPreset(preset: EqualizerPreset) {
        _currentPreset.value = preset
        if (preset != EqualizerPreset.CUSTOM) {
            _bassLevel.value = (preset.bassStrength / 10).coerceIn(0, 100)
            _virtualizerLevel.value = (preset.virtualizerStrength / 10).coerceIn(0, 100)
            _bandGains.value = preset.bandGains
        }
        applyAllSettings()
        persistSettings()
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
            current[bandIndex] = gain.coerceIn(-100, 100)
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
            val minRange = eq.bandLevelRange?.getOrNull(0) ?: -1000
            val maxRange = eq.bandLevelRange?.getOrNull(1) ?: 1000

            val numBands = eq.numberOfBands.toInt()
            val gains = _bandGains.value

            for (i in 0 until numBands) {
                val gainIndex = (i * gains.size / numBands).coerceIn(0, gains.size - 1)
                val gain = gains[gainIndex] // -100 to +100 (-10.0dB to +10.0dB)
                // 1 dB = 100 millibels (mB). So gain * 10 accurately converts dB*10 into millibels.
                val targetMb = (gain * 10).coerceIn(minRange.toInt(), maxRange.toInt()).toShort()
                eq.setBandLevel(i.toShort(), targetMb)
            }
        } catch (e: Exception) {
            Log.e("EqualizerManager", "Error applying band gains: ${e.message}")
        }
    }

    private fun persistSettings() {
        val enabled = _isEnabled.value
        val presetName = _currentPreset.value.name
        val bass = _bassLevel.value
        val virtualizer = _virtualizerLevel.value
        val gains = _bandGains.value

        coroutineScope.launch {
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
