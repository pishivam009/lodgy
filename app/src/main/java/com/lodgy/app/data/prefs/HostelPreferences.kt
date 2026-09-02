package com.lodgy.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.hostelDataStore by preferencesDataStore(name = "hostel_prefs")

class HostelPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private val selectedHostelIdKey = stringPreferencesKey("selected_hostel_id")

    val selectedHostelId: Flow<String?> =
        context.hostelDataStore.data.map { it[selectedHostelIdKey] }

    suspend fun setSelectedHostelId(id: String) {
        context.hostelDataStore.edit { it[selectedHostelIdKey] = id }
    }
}
