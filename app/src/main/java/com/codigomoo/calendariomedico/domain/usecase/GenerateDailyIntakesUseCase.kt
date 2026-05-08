package com.codigomoo.calendariomedico.domain.usecase

import com.codigomoo.calendariomedico.data.repository.IntakeRepository
import com.codigomoo.calendariomedico.data.repository.MedicationRepository
import com.codigomoo.calendariomedico.data.repository.TreatmentRepository
import com.codigomoo.calendariomedico.domain.model.IntakeStatus
import com.codigomoo.calendariomedico.domain.model.MedicationIntake
import com.codigomoo.calendariomedico.domain.model.TimeSlot

import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

class GenerateDailyIntakesUseCase @Inject constructor(
    private val treatmentRepository: TreatmentRepository,
    private val medicationRepository: MedicationRepository,
    private val intakeRepository: IntakeRepository
) {
    suspend operator fun invoke(date: LocalDate) {
        val treatment = treatmentRepository.getActive().first() ?: return
        if (date < treatment.startDate || date > treatment.endDate) return

        val medications = medicationRepository.getByTreatmentOnce(treatment.id)
        val toInsert = mutableListOf<MedicationIntake>()

        for (medication in medications) {
            if (medication.timeSlot == TimeSlot.AS_NEEDED) continue
            if (intakeRepository.existsForDay(medication.id, date)) continue

            val schedules = medicationRepository.getSchedules(medication.id)
            val isScheduledToday = schedules.isEmpty() || schedules.any { it.dayOfWeek == date.dayOfWeek }
            if (!isScheduledToday) continue

            val doseOverride = schedules.find { it.dayOfWeek == date.dayOfWeek }?.doseOverride
            val dose = doseOverride ?: medication.dose
            val status = when {
                !medication.isRequired -> IntakeStatus.OPTIONAL
                date < LocalDate.now() -> IntakeStatus.MISSED
                else -> IntakeStatus.PENDING
            }

            toInsert += MedicationIntake(
                treatmentId = treatment.id,
                medicationId = medication.id,
                medicationName = medication.name,
                date = date,
                scheduledTimeSlot = medication.timeSlot,
                dose = dose,
                status = status
            )
        }

        if (toInsert.isNotEmpty()) intakeRepository.insertAll(toInsert)
    }
}
