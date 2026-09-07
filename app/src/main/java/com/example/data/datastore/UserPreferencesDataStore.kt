package com.example.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferencesDataStore(
    private val context: Context
) {
    companion object {
        private val LAST_PLAYED_TRACK_ID = stringPreferencesKey("last_played_track_id")
        private val HIGH_QUALITY_AUDIO = booleanPreferencesKey("high_quality_audio")
        private val GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
        private val NORMALIZE_VOLUME = booleanPreferencesKey("normalize_volume")
        private val SHUFFLE_MODE = booleanPreferencesKey("shuffle_mode")
        private val REPEAT_MODE = intPreferencesKey("repeat_mode")

        // Equalizer preferences
        private val EQUALIZER_ENABLED = booleanPreferencesKey("equalizer_enabled")
        private val EQUALIZER_PRESET = stringPreferencesKey("equalizer_preset")
        private val EQUALIZER_BASS = intPreferencesKey("equalizer_bass")
        private val EQUALIZER_VIRTUALIZER = intPreferencesKey("equalizer_virtualizer")
        private val EQUALIZER_BAND_GAINS = stringPreferencesKey("equalizer_band_gains")
    }

    private val safeDataFlow: Flow<Preferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    val lastPlayedTrackId: Flow<String?> = safeDataFlow.map { it[LAST_PLAYED_TRACK_ID] }
    val isHighQualityAudio: Flow<Boolean> = safeDataFlow.map { it[HIGH_QUALITY_AUDIO] ?: true }
    val isGaplessPlayback: Flow<Boolean> = safeDataFlow.map { it[GAPLESS_PLAYBACK] ?: true }
    val isNormalizeVolume: Flow<Boolean> = safeDataFlow.map { it[NORMALIZE_VOLUME] ?: false }
    val shuffleMode: Flow<Boolean> = safeDataFlow.map { it[SHUFFLE_MODE] ?: false }
    val repeatMode: Flow<Int> = safeDataFlow.map { it[REPEAT_MODE] ?: 0 }

    val equalizerEnabled: Flow<Boolean> = safeDataFlow.map { it[EQUALIZER_ENABLED] ?: true }
    val equalizerPreset: Flow<String> = safeDataFlow.map { it[EQUALIZER_PRESET] ?: "FLAT" }
    val equalizerBass: Flow<Int> = safeDataFlow.map { it[EQUALIZER_BASS] ?: 0 }
    val equalizerVirtualizer: Flow<Int> = safeDataFlow.map { it[EQUALIZER_VIRTUALIZER] ?: 0 }
    val equalizerBandGains: Flow<List<Int>> = safeDataFlow.map { prefs ->
        val saved = prefs[EQUALIZER_BAND_GAINS]
        if (saved != null) {
            val parsed = saved.split(",").mapNotNull { it.trim().toIntOrNull() }
            if (parsed.size == 5) parsed else listOf(0, 0, 0, 0, 0)
        } else {
            listOf(0, 0, 0, 0, 0)
        }
    }

    suspend fun saveLastPlayedTrackId(id: String) {
        context.dataStore.edit { it[LAST_PLAYED_TRACK_ID] = id }
    }

    suspend fun setHighQualityAudio(enabled: Boolean) {
        context.dataStore.edit { it[HIGH_QUALITY_AUDIO] = enabled }
    }

    suspend fun setGaplessPlayback(enabled: Boolean) {
        context.dataStore.edit { it[GAPLESS_PLAYBACK] = enabled }
    }

    suspend fun setNormalizeVolume(enabled: Boolean) {
        context.dataStore.edit { it[NORMALIZE_VOLUME] = enabled }
    }

    suspend fun setShuffleMode(enabled: Boolean) {
        context.dataStore.edit { it[SHUFFLE_MODE] = enabled }
    }

    suspend fun setRepeatMode(mode: Int) {
        context.dataStore.edit { it[REPEAT_MODE] = mode }
    }

    suspend fun saveEqualizerSettings(
        enabled: Boolean,
        presetName: String,
        bass: Int,
        virtualizer: Int,
        bandGains: List<Int>
    ) {
        context.dataStore.edit { prefs ->
            prefs[EQUALIZER_ENABLED] = enabled
            prefs[EQUALIZER_PRESET] = presetName
            prefs[EQUALIZER_BASS] = bass
            prefs[EQUALIZER_VIRTUALIZER] = virtualizer
            prefs[EQUALIZER_BAND_GAINS] = bandGains.joinToString(",")
        }
    }

    suspend fun getInitialEqualizerSnapshot(): EqualizerPreferencesSnapshot {
        val prefs = safeDataFlow.first()
        val gainsStr = prefs[EQUALIZER_BAND_GAINS]
        val parsedGains = if (gainsStr != null) {
            val parsed = gainsStr.split(",").mapNotNull { it.trim().toIntOrNull() }
            if (parsed.size == 5) parsed else listOf(0, 0, 0, 0, 0)
        } else {
            listOf(0, 0, 0, 0, 0)
        }
        return EqualizerPreferencesSnapshot(
            enabled = prefs[EQUALIZER_ENABLED] ?: true,
            presetName = prefs[EQUALIZER_PRESET] ?: "FLAT",
            bass = prefs[EQUALIZER_BASS] ?: 0,
            virtualizer = prefs[EQUALIZER_VIRTUALIZER] ?: 0,
            bandGains = parsedGains
        )
    }
}

data class EqualizerPreferencesSnapshot(
    val enabled: Boolean,
    val presetName: String,
    val bass: Int,
    val virtualizer: Int,
    val bandGains: List<Int>
)
