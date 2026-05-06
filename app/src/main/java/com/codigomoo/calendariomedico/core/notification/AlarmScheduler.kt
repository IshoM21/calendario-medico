package com.codigomoo.calendariomedico.core.notification

import com.codigomoo.calendariomedico.domain.model.TimeSlot
import java.time.LocalTime

interface AlarmScheduler {
    fun scheduleTimeSlot(timeSlot: TimeSlot, time: LocalTime)
    fun cancelAll()
}
