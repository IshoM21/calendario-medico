package com.codigomoo.calendariomedico.data.repository

import com.codigomoo.calendariomedico.data.local.dao.MedicationDao
import com.codigomoo.calendariomedico.data.local.dao.ScheduleDao
import com.codigomoo.calendariomedico.data.local.entity.MedicationEntity
import com.codigomoo.calendariomedico.data.local.entity.MedicationScheduleEntity
import com.codigomoo.calendariomedico.domain.model.Medication
import com.codigomoo.calendariomedico.domain.model.MedicationSchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicationRepository @Inject constructor(
    private val medicationDao: MedicationDao,
    private val scheduleDao: ScheduleDao
) {

    fun getByTreatment(treatmentId: Long): Flow<List<Medication>> =
        medicationDao.getByTreatment(treatmentId).map { list -> list.map { it.toDomain() } }

    suspend fun getByTreatmentOnce(treatmentId: Long): List<Medication> =
        medicationDao.getByTreatmentOnce(treatmentId).map { it.toDomain() }

    suspend fun getById(id: Long): Medication? = medicationDao.getById(id)?.toDomain()

    suspend fun save(medication: Medication): Long {
        val now = LocalDateTime.now()
        val entity = MedicationEntity.fromDomain(
            if (medication.id == 0L) medication.copy(createdAt = now, updatedAt = now)
            else medication.copy(updatedAt = now)
        )
        return medicationDao.insert(entity)
    }

    suspend fun update(medication: Medication) =
        medicationDao.update(MedicationEntity.fromDomain(medication.copy(updatedAt = LocalDateTime.now())))

    suspend fun delete(medication: Medication) =
        medicationDao.delete(MedicationEntity.fromDomain(medication))

    suspend fun getSchedules(medicationId: Long): List<MedicationSchedule> =
        scheduleDao.getByMedication(medicationId).map { it.toDomain() }

    suspend fun getScheduleForDay(medicationId: Long, dayOfWeek: DayOfWeek): MedicationSchedule? =
        scheduleDao.getByMedicationAndDay(medicationId, dayOfWeek)?.toDomain()

    suspend fun replaceSchedules(medicationId: Long, schedules: List<MedicationSchedule>) {
        scheduleDao.deleteByMedication(medicationId)
        scheduleDao.insertAll(schedules.map { MedicationScheduleEntity.fromDomain(it) })
    }
}
