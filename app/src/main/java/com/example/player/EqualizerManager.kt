package com.example.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class EqualizerPreset(
    val displayName: String,
    val bassStrength: Short,     // 0 to 1000
    val virtualizerStrength: Short, // 0 to 1000 (3D / spatializer)
    val bandMultipliers: List<Float> // Relative boost/cut (-1f to +1f) for [60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz]
) {
    FLAT("Flat", 0, 0, listOf(0f, 0f, 0f, 0f, 0f)),
    BASS("Bass Boost", 850, 200, listOf(0.85f, 0.55f, 0.1f, -0.1f, -0.15f)),
    THREE_D("3D Surround", 400, 950, listOf(0.3f, 0.2f, 0.4f, 0.7f, 0.85f)),
    CLEAR_VOICE("Clear Voice", 100, 150, listOf(-0.35f, 0.1f, 0.8f, 0.75f, 0.3f)),
    ELECTRONIC("Electronic", 700, 500, listOf(0.7f, 0.4f, 0.0f, 0.5f, 0.7f)),
    VOCAL_BOOST("Vocal Boost", 0, 300, listOf(-0.4f, 0.0f, 0.85f, 0.6f, 0.1f))
}

class EqualizerManager {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private val _isEnabled = MutableStateFlow(true)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _currentPreset = MutableStateFlow(EqualizerPreset.FLAT)
    val currentPreset: StateFlow<EqualizerPreset> = _currentPreset.asStateFlow()

    private val _bassLevel = MutableStateFlow(0) // 0 to 100
    val bassLevel: StateFlow<Int> = _bassLevel.asStateFlow()

    private val _virtualizerLevel = MutableStateFlow(0) // 0 to 100 (3D)
    val virtualizerLevel: StateFlow<Int> = _virtualizerLevel.asStateFlow()

    // Normalized band gains: 5 bands each from -100 to +100 (-10dB to +10dB)
    private val _bandGains = MutableStateFlow(listOf(0, 0, 0, 0, 0))
    val bandGains: StateFlow<List<Int>> = _bandGains.asStateFlow()

    private var currentAudioSessionId: Int = 0

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
                    setStrength((_bassLevel.value * 10).toShort())
                }
            }
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = _isEnabled.value
                if (strengthSupported) {
                    setStrength((_virtualizerLevel.value * 10).toShort())
                }
            }
            applyCurrentPreset()
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
    }

    fun selectPreset(preset: EqualizerPreset) {
        _currentPreset.value = preset
        _bassLevel.value = (preset.bassStrength / 10)
        _virtualizerLevel.value = (preset.virtualizerStrength / 10)
        
        val gains = preset.bandMultipliers.map { (it * 100).toInt() }
        _bandGains.value = gains

        applyCurrentPreset()
    }

    fun setBass(level: Int) {
        val clamped = level.coerceIn(0, 100)
        _bassLevel.value = clamped
        try {
            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength((clamped * 10).toShort())
            }
        } catch (e: Exception) {
            Log.e("EqualizerManager", "Error setting bass: ${e.message}")
        }
    }

    fun setVirtualizer(level: Int) {
        val clamped = level.coerceIn(0, 100)
        _virtualizerLevel.value = clamped
        try {
            if (virtualizer?.strengthSupported == true) {
                virtualizer?.setStrength((clamped * 10).toShort())
            }
        } catch (e: Exception) {
            Log.e("EqualizerManager", "Error setting 3D virtualizer: ${e.message}")
        }
    }

    fun setBandGain(bandIndex: Int, gain: Int) {
        val current = _bandGains.value.toMutableList()
        if (bandIndex in current.indices) {
            current[bandIndex] = gain.coerceIn(-100, 100)
            _bandGains.value = current
            applyBandGains()
        }
    }

    private fun applyCurrentPreset() {
        try {
            bassBoost?.let {
                if (it.strengthSupported) {
                    it.setStrength((_bassLevel.value * 10).toShort())
                }
            }
            virtualizer?.let {
                if (it.strengthSupported) {
                    it.setStrength((_virtualizerLevel.value * 10).toShort())
                }
            }
            applyBandGains()
        } catch (e: Exception) {
            Log.e("EqualizerManager", "Error applying preset: ${e.message}")
        }
    }

    private fun applyBandGains() {
        val eq = equalizer ?: return
        try {
            val minRange = eq.bandLevelRange?.getOrNull(0) ?: -1000
            val maxRange = eq.bandLevelRange?.getOrNull(1) ?: 1000
            val rangeSpan = (maxRange - minRange).toFloat()

            val numBands = eq.numberOfBands.toInt()
            val gains = _bandGains.value

            for (i in 0 until numBands) {
                // Map the 5 UI bands to the available hardware bands
                val gainIndex = (i * gains.size / numBands).coerceIn(0, gains.size - 1)
                val normalizedRatio = (gains[gainIndex] + 100f) / 200f // 0f to 1f
                val hardwareLevel = (minRange + normalizedRatio * rangeSpan).toInt().toShort()
                eq.setBandLevel(i.toShort(), hardwareLevel)
            }
        } catch (e: Exception) {
            Log.e("EqualizerManager", "Error applying band gains: ${e.message}")
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
