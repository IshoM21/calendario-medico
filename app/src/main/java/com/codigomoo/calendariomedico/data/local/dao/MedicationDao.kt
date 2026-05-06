package com.codigomoo.calendariomedico.data.local.dao

import androidx.room.*
import com.codigomoo.calendariomedico.data.local.entity.MedicationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {

    @Query("SELECT * FROM medications WHERE treatmentId = :treatmentId ORDER BY name ASC")
    fun getByTreatment(treatmentId: Long): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE treatmentId = :treatmentId ORDER BY name ASC")
    suspend fun getByTreatmentOnce(treatmentId: Long): List<MedicationEntity>

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getById(id: Long): MedicationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medication: MedicationEntity): Long

    @Update
    suspend fun update(medication: MedicationEntity)

    @Delete
    suspend fun delete(medication: MedicationEntity)
}
