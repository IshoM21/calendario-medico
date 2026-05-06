package com.codigomoo.calendariomedico.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.codigomoo.calendariomedico.domain.model.MedicationSchedule
import java.time.DayOfWeek

@Entity(
    tableName = "medication_schedules",
    foreignKeys = [ForeignKey(
        entity = MedicationEntity::class,
        parentColumns = ["id"],
        childColumns = ["medicationId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("medicationId")]
)
data class MedicationScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val dayOfWeek: DayOfWeek,
    val doseOverride: String?
) {
    fun toDomain() = MedicationSchedule(id, medicationId, dayOfWeek, doseOverride)

    companion object {
        fun fromDomain(s: MedicationSchedule) =
            MedicationScheduleEntity(s.id, s.medicationId, s.dayOfWeek, s.doseOverride)
    }
}
