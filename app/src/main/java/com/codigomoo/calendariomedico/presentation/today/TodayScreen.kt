package com.codigomoo.calendariomedico.presentation.today

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.codigomoo.calendariomedico.core.date.toDisplayString
import com.codigomoo.calendariomedico.domain.model.IntakeStatus
import com.codigomoo.calendariomedico.domain.model.MedicationIntake
import com.codigomoo.calendariomedico.domain.model.TimeSlot
import com.codigomoo.calendariomedico.presentation.navigation.Route
import java.time.LocalDateTime

@Composable
fun TodayScreen(
    navController: NavController,
    innerPadding: PaddingValues = PaddingValues(),
    viewModel: TodayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var intakeIdToSkip by remember { mutableStateOf<Long?>(null) }

    intakeIdToSkip?.let { intakeId ->
        AlertDialog(
            onDismissRequest = { intakeIdToSkip = null },
            title = { Text("Omitir toma") },
            text = { Text("¿Confirmas que deseas omitir esta toma?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.skipIntake(intakeId)
                    intakeIdToSkip = null
                }) { Text("Omitir") }
            },
            dismissButton = {
                TextButton(onClick = { intakeIdToSkip = null }) { Text("Cancelar") }
            }
        )
    }

    uiState.asNeededDialog?.let { dialog ->
        AsNeededRegistrationDialog(
            dialog = dialog,
            onNoteChange = { viewModel.onAsNeededNoteChange(it) },
            onConfirm = { viewModel.confirmAsNeededIntake() },
            onDismiss = { viewModel.dismissAsNeededDialog() }
        )
    }

    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        }

        uiState.error != null -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
        }

        uiState.isFirstLaunch && !uiState.hasActiveTreatment -> {
            OnboardingState(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                onStart = {
                    viewModel.completeOnboarding()
                    navController.navigate(Route.PinLock("set"))
                }
            )
        }

        !uiState.hasActiveTreatment -> {
            EmptyTreatmentState(
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            )
        }

        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 24.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    TodayHeader(uiState = uiState)
                    Spacer(Modifier.height(8.dp))
                }

                if (uiState.isTreatmentExpired) {
                    item(key = "expired_banner") {
                        ExpiredTreatmentBanner(
                            endDate = uiState.treatmentEndDate!!.toDisplayString(),
                            onCreateNew = {
                                navController.navigate(Route.PinLock("enter"))
                            }
                        )
                    }
                } else {
                    if (uiState.isAllDone) {
                        item { AllDoneCard() }
                    } else {
                        uiState.intakesBySlot.forEach { (timeSlot, intakes) ->
                            item(key = "header_${timeSlot.name}") {
                                TimeSlotHeader(timeSlot = timeSlot)
                            }
                            items(intakes, key = { "intake_${it.id}" }) { intake ->
                                IntakeCard(
                                    intake = intake,
                                    onTake = { viewModel.markAsTaken(intake.id) },
                                    onSkip = { intakeIdToSkip = intake.id }
                                )
                            }
                            item(key = "spacer_${timeSlot.name}") { Spacer(Modifier.height(4.dp)) }
                        }
                    }

                    if (uiState.asNeededIntakes.isNotEmpty()) {
                        item(key = "as_needed_header") {
                            AsNeededSectionHeader(
                                count = uiState.asNeededIntakes.size,
                                expanded = uiState.asNeededExpanded,
                                onToggle = { viewModel.toggleAsNeededExpanded() }
                            )
                        }
                        if (uiState.asNeededExpanded) {
                            items(uiState.asNeededIntakes, key = { "as_needed_${it.id}" }) { intake ->
                                AsNeededCard(
                                    intake = intake,
                                    onRegister = { viewModel.startAsNeededRegistration(intake) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayHeader(uiState: TodayUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = uiState.date.toDisplayString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (uiState.treatmentName.isNotEmpty()) {
                Text(
                    text = uiState.treatmentName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { uiState.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
            Text(
                text = "${uiState.totalTaken} de ${uiState.totalRequired} medicamentos tomados",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun ExpiredTreatmentBanner(endDate: String, onCreateNew: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Tratamiento vencido",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = "El tratamiento venció el $endDate. Configura uno nuevo para continuar el seguimiento.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Button(
                onClick = onCreateNew,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Crear nuevo tratamiento")
            }
        }
    }
}

@Composable
private fun TimeSlotHeader(timeSlot: TimeSlot) {
    val color = timeSlot.accentColor()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(22.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(
            text = timeSlot.displayLabel(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun IntakeCard(
    intake: MedicationIntake,
    onTake: () -> Unit,
    onSkip: () -> Unit
) {
    val accentColor = intake.scheduledTimeSlot.accentColor()
    val barColor = when (intake.status) {
        IntakeStatus.TAKEN -> Color(0xFF2E7D32)
        IntakeStatus.SKIPPED, IntakeStatus.MISSED -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        else -> accentColor
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(barColor)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = intake.medicationName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    IntakeStatusBadge(status = intake.status, confirmedAt = intake.confirmedAt)
                }

                Text(
                    text = intake.dose,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                if (intake.status == IntakeStatus.PENDING) {
                    Spacer(Modifier.height(4.dp))
                    Button(onClick = onTake, modifier = Modifier.fillMaxWidth()) {
                        Text("Tomar ahora")
                    }
                    TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Omitir esta toma",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IntakeStatusBadge(status: IntakeStatus, confirmedAt: LocalDateTime?) {
    when (status) {
        IntakeStatus.TAKEN -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Done,
                contentDescription = null,
                tint = Color(0xFF2E7D32),
                modifier = Modifier.size(18.dp)
            )
            confirmedAt?.let {
                Text(
                    text = it.toLocalTime().toDisplayString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF2E7D32)
                )
            }
        }
        IntakeStatus.SKIPPED -> Text(
            text = "Omitida",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        IntakeStatus.MISSED -> Text(
            text = "No tomada",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error
        )
        IntakeStatus.OPTIONAL -> Text(
            text = "Opcional",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        IntakeStatus.PENDING -> Unit
    }
}

@Composable
private fun AsNeededSectionHeader(count: Int, expanded: Boolean, onToggle: () -> Unit) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        label = "chevron"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(22.dp)
                    .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(2.dp))
            )
            Text(
                text = "Si lo necesitas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = "($count)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
            )
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = if (expanded) "Colapsar" else "Expandir",
            modifier = Modifier.rotate(chevronRotation),
            tint = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun AsNeededCard(intake: MedicationIntake, onRegister: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = intake.medicationName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (intake.status == IntakeStatus.TAKEN) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(18.dp)
                        )
                        intake.confirmedAt?.let {
                            Text(
                                text = it.toLocalTime().toDisplayString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
            }

            Text(
                text = intake.dose,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            if (intake.status == IntakeStatus.OPTIONAL) {
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onRegister,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Registrar toma")
                }
            }
        }
    }
}

@Composable
private fun AsNeededRegistrationDialog(
    dialog: AsNeededDialogState,
    onNoteChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialog.intake.medicationName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                dialog.intervalWarning?.let { warning ->
                    Text(
                        text = warning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    HorizontalDivider()
                }
                OutlinedTextField(
                    value = dialog.notes,
                    onValueChange = onNoteChange,
                    label = { Text("Nota (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(if (dialog.intervalWarning != null) "Registrar igualmente" else "Registrar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun AllDoneCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2E7D32).copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Done,
                contentDescription = null,
                tint = Color(0xFF2E7D32),
                modifier = Modifier.size(52.dp)
            )
            Text(
                text = "¡Todo al día!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )
            Text(
                text = "Has tomado todos los medicamentos de hoy",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun OnboardingState(modifier: Modifier = Modifier, onStart: () -> Unit) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.DateRange,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Bienvenido a CalendarioMédico",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Tu asistente personal para el seguimiento de medicamentos.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Aviso de salud: Esta app es una ayuda para el seguimiento y no reemplaza el criterio médico. " +
                    "Consulta siempre a tu médico o farmacéutico antes de modificar tu tratamiento.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Configurar tratamiento")
        }
    }
}

@Composable
private fun EmptyTreatmentState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.DateRange,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Sin tratamiento activo",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Ve a Perfil para configurar un tratamiento",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

private fun TimeSlot.displayLabel(): String = when (this) {
    TimeSlot.MORNING -> "Mañana"
    TimeSlot.NOON -> "Comida"
    TimeSlot.NIGHT -> "Noche"
    TimeSlot.AS_NEEDED -> "Según necesidad"
}

@Composable
private fun TimeSlot.accentColor(): Color = when (this) {
    TimeSlot.MORNING -> MaterialTheme.colorScheme.primary
    TimeSlot.NOON -> Color(0xFFE67E22)
    TimeSlot.NIGHT -> Color(0xFF5C6BC0)
    TimeSlot.AS_NEEDED -> MaterialTheme.colorScheme.secondary
}
