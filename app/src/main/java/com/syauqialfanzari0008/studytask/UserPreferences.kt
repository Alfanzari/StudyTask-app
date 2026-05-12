package com.syauqialfanzari0008.studytask

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {

    companion object {
        val USERNAME_KEY = stringPreferencesKey("username")
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
    }

    val username: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[USERNAME_KEY] ?: ""
    }

    val darkMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[DARK_MODE_KEY] ?: false
    }

    suspend fun saveUsername(name: String) {
        context.dataStore.edit { prefs ->
            prefs[USERNAME_KEY] = name
        }
    }

    suspend fun saveDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DARK_MODE_KEY] = enabled
        }
    }
}