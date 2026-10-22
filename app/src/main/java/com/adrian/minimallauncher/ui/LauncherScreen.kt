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
import androidx.compose.runtime.CompositionLocalProvider
import com.adrian.minimallauncher.ui.components.LocalReactions
import com.adrian.minimallauncher.ui.components.rememberReactions
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.adrian.minimallauncher.data.PageKind
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.adrian.minimallauncher.feature.exercise.ExerciseScreen
import com.adrian.minimallauncher.feature.finance.FinanceScreen
import com.adrian.minimallauncher.feature.notes.NotesScreen
import com.adrian.minimallauncher.feature.exercise.ExerciseViewModel
import com.adrian.minimallauncher.feature.vault.VaultScreen
import com.adrian.minimallauncher.feature.vault.unlockVault
import com.adrian.minimallauncher.ui.theme.LauncherTheme
import com.adrian.minimallauncher.data.PersonalRecords
import com.adrian.minimallauncher.data.PersonalRecordActions
import com.adrian.minimallauncher.viewmodel.LauncherViewModel
import kotlinx.coroutines.launch

@Composable
fun LauncherScreen(viewModel: LauncherViewModel, exerciseViewModel: ExerciseViewModel) {
    val apps by viewModel.apps.collectAsState()
    val now by viewModel.currentTime.collectAsState()
    val space by viewModel.currentSpace.collectAsState()
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
    val homePages by viewModel.homePages.collectAsState()
    val widgetLayouts by viewModel.widgetLayouts.collectAsState()
    val moduleSettings by viewModel.moduleSettings.collectAsState()
    val exerciseDay by exerciseViewModel.day.collectAsState()
    // Which module page is open; never saved, so Bancos is always locked after the app restarts.
    var openModule by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    // A widget just placed that asks for its setup (Mundo, Notas).
    var setupWidget by remember { mutableStateOf<String?>(null) }
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
        setNotePinned = viewModel::setNotePinned,
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
    val homeData = HomeData(personalRecords, exerciseDay, moduleSettings, vaultApps.size)
    val closeModule = { openModule = null }

    LauncherTheme {
    // Vibration and other feedback for actions, on unless turned off in Módulos.
    CompositionLocalProvider(LocalReactions provides rememberReactions(moduleSettings.haptics)) {
        val screenModifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
        val blockedApp = restrictedApp

        LaunchedEffect(Unit) {
            viewModel.homePressed.collect {
                showSettings = false
                setupWidget = null
                openModule = null
                restrictedApp = null
            }
        }

        HomePagesHost(
            pages = homePages,
            layouts = widgetLayouts,
            homePressed = viewModel.homePressed,
            data = homeData,
            now = now,
            onAddPage = viewModel::addPage,
            onDeletePage = viewModel::deletePage,
            onSetMain = viewModel::setMainPage,
            onRestoreWeek = viewModel::restoreWeekPage,
            onAddWidget = { pageId, widgetId, stackId, size, at ->
                if (stackId != null) viewModel.addWidgetToStack(pageId, stackId, widgetId) else viewModel.addWidgetAsNewStack(pageId, widgetId, size.w, size.h, at)
                if (widgetId in WIDGETS_WITH_SETUP) setupWidget = widgetId
            },
            onSettings = { openModule = "modules" },
        ) { page, pageScope ->
            when (page.kind) {
                PageKind.APPS -> AppsPage(
                    apps = apps,
                    isAllowed = space::allows,
                    onOpenApp = viewModel::openApp,
                    onRestrictedApp = { restrictedApp = it },
                    hiddenSections = hiddenSections,
                    sectionOrder = sectionOrder,
                    space = space,
                    spaces = spaces,
                    now = now,
                    onSettings = { openModule = "modules" },
                    modifier = screenModifier,
                    widgets = {
                        WidgetGridView(
                            stacks = widgetLayouts[page.id].orEmpty(),
                            editMode = pageScope.editMode,
                            data = homeData,
                            now = now,
                            onOpenModule = openModuleById,
                            onEnterEdit = pageScope.enterEdit,
                            onMove = { stackId, col, row -> viewModel.moveStackTo(page.id, stackId, col, row) },
                            onResize = { stackId, w, h -> viewModel.resizeStack(page.id, stackId, w, h) },
                            onMerge = { draggedId, targetId -> viewModel.mergeStacks(page.id, draggedId, targetId) },
                            onRemoveWidget = { stackId, widgetId -> viewModel.removeWidgetFromStack(page.id, stackId, widgetId) },
                            onAddToStack = { stackId -> pageScope.openCatalog(stackId) },
                        )
                    },
                    hasClock = widgetLayouts[page.id].orEmpty().any { "clock" in it.widgetIds },
                    editMode = pageScope.editMode,
                    onExitEdit = pageScope.exitEdit,
                )
                PageKind.WEEK -> WeekSummaryScreen(
                    now = now,
                    records = personalRecords,
                    exercise = exerciseDay,
                    settings = moduleSettings,
                    onOpenModule = openModuleById,
                    onNavigateHome = pageScope.goToMain,
                    modifier = screenModifier,
                )
                PageKind.WIDGETS -> HomeGrid(
                    spaceName = space.name,
                    now = now,
                    data = homeData,
                    widgetLayout = widgetLayouts[page.id].orEmpty(),
                    editMode = pageScope.editMode,
                    onEnterEdit = pageScope.enterEdit,
                    onOpenModule = openModuleById,
                    onSettings = { openModule = "modules" },
                    onMove = { stackId, col, row -> viewModel.moveStackTo(page.id, stackId, col, row) },
                    onResize = { stackId, w, h -> viewModel.resizeStack(page.id, stackId, w, h) },
                    onMerge = { draggedId, targetId -> viewModel.mergeStacks(page.id, draggedId, targetId) },
                    onRemoveWidget = { stackId, widgetId -> viewModel.removeWidgetFromStack(page.id, stackId, widgetId) },
                    onAddToStack = { stackId -> pageScope.openCatalog(stackId) },
                    modifier = screenModifier,
                )
            }
        }

        // Swallows touches so nothing reaches the home pages under an open screen.
        val overlay = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { } }
        val setup = setupWidget
        if (setup != null) Box(overlay) {
            WidgetSetupScreen(
                setup, moduleSettings, notes, viewModel::updateModuleSettings,
                { id -> viewModel.setNotePinned(id, true) }, { setupWidget = null }, screenModifier,
            )
        } else if (showSettings) Box(overlay) {
            SettingsScreen(
                spaces, hiddenSections, sectionOrder, pauseSeconds, viewModel::updateSpaces, viewModel::setSectionVisible, viewModel::moveSection,
                viewModel::setPauseSeconds, { showSettings = false }, screenModifier,
            )
        } else if (openModule == "modules" || openModule?.startsWith("settings:") == true) Box(overlay) {
            val lastBackupAt by viewModel.lastBackupAt.collectAsState()
            val backupMessage by viewModel.backupMessage.collectAsState()
            // "settings:<module>" comes from a module's gear: it opens straight to that module's settings and back returns to it.
            val fromModule = openModule?.removePrefix("settings:")?.takeIf { it != openModule }
            ModulesScreen(
                moduleSettings, viewModel::updateModuleSettings,
                BackupUi(lastBackupAt, backupMessage, viewModel::exportBackup, viewModel::importBackup),
                { showSettings = true }, if (fromModule != null) ({ openModule = fromModule }) else closeModule, screenModifier,
                initialEditing = fromModule,
            )
        } else if (openModule == "exercise") Box(overlay) {
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
                onBack = closeModule,
                modifier = screenModifier,
            )
        } else if (openModule == "alarms") Box(overlay) {
            AlarmsPage(now, personalRecords, personalActions, space.name, closeModule, screenModifier)
        } else if (openModule == "world") Box(overlay) {
            WorldClockPage(now, moduleSettings.worldZones, closeModule, screenModifier)
        } else if (openModule == "diet") Box(overlay) {
            DietPage(now, personalRecords, personalActions, moduleSettings, viewModel::setHabit, { openModule = "settings:diet" }, closeModule, screenModifier)
        } else if (openModule == "finance") Box(overlay) {
            FinanceScreen(finances, now, viewModel::addFinance, viewModel::deleteFinance, { openModule = "settings:finance" }, closeModule, screenModifier)
        } else if (openModule == "notes") Box(overlay) {
            NotesScreen(notes, viewModel::addNote, viewModel::deleteNote, viewModel::setNotePinned, closeModule, screenModifier)
        } else if (openModule == "vault") Box(overlay) {
            VaultScreen(
                apps = vaultApps,
                lockSeconds = moduleSettings.vaultLockSeconds,
                onOpenApp = viewModel::openApp,
                onLockSecondsChange = viewModel::setVaultLockSeconds,
                onLock = closeModule,
                modifier = screenModifier,
            )
        } else if (blockedApp != null) Box(overlay) {
            FrictionScreen(
                blockedApp, space.name, pauseSeconds, { restrictedApp = null },
                { packageName -> viewModel.openApp(packageName); restrictedApp = null }, screenModifier,
            )
        }
    }
    }
}
