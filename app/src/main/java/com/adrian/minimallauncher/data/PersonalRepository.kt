package com.adrian.minimallauncher.data

import androidx.room.withTransaction
import com.adrian.minimallauncher.feature.bank.Bank
import com.adrian.minimallauncher.feature.bank.BankAccount
import com.adrian.minimallauncher.feature.bank.BankKind
import com.adrian.minimallauncher.feature.bank.BankReceivable
import com.adrian.minimallauncher.feature.bank.applyMovement
import com.adrian.minimallauncher.feature.finance.Budget
import com.adrian.minimallauncher.feature.finance.BudgetLimit
import com.adrian.minimallauncher.feature.finance.BudgetPeriod
import kotlinx.coroutines.flow.map
import java.util.UUID

class PersonalRepository(private val db: LauncherDatabase) {
    private val dao = db.personal()
    private val budgetDao = db.budgets()
    private val bank = db.bank()

    val notes = dao.notes()
    val reminders = dao.reminders()
    /** Every money movement, incomes included. */
    val money = dao.finances()
    /** Day-to-day spending only (extras and shared bills), what the finance page has always listed. */
    val finances = dao.finances().map { rows -> rows.filter(Budget::isExtra) }
    val budgets = budgetDao.budgets()
    val bankAccounts = bank.accounts()
    val bankReceivables = bank.receivables()
    val meals = dao.meals()
    val bodyMetrics = dao.bodyMetrics()
    val completedWorkouts = dao.checks(DayCheck.WORKOUT).map { it.toSet() }
    val completedHabits = dao.checks(DayCheck.HABIT).map { it.toSet() }
    val completedRoutine = dao.checks(DayCheck.ROUTINE).map { it.toSet() }
    val completedReminders = dao.checks(DayCheck.REMINDER).map { it.toSet() }

    private fun newId() = UUID.randomUUID().toString()
    private fun now() = System.currentTimeMillis()

    suspend fun addNote(category: NoteCategory, text: String) = dao.insertNotes(listOf(NoteRecord(newId(), category, text, now())))
    suspend fun deleteNote(id: String) = dao.deleteNote(id)
    suspend fun setNotePinned(id: String, pinned: Boolean) = if (pinned) dao.pinOnly(id) else dao.unpin(id)
    suspend fun setNoteDone(id: String, done: Boolean) = dao.setNoteDone(id, done)
    suspend fun addReminder(title: String, hour: Int, minute: Int, phaseId: String): String {
        val id = newId()
        dao.insertReminders(listOf(ReminderRecord(id, title, hour, minute, phaseId)))
        return id
    }
    suspend fun upsertReminder(reminder: ReminderRecord) = dao.upsertReminder(reminder)
    suspend fun deleteReminder(id: String) = dao.deleteReminder(id)
    suspend fun addFinance(amountCents: Long, description: String) = dao.insertFinances(listOf(FinanceRecord(newId(), amountCents, description, now())))
    suspend fun deleteFinance(id: String) = db.withTransaction {
        dao.finance(id)?.let { bank.applyMovement(it, -1) }
        dao.deleteFinance(id)
    }

    /** Saves any money movement and returns its id (for linking a meal or a pending payment to it). */
    suspend fun addMoney(kind: MoneyKind, amountCents: Long, description: String, category: String = "", pendingFromOthersCents: Long = 0, mealId: String? = null, account: String = ""): String {
        val id = if (kind == MoneyKind.FIXED && category.startsWith("recurring:"))
            "$category:${java.time.YearMonth.now()}" else newId()
        val record = FinanceRecord(id, amountCents, description, now(), kind, category.trim().lowercase(), pendingFromOthersCents.coerceIn(0, amountCents), mealId, account)
        db.withTransaction {
            if (dao.finance(id) == null) {
                dao.insertFinances(listOf(record))
                bank.applyMovement(record, 1)
            }
        }
        return id
    }

    /** The others paid back [paidCents] of a shared bill; at 0 pending it is closed. */
    suspend fun settle(id: String, paidCents: Long) = db.withTransaction {
        val record = dao.finance(id) ?: return@withTransaction
        val paid = paidCents.coerceIn(0, record.pendingFromOthersCents)
        dao.upsertFinance(Budget.settled(record, paidCents))
        // The card was charged in full and still is; only what others owe it goes down.
        bank.allAccounts().firstOrNull { it.id == record.account && it.kind == BankKind.CARD }?.let { card ->
            bank.upsertAccount(card.copy(owedByOthersCents = (card.owedByOthersCents - paid).coerceAtLeast(0)))
        }
    }

    /** Saves an edit of a movement: same id, date and meal link. */
    suspend fun updateMoney(record: FinanceRecord) = db.withTransaction {
        dao.finance(record.id)?.let { bank.applyMovement(it, -1) }
        bank.applyMovement(record, 1)
        dao.upsertFinance(record)
    }

    /** Adds or edits a card or account (an edit keeps its id). */
    suspend fun saveAccount(account: BankAccount) {
        require((account.kind == BankKind.CASH) == (account.id == Bank.CASH_ACCOUNT_ID)) { "Cash uses the reserved wallet account" }
        bank.upsertAccount(account)
    }
    suspend fun deleteAccount(id: String) = db.withTransaction {
        require(dao.allFinances().none { it.account == id || (id == Bank.CASH_ACCOUNT_ID && it.kind == MoneyKind.WITHDRAWAL) }) { "Account has linked movements" }
        bank.deleteAccount(id)
    }
    /** Adds or edits money still to come in. */
    suspend fun saveReceivable(receivable: BankReceivable) = bank.upsertReceivable(receivable)
    suspend fun deleteReceivable(id: String) = bank.deleteReceivable(id)

    /** The money of a receivable arrived: it goes into the debit account (when there is one) and the receivable is closed. */
    suspend fun collectReceivable(receivable: BankReceivable) = db.withTransaction {
        bank.allAccounts().firstOrNull { it.kind == BankKind.DEBIT }?.let { debit ->
            bank.upsertAccount(debit.copy(balanceCents = debit.balanceCents + receivable.amountCents))
        }
        bank.deleteReceivable(receivable.id)
    }

    suspend fun linkMeal(financeId: String, mealId: String) {
        dao.finance(financeId)?.let { dao.upsertFinance(it.copy(mealId = mealId)) }
    }

    /** Sets a limit for a topic and period; 0 removes it. */
    suspend fun setBudget(topic: String, period: BudgetPeriod, limitCents: Long) {
        val key = topic.trim().lowercase().ifBlank { BudgetLimit.TOTAL }
        val id = BudgetLimit.idOf(key, period)
        if (limitCents <= 0) budgetDao.delete(id) else budgetDao.upsert(BudgetLimit(id, key, period, limitCents))
    }
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

    /** Clears all routine action and routine reminder checks so the entire routine flow can be tested again. */
    suspend fun clearRoutineChecks() {
        dao.deleteChecksByKind(DayCheck.ROUTINE)
        dao.deleteChecksByKind(DayCheck.REMINDER)
    }

    /** One-time copy of the records that used to live in DataStore; ids are kept, so running it twice is harmless. */
    /** Cards whose payment day has gone by: see [com.adrian.minimallauncher.feature.bank.Bank.rolled]. */
    suspend fun rollCardDues(today: java.time.LocalDate) {
        bank.allAccounts().forEach { account ->
            val rolled = com.adrian.minimallauncher.feature.bank.Bank.rolled(account, today)
            if (rolled != account) bank.upsertAccount(rolled)
        }
    }

    suspend fun importLegacy(records: PersonalRecords) = db.withTransaction {
        dao.insertNotes(records.notes)
        dao.insertReminders(records.reminders)
        dao.insertFinances(records.finances)
        dao.insertMeals(records.meals)
        dao.insertBodyMetrics(records.bodyMetrics)
        dao.insertChecks(records.completedWorkouts.map { DayCheck(it, DayCheck.WORKOUT) } + records.completedHabits.map { DayCheck(it, DayCheck.HABIT) })
    }
}
