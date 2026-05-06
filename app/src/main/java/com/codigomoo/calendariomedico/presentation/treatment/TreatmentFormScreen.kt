package com.codigomoo.calendariomedico.presentation.treatment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.codigomoo.calendariomedico.core.date.toDisplayString
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreatmentFormScreen(
    treatmentId: Long? = null,
    navController: NavController,
    viewModel: TreatmentViewModel = hiltViewModel()
) {
    val form by viewModel.form.collectAsStateWithLifecycle()

    LaunchedEffect(treatmentId) {
        if (treatmentId != null) viewModel.loadForEdit(treatmentId)
        else viewModel.resetForm()
    }

    LaunchedEffect(form.isSaved) {
        if (form.isSaved) navController.popBackStack()
    }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    if (showStartDatePicker) {
        DateSelectorDialog(
            initial = form.startDate,
            onConfirm = { viewModel.onStartDateChange(it); showStartDatePicker = false },
            onDismiss = { showStartDatePicker = false }
        )
    }
    if (showEndDatePicker) {
        DateSelectorDialog(
            initial = form.endDate,
            onConfirm = { viewModel.onEndDateChange(it); showEndDatePicker = false },
            onDismiss = { showEndDatePicker = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (treatmentId == null) "Nuevo tratamiento" else "Editar tratamiento") }
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
                label = { Text("Nombre del tratamiento *") },
                isError = form.nameError != null,
                supportingText = form.nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = form.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Descripción (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            DateFieldButton(
                label = "Fecha de inicio",
                date = form.startDate,
                onClick = { showStartDatePicker = true }
            )

            DateFieldButton(
                label = "Fecha de fin",
                date = form.endDate,
                onClick = { showEndDatePicker = true },
                error = form.dateError
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Tratamiento activo", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Solo puede haber uno activo a la vez",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
                Switch(
                    checked = form.isActive,
                    onCheckedChange = viewModel::onIsActiveChange
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.save(treatmentId) },
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

@Composable
private fun DateFieldButton(
    label: String,
    date: LocalDate,
    onClick: () -> Unit,
    error: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(date.toDisplayString())
        }
        error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSelectorDialog(
    initial: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis ->
                    onConfirm(LocalDate.ofEpochDay(millis / 86_400_000L))
                }
            }) { Text("Aceptar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    ) { DatePicker(state = state) }
}
