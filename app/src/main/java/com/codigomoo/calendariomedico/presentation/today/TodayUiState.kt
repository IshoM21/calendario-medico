package com.codigomoo.calendariomedico.presentation.today

import com.codigomoo.calendariomedico.domain.model.IntakeStatus
import com.codigomoo.calendariomedico.domain.model.MedicationIntake
import com.codigomoo.calendariomedico.domain.model.TimeSlot
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class AsNeededItem(
    val medicationId: Long,
    val treatmentId: Long,
    val medicationName: String,
    val dose: String,
    val takenToday: List<MedicationIntake> = emptyList()
)

data class AsNeededDialogState(
    val medicationId: Long,
    val treatmentId: Long,
    val medicationName: String,
    val dose: String,
    val notes: String = "",
    val intervalWarning: String? = null
)

data class SlotMedItem(
    val intake: MedicationIntake,
    val colorHex: String? = null,
    val instructions: String? = null,
    val specificTime: java.time.LocalTime? = null
)

data class NextIntakeInfo(
    val intake: MedicationIntake,
    val instructions: String? = null,
    val specificTime: java.time.LocalTime? = null
)

data class SlotTimes(
    val morning: LocalTime = LocalTime.of(8, 0),
    val noon: LocalTime = LocalTime.of(13, 0),
    val night: LocalTime = LocalTime.of(21, 0)
) {
    private val fmt = DateTimeFormatter.ofPattern("H:mm")

    fun timeOf(slot: TimeSlot): LocalTime = when (slot) {
        TimeSlot.MORNING -> morning
        TimeSlot.NOON -> noon
        TimeSlot.NIGHT -> night
        TimeSlot.AS_NEEDED -> LocalTime.MAX
    }

    fun rangeLabel(slot: TimeSlot): String = when (slot) {
        TimeSlot.MORNING -> "${morning.format(fmt)} – ${noon.minusMinutes(1).format(fmt)}"
        TimeSlot.NOON -> "${noon.format(fmt)} – ${night.minusMinutes(1).format(fmt)}"
        TimeSlot.NIGHT -> "${night.format(fmt)} – 23:59"
        TimeSlot.AS_NEEDED -> ""
    }
}

data class TakenConfirmData(
    val label: String,
    val confirmedAtTime: String,
    val nextMedicationName: String = "",
    val nextSlotTime: String = ""
)

data class TodayUiState(
    val isLoading: Boolean = true,
    val date: LocalDate = LocalDate.now(),
    val treatmentName: String = "",
    val treatmentEndDate: LocalDate? = null,
    val patientName: String = "",
    val slotTimes: SlotTimes = SlotTimes(),
    val currentTime: LocalTime = LocalTime.now(),
    val orderedSlots: List<Pair<TimeSlot, List<SlotMedItem>>> = emptyList(),
    val nextIntakeInfo: NextIntakeInfo? = null,
    val nextSlotPendingIntakes: List<MedicationIntake> = emptyList(),
    val markTakenDialog: List<MedicationIntake>? = null,
    val navigateToConfirmation: TakenConfirmData? = null,
    val expandedSlots: Set<TimeSlot> = emptySet(),
    val asNeededItems: List<AsNeededItem> = emptyList(),
    val asNeededExpanded: Boolean = true,
    val asNeededDialog: AsNeededDialogState? = null,
    val totalRequired: Int = 0,
    val totalTaken: Int = 0,
    val hasActiveTreatment: Boolean = false,
    val isFirstLaunch: Boolean = false,
    val error: String? = null
) {
    val progressFraction: Float
        get() = if (totalRequired == 0) 0f else totalTaken.toFloat() / totalRequired

    val isAllDone: Boolean
        get() = hasActiveTreatment && totalRequired > 0 &&
            orderedSlots.flatMap { it.second }.none { it.intake.status == IntakeStatus.PENDING }

    val isTreatmentExpired: Boolean
        get() = hasActiveTreatment && treatmentEndDate != null && treatmentEndDate.isBefore(LocalDate.now())
}
