package com.codigomoo.calendariomedico.domain.usecase

import com.codigomoo.calendariomedico.core.notification.AlarmScheduler
import com.codigomoo.calendariomedico.data.preferences.ReminderPreferences
import com.codigomoo.calendariomedico.data.repository.MedicationRepository
import com.codigomoo.calendariomedico.data.repository.TreatmentRepository
import com.codigomoo.calendariomedico.domain.model.TimeSlot
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

class RescheduleRemindersUseCase @Inject constructor(
    private val preferences: ReminderPreferences,
    private val treatmentRepository: TreatmentRepository,
    private val medicationRepository: MedicationRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke() {
        alarmScheduler.cancelAll()

        if (!preferences.notificationsEnabled.first()) return

        val treatment = treatmentRepository.getActive().first() ?: return
        if (LocalDate.now() > treatment.endDate) return

        val meds = medicationRepository.getByTreatmentOnce(treatment.id)

        // Cancel all existing per-med alarms first (cleans up stale ones)
        meds.forEach { alarmScheduler.cancelMedication(it.id) }

        // Schedule per-med alarms for meds with specificTime
        meds.filter { it.specificTime != null }.forEach { med ->
            alarmScheduler.scheduleMedication(med.id, med.specificTime!!)
        }

        // Schedule slot alarms (the receiver will filter out meds with specificTime at fire time)
        alarmScheduler.scheduleTimeSlot(TimeSlot.MORNING, preferences.morningTime.first())
        alarmScheduler.scheduleTimeSlot(TimeSlot.NOON, preferences.noonTime.first())
        alarmScheduler.scheduleTimeSlot(TimeSlot.NIGHT, preferences.nightTime.first())
    }
}
