package com.codigomoo.calendariomedico.data.local.dao

import androidx.room.*
import com.codigomoo.calendariomedico.data.local.entity.MedicationIntakeEntity
import com.codigomoo.calendariomedico.domain.model.IntakeStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

@Dao
abstract class IntakeDao {

    @Query("SELECT * FROM medication_intakes WHERE date = :date ORDER BY scheduledTimeSlot ASC")
    abstract fun getByDate(date: LocalDate): Flow<List<MedicationIntakeEntity>>

    @Query("SELECT * FROM medication_intakes WHERE date = :date ORDER BY scheduledTimeSlot ASC")
    abstract suspend fun getByDateOnce(date: LocalDate): List<MedicationIntakeEntity>

    @Query("SELECT * FROM medication_intakes WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, scheduledTimeSlot ASC")
    abstract fun getByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<MedicationIntakeEntity>>

    @Query("SELECT * FROM medication_intakes WHERE status = :status AND date <= :date")
    abstract suspend fun getPendingUntil(status: IntakeStatus, date: LocalDate): List<MedicationIntakeEntity>

    @Query("SELECT * FROM medication_intakes WHERE medicationId = :medicationId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    abstract fun getByMedicationAndDateRange(medicationId: Long, startDate: LocalDate, endDate: LocalDate): Flow<List<MedicationIntakeEntity>>

    @Query("SELECT * FROM medication_intakes WHERE medicationId = :medicationId AND status = :status ORDER BY confirmedAt DESC LIMIT 1")
    abstract suspend fun getLastTaken(medicationId: Long, status: IntakeStatus): MedicationIntakeEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertAll(intakes: List<MedicationIntakeEntity>)

    @Query("UPDATE medication_intakes SET status = :status, confirmedAt = :confirmedAt, notes = :notes WHERE id = :id")
    abstract suspend fun updateStatus(id: Long, status: IntakeStatus, confirmedAt: LocalDateTime?, notes: String?)

    @Query("SELECT EXISTS(SELECT 1 FROM medication_intakes WHERE medicationId = :medicationId AND date = :date)")
    abstract suspend fun existsForDay(medicationId: Long, date: LocalDate): Boolean

    @Query("DELETE FROM medication_intakes WHERE treatmentId = :treatmentId AND date >= :fromDate AND (status = 'PENDING' OR status = 'OPTIONAL')")
    abstract suspend fun deleteFuturePending(treatmentId: Long, fromDate: LocalDate)

    @Query("DELETE FROM medication_intakes WHERE medicationId = :medicationId AND date >= :fromDate AND (status = 'PENDING' OR status = 'OPTIONAL')")
    abstract suspend fun deleteFuturePendingByMedication(medicationId: Long, fromDate: LocalDate)

    @Query("UPDATE medication_intakes SET status = 'MISSED' WHERE date < :date AND status = 'PENDING'")
    abstract suspend fun markPendingBeforeDateAsMissed(date: LocalDate)

    @Transaction
    open suspend fun insertAllIfAbsent(intakes: List<MedicationIntakeEntity>) {
        val toInsert = intakes.filter { !existsForDay(it.medicationId, it.date) }
        if (toInsert.isNotEmpty()) insertAll(toInsert)
    }
}
