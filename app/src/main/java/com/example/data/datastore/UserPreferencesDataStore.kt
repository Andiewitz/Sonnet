package com.example.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferencesDataStore(
    private val context: Context
) {
    private val LAST_PLAYED_TRACK_ID = stringPreferencesKey("last_played_track_id")

    val lastPlayedTrackId: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[LAST_PLAYED_TRACK_ID]
        }

    suspend fun saveLastPlayedTrackId(id: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_PLAYED_TRACK_ID] = id
        }
    }
}
