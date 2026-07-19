package io.github.jpcottin.excusemyfrench.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
    val insultLevel: Flow<Int>
    suspend fun setInsultLevel(level: Int)
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreSettingsRepository(private val context: Context) : SettingsRepository {

    override val isMuted: Flow<Boolean> =
        context.dataStore.data.map { preferences -> preferences[MUTED_KEY] ?: DEFAULT_MUTED }

    override suspend fun setMuted(muted: Boolean) {
        context.dataStore.edit { preferences -> preferences[MUTED_KEY] = muted }
    }

    override val insultLevel: Flow<Int> =
        context.dataStore.data.map { preferences -> preferences[LEVEL_KEY] ?: DEFAULT_LEVEL }

    override suspend fun setInsultLevel(level: Int) {
        context.dataStore.edit { preferences -> preferences[LEVEL_KEY] = level }
    }

    companion object {
        // Default to muted so the app never speaks unexpectedly on first launch.
        private const val DEFAULT_MUTED = true
        // Default to family-friendly so offensive content is opt-in on first launch.
        const val DEFAULT_LEVEL = 1
        private val MUTED_KEY = booleanPreferencesKey("is_muted")
        private val LEVEL_KEY = intPreferencesKey("insult_level")
    }
}
