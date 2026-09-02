package com.lodgy.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.authDataStore by preferencesDataStore(name = "auth_prefs")

class AuthPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private val biometricEnabledKey = booleanPreferencesKey("biometric_enabled")

    val biometricEnabled: Flow<Boolean> =
        context.authDataStore.data.map { it[biometricEnabledKey] ?: false }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.authDataStore.edit { it[biometricEnabledKey] = enabled }
    }
}
