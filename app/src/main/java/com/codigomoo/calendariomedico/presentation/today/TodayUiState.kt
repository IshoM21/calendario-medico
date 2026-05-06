package com.codigomoo.calendariomedico.presentation.today

import com.codigomoo.calendariomedico.domain.model.IntakeStatus
import com.codigomoo.calendariomedico.domain.model.MedicationIntake
import com.codigomoo.calendariomedico.domain.model.TimeSlot
import java.time.LocalDate

data class TodayUiState(
    val isLoading: Boolean = true,
    val date: LocalDate = LocalDate.now(),
    val treatmentName: String = "",
    val intakesBySlot: Map<TimeSlot, List<MedicationIntake>> = emptyMap(),
    val totalRequired: Int = 0,
    val totalTaken: Int = 0,
    val hasActiveTreatment: Boolean = false,
    val error: String? = null
) {
    val progressFraction: Float
        get() = if (totalRequired == 0) 0f else totalTaken.toFloat() / totalRequired

    val isAllDone: Boolean
        get() = hasActiveTreatment && totalRequired > 0 &&
            intakesBySlot.values.flatten().none { it.status == IntakeStatus.PENDING }
}
