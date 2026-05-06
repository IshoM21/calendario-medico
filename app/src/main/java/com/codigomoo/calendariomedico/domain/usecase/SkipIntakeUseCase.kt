package com.codigomoo.calendariomedico.domain.usecase

import com.codigomoo.calendariomedico.data.repository.IntakeRepository
import javax.inject.Inject

class SkipIntakeUseCase @Inject constructor(
    private val intakeRepository: IntakeRepository
) {
    suspend operator fun invoke(intakeId: Long, notes: String? = null) =
        intakeRepository.markSkipped(intakeId, notes)
}
