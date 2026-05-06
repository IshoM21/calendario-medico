package com.codigomoo.calendariomedico.core.date

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val LOCALE_ES_MX = Locale("es", "MX")

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", LOCALE_ES_MX)
private val DATE_SHORT_FORMATTER = DateTimeFormatter.ofPattern("d MMM", LOCALE_ES_MX)
private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", LOCALE_ES_MX)
private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("d MMM HH:mm", LOCALE_ES_MX)

fun LocalDate.toDisplayString(): String = format(DATE_FORMATTER)

fun LocalDate.toShortDisplayString(): String = format(DATE_SHORT_FORMATTER)

fun LocalTime.toDisplayString(): String = format(TIME_FORMATTER)

fun LocalDateTime.toDisplayString(): String = format(DATE_TIME_FORMATTER)

fun DayOfWeek.toDisplayString(): String = getDisplayName(TextStyle.FULL, LOCALE_ES_MX)
    .replaceFirstChar { it.uppercase() }

fun DayOfWeek.toShortDisplayString(): String = getDisplayName(TextStyle.SHORT, LOCALE_ES_MX)
    .replaceFirstChar { it.uppercase() }

fun LocalDate.isToday(): Boolean = this == LocalDate.now()

fun LocalDate.weekRange(): Pair<LocalDate, LocalDate> {
    val start = with(DayOfWeek.MONDAY).let {
        if (it.isAfter(this)) it.minusWeeks(1) else it
    }
    return start to start.plusDays(6)
}
