package com.adrian.minimallauncher.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.map
import java.util.UUID

class PersonalRepository(private val db: LauncherDatabase) {
    private val dao = db.personal()

    val notes = dao.notes()
    val reminders = dao.reminders()
    val finances = dao.finances()
    val meals = dao.meals()
    val bodyMetrics = dao.bodyMetrics()
    val completedWorkouts = dao.checks(DayCheck.WORKOUT).map { it.toSet() }
    val completedHabits = dao.checks(DayCheck.HABIT).map { it.toSet() }

    private fun newId() = UUID.randomUUID().toString()
    private fun now() = System.currentTimeMillis()

    suspend fun addNote(category: NoteCategory, text: String) = dao.insertNotes(listOf(NoteRecord(newId(), category, text, now())))
    suspend fun deleteNote(id: String) = dao.deleteNote(id)
    suspend fun setNotePinned(id: String, pinned: Boolean) = if (pinned) dao.pinOnly(id) else dao.unpin(id)
    suspend fun addReminder(title: String, hour: Int, minute: Int, spaceName: String) = dao.insertReminders(listOf(ReminderRecord(newId(), title, hour, minute, spaceName)))
    suspend fun deleteReminder(id: String) = dao.deleteReminder(id)
    suspend fun addFinance(amountCents: Long, description: String) = dao.insertFinances(listOf(FinanceRecord(newId(), amountCents, description, now())))
    suspend fun deleteFinance(id: String) = dao.deleteFinance(id)
    suspend fun addMeal(description: String) = dao.insertMeals(listOf(MealRecord(newId(), description, now())))
    suspend fun deleteMeal(id: String) = dao.deleteMeal(id)
    suspend fun addBodyMetric(type: String, value: Float) = dao.insertBodyMetrics(listOf(BodyMetricRecord(newId(), type, value, now())))
    suspend fun deleteBodyMetric(id: String) = dao.deleteBodyMetric(id)

    suspend fun toggleCheck(key: String, kind: String) {
        if (dao.hasCheck(key) > 0) dao.deleteCheck(key) else dao.insertChecks(listOf(DayCheck(key, kind)))
    }

    suspend fun setCheck(key: String, kind: String, done: Boolean) {
        if (done) dao.insertChecks(listOf(DayCheck(key, kind))) else dao.deleteCheck(key)
    }

    /** One-time copy of the records that used to live in DataStore; ids are kept, so running it twice is harmless. */
    suspend fun importLegacy(records: PersonalRecords) = db.withTransaction {
        dao.insertNotes(records.notes)
        dao.insertReminders(records.reminders)
        dao.insertFinances(records.finances)
        dao.insertMeals(records.meals)
        dao.insertBodyMetrics(records.bodyMetrics)
        dao.insertChecks(records.completedWorkouts.map { DayCheck(it, DayCheck.WORKOUT) } + records.completedHabits.map { DayCheck(it, DayCheck.HABIT) })
    }
}
