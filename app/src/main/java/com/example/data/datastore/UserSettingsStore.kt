package com.example.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserSettingsStore(private val context: Context) {

    companion object {
        val USER_NAME_KEY = stringPreferencesKey("user_name")
        val USER_PHONE_KEY = stringPreferencesKey("user_phone")
        val SOUND_ENABLED_KEY = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED_KEY = booleanPreferencesKey("vibration_enabled")
        val CAREGIVER_MODE_ENABLED_KEY = booleanPreferencesKey("caregiver_mode_enabled")
        val REMINDER_ADVANCE_TIME_KEY = intPreferencesKey("reminder_advance_time")
    }

    val userName: Flow<String> = context.userSettingsDataStore.data.map { preferences ->
        preferences[USER_NAME_KEY] ?: "Margaret Vance"
    }

    val userPhone: Flow<String> = context.userSettingsDataStore.data.map { preferences ->
        preferences[USER_PHONE_KEY] ?: "+1 (555) 234-5678"
    }

    val soundEnabled: Flow<Boolean> = context.userSettingsDataStore.data.map { preferences ->
        preferences[SOUND_ENABLED_KEY] ?: true
    }

    val vibrationEnabled: Flow<Boolean> = context.userSettingsDataStore.data.map { preferences ->
        preferences[VIBRATION_ENABLED_KEY] ?: true
    }

    val caregiverModeEnabled: Flow<Boolean> = context.userSettingsDataStore.data.map { preferences ->
        preferences[CAREGIVER_MODE_ENABLED_KEY] ?: false
    }

    val reminderAdvanceTime: Flow<Int> = context.userSettingsDataStore.data.map { preferences ->
        preferences[REMINDER_ADVANCE_TIME_KEY] ?: 5
    }

    suspend fun saveUserName(name: String) {
        context.userSettingsDataStore.edit { preferences ->
            preferences[USER_NAME_KEY] = name
        }
    }

    suspend fun saveUserPhone(phone: String) {
        context.userSettingsDataStore.edit { preferences ->
            preferences[USER_PHONE_KEY] = phone
        }
    }

    suspend fun saveSoundEnabled(enabled: Boolean) {
        context.userSettingsDataStore.edit { preferences ->
            preferences[SOUND_ENABLED_KEY] = enabled
        }
    }

    suspend fun saveVibrationEnabled(enabled: Boolean) {
        context.userSettingsDataStore.edit { preferences ->
            preferences[VIBRATION_ENABLED_KEY] = enabled
        }
    }

    suspend fun saveCaregiverModeEnabled(enabled: Boolean) {
        context.userSettingsDataStore.edit { preferences ->
            preferences[CAREGIVER_MODE_ENABLED_KEY] = enabled
        }
    }

    suspend fun saveReminderAdvanceTime(timeMinutes: Int) {
        context.userSettingsDataStore.edit { preferences ->
            preferences[REMINDER_ADVANCE_TIME_KEY] = timeMinutes
        }
    }
}
