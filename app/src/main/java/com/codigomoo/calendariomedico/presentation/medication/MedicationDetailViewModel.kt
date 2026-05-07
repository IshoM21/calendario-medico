package com.codigomoo.calendariomedico.presentation.medication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codigomoo.calendariomedico.data.repository.IntakeRepository
import com.codigomoo.calendariomedico.data.repository.MedicationRepository
import com.codigomoo.calendariomedico.domain.model.IntakeStatus
import com.codigomoo.calendariomedico.domain.model.Medication
import com.codigomoo.calendariomedico.domain.model.MedicationIntake
import com.codigomoo.calendariomedico.domain.usecase.GenerateDailyIntakesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class MedicationDetailUiState(
    val medication: Medication? = null,
    val todayIntake: MedicationIntake? = null,
    val recentIntakes: List<MedicationIntake> = emptyList(),
    val isLoading: Boolean = true
) {
    val last7Days: List<Pair<LocalDate, IntakeStatus?>> get() {
        val today = LocalDate.now()
        return (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val intake = recentIntakes.firstOrNull { it.date == date }
            date to intake?.status
        }
    }
}

@HiltViewModel
class MedicationDetailViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val intakeRepository: IntakeRepository,
    private val generateDailyIntakesUseCase: GenerateDailyIntakesUseCase
) : ViewModel() {

    private val _medicationId = MutableStateFlow(0L)

    private val recentIntakes = _medicationId.flatMapLatest { id ->
        if (id == 0L) flowOf(emptyList())
        else {
            val today = LocalDate.now()
            intakeRepository.getByMedicationAndDateRange(id, today.minusDays(6), today)
        }
    }

    val uiState: StateFlow<MedicationDetailUiState> = combine(_medicationId, recentIntakes) { id, intakes ->
        val medication = if (id == 0L) null else medicationRepository.getById(id)
        val today = LocalDate.now()
        MedicationDetailUiState(
            medication = medication,
            todayIntake = intakes.firstOrNull { it.date == today },
            recentIntakes = intakes,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MedicationDetailUiState())

    fun load(medicationId: Long) {
        if (_medicationId.value == medicationId) return
        _medicationId.value = medicationId
        viewModelScope.launch {
            for (i in 6 downTo 0) generateDailyIntakesUseCase(LocalDate.now().minusDays(i.toLong()))
        }
    }
}
