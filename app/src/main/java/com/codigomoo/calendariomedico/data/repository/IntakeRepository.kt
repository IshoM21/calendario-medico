package com.codigomoo.calendariomedico.data.repository

import com.codigomoo.calendariomedico.data.local.dao.IntakeDao
import com.codigomoo.calendariomedico.data.local.entity.MedicationIntakeEntity
import com.codigomoo.calendariomedico.domain.model.IntakeStatus
import com.codigomoo.calendariomedico.domain.model.MedicationIntake
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntakeRepository @Inject constructor(
    private val dao: IntakeDao
) {

    fun getByDate(date: LocalDate): Flow<List<MedicationIntake>> =
        dao.getByDate(date).map { list -> list.map { it.toDomain() } }

    suspend fun getByDateOnce(date: LocalDate): List<MedicationIntake> =
        dao.getByDateOnce(date).map { it.toDomain() }

    fun getByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<MedicationIntake>> =
        dao.getByDateRange(startDate, endDate).map { list -> list.map { it.toDomain() } }

    fun getByMedicationAndDateRange(medicationId: Long, startDate: LocalDate, endDate: LocalDate): Flow<List<MedicationIntake>> =
        dao.getByMedicationAndDateRange(medicationId, startDate, endDate).map { list -> list.map { it.toDomain() } }

    suspend fun getPendingUntil(date: LocalDate): List<MedicationIntake> =
        dao.getPendingUntil(IntakeStatus.PENDING, date).map { it.toDomain() }

    suspend fun getLastTaken(medicationId: Long): MedicationIntake? =
        dao.getLastTaken(medicationId, IntakeStatus.TAKEN)?.toDomain()

    suspend fun insertAll(intakes: List<MedicationIntake>) =
        dao.insertAll(intakes.map { MedicationIntakeEntity.fromDomain(it) })

    suspend fun insertAllIfAbsent(intakes: List<MedicationIntake>) =
        dao.insertAllIfAbsent(intakes.map { MedicationIntakeEntity.fromDomain(it) })

    suspend fun markTaken(id: Long, notes: String? = null) =
        dao.updateStatus(id, IntakeStatus.TAKEN, LocalDateTime.now(), notes)

    suspend fun markSkipped(id: Long, notes: String? = null) =
        dao.updateStatus(id, IntakeStatus.SKIPPED, LocalDateTime.now(), notes)

    suspend fun markMissed(id: Long) =
        dao.updateStatus(id, IntakeStatus.MISSED, null, null)

    suspend fun existsForDay(medicationId: Long, date: LocalDate): Boolean =
        dao.existsForDay(medicationId, date)

    suspend fun deleteFuturePending(treatmentId: Long, fromDate: LocalDate) =
        dao.deleteFuturePending(treatmentId, fromDate)

    suspend fun deleteFuturePendingByMedication(medicationId: Long, fromDate: LocalDate) =
        dao.deleteFuturePendingByMedication(medicationId, fromDate)

    suspend fun markPendingBeforeDateAsMissed(date: LocalDate) =
        dao.markPendingBeforeDateAsMissed(date)
}
