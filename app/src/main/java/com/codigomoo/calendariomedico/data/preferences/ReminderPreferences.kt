package com.codigomoo.calendariomedico.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "reminder_prefs")

@Singleton
class ReminderPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object Keys {
        val MORNING_TIME = stringPreferencesKey("morning_time")
        val NOON_TIME = stringPreferencesKey("noon_time")
        val NIGHT_TIME = stringPreferencesKey("night_time")
        val PENDING_ALERT_DELAY_MINUTES = intPreferencesKey("pending_alert_delay_minutes")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val PATIENT_NAME = stringPreferencesKey("patient_name")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val FIRST_DAY_OF_WEEK = intPreferencesKey("first_day_of_week")
    }

    val morningTime: Flow<LocalTime> = context.dataStore.data.map { prefs ->
        prefs[Keys.MORNING_TIME]?.let { LocalTime.parse(it) } ?: LocalTime.of(8, 0)
    }

    val noonTime: Flow<LocalTime> = context.dataStore.data.map { prefs ->
        prefs[Keys.NOON_TIME]?.let { LocalTime.parse(it) } ?: LocalTime.of(13, 0)
    }

    val nightTime: Flow<LocalTime> = context.dataStore.data.map { prefs ->
        prefs[Keys.NIGHT_TIME]?.let { LocalTime.parse(it) } ?: LocalTime.of(21, 0)
    }

    val pendingAlertDelayMinutes: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.PENDING_ALERT_DELAY_MINUTES] ?: 30
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.NOTIFICATIONS_ENABLED] ?: true
    }

    val patientName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.PATIENT_NAME] ?: ""
    }

    suspend fun setMorningTime(time: LocalTime) {
        context.dataStore.edit { it[Keys.MORNING_TIME] = time.toString() }
    }

    suspend fun setNoonTime(time: LocalTime) {
        context.dataStore.edit { it[Keys.NOON_TIME] = time.toString() }
    }

    suspend fun setNightTime(time: LocalTime) {
        context.dataStore.edit { it[Keys.NIGHT_TIME] = time.toString() }
    }

    suspend fun setPendingAlertDelayMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.PENDING_ALERT_DELAY_MINUTES] = minutes }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setPatientName(name: String) {
        context.dataStore.edit { it[Keys.PATIENT_NAME] = name }
    }

    val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.HAS_COMPLETED_ONBOARDING] ?: false
    }

    suspend fun setHasCompletedOnboarding(value: Boolean) {
        context.dataStore.edit { it[Keys.HAS_COMPLETED_ONBOARDING] = value }
    }

    val firstDayOfWeek: Flow<DayOfWeek> = context.dataStore.data.map { prefs ->
        DayOfWeek.of(prefs[Keys.FIRST_DAY_OF_WEEK] ?: DayOfWeek.MONDAY.value)
    }

    suspend fun setFirstDayOfWeek(day: DayOfWeek) {
        context.dataStore.edit { it[Keys.FIRST_DAY_OF_WEEK] = day.value }
    }
}
