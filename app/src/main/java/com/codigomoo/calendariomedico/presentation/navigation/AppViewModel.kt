package com.codigomoo.calendariomedico.presentation.navigation

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codigomoo.calendariomedico.core.notification.EXTRA_TIME_SLOT
import com.codigomoo.calendariomedico.core.notification.InAppReminderBus
import com.codigomoo.calendariomedico.core.notification.InAppReminderEvent
import com.codigomoo.calendariomedico.core.notification.ReminderAlarmReceiver
import com.codigomoo.calendariomedico.core.notification.SNOOZE_REQUEST_CODE
import com.codigomoo.calendariomedico.data.repository.IntakeRepository
import com.codigomoo.calendariomedico.domain.model.MedicationIntake
import com.codigomoo.calendariomedico.domain.model.TimeSlot
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val reminderBus: InAppReminderBus,
    private val intakeRepository: IntakeRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Expires events older than 15 minutes (stale alarm, app was in background)
    val reminder: StateFlow<InAppReminderEvent?> = reminderBus.event
        .map { event ->
            event?.takeIf { Duration.between(it.firedAt, LocalDateTime.now()).toMinutes() < 15 }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _takenLabel = MutableStateFlow<String?>(null)
    val takenLabel: StateFlow<String?> = _takenLabel.asStateFlow()

    fun dismissReminder() { reminderBus.clear() }

    fun snooze(timeSlot: TimeSlot) {
        reminderBus.clear()
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(EXTRA_TIME_SLOT, timeSlot.name)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, SNOOZE_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerTime = System.currentTimeMillis() + 10 * 60 * 1000L
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    fun markAllTaken(intakes: List<MedicationIntake>) {
        reminderBus.clear()
        _takenLabel.value = if (intakes.size == 1) "${intakes[0].medicationName} ${intakes[0].dose}"
                            else "${intakes.size} medicamentos"
        viewModelScope.launch {
            intakes.forEach { intakeRepository.markTaken(it.id) }
        }
    }

    fun consumeTakenLabel() { _takenLabel.value = null }
}
