package com.codigomoo.calendariomedico.domain.usecase

import com.codigomoo.calendariomedico.data.preferences.ReminderPreferences
import com.codigomoo.calendariomedico.data.repository.IntakeRepository
import com.codigomoo.calendariomedico.domain.model.MedicationIntake
import com.codigomoo.calendariomedico.domain.model.TimeSlot
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

class CheckPendingDosesUseCase @Inject constructor(
    private val intakeRepository: IntakeRepository,
    private val preferences: ReminderPreferences
) {
    suspend operator fun invoke(): List<MedicationIntake> {
        val delayMinutes = preferences.pendingAlertDelayMinutes.first().toLong()
        val morningTime = preferences.morningTime.first()
        val noonTime = preferences.noonTime.first()
        val nightTime = preferences.nightTime.first()
        val now = LocalDateTime.now()

        return intakeRepository.getPendingUntil(LocalDate.now()).filter { intake ->
            val slotTime: LocalTime = when (intake.scheduledTimeSlot) {
                TimeSlot.MORNING -> morningTime
                TimeSlot.NOON -> noonTime
                TimeSlot.NIGHT -> nightTime
                TimeSlot.AS_NEEDED -> return@filter false
            }
            val cutoff = LocalDateTime.of(intake.date, slotTime).plusMinutes(delayMinutes)
            now.isAfter(cutoff)
        }
    }
}
