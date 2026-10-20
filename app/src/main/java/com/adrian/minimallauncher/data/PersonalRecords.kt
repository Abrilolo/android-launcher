package com.adrian.minimallauncher.data

data class NoteRecord(val id: String, val category: NoteCategory, val text: String, val createdAt: Long)

enum class NoteCategory(val label: String) {
    FINANCES("Finanzas"), DIET("Dieta"), GENERAL("General");

    fun next(): NoteCategory = entries[(ordinal + 1) % entries.size]
}

data class ReminderRecord(val id: String, val title: String, val hour: Int, val minute: Int, val spaceName: String)

data class FinanceRecord(val id: String, val amountCents: Long, val description: String, val createdAt: Long)

data class MealRecord(val id: String, val description: String, val createdAt: Long)

data class PersonalRecords(
    val notes: List<NoteRecord> = emptyList(),
    val reminders: List<ReminderRecord> = emptyList(),
    val finances: List<FinanceRecord> = emptyList(),
    val meals: List<MealRecord> = emptyList(),
)

data class PersonalRecordActions(
    val addNote: (NoteCategory, String) -> Unit,
    val deleteNote: (String) -> Unit,
    val addReminder: (String, Int, Int, String) -> Unit,
    val deleteReminder: (String) -> Unit,
    val addFinance: (Long, String) -> Unit,
    val deleteFinance: (String) -> Unit,
    val addMeal: (String) -> Unit,
    val deleteMeal: (String) -> Unit,
)
