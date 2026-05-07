package com.codigomoo.calendariomedico.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codigomoo.calendariomedico.data.preferences.ReminderPreferences
import com.codigomoo.calendariomedico.data.repository.IntakeRepository
import com.codigomoo.calendariomedico.data.repository.TreatmentRepository
import com.codigomoo.calendariomedico.domain.model.IntakeStatus
import com.codigomoo.calendariomedico.domain.model.Treatment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DayCompliance(
    val date: LocalDate,
    val percent: Int?
)

data class ProfileUiState(
    val patientName: String = "",
    val activeTreatment: Treatment? = null,
    val weekCompliance: List<DayCompliance> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val treatmentRepository: TreatmentRepository,
    private val intakeRepository: IntakeRepository,
    private val reminderPreferences: ReminderPreferences
) : ViewModel() {

    private val today = LocalDate.now()
    private val weekStart = today.minusDays(6)

    val uiState: StateFlow<ProfileUiState> = combine(
        reminderPreferences.patientName,
        treatmentRepository.getActive(),
        intakeRepository.getByDateRange(weekStart, today)
    ) { name, treatment, intakes ->
        val byDate = intakes.groupBy { it.date }
        val weekCompliance = (0..6).map { offset ->
            val date = weekStart.plusDays(offset.toLong())
            val dayIntakes = byDate[date] ?: emptyList()
            val required = dayIntakes.count { it.status != IntakeStatus.OPTIONAL }
            val taken = dayIntakes.count { it.status == IntakeStatus.TAKEN }
            DayCompliance(
                date = date,
                percent = if (required == 0) null else taken * 100 / required
            )
        }
        ProfileUiState(
            patientName = name,
            activeTreatment = treatment,
            weekCompliance = weekCompliance,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun savePatientName(name: String) {
        viewModelScope.launch { reminderPreferences.setPatientName(name) }
    }
}
