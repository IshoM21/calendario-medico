package com.codigomoo.calendariomedico.core.notification

import com.codigomoo.calendariomedico.domain.model.MedicationIntake
import com.codigomoo.calendariomedico.domain.model.TimeSlot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

data class InAppReminderEvent(
    val timeSlot: TimeSlot,
    val intakes: List<MedicationIntake>,
    val firedAt: LocalDateTime = LocalDateTime.now()
)

@Singleton
class InAppReminderBus @Inject constructor() {
    private val _event = MutableStateFlow<InAppReminderEvent?>(null)
    val event: StateFlow<InAppReminderEvent?> = _event.asStateFlow()

    fun post(event: InAppReminderEvent) { _event.value = event }
    fun clear() { _event.value = null }
}
