package com.codigomoo.calendariomedico.presentation.medication

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@Composable
fun MedicationFormScreen(
    treatmentId: Long,
    medicationId: Long? = null,
    navController: NavController
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Formulario Medicamento", style = MaterialTheme.typography.headlineMedium)
    }
}
