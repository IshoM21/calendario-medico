package com.codigomoo.calendariomedico.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.codigomoo.calendariomedico.core.date.toDisplayString
import com.codigomoo.calendariomedico.domain.model.IntakeStatus
import com.codigomoo.calendariomedico.domain.model.MedicationIntake
import com.codigomoo.calendariomedico.domain.model.TimeSlot
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val LOCALE_ES = Locale("es", "MX")

// Domingo primero
private val WEEK_DAY_LETTERS = listOf("D", "L", "M", "X", "J", "V", "S")
private val WEEK_DAY_ORDER = listOf(
    DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
)

@OptIn(ExperimentalMaterial3Api::class)
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
        ) { androidx.compose.material3.CircularProgressIndicator() }
        return
    }

    val selectedIntakes = uiState.selectedDateIntakes
    val required = selectedIntakes.count { it.status != IntakeStatus.OPTIONAL }
    val taken = selectedIntakes.count { it.status == IntakeStatus.TAKEN }
    val compliance = if (required == 0) null else taken * 100 / required

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = innerPadding.calculateTopPadding() + 8.dp,
            bottom = innerPadding.calculateBottomPadding() + 16.dp
        )
    ) {
        item {
            PeriodNavigationHeader(
                uiState = uiState,
                onPrevious = viewModel::previous,
                onNext = viewModel::next,
                onToggleView = viewModel::toggleView,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(Modifier.height(8.dp))

            if (uiState.isMonthlyView) {
                MonthGrid(
                    periodStart = uiState.periodStart,
                    selectedDate = uiState.selectedDate,
                    intakesByDate = uiState.intakesByDate,
                    onDayClick = viewModel::selectDate,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            } else {
                WeekRow(
                    weekStart = uiState.periodStart,
                    selectedDate = uiState.selectedDate,
                    intakesByDate = uiState.intakesByDate,
                    onDayClick = viewModel::selectDate,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.selectedDate.toDisplayString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                compliance?.let { ComplianceBadge(percent = it) }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (selectedIntakes.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sin tomas registradas para este día",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(selectedIntakes, key = { it.id }) { intake ->
                IntakeRowReadOnly(
                    intake = intake,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodNavigationHeader(
    uiState: CalendarUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleView: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = if (uiState.isMonthlyView) {
        val month = uiState.periodStart.month.displayName()
            .replaceFirstChar { it.uppercase() }
        "$month ${uiState.periodStart.year}"
    } else {
        val weekEnd = uiState.periodStart.plusDays(6)
        if (uiState.periodStart.month == weekEnd.month) {
            "${uiState.periodStart.dayOfMonth}–${weekEnd.dayOfMonth} ${weekEnd.month.displayName()}"
        } else {
            "${uiState.periodStart.dayOfMonth} ${uiState.periodStart.month.displayName()} – " +
                "${weekEnd.dayOfMonth} ${weekEnd.month.displayName()}"
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.Default.KeyboardArrowLeft,
                    contentDescription = if (uiState.isMonthlyView) "Mes anterior" else "Semana anterior")
            }
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onNext) {
                Icon(Icons.Default.KeyboardArrowRight,
                    contentDescription = if (uiState.isMonthlyView) "Mes siguiente" else "Semana siguiente")
            }
        }
        SingleChoiceSegmentedButtonRow(modifier = Modifier.align(Alignment.CenterHorizontally)) {
            SegmentedButton(
                selected = !uiState.isMonthlyView,
                onClick = { if (uiState.isMonthlyView) onToggleView() },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) { Text("Semana") }
            SegmentedButton(
                selected = uiState.isMonthlyView,
                onClick = { if (!uiState.isMonthlyView) onToggleView() },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) { Text("Mes") }
        }
    }
}

@Composable
private fun WeekRow(
    weekStart: LocalDate,
    selectedDate: LocalDate,
    intakesByDate: Map<LocalDate, List<MedicationIntake>>,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        for (i in 0..6) {
            val date = weekStart.plusDays(i.toLong())
            DayCell(
                date = date,
                isSelected = date == selectedDate,
                isToday = date == LocalDate.now(),
                intakes = intakesByDate[date] ?: emptyList(),
                onClick = { onDayClick(date) },
                modifier = Modifier.width(44.dp)
            )
        }
    }
}

@Composable
private fun MonthGrid(
    periodStart: LocalDate,
    selectedDate: LocalDate,
    intakesByDate: Map<LocalDate, List<MedicationIntake>>,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridStart = periodStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
    val monthEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth())
    val gridEnd = monthEnd.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))

    Column(modifier = modifier.fillMaxWidth()) {
        // Day headers
        Row(modifier = Modifier.fillMaxWidth()) {
            WEEK_DAY_LETTERS.forEach { letter ->
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
        Spacer(Modifier.height(4.dp))

        // Week rows
        var current = gridStart
        while (!current.isAfter(gridEnd)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (i in 0..6) {
                    val date = current.plusDays(i.toLong())
                    val inMonth = date.month == periodStart.month
                    DayCell(
                        date = date,
                        isSelected = date == selectedDate,
                        isToday = date == LocalDate.now(),
                        intakes = if (inMonth) intakesByDate[date] ?: emptyList() else emptyList(),
                        onClick = { if (inMonth) onDayClick(date) },
                        modifier = Modifier.weight(1f),
                        dimmed = !inMonth
                    )
                }
            }
            current = current.plusWeeks(1)
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    intakes: List<MedicationIntake>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false
) {
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        dimmed -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
        else -> MaterialTheme.colorScheme.onBackground
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(enabled = !dimmed, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = WEEK_DAY_LETTERS.getOrNull(WEEK_DAY_ORDER.indexOf(date.dayOfWeek)) ?: "",
            style = MaterialTheme.typography.labelSmall,
            color = textColor.copy(alpha = if (dimmed) 0.25f else 0.7f)
        )
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            intakes.filter { it.status != IntakeStatus.OPTIONAL }.take(2).forEach { intake ->
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(intakeStatusDotColor(intake.status), CircleShape)
                )
            }
        }
    }
}

@Composable
private fun IntakeRowReadOnly(intake: MedicationIntake, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(intake.medicationName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    text = "${intake.dose} · ${intake.scheduledTimeSlot.displayLabel()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            IntakeStatusLabel(
                status = intake.status,
                confirmedAt = intake.confirmedAt?.toLocalTime()?.toString()
            )
        }
    }
}

@Composable
private fun IntakeStatusLabel(status: IntakeStatus, confirmedAt: String?) {
    val (label, color) = when (status) {
        IntakeStatus.TAKEN -> "Tomado${confirmedAt?.let { " $it" } ?: ""}" to Color(0xFF2E7D32)
        IntakeStatus.SKIPPED -> "Omitida" to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        IntakeStatus.MISSED -> "No tomada" to MaterialTheme.colorScheme.error
        IntakeStatus.OPTIONAL -> "Opcional" to MaterialTheme.colorScheme.secondary
        IntakeStatus.PENDING -> "Pendiente" to MaterialTheme.colorScheme.primary
    }
    Text(label, style = MaterialTheme.typography.labelSmall, color = color)
}

@Composable
private fun ComplianceBadge(percent: Int) {
    val color = when {
        percent >= 100 -> Color(0xFF2E7D32)
        percent >= 75 -> Color(0xFFE67E22)
        else -> MaterialTheme.colorScheme.error
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = "$percent%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

private fun intakeStatusDotColor(status: IntakeStatus) = when (status) {
    IntakeStatus.TAKEN -> Color(0xFF2E7D32)
    IntakeStatus.MISSED -> Color(0xFFC62828)
    IntakeStatus.SKIPPED -> Color(0xFF78909C)
    IntakeStatus.PENDING -> Color(0xFF1F8A8A)
    IntakeStatus.OPTIONAL -> Color(0xFF5C6BC0)
}

private fun Month.displayName(): String = getDisplayName(TextStyle.FULL, LOCALE_ES).lowercase()

private fun TimeSlot.displayLabel() = when (this) {
    TimeSlot.MORNING -> "Mañana"
    TimeSlot.NOON -> "Comida"
    TimeSlot.NIGHT -> "Noche"
    TimeSlot.AS_NEEDED -> "S/N"
}
