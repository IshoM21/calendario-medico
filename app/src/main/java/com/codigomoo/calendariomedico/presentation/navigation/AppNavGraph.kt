package com.codigomoo.calendariomedico.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.codigomoo.calendariomedico.core.notification.InAppReminderEvent
import com.codigomoo.calendariomedico.domain.model.TimeSlot
import com.codigomoo.calendariomedico.presentation.calendar.CalendarScreen
import com.codigomoo.calendariomedico.presentation.caregiver.CaregiverHubScreen
import com.codigomoo.calendariomedico.presentation.caregiver.PinLockScreen
import com.codigomoo.calendariomedico.presentation.history.HistoryScreen
import com.codigomoo.calendariomedico.presentation.medication.MedicationDetailScreen
import com.codigomoo.calendariomedico.presentation.medication.MedicationFormScreen
import com.codigomoo.calendariomedico.presentation.medication.MedicationListScreen
import com.codigomoo.calendariomedico.presentation.profile.ProfileScreen
import com.codigomoo.calendariomedico.presentation.settings.SettingsScreen
import com.codigomoo.calendariomedico.presentation.today.ConfirmationScreen
import com.codigomoo.calendariomedico.presentation.today.TodayScreen
import com.codigomoo.calendariomedico.presentation.treatment.TreatmentFormScreen
import com.codigomoo.calendariomedico.presentation.treatment.TreatmentListScreen
import kotlinx.serialization.Serializable
import java.time.LocalTime
import java.time.format.DateTimeFormatter

sealed interface Route {
    @Serializable data object Today : Route
    @Serializable data object Calendar : Route
    @Serializable data object History : Route
    @Serializable data object Profile : Route
    @Serializable data object TreatmentList : Route
    @Serializable data class TreatmentForm(val treatmentId: Long? = null) : Route
    @Serializable data class MedicationList(val treatmentId: Long) : Route
    @Serializable data class MedicationForm(val treatmentId: Long, val medicationId: Long? = null) : Route
    @Serializable data class MedicationDetail(val medicationId: Long) : Route
    @Serializable data class PinLock(val mode: String, val destination: String? = null) : Route
    @Serializable data object CaregiverHub : Route
    @Serializable data object Settings : Route
    @Serializable data class IntakeTaken(
        val label: String,
        val confirmedAtTime: String,
        val nextMedicationName: String = "",
        val nextSlotTime: String = ""
    ) : Route
}

@Composable
fun AppNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = navBackStackEntry?.destination

    val showBottomBar = currentDest?.let {
        it.hasRoute(Route.Today::class) ||
        it.hasRoute(Route.Calendar::class) ||
        it.hasRoute(Route.Profile::class)
    } ?: false

    val appViewModel: AppViewModel = hiltViewModel()
    val reminderEvent by appViewModel.reminder.collectAsStateWithLifecycle()
    val takenLabel by appViewModel.takenLabel.collectAsStateWithLifecycle()

    // Navigate to ConfirmationScreen when in-app dialog marks intakes as taken
    LaunchedEffect(takenLabel) {
        takenLabel ?: return@LaunchedEffect
        navController.navigate(
            Route.IntakeTaken(
                label = takenLabel!!,
                confirmedAtTime = LocalTime.now().format(DateTimeFormatter.ofPattern("H:mm"))
            )
        )
        appViewModel.consumeTakenLabel()
    }

    reminderEvent?.let { event ->
        InAppReminderDialog(
            event = event,
            onDismiss = appViewModel::dismissReminder,
            onSnooze = { appViewModel.snooze(event.timeSlot) },
            onMarkTaken = { appViewModel.markAllTaken(event.intakes) }
        )
    }

    Scaffold(
        bottomBar = { if (showBottomBar) AppBottomBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Route.Today,
            modifier = modifier
        ) {
            composable<Route.Today>(
                deepLinks = listOf(navDeepLink { uriPattern = "calendariomedico://today" })
            ) {
                TodayScreen(navController = navController, innerPadding = innerPadding)
            }
            composable<Route.Calendar> {
                CalendarScreen(navController = navController, innerPadding = innerPadding)
            }
            composable<Route.History> {
                HistoryScreen(navController = navController, innerPadding = innerPadding)
            }
            composable<Route.Profile> {
                ProfileScreen(navController = navController, innerPadding = innerPadding)
            }
            composable<Route.TreatmentList> {
                TreatmentListScreen(navController = navController)
            }
            composable<Route.TreatmentForm> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.TreatmentForm>()
                TreatmentFormScreen(treatmentId = route.treatmentId, navController = navController)
            }
            composable<Route.MedicationList> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.MedicationList>()
                MedicationListScreen(treatmentId = route.treatmentId, navController = navController)
            }
            composable<Route.MedicationForm> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.MedicationForm>()
                MedicationFormScreen(
                    treatmentId = route.treatmentId,
                    medicationId = route.medicationId,
                    navController = navController
                )
            }
            composable<Route.MedicationDetail> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.MedicationDetail>()
                MedicationDetailScreen(medicationId = route.medicationId, navController = navController)
            }
            composable<Route.PinLock> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.PinLock>()
                PinLockScreen(mode = route.mode, destination = route.destination, navController = navController)
            }
            composable<Route.CaregiverHub> {
                CaregiverHubScreen(navController = navController)
            }
            composable<Route.Settings> {
                SettingsScreen(navController = navController)
            }
            composable<Route.IntakeTaken> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.IntakeTaken>()
                ConfirmationScreen(
                    label = route.label,
                    confirmedAtTime = route.confirmedAtTime,
                    nextMedicationName = route.nextMedicationName,
                    nextSlotTime = route.nextSlotTime,
                    innerPadding = innerPadding,
                    navController = navController
                )
            }
        }
    }
}

@Composable
private fun InAppReminderDialog(
    event: InAppReminderEvent,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
    onMarkTaken: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = { Text(slotAlertTitle(event.timeSlot)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                event.intakes.forEach { intake ->
                    Text(
                        text = "• ${intake.medicationName} ${intake.dose}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onMarkTaken,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (event.intakes.size == 1) "Ya lo tomé" else "Ya los tomé")
            }
        },
        dismissButton = {
            TextButton(onClick = onSnooze) { Text("En 10 min") }
        }
    )
}

private fun slotAlertTitle(slot: TimeSlot) = when (slot) {
    TimeSlot.MORNING -> "Medicamentos de Mañana"
    TimeSlot.NOON -> "Medicamentos de Comida"
    TimeSlot.NIGHT -> "Medicamentos de Noche"
    TimeSlot.AS_NEEDED -> "Medicamentos"
}

@Composable
private fun AppBottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = navBackStackEntry?.destination

    NavigationBar {
        NavigationBarItem(
            selected = currentDest?.hasRoute(Route.Today::class) == true,
            onClick = {
                navController.navigate(Route.Today) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("Hoy") }
        )
        NavigationBarItem(
            selected = currentDest?.hasRoute(Route.Calendar::class) == true,
            onClick = {
                navController.navigate(Route.Calendar) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
            label = { Text("Calendario") }
        )
        NavigationBarItem(
            selected = currentDest?.hasRoute(Route.Profile::class) == true,
            onClick = {
                navController.navigate(Route.Profile) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            label = { Text("Perfil") }
        )
    }
}
