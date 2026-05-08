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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
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

    private val tickerFlow = flow {
        while (true) {
            emit(LocalTime.now())
            delay(60_000L)
        }
    }

    private data class SlotTimesState(val times: SlotTimes, val currentTime: LocalTime)

    init {
        viewModelScope.launch { intakeRepository.markPendingBeforeDateAsMissed(LocalDate.now()) }
        loadIntakes()
    }

    private fun loadIntakes() {
        viewModelScope.launch {
            try {
                val slotTimesFlow = combine(
                    reminderPreferences.morningTime,
                    reminderPreferences.noonTime,
                    reminderPreferences.nightTime,
                    tickerFlow
                ) { morning, noon, night, currentTime ->
                    SlotTimesState(SlotTimes(morning, noon, night), currentTime)
                }

                treatmentRepository.getActive()
                    .flatMapLatest { treatment ->
                        if (treatment == null) {
                            combine(
                                reminderPreferences.hasCompletedOnboarding,
                                reminderPreferences.patientName,
                                slotTimesFlow
                            ) { onboarded, name, slotTimesState ->
                                buildUiState(null, emptyMap(), emptyList(), onboarded, name, slotTimesState)
                            }
                        } else {
                            combine(
                                getTodayIntakesUseCase(),
                                medicationRepository.getByTreatment(treatment.id),
                                reminderPreferences.hasCompletedOnboarding,
                                reminderPreferences.patientName,
                                slotTimesFlow
                            ) { intakesBySlot, allMeds, onboarded, patientName, slotTimesState ->
                                buildUiState(treatment, intakesBySlot, allMeds, onboarded, patientName, slotTimesState)
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
                                asNeededDialog = current.asNeededDialog,
                                expandedSlots = current.expandedSlots,
                                markTakenDialog = current.markTakenDialog,
                                navigateToConfirmation = current.navigateToConfirmation
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error al cargar datos") }
            }
        }
    }

    fun markAsTaken(intakeId: Long) {
        val state = _uiState.value
        val fmt = DateTimeFormatter.ofPattern("H:mm")
        val item = state.orderedSlots.flatMap { it.second }.find { it.intake.id == intakeId }
        val confirmData = if (item != null) {
            val next = findNextPendingAfter(intakeId, state)
            TakenConfirmData(
                label = "${item.intake.medicationName} ${item.intake.dose}",
                confirmedAtTime = LocalTime.now().format(fmt),
                nextMedicationName = next?.first ?: "",
                nextSlotTime = next?.second ?: ""
            )
        } else null
        _uiState.update { it.copy(markTakenDialog = null, navigateToConfirmation = confirmData) }
        viewModelScope.launch { markIntakeAsTakenUseCase(intakeId) }
    }

    fun confirmationNavigated() {
        _uiState.update { it.copy(navigateToConfirmation = null) }
    }

    private fun findNextPendingAfter(excludeId: Long, state: TodayUiState): Pair<String, String>? {
        val fmt = DateTimeFormatter.ofPattern("H:mm")
        for ((slot, items) in state.orderedSlots) {
            val item = items.firstOrNull {
                it.intake.id != excludeId && it.intake.status == IntakeStatus.PENDING
            } ?: continue
            return item.intake.medicationName to state.slotTimes.timeOf(slot).format(fmt)
        }
        return null
    }

    fun skipIntake(intakeId: Long) {
        viewModelScope.launch { skipIntakeUseCase(intakeId) }
    }

    fun completeOnboarding() {
        viewModelScope.launch { reminderPreferences.setHasCompletedOnboarding(true) }
    }

    fun toggleSlotExpanded(slot: TimeSlot) {
        _uiState.update { current ->
            val newExpanded = if (slot in current.expandedSlots)
                current.expandedSlots - slot
            else
                current.expandedSlots + slot
            current.copy(expandedSlots = newExpanded)
        }
    }

    fun markNextAsTaken() {
        val pending = _uiState.value.nextSlotPendingIntakes
        when {
            pending.isEmpty() -> return
            pending.size == 1 -> markAsTaken(pending.first().id)
            else -> _uiState.update { it.copy(markTakenDialog = pending) }
        }
    }

    fun dismissMarkTakenDialog() {
        _uiState.update { it.copy(markTakenDialog = null) }
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
        allMeds: List<Medication>,
        hasCompletedOnboarding: Boolean,
        patientName: String,
        slotTimesState: SlotTimesState
    ): TodayUiState {
        val slotTimes = slotTimesState.times
        val currentTime = slotTimesState.currentTime
        val allMedsById = allMeds.associateBy { it.id }

        val regularSlots = intakesBySlot.filterKeys { it != TimeSlot.AS_NEEDED }
        val allRequired = regularSlots.values.flatten()
        val totalRequired = allRequired.count { it.status != IntakeStatus.OPTIONAL }
        val totalTaken = allRequired.count { it.status == IntakeStatus.TAKEN }

        val orderedSlots = listOf(TimeSlot.MORNING, TimeSlot.NOON, TimeSlot.NIGHT)
            .mapNotNull { slot ->
                val intakes = regularSlots[slot]
                if (!intakes.isNullOrEmpty()) {
                    val items = intakes.map { intake ->
                        val med = allMedsById[intake.medicationId]
                        SlotMedItem(intake, med?.colorHex, med?.instructions)
                    }
                    slot to items
                } else null
            }
            .sortedWith(compareBy({ slotPriority(it.first, slotTimesState) }, { it.first.ordinal }))

        var nextIntakeInfo: NextIntakeInfo? = null
        var nextSlotPendingIntakes: List<MedicationIntake> = emptyList()
        for (slot in listOf(TimeSlot.MORNING, TimeSlot.NOON, TimeSlot.NIGHT).sortedBy { slotTimes.timeOf(it) }) {
            val pending = intakesBySlot[slot]?.filter { it.status == IntakeStatus.PENDING } ?: continue
            if (pending.isNotEmpty()) {
                val first = pending.first()
                nextIntakeInfo = NextIntakeInfo(first, allMedsById[first.medicationId]?.instructions)
                nextSlotPendingIntakes = pending
                break
            }
        }

        val asNeededTakenToday = intakesBySlot[TimeSlot.AS_NEEDED] ?: emptyList()
        val asNeededMeds = allMeds.filter { it.timeSlot == TimeSlot.AS_NEEDED }
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
            patientName = patientName,
            slotTimes = slotTimes,
            currentTime = currentTime,
            orderedSlots = orderedSlots,
            nextIntakeInfo = nextIntakeInfo,
            nextSlotPendingIntakes = nextSlotPendingIntakes,
            asNeededItems = asNeededItems,
            totalRequired = totalRequired,
            totalTaken = totalTaken,
            hasActiveTreatment = treatment != null,
            isFirstLaunch = !hasCompletedOnboarding
        )
    }

    private fun slotPriority(slot: TimeSlot, state: SlotTimesState): Int {
        val start: LocalTime
        val end: LocalTime
        when (slot) {
            TimeSlot.MORNING -> { start = state.times.morning; end = state.times.noon.minusMinutes(1) }
            TimeSlot.NOON -> { start = state.times.noon; end = state.times.night.minusMinutes(1) }
            TimeSlot.NIGHT -> { start = state.times.night; end = LocalTime.of(23, 59) }
            else -> return 1
        }
        return when {
            !state.currentTime.isBefore(start) && !state.currentTime.isAfter(end) -> 0
            state.currentTime.isBefore(start) -> 1
            else -> 2
        }
    }
}
