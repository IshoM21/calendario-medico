package com.codigomoo.calendariomedico.presentation.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codigomoo.calendariomedico.data.preferences.ReminderPreferences
import com.codigomoo.calendariomedico.data.repository.IntakeRepository
import com.codigomoo.calendariomedico.data.repository.MedicationRepository
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
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val getTodayIntakesUseCase: GetTodayIntakesUseCase,
    private val markIntakeAsTakenUseCase: MarkIntakeAsTakenUseCase,
    private val skipIntakeUseCase: SkipIntakeUseCase,
    private val treatmentRepository: TreatmentRepository,
    private val medicationRepository: MedicationRepository,
    private val intakeRepository: IntakeRepository,
    private val reminderPreferences: ReminderPreferences
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
                combine(
                    treatmentRepository.getActive(),
                    intakesFlow,
                    reminderPreferences.hasCompletedOnboarding
                ) { treatment, intakesBySlot, onboarded ->
                    buildUiState(treatment, intakesBySlot, onboarded)
                }.catch { e ->
                    emit(TodayUiState(isLoading = false, error = e.message ?: "Error al cargar datos"))
                }.collect { newState ->
                    _uiState.update { current ->
                        newState.copy(
                            asNeededExpanded = current.asNeededExpanded,
                            asNeededDialog = current.asNeededDialog
                        )
                    }
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

    fun completeOnboarding() {
        viewModelScope.launch { reminderPreferences.setHasCompletedOnboarding(true) }
    }

    fun startAsNeededRegistration(intake: MedicationIntake) {
        viewModelScope.launch {
            val medication = medicationRepository.getById(intake.medicationId)
            val intervalWarning: String? = if (medication?.minIntervalHours != null) {
                val lastTaken = intakeRepository.getLastTaken(intake.medicationId)
                val confirmed = lastTaken?.confirmedAt
                if (confirmed != null) {
                    val hoursSince = Duration.between(confirmed, LocalDateTime.now()).toHours()
                    if (hoursSince < medication.minIntervalHours) {
                        "Ya tomaste este medicamento hace ${hoursSince}h. " +
                            "El intervalo mínimo recomendado es ${medication.minIntervalHours}h."
                    } else null
                } else null
            } else null
            _uiState.update {
                it.copy(asNeededDialog = AsNeededDialogState(intake = intake, intervalWarning = intervalWarning))
            }
        }
    }

    fun onAsNeededNoteChange(note: String) {
        _uiState.update { current ->
            current.copy(asNeededDialog = current.asNeededDialog?.copy(notes = note))
        }
    }

    fun confirmAsNeededIntake() {
        val dialog = _uiState.value.asNeededDialog ?: return
        viewModelScope.launch {
            markIntakeAsTakenUseCase(dialog.intake.id, dialog.notes.ifBlank { null })
            _uiState.update { it.copy(asNeededDialog = null) }
        }
    }

    fun dismissAsNeededDialog() {
        _uiState.update { it.copy(asNeededDialog = null) }
    }

    fun toggleAsNeededExpanded() {
        _uiState.update { it.copy(asNeededExpanded = !it.asNeededExpanded) }
    }

    private fun buildUiState(
        treatment: Treatment?,
        intakesBySlot: Map<TimeSlot, List<MedicationIntake>>,
        hasCompletedOnboarding: Boolean
    ): TodayUiState {
        val regularSlots = intakesBySlot.filterKeys { it != TimeSlot.AS_NEEDED }
        val asNeededIntakes = intakesBySlot[TimeSlot.AS_NEEDED] ?: emptyList()
        val allRequired = regularSlots.values.flatten()
        val totalRequired = allRequired.count { it.status != IntakeStatus.OPTIONAL }
        val totalTaken = allRequired.count { it.status == IntakeStatus.TAKEN }
        return TodayUiState(
            isLoading = false,
            date = LocalDate.now(),
            treatmentName = treatment?.name ?: "",
            treatmentEndDate = treatment?.endDate,
            intakesBySlot = regularSlots,
            asNeededIntakes = asNeededIntakes,
            totalRequired = totalRequired,
            totalTaken = totalTaken,
            hasActiveTreatment = treatment != null,
            isFirstLaunch = !hasCompletedOnboarding
        )
    }
}
