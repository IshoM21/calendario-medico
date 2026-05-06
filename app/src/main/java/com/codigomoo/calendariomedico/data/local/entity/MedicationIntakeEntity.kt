package com.codigomoo.calendariomedico.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.codigomoo.calendariomedico.domain.model.IntakeStatus
import com.codigomoo.calendariomedico.domain.model.MedicationIntake
import com.codigomoo.calendariomedico.domain.model.TimeSlot
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity(
    tableName = "medication_intakes",
    foreignKeys = [
        ForeignKey(TreatmentEntity::class, ["id"], ["treatmentId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(MedicationEntity::class, ["id"], ["medicationId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("treatmentId"), Index("medicationId"), Index("date")]
)
data class MedicationIntakeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val treatmentId: Long,
    val medicationId: Long,
    val medicationName: String,
    val date: LocalDate,
    val scheduledTimeSlot: TimeSlot,
    val scheduledTime: LocalTime?,
    val dose: String,
    val status: IntakeStatus,
    val confirmedAt: LocalDateTime?,
    val notes: String?
) {
    fun toDomain() = MedicationIntake(
        id, treatmentId, medicationId, medicationName, date,
        scheduledTimeSlot, scheduledTime, dose, status, confirmedAt, notes
    )

    companion object {
        fun fromDomain(i: MedicationIntake) = MedicationIntakeEntity(
            i.id, i.treatmentId, i.medicationId, i.medicationName, i.date,
            i.scheduledTimeSlot, i.scheduledTime, i.dose, i.status, i.confirmedAt, i.notes
        )
    }
}
