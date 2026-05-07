package com.codigomoo.calendariomedico.presentation.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.codigomoo.calendariomedico.core.date.toDisplayString
import com.codigomoo.calendariomedico.presentation.navigation.Route

@Composable
fun ProfileScreen(
    navController: NavController,
    innerPadding: PaddingValues = PaddingValues(),
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center
        ) { androidx.compose.material3.CircularProgressIndicator() }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Perfil",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Paciente",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
            )
            Text(
                text = uiState.patientName.ifBlank { "Sin nombre" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (uiState.patientName.isBlank())
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                else
                    MaterialTheme.colorScheme.onBackground
            )
        }

        HorizontalDivider()

        Text(
            text = "Tratamiento activo",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        val treatment = uiState.activeTreatment
        if (treatment == null) {
            Text(
                text = "Sin tratamiento activo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = treatment.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${treatment.startDate.toDisplayString()} — ${treatment.endDate.toDisplayString()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    treatment.description?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        Text(
            text = "Cumplimiento — últimos 7 días",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        if (uiState.weekCompliance.isEmpty()) {
            Text(
                text = "Sin datos",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                uiState.weekCompliance.forEach { day ->
                    ComplianceDayCell(day = day)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { navController.navigate(Route.PinLock("enter")) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Modo Cuidador")
        }
    }
}

@Composable
private fun ComplianceDayCell(day: DayCompliance) {
    val today = java.time.LocalDate.now()
    val isToday = day.date == today
    val (bgColor, label) = when {
        day.percent == null -> MaterialTheme.colorScheme.surfaceVariant to "—"
        day.percent >= 100 -> Color(0xFF2E7D32) to "✓"
        day.percent >= 75 -> Color(0xFFE67E22) to "${day.percent}%"
        else -> MaterialTheme.colorScheme.error to "${day.percent}%"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = day.date.toShortDisplayString(),
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
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = bgColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
