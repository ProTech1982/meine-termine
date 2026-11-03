package de.protech.termine

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import de.protech.termine.look.AppBackground
import de.protech.termine.look.LocalAppearance
import de.protech.termine.ui.AppearanceScreen
import de.protech.termine.ui.CategoryScreen
import de.protech.termine.ui.EditScreen
import de.protech.termine.ui.HomeScreen
import de.protech.termine.ui.LegalScreen
import de.protech.termine.ui.SettingsScreen
import de.protech.termine.ui.SetupStatus
import de.protech.termine.ui.TermineViewModel
import de.protech.termine.ui.theme.MeineTermineTheme

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            status.value = readStatus()
        }

    private val status: MutableState<SetupStatus> = mutableStateOf(SetupStatus())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        status.value = readStatus()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val store = (application as TermineApp).appearance

        setContent {
            val look = store.value

            MeineTermineTheme(look = look) {
                CompositionLocalProvider(LocalAppearance provides look) {
                    AppBackground {
                        val navController = rememberNavController()
                        val viewModel: TermineViewModel = viewModel()
                        var hintDismissed by remember { mutableStateOf(false) }
                        val setup by status

                        NavHost(navController = navController, startDestination = "start") {
                    composable("start") {
                        HomeScreen(
                            viewModel = viewModel,
                            showSetupHint = !setup.allGood && !hintDismissed,
                            onDismissSetupHint = { hintDismissed = true },
                            onFixSetup = { navController.navigate("einstellungen") },
                            onAdd = { navController.navigate("bearbeiten/0") },
                            onOpen = { id -> navController.navigate("bearbeiten/$id") },
                            onSettings = { navController.navigate("einstellungen") }
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
                    composable("einstellungen") {
                        SettingsScreen(
                            status = setup,
                            onBack = { navController.popBackStack() },
                            onCategories = { navController.navigate("kategorien") },
                            onAppearance = { navController.navigate("darstellung") },
                            onLegal = { navController.navigate("rechtliches") },
                            onFixNotifications = { openNotificationSettings() },
                            onFixExactAlarms = { openExactAlarmSettings() },
                            onFixBattery = { openBatterySettings() }
                        )
                    }
                    composable("rechtliches") {
                        LegalScreen(onBack = { navController.popBackStack() })
                    }
                    composable("kategorien") {
                        CategoryScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("darstellung") {
                        AppearanceScreen(
                            onBack = { navController.popBackStack() },
                            onThemeMode = store::setThemeMode,
                            onAccent = store::setAccent,
                            onSystemColors = store::setUseSystemColors,
                            onShape = store::setShape,
                            onOpacity = store::setOpacity,
                            onPattern = store::setPattern,
                            onPatternStrength = store::setPatternStrength,
                            onReset = store::reset
                        )
                    }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Nach einem Ausflug in die Systemeinstellungen neu nachsehen.
        status.value = readStatus()
    }

    private fun readStatus(): SetupStatus {
        val notificationsAllowed = NotificationManagerCompat.from(this).areNotificationsEnabled()

        val exactAlarmsAllowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService<AlarmManager>()?.canScheduleExactAlarms() ?: true
        } else true

        val batteryUnrestricted = getSystemService<PowerManager>()
            ?.isIgnoringBatteryOptimizations(packageName) ?: true

        return SetupStatus(
            notificationsAllowed = notificationsAllowed,
            exactAlarmsAllowed = exactAlarmsAllowed,
            batteryUnrestricted = batteryUnrestricted
        )
    }

    private fun openNotificationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        runCatching { startActivity(intent) }.onFailure { openAppDetails() }
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                .setData(Uri.parse("package:$packageName"))
            runCatching { startActivity(intent) }.onFailure { openAppDetails() }
        }
    }

    private fun openBatterySettings() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData(Uri.parse("package:$packageName"))
        runCatching { startActivity(intent) }.onFailure { openAppDetails() }
    }

    private fun openAppDetails() {
        runCatching {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:$packageName"))
            )
        }
    }
}
