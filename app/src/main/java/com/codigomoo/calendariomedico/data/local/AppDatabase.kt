package com.codigomoo.calendariomedico.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.codigomoo.calendariomedico.data.local.dao.IntakeDao
import com.codigomoo.calendariomedico.data.local.dao.MedicationDao
import com.codigomoo.calendariomedico.data.local.dao.ScheduleDao
import com.codigomoo.calendariomedico.data.local.dao.TreatmentDao
import com.codigomoo.calendariomedico.data.local.entity.MedicationEntity
import com.codigomoo.calendariomedico.data.local.entity.MedicationIntakeEntity
import com.codigomoo.calendariomedico.data.local.entity.MedicationScheduleEntity
import com.codigomoo.calendariomedico.data.local.entity.TreatmentEntity

@Database(
    entities = [
        TreatmentEntity::class,
        MedicationEntity::class,
        MedicationScheduleEntity::class,
        MedicationIntakeEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun treatmentDao(): TreatmentDao
    abstract fun medicationDao(): MedicationDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun intakeDao(): IntakeDao
}
