package com.codigomoo.calendariomedico.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.codigomoo.calendariomedico.MainActivity
import com.codigomoo.calendariomedico.R
import com.codigomoo.calendariomedico.domain.model.MedicationIntake
import com.codigomoo.calendariomedico.domain.model.TimeSlot
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

const val CHANNEL_ID = "medication_reminders"
const val EXTRA_TIME_SLOT = "extra_time_slot"
const val EXTRA_INTAKE_IDS = "extra_intake_ids"
const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
const val ACTION_MARK_TAKEN = "com.codigomoo.calendariomedico.ACTION_MARK_TAKEN"
const val ACTION_SNOOZE = "com.codigomoo.calendariomedico.ACTION_SNOOZE"
const val SNOOZE_REQUEST_CODE = 9001
const val PENDING_NOTIFICATION_ID = 2001

fun timeSlotNotificationId(slot: TimeSlot) = when (slot) {
    TimeSlot.MORNING -> 1001
    TimeSlot.NOON -> 1002
    TimeSlot.NIGHT -> 1003
    TimeSlot.AS_NEEDED -> 1004
}

fun timeSlotAlarmRequestCode(slot: TimeSlot) = when (slot) {
    TimeSlot.MORNING -> 101
    TimeSlot.NOON -> 102
    TimeSlot.NIGHT -> 103
    TimeSlot.AS_NEEDED -> 104
}

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Recordatorios de medicamentos",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alertas para tomar medicamentos a tiempo"
        }
        manager.createNotificationChannel(channel)
    }

    fun showReminderNotification(timeSlot: TimeSlot, intakes: List<MedicationIntake>) {
        if (intakes.isEmpty()) return
        val notificationId = timeSlotNotificationId(timeSlot)
        val title = timeSlotTitle(timeSlot)
        val body = intakes.joinToString(", ") { "${it.medicationName} ${it.dose}" }
        val intakeIds = intakes.map { it.id }.toLongArray()

        val tapPendingIntent = buildTapPendingIntent()
        val markTakenAction = buildMarkTakenAction(notificationId, intakeIds)
        val snoozeAction = buildSnoozeAction(notificationId, timeSlot)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(tapPendingIntent)
            .addAction(markTakenAction)
            .addAction(snoozeAction)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(notificationId, notification)
    }

    fun showPendingNotification(intakes: List<MedicationIntake>) {
        if (intakes.isEmpty()) return
        val body = intakes.joinToString(", ") { it.medicationName }

        val tapPendingIntent = buildTapPendingIntent()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Tomas pendientes sin registrar")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(tapPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(PENDING_NOTIFICATION_ID, notification)
    }

    private fun buildTapPendingIntent(): PendingIntent {
        val intent = Intent(
            Intent.ACTION_VIEW,
            "calendariomedico://today".toUri(),
            context,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildMarkTakenAction(
        notificationId: Int,
        intakeIds: LongArray
    ): NotificationCompat.Action {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_MARK_TAKEN
            putExtra(EXTRA_INTAKE_IDS, intakeIds)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, notificationId * 10,
            intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action(0, "Tomado ✓", pendingIntent)
    }

    private fun buildSnoozeAction(
        notificationId: Int,
        timeSlot: TimeSlot
    ): NotificationCompat.Action {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_TIME_SLOT, timeSlot.name)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, notificationId * 10 + 1,
            intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action(0, "En 10 min ⏰", pendingIntent)
    }

    private fun timeSlotTitle(slot: TimeSlot) = when (slot) {
        TimeSlot.MORNING -> "Medicamentos de Mañana"
        TimeSlot.NOON -> "Medicamentos de Comida"
        TimeSlot.NIGHT -> "Medicamentos de Noche"
        TimeSlot.AS_NEEDED -> "Medicamentos"
    }
}
