package com.codigomoo.calendariomedico.domain.model

import java.time.DayOfWeek

data class MedicationSchedule(
    val id: Long = 0,
    val medicationId: Long,
    val dayOfWeek: DayOfWeek,
    val doseOverride: String? = null
)
