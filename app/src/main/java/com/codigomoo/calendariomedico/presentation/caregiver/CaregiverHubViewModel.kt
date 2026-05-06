package com.codigomoo.calendariomedico.presentation.caregiver

import androidx.lifecycle.ViewModel
import com.codigomoo.calendariomedico.data.repository.TreatmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CaregiverHubViewModel @Inject constructor(
    treatmentRepository: TreatmentRepository
) : ViewModel() {
    val activeTreatment = treatmentRepository.getActive()
}
