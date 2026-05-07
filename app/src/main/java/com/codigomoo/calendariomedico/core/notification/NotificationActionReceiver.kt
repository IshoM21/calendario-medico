package com.codigomoo.calendariomedico.core.notification

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.codigomoo.calendariomedico.data.repository.IntakeRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var intakeRepository: IntakeRepository

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val notificationManager = context.getSystemService(NotificationManager::class.java)

        when (intent.action) {
            ACTION_MARK_TAKEN -> {
                val intakeIds = intent.getLongArrayExtra(EXTRA_INTAKE_IDS) ?: return
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        intakeIds.forEach { id -> intakeRepository.markTaken(id) }
                        notificationManager.cancel(notificationId)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_SNOOZE -> {
                notificationManager.cancel(notificationId)
                val timeSlotName = intent.getStringExtra(EXTRA_TIME_SLOT) ?: return
                val alarmManager = context.getSystemService(AlarmManager::class.java)
                val snoozeIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
                    putExtra(EXTRA_TIME_SLOT, timeSlotName)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context, SNOOZE_REQUEST_CODE, snoozeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val triggerTime = System.currentTimeMillis() + 10 * 60 * 1000L
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            }
        }
    }
}
