package com.codigomoo.calendariomedico.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class MedicationIntake(
    val id: Long = 0,
    val treatmentId: Long,
    val medicationId: Long,
    val medicationName: String,
    val date: LocalDate,
    val scheduledTimeSlot: TimeSlot,
    val scheduledTime: LocalTime? = null,
    val dose: String,
    val status: IntakeStatus,
    val confirmedAt: LocalDateTime? = null,
    val notes: String? = null
)
