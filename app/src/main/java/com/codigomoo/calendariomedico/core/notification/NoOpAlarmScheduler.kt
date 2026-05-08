package com.codigomoo.calendariomedico.core.notification

import com.codigomoo.calendariomedico.domain.model.TimeSlot
import java.time.LocalTime
import javax.inject.Inject

class NoOpAlarmScheduler @Inject constructor() : AlarmScheduler {
    override fun scheduleTimeSlot(timeSlot: TimeSlot, time: LocalTime) = Unit
    override fun scheduleMedication(medicationId: Long, time: LocalTime) = Unit
    override fun cancelMedication(medicationId: Long) = Unit
    override fun cancelAll() = Unit
}
