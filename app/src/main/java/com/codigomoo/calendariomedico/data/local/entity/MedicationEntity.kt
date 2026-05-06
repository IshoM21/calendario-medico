package com.codigomoo.calendariomedico.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.codigomoo.calendariomedico.domain.model.Medication
import com.codigomoo.calendariomedico.domain.model.TimeSlot
import java.time.LocalDateTime

@Entity(
    tableName = "medications",
    foreignKeys = [ForeignKey(
        entity = TreatmentEntity::class,
        parentColumns = ["id"],
        childColumns = ["treatmentId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("treatmentId")]
)
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val treatmentId: Long,
    val name: String,
    val dose: String,
    val instructions: String?,
    val timeSlot: TimeSlot,
    val isRequired: Boolean,
    val colorHex: String?,
    val minIntervalHours: Int?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    fun toDomain() = Medication(
        id, treatmentId, name, dose, instructions, timeSlot, isRequired, colorHex, minIntervalHours, createdAt, updatedAt
    )

    companion object {
        fun fromDomain(m: Medication) = MedicationEntity(
            m.id, m.treatmentId, m.name, m.dose, m.instructions, m.timeSlot,
            m.isRequired, m.colorHex, m.minIntervalHours, m.createdAt, m.updatedAt
        )
    }
}
