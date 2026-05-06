package com.codigomoo.calendariomedico.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

data class Treatment(
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
