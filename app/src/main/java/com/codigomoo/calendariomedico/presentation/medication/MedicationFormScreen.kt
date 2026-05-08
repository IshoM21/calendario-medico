package com.codigomoo.calendariomedico.presentation.medication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.codigomoo.calendariomedico.domain.model.TimeSlot
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")

private val DAYS_ORDERED = listOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
)
private val DAY_LABELS = mapOf(
    DayOfWeek.MONDAY to "L", DayOfWeek.TUESDAY to "M", DayOfWeek.WEDNESDAY to "X",
    DayOfWeek.THURSDAY to "J", DayOfWeek.FRIDAY to "V",
    DayOfWeek.SATURDAY to "S", DayOfWeek.SUNDAY to "D"
)
private val DAY_NAMES = mapOf(
    DayOfWeek.MONDAY to "Lunes", DayOfWeek.TUESDAY to "Martes", DayOfWeek.WEDNESDAY to "Miércoles",
    DayOfWeek.THURSDAY to "Jueves", DayOfWeek.FRIDAY to "Viernes",
    DayOfWeek.SATURDAY to "Sábado", DayOfWeek.SUNDAY to "Domingo"
)
private val TIMESLOT_LABELS = mapOf(
    TimeSlot.MORNING to "Mañana", TimeSlot.NOON to "Comida",
    TimeSlot.NIGHT to "Noche", TimeSlot.AS_NEEDED to "S/N"
)
private val PRESET_COLORS = listOf(
    "#1F8A8A", "#E67E22", "#5C6BC0", "#2E7D32", "#C62828", "#6D4C41", "#455A64", "#7B1FA2"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MedicationFormScreen(
    treatmentId: Long,
    medicationId: Long? = null,
    navController: NavController,
    viewModel: MedicationViewModel = hiltViewModel()
) {
    val form by viewModel.form.collectAsStateWithLifecycle()

    LaunchedEffect(medicationId) {
        if (medicationId != null) viewModel.loadForEdit(medicationId)
        else viewModel.resetForm(treatmentId)
    }

    LaunchedEffect(form.isSaved) {
        if (form.isSaved) navController.popBackStack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (medicationId == null) "Nuevo medicamento" else "Editar medicamento") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = form.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Nombre del medicamento *") },
                isError = form.nameError != null,
                supportingText = form.nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = form.dose,
                onValueChange = viewModel::onDoseChange,
                label = { Text("Dosis *") },
                placeholder = { Text("Ej: 1 tableta, 5 mg") },
                isError = form.doseError != null,
                supportingText = form.doseError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = form.instructions,
                onValueChange = viewModel::onInstructionsChange,
                label = { Text("Instrucciones (opcional)") },
                placeholder = { Text("Ej: Con alimentos") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Horario", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    TimeSlot.entries.forEachIndexed { index, slot ->
                        SegmentedButton(
                            selected = form.timeSlot == slot,
                            onClick = { viewModel.onTimeSlotChange(slot) },
                            shape = SegmentedButtonDefaults.itemShape(index, TimeSlot.entries.size),
                            label = { Text(TIMESLOT_LABELS[slot] ?: slot.name, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            if (form.timeSlot != TimeSlot.AS_NEEDED) {
                SpecificTimeRow(
                    specificTime = form.specificTime,
                    onTimeSelected = viewModel::onSpecificTimeChange
                )
            }

            if (form.timeSlot != TimeSlot.AS_NEEDED) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Días (sin selección = todos los días)",
                        style = MaterialTheme.typography.labelLarge
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DAYS_ORDERED.forEach { day ->
                            FilterChip(
                                selected = day in form.selectedDays,
                                onClick = { viewModel.onDayToggle(day) },
                                label = { Text(DAY_LABELS[day] ?: "") }
                            )
                        }
                    }

                    if (form.selectedDays.isNotEmpty()) {
                        Text(
                            text = "Dosis por día (deja vacío para usar la dosis principal)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        form.selectedDays.sorted().forEach { day ->
                            OutlinedTextField(
                                value = form.doseOverrides[day] ?: "",
                                onValueChange = { viewModel.onDoseOverrideChange(day, it) },
                                label = { Text(DAY_NAMES[day] ?: "") },
                                placeholder = { Text(form.dose.ifBlank { "Dosis" }) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Medicamento obligatorio", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (form.isRequired) "Cuenta en el progreso diario"
                        else "No afecta el porcentaje de cumplimiento",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
                Switch(checked = form.isRequired, onCheckedChange = viewModel::onIsRequiredChange)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Color (opcional)", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PRESET_COLORS.forEach { hex ->
                        val color = runCatching {
                            Color(android.graphics.Color.parseColor(hex))
                        }.getOrElse { Color.Gray }
                        val isSelected = form.colorHex == hex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                                    else Modifier
                                )
                                .clickable { viewModel.onColorChange(if (isSelected) null else hex) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Done,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.save(treatmentId, medicationId) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !form.isSaving
            ) {
                Text(if (form.isSaving) "Guardando…" else "Guardar")
            }
            OutlinedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpecificTimeRow(
    specificTime: LocalTime?,
    onTimeSelected: (LocalTime?) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Hora específica", style = MaterialTheme.typography.bodyLarge)
            Text(
                if (specificTime != null) specificTime.format(TIME_FMT)
                else "Usa el horario del slot (Mañana / Comida / Noche)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (specificTime != null) {
                TextButton(onClick = { onTimeSelected(null) }) { Text("Quitar") }
            }
            TextButton(onClick = { showPicker = true }) {
                Text(if (specificTime != null) "Cambiar" else "Asignar")
            }
        }
    }

    if (showPicker) {
        val initial = specificTime ?: LocalTime.of(8, 0)
        val state = rememberTimePickerState(
            initialHour = initial.hour,
            initialMinute = initial.minute,
            is24Hour = true
        )
        Dialog(onDismissRequest = { showPicker = false }) {
            androidx.compose.material3.Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = androidx.compose.ui.Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text("Selecciona la hora", style = MaterialTheme.typography.labelLarge)
                    TimePicker(state = state)
                    Row(
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showPicker = false }) { Text("Cancelar") }
                        TextButton(onClick = {
                            onTimeSelected(LocalTime.of(state.hour, state.minute))
                            showPicker = false
                        }) { Text("Aceptar") }
                    }
                }
            }
        }
    }
}
