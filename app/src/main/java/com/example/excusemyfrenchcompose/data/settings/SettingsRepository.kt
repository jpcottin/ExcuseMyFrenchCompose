package com.example.excusemyfrenchcompose.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persists user preferences. Kept behind an interface so the ViewModel can be unit tested
 * without touching the Android DataStore implementation.
 */
interface SettingsRepository {
    val isMuted: Flow<Boolean>
    suspend fun setMuted(muted: Boolean)
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreSettingsRepository(private val context: Context) : SettingsRepository {

    override val isMuted: Flow<Boolean> =
        context.dataStore.data.map { preferences -> preferences[MUTED_KEY] ?: DEFAULT_MUTED }

    override suspend fun setMuted(muted: Boolean) {
        context.dataStore.edit { preferences -> preferences[MUTED_KEY] = muted }
    }

    companion object {
        // Default to muted so the app never speaks unexpectedly on first launch.
        private const val DEFAULT_MUTED = true
        private val MUTED_KEY = booleanPreferencesKey("is_muted")
    }
}
