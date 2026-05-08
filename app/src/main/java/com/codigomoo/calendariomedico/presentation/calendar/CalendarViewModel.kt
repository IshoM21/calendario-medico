package com.codigomoo.calendariomedico.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codigomoo.calendariomedico.data.preferences.ReminderPreferences
import com.codigomoo.calendariomedico.data.repository.IntakeRepository
import com.codigomoo.calendariomedico.domain.model.IntakeStatus
import com.codigomoo.calendariomedico.domain.model.MedicationIntake
import com.codigomoo.calendariomedico.domain.model.TimeSlot
import com.codigomoo.calendariomedico.domain.usecase.GenerateDailyIntakesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class CalendarUiState(
    val weekStart: LocalDate = mondayOfCurrentWeek(),
    val selectedDate: LocalDate = LocalDate.now(),
    val weekIntakesByDate: Map<LocalDate, List<MedicationIntake>> = emptyMap(),
    val monthIntakesByDate: Map<LocalDate, List<MedicationIntake>> = emptyMap(),
    val selectedDateIntakes: List<MedicationIntake> = emptyList(),
    val nextPendingIntakeId: Long? = null,
    val morningTime: LocalTime = LocalTime.of(8, 0),
    val noonTime: LocalTime = LocalTime.of(13, 0),
    val nightTime: LocalTime = LocalTime.of(21, 0),
    val isLoading: Boolean = true
)

fun mondayOfCurrentWeek(): LocalDate =
    LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val intakeRepository: IntakeRepository,
    private val generateDailyIntakesUseCase: GenerateDailyIntakesUseCase,
    private val reminderPreferences: ReminderPreferences
) : ViewModel() {

    private val _weekStart = MutableStateFlow(mondayOfCurrentWeek())
    private val _selectedDate = MutableStateFlow(LocalDate.now())

    private val monthStartFlow = _selectedDate
        .map { it.withDayOfMonth(1) }
        .distinctUntilChanged()

    private val weekIntakesFlow = _weekStart.flatMapLatest { start ->
        intakeRepository.getByDateRange(start, start.plusDays(6))
    }

    private val monthIntakesFlow = monthStartFlow.flatMapLatest { monthStart ->
        intakeRepository.getByDateRange(
            monthStart,
            monthStart.withDayOfMonth(monthStart.lengthOfMonth())
        )
    }

    private val slotTimesFlow = combine(
        reminderPreferences.morningTime,
        reminderPreferences.noonTime,
        reminderPreferences.nightTime
    ) { morning, noon, night -> Triple(morning, noon, night) }

    val uiState: StateFlow<CalendarUiState> = combine(
        _weekStart,
        _selectedDate,
        weekIntakesFlow,
        monthIntakesFlow,
        slotTimesFlow
    ) { weekStart, selected, weekIntakes, monthIntakes, slotTimes ->
        val weekByDate = weekIntakes.groupBy { it.date }
        val monthByDate = monthIntakes.groupBy { it.date }
        val selectedIntakes = (monthByDate[selected] ?: weekByDate[selected] ?: emptyList())
            .filter { it.scheduledTimeSlot != TimeSlot.AS_NEEDED }
            .sortedWith(compareBy({ slotOrder(it.scheduledTimeSlot) }, { it.medicationName }))
        CalendarUiState(
            weekStart = weekStart,
            selectedDate = selected,
            weekIntakesByDate = weekByDate,
            monthIntakesByDate = monthByDate,
            selectedDateIntakes = selectedIntakes,
            nextPendingIntakeId = if (selected == LocalDate.now())
                selectedIntakes.firstOrNull { it.status == IntakeStatus.PENDING }?.id
            else null,
            morningTime = slotTimes.first,
            noonTime = slotTimes.second,
            nightTime = slotTimes.third,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    init {
        viewModelScope.launch {
            combine(_weekStart, monthStartFlow) { w, m -> w to m }
                .distinctUntilChanged()
                .collect { (weekStart, monthStart) ->
                    val monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth())
                    val dates = buildSet {
                        var d = monthStart; while (!d.isAfter(monthEnd)) { add(d); d = d.plusDays(1) }
                        for (i in 0..6) add(weekStart.plusDays(i.toLong()))
                    }
                    dates.forEach { date -> launch { generateDailyIntakesUseCase(date) } }
                }
        }
    }

    fun previous() {
        _weekStart.update { it.minusWeeks(1) }
    }

    fun next() {
        _weekStart.update { it.plusWeeks(1) }
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    private fun slotOrder(slot: TimeSlot) = when (slot) {
        TimeSlot.MORNING -> 0
        TimeSlot.NOON -> 1
        TimeSlot.NIGHT -> 2
        TimeSlot.AS_NEEDED -> 3
    }
}
