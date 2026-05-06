package com.codigomoo.calendariomedico.data.local.dao

import androidx.room.*
import com.codigomoo.calendariomedico.data.local.entity.MedicationScheduleEntity
import java.time.DayOfWeek

@Dao
interface ScheduleDao {

    @Query("SELECT * FROM medication_schedules WHERE medicationId = :medicationId")
    suspend fun getByMedication(medicationId: Long): List<MedicationScheduleEntity>

    @Query("SELECT * FROM medication_schedules WHERE medicationId = :medicationId AND dayOfWeek = :dayOfWeek")
    suspend fun getByMedicationAndDay(medicationId: Long, dayOfWeek: DayOfWeek): MedicationScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(schedules: List<MedicationScheduleEntity>)

    @Query("DELETE FROM medication_schedules WHERE medicationId = :medicationId")
    suspend fun deleteByMedication(medicationId: Long)
}
