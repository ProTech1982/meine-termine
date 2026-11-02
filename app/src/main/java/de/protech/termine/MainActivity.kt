package de.protech.termine

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.core.content.getSystemService
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import de.protech.termine.ui.CategoryScreen
import de.protech.termine.ui.EditScreen
import de.protech.termine.ui.ListScreen
import de.protech.termine.ui.TermineViewModel
import de.protech.termine.ui.theme.MeineTermineTheme

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val setupOk: MutableState<Boolean> = mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setupOk.value = isFullySetUp()

        setContent {
            MeineTermineTheme {
                val navController = rememberNavController()
                val viewModel: TermineViewModel = viewModel()
                var hintDismissed by remember { mutableStateOf(false) }
                val showHint = !setupOk.value && !hintDismissed

                NavHost(navController = navController, startDestination = "liste") {
                    composable("liste") {
                        ListScreen(
                            viewModel = viewModel,
                            showSetupHint = showHint,
                            onDismissSetupHint = { hintDismissed = true },
                            onFixSetup = { openSetup() },
                            onAdd = { navController.navigate("bearbeiten/0") },
                            onOpen = { id -> navController.navigate("bearbeiten/$id") },
                            onCategories = { navController.navigate("kategorien") }
                        )
                    }
                    composable(
                        route = "bearbeiten/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType })
                    ) { entry ->
                        EditScreen(
                            viewModel = viewModel,
                            appointmentId = entry.arguments?.getLong("id") ?: 0L,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("kategorien") {
                        CategoryScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupOk.value = isFullySetUp()
    }

    /** Exakte Alarme erlaubt und Akku-Optimierung aus? */
    private fun isFullySetUp(): Boolean {
        val alarmOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService<AlarmManager>()?.canScheduleExactAlarms() ?: true
        } else true

        val batteryOk = getSystemService<PowerManager>()
            ?.isIgnoringBatteryOptimizations(packageName) ?: true

        return alarmOk && batteryOk
    }

    private fun openSetup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService<AlarmManager>()
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        .setData(Uri.parse("package:$packageName"))
                )
                return
            }
        }
        val power = getSystemService<PowerManager>()
        if (power != null && !power.isIgnoringBatteryOptimizations(packageName)) {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(Uri.parse("package:$packageName"))
            )
        }
    }
}
