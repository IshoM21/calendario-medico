package com.codigomoo.calendariomedico.core.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.codigomoo.calendariomedico.domain.usecase.CheckPendingDosesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class PendingDoseWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val checkPendingDosesUseCase: CheckPendingDosesUseCase,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val pending = checkPendingDosesUseCase()
        if (pending.isNotEmpty()) {
            notificationHelper.showPendingNotification(pending)
        }
        return Result.success()
    }
}
