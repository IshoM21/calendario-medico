package com.codigomoo.calendariomedico.domain.model

import java.time.LocalDateTime

data class Medication(
    val id: Long = 0,
    val treatmentId: Long,
    val name: String,
    val dose: String,
    val instructions: String? = null,
    val timeSlot: TimeSlot,
    val isRequired: Boolean = true,
    val colorHex: String? = null,
    val minIntervalHours: Int? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
