package com.codigomoo.calendariomedico.domain.usecase

import com.codigomoo.calendariomedico.data.repository.IntakeRepository
import com.codigomoo.calendariomedico.domain.model.MedicationIntake
import com.codigomoo.calendariomedico.domain.model.TimeSlot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class GetTodayIntakesUseCase @Inject constructor(
    private val generateDailyIntakesUseCase: GenerateDailyIntakesUseCase,
    private val intakeRepository: IntakeRepository
) {
    suspend operator fun invoke(): Flow<Map<TimeSlot, List<MedicationIntake>>> {
        val today = LocalDate.now()
        generateDailyIntakesUseCase(today)
        return intakeRepository.getByDate(today).map { intakes ->
            TimeSlot.entries.associateWith { slot ->
                intakes.filter { it.scheduledTimeSlot == slot }
            }.filterValues { it.isNotEmpty() }
        }
    }
}
