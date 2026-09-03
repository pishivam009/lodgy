package com.lodgy.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { LIGHT, DARK, SYSTEM }

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

class ThemePreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private val themeModeKey = stringPreferencesKey("theme_mode")

    val themeMode: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        prefs[themeModeKey]?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } } ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { it[themeModeKey] = mode.name }
    }
}
