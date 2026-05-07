package com.codigomoo.calendariomedico.presentation.today

import com.codigomoo.calendariomedico.domain.model.IntakeStatus
import com.codigomoo.calendariomedico.domain.model.MedicationIntake
import com.codigomoo.calendariomedico.domain.model.TimeSlot
import java.time.LocalDate

data class AsNeededDialogState(
    val intake: MedicationIntake,
    val notes: String = "",
    val intervalWarning: String? = null
)

data class TodayUiState(
    val isLoading: Boolean = true,
    val date: LocalDate = LocalDate.now(),
    val treatmentName: String = "",
    val treatmentEndDate: LocalDate? = null,
    val intakesBySlot: Map<TimeSlot, List<MedicationIntake>> = emptyMap(),
    val asNeededIntakes: List<MedicationIntake> = emptyList(),
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
            intakesBySlot.values.flatten().none { it.status == IntakeStatus.PENDING }

    val isTreatmentExpired: Boolean
        get() = hasActiveTreatment && treatmentEndDate != null && treatmentEndDate.isBefore(LocalDate.now())
}
