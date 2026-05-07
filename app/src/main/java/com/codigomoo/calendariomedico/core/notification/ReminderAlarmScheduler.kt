package com.codigomoo.calendariomedico.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.codigomoo.calendariomedico.domain.model.TimeSlot
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun scheduleTimeSlot(timeSlot: TimeSlot, time: LocalTime) {
        if (timeSlot == TimeSlot.AS_NEEDED) return
        val triggerMillis = nextTriggerMillis(time)
        val pendingIntent = buildSchedulePendingIntent(timeSlot)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    }

    override fun cancelAll() {
        listOf(TimeSlot.MORNING, TimeSlot.NOON, TimeSlot.NIGHT).forEach { slot ->
            val intent = Intent(context, ReminderAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, timeSlotAlarmRequestCode(slot), intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let { alarmManager.cancel(it) }
        }
    }

    private fun buildSchedulePendingIntent(timeSlot: TimeSlot): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(EXTRA_TIME_SLOT, timeSlot.name)
        }
        return PendingIntent.getBroadcast(
            context, timeSlotAlarmRequestCode(timeSlot), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextTriggerMillis(time: LocalTime): Long {
        val now = ZonedDateTime.now()
        var trigger = LocalDate.now().atTime(time).atZone(ZoneId.systemDefault())
        if (!trigger.isAfter(now)) trigger = trigger.plusDays(1)
        return trigger.toInstant().toEpochMilli()
    }
}
