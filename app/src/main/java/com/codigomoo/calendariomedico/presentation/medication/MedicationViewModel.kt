package com.codigomoo.calendariomedico.presentation.medication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codigomoo.calendariomedico.data.repository.IntakeRepository
import com.codigomoo.calendariomedico.data.repository.MedicationRepository
import com.codigomoo.calendariomedico.domain.model.Medication
import com.codigomoo.calendariomedico.domain.model.MedicationSchedule
import com.codigomoo.calendariomedico.domain.model.TimeSlot
import com.codigomoo.calendariomedico.domain.usecase.GenerateDailyIntakesUseCase
import com.codigomoo.calendariomedico.domain.usecase.RescheduleRemindersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

data class MedicationFormState(
    val name: String = "",
    val dose: String = "",
    val instructions: String = "",
    val timeSlot: TimeSlot = TimeSlot.MORNING,
    val selectedDays: Set<DayOfWeek> = emptySet(),
    val doseOverrides: Map<DayOfWeek, String> = emptyMap(),
    val isRequired: Boolean = true,
    val colorHex: String? = null,
    val minIntervalHours: Int? = null,
    val specificTime: LocalTime? = null,
    val nameError: String? = null,
    val doseError: String? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false
)

@HiltViewModel
class MedicationViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val intakeRepository: IntakeRepository,
    private val rescheduleRemindersUseCase: RescheduleRemindersUseCase,
    private val generateDailyIntakesUseCase: GenerateDailyIntakesUseCase
) : ViewModel() {

    private val _medications = MutableStateFlow<List<Medication>>(emptyList())
    val medications: StateFlow<List<Medication>> = _medications.asStateFlow()

    private val _form = MutableStateFlow(MedicationFormState())
    val form: StateFlow<MedicationFormState> = _form.asStateFlow()

    private var listJob: Job? = null
    private var currentTreatmentId: Long? = null

    fun loadList(treatmentId: Long) {
        if (currentTreatmentId == treatmentId) return
        currentTreatmentId = treatmentId
        listJob?.cancel()
        listJob = viewModelScope.launch {
            medicationRepository.getByTreatment(treatmentId).collect { _medications.value = it }
        }
    }

    fun resetForm(treatmentId: Long) {
        _form.value = MedicationFormState()
    }

    fun loadForEdit(medicationId: Long) {
        viewModelScope.launch {
            val med = medicationRepository.getById(medicationId) ?: return@launch
            val schedules = medicationRepository.getSchedules(medicationId)
            val selectedDays = schedules.map { it.dayOfWeek }.toSet()
            val overrides = schedules
                .filter { it.doseOverride != null }
                .associate { it.dayOfWeek to it.doseOverride!! }
            _form.update {
                it.copy(
                    name = med.name,
                    dose = med.dose,
                    instructions = med.instructions ?: "",
                    timeSlot = med.timeSlot,
                    selectedDays = selectedDays,
                    doseOverrides = overrides,
                    isRequired = med.isRequired,
                    colorHex = med.colorHex,
                    minIntervalHours = med.minIntervalHours,
                    specificTime = med.specificTime,
                    isSaved = false
                )
            }
        }
    }

    fun onNameChange(v: String) = _form.update { it.copy(name = v, nameError = null) }
    fun onDoseChange(v: String) = _form.update { it.copy(dose = v, doseError = null) }
    fun onInstructionsChange(v: String) = _form.update { it.copy(instructions = v) }
    fun onTimeSlotChange(v: TimeSlot) = _form.update { it.copy(timeSlot = v, specificTime = null) }
    fun onIsRequiredChange(v: Boolean) = _form.update { it.copy(isRequired = v) }
    fun onColorChange(v: String?) = _form.update { it.copy(colorHex = v) }
    fun onSpecificTimeChange(v: LocalTime?) = _form.update { it.copy(specificTime = v) }

    fun onDayToggle(day: DayOfWeek) {
        _form.update { s ->
            val newDays = if (day in s.selectedDays) s.selectedDays - day else s.selectedDays + day
            val newOverrides = s.doseOverrides.filterKeys { it in newDays }
            s.copy(selectedDays = newDays, doseOverrides = newOverrides)
        }
    }

    fun onDoseOverrideChange(day: DayOfWeek, dose: String) {
        _form.update { s ->
            val newOverrides = if (dose.isBlank()) s.doseOverrides - day
            else s.doseOverrides + (day to dose)
            s.copy(doseOverrides = newOverrides)
        }
    }

    fun save(treatmentId: Long, medicationId: Long?) {
        val s = _form.value
        if (s.name.isBlank()) { _form.update { it.copy(nameError = "El nombre es obligatorio") }; return }
        if (s.dose.isBlank()) { _form.update { it.copy(doseError = "La dosis es obligatoria") }; return }

        viewModelScope.launch {
            _form.update { it.copy(isSaving = true) }
            val now = LocalDateTime.now()
            val med = Medication(
                id = medicationId ?: 0L,
                treatmentId = treatmentId,
                name = s.name,
                dose = s.dose,
                instructions = s.instructions.ifBlank { null },
                timeSlot = s.timeSlot,
                isRequired = s.isRequired,
                colorHex = s.colorHex,
                minIntervalHours = s.minIntervalHours,
                specificTime = if (s.timeSlot == TimeSlot.AS_NEEDED) null else s.specificTime,
                createdAt = now,
                updatedAt = now
            )
            val savedId = if (medicationId == null) medicationRepository.save(med)
            else { medicationRepository.update(med.copy(id = medicationId)); medicationId }

            val schedules = if (s.selectedDays.isEmpty()) emptyList()
            else s.selectedDays.map { day ->
                MedicationSchedule(
                    medicationId = savedId,
                    dayOfWeek = day,
                    doseOverride = s.doseOverrides[day]
                )
            }
            medicationRepository.replaceSchedules(savedId, schedules)

            val today = LocalDate.now()
            if (medicationId != null) {
                intakeRepository.deleteFuturePendingByMedication(savedId, today)
            }
            generateDailyIntakesUseCase(today)
            rescheduleRemindersUseCase()

            _form.update { it.copy(isSaving = false, isSaved = true) }
        }
    }

    fun delete(medication: Medication) {
        viewModelScope.launch { medicationRepository.delete(medication) }
    }
}
