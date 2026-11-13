package com.adrian.minimallauncher.feature.exercise

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adrian.minimallauncher.data.BodyMetricRecord
import com.adrian.minimallauncher.data.DayCheck
import com.adrian.minimallauncher.data.LauncherDatabase
import com.adrian.minimallauncher.data.ModuleSettings
import com.adrian.minimallauncher.data.PersonalRepository
import com.adrian.minimallauncher.data.SettingsRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the exercise page and widget show for one day. */
data class ExerciseDay(
    val date: LocalDate = LocalDate.now(),
    val session: Session = TokyoGuide.sessionFor(LocalDate.now()),
    val sets: Map<String, List<SetLog>> = emptyMap(),
    val lastLogs: Map<String, SetLog> = emptyMap(),
    val completed: Boolean = false,
    /** Days of the current week (Monday first) with a finished session or at least one logged set. */
    val weekDone: Set<LocalDate> = emptySet(),
    /** Days of the current week that the routine leaves free. */
    val weekRest: Set<LocalDate> = emptySet(),
    /** Monday to Sunday of the current week, each with its routine. */
    val week: List<Pair<LocalDate, Session>> = emptyList(),
    /** The saved plans (week and single days), to work out any date's routine with [RoutinePlans.sessionFor]. */
    val plans: Map<String, String> = emptyMap(),
    /** Days of the last twelve weeks with a finished session or a logged set: the consistency history. */
    val doneDates: Set<LocalDate> = emptySet(),
) {
    val doneSets: Int get() = session.exercises.sumOf { ex -> sets[ex.id].orEmpty().size.coerceAtMost(ex.sets) }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseViewModel(application: Application) : AndroidViewModel(application) {
    private val db = LauncherDatabase.get(application)
    private val dao = db.exercise()
    private val personal = PersonalRepository(db)
    private val settingsRepository = SettingsRepository(application)

    // Re-checked every minute so the page rolls over to the next day's session at midnight.
    private val today = flow {
        while (true) {
            emit(LocalDate.now())
            delay(60_000L)
        }
    }.distinctUntilChanged()

    val day: StateFlow<ExerciseDay> = today.flatMapLatest { date ->
        val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        combine(
            dao.setsOn(date.toString()),
            dao.lastBefore(date.toString()),
            dao.activeDates(monday.minusWeeks(12).toString()),
            personal.completedWorkouts,
            db.guidedExercise().plans().map { rows -> rows.associate { it.id to it.payload } }.distinctUntilChanged(),
        ) { sets, last, active, workouts, plans ->
            val weekDays = (0L..6L).map { monday.plusDays(it) }
            ExerciseDay(
                date = date,
                session = RoutinePlans.sessionFor(date, plans),
                sets = sets.groupBy { it.exerciseId },
                lastLogs = last.associateBy { it.exerciseId },
                completed = date.toString() in workouts,
                weekDone = weekDays.filter { it.toString() in workouts || it.toString() in active }.toSet(),
                plans = plans,
                doneDates = (workouts.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() } + active.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() })
                    .filter { !it.isBefore(monday.minusWeeks(12)) }.toSet(),
                weekRest = weekDays.filter { RoutinePlans.sessionFor(it, plans).kind == SessionKind.REST }.toSet(),
                week = weekDays.map { it to RoutinePlans.sessionFor(it, plans) },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ExerciseDay())

    val bodyMetrics: StateFlow<List<BodyMetricRecord>> = personal.bodyMetrics.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val settings: StateFlow<ModuleSettings> = settingsRepository.moduleSettings.stateIn(viewModelScope, SharingStarted.Eagerly, ModuleSettings())

    fun logSet(exercise: PlanExercise, weightKg: Float, reps: Int) {
        val current = day.value
        val index = current.sets[exercise.id].orEmpty().size
        if (index >= exercise.sets) return
        viewModelScope.launch {
            dao.insert(SetLog(date = current.date.toString(), exerciseId = exercise.id, setIndex = index, weightKg = weightKg, reps = reps))
            // Logging the last set of the plan finishes the session on its own.
            if (dao.countOn(current.date.toString()) >= current.session.totalSets) {
                personal.setCheck(current.date.toString(), DayCheck.WORKOUT, true)
            }
        }
    }

    /** Removes [setIndex] and every later set of that exercise, so a mistaken tap can be undone. */
    fun undoSet(exercise: PlanExercise, setIndex: Int) {
        val date = day.value.date.toString()
        viewModelScope.launch {
            dao.deleteFrom(date, exercise.id, setIndex)
            personal.setCheck(date, DayCheck.WORKOUT, false)
        }
    }

    fun setCompleted(done: Boolean) {
        viewModelScope.launch { personal.setCheck(day.value.date.toString(), DayCheck.WORKOUT, done) }
    }

    /** Clears today's sets, workout completion check, and guided session state to test the routine flow from scratch. */
    fun resetRoutine() {
        val date = day.value.date
        viewModelScope.launch { GuidedExerciseRepository(db).reset(date) }
    }

    /** Saves [draft] for [date] only, or as the routine of its weekday from now on. */
    fun saveRoutine(date: LocalDate, draft: RoutineDraft, onlyThisDay: Boolean) {
        viewModelScope.launch {
            val repository = GuidedExerciseRepository(db)
            if (onlyThisDay) repository.saveDay(date, draft) else repository.saveWeekday(date, draft)
        }
    }

    /** Gives the week starting [toMonday] the same routines, day by day, as the week before it. */
    fun copyPreviousWeek(toMonday: LocalDate) {
        val plans = day.value.plans
        viewModelScope.launch {
            val repository = GuidedExerciseRepository(db)
            (0L..6L).forEach { i ->
                val from = toMonday.minusWeeks(1).plusDays(i)
                repository.saveDay(toMonday.plusDays(i), RoutinePlans.draftOf(RoutinePlans.sessionFor(from, plans)) { null })
            }
        }
    }

    fun addBodyMetric(type: String, value: Float) { viewModelScope.launch { personal.addBodyMetric(type, value) } }

    fun saveSettings(value: ModuleSettings) { viewModelScope.launch { settingsRepository.setModuleSettings(value) } }
}
