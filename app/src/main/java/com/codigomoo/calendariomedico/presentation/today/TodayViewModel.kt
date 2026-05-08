package com.codigomoo.calendariomedico.presentation.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codigomoo.calendariomedico.data.preferences.ReminderPreferences
import com.codigomoo.calendariomedico.data.repository.IntakeRepository
import com.codigomoo.calendariomedico.data.repository.MedicationRepository
import com.codigomoo.calendariomedico.data.repository.TreatmentRepository
import com.codigomoo.calendariomedico.domain.model.IntakeStatus
import com.codigomoo.calendariomedico.domain.model.Medication
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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
        viewModelScope.launch { intakeRepository.markPendingBeforeDateAsMissed(LocalDate.now()) }
        loadIntakes()
    }

    private fun loadIntakes() {
        viewModelScope.launch {
            try {
                treatmentRepository.getActive()
                    .flatMapLatest { treatment ->
                        if (treatment == null) {
                            reminderPreferences.hasCompletedOnboarding.map { onboarded ->
                                buildUiState(null, emptyMap(), emptyList(), onboarded)
                            }
                        } else {
                            combine(
                                getTodayIntakesUseCase(),
                                medicationRepository.getByTreatment(treatment.id),
                                reminderPreferences.hasCompletedOnboarding
                            ) { intakesBySlot, allMeds, onboarded ->
                                val asNeededMeds = allMeds.filter { it.timeSlot == TimeSlot.AS_NEEDED }
                                buildUiState(treatment, intakesBySlot, asNeededMeds, onboarded)
                            }
                        }
                    }
                    .catch { e ->
                        emit(TodayUiState(isLoading = false, error = e.message ?: "Error al cargar datos"))
                    }
                    .collect { newState ->
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

    fun startAsNeededRegistration(item: AsNeededItem) {
        viewModelScope.launch {
            val medication = medicationRepository.getById(item.medicationId)
            val intervalWarning: String? = if (medication?.minIntervalHours != null) {
                val lastTaken = intakeRepository.getLastTaken(item.medicationId)
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
                it.copy(
                    asNeededDialog = AsNeededDialogState(
                        medicationId = item.medicationId,
                        treatmentId = item.treatmentId,
                        medicationName = item.medicationName,
                        dose = item.dose,
                        intervalWarning = intervalWarning
                    )
                )
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
            val intake = MedicationIntake(
                treatmentId = dialog.treatmentId,
                medicationId = dialog.medicationId,
                medicationName = dialog.medicationName,
                date = LocalDate.now(),
                scheduledTimeSlot = TimeSlot.AS_NEEDED,
                dose = dialog.dose,
                status = IntakeStatus.TAKEN,
                confirmedAt = LocalDateTime.now(),
                notes = dialog.notes.ifBlank { null }
            )
            intakeRepository.insertAll(listOf(intake))
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
        asNeededMeds: List<Medication>,
        hasCompletedOnboarding: Boolean
    ): TodayUiState {
        val regularSlots = intakesBySlot.filterKeys { it != TimeSlot.AS_NEEDED }
        val allRequired = regularSlots.values.flatten()
        val totalRequired = allRequired.count { it.status != IntakeStatus.OPTIONAL }
        val totalTaken = allRequired.count { it.status == IntakeStatus.TAKEN }
        val asNeededTakenToday = intakesBySlot[TimeSlot.AS_NEEDED] ?: emptyList()
        val asNeededItems = if (treatment != null) {
            asNeededMeds.map { med ->
                AsNeededItem(
                    medicationId = med.id,
                    treatmentId = treatment.id,
                    medicationName = med.name,
                    dose = med.dose,
                    takenToday = asNeededTakenToday.filter { it.medicationId == med.id }
                )
            }
        } else emptyList()
        return TodayUiState(
            isLoading = false,
            date = LocalDate.now(),
            treatmentName = treatment?.name ?: "",
            treatmentEndDate = treatment?.endDate,
            intakesBySlot = regularSlots,
            asNeededItems = asNeededItems,
            totalRequired = totalRequired,
            totalTaken = totalTaken,
            hasActiveTreatment = treatment != null,
            isFirstLaunch = !hasCompletedOnboarding
        )
    }
}
