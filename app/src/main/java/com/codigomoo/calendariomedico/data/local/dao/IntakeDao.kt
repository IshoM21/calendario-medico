package com.codigomoo.calendariomedico.data.local.dao

import androidx.room.*
import com.codigomoo.calendariomedico.data.local.entity.MedicationIntakeEntity
import com.codigomoo.calendariomedico.domain.model.IntakeStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

@Dao
interface IntakeDao {

    @Query("SELECT * FROM medication_intakes WHERE date = :date ORDER BY scheduledTimeSlot ASC")
    fun getByDate(date: LocalDate): Flow<List<MedicationIntakeEntity>>

    @Query("SELECT * FROM medication_intakes WHERE date = :date ORDER BY scheduledTimeSlot ASC")
    suspend fun getByDateOnce(date: LocalDate): List<MedicationIntakeEntity>

    @Query("SELECT * FROM medication_intakes WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, scheduledTimeSlot ASC")
    fun getByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<MedicationIntakeEntity>>

    @Query("SELECT * FROM medication_intakes WHERE status = :status AND date <= :date")
    suspend fun getPendingUntil(status: IntakeStatus, date: LocalDate): List<MedicationIntakeEntity>

    @Query("SELECT * FROM medication_intakes WHERE medicationId = :medicationId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getByMedicationAndDateRange(medicationId: Long, startDate: LocalDate, endDate: LocalDate): Flow<List<MedicationIntakeEntity>>

    @Query("SELECT * FROM medication_intakes WHERE medicationId = :medicationId AND status = :status ORDER BY confirmedAt DESC LIMIT 1")
    suspend fun getLastTaken(medicationId: Long, status: IntakeStatus): MedicationIntakeEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(intakes: List<MedicationIntakeEntity>)

    @Query("UPDATE medication_intakes SET status = :status, confirmedAt = :confirmedAt, notes = :notes WHERE id = :id")
    suspend fun updateStatus(id: Long, status: IntakeStatus, confirmedAt: LocalDateTime?, notes: String?)

    @Query("SELECT EXISTS(SELECT 1 FROM medication_intakes WHERE medicationId = :medicationId AND date = :date)")
    suspend fun existsForDay(medicationId: Long, date: LocalDate): Boolean

    @Query("DELETE FROM medication_intakes WHERE treatmentId = :treatmentId AND date >= :fromDate AND (status = 'PENDING' OR status = 'OPTIONAL')")
    suspend fun deleteFuturePending(treatmentId: Long, fromDate: LocalDate)
}
