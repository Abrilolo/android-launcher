package com.adrian.minimallauncher.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.adrian.minimallauncher.data.APPS_DEFAULT_LAYOUT
import com.adrian.minimallauncher.data.APPS_PAGE
import com.adrian.minimallauncher.data.AppRepository
import com.adrian.minimallauncher.data.BackupRepository
import com.adrian.minimallauncher.data.BodyMetricRecord
import com.adrian.minimallauncher.data.DEFAULT_SECTION_ORDER
import com.adrian.minimallauncher.data.DEFAULT_HOME_PAGES
import com.adrian.minimallauncher.data.DEFAULT_WIDGET_LAYOUT
import com.adrian.minimallauncher.data.FIRST_WIDGET_PAGE
import com.adrian.minimallauncher.data.HomePage
import com.adrian.minimallauncher.data.HomePages
import com.adrian.minimallauncher.data.PageKind
import com.adrian.minimallauncher.data.LauncherApp
import com.adrian.minimallauncher.data.NoteCategory
import com.adrian.minimallauncher.data.NoteRecord
import com.adrian.minimallauncher.data.ReminderRecord
import com.adrian.minimallauncher.data.FinanceRecord
import com.adrian.minimallauncher.data.MealRecord
import com.adrian.minimallauncher.data.SettingsRepository
import com.adrian.minimallauncher.data.fits
import com.adrian.minimallauncher.data.firstFreeSpot
import com.adrian.minimallauncher.data.placed
import com.adrian.minimallauncher.data.WidgetStackConfig
import com.adrian.minimallauncher.data.DayCheck
import com.adrian.minimallauncher.data.LauncherDatabase
import com.adrian.minimallauncher.data.ModuleSettings
import com.adrian.minimallauncher.data.PersonalRepository
import com.adrian.minimallauncher.data.Space
import com.adrian.minimallauncher.data.ReminderScheduler
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    private val settings = SettingsRepository(application)
    private val personal = PersonalRepository(LauncherDatabase.get(application))
    private val backup = BackupRepository(application, LauncherDatabase.get(application), settings)
    private val reminderScheduler = ReminderScheduler(application)
    private val _apps = MutableStateFlow<List<LauncherApp>>(emptyList())
    val apps: StateFlow<List<LauncherApp>> = _apps

    val currentTime: StateFlow<LocalDateTime> = flow {
        while (true) {
            val now = LocalDateTime.now()
            emit(now)
            delay(60_000L - now.second * 1_000L - now.nano / 1_000_000L)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LocalDateTime.now())

    val spaces: StateFlow<List<Space>> = settings.spaces.stateIn(viewModelScope, SharingStarted.Eagerly, Space.all)
    val hiddenSections: StateFlow<Set<String>> = settings.hiddenSections.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val sectionOrder: StateFlow<List<String>> = settings.sectionOrder.stateIn(viewModelScope, SharingStarted.Eagerly, DEFAULT_SECTION_ORDER)
    val homePages: StateFlow<HomePages> = settings.homePages.stateIn(viewModelScope, SharingStarted.Eagerly, DEFAULT_HOME_PAGES)
    val widgetLayouts: StateFlow<Map<String, List<WidgetStackConfig>>> =
        settings.widgetLayouts.stateIn(viewModelScope, SharingStarted.Eagerly, mapOf(FIRST_WIDGET_PAGE to DEFAULT_WIDGET_LAYOUT.placed(), APPS_PAGE to APPS_DEFAULT_LAYOUT))
    val pauseSeconds: StateFlow<Int> = settings.pauseSeconds.stateIn(viewModelScope, SharingStarted.Eagerly, 30)
    val currentSpace: StateFlow<Space> = combine(currentTime, spaces) { time, schedule -> Space.at(time, schedule) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Space.at(LocalDateTime.now()))
    val notes: StateFlow<List<NoteRecord>> = personal.notes.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val reminders: StateFlow<List<ReminderRecord>> = personal.reminders.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val finances: StateFlow<List<FinanceRecord>> = personal.finances.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val meals: StateFlow<List<MealRecord>> = personal.meals.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val bodyMetrics: StateFlow<List<BodyMetricRecord>> = personal.bodyMetrics.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val completedWorkouts: StateFlow<Set<String>> = personal.completedWorkouts.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val completedHabits: StateFlow<Set<String>> = personal.completedHabits.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val moduleSettings: StateFlow<ModuleSettings> = settings.moduleSettings.stateIn(viewModelScope, SharingStarted.Eagerly, ModuleSettings())
    val lastBackupAt: StateFlow<Long?> = settings.lastBackupAt.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val _backupMessage = MutableStateFlow<String?>(null)
    /** Outcome of the last export or import, shown on the backup page. */
    val backupMessage: StateFlow<String?> = _backupMessage

    init {
        refreshApps()
        viewModelScope.launch {
            settings.legacyRecords()?.let { legacy ->
                personal.importLegacy(legacy)
                settings.markRecordsMigrated()
            }
        }
        viewModelScope.launch { personal.reminders.collect { list -> list.forEach(reminderScheduler::schedule) } }
    }

    fun refreshApps() { viewModelScope.launch(Dispatchers.IO) { _apps.value = repository.loadApps() } }
    fun openApp(packageName: String) = repository.openApp(packageName)
    fun addNote(category: NoteCategory, text: String) { viewModelScope.launch { personal.addNote(category, text) } }
    fun deleteNote(id: String) { viewModelScope.launch { personal.deleteNote(id) } }
    fun setNotePinned(id: String, pinned: Boolean) { viewModelScope.launch { personal.setNotePinned(id, pinned) } }
    fun addReminder(title: String, hour: Int, minute: Int, spaceName: String) { viewModelScope.launch { personal.addReminder(title, hour, minute, spaceName) } }
    fun deleteReminder(id: String) { reminderScheduler.cancel(id); viewModelScope.launch { personal.deleteReminder(id) } }
    fun addFinance(amountCents: Long, description: String) { viewModelScope.launch { personal.addFinance(amountCents, description) } }
    fun deleteFinance(id: String) { viewModelScope.launch { personal.deleteFinance(id) } }
    fun addMeal(description: String) { viewModelScope.launch { personal.addMeal(description) } }
    fun deleteMeal(id: String) { viewModelScope.launch { personal.deleteMeal(id) } }
    fun addBodyMetric(type: String, value: Float) { viewModelScope.launch { personal.addBodyMetric(type, value) } }
    fun deleteBodyMetric(id: String) { viewModelScope.launch { personal.deleteBodyMetric(id) } }
    fun toggleWorkout(key: String) { viewModelScope.launch { personal.toggleCheck(key, DayCheck.WORKOUT) } }
    fun toggleHabit(key: String) { viewModelScope.launch { personal.toggleCheck(key, DayCheck.HABIT) } }
    fun updateSpaces(value: List<Space>) { viewModelScope.launch { settings.setSpaces(value) } }
    fun setSectionVisible(section: String, visible: Boolean) { viewModelScope.launch { settings.setSectionVisible(section, visible) } }
    fun moveSection(section: String, delta: Int) { viewModelScope.launch { settings.moveSection(section, delta) } }
    fun setVaultLockSeconds(seconds: Int) = updateModuleSettings { it.copy(vaultLockSeconds = seconds) }
    fun updateModuleSettings(change: (ModuleSettings) -> ModuleSettings) { viewModelScope.launch { settings.setModuleSettings(change(moduleSettings.value)) } }
    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _backupMessage.value = runCatching { backup.export(uri) }
                .fold({ "RESPALDO GUARDADO · ${it.records} REGISTROS" }, { "NO SE PUDO GUARDAR: ${it.message.orEmpty().uppercase()}" })
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _backupMessage.value = runCatching { backup.import(uri) }
                .fold({ if (it.added == 0) "NADA NUEVO · YA TENÍAS TODO" else "IMPORTADO · ${it.added} REGISTROS NUEVOS" }, { "NO SE PUDO IMPORTAR: ${it.message.orEmpty().uppercase()}" })
        }
    }

    fun setHabit(key: String, done: Boolean) { viewModelScope.launch { personal.setCheck(key, DayCheck.HABIT, done) } }
    fun setPauseSeconds(seconds: Int) { viewModelScope.launch { settings.setPauseSeconds(seconds) } }

    // Widget stacks are kept per page; every change rewrites that page's list.
    private fun updatePageLayout(pageId: String, change: (MutableList<WidgetStackConfig>) -> Unit) {
        val all = widgetLayouts.value.toMutableMap()
        val stacks = all[pageId].orEmpty().toMutableList()
        change(stacks)
        all[pageId] = stacks
        viewModelScope.launch { settings.setWidgetLayouts(all) }
    }

    /** Moves a stack to another top-left cell; ignored if it would overlap another widget. */
    fun moveStackTo(pageId: String, stackId: String, col: Int, row: Int) = updatePageLayout(pageId) { stacks ->
        val index = stacks.indexOfFirst { it.id == stackId }
        if (index < 0) return@updatePageLayout
        val stack = stacks[index]
        if (stacks.fits(col, row, stack.w, stack.h, ignoreId = stackId)) stacks[index] = stack.copy(col = col, row = row)
    }

    /** Resizes a stack in place; ignored if the new size would overlap another widget or leave the grid. */
    fun resizeStack(pageId: String, stackId: String, w: Int, h: Int) = updatePageLayout(pageId) { stacks ->
        val index = stacks.indexOfFirst { it.id == stackId }
        if (index < 0) return@updatePageLayout
        val stack = stacks[index]
        if (stacks.fits(stack.col, stack.row, w, h, ignoreId = stackId)) stacks[index] = stack.copy(w = w, h = h)
    }

    /** Drops [draggedId] onto [targetId]: their widgets become one stack at the target's place. */
    fun mergeStacks(pageId: String, draggedId: String, targetId: String) = updatePageLayout(pageId) { stacks ->
        val dragged = stacks.firstOrNull { it.id == draggedId } ?: return@updatePageLayout
        val targetIndex = stacks.indexOfFirst { it.id == targetId }
        if (targetIndex < 0 || draggedId == targetId) return@updatePageLayout
        val target = stacks[targetIndex]
        stacks[targetIndex] = target.copy(widgetIds = (target.widgetIds + dragged.widgetIds).distinct())
        stacks.remove(dragged)
    }

    fun removeWidgetFromStack(pageId: String, stackId: String, widgetId: String) = updatePageLayout(pageId) { stacks ->
        val index = stacks.indexOfFirst { it.id == stackId }
        if (index >= 0) {
            val remaining = stacks[index].widgetIds - widgetId
            if (remaining.isEmpty()) stacks.removeAt(index) else stacks[index] = stacks[index].copy(widgetIds = remaining)
        }
    }

    fun addWidgetToStack(pageId: String, stackId: String, widgetId: String) = updatePageLayout(pageId) { stacks ->
        val index = stacks.indexOfFirst { it.id == stackId }
        if (index >= 0 && widgetId !in stacks[index].widgetIds) stacks[index] = stacks[index].copy(widgetIds = stacks[index].widgetIds + widgetId)
    }

    /** Places a widget at [at] when it fits there (dragged from the catalog), else in the first free spot, like One UI's "Añadir". */
    fun addWidgetAsNewStack(pageId: String, widgetId: String, w: Int, h: Int, at: Pair<Int, Int>? = null) = updatePageLayout(pageId) { stacks ->
        val (col, row) = at?.takeIf { (c, r) -> stacks.fits(c, r, w, h) } ?: stacks.firstFreeSpot(w, h)
        stacks.add(WidgetStackConfig("stack_" + System.currentTimeMillis(), listOf(widgetId), span = if (w >= 4) 2 else 1, col = col, row = row, w = w, h = h))
    }

    private fun savePages(value: HomePages) { viewModelScope.launch { settings.setHomePages(value) } }

    /** Adds an empty widget page at the left or right end and returns its id. */
    fun addPage(atStart: Boolean): String {
        val page = HomePage("widgets_" + System.currentTimeMillis(), PageKind.WIDGETS)
        val current = homePages.value
        savePages(current.copy(pages = if (atStart) listOf(page) + current.pages else current.pages + page))
        return page.id
    }

    /** Removes a page and its widgets. The app list can never be removed: apps only live there. */
    fun deletePage(pageId: String) {
        val current = homePages.value
        val page = current.pages.firstOrNull { it.id == pageId } ?: return
        if (page.kind == PageKind.APPS) return
        val remaining = current.pages - page
        val main = if (current.mainId == pageId) remaining.first { it.kind == PageKind.APPS }.id else current.mainId
        savePages(HomePages(remaining, main))
        if (page.kind == PageKind.WIDGETS) {
            viewModelScope.launch { settings.setWidgetLayouts(widgetLayouts.value - pageId) }
        }
    }

    fun setMainPage(pageId: String) = savePages(homePages.value.copy(mainId = pageId))

    /** Puts the week summary back as the first page after it was deleted. */
    fun restoreWeekPage() {
        val current = homePages.value
        if (current.pages.none { it.kind == PageKind.WEEK }) savePages(current.copy(pages = listOf(HomePage("week", PageKind.WEEK)) + current.pages))
    }

    private val _homePressed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Emits when the home button or gesture is used while the launcher is already open. */
    val homePressed: SharedFlow<Unit> = _homePressed
    fun onHomePressed() { _homePressed.tryEmit(Unit) }
}
