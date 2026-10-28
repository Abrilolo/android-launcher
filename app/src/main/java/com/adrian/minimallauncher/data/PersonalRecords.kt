package com.adrian.minimallauncher.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * At most one note is [pinned]; it is the one the home tile and the top of the notes page show.
 * A note is pending until it is [done]; the pending ones per aspect feed the notes widget.
 */
@Entity(tableName = "notes")
data class NoteRecord(
    @PrimaryKey val id: String,
    val category: NoteCategory,
    val text: String,
    val createdAt: Long,
    val pinned: Boolean = false,
    @ColumnInfo(defaultValue = "0") val done: Boolean = false,
)

/** Stored by name, so the order here is only the order of the filter chips. */
enum class NoteCategory(val label: String) {
    GENERAL("General"), WORK("Trabajo"), IDEAS("Ideas"), FINANCES("Finanzas"), DIET("Dieta"),
    SCHOOL("Escuela"), EXERCISE("Ejercicio"), FOOD("Comida");

    companion object {
        /** The aspects the quick notes and their widget count pending items for. */
        val ASPECTS = listOf(WORK, SCHOOL, EXERCISE, FOOD, DIET)
    }
}

/** A reminder at an hour that belongs to a routine phase: it shows and rings only while [phaseId] is the current phase. */
@Entity(tableName = "reminders")
data class ReminderRecord(@PrimaryKey val id: String, val title: String, val hour: Int, val minute: Int, val phaseId: String)

/**
 * What a money movement was. Stability income (salary, allowance) feeds the budget; transfers, gifts and
 * payments for an outside service come in but are not counted on. Records saved before version 3 are [EXTRA].
 */
enum class MoneyKind {
    INCOME_FIXED, TRANSFER, GIFT, SERVICE, FIXED, EXTRA, SHARED;

    val isIncome: Boolean get() = this == INCOME_FIXED || this == TRANSFER || this == GIFT || this == SERVICE
    val isExpense: Boolean get() = !isIncome
}

/**
 * One money movement; [amountCents] is always positive and [kind] says which way it went.
 * A [MoneyKind.SHARED] bill was paid in full by the user; [pendingFromOthersCents] is what the others still owe.
 * [mealId] links a food expense to the meal registered with it.
 */
@Entity(tableName = "finances")
data class FinanceRecord(
    @PrimaryKey val id: String,
    val amountCents: Long,
    val description: String,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "EXTRA") val kind: MoneyKind = MoneyKind.EXTRA,
    @ColumnInfo(defaultValue = "") val category: String = "",
    @ColumnInfo(defaultValue = "0") val pendingFromOthersCents: Long = 0,
    val mealId: String? = null,
)

/**
 * A meal eaten. It may come from a recipe ([recipeId], [servings]) or from ingredients (rows in `meal_items`);
 * the plate's totals are stored so history stays the same when a recipe or an ingredient is edited later.
 */
@Entity(tableName = "meals")
data class MealRecord(
    @PrimaryKey val id: String,
    val description: String,
    val createdAt: Long,
    val recipeId: String? = null,
    @ColumnInfo(defaultValue = "1") val servings: Float = 1f,
    @ColumnInfo(defaultValue = "0") val kcal: Float = 0f,
    @ColumnInfo(defaultValue = "0") val proteinG: Float = 0f,
    @ColumnInfo(defaultValue = "0") val carbsG: Float = 0f,
    @ColumnInfo(defaultValue = "0") val fatG: Float = 0f,
    @ColumnInfo(defaultValue = "0") val fiberG: Float = 0f,
)

@Entity(tableName = "body_metrics")
data class BodyMetricRecord(@PrimaryKey val id: String, val type: String, val value: Float, val createdAt: Long)

/** A yes/no mark for a day: a finished workout ("2026-10-21") or a habit ("2026-10-21|habit_walk"). */
@Entity(tableName = "day_checks")
data class DayCheck(@PrimaryKey val key: String, val kind: String) {
    companion object {
        const val WORKOUT = "workout"
        const val HABIT = "habit"
        /** A routine phase reminder done, "<date>|routine_<phase>_<dot>". */
        const val ROUTINE = "routine"
        /** A reminder done that day, "<date>|reminder_<id>". */
        const val REMINDER = "reminder"
    }
}

data class PersonalRecords(
    val notes: List<NoteRecord> = emptyList(),
    val reminders: List<ReminderRecord> = emptyList(),
    val finances: List<FinanceRecord> = emptyList(),
    val meals: List<MealRecord> = emptyList(),
    val bodyMetrics: List<BodyMetricRecord> = emptyList(),
    val completedWorkouts: Set<String> = emptySet(),
    val completedHabits: Set<String> = emptySet(),
)

data class PersonalRecordActions(
    val addNote: (NoteCategory, String) -> Unit,
    val deleteNote: (String) -> Unit,
    val setNotePinned: (String, Boolean) -> Unit = { _, _ -> },
    val addReminder: (String, Int, Int, String) -> Unit,
    val deleteReminder: (String) -> Unit,
    val addFinance: (Long, String) -> Unit,
    val deleteFinance: (String) -> Unit,
    val addMeal: (String) -> Unit,
    val deleteMeal: (String) -> Unit,
    val addBodyMetric: (String, Float) -> Unit = { _, _ -> },
    val deleteBodyMetric: (String) -> Unit = {},
    val toggleWorkout: (String) -> Unit = {},
    val toggleHabit: (String) -> Unit = {},
)
