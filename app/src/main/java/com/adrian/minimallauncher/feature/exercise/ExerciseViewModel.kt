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
            dao.activeDates(monday.toString()),
            personal.completedWorkouts,
        ) { sets, last, active, workouts ->
            val weekDays = (0L..6L).map { monday.plusDays(it) }
            ExerciseDay(
                date = date,
                session = TokyoGuide.sessionFor(date),
                sets = sets.groupBy { it.exerciseId },
                lastLogs = last.associateBy { it.exerciseId },
                completed = date.toString() in workouts,
                weekDone = weekDays.filter { it.toString() in workouts || it.toString() in active }.toSet(),
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

    fun addBodyMetric(type: String, value: Float) { viewModelScope.launch { personal.addBodyMetric(type, value) } }

    fun saveSettings(value: ModuleSettings) { viewModelScope.launch { settingsRepository.setModuleSettings(value) } }
}
