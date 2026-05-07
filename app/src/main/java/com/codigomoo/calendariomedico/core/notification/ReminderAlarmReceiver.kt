package com.codigomoo.calendariomedico.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.codigomoo.calendariomedico.data.preferences.ReminderPreferences
import com.codigomoo.calendariomedico.data.repository.IntakeRepository
import com.codigomoo.calendariomedico.domain.model.IntakeStatus
import com.codigomoo.calendariomedico.domain.model.TimeSlot
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var intakeRepository: IntakeRepository
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var alarmScheduler: AlarmScheduler
    @Inject lateinit var preferences: ReminderPreferences

    override fun onReceive(context: Context, intent: Intent) {
        val timeSlotName = intent.getStringExtra(EXTRA_TIME_SLOT) ?: return
        val timeSlot = runCatching { TimeSlot.valueOf(timeSlotName) }.getOrNull() ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val today = LocalDate.now()
                val intakes = intakeRepository.getByDateOnce(today)
                    .filter { it.scheduledTimeSlot == timeSlot && it.status == IntakeStatus.PENDING }

                notificationHelper.showReminderNotification(timeSlot, intakes)

                val slotTime = when (timeSlot) {
                    TimeSlot.MORNING -> preferences.morningTime.first()
                    TimeSlot.NOON -> preferences.noonTime.first()
                    TimeSlot.NIGHT -> preferences.nightTime.first()
                    TimeSlot.AS_NEEDED -> return@launch
                }
                alarmScheduler.scheduleTimeSlot(timeSlot, slotTime)

                val delayMinutes = preferences.pendingAlertDelayMinutes.first().toLong()
                val workRequest = OneTimeWorkRequestBuilder<PendingDoseWorker>()
                    .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                    .build()
                WorkManager.getInstance(context).enqueue(workRequest)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
