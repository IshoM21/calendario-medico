package com.codigomoo.calendariomedico.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDeepLink
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.codigomoo.calendariomedico.presentation.calendar.CalendarScreen
import com.codigomoo.calendariomedico.presentation.caregiver.CaregiverHubScreen
import com.codigomoo.calendariomedico.presentation.caregiver.PinLockScreen
import com.codigomoo.calendariomedico.presentation.history.HistoryScreen
import com.codigomoo.calendariomedico.presentation.medication.MedicationDetailScreen
import com.codigomoo.calendariomedico.presentation.medication.MedicationFormScreen
import com.codigomoo.calendariomedico.presentation.medication.MedicationListScreen
import com.codigomoo.calendariomedico.presentation.profile.ProfileScreen
import com.codigomoo.calendariomedico.presentation.settings.SettingsScreen
import com.codigomoo.calendariomedico.presentation.today.TodayScreen
import com.codigomoo.calendariomedico.presentation.treatment.TreatmentFormScreen
import com.codigomoo.calendariomedico.presentation.treatment.TreatmentListScreen
import kotlinx.serialization.Serializable

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
    @Serializable data class PinLock(val mode: String) : Route
    @Serializable data object CaregiverHub : Route
    @Serializable data object Settings : Route
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
                PinLockScreen(mode = route.mode, navController = navController)
            }
            composable<Route.CaregiverHub> {
                CaregiverHubScreen(navController = navController)
            }
            composable<Route.Settings> {
                SettingsScreen(navController = navController)
            }
        }
    }
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
