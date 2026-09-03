package com.lodgy.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.notificationDataStore by preferencesDataStore(name = "notification_prefs")

class NotificationPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private val vacancyEnabledKey = booleanPreferencesKey("vacancy_enabled")
    private val duesEnabledKey = booleanPreferencesKey("dues_enabled")
    private val vacancyThresholdKey = intPreferencesKey("vacancy_threshold_days")
    private val notifiedBedIdsKey = stringSetPreferencesKey("notified_bed_ids")

    val vacancyEnabled: Flow<Boolean> =
        context.notificationDataStore.data.map { it[vacancyEnabledKey] ?: true }

    /** Separate switch from [vacancyEnabled] so turning one category off leaves the other alone. */
    val duesEnabled: Flow<Boolean> =
        context.notificationDataStore.data.map { it[duesEnabledKey] ?: true }

    /** Warden-set, not a constant. Clamped so a stray value can never disable or spam the check. */
    val vacancyThresholdDays: Flow<Int> = context.notificationDataStore.data.map {
        (it[vacancyThresholdKey] ?: DEFAULT_VACANCY_THRESHOLD_DAYS).coerceIn(MIN_THRESHOLD_DAYS, MAX_THRESHOLD_DAYS)
    }

    /** Beds already nudged about, so a still-vacant bed is reported once rather than daily. */
    val notifiedBedIds: Flow<Set<String>> =
        context.notificationDataStore.data.map { it[notifiedBedIdsKey] ?: emptySet() }

    suspend fun setVacancyEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { it[vacancyEnabledKey] = enabled }
    }

    suspend fun setDuesEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { it[duesEnabledKey] = enabled }
    }

    suspend fun setVacancyThresholdDays(days: Int) {
        context.notificationDataStore.edit {
            it[vacancyThresholdKey] = days.coerceIn(MIN_THRESHOLD_DAYS, MAX_THRESHOLD_DAYS)
        }
    }

    suspend fun setNotifiedBedIds(ids: Set<String>) {
        context.notificationDataStore.edit { it[notifiedBedIdsKey] = ids }
    }

    companion object {
        const val DEFAULT_VACANCY_THRESHOLD_DAYS = 7
        const val MIN_THRESHOLD_DAYS = 1
        const val MAX_THRESHOLD_DAYS = 90
    }
}
