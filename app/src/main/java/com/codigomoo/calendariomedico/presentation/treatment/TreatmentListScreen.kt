package com.codigomoo.calendariomedico.presentation.treatment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.codigomoo.calendariomedico.core.date.toShortDisplayString
import com.codigomoo.calendariomedico.domain.model.Treatment
import com.codigomoo.calendariomedico.presentation.navigation.Route

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreatmentListScreen(
    navController: NavController,
    viewModel: TreatmentViewModel = hiltViewModel()
) {
    val treatments by viewModel.treatments.collectAsStateWithLifecycle(emptyList())
    var treatmentToDelete by remember { mutableStateOf<Treatment?>(null) }

    treatmentToDelete?.let { t ->
        AlertDialog(
            onDismissRequest = { treatmentToDelete = null },
            title = { Text("Eliminar tratamiento") },
            text = { Text("¿Eliminar \"${t.name}\"? Se eliminarán todos sus medicamentos y tomas.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTreatment(t)
                    treatmentToDelete = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { treatmentToDelete = null }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Tratamientos") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Route.TreatmentForm()) }) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo tratamiento")
            }
        }
    ) { innerPadding ->
        if (treatments.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay tratamientos.\nToca + para crear uno.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
            ) {
                items(treatments, key = { it.id }) { treatment ->
                    TreatmentCard(
                        treatment = treatment,
                        onEdit = { navController.navigate(Route.TreatmentForm(treatment.id)) },
                        onDelete = { treatmentToDelete = treatment }
                    )
                }
            }
        }
    }
}

@Composable
private fun TreatmentCard(treatment: Treatment, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onEdit,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(treatment.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (treatment.isActive) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Text(
                                text = "Activo",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "${treatment.startDate.toShortDisplayString()} — ${treatment.endDate.toShortDisplayString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                treatment.description?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
            }
        }
    }
}
