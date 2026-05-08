package com.codigomoo.calendariomedico.core.notification

import com.codigomoo.calendariomedico.domain.model.TimeSlot
import java.time.LocalTime

interface AlarmScheduler {
    fun scheduleTimeSlot(timeSlot: TimeSlot, time: LocalTime)
    fun scheduleMedication(medicationId: Long, time: LocalTime)
    fun cancelMedication(medicationId: Long)
    fun cancelAll()
}
