package com.adrian.minimallauncher.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.adrian.minimallauncher.data.PageKind
import com.adrian.minimallauncher.data.PersonalRecordActions
import com.adrian.minimallauncher.data.PersonalRecords
import com.adrian.minimallauncher.data.routine.PhaseMode
import com.adrian.minimallauncher.feature.ai.AI_CAMERA
import com.adrian.minimallauncher.feature.ai.AI_GALLERY
import com.adrian.minimallauncher.feature.ai.AI_STRIP
import com.adrian.minimallauncher.feature.ai.AiApp
import com.adrian.minimallauncher.feature.ai.aiStates
import com.adrian.minimallauncher.feature.exercise.ExerciseScreen
import com.adrian.minimallauncher.feature.exercise.ExerciseViewModel
import com.adrian.minimallauncher.feature.finance.FinanceScreen
import com.adrian.minimallauncher.feature.notes.NotesScreen
import com.adrian.minimallauncher.feature.vault.VaultScreen
import com.adrian.minimallauncher.feature.vault.unlockVault
import com.adrian.minimallauncher.sync.NookBridgeScreen
import com.adrian.minimallauncher.ui.components.LocalReactions
import com.adrian.minimallauncher.ui.components.RoutineWidgetData
import com.adrian.minimallauncher.ui.components.rememberReactions
import com.adrian.minimallauncher.ui.theme.LauncherTheme
import com.adrian.minimallauncher.viewmodel.LauncherViewModel

/** A widget just placed that asks for its setup, and the stack it went into. */
private data class WidgetSetup(val widgetId: String, val pageId: String, val stackId: String)

@Composable
fun LauncherScreen(viewModel: LauncherViewModel, exerciseViewModel: ExerciseViewModel) {
    val apps by viewModel.apps.collectAsState()
    val appCategories by viewModel.appCategories.collectAsState()
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
    val bridgeStatus by viewModel.bridgeStatus.collectAsState()
    val bridgeMessages by viewModel.bridgeMessages.collectAsState()
    // Which module page is open; never saved, so Bancos is always locked after the app restarts.
    var openModule by remember { mutableStateOf<String?>(null) }
    // Set by the + on the Finanzas Hoy widget: Finanzas opens with the register up.
    var financeEntry by remember { mutableStateOf(false) }
    // The tab Finanzas opens on when a route names one ("finance:banco").
    var financeTab by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    val drawer = rememberAppsDrawerState()
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
    // Where a widget sends Ejercicio: "start" (today's session) or "day:<date>" (that day's routine).
    var exerciseRoute by remember { mutableStateOf<String?>(null) }
    val openModuleById: (String) -> Unit = { id ->
        when {
            id == com.adrian.minimallauncher.feature.finance.FinanceWidgets.ADD_ROUTE -> { financeEntry = true; openModule = "finance" }
            id.startsWith("exercise:") -> { exerciseRoute = id.removePrefix("exercise:"); openModule = "exercise" }
            id.startsWith("finance:") -> { financeTab = id.removePrefix("finance:"); openModule = "finance" }
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
    val openRequest by viewModel.openRequest.collectAsState()
    LaunchedEffect(openRequest) {
        openRequest?.let { route -> openModuleById(route); viewModel.openRequest.value = null }
    }
    val homeFinance by viewModel.financeSummary.collectAsState()
    val homeMoney by viewModel.money.collectAsState()
    val homeReceivables by viewModel.bankReceivables.collectAsState()
    val homeAccounts by viewModel.bankAccounts.collectAsState()
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
        bridge = bridgeStatus,
        finance = homeFinance,
        money = homeMoney,
        receivables = homeReceivables,
        accounts = homeAccounts,
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
                    drawer.snapClosed()
                    showSettings = false
                    setupWidget = null
                    openModule = null
                    restrictedApp = null
                }
            }

            Box(Modifier.fillMaxSize().onSizeChanged { drawer.heightPx = (it.height * DRAWER_TRAVEL).coerceAtLeast(1f) }.drawerBackdrop(drawer)) {
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
                drawer = drawer,
                overlayOpen = drawer.visible || setupWidget != null || showSettings || openModule != null || restrictedApp != null,
            ) { page, pageScope ->
                when (page.kind) {
                    PageKind.WEEK -> WeekSummaryScreen(
                        now = now,
                        records = personalRecords,
                        exercise = exerciseDay,
                        settings = moduleSettings,
                        onOpenModule = openModuleById,
                        modifier = screenModifier,
                    )
                    PageKind.APPS, PageKind.WIDGETS -> HomeGrid(
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
                        essentialApps = if (page.kind == PageKind.APPS) {
                            apps.filter { it.categoryId == "apps" && !it.isVault }.sortedBy { it.label.lowercase() }
                        } else {
                            emptyList()
                        },
                        isAllowed = space::allows,
                        onOpenApp = viewModel::openApp,
                        onRestrictedApp = { restrictedApp = it },
                    )
                }
            }
            }

            // Swallows touches so nothing reaches the home pages under an open screen.
            val overlay = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { } }

            // Swipe-up App Drawer: follows the finger, over the home blurred and dimmed as the exercise sheets.
            if (drawer.visible) {
                Box(
                    overlay
                        .graphicsLayer { alpha = drawer.value }
                        .background(Color.Black.copy(alpha = DRAWER_DIM)),
                ) {
                    AppsPage(
                        apps = apps,
                        categories = appCategories,
                        isAllowed = space::allows,
                        onOpenApp = viewModel::openApp,
                        onRestrictedApp = { restrictedApp = it },
                        onSetAppCategory = viewModel::setAppCategory,
                        onSetCategoryApps = viewModel::setCategoryApps,
                        onSetAppDistraction = viewModel::setAppDistraction,
                        onAddCategory = viewModel::addAppCategory,
                        onRenameCategory = viewModel::renameAppCategory,
                        onSetCategoryDistraction = viewModel::setAppCategoryDistraction,
                        onSetCategoryVisible = viewModel::setSectionVisible,
                        onMoveCategory = viewModel::moveSection,
                        onDeleteCategory = viewModel::deleteAppCategory,
                        space = space,
                        spaces = spaces,
                        now = now,
                        onSettings = { openModule = "modules" },
                        onClose = drawer::close,
                        drawer = drawer,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { translationY = (1f - drawer.value) * size.height * DRAWER_TRAVEL }
                            .safeDrawingPadding(),
                    )
                }
            }

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
                    appCategories = appCategories,
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
                    onOpenBridge = { openModule = "bridge" },
                )
            } else if (openModule == "bridge") Box(overlay) {
                NookBridgeScreen(
                    status = bridgeStatus,
                    messages = bridgeMessages,
                    onRefreshIps = viewModel::refreshBridgeIps,
                    onRunSelfTest = viewModel::runBridgeSelfTest,
                    onSendMessageToPc = viewModel::sendBridgeMessageToPc,
                    onSetPcTargetUrl = viewModel::setBridgePcUrl,
                    onBack = closeModule,
                    modifier = screenModifier,
                )
            } else if (openModule == "routine") Box(overlay) {
                RoutineHost(viewModel, closeModule)
            } else if (openModule == "exercise") Box(overlay) {
                val exerciseSettings by exerciseViewModel.settings.collectAsState()
                val exerciseMetrics by exerciseViewModel.bodyMetrics.collectAsState()
                val exerciseStack by viewModel.exerciseStack.collectAsState()
                ExerciseScreen(
                    day = exerciseDay,
                    settings = exerciseSettings,
                    bodyMetrics = exerciseMetrics,
                    stack = exerciseStack,
                    onAddBodyMetric = exerciseViewModel::addBodyMetric,
                    onSaveSettings = exerciseViewModel::saveSettings,
                    onSaveStackExercise = viewModel::saveStackExercise,
                    onDeleteStackExercise = viewModel::deleteStackExercise,
                    onClearPlaceholders = viewModel::clearPlaceholderStack,
                    onLogSet = exerciseViewModel::logSet,
                    onUndoSet = exerciseViewModel::undoSet,
                    onFinish = exerciseViewModel::setCompleted,
                    onSaveRoutine = exerciseViewModel::saveRoutine,
                    onCopyPreviousWeek = exerciseViewModel::copyPreviousWeek,
                    route = exerciseRoute,
                    onRouteShown = { exerciseRoute = null },
                    onBack = closeModule,
                    modifier = screenModifier,
                )
            } else if (openModule == "alarms") Box(overlay) {
                AlarmsPage(viewModel, { openModule = "settings:alarms" }, closeModule, screenModifier)
            } else if (openModule == "world") Box(overlay) {
                WorldClockPage(now, moduleSettings.worldZones, closeModule, screenModifier)
            } else if (openModule == "diet") Box(overlay) {
                DietPage(now, personalRecords, personalActions, moduleSettings, viewModel, viewModel::setHabit, closeModule, screenModifier)
            } else if (openModule == "finance") Box(overlay) {
                androidx.compose.runtime.DisposableEffect(Unit) { onDispose { financeTab = null } }
                val financeSummary by viewModel.financeSummary.collectAsState()
                val money by viewModel.money.collectAsState()
                val bankAccounts by viewModel.bankAccounts.collectAsState()
                val bankReceivables by viewModel.bankReceivables.collectAsState()
                FinanceScreen(
                    financeSummary, money, moduleSettings.dailyBudgetCents,
                    financePlan = moduleSettings.financePlan,
                    onSavePlan = { plan -> viewModel.updateModuleSettings { it.copy(financePlan = plan) } },
                    savingGoal = moduleSettings.savingGoal,
                    onSaveGoal = { goal -> viewModel.updateModuleSettings { it.copy(savingGoal = goal) } },
                    onAdd = viewModel::addMoney, onSettle = viewModel::settleShared, onSetBudget = viewModel::setBudget,
                    onUpdate = viewModel::updateMoney,
                    accounts = bankAccounts, receivables = bankReceivables,
                    onSaveAccount = viewModel::saveBankAccount, onDeleteAccount = viewModel::deleteBankAccount,
                    onSaveReceivable = viewModel::saveBankReceivable, onDeleteReceivable = viewModel::deleteBankReceivable,
                    onCollectReceivable = viewModel::collectBankReceivable,
                    onSavingPct = viewModel::setSavingPct,
                    onDailyLimit = { cents -> viewModel.updateModuleSettings { it.copy(dailyBudgetCents = cents) } },
                    onDelete = viewModel::deleteFinance, onBack = closeModule, modifier = screenModifier,
                    openEntry = financeEntry, onEntryShown = { financeEntry = false },
                    startTab = financeTab,
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
