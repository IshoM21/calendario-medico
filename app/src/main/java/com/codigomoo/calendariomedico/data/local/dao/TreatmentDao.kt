package com.codigomoo.calendariomedico.data.local.dao

import androidx.room.*
import com.codigomoo.calendariomedico.data.local.entity.TreatmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TreatmentDao {

    @Query("SELECT * FROM treatments ORDER BY createdAt DESC")
    fun getAll(): Flow<List<TreatmentEntity>>

    @Query("SELECT * FROM treatments WHERE isActive = 1 LIMIT 1")
    fun getActive(): Flow<TreatmentEntity?>

    @Query("SELECT * FROM treatments WHERE id = :id")
    suspend fun getById(id: Long): TreatmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(treatment: TreatmentEntity): Long

    @Update
    suspend fun update(treatment: TreatmentEntity)

    @Delete
    suspend fun delete(treatment: TreatmentEntity)

    @Query("UPDATE treatments SET isActive = 0 WHERE id != :id")
    suspend fun deactivateAllExcept(id: Long)
}
