package com.codigomoo.calendariomedico.presentation.settings

import android.app.AlarmManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.codigomoo.calendariomedico.core.date.toDisplayString
import com.codigomoo.calendariomedico.presentation.navigation.Route
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    val needsExactAlarmPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        !alarmManager.canScheduleExactAlarms()

    var showMorningPicker by remember { mutableStateOf(false) }
    var showNoonPicker by remember { mutableStateOf(false) }
    var showNightPicker by remember { mutableStateOf(false) }

    var patientNameDraft by remember { mutableStateOf("") }
    var minutesDraft by remember { mutableStateOf("") }

    LaunchedEffect(uiState.patientName) { patientNameDraft = uiState.patientName }
    LaunchedEffect(uiState.pendingAlertDelayMinutes) { minutesDraft = uiState.pendingAlertDelayMinutes.toString() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { SectionLabel("Horarios de aviso") }
            item {
                SettingsCard {
                    TimeRow("Mañana", uiState.morningTime) { showMorningPicker = true }
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    TimeRow("Comida", uiState.noonTime) { showNoonPicker = true }
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    TimeRow("Noche", uiState.nightTime) { showNightPicker = true }
                }
            }

            item { SectionLabel("Notificaciones") }
            item {
                SettingsCard {
                    SwitchRow(
                        label = "Activar notificaciones",
                        checked = uiState.notificationsEnabled,
                        onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    MinutesRow(
                        label = "Alerta de pendientes",
                        value = minutesDraft,
                        onChange = { minutesDraft = it },
                        onDone = {
                            val mins = minutesDraft.toIntOrNull()?.coerceIn(1, 120) ?: 30
                            minutesDraft = mins.toString()
                            viewModel.setPendingAlertDelayMinutes(mins)
                            focusManager.clearFocus()
                        }
                    )
                }
            }

            item { SectionLabel("Cuenta") }
            item {
                SettingsCard {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        OutlinedTextField(
                            value = patientNameDraft,
                            onValueChange = { patientNameDraft = it },
                            label = { Text("Nombre del paciente") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                viewModel.setPatientName(patientNameDraft.trim())
                                focusManager.clearFocus()
                            })
                        )
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    ClickableRow(label = "Cambiar PIN") {
                        navController.navigate(Route.PinLock("set"))
                    }
                }
            }

            if (needsExactAlarmPermission) {
                item { SectionLabel("Permisos") }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Alarmas exactas no disponibles",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Sin este permiso las notificaciones pueden llegar con retraso. Actívalo en Ajustes del sistema.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            TextButton(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        context.startActivity(
                                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                        )
                                    }
                                }
                            ) {
                                Text(
                                    "Abrir ajustes del sistema",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            item { SectionLabel("Acerca de") }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Aviso de salud: Esta app es una ayuda para el seguimiento de medicamentos y no reemplaza el criterio médico. " +
                            "Consulta siempre a tu médico o farmacéutico antes de modificar tu tratamiento.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }

    if (showMorningPicker) {
        TimePickerDialog(
            title = "Horario de mañana",
            initialTime = uiState.morningTime,
            onConfirm = { viewModel.setMorningTime(it) },
            onDismiss = { showMorningPicker = false }
        )
    }
    if (showNoonPicker) {
        TimePickerDialog(
            title = "Horario de comida",
            initialTime = uiState.noonTime,
            onConfirm = { viewModel.setNoonTime(it) },
            onDismiss = { showNoonPicker = false }
        )
    }
    if (showNightPicker) {
        TimePickerDialog(
            title = "Horario de noche",
            initialTime = uiState.nightTime,
            onConfirm = { viewModel.setNightTime(it) },
            onDismiss = { showNightPicker = false }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(content = content)
    }
}

@Composable
private fun TimeRow(label: String, time: LocalTime, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = time.toDisplayString(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun MinutesRow(label: String, value: String, onChange: (String) -> Unit, onDone: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = value,
                onValueChange = { if (it.length <= 3) onChange(it.filter(Char::isDigit)) },
                modifier = Modifier.width(72.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onDone() })
            )
            Spacer(Modifier.width(6.dp))
            Text("min", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ClickableRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Icon(
            Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    title: String,
    initialTime: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true
    )
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(28.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(24.dp))
                TimePicker(state = state)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        onConfirm(LocalTime.of(state.hour, state.minute))
                        onDismiss()
                    }) { Text("Aceptar") }
                }
            }
        }
    }
}
