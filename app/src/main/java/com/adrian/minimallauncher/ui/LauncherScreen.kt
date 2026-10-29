package com.adrian.minimallauncher.ui

import com.adrian.minimallauncher.data.routine.PhaseMode
import com.adrian.minimallauncher.ui.components.RoutineWidgetData
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
import androidx.compose.runtime.CompositionLocalProvider
import com.adrian.minimallauncher.ui.components.LocalReactions
import com.adrian.minimallauncher.ui.components.rememberReactions
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.adrian.minimallauncher.feature.ai.AI_STRIP
import com.adrian.minimallauncher.feature.ai.AI_CAMERA
import com.adrian.minimallauncher.feature.ai.AI_GALLERY
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.runtime.saveable.rememberSaveable
import com.adrian.minimallauncher.feature.ai.AiApp
import com.adrian.minimallauncher.feature.ai.aiStates

/** A widget just placed that asks for its setup, and the stack it went into. */
private data class WidgetSetup(val widgetId: String, val pageId: String, val stackId: String)

@Composable
fun LauncherScreen(viewModel: LauncherViewModel, exerciseViewModel: ExerciseViewModel) {
    val apps by viewModel.apps.collectAsState()
    val now by viewModel.currentTime.collectAsState()
    val space by viewModel.currentSpace.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val reminders by viewModel.reminders.collectAsState()
    val finances by viewModel.finances.collectAsState()
    val meals by viewModel.meals.collectAsState()
    val routinePhases by viewModel.phases.collectAsState()
    val routineCurrent by viewModel.currentPhase.collectAsState()
    val routinePicks by viewModel.widgetPicks.collectAsState()
    val routineReminders by viewModel.phaseReminders.collectAsState()
    val routineChecks by viewModel.phaseChecks.collectAsState()
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
    val aiShortcuts by viewModel.aiShortcuts.collectAsState()
    // Which module page is open; never saved, so Bancos is always locked after the app restarts.
    var openModule by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    // A widget just placed that asks for its setup (Mundo, Notas, which assistant).
    var setupWidget by remember { mutableStateOf<WidgetSetup?>(null) }
    var restrictedApp by remember { mutableStateOf<com.adrian.minimallauncher.data.LauncherApp?>(null) }
    val context = LocalContext.current
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
    // Claude's camera and gallery shortcuts: the launcher takes or picks the photo, then shares it to the app.
    var photoFor by rememberSaveable { mutableStateOf<String?>(null) }
    var photoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = photoUri
        val pkg = photoFor
        if (ok && uri != null && pkg != null) viewModel.shareImage(pkg, uri)
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val pkg = photoFor
        if (uri != null && pkg != null) viewModel.shareImage(pkg, uri)
    }
    // Assistant widgets open apps: through the pause when the schedule keeps them closed, or the store when missing.
    val openAppRoute: (String, () -> Unit) -> Unit = { pkg, open ->
        val app = apps.firstOrNull { it.packageName == pkg }
        when {
            app == null -> viewModel.openStore(pkg)
            space.allows(app) -> open()
            else -> restrictedApp = app
        }
    }
    val openModuleById: (String) -> Unit = { id ->
        when {
            id == "vault" -> (context as? FragmentActivity)?.let { activity -> unlockVault(activity) { openModule = "vault" } }
            id.startsWith("app:") -> id.removePrefix("app:").let { pkg -> openAppRoute(pkg) { viewModel.openApp(pkg) } }
            id.startsWith("install:") -> viewModel.openStore(id.removePrefix("install:"))
            id.startsWith("shortcut:") -> {
                val pkg = id.removePrefix("shortcut:").substringBefore('/')
                openAppRoute(pkg) {
                    when (val shortcut = id.substringAfter('/')) {
                        AI_CAMERA -> viewModel.newPhotoUri().let { uri ->
                            photoFor = pkg
                            photoUri = uri
                            runCatching { takePhoto.launch(uri) }
                        }
                        AI_GALLERY -> {
                            photoFor = pkg
                            runCatching { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                        }
                        else -> viewModel.openShortcut(pkg, shortcut)
                    }
                }
            }
            else -> openModule = id
        }
    }
    val homeData = HomeData(
        personalRecords, exerciseDay, moduleSettings, vaultApps.size, viewModel::setHabit,
        ai = aiStates(apps, space, spaces, now, aiShortcuts),
        routine = RoutineWidgetData(
            phases = routinePhases,
            currentIndex = routinePhases.indexOfFirst { it.id == routineCurrent.id },
            picks = routinePicks,
            reminders = routineReminders,
            checks = routineChecks,
            ordered = routineCurrent.mode == PhaseMode.ORDERED,
            onMarkAction = viewModel::markAction,
            onToggleReminder = viewModel::toggleReminderDone,
        ),
    )
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
                // Asistentes IA at 2×2 or 4×2 holds one app: it starts with one the stack doesn't have yet, then asks which.
                val stack = stackId?.let { id -> widgetLayouts[pageId].orEmpty().firstOrNull { it.id == id } }
                val oneApp = widgetId == AI_STRIP && (stack?.h ?: size.h) > 1
                val placed = if (oneApp) AiApp.firstFree(moduleSettings.aiApps, stack?.widgetIds.orEmpty()).widgetId else widgetId
                val placedIn = if (stackId != null) {
                    viewModel.addWidgetToStack(pageId, stackId, placed)
                    stackId
                } else {
                    viewModel.addWidgetAsNewStack(pageId, placed, size.w, size.h, at)
                }
                if (oneApp || placed in WIDGETS_WITH_SETUP) setupWidget = WidgetSetup(placed, pageId, placedIn)
            },
            onSettings = { openModule = "modules" },
            overlayOpen = setupWidget != null || showSettings || openModule != null || restrictedApp != null,
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
                            onMove = { stackId, col, row -> viewModel.moveStackTo(page.id, stackId, col, row) },
                            onResize = { stackId, w, h -> viewModel.resizeStack(page.id, stackId, w, h) },
                            onMerge = { draggedId, targetId -> viewModel.mergeStacks(page.id, draggedId, targetId) },
                            onRemoveWidget = { stackId, widgetId -> viewModel.removeWidgetFromStack(page.id, stackId, widgetId) },
                            onRemoveStack = { stackId -> viewModel.removeStack(page.id, stackId) },
                            onAddToStack = { stackId -> pageScope.openCatalog(stackId) },
                            onSetAlpha = { stackId, alpha -> viewModel.setWidgetAlpha(page.id, stackId, alpha) },
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
                    modifier = screenModifier,
                )
                PageKind.WIDGETS -> HomeGrid(
                    spaceName = space.name,
                    now = now,
                    data = homeData,
                    widgetLayout = widgetLayouts[page.id].orEmpty(),
                    editMode = pageScope.editMode,
                    onOpenModule = openModuleById,
                    onSettings = { openModule = "modules" },
                    onMove = { stackId, col, row -> viewModel.moveStackTo(page.id, stackId, col, row) },
                    onResize = { stackId, w, h -> viewModel.resizeStack(page.id, stackId, w, h) },
                    onMerge = { draggedId, targetId -> viewModel.mergeStacks(page.id, draggedId, targetId) },
                    onRemoveWidget = { stackId, widgetId -> viewModel.removeWidgetFromStack(page.id, stackId, widgetId) },
                    onRemoveStack = { stackId -> viewModel.removeStack(page.id, stackId) },
                    onAddToStack = { stackId -> pageScope.openCatalog(stackId) },
                    onSetAlpha = { stackId, alpha -> viewModel.setWidgetAlpha(page.id, stackId, alpha) },
                    modifier = screenModifier,
                )
            }
        }

        // Swallows touches so nothing reaches the home pages under an open screen.
        val overlay = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { } }
        val setup = setupWidget
        if (setup != null) Box(overlay) {
            WidgetSetupScreen(
                setup.widgetId, moduleSettings, notes, viewModel::updateModuleSettings,
                { id -> viewModel.setNotePinned(id, true) }, { setupWidget = null }, screenModifier,
                onPickAi = { app ->
                    viewModel.replaceWidget(setup.pageId, setup.stackId, setup.widgetId, app.widgetId)
                    setupWidget = setup.copy(widgetId = app.widgetId)
                },
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
        } else if (openModule == "routine") Box(overlay) {
            RoutineHost(viewModel, closeModule)
        } else if (openModule == "exercise") Box(overlay) {
            val exerciseSettings by exerciseViewModel.settings.collectAsState()
            val exerciseMetrics by exerciseViewModel.bodyMetrics.collectAsState()
            val exerciseStack by viewModel.exerciseStack.collectAsState()
            val exerciseFood by viewModel.foodDay.collectAsState()
            ExerciseScreen(
                day = exerciseDay,
                settings = exerciseSettings,
                bodyMetrics = exerciseMetrics,
                stack = exerciseStack,
                kcalOver = exerciseFood.over,
                onLogSet = exerciseViewModel::logSet,
                onUndoSet = exerciseViewModel::undoSet,
                onSetCompleted = exerciseViewModel::setCompleted,
                onAddBodyMetric = exerciseViewModel::addBodyMetric,
                onSaveSettings = exerciseViewModel::saveSettings,
                onSaveStackExercise = viewModel::saveStackExercise,
                onDeleteStackExercise = viewModel::deleteStackExercise,
                onClearPlaceholders = viewModel::clearPlaceholderStack,
                onBack = closeModule,
                modifier = screenModifier,
            )
        } else if (openModule == "alarms") Box(overlay) {
            AlarmsPage(viewModel, { openModule = "settings:alarms" }, closeModule, screenModifier)
        } else if (openModule == "world") Box(overlay) {
            WorldClockPage(now, moduleSettings.worldZones, closeModule, screenModifier)
        } else if (openModule == "diet") Box(overlay) {
            DietPage(now, personalRecords, personalActions, moduleSettings, viewModel, viewModel::setHabit,{ openModule = "settings:diet" }, closeModule, screenModifier)
        } else if (openModule == "finance") Box(overlay) {
            val financeSummary by viewModel.financeSummary.collectAsState()
            val money by viewModel.money.collectAsState()
            FinanceScreen(
                financeSummary, money, moduleSettings.dailyBudgetCents,
                onAdd = viewModel::addMoney, onSettle = viewModel::settleShared, onSetBudget = viewModel::setBudget,
                onSavingPct = viewModel::setSavingPct,
                onDailyLimit = { cents -> viewModel.updateModuleSettings { it.copy(dailyBudgetCents = cents) } },
                onDelete = viewModel::deleteFinance, onBack = closeModule, modifier = screenModifier,
            )
        } else if (openModule == "notes") Box(overlay) {
            NotesScreen(viewModel, { openModule = "settings:notes" }, closeModule, screenModifier)
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
