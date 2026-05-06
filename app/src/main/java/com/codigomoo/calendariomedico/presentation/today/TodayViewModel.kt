package com.codigomoo.calendariomedico.presentation.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codigomoo.calendariomedico.data.repository.TreatmentRepository
import com.codigomoo.calendariomedico.domain.model.IntakeStatus
import com.codigomoo.calendariomedico.domain.model.MedicationIntake
import com.codigomoo.calendariomedico.domain.model.TimeSlot
import com.codigomoo.calendariomedico.domain.model.Treatment
import com.codigomoo.calendariomedico.domain.usecase.GetTodayIntakesUseCase
import com.codigomoo.calendariomedico.domain.usecase.MarkIntakeAsTakenUseCase
import com.codigomoo.calendariomedico.domain.usecase.SkipIntakeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val getTodayIntakesUseCase: GetTodayIntakesUseCase,
    private val markIntakeAsTakenUseCase: MarkIntakeAsTakenUseCase,
    private val skipIntakeUseCase: SkipIntakeUseCase,
    private val treatmentRepository: TreatmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    init {
        loadIntakes()
    }

    private fun loadIntakes() {
        viewModelScope.launch {
            try {
                val intakesFlow = getTodayIntakesUseCase()
                combine(treatmentRepository.getActive(), intakesFlow) { treatment, intakesBySlot ->
                    buildUiState(treatment, intakesBySlot)
                }.catch { e ->
                    emit(TodayUiState(isLoading = false, error = e.message ?: "Error al cargar datos"))
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error al cargar datos") }
            }
        }
    }

    fun markAsTaken(intakeId: Long) {
        viewModelScope.launch { markIntakeAsTakenUseCase(intakeId) }
    }

    fun skipIntake(intakeId: Long) {
        viewModelScope.launch { skipIntakeUseCase(intakeId) }
    }

    private fun buildUiState(
        treatment: Treatment?,
        intakesBySlot: Map<TimeSlot, List<MedicationIntake>>
    ): TodayUiState {
        val allIntakes = intakesBySlot.values.flatten()
        val totalRequired = allIntakes.count { it.status != IntakeStatus.OPTIONAL }
        val totalTaken = allIntakes.count { it.status == IntakeStatus.TAKEN }
        return TodayUiState(
            isLoading = false,
            date = LocalDate.now(),
            treatmentName = treatment?.name ?: "",
            intakesBySlot = intakesBySlot,
            totalRequired = totalRequired,
            totalTaken = totalTaken,
            hasActiveTreatment = treatment != null
        )
    }
}
