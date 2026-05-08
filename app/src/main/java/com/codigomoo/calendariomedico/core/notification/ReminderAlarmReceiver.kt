package com.codigomoo.calendariomedico.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.codigomoo.calendariomedico.data.preferences.ReminderPreferences
import com.codigomoo.calendariomedico.data.repository.IntakeRepository
import com.codigomoo.calendariomedico.data.repository.MedicationRepository
import com.codigomoo.calendariomedico.domain.model.IntakeStatus
import com.codigomoo.calendariomedico.domain.model.TimeSlot
import com.codigomoo.calendariomedico.domain.usecase.GenerateDailyIntakesUseCase
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
    @Inject lateinit var medicationRepository: MedicationRepository
    @Inject lateinit var generateDailyIntakesUseCase: GenerateDailyIntakesUseCase
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var alarmScheduler: AlarmScheduler
    @Inject lateinit var preferences: ReminderPreferences
    @Inject lateinit var reminderBus: InAppReminderBus

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val medicationId = intent.getLongExtra(EXTRA_MEDICATION_ID, -1L)
                if (medicationId != -1L) {
                    handleMedicationAlarm(context, medicationId)
                } else {
                    val timeSlotName = intent.getStringExtra(EXTRA_TIME_SLOT) ?: return@launch
                    val timeSlot = runCatching { TimeSlot.valueOf(timeSlotName) }.getOrNull() ?: return@launch
                    handleSlotAlarm(context, timeSlot)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleMedicationAlarm(context: Context, medicationId: Long) {
        val today = LocalDate.now()
        generateDailyIntakesUseCase(today)

        val intake = intakeRepository.getByDateOnce(today)
            .firstOrNull { it.medicationId == medicationId && it.status == IntakeStatus.PENDING }
            ?: return

        notificationHelper.showMedicationNotification(intake)
        reminderBus.post(
            InAppReminderEvent(
                timeSlot = intake.scheduledTimeSlot,
                intakes = listOf(intake),
                medicationId = medicationId
            )
        )

        val medication = medicationRepository.getById(medicationId) ?: return
        val specificTime = medication.specificTime ?: return
        alarmScheduler.scheduleMedication(medicationId, specificTime)

        scheduleWorkManager(context)
    }

    private suspend fun handleSlotAlarm(context: Context, timeSlot: TimeSlot) {
        val today = LocalDate.now()
        generateDailyIntakesUseCase(today)

        val allIntakes = intakeRepository.getByDateOnce(today)
            .filter { it.scheduledTimeSlot == timeSlot && it.status == IntakeStatus.PENDING }

        // Exclude meds that have their own specific-time alarm
        val medsWithSpecificTime = if (allIntakes.isNotEmpty()) {
            val treatmentId = allIntakes.first().treatmentId
            medicationRepository.getByTreatmentOnce(treatmentId)
                .filter { it.specificTime != null }.map { it.id }.toSet()
        } else emptySet()

        val intakes = allIntakes.filter { it.medicationId !in medsWithSpecificTime }

        notificationHelper.showReminderNotification(timeSlot, intakes)
        if (intakes.isNotEmpty()) {
            reminderBus.post(InAppReminderEvent(timeSlot = timeSlot, intakes = intakes))
        }

        val slotTime = when (timeSlot) {
            TimeSlot.MORNING -> preferences.morningTime.first()
            TimeSlot.NOON -> preferences.noonTime.first()
            TimeSlot.NIGHT -> preferences.nightTime.first()
            TimeSlot.AS_NEEDED -> return
        }
        alarmScheduler.scheduleTimeSlot(timeSlot, slotTime)

        scheduleWorkManager(context)
    }

    private suspend fun scheduleWorkManager(context: Context) {
        val delayMinutes = preferences.pendingAlertDelayMinutes.first().toLong()
        val workRequest = OneTimeWorkRequestBuilder<PendingDoseWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
