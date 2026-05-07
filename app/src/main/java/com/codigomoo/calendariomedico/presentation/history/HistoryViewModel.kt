package com.codigomoo.calendariomedico.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codigomoo.calendariomedico.data.repository.IntakeRepository
import com.codigomoo.calendariomedico.domain.model.IntakeStatus
import com.codigomoo.calendariomedico.domain.model.MedicationIntake
import com.codigomoo.calendariomedico.domain.usecase.GenerateDailyIntakesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HistoryUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val intakes: List<MedicationIntake> = emptyList(),
    val isLoading: Boolean = true
) {
    val required: Int get() = intakes.count { it.status != IntakeStatus.OPTIONAL }
    val taken: Int get() = intakes.count { it.status == IntakeStatus.TAKEN }
    val compliancePercent: Int? get() = if (required == 0) null else taken * 100 / required
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val intakeRepository: IntakeRepository,
    private val generateDailyIntakesUseCase: GenerateDailyIntakesUseCase
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())

    private val dateIntakes = _selectedDate.flatMapLatest { date ->
        intakeRepository.getByDate(date)
    }

    val uiState: StateFlow<HistoryUiState> = combine(_selectedDate, dateIntakes) { date, intakes ->
        HistoryUiState(selectedDate = date, intakes = intakes, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    init {
        viewModelScope.launch {
            _selectedDate.collect { date ->
                generateDailyIntakesUseCase(date)
            }
        }
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.update { date }
    }
}
