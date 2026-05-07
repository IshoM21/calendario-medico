package com.codigomoo.calendariomedico.presentation.medication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.codigomoo.calendariomedico.core.date.toShortDisplayString
import com.codigomoo.calendariomedico.domain.model.IntakeStatus
import com.codigomoo.calendariomedico.domain.model.TimeSlot
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationDetailScreen(
    medicationId: Long,
    navController: NavController,
    viewModel: MedicationDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(medicationId) { viewModel.load(medicationId) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.medication?.name ?: "Detalle") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        val med = uiState.medication ?: return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = med.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    DetailRow(label = "Dosis", value = med.dose)
                    DetailRow(label = "Horario", value = timeSlotLabel(med.timeSlot))
                    DetailRow(label = "Tipo", value = if (med.isRequired) "Obligatorio" else "Opcional")
                    med.instructions?.takeIf { it.isNotBlank() }?.let {
                        DetailRow(label = "Instrucciones", value = it)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Estado hoy",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                val todayIntake = uiState.todayIntake
                if (todayIntake == null) {
                    Text(
                        text = "Sin registro para hoy",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(todayIntake.dose, style = MaterialTheme.typography.bodyMedium)
                            DetailStatusLabel(status = todayIntake.status)
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Últimos 7 días",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    uiState.last7Days.forEach { (date, status) ->
                        DayStatusCell(date = date, status = status)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.width(100.dp)
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DetailStatusLabel(status: IntakeStatus) {
    val (label, color) = when (status) {
        IntakeStatus.TAKEN -> "Tomado" to Color(0xFF2E7D32)
        IntakeStatus.SKIPPED -> "Omitida" to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        IntakeStatus.MISSED -> "No tomada" to MaterialTheme.colorScheme.error
        IntakeStatus.OPTIONAL -> "Opcional" to MaterialTheme.colorScheme.secondary
        IntakeStatus.PENDING -> "Pendiente" to MaterialTheme.colorScheme.primary
    }
    Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = color)
}

@Composable
private fun DayStatusCell(date: LocalDate, status: IntakeStatus?) {
    val isToday = date == LocalDate.now()
    val (bgColor, label) = when (status) {
        IntakeStatus.TAKEN -> Color(0xFF2E7D32) to "✓"
        IntakeStatus.MISSED -> Color(0xFFC62828) to "✗"
        IntakeStatus.SKIPPED -> Color(0xFF78909C) to "—"
        IntakeStatus.PENDING -> MaterialTheme.colorScheme.primary to "·"
        IntakeStatus.OPTIONAL -> MaterialTheme.colorScheme.secondary to "?"
        null -> MaterialTheme.colorScheme.surfaceVariant to "·"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = date.toShortDisplayString(),
            style = MaterialTheme.typography.labelSmall,
            color = if (isToday) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(bgColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = bgColor)
        }
    }
}

private fun timeSlotLabel(slot: TimeSlot) = when (slot) {
    TimeSlot.MORNING -> "Mañana"
    TimeSlot.NOON -> "Comida"
    TimeSlot.NIGHT -> "Noche"
    TimeSlot.AS_NEEDED -> "Según necesidad"
}
