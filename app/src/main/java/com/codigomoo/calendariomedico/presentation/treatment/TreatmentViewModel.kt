package com.codigomoo.calendariomedico.presentation.treatment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codigomoo.calendariomedico.data.repository.IntakeRepository
import com.codigomoo.calendariomedico.data.repository.TreatmentRepository
import com.codigomoo.calendariomedico.domain.model.Treatment
import com.codigomoo.calendariomedico.domain.usecase.RescheduleRemindersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

data class TreatmentFormState(
    val name: String = "",
    val description: String = "",
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate = LocalDate.now().plusMonths(1),
    val isActive: Boolean = true,
    val nameError: String? = null,
    val dateError: String? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false
)

@HiltViewModel
class TreatmentViewModel @Inject constructor(
    private val treatmentRepository: TreatmentRepository,
    private val intakeRepository: IntakeRepository,
    private val rescheduleRemindersUseCase: RescheduleRemindersUseCase
) : ViewModel() {

    val treatments = treatmentRepository.getAll()

    private val _form = MutableStateFlow(TreatmentFormState())
    val form: StateFlow<TreatmentFormState> = _form.asStateFlow()

    fun loadForEdit(id: Long) {
        viewModelScope.launch {
            val t = treatmentRepository.getById(id) ?: return@launch
            _form.update {
                it.copy(
                    name = t.name,
                    description = t.description ?: "",
                    startDate = t.startDate,
                    endDate = t.endDate,
                    isActive = t.isActive,
                    nameError = null,
                    dateError = null,
                    isSaved = false
                )
            }
        }
    }

    fun resetForm() {
        _form.value = TreatmentFormState()
    }

    fun onNameChange(v: String) = _form.update { it.copy(name = v, nameError = null) }
    fun onDescriptionChange(v: String) = _form.update { it.copy(description = v) }
    fun onStartDateChange(v: LocalDate) = _form.update { it.copy(startDate = v, dateError = null) }
    fun onEndDateChange(v: LocalDate) = _form.update { it.copy(endDate = v, dateError = null) }
    fun onIsActiveChange(v: Boolean) = _form.update { it.copy(isActive = v) }

    fun save(treatmentId: Long?) {
        val s = _form.value
        if (s.name.isBlank()) {
            _form.update { it.copy(nameError = "El nombre es obligatorio") }
            return
        }
        if (!s.endDate.isAfter(s.startDate)) {
            _form.update { it.copy(dateError = "La fecha fin debe ser posterior al inicio") }
            return
        }
        viewModelScope.launch {
            _form.update { it.copy(isSaving = true) }
            val treatment = Treatment(
                id = treatmentId ?: 0L,
                name = s.name,
                description = s.description.ifBlank { null },
                startDate = s.startDate,
                endDate = s.endDate,
                isActive = s.isActive,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            val savedId = treatmentRepository.save(treatment)
            if (s.isActive) treatmentRepository.activate(savedId)
            if (treatmentId != null) {
                intakeRepository.deleteFuturePending(savedId, LocalDate.now())
            }
            rescheduleRemindersUseCase()
            _form.update { it.copy(isSaving = false, isSaved = true) }
        }
    }

    fun deleteTreatment(treatment: Treatment) {
        viewModelScope.launch { treatmentRepository.delete(treatment) }
    }
}
