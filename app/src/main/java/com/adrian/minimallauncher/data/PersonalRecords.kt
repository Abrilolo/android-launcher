package com.adrian.minimallauncher.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** At most one note is [pinned]; it is the one the home tile and the top of the notes page show. */
@Entity(tableName = "notes")
data class NoteRecord(@PrimaryKey val id: String, val category: NoteCategory, val text: String, val createdAt: Long, val pinned: Boolean = false)

/** Stored by name, so the order here is only the order of the filter chips. */
enum class NoteCategory(val label: String) {
    GENERAL("General"), WORK("Trabajo"), IDEAS("Ideas"), FINANCES("Finanzas"), DIET("Dieta"),
}

@Entity(tableName = "reminders")
data class ReminderRecord(@PrimaryKey val id: String, val title: String, val hour: Int, val minute: Int, val spaceName: String)

@Entity(tableName = "finances")
data class FinanceRecord(@PrimaryKey val id: String, val amountCents: Long, val description: String, val createdAt: Long)

@Entity(tableName = "meals")
data class MealRecord(@PrimaryKey val id: String, val description: String, val createdAt: Long)

@Entity(tableName = "body_metrics")
data class BodyMetricRecord(@PrimaryKey val id: String, val type: String, val value: Float, val createdAt: Long)

/** A yes/no mark for a day: a finished workout ("2026-10-21") or a habit ("2026-10-21|habit_walk"). */
@Entity(tableName = "day_checks")
data class DayCheck(@PrimaryKey val key: String, val kind: String) {
    companion object {
        const val WORKOUT = "workout"
        const val HABIT = "habit"
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
