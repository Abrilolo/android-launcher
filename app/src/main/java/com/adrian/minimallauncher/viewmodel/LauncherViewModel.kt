package com.adrian.minimallauncher.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.adrian.minimallauncher.data.AppRepository
import com.adrian.minimallauncher.data.BackupRepository
import com.adrian.minimallauncher.data.BodyMetricRecord
import com.adrian.minimallauncher.data.DEFAULT_SECTION_ORDER
import com.adrian.minimallauncher.data.LauncherApp
import com.adrian.minimallauncher.data.NoteCategory
import com.adrian.minimallauncher.data.NoteRecord
import com.adrian.minimallauncher.data.ReminderRecord
import com.adrian.minimallauncher.data.FinanceRecord
import com.adrian.minimallauncher.data.MealRecord
import com.adrian.minimallauncher.data.SettingsRepository
import com.adrian.minimallauncher.data.DayCheck
import com.adrian.minimallauncher.data.LauncherDatabase
import com.adrian.minimallauncher.data.ModuleSettings
import com.adrian.minimallauncher.data.PersonalRepository
import com.adrian.minimallauncher.data.Space
import com.adrian.minimallauncher.data.ReminderScheduler
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
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
}
