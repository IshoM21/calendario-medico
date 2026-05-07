package com.codigomoo.calendariomedico.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codigomoo.calendariomedico.data.preferences.ReminderPreferences
import com.codigomoo.calendariomedico.domain.usecase.RescheduleRemindersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

data class SettingsUiState(
    val morningTime: LocalTime = LocalTime.of(8, 0),
    val noonTime: LocalTime = LocalTime.of(13, 0),
    val nightTime: LocalTime = LocalTime.of(21, 0),
    val pendingAlertDelayMinutes: Int = 30,
    val notificationsEnabled: Boolean = true,
    val patientName: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: ReminderPreferences,
    private val rescheduleRemindersUseCase: RescheduleRemindersUseCase
) : ViewModel() {

    private val timesFlow = combine(
        preferences.morningTime,
        preferences.noonTime,
        preferences.nightTime
    ) { m, n, ni -> Triple(m, n, ni) }

    private val alertFlow = combine(
        preferences.pendingAlertDelayMinutes,
        preferences.notificationsEnabled
    ) { d, e -> Pair(d, e) }

    val uiState: StateFlow<SettingsUiState> = combine(
        timesFlow, alertFlow, preferences.patientName
    ) { times, alert, name ->
        SettingsUiState(
            morningTime = times.first,
            noonTime = times.second,
            nightTime = times.third,
            pendingAlertDelayMinutes = alert.first,
            notificationsEnabled = alert.second,
            patientName = name,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setMorningTime(time: LocalTime) {
        viewModelScope.launch { preferences.setMorningTime(time); rescheduleRemindersUseCase() }
    }

    fun setNoonTime(time: LocalTime) {
        viewModelScope.launch { preferences.setNoonTime(time); rescheduleRemindersUseCase() }
    }

    fun setNightTime(time: LocalTime) {
        viewModelScope.launch { preferences.setNightTime(time); rescheduleRemindersUseCase() }
    }

    fun setPendingAlertDelayMinutes(minutes: Int) {
        viewModelScope.launch { preferences.setPendingAlertDelayMinutes(minutes) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setNotificationsEnabled(enabled); rescheduleRemindersUseCase() }
    }

    fun setPatientName(name: String) {
        viewModelScope.launch { preferences.setPatientName(name) }
    }
}
