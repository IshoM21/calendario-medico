package com.codigomoo.calendariomedico.data.local

import androidx.room.TypeConverter
import com.codigomoo.calendariomedico.domain.model.IntakeStatus
import com.codigomoo.calendariomedico.domain.model.TimeSlot
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class Converters {

    @TypeConverter fun localDateToString(v: LocalDate?): String? = v?.toString()
    @TypeConverter fun stringToLocalDate(v: String?): LocalDate? = v?.let { LocalDate.parse(it) }

    @TypeConverter fun localTimeToString(v: LocalTime?): String? = v?.toString()
    @TypeConverter fun stringToLocalTime(v: String?): LocalTime? = v?.let { LocalTime.parse(it) }

    @TypeConverter fun localDateTimeToString(v: LocalDateTime?): String? = v?.toString()
    @TypeConverter fun stringToLocalDateTime(v: String?): LocalDateTime? = v?.let { LocalDateTime.parse(it) }

    @TypeConverter fun dayOfWeekToInt(v: DayOfWeek?): Int? = v?.value
    @TypeConverter fun intToDayOfWeek(v: Int?): DayOfWeek? = v?.let { DayOfWeek.of(it) }

    @TypeConverter fun timeSlotToString(v: TimeSlot?): String? = v?.name
    @TypeConverter fun stringToTimeSlot(v: String?): TimeSlot? = v?.let { TimeSlot.valueOf(it) }

    @TypeConverter fun intakeStatusToString(v: IntakeStatus?): String? = v?.name
    @TypeConverter fun stringToIntakeStatus(v: String?): IntakeStatus? = v?.let { IntakeStatus.valueOf(it) }
}
