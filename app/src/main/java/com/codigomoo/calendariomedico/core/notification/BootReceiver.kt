package com.codigomoo.calendariomedico.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.codigomoo.calendariomedico.domain.usecase.RescheduleRemindersUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var rescheduleRemindersUseCase: RescheduleRemindersUseCase

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                rescheduleRemindersUseCase()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
