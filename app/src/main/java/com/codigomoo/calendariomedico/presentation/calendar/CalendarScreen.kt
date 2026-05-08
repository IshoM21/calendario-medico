package com.codigomoo.calendariomedico.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.codigomoo.calendariomedico.domain.model.IntakeStatus
import com.codigomoo.calendariomedico.domain.model.MedicationIntake
import com.codigomoo.calendariomedico.domain.model.TimeSlot
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val LOCALE_ES = Locale("es", "MX")
private val WEEK_LETTERS = listOf("L", "M", "X", "J", "V", "S", "D")
private val TIME_FMT = DateTimeFormatter.ofPattern("H:mm")

enum class MonthDayStatus { NONE, COMPLETE, PARTIAL, MISSED }

@Composable
fun CalendarScreen(
    navController: NavController,
    innerPadding: PaddingValues = PaddingValues(),
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator() }
        return
    }

    val monthName = uiState.selectedDate.month
        .getDisplayName(TextStyle.FULL, LOCALE_ES)
        .replaceFirstChar { it.uppercase() }
    val monthTitle = "$monthName ${uiState.selectedDate.year}"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = innerPadding.calculateTopPadding() + 8.dp,
            bottom = innerPadding.calculateBottomPadding() + 24.dp
        )
    ) {
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "CALENDARIO",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
        }

        item {
            WeekStrip(
                weekStart = uiState.weekStart,
                selectedDate = uiState.selectedDate,
                intakesByDate = uiState.weekIntakesByDate,
                onPrevious = viewModel::previous,
                onNext = viewModel::next,
                onDayClick = viewModel::selectDate,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(Modifier.height(14.dp))
        }

        item {
            DayDetailCard(
                date = uiState.selectedDate,
                intakes = uiState.selectedDateIntakes,
                morningTime = uiState.morningTime,
                noonTime = uiState.noonTime,
                nightTime = uiState.nightTime,
                firstPendingId = uiState.nextPendingIntakeId,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(22.dp))
        }

        item {
            MonthSection(
                monthStart = uiState.selectedDate.withDayOfMonth(1),
                selectedDate = uiState.selectedDate,
                intakesByDate = uiState.monthIntakesByDate,
                onDayClick = viewModel::selectDate,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun WeekStrip(
    weekStart: LocalDate,
    selectedDate: LocalDate,
    intakesByDate: Map<LocalDate, List<MedicationIntake>>,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.KeyboardArrowLeft,
                contentDescription = "Semana anterior",
                modifier = Modifier.size(20.dp)
            )
        }
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
            for (i in 0..6) {
                val date = weekStart.plusDays(i.toLong())
                WeekDayCell(
                    date = date,
                    letter = WEEK_LETTERS[i],
                    isSelected = date == selectedDate,
                    isToday = date == LocalDate.now(),
                    intakes = intakesByDate[date] ?: emptyList(),
                    onClick = { onDayClick(date) }
                )
            }
        }
        IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = "Semana siguiente",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun WeekDayCell(
    date: LocalDate,
    letter: String,
    isSelected: Boolean,
    isToday: Boolean,
    intakes: List<MedicationIntake>,
    onClick: () -> Unit
) {
    val isPastOrToday = !date.isAfter(LocalDate.now())
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onBackground

    val required = intakes.filter {
        it.status != IntakeStatus.OPTIONAL && it.scheduledTimeSlot != TimeSlot.AS_NEEDED
    }
    val dotColor: Color? = if (required.isNotEmpty() && isPastOrToday) {
        val allTaken = required.all { it.status == IntakeStatus.TAKEN }
        val anyMissed = required.any { it.status == IntakeStatus.MISSED }
        when {
            allTaken -> Color(0xFF2E7D32)
            anyMissed -> Color(0xFFC62828)
            else -> Color(0xFFE67E22)
        }
    } else null

    Column(
        modifier = Modifier
            .width(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.labelSmall,
            color = textColor.copy(alpha = 0.65f)
        )
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        if (dotColor != null) {
            Box(modifier = Modifier.size(5.dp).background(dotColor, CircleShape))
        } else {
            Spacer(Modifier.height(5.dp))
        }
    }
}

@Composable
private fun DayDetailCard(
    date: LocalDate,
    intakes: List<MedicationIntake>,
    morningTime: LocalTime,
    noonTime: LocalTime,
    nightTime: LocalTime,
    firstPendingId: Long?,
    modifier: Modifier = Modifier
) {
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, LOCALE_ES).replaceFirstChar { it.uppercase() }
    val monthName = date.month.getDisplayName(TextStyle.FULL, LOCALE_ES)
    val dateLabel = "$dayName, ${date.dayOfMonth} de $monthName"

    val required = intakes.count { it.status != IntakeStatus.OPTIONAL }
    val taken = intakes.count { it.status == IntakeStatus.TAKEN }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (required > 0) {
                    ComplianceBadge(taken = taken, required = required)
                }
            }

            if (intakes.isEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Sin tomas registradas para este día",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            } else {
                Spacer(Modifier.height(12.dp))
                intakes.forEachIndexed { index, intake ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 7.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        )
                    }
                    IntakeRowDetail(
                        intake = intake,
                        slotTime = slotTimeFor(intake.scheduledTimeSlot, morningTime, noonTime, nightTime),
                        isNext = firstPendingId != null && intake.id == firstPendingId
                    )
                }
            }
        }
    }
}

@Composable
private fun IntakeRowDetail(
    intake: MedicationIntake,
    slotTime: LocalTime?,
    isNext: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(slotDotColor(intake.scheduledTimeSlot), CircleShape)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = intake.medicationName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            val subLabel = buildString {
                append(intake.dose)
                slotTime?.let { append(" · ${it.format(TIME_FMT)}") }
            }
            Text(
                text = subLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
        if (isNext) {
            NextBadge()
        } else {
            IntakeStatusChip(status = intake.status, confirmedAt = intake.confirmedAt?.toLocalTime())
        }
    }
}

@Composable
private fun NextBadge() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = "Próxima",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun IntakeStatusChip(status: IntakeStatus, confirmedAt: LocalTime?) {
    val (label, color) = when (status) {
        IntakeStatus.TAKEN -> {
            val t = confirmedAt?.format(TIME_FMT)?.let { " $it" } ?: ""
            "Tomado$t" to Color(0xFF2E7D32)
        }
        IntakeStatus.SKIPPED -> "Omitida" to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        IntakeStatus.MISSED -> "No tomada" to MaterialTheme.colorScheme.error
        IntakeStatus.OPTIONAL -> "S/N" to MaterialTheme.colorScheme.secondary
        IntakeStatus.PENDING -> "Pendiente" to MaterialTheme.colorScheme.primary
    }
    Text(label, style = MaterialTheme.typography.labelSmall, color = color)
}

@Composable
private fun ComplianceBadge(taken: Int, required: Int) {
    val percent = taken * 100 / required
    val color = when {
        percent >= 100 -> Color(0xFF2E7D32)
        percent >= 50 -> Color(0xFFE67E22)
        else -> MaterialTheme.colorScheme.error
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = "$taken/$required",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun MonthSection(
    monthStart: LocalDate,
    selectedDate: LocalDate,
    intakesByDate: Map<LocalDate, List<MedicationIntake>>,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "VISTA MENSUAL",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            WEEK_LETTERS.forEach { letter ->
                Text(
                    text = letter,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        val gridStart = monthStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth())
        val gridEnd = monthEnd.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

        var current = gridStart
        while (!current.isAfter(gridEnd)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (i in 0..6) {
                    val date = current.plusDays(i.toLong())
                    val inMonth = date.month == monthStart.month
                    val intakes = if (inMonth) intakesByDate[date] ?: emptyList() else emptyList()
                    MonthDayCell(
                        date = date,
                        isSelected = date == selectedDate,
                        isToday = date == LocalDate.now(),
                        inMonth = inMonth,
                        status = computeDayStatus(intakes, date),
                        onClick = { if (inMonth) onDayClick(date) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            current = current.plusWeeks(1)
        }

        MonthLegend()
    }
}

@Composable
private fun MonthDayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    inMonth: Boolean,
    status: MonthDayStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        !inMonth -> Color.Transparent
        status == MonthDayStatus.COMPLETE -> Color(0xFF2E7D32).copy(alpha = 0.15f)
        status == MonthDayStatus.PARTIAL -> Color(0xFFE67E22).copy(alpha = 0.15f)
        status == MonthDayStatus.MISSED -> Color(0xFFC62828).copy(alpha = 0.15f)
        else -> Color.Transparent
    }
    val textColor = when {
        !inMonth -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
        isSelected -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onBackground
    }
    val selectedBg = if (isSelected) MaterialTheme.colorScheme.primary else bgColor

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(selectedBg)
            .then(
                if (isToday && !isSelected)
                    Modifier.border(
                        1.5.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp)
                    )
                else Modifier
            )
            .clickable(enabled = inMonth, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (inMonth) date.dayOfMonth.toString() else "",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MonthLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
    ) {
        LegendDot(color = Color(0xFF2E7D32), label = "Completo")
        LegendDot(color = Color(0xFFE67E22), label = "Parcial")
        LegendDot(color = Color(0xFFC62828), label = "Olvido")
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

private fun computeDayStatus(intakes: List<MedicationIntake>, date: LocalDate): MonthDayStatus {
    val required = intakes.filter {
        it.status != IntakeStatus.OPTIONAL && it.scheduledTimeSlot != TimeSlot.AS_NEEDED
    }
    if (required.isEmpty()) return MonthDayStatus.NONE
    val taken = required.count { it.status == IntakeStatus.TAKEN }
    val missed = required.count { it.status == IntakeStatus.MISSED }
    return when {
        taken == required.size -> MonthDayStatus.COMPLETE
        taken > 0 -> MonthDayStatus.PARTIAL
        missed > 0 && date.isBefore(LocalDate.now()) -> MonthDayStatus.MISSED
        else -> MonthDayStatus.NONE
    }
}

private fun slotTimeFor(slot: TimeSlot, morning: LocalTime, noon: LocalTime, night: LocalTime): LocalTime? =
    when (slot) {
        TimeSlot.MORNING -> morning
        TimeSlot.NOON -> noon
        TimeSlot.NIGHT -> night
        TimeSlot.AS_NEEDED -> null
    }

private fun slotDotColor(slot: TimeSlot) = when (slot) {
    TimeSlot.MORNING -> Color(0xFFF57C00)
    TimeSlot.NOON -> Color(0xFF1F8A8A)
    TimeSlot.NIGHT -> Color(0xFF3F51B5)
    TimeSlot.AS_NEEDED -> Color(0xFF78909C)
}
