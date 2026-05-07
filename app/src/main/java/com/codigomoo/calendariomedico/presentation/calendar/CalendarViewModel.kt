package com.codigomoo.calendariomedico.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codigomoo.calendariomedico.data.repository.IntakeRepository
import com.codigomoo.calendariomedico.domain.model.MedicationIntake
import com.codigomoo.calendariomedico.domain.usecase.GenerateDailyIntakesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class CalendarUiState(
    val periodStart: LocalDate = sundayOfCurrentWeek(),
    val selectedDate: LocalDate = LocalDate.now(),
    val intakesByDate: Map<LocalDate, List<MedicationIntake>> = emptyMap(),
    val selectedDateIntakes: List<MedicationIntake> = emptyList(),
    val isMonthlyView: Boolean = false,
    val isLoading: Boolean = true
)

private fun sundayOfCurrentWeek(): LocalDate =
    LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val intakeRepository: IntakeRepository,
    private val generateDailyIntakesUseCase: GenerateDailyIntakesUseCase
) : ViewModel() {

    private val _isMonthlyView = MutableStateFlow(false)
    private val _periodStart = MutableStateFlow(sundayOfCurrentWeek())
    private val _selectedDate = MutableStateFlow(LocalDate.now())

    private val periodRange = combine(_periodStart, _isMonthlyView) { start, monthly ->
        if (monthly) start to start.plusDays((start.lengthOfMonth() - 1).toLong())
        else start to start.plusDays(6)
    }

    private val periodIntakes = periodRange.flatMapLatest { (from, to) ->
        intakeRepository.getByDateRange(from, to)
    }

    val uiState: StateFlow<CalendarUiState> = combine(
        _periodStart, _selectedDate, periodIntakes, _isMonthlyView
    ) { start, selected, intakes, monthly ->
        val byDate = intakes.groupBy { it.date }
        CalendarUiState(
            periodStart = start,
            selectedDate = selected,
            intakesByDate = byDate,
            selectedDateIntakes = byDate[selected] ?: emptyList(),
            isMonthlyView = monthly,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    init {
        viewModelScope.launch {
            periodRange.distinctUntilChanged().collect { (from, to) ->
                var date = from
                while (!date.isAfter(to)) {
                    launch { generateDailyIntakesUseCase(date) }
                    date = date.plusDays(1)
                }
            }
        }
    }

    fun previous() {
        _periodStart.update {
            if (_isMonthlyView.value) it.minusMonths(1).withDayOfMonth(1)
            else it.minusWeeks(1)
        }
    }

    fun next() {
        _periodStart.update {
            if (_isMonthlyView.value) it.plusMonths(1).withDayOfMonth(1)
            else it.plusWeeks(1)
        }
    }

    fun selectDate(date: LocalDate) { _selectedDate.value = date }

    fun toggleView() {
        val nowMonthly = !_isMonthlyView.value
        _isMonthlyView.value = nowMonthly
        val today = LocalDate.now()
        _periodStart.value = if (nowMonthly) today.withDayOfMonth(1)
        else sundayOfCurrentWeek()
    }
}
