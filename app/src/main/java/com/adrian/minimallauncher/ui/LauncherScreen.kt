package com.adrian.minimallauncher.ui
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.adrian.minimallauncher.ui.theme.LauncherTheme
import com.adrian.minimallauncher.data.PersonalRecords
import com.adrian.minimallauncher.data.PersonalRecordActions
import com.adrian.minimallauncher.viewmodel.LauncherViewModel
@Composable
fun LauncherScreen(viewModel: LauncherViewModel) {
    val apps by viewModel.apps.collectAsState()
    val now by viewModel.currentTime.collectAsState()
    val space by viewModel.currentSpace.collectAsState()
    val widgetPlacement by viewModel.widgetPlacement.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val reminders by viewModel.reminders.collectAsState()
    val finances by viewModel.finances.collectAsState()
    val meals by viewModel.meals.collectAsState()
    val spaces by viewModel.spaces.collectAsState()
    val hiddenSections by viewModel.hiddenSections.collectAsState()
    val pauseSeconds by viewModel.pauseSeconds.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    var restrictedApp by remember { mutableStateOf<com.adrian.minimallauncher.data.LauncherApp?>(null) }
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LauncherTheme {
        val screenModifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
        val blockedApp = restrictedApp
        if (showSettings) {
            SettingsScreen(
                spaces, hiddenSections, pauseSeconds, viewModel::updateSpaces, viewModel::setSectionVisible,
                viewModel::setPauseSeconds, { showSettings = false }, screenModifier,
            )
        } else if (blockedApp == null) {
            MainZone(
                apps = apps,
                isAllowed = space::allows,
                onOpenApp = viewModel::openApp,
                onRestrictedApp = { restrictedApp = it },
                widgetPlacement = widgetPlacement,
                onWidgetPlacementChange = viewModel::setWidgetPlacement,
                spaceName = space.name,
                now = now,
                hiddenSections = hiddenSections,
                onSettings = { showSettings = true },
                personalRecords = PersonalRecords(notes, reminders, finances, meals),
                personalActions = PersonalRecordActions(
                    viewModel::addNote, viewModel::deleteNote,
                    { title, hour, minute, spaceName ->
                        viewModel.addReminder(title, hour, minute, spaceName)
                        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }, viewModel::deleteReminder,
                    viewModel::addFinance, viewModel::deleteFinance,
                    viewModel::addMeal, viewModel::deleteMeal,
                ),
                modifier = screenModifier,
            )
        } else {
            FrictionScreen(
                blockedApp, space.name, pauseSeconds, { restrictedApp = null },
                { packageName -> viewModel.openApp(packageName); restrictedApp = null }, screenModifier,
            )
        }
    }
}
