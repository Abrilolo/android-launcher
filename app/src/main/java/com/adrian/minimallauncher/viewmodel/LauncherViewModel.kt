package com.adrian.minimallauncher.viewmodel
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adrian.minimallauncher.data.AppRepository
import com.adrian.minimallauncher.data.LauncherApp
import com.adrian.minimallauncher.data.NoteCategory
import com.adrian.minimallauncher.data.NoteRecord
import com.adrian.minimallauncher.data.ReminderRecord
import com.adrian.minimallauncher.data.FinanceRecord
import com.adrian.minimallauncher.data.MealRecord
import com.adrian.minimallauncher.data.SettingsRepository
import com.adrian.minimallauncher.data.Space
import com.adrian.minimallauncher.data.ReminderScheduler
import com.adrian.minimallauncher.data.WidgetPlacement
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    private val settings = SettingsRepository(application)
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
    val pauseSeconds: StateFlow<Int> = settings.pauseSeconds.stateIn(viewModelScope, SharingStarted.Eagerly, 30)
    val currentSpace: StateFlow<Space> = combine(currentTime, spaces) { time, schedule -> Space.at(time, schedule) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Space.at(LocalDateTime.now()))
    val widgetPlacement: StateFlow<WidgetPlacement> = settings.widgetPlacement.stateIn(viewModelScope, SharingStarted.Eagerly, WidgetPlacement())
    val notes: StateFlow<List<NoteRecord>> = settings.notes.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val reminders: StateFlow<List<ReminderRecord>> = settings.reminders.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val finances: StateFlow<List<FinanceRecord>> = settings.finances.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val meals: StateFlow<List<MealRecord>> = settings.meals.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    init {
        refreshApps()
        viewModelScope.launch { settings.reminders.collect { list -> list.forEach(reminderScheduler::schedule) } }
    }
    fun refreshApps() { viewModelScope.launch(Dispatchers.IO) { _apps.value = repository.loadApps() } }
    fun openApp(packageName: String) = repository.openApp(packageName)
    fun setWidgetPlacement(placement: WidgetPlacement) { viewModelScope.launch { settings.setWidgetPlacement(placement) } }
    fun addNote(category: NoteCategory, text: String) { viewModelScope.launch { settings.addNote(category, text) } }
    fun deleteNote(id: String) { viewModelScope.launch { settings.deleteNote(id) } }
    fun addReminder(title: String, hour: Int, minute: Int, spaceName: String) { viewModelScope.launch { settings.addReminder(title, hour, minute, spaceName) } }
    fun deleteReminder(id: String) { reminderScheduler.cancel(id); viewModelScope.launch { settings.deleteReminder(id) } }
    fun addFinance(amountCents: Long, description: String) { viewModelScope.launch { settings.addFinance(amountCents, description) } }
    fun deleteFinance(id: String) { viewModelScope.launch { settings.deleteFinance(id) } }
    fun addMeal(description: String) { viewModelScope.launch { settings.addMeal(description) } }
    fun deleteMeal(id: String) { viewModelScope.launch { settings.deleteMeal(id) } }
    fun updateSpaces(value: List<Space>) { viewModelScope.launch { settings.setSpaces(value) } }
    fun setSectionVisible(section: String, visible: Boolean) { viewModelScope.launch { settings.setSectionVisible(section, visible) } }
    fun setPauseSeconds(seconds: Int) { viewModelScope.launch { settings.setPauseSeconds(seconds) } }
}
