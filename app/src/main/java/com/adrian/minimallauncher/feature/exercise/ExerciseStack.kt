package com.adrian.minimallauncher.feature.exercise

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * One exercise of the user's stack, grouped by [muscleGroup] ("pecho", "espalda", "piernas"…).
 * [met] is its intensity (metabolic equivalent); a bout of [minutes] burns about MET × kg × hours kcal.
 * [placeholder] rows are the sample stack the launcher starts with until the user fills their own.
 */
@Entity(tableName = "stack_exercises")
data class StackExercise(
    @PrimaryKey val id: String,
    val name: String,
    val muscleGroup: String,
    val met: Float,
    val minutes: Int,
    val sortOrder: Int = 0,
    val placeholder: Boolean = false,
)

@Dao
interface StackDao {
    @Query("SELECT * FROM stack_exercises ORDER BY muscleGroup, sortOrder") fun stack(): Flow<List<StackExercise>>
    @Upsert suspend fun upsert(exercise: StackExercise)
    @Query("DELETE FROM stack_exercises WHERE id = :id") suspend fun delete(id: String)
    @Query("DELETE FROM stack_exercises WHERE placeholder = 1") suspend fun clearPlaceholders()
    @Query("SELECT COUNT(*) FROM stack_exercises") suspend fun count(): Int
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(rows: List<StackExercise>)
    @Query("SELECT * FROM stack_exercises") suspend fun all(): List<StackExercise>
}

/** What to add today: [exercise] for [minutes], burning about [kcal]. */
data class StackChoice(val exercise: StackExercise, val minutes: Int, val kcal: Float, val excessKcal: Float)

object ExerciseBurn {
    /** Default body weight for the estimate until the user records one. */
    const val DEFAULT_WEIGHT_KG = 70f

    /** kcal per minute = MET × 3.5 × kg ÷ 200 (ACSM). */
    fun perMinute(met: Float, weightKg: Float): Float = met * 3.5f * weightKg / 200f

    fun kcal(exercise: StackExercise, weightKg: Float, minutes: Int = exercise.minutes): Float = perMinute(exercise.met, weightKg) * minutes

    /** The muscle group a session of the plan works ("pecho", "piernas"…); cardio and rest days are "cardio". */
    fun groupOf(session: Session): String = when {
        session.kind != SessionKind.STRENGTH -> "cardio"
        session.title.startsWith("Piernas", ignoreCase = true) -> "piernas"
        else -> "pecho"
    }

    /**
     * When the day's kcal went [excessKcal] over the goal: the exercise of [group] whose usual bout burns at
     * least the excess with the least extra work; if none does, the strongest one for as many minutes as it
     * takes (at most [maxMinutes]). Null with no excess or nothing in that group.
     */
    fun pick(stack: List<StackExercise>, group: String, excessKcal: Float, weightKg: Float, maxMinutes: Int = 90): StackChoice? {
        if (excessKcal <= 0f) return null
        val options = stack.filter { it.muscleGroup.equals(group, ignoreCase = true) }.ifEmpty { return null }
        options.filter { kcal(it, weightKg) >= excessKcal }.minByOrNull { kcal(it, weightKg) }?.let {
            return StackChoice(it, it.minutes, kcal(it, weightKg), excessKcal)
        }
        val strongest = options.maxBy { it.met }
        val minutes = kotlin.math.ceil(excessKcal / perMinute(strongest.met, weightKg)).toInt().coerceIn(strongest.minutes, maxMinutes)
        return StackChoice(strongest, minutes, kcal(strongest, weightKg, minutes), excessKcal)
    }

    /** Sample stack (MET values from the Compendium of Physical Activities) until the user fills their own. */
    val PLACEHOLDER_STACK: List<StackExercise> = listOf(
        StackExercise("ph_pecho_flexiones", "Flexiones extra", "pecho", 3.8f, 10, 0, true),
        StackExercise("ph_pecho_circuito", "Circuito de pecho", "pecho", 5.0f, 15, 1, true),
        StackExercise("ph_espalda_remo", "Remo con mancuerna extra", "espalda", 3.5f, 10, 0, true),
        StackExercise("ph_espalda_circuito", "Circuito de espalda", "espalda", 5.0f, 15, 1, true),
        StackExercise("ph_piernas_sentadillas", "Sentadillas extra", "piernas", 5.0f, 10, 0, true),
        StackExercise("ph_piernas_zancadas", "Zancadas caminando", "piernas", 6.0f, 15, 1, true),
        StackExercise("ph_hombros_press", "Press de hombros extra", "hombros", 3.5f, 10, 0, true),
        StackExercise("ph_brazos_curl", "Curl y extensión extra", "brazos", 3.0f, 10, 0, true),
        StackExercise("ph_core_plancha", "Plancha y crunch", "core", 3.8f, 10, 0, true),
        StackExercise("ph_cardio_caminar", "Caminata rápida", "cardio", 4.3f, 20, 0, true),
        StackExercise("ph_cardio_bici", "Bici moderada", "cardio", 6.8f, 20, 1, true),
        StackExercise("ph_cardio_trote", "Trote", "cardio", 8.0f, 20, 2, true),
    )
}
