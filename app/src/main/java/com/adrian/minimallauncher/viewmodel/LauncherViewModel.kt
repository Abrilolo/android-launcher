package com.adrian.minimallauncher.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.adrian.minimallauncher.data.APPS_DEFAULT_LAYOUT
import com.adrian.minimallauncher.data.AppGroup
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
import com.adrian.minimallauncher.data.MoneyKind
import com.adrian.minimallauncher.feature.exercise.ExerciseBurn
import com.adrian.minimallauncher.feature.exercise.StackChoice
import com.adrian.minimallauncher.feature.exercise.StackExercise
import com.adrian.minimallauncher.feature.exercise.TokyoGuide
import com.adrian.minimallauncher.feature.food.FoodDay
import com.adrian.minimallauncher.feature.food.FoodMath
import com.adrian.minimallauncher.feature.food.FoodRepository
import com.adrian.minimallauncher.feature.food.Ingredient
import com.adrian.minimallauncher.feature.food.Nutrition
import com.adrian.minimallauncher.feature.food.Recipe
import com.adrian.minimallauncher.feature.food.RecipeDetail
import com.adrian.minimallauncher.feature.food.RecipeItem
import com.adrian.minimallauncher.feature.finance.Budget
import com.adrian.minimallauncher.feature.finance.BudgetLimit
import com.adrian.minimallauncher.feature.finance.BudgetPeriod
import com.adrian.minimallauncher.feature.finance.FinanceSummary
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
import com.adrian.minimallauncher.data.ForcedMode
import com.adrian.minimallauncher.data.ForcedRepository
import com.adrian.minimallauncher.data.ForcedState
import com.adrian.minimallauncher.data.MealSlot
import com.adrian.minimallauncher.data.routine.ActionCatalog
import com.adrian.minimallauncher.data.routine.ActionKind
import com.adrian.minimallauncher.data.routine.ActionType
import com.adrian.minimallauncher.data.routine.PhaseMode
import com.adrian.minimallauncher.data.routine.RoutineAction
import com.adrian.minimallauncher.data.routine.newRoutineId
import com.adrian.minimallauncher.data.routine.Place
import com.adrian.minimallauncher.data.routine.Places
import com.adrian.minimallauncher.data.routine.RoutineActions
import com.adrian.minimallauncher.data.routine.WidgetInputs
import com.adrian.minimallauncher.data.routine.WidgetPick
import com.adrian.minimallauncher.data.routine.PhaseReminder
import com.adrian.minimallauncher.data.routine.PhaseReminders
import com.adrian.minimallauncher.data.routine.WidgetSelector
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import com.adrian.minimallauncher.data.routine.ActionState
import com.adrian.minimallauncher.data.routine.PlaceEvent
import com.adrian.minimallauncher.data.routine.RoutineDefaults
import com.adrian.minimallauncher.data.routine.RoutineEngine
import com.adrian.minimallauncher.data.routine.RoutinePhase
import com.adrian.minimallauncher.data.routine.RoutineRepository
import com.adrian.minimallauncher.data.routine.Trigger
import com.adrian.minimallauncher.feature.ai.AiApp
import com.adrian.minimallauncher.feature.ai.AppShortcut
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold
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
    private val routine = RoutineRepository(application, settings)
    private val _phaseEntered = Channel<RoutinePhase>(Channel.BUFFERED)
    private val food = FoodRepository(application)
    private val forced = ForcedRepository(application)
    // Answers to "¿comida?" by payment id; kept in memory (a saved food expense resumes at the meal step).
    private val foodAnswers = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private val stackDao = LauncherDatabase.get(application).stack()
    private val _apps = MutableStateFlow<List<LauncherApp>>(emptyList())
    val apps: StateFlow<List<LauncherApp>> = _apps

    val currentTime: StateFlow<LocalDateTime> = flow {
        while (true) {
            val now = LocalDateTime.now()
            emit(now)
            delay(60_000L - now.second * 1_000L - now.nano / 1_000_000L)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LocalDateTime.now())

    /** The five routine phases (inicio, traslado, trabajo, salida, casa); they replaced the three spaces. */
    val phases: StateFlow<List<RoutinePhase>> = routine.phases.stateIn(viewModelScope, SharingStarted.Eagerly, RoutineDefaults.fromSpaces(Space.all))
    /** The last arrive/leave event (geofence or test), which can start salida or casa before their time. */
    val lastPlaceEvent: StateFlow<PlaceEvent?> = routine.lastEvent.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    /** The phase now, by time window or by the last place event. */
    val currentPhase: StateFlow<RoutinePhase> = combine(currentTime, phases, lastPlaceEvent) { time, all, event -> RoutineEngine.phaseAt(time, all, event) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, RoutineEngine.phaseAt(LocalDateTime.now(), RoutineDefaults.fromSpaces(Space.all), null))
    /** The current phase's dot reminders with today's done marks, in order. */
    val phaseChecks: StateFlow<List<ActionState>> = combine(currentPhase, currentTime, personal.completedRoutine) { phase, time, done ->
        RoutineEngine.actionStates(phase, time.toLocalDate(), done)
    }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    /** The next checklist step of the phase (casa: ordenar, agua, descanso, gimnasio, dormir), or null when all are done. */
    val nextPhaseStep: StateFlow<ActionState?> = phaseChecks.map(RoutineEngine::nextStep).stateIn(viewModelScope, SharingStarted.Eagerly, null)
    /** Every routine day check, so the Rutinas view can show today's marks of phases other than the current one. */
    val routineDone: StateFlow<Set<String>> = personal.completedRoutine.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /** Casa and oficina, once set from the phone's location. */
    val places: StateFlow<List<Place>> = routine.places.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Each phase as a space, so the settings page, the app list and the pause keep working unchanged. */
    val spaces: StateFlow<List<Space>> = phases.map { all -> all.map { it.toSpace() } }.stateIn(viewModelScope, SharingStarted.Eagerly, Space.all)
    val hiddenSections: StateFlow<Set<String>> = settings.hiddenSections.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val sectionOrder: StateFlow<List<String>> = settings.sectionOrder.stateIn(viewModelScope, SharingStarted.Eagerly, DEFAULT_SECTION_ORDER)
    val homePages: StateFlow<HomePages> = settings.homePages.stateIn(viewModelScope, SharingStarted.Eagerly, DEFAULT_HOME_PAGES)
    val widgetLayouts: StateFlow<Map<String, List<WidgetStackConfig>>> =
        settings.widgetLayouts.stateIn(viewModelScope, SharingStarted.Eagerly, mapOf(FIRST_WIDGET_PAGE to DEFAULT_WIDGET_LAYOUT.placed(), APPS_PAGE to APPS_DEFAULT_LAYOUT))
    val pauseSeconds: StateFlow<Int> = settings.pauseSeconds.stateIn(viewModelScope, SharingStarted.Eagerly, 30)
    /** The current phase as a space: its apps are the ones that open now. */
    val currentSpace: StateFlow<Space> = currentPhase.map { it.toSpace() }.stateIn(viewModelScope, SharingStarted.Eagerly, currentPhase.value.toSpace())
    val notes: StateFlow<List<NoteRecord>> = personal.notes.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val reminders: StateFlow<List<ReminderRecord>> = personal.reminders.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    /** Ids of the reminders (of every phase) marked done today; [toggleReminderDone] works for any phase. */
    val remindersDoneToday: StateFlow<Set<String>> = combine(currentTime.map { it.toLocalDate() }.distinctUntilChanged(), personal.completedReminders) { date, keys ->
        val prefix = PhaseReminders.doneKey(date, "")
        keys.filter { it.startsWith(prefix) }.map { it.removePrefix(prefix) }.toSet()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    /**
     * The current phase's actions that have an hour plus its linked reminders, sorted by time, each with icon
     * key, hour and done for today. What the Rutinas widget lists.
     */
    val phaseReminders: StateFlow<List<PhaseReminder>> = combine(currentPhase, currentTime, reminders, personal.completedRoutine, personal.completedReminders) { phase, time, list, actionsDone, remindersDone ->
        PhaseReminders.of(phase, list, time.toLocalDate(), actionsDone + remindersDone)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    /** Day-to-day spending (extras and shared bills); incomes and fixed costs are in [money]. */
    val finances: StateFlow<List<FinanceRecord>> = personal.finances.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val money: StateFlow<List<FinanceRecord>> = personal.money.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val budgets: StateFlow<List<BudgetLimit>> = personal.budgets.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val meals: StateFlow<List<MealRecord>> = personal.meals.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val bodyMetrics: StateFlow<List<BodyMetricRecord>> = personal.bodyMetrics.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val completedWorkouts: StateFlow<Set<String>> = personal.completedWorkouts.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val completedHabits: StateFlow<Set<String>> = personal.completedHabits.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /** The assistants' own shortcuts, by package; empty until this is the default launcher. */
    private val _aiShortcuts = MutableStateFlow<Map<String, List<AppShortcut>>>(emptyMap())
    val aiShortcuts: StateFlow<Map<String, List<AppShortcut>>> = _aiShortcuts

    val moduleSettings: StateFlow<ModuleSettings> = settings.moduleSettings.stateIn(viewModelScope, SharingStarted.Eagerly, ModuleSettings())

    val recipes: StateFlow<List<Recipe>> = food.recipes.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val recipeItems: StateFlow<List<RecipeItem>> = food.recipeItems.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    /** USDA seed plus the user's own; values per 100 g, with the usual portion. */
    val ingredients: StateFlow<List<Ingredient>> = food.ingredients.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    /** Today's kcal and macros against the goals, latest weight and BMI (IMC). */
    val foodDay: StateFlow<FoodDay> = combine(meals, bodyMetrics, moduleSettings, currentTime.map { it.toLocalDate() }.distinctUntilChanged()) { m, metrics, s, date ->
        FoodMath.day(date, m, metrics, s)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, FoodDay())
    /** The exercise stack by muscle group (placeholder rows until the user fills it). */
    val exerciseStack: StateFlow<List<StackExercise>> = stackDao.stack().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    /** When today's kcal pass the goal: the exercise of today's muscle group that burns the excess; null otherwise. */
    val exerciseAdjustment: StateFlow<StackChoice?> = combine(foodDay, exerciseStack) { day, stack ->
        ExerciseBurn.pick(stack, ExerciseBurn.groupOf(TokyoGuide.sessionFor(day.date)), day.over, day.weightKg ?: ExerciseBurn.DEFAULT_WEIGHT_KG)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Today's meals whose time already passed and are not marked: what salida asks to register. */
    val unmarkedMeals: StateFlow<List<MealSlot>> = combine(currentTime, moduleSettings, completedHabits) { now, s, habits ->
        val date = now.toLocalDate()
        s.meals.filter { !it.time.isAfter(now.toLocalTime()) && "$date|meal_${it.id}" !in habits }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Non-null while something must be registered before using the phone: the launcher shows the flow
     * (¿comida? → gasto → comida, or salida's unmarked meals) and back does not leave it.
     */
    val forcedState: StateFlow<ForcedState?> = combine(forced.openPayments, foodAnswers, forced.routinePending) { open, answers, routinePending ->
        ForcedMode.state(open, answers, routinePending)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Notes not done yet per aspect (Trabajo, Escuela, Ejercicio, Comida, Dieta), for the notes widget. */
    val pendingNotes: StateFlow<Map<NoteCategory, Int>> = notes.map(WidgetSelector::pendingByAspect).stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val widgetInputs = combine(
        combine(forced.openPayments, moduleSettings, completedHabits, completedWorkouts) { open, s, habits, workouts ->
            WidgetInputs(open.size, open.sumOf { it.amountCents }, s.meals, habits, s.exerciseStart, workouts)
        },
        phaseChecks,
        pendingNotes,
        phaseReminders,
    ) { base, checks, pendingByAspect, timed -> base.copy(phaseChecks = checks, pendingNotes = pendingByAspect, phaseReminders = timed) }

    /** What the widgets should show now for the current phase, most urgent first (pending payment, meal soon, training…). */
    val widgetPicks: StateFlow<List<WidgetPick>> = combine(currentPhase, currentTime, widgetInputs) { phase, now, inputs -> WidgetSelector.pick(phase, now, inputs) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Budget, savings, extras bag, today's allowance (lowered by overspending this week), limits per topic and shared bills. */
    val financeSummary: StateFlow<FinanceSummary> = combine(money, budgets, moduleSettings, currentTime.map { it.toLocalDate() }.distinctUntilChanged()) { rows, limits, s, date ->
        Budget.summary(rows, limits, s.savingPct, s.dailyBudgetCents, date)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, FinanceSummary())
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
        // Reminders are rescheduled when they or the phases change; the receiver rings only in their phase.
        viewModelScope.launch { combine(personal.reminders, routine.phases) { list, _ -> list }.collect { list -> list.forEach(reminderScheduler::schedule) } }
        viewModelScope.launch {
            // Links reminders saved with an old space name, or whose phase was deleted, to a phase.
            combine(personal.reminders, routine.phases) { list, all -> list to all }.collect { (list, all) ->
                val spaces = settings.spaces.first()
                list.forEach { r ->
                    if (all.isNotEmpty() && all.none { it.id == r.phaseId }) {
                        val resolved = PhaseReminders.phaseIdFor(r.phaseId, r.hour, r.minute, all, spaces)
                        if (resolved != r.phaseId) personal.upsertReminder(r.copy(phaseId = resolved))
                    }
                }
            }
        }
        viewModelScope.launch { Places.register(getApplication(), routine.places.first()) }
        viewModelScope.launch {
            food.seedIfNeeded()
            if (stackDao.count() == 0) stackDao.insert(ExerciseBurn.PLACEHOLDER_STACK)
        }
        viewModelScope.launch {
            routine.migrateSpaces()
            // Reschedule the dots whenever the phases change, cancelling the ones that were removed.
            routine.phases.runningFold(emptyList<RoutinePhase>() to emptyList<RoutinePhase>()) { (_, previous), next -> previous to next }
                .collect { (previous, next) -> if (next.isNotEmpty()) reminderScheduler.schedulePhases(next, previous) }
        }
        viewModelScope.launch {
            currentPhase.map { it.id }.distinctUntilChanged().collect { id ->
                if (routine.lastEntered.first() != id) {
                    routine.setLastEntered(id)
                    val phase = currentPhase.value
                    RoutineActions.onEnter(getApplication(), phase)
                    _phaseEntered.trySend(phase)
                }
            }
        }
    }

    fun refreshApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _apps.value = repository.loadApps()
            _aiShortcuts.value = repository.loadShortcuts(AiApp.entries.map { it.packageName })
        }
    }
    fun openApp(packageName: String) = repository.openApp(packageName)
    fun openShortcut(packageName: String, id: String) = repository.startShortcut(packageName, id)
    fun openStore(packageName: String) = repository.openStore(packageName)
    fun newPhotoUri(): Uri = repository.newPhotoUri()
    fun shareImage(packageName: String, uri: Uri) = repository.shareImage(packageName, uri)

    /** Swaps one widget of a stack for another in place, e.g. the app an assistant widget shows. */
    fun replaceWidget(pageId: String, stackId: String, oldId: String, newId: String) = updatePageLayout(pageId) { stacks ->
        val index = stacks.indexOfFirst { it.id == stackId }
        if (index < 0 || oldId == newId) return@updatePageLayout
        val ids = stacks[index].widgetIds
        stacks[index] = stacks[index].copy(widgetIds = (if (newId in ids) ids - oldId else ids.map { if (it == oldId) newId else it }))
    }
    fun addNote(category: NoteCategory, text: String) { viewModelScope.launch { personal.addNote(category, text) } }
    fun deleteNote(id: String) { viewModelScope.launch { personal.deleteNote(id) } }
    fun setNotePinned(id: String, pinned: Boolean) { viewModelScope.launch { personal.setNotePinned(id, pinned) } }
    /** A quick note is pending until marked done; the pending ones per aspect are [pendingNotes]. */
    fun setNoteDone(id: String, done: Boolean) { viewModelScope.launch { personal.setNoteDone(id, done) } }
    /** Adds a reminder to a phase ([phase] is its id, or its name as the old reminders form passes); default: the current phase. */
    fun addReminder(title: String, hour: Int, minute: Int, phase: String = currentPhase.value.id) {
        val all = phases.value
        val phaseId = all.firstOrNull { it.id == phase }?.id ?: all.firstOrNull { it.name.equals(phase, ignoreCase = true) }?.id ?: currentPhase.value.id
        viewModelScope.launch { personal.addReminder(title, hour, minute, phaseId) }
    }
    fun updateReminder(id: String, title: String, hour: Int, minute: Int) {
        val r = reminders.value.firstOrNull { it.id == id } ?: return
        viewModelScope.launch { personal.upsertReminder(r.copy(title = title, hour = hour, minute = minute)) }
    }
    /** Moves a reminder to another phase; it then rings only there. */
    fun moveReminder(id: String, phaseId: String) {
        val r = reminders.value.firstOrNull { it.id == id } ?: return
        if (phases.value.none { it.id == phaseId }) return
        viewModelScope.launch { personal.upsertReminder(r.copy(phaseId = phaseId)) }
    }
    /** Marks a reminder done or not for today, like the routine dots. */
    fun toggleReminderDone(id: String) { viewModelScope.launch { personal.toggleCheck(PhaseReminders.doneKey(java.time.LocalDate.now(), id), DayCheck.REMINDER) } }
    /** Marks an item of [phaseReminders]: an action through [markAction], a reminder through [toggleReminderDone]. */
    fun markPhaseReminder(item: PhaseReminder) = when (item.source) {
        PhaseReminder.Source.ACTION -> markAction(item.id)
        PhaseReminder.Source.REMINDER -> toggleReminderDone(item.id)
    }
    fun deleteReminder(id: String) { reminderScheduler.cancel(id); viewModelScope.launch { personal.deleteReminder(id) } }
    fun addFinance(amountCents: Long, description: String) { viewModelScope.launch { personal.addFinance(amountCents, description) } }
    fun deleteFinance(id: String) { viewModelScope.launch { personal.deleteFinance(id) } }
    /** Any movement: incomes (fixed, transfer, gift, service), fixed costs, extras, or a shared bill with what others owe. */
    fun addMoney(kind: MoneyKind, amountCents: Long, description: String, category: String = "", pendingFromOthersCents: Long = 0) {
        viewModelScope.launch { personal.addMoney(kind, amountCents, description, category, pendingFromOthersCents) }
    }
    fun settleShared(id: String, paidCents: Long) { viewModelScope.launch { personal.settle(id, paidCents) } }
    /** Editable limit per topic ("comida", "transporte"… or [BudgetLimit.TOTAL]) and period; 0 removes it. */
    fun setBudget(topic: String, period: BudgetPeriod, limitCents: Long) { viewModelScope.launch { personal.setBudget(topic, period, limitCents) } }
    fun setSavingPct(pct: Int) = updateModuleSettings { it.copy(savingPct = pct.coerceIn(0, 100)) }
    fun searchIngredients(query: String, onResult: (List<Ingredient>) -> Unit) { viewModelScope.launch { onResult(food.search(query)) } }
    /** The user's own ingredient, values per 100 g. */
    fun addIngredient(name: String, per100g: Nutrition, portionGrams: Float = 100f, portionName: String = "100 g") {
        viewModelScope.launch { food.addIngredient(name, per100g, portionGrams, portionName) }
    }
    fun updateIngredient(ingredient: Ingredient) { viewModelScope.launch { food.updateIngredient(ingredient) } }
    /** Edits an ingredient's usual portion ("1 pieza" = 118 g). */
    fun setIngredientPortion(ingredient: Ingredient, grams: Float, name: String) { viewModelScope.launch { food.setPortion(ingredient, grams, name) } }
    fun deleteIngredient(id: String) { viewModelScope.launch { food.deleteIngredient(id) } }
    fun newRecipe(name: String): Recipe = food.newRecipe(name)
    /** Saves a recipe (photo uri, steps, prep time, servings) and its ingredients, id to grams. */
    fun saveRecipe(recipe: Recipe, items: Map<String, Float>) { viewModelScope.launch { food.saveRecipe(recipe, items) } }
    fun deleteRecipe(id: String) {
        viewModelScope.launch {
            food.deleteRecipe(id)
            // A deleted recipe is no longer planned for any meal.
            moduleSettings.value.meals.filter { it.recipeId == id }.forEach { settings.setMealPlan(it.id, null) }
        }
    }
    fun recipeDetail(id: String, onResult: (RecipeDetail?) -> Unit) { viewModelScope.launch { onResult(food.detail(id)) } }
    /** Registers a meal from a recipe; [onSaved] gets the meal id. */
    fun logRecipeMeal(recipeId: String, servings: Float = 1f, slotId: String? = null, onSaved: (String?) -> Unit = {}) {
        viewModelScope.launch { onSaved(food.logRecipe(recipeId, servings, slotId = slotId)) }
    }
    /** Registers a meal from ingredients, id to grams; [onSaved] gets the meal id. */
    fun logIngredientMeal(description: String, items: Map<String, Float>, slotId: String? = null, onSaved: (String) -> Unit = {}) {
        viewModelScope.launch { onSaved(food.logIngredients(description, items, slotId)) }
    }
    /** Plans a recipe for a meal slot ([MealSlot.recipeId]); it stays every day until changed. Null clears it. */
    fun setMealPlan(slotId: String, recipeId: String?) { viewModelScope.launch { settings.setMealPlan(slotId, recipeId) } }
    fun setFoodGoals(kcal: Int, proteinG: Int, carbsG: Int, fatG: Int, fiberG: Int) =
        updateModuleSettings { it.copy(kcalGoal = kcal, proteinGoalG = proteinG, carbsGoalG = carbsG, fatGoalG = fatG, fiberGoalG = fiberG) }
    /** Weight goal and height for the BMI; the weight itself is a body metric ("WEIGHT"). */
    fun setBodyGoals(weightGoalKg: Float, heightCm: Float) = updateModuleSettings { it.copy(weightGoalKg = weightGoalKg, heightCm = heightCm) }
    fun saveStackExercise(exercise: StackExercise) { viewModelScope.launch { stackDao.upsert(exercise.copy(placeholder = false)) } }
    fun deleteStackExercise(id: String) { viewModelScope.launch { stackDao.delete(id) } }
    /** Removes the sample rows once the user starts their own stack. */
    fun clearPlaceholderStack() { viewModelScope.launch { stackDao.clearPlaceholders() } }

    /** Adds a place to the user's list (no coordinates yet; set them with [setPlaceHere]) and returns its id. */
    fun addPlace(name: String): String {
        val place = Place(newRoutineId(), name.trim())
        viewModelScope.launch { routine.setPlace(place) }
        return place.id
    }
    fun renamePlace(id: String, name: String) {
        val place = places.value.firstOrNull { it.id == id } ?: return
        viewModelScope.launch { routine.setPlace(place.copy(name = name.trim())) }
    }
    /** Saves a place at the current location (needs location); [onResult] gets null when there was no fix. */
    fun setPlaceHere(id: String, onResult: (Place?) -> Unit = {}) { viewModelScope.launch { onResult(Places.setHere(getApplication(), id)) } }
    /** Removes a place; phases it started keep their clock window. */
    fun removePlace(id: String) {
        viewModelScope.launch {
            routine.removePlace(id)
            Places.register(getApplication(), routine.places.first())
        }
    }
    /** Re-registers the geofences, e.g. after the location permissions were granted. */
    fun registerPlaces() { viewModelScope.launch { Places.register(getApplication(), places.value) } }
    fun locationGranted(): Boolean = Places.hasForeground(getApplication()) && Places.hasBackground(getApplication())

    /** Test stand-in for a geofence: arriving at or leaving a place starts its phase now. */
    fun simulatePlace(place: String, entered: Boolean) {
        val phase = RoutineEngine.phaseFor(place, entered, phases.value) ?: return
        viewModelScope.launch { routine.setLastEvent(PlaceEvent(phase.id, LocalDateTime.now())) }
    }

    fun setWhatsApp(phone: String, text: String) = updateModuleSettings { it.copy(whatsappPhone = phone.filter(Char::isDigit), whatsappText = text) }
    /** Opens WhatsApp on the salida chat with the text ready; false without a phone set. */
    fun openWhatsAppLeft(): Boolean = moduleSettings.value.let { Places.openWhatsApp(getApplication(), it.whatsappPhone, it.whatsappText) }
    /** Opens the chat of [phase]'s WhatsApp action (its own chat, else the one in settings); false without one. */
    fun openWhatsAppFor(phase: RoutinePhase): Boolean {
        val (phone, text) = RoutineActions.whatsAppTarget(phase, moduleSettings.value) ?: return false
        return Places.openWhatsApp(getApplication(), phone, text)
    }

    /** Forced flow, step ¿comida?: the answer decides whether a meal is asked after the expense. */
    fun forcedAnswerFood(isFood: Boolean) {
        val payment = forcedState.value?.payment ?: return
        foodAnswers.value = foodAnswers.value + (payment.id to isFood)
    }

    /** Forced flow, step gasto: saves the expense (category "comida" when it was food). */
    fun forcedSaveExpense(amountCents: Long, description: String, category: String = "") {
        val state = forcedState.value ?: return
        val payment = state.payment ?: return
        val isFood = state.isFood == true
        viewModelScope.launch { forced.saveExpense(payment, amountCents, description, category.ifBlank { if (isFood) "comida" else "" }, isFood) }
    }

    /** Forced flow, step comida: a quick meal by description, or made of ingredients (id to grams). */
    fun forcedSaveMeal(description: String, items: Map<String, Float> = emptyMap()) {
        val payment = forcedState.value?.payment ?: return
        viewModelScope.launch { forced.saveMeal(payment, food.logIngredients(description, items)) }
    }

    fun forcedSaveRecipeMeal(recipeId: String, servings: Float = 1f) {
        val payment = forcedState.value?.payment ?: return
        viewModelScope.launch { food.logRecipe(recipeId, servings)?.let { forced.saveMeal(payment, it) } }
    }

    /** Salida's registration is done (the unmarked meals were marked or registered). */
    fun finishRoutineRegistration() { viewModelScope.launch { forced.setRoutinePending(false) } }

    /** Debug stand-in for a bank notification: a $85.00 OXXO payment. */
    fun debugAddPendingPayment(amountCents: Long = 8500, merchant: String = "OXXO") { viewModelScope.launch { forced.addTestPayment(amountCents, merchant) } }

    /** Whether the accessibility service that covers the whole phone is on. */
    fun forcedServiceEnabled(): Boolean = ForcedMode.serviceEnabled(getApplication())

    /** True when spending [amountCents] on [category] now would go over what is left today. */
    fun wouldExceedBudget(category: String, amountCents: Long): Boolean = Budget.wouldExceed(financeSummary.value, category, amountCents)
    fun addMeal(description: String) { viewModelScope.launch { personal.addMeal(description) } }
    fun deleteMeal(id: String) { viewModelScope.launch { personal.deleteMeal(id) } }
    fun addBodyMetric(type: String, value: Float) { viewModelScope.launch { personal.addBodyMetric(type, value) } }
    fun deleteBodyMetric(id: String) { viewModelScope.launch { personal.deleteBodyMetric(id) } }
    fun toggleWorkout(key: String) { viewModelScope.launch { personal.toggleCheck(key, DayCheck.WORKOUT) } }
    fun toggleHabit(key: String) { viewModelScope.launch { personal.toggleCheck(key, DayCheck.HABIT) } }
    /** Saves the settings page's spaces into the phases (same order): names, hours and app groups. */
    fun updateSpaces(value: List<Space>) {
        val current = phases.value
        if (value.size != current.size) {
            viewModelScope.launch { settings.setSpaces(value) }
            return
        }
        updatePhases(current.zip(value) { phase, space ->
            val window = if (phase.window != null || space.startsAt != space.endsAt) Trigger.Between(space.startsAt, space.endsAt) else null
            phase.copy(name = space.name, window = window, trigger = if (phase.trigger is Trigger.Between && window != null) window else phase.trigger, groups = space.availableGroups)
        })
    }

    fun updatePhases(value: List<RoutinePhase>) { viewModelScope.launch { routine.setPhases(value) } }
    fun updatePhase(id: String, change: (RoutinePhase) -> RoutinePhase) = updatePhases(phases.value.map { if (it.id == id) change(it) else it })

    // Phases: the user's ordered list; the five of the plan are only the first-load seed.

    /** The action types the user can add to a phase (agua, música, WhatsApp, descanso…). */
    val actionCatalog: List<ActionType> get() = ActionCatalog.all

    /** Adds a phase at the end (or at [index]) and returns its id. A time trigger is also its window. */
    fun addPhase(name: String, trigger: Trigger, mode: PhaseMode = PhaseMode.CHOOSER, index: Int? = null): String {
        val phase = RoutinePhase.new(name.trim(), trigger, currentPhase.value.groups).copy(mode = mode)
        val list = phases.value.toMutableList()
        list.add((index ?: list.size).coerceIn(0, list.size), phase)
        updatePhases(list)
        return phase.id
    }
    /** Deletes a phase; the last one stays (some phase always holds). */
    fun deletePhase(id: String) { if (phases.value.size > 1) updatePhases(phases.value.filterNot { it.id == id }) }
    fun renamePhase(id: String, name: String) = updatePhase(id) { it.copy(name = name.trim()) }
    fun movePhase(id: String, delta: Int) = updatePhases(phases.value.moved({ it.id == id }, delta))
    /** What starts the phase, and its clock window ([window] null: only a place event starts it). */
    fun setPhaseTrigger(id: String, trigger: Trigger, window: Trigger.Between? = trigger as? Trigger.Between) = updatePhase(id) { it.copy(trigger = trigger, window = window) }
    fun setPhaseMode(id: String, mode: PhaseMode) = updatePhase(id) { it.copy(mode = mode) }
    /** Which app groups open during the phase (the old space rule). */
    fun setPhaseGroups(id: String, groups: Set<AppGroup>) = updatePhase(id) { it.copy(groups = groups) }

    /** Adds an action of catalog [type] with its config (hour, minutes, chat, app package) and returns its id. */
    fun addAction(phaseId: String, type: String, config: Map<String, String> = emptyMap(), index: Int? = null): String {
        val action = RoutineAction.new(type, config)
        updatePhase(phaseId) { p -> p.copy(actions = p.actions.toMutableList().apply { add((index ?: size).coerceIn(0, size), action) }) }
        return action.id
    }
    fun removeAction(phaseId: String, actionId: String) = updatePhase(phaseId) { p -> p.copy(actions = p.actions.filterNot { it.id == actionId }) }
    fun moveAction(phaseId: String, actionId: String, delta: Int) = updatePhase(phaseId) { p -> p.copy(actions = p.actions.moved({ it.id == actionId }, delta)) }
    /** Replaces an action's config; blank values are dropped. */
    fun setActionConfig(phaseId: String, actionId: String, config: Map<String, String>) = updatePhase(phaseId) { p ->
        p.copy(actions = p.actions.map { if (it.id == actionId) it.copy(config = config.filterValues { v -> v.isNotBlank() }) else it })
    }

    /** Marks an action of the current phase done or not; in an ordered phase only the next step can be marked. */
    fun markAction(actionId: String) {
        val states = phaseChecks.value
        if (!RoutineEngine.canMark(currentPhase.value, states, actionId)) return
        val key = states.first { it.action.id == actionId }.key
        viewModelScope.launch { personal.toggleCheck(key, DayCheck.ROUTINE) }
    }
    /** Turns an action on or off; it stays that way until edited again (no daily reset), as "Iniciar el día" is set up once. */
    fun setActionEnabled(phaseId: String, actionId: String, on: Boolean) = updatePhase(phaseId) { p ->
        p.copy(actions = p.actions.map { if (it.id == actionId) it.copy(enabled = on) else it })
    }

    /** Opens an audio action's app ([actionId], or the phase's first audio with an app); false when none is chosen. */
    fun openPhaseAudio(actionId: String? = null): Boolean {
        val pkg = currentPhase.value.actions
            .filter { it.kind.kind == ActionKind.AUDIO && (actionId == null || it.id == actionId) }
            .firstNotNullOfOrNull { it.packageName.takeIf(String::isNotBlank) } ?: return false
        repository.openApp(pkg)
        return true
    }

    /** Resets the routine: clears completed marks, place event overrides, and entered phase state to test the whole flow again. */
    fun resetRoutine() {
        viewModelScope.launch {
            personal.clearRoutineChecks()
            routine.clearLastEvent()
            routine.clearLastEntered()
        }
    }

    private fun <T> List<T>.moved(match: (T) -> Boolean, delta: Int): List<T> {
        val from = indexOfFirst(match)
        val to = from + delta
        if (from < 0 || to !in indices) return this
        return toMutableList().apply { add(to, removeAt(from)) }
    }
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
        val merged = (target.widgetIds + dragged.widgetIds).distinct()
        val alpha = if (target.widgetIds == listOf("clock") && target.bgAlpha == 0f && merged.size > 1) 1f else target.bgAlpha
        stacks[targetIndex] = target.copy(widgetIds = merged, bgAlpha = alpha)
        stacks.remove(dragged)
    }

    fun removeStack(pageId: String, stackId: String) = updatePageLayout(pageId) { stacks ->
        stacks.removeAll { it.id == stackId }
    }

    fun removeWidgetFromStack(pageId: String, stackId: String, widgetId: String) = updatePageLayout(pageId) { stacks ->
        val index = stacks.indexOfFirst { it.id == stackId }
        if (index >= 0) {
            val remaining = stacks[index].widgetIds - widgetId
            if (remaining.isEmpty()) stacks.removeAt(index) else stacks[index] = stacks[index].copy(widgetIds = remaining)
        }
    }

    /** Sets how solid the card behind a stack is, from the widget's settings (0 = invisible, 1 = solid). */
    fun setWidgetAlpha(pageId: String, stackId: String, alpha: Float) = updatePageLayout(pageId) { stacks ->
        val index = stacks.indexOfFirst { it.id == stackId }
        if (index >= 0) stacks[index] = stacks[index].copy(bgAlpha = alpha.coerceIn(0f, 1f))
    }

    fun addWidgetToStack(pageId: String, stackId: String, widgetId: String) = updatePageLayout(pageId) { stacks ->
        val index = stacks.indexOfFirst { it.id == stackId }
        if (index >= 0 && widgetId !in stacks[index].widgetIds) {
            val s = stacks[index]
            val alpha = if (s.widgetIds == listOf("clock") && s.bgAlpha == 0f) 1f else s.bgAlpha
            stacks[index] = s.copy(widgetIds = s.widgetIds + widgetId, bgAlpha = alpha)
        }
    }

    /** Places a widget at [at] when it fits there (dragged from the catalog), else in the first free spot, like One UI's "Añadir". */
    fun addWidgetAsNewStack(pageId: String, widgetId: String, w: Int, h: Int, at: Pair<Int, Int>? = null): String {
        val id = "stack_" + System.currentTimeMillis()
        updatePageLayout(pageId) { stacks ->
            val (col, row) = at?.takeIf { (c, r) -> stacks.fits(c, r, w, h) } ?: stacks.firstFreeSpot(w, h)
            stacks.add(WidgetStackConfig(id, listOf(widgetId), span = if (w >= 4) 2 else 1, col = col, row = row, w = w, h = h))
        }
        return id
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

    /**
     * Each new phase once, while the launcher is open (its screen actions are due: salida opens WhatsApp).
     * Consumed once; a phase started by a geofence with the launcher closed is handled by the receiver instead.
     */
    val phaseEntered: Flow<RoutinePhase> = _phaseEntered.receiveAsFlow()

    private val _homePressed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Emits when the home button or gesture is used while the launcher is already open. */
    val homePressed: SharedFlow<Unit> = _homePressed
    fun onHomePressed() { _homePressed.tryEmit(Unit) }
}
