package com.codigomoo.calendariomedico.domain.usecase

import com.codigomoo.calendariomedico.data.repository.IntakeRepository
import javax.inject.Inject

class MarkIntakeAsTakenUseCase @Inject constructor(
    private val intakeRepository: IntakeRepository
) {
    suspend operator fun invoke(intakeId: Long, notes: String? = null) =
        intakeRepository.markTaken(intakeId, notes)
}
