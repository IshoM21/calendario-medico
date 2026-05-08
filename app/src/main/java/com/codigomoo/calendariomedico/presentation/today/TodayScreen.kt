package com.codigomoo.calendariomedico.presentation.today

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.codigomoo.calendariomedico.domain.model.IntakeStatus
import com.codigomoo.calendariomedico.domain.model.MedicationIntake
import com.codigomoo.calendariomedico.domain.model.TimeSlot
import com.codigomoo.calendariomedico.presentation.navigation.Route
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodayScreen(
    navController: NavController,
    innerPadding: PaddingValues = PaddingValues(),
    viewModel: TodayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val confirmData = uiState.navigateToConfirmation
    androidx.compose.runtime.LaunchedEffect(confirmData) {
        confirmData ?: return@LaunchedEffect
        navController.navigate(
            Route.IntakeTaken(
                label = confirmData.label,
                confirmedAtTime = confirmData.confirmedAtTime,
                nextMedicationName = confirmData.nextMedicationName,
                nextSlotTime = confirmData.nextSlotTime
            )
        )
        viewModel.confirmationNavigated()
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionsRefreshKey by remember { mutableStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionsRefreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val alarmManager = context.getSystemService(AlarmManager::class.java)
    val needsAlarmPermission = (permissionsRefreshKey >= 0) &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        !alarmManager.canScheduleExactAlarms()

    val needsNotifPermission = (permissionsRefreshKey >= 0) &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

    var notifPermissionRequested by rememberSaveable { mutableStateOf(false) }
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { notifPermissionRequested = true }

    val notifPermanentlyDenied = needsNotifPermission && notifPermissionRequested &&
        (context as? android.app.Activity)
            ?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == false

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

    uiState.markTakenDialog?.let { intakes ->
        MarkTakenDialog(
            intakes = intakes,
            onSelect = { viewModel.markAsTaken(it.id) },
            onDismiss = viewModel::dismissMarkTakenDialog
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
            EmptyTreatmentState(modifier = Modifier.fillMaxSize().padding(innerPadding))
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (needsNotifPermission) {
                    item(key = "notif_banner") {
                        NotificationsPermissionBanner(
                            permanentlyDenied = notifPermanentlyDenied,
                            onActivate = {
                                if (notifPermanentlyDenied) {
                                    context.startActivity(
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                    )
                                } else {
                                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        )
                    }
                }
                if (needsAlarmPermission) {
                    item(key = "alarm_banner") {
                        AlarmPermissionBanner(
                            onFix = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                                }
                            }
                        )
                    }
                }

                item(key = "header") { TodayHeader(uiState) }
                item(key = "progress") { ProgressCard(uiState) }

                if (uiState.isTreatmentExpired) {
                    item(key = "expired") {
                        ExpiredTreatmentBanner(
                            endDate = uiState.treatmentEndDate!!
                                .format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale("es", "MX"))),
                            onCreateNew = {
                                navController.navigate(Route.PinLock("enter", "treatment_form"))
                            }
                        )
                    }
                } else {
                    val nextIntakeInfo = uiState.nextIntakeInfo
                    if (uiState.isAllDone) {
                        item(key = "done") { DiaCompletoCard() }
                    } else if (nextIntakeInfo != null) {
                        item(key = "proxima") {
                            ProximaTomadCard(
                                info = nextIntakeInfo,
                                slotTimes = uiState.slotTimes,
                                currentTime = uiState.currentTime,
                                onMark = viewModel::markNextAsTaken
                            )
                        }
                    }

                    uiState.orderedSlots.forEach { (slot, items) ->
                        item(key = "sh_${slot.name}") {
                            SlotSectionHeader(
                                slot = slot,
                                count = items.size,
                                slotTimes = uiState.slotTimes,
                                expanded = slot in uiState.expandedSlots,
                                onToggle = { viewModel.toggleSlotExpanded(slot) }
                            )
                        }
                        if (slot in uiState.expandedSlots) {
                            items(items, key = { "si_${it.intake.id}" }) { item ->
                                SlotMedCard(
                                    item = item,
                                    slotTime = uiState.slotTimes.timeOf(slot),
                                    onTake = { viewModel.markAsTaken(item.intake.id) },
                                    onSkip = { intakeIdToSkip = item.intake.id }
                                )
                            }
                        }
                    }

                    if (uiState.asNeededItems.isNotEmpty()) {
                        item(key = "as_needed_header") {
                            AsNeededSectionHeader(
                                count = uiState.asNeededItems.size,
                                expanded = uiState.asNeededExpanded,
                                onToggle = { viewModel.toggleAsNeededExpanded() }
                            )
                        }
                        if (uiState.asNeededExpanded) {
                            items(uiState.asNeededItems, key = { "an_${it.medicationId}" }) { item ->
                                AsNeededCard(
                                    item = item,
                                    onRegister = { viewModel.startAsNeededRegistration(item) }
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
    val dateLabel = uiState.date
        .format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("es", "MX")))
        .uppercase(Locale("es", "MX"))
    val greeting = if (uiState.patientName.isNotBlank()) "Hola, ${uiState.patientName}" else "Hola"
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
        )
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (uiState.totalRequired > 0) {
            Text(
                text = "Hoy te tocan ${uiState.totalRequired} tomas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ProgressCard(uiState: TodayUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Progreso del día",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "${uiState.totalTaken} de ${uiState.totalRequired}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            LinearProgressIndicator(
                progress = { uiState.progressFraction },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )
        }
    }
}

@Composable
private fun ProximaTomadCard(
    info: NextIntakeInfo,
    slotTimes: SlotTimes,
    currentTime: LocalTime,
    onMark: () -> Unit
) {
    val slotTime = slotTimes.timeOf(info.intake.scheduledTimeSlot)
    val minutesUntil = java.time.Duration.between(currentTime, slotTime).toMinutes()
    val timeFmt = DateTimeFormatter.ofPattern("H:mm")
    val subtitle = buildString {
        if (minutesUntil > 0) append("En ${minutesUntil} min · ")
        append(slotTime.format(timeFmt))
        if (info.instructions != null) append(" · ${info.instructions}")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "TU PRÓXIMA TOMA",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
            Text(
                text = info.intake.medicationName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onMark,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Marcar como tomado", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DiaCompletoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32).copy(alpha = 0.08f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF2E7D32),
                modifier = Modifier.size(56.dp)
            )
            Text(
                text = "¡Día completo!",
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
private fun SlotSectionHeader(
    slot: TimeSlot,
    count: Int,
    slotTimes: SlotTimes,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val accentColor = slot.accentColor()
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "chevron_${slot.name}"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = slot.icon(),
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = slot.displayLabel(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = accentColor
            )
            Text(
                text = slotTimes.rangeLabel(slot),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
        Box(
            modifier = Modifier
                .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = if (expanded) "Colapsar" else "Expandir",
            modifier = Modifier.size(20.dp).rotate(chevronRotation),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun SlotMedCard(
    item: SlotMedItem,
    slotTime: LocalTime,
    onTake: () -> Unit,
    onSkip: () -> Unit
) {
    val intake = item.intake
    val isTaken = intake.status == IntakeStatus.TAKEN
    val isMissedOrSkipped = intake.status == IntakeStatus.MISSED || intake.status == IntakeStatus.SKIPPED
    val medColor = remember(item.colorHex) {
        item.colorHex?.let {
            try { Color(android.graphics.Color.parseColor(it)) } catch (_: Exception) { null }
        }
    }
    val dotColor = medColor ?: Color(0xFF1F8A8A)
    val timeFmt = DateTimeFormatter.ofPattern("H:mm")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isTaken) MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                             else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(dotColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(dotColor, CircleShape)
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = intake.medicationName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isTaken) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isTaken) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(
                    text = "${intake.dose} · ${slotTime.format(timeFmt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                if (item.instructions != null) {
                    Text(
                        text = item.instructions,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            when {
                isTaken -> Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Tomado",
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(28.dp)
                )
                isMissedOrSkipped -> Icon(
                    imageVector = Icons.Default.RadioButtonUnchecked,
                    contentDescription = intake.status.name,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                    modifier = Modifier.size(28.dp)
                )
                else -> Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = onSkip,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "Omitir",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Pendiente — toca para marcar",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable(onClick = onTake)
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkTakenDialog(
    intakes: List<MedicationIntake>,
    onSelect: (MedicationIntake) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Cuál tomaste?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                intakes.forEach { intake ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(intake) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(intake.medicationName, fontWeight = FontWeight.SemiBold)
                            Text(
                                intake.dose,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun AsNeededSectionHeader(count: Int, expanded: Boolean, onToggle: () -> Unit) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "chevron_as_needed"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Si lo necesitas",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = "$count medicamentos",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
            )
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = if (expanded) "Colapsar" else "Expandir",
            modifier = Modifier.size(20.dp).rotate(chevronRotation),
            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun AsNeededCard(item: AsNeededItem, onRegister: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.medicationName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = item.dose,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                if (item.takenToday.isNotEmpty()) {
                    Text(
                        text = if (item.takenToday.size == 1) "1 vez hoy"
                               else "${item.takenToday.size} veces hoy",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
            OutlinedButton(
                onClick = onRegister,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Registrar", style = MaterialTheme.typography.labelMedium)
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
        title = { Text(dialog.medicationName) },
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
private fun ExpiredTreatmentBanner(endDate: String, onCreateNew: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
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
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
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

@Composable
private fun NotificationsPermissionBanner(permanentlyDenied: Boolean, onActivate: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (permanentlyDenied)
                    "Notificaciones bloqueadas. Actívalas desde Ajustes del sistema."
                else
                    "Activa las notificaciones para recibir recordatorios.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onActivate) {
                Text(
                    text = if (permanentlyDenied) "Ajustes" else "Activar",
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun AlarmPermissionBanner(onFix: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Las alarmas exactas no están activas. Las notificaciones pueden llegar tarde.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onFix) {
                Text("Activar", color = MaterialTheme.colorScheme.error)
            }
        }
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
    TimeSlot.MORNING -> Color(0xFFE67E22)
    TimeSlot.NOON -> MaterialTheme.colorScheme.primary
    TimeSlot.NIGHT -> Color(0xFF5C6BC0)
    TimeSlot.AS_NEEDED -> MaterialTheme.colorScheme.secondary
}

private fun TimeSlot.icon(): ImageVector = when (this) {
    TimeSlot.MORNING -> Icons.Default.WbSunny
    TimeSlot.NOON -> Icons.Default.Restaurant
    TimeSlot.NIGHT -> Icons.Default.Bedtime
    TimeSlot.AS_NEEDED -> Icons.Default.Notifications
}
