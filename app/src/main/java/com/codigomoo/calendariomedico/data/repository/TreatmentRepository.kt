package com.codigomoo.calendariomedico.data.repository

import com.codigomoo.calendariomedico.data.local.dao.TreatmentDao
import com.codigomoo.calendariomedico.data.local.entity.TreatmentEntity
import com.codigomoo.calendariomedico.domain.model.Treatment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TreatmentRepository @Inject constructor(
    private val dao: TreatmentDao
) {

    fun getAll(): Flow<List<Treatment>> = dao.getAll().map { list -> list.map { it.toDomain() } }

    fun getActive(): Flow<Treatment?> = dao.getActive().map { it?.toDomain() }

    suspend fun getById(id: Long): Treatment? = dao.getById(id)?.toDomain()

    suspend fun save(treatment: Treatment): Long {
        val now = LocalDateTime.now()
        return if (treatment.id == 0L) {
            dao.insert(TreatmentEntity.fromDomain(treatment.copy(createdAt = now, updatedAt = now)))
        } else {
            dao.update(TreatmentEntity.fromDomain(treatment.copy(updatedAt = now)))
            treatment.id
        }
    }

    suspend fun activate(id: Long) {
        val entity = dao.getById(id) ?: return
        dao.deactivateAllExcept(id)
        dao.update(entity.copy(isActive = true, updatedAt = LocalDateTime.now()))
    }

    suspend fun deactivate(id: Long) {
        val entity = dao.getById(id) ?: return
        dao.update(entity.copy(isActive = false, updatedAt = LocalDateTime.now()))
    }

    suspend fun delete(treatment: Treatment) = dao.delete(TreatmentEntity.fromDomain(treatment))
}
