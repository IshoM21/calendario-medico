package com.codigomoo.calendariomedico.presentation.caregiver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codigomoo.calendariomedico.data.preferences.ReminderPreferences
import com.codigomoo.calendariomedico.data.repository.TreatmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CaregiverHubViewModel @Inject constructor(
    treatmentRepository: TreatmentRepository,
    private val reminderPreferences: ReminderPreferences
) : ViewModel() {
    val activeTreatment = treatmentRepository.getActive()
    val patientName = reminderPreferences.patientName

    fun savePatientName(name: String) {
        viewModelScope.launch { reminderPreferences.setPatientName(name) }
    }
}
