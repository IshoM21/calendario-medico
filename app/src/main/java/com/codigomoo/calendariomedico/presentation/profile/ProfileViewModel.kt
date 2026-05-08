package com.codigomoo.calendariomedico.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codigomoo.calendariomedico.data.preferences.ReminderPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val patientName: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val reminderPreferences: ReminderPreferences
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = reminderPreferences.patientName
        .map { name -> ProfileUiState(patientName = name, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun savePatientName(name: String) {
        viewModelScope.launch { reminderPreferences.setPatientName(name) }
    }
}
