package com.codigomoo.calendariomedico.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.codigomoo.calendariomedico.domain.model.Treatment
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "treatments")
data class TreatmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String?,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    fun toDomain() = Treatment(id, name, description, startDate, endDate, isActive, createdAt, updatedAt)

    companion object {
        fun fromDomain(t: Treatment) = TreatmentEntity(
            t.id, t.name, t.description, t.startDate, t.endDate, t.isActive, t.createdAt, t.updatedAt
        )
    }
}
