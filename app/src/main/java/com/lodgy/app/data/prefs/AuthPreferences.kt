package com.lodgy.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.authDataStore by preferencesDataStore(name = "auth_prefs")

class AuthPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private val biometricEnabledKey = booleanPreferencesKey("biometric_enabled")
    private val pinLengthKey = intPreferencesKey("pin_length")
    private val failedAttemptsKey = intPreferencesKey("failed_pin_attempts")
    private val lastFailedAtKey = longPreferencesKey("last_failed_pin_at")

    val biometricEnabled: Flow<Boolean> =
        context.authDataStore.data.map { it[biometricEnabledKey] ?: false }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.authDataStore.edit { it[biometricEnabledKey] = enabled }
    }

    /** How many digits the lock screen should wait for. Absent means an install from before the
     *  length was configurable, whose PIN is 4 digits - the bcrypt hash itself carries no length,
     *  so this is the only thing that has to remember. */
    val pinLength: Flow<Int> =
        context.authDataStore.data.map { (it[pinLengthKey] ?: DEFAULT_PIN_LENGTH).coerceIn(MIN_PIN_LENGTH, MAX_PIN_LENGTH) }

    suspend fun setPinLength(length: Int) {
        context.authDataStore.edit { it[pinLengthKey] = length.coerceIn(MIN_PIN_LENGTH, MAX_PIN_LENGTH) }
    }

    /** Consecutive wrong-PIN count, persisted so force-stopping or rebooting the phone is not a way
     *  around the delay (LODGY-77). */
    val failedPinAttempts: Flow<Int> =
        context.authDataStore.data.map { it[failedAttemptsKey] ?: 0 }

    /** Wall-clock time of the most recent wrong PIN, against which the required delay is measured. */
    val lastFailedPinAt: Flow<Long> =
        context.authDataStore.data.map { it[lastFailedAtKey] ?: 0L }

    /** Records one wrong attempt at [at] and returns the new consecutive-failure count. */
    suspend fun recordFailedPinAttempt(at: Long): Int {
        var newCount = 0
        context.authDataStore.edit {
            newCount = (it[failedAttemptsKey] ?: 0) + 1
            it[failedAttemptsKey] = newCount
            it[lastFailedAtKey] = at
        }
        return newCount
    }

    /** Clears the count the moment the warden gets in, by PIN or biometric (LODGY-77). */
    suspend fun resetFailedPinAttempts() {
        context.authDataStore.edit {
            it.remove(failedAttemptsKey)
            it.remove(lastFailedAtKey)
        }
    }

    companion object {
        const val MIN_PIN_LENGTH = 4
        const val MAX_PIN_LENGTH = 6
        const val DEFAULT_PIN_LENGTH = 4
    }
}
