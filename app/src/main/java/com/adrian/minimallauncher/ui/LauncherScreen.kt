package com.adrian.minimallauncher.ui

import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.adrian.minimallauncher.feature.exercise.ExerciseScreen
import com.adrian.minimallauncher.feature.exercise.ExerciseViewModel
import com.adrian.minimallauncher.feature.vault.VaultScreen
import com.adrian.minimallauncher.feature.vault.unlockVault
import com.adrian.minimallauncher.ui.theme.LauncherTheme
import com.adrian.minimallauncher.ui.widgets.HomeWidgets
import com.adrian.minimallauncher.data.PersonalRecords
import com.adrian.minimallauncher.data.PersonalRecordActions
import com.adrian.minimallauncher.viewmodel.LauncherViewModel
import kotlinx.coroutines.launch

@Composable
fun LauncherScreen(viewModel: LauncherViewModel, exerciseViewModel: ExerciseViewModel) {
    val apps by viewModel.apps.collectAsState()
    val now by viewModel.currentTime.collectAsState()
    val space by viewModel.currentSpace.collectAsState()
    val widgetPlacement by viewModel.widgetPlacement.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val reminders by viewModel.reminders.collectAsState()
    val finances by viewModel.finances.collectAsState()
    val meals by viewModel.meals.collectAsState()
    val bodyMetrics by viewModel.bodyMetrics.collectAsState()
    val completedWorkouts by viewModel.completedWorkouts.collectAsState()
    val completedHabits by viewModel.completedHabits.collectAsState()
    val spaces by viewModel.spaces.collectAsState()
    val hiddenSections by viewModel.hiddenSections.collectAsState()
    val pauseSeconds by viewModel.pauseSeconds.collectAsState()
    val sectionOrder by viewModel.sectionOrder.collectAsState()
    val moduleSettings by viewModel.moduleSettings.collectAsState()
    val exerciseDay by exerciseViewModel.day.collectAsState()
    // Which module page is open; never saved, so Bancos is always locked after the app restarts.
    var openModule by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var restrictedApp by remember { mutableStateOf<com.adrian.minimallauncher.data.LauncherApp?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    val personalRecords = PersonalRecords(
        notes = notes,
        reminders = reminders,
        finances = finances,
        meals = meals,
        bodyMetrics = bodyMetrics,
        completedWorkouts = completedWorkouts,
        completedHabits = completedHabits,
    )

    val personalActions = PersonalRecordActions(
        addNote = viewModel::addNote,
        deleteNote = viewModel::deleteNote,
        addReminder = { title, hour, minute, spaceName ->
            viewModel.addReminder(title, hour, minute, spaceName)
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        deleteReminder = viewModel::deleteReminder,
        addFinance = viewModel::addFinance,
        deleteFinance = viewModel::deleteFinance,
        addMeal = viewModel::addMeal,
        deleteMeal = viewModel::deleteMeal,
        addBodyMetric = viewModel::addBodyMetric,
        deleteBodyMetric = viewModel::deleteBodyMetric,
        toggleWorkout = viewModel::toggleWorkout,
        toggleHabit = viewModel::toggleHabit,
    )

    val vaultApps = apps.filter { it.isVault }
    val openModuleById: (String) -> Unit = { id ->
        if (id == "vault") (context as? FragmentActivity)?.let { activity -> unlockVault(activity) { openModule = "vault" } }
        else openModule = id
    }
    val homeWidgets = HomeWidgets(exerciseDay, moduleSettings.exerciseStart, vaultApps.size, openModuleById)

    LauncherTheme {
        val screenModifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
        val blockedApp = restrictedApp

        if (showSettings) {
            SettingsScreen(
                spaces, hiddenSections, sectionOrder, pauseSeconds, viewModel::updateSpaces, viewModel::setSectionVisible, viewModel::moveSection,
                viewModel::setPauseSeconds, { showSettings = false }, screenModifier,
            )
        } else if (openModule == "exercise") {
            val exerciseSettings by exerciseViewModel.settings.collectAsState()
            val exerciseMetrics by exerciseViewModel.bodyMetrics.collectAsState()
            ExerciseScreen(
                day = exerciseDay,
                settings = exerciseSettings,
                bodyMetrics = exerciseMetrics,
                onLogSet = exerciseViewModel::logSet,
                onUndoSet = exerciseViewModel::undoSet,
                onSetCompleted = exerciseViewModel::setCompleted,
                onAddBodyMetric = exerciseViewModel::addBodyMetric,
                onSaveSettings = exerciseViewModel::saveSettings,
                onBack = { openModule = null },
                modifier = screenModifier,
            )
        } else if (openModule == "vault") {
            VaultScreen(
                apps = vaultApps,
                lockSeconds = moduleSettings.vaultLockSeconds,
                onOpenApp = viewModel::openApp,
                onLockSecondsChange = viewModel::setVaultLockSeconds,
                onLock = { openModule = null },
                modifier = screenModifier,
            )
        } else if (blockedApp == null) {
            val pagerState = rememberPagerState(initialPage = 1, pageCount = { 2 })
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                if (page == 0) {
                    InsightsScreen(
                        records = personalRecords,
                        actions = personalActions,
                        now = now,
                        onNavigateHome = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                        exerciseDay = exerciseDay,
                        onOpenExercise = { openModule = "exercise" },
                        modifier = screenModifier,
                    )
                } else {
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
                        sectionOrder = sectionOrder,
                        homeWidgets = homeWidgets,
                        onSettings = { showSettings = true },
                        personalRecords = personalRecords,
                        personalActions = personalActions,
                        modifier = screenModifier,
                    )
                }
            }
        } else {
            FrictionScreen(
                blockedApp, space.name, pauseSeconds, { restrictedApp = null },
                { packageName -> viewModel.openApp(packageName); restrictedApp = null }, screenModifier,
            )
        }
    }
}
