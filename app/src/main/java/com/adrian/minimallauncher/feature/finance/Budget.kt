package com.adrian.minimallauncher.feature.finance

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import com.adrian.minimallauncher.data.FinanceRecord
import com.adrian.minimallauncher.data.MoneyKind
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.flow.Flow

enum class BudgetPeriod { DAY, WEEK, MONTH }

/**
 * A spending limit the user edits: [topic] is a finance category ("comida", "transporte"…) or [TOTAL] for
 * all extra spending. A day limit also sets the week and month it adds up to, unless those have their own row.
 */
@Entity(tableName = "budgets")
data class BudgetLimit(
    @PrimaryKey val id: String,
    val topic: String,
    val period: BudgetPeriod,
    val limitCents: Long,
) {
    companion object {
        const val TOTAL = "total"
        fun idOf(topic: String, period: BudgetPeriod) = "${topic}|${period.name}"
    }
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY topic, period") fun budgets(): Flow<List<BudgetLimit>>
    @Upsert suspend fun upsert(limit: BudgetLimit)
    @Query("DELETE FROM budgets WHERE id = :id") suspend fun delete(id: String)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(rows: List<BudgetLimit>)
    @Query("SELECT * FROM budgets") suspend fun all(): List<BudgetLimit>
}

/** How one limit is going: spent so far in its period, what is left, and today's share of it. */
data class TopicStatus(
    val topic: String,
    val period: BudgetPeriod,
    val limitCents: Long,
    val spentCents: Long,
    /** What can still be spent today; for day limits it carries the week's overspending (see [Budget.todayAllowance]). */
    val todayAllowanceCents: Long,
    val todaySpentCents: Long,
) {
    val leftCents: Long get() = limitCents - spentCents
    val todayLeftCents: Long get() = todayAllowanceCents - todaySpentCents
    val over: Boolean get() = spentCents > limitCents || todaySpentCents > todayAllowanceCents
}

/** Everything the finance views show, for one day. Amounts in cents. */
data class FinanceSummary(
    val date: LocalDate = LocalDate.now(),
    /** Stability income this month (salary, allowance). */
    val fixedIncomeCents: Long = 0,
    /** Transfers, gifts and outside service payments this month: received, but not budgeted on. */
    val otherIncomeCents: Long = 0,
    val transfersCents: Long = 0,
    val fixedCostsCents: Long = 0,
    val savingPct: Int = 0,
    val savingCents: Long = 0,
    /** The extras bag: (fixed income − fixed costs) minus savings. */
    val bagCents: Long = 0,
    val bagLeftCents: Long = 0,
    val extrasMonthCents: Long = 0,
    val weekAllowanceCents: Long = 0,
    val weekSpentCents: Long = 0,
    val todayAllowanceCents: Long = 0,
    val todaySpentCents: Long = 0,
    /** Money others still owe from shared bills. */
    val pendingFromOthersCents: Long = 0,
    val sharedOpen: List<FinanceRecord> = emptyList(),
    val topics: List<TopicStatus> = emptyList(),
) {
    val todayLeftCents: Long get() = todayAllowanceCents - todaySpentCents
    val weekLeftCents: Long get() = weekAllowanceCents - weekSpentCents
}

/**
 * The budget rules from the plan: stability income minus fixed costs is split into savings and the extras bag;
 * the bag gives a week allowance, and each day may spend what is left of the week divided by the days left,
 * so spending too much today lowers tomorrow.
 */
object Budget {
    private val zone: ZoneId get() = ZoneId.systemDefault()
    fun dateOf(record: FinanceRecord): LocalDate = Instant.ofEpochMilli(record.createdAt).atZone(zone).toLocalDate()
    fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    /** Days from [date] to Sunday, [date] included. */
    fun daysLeftInWeek(date: LocalDate): Int = 8 - date.dayOfWeek.value

    /** Day-to-day spending: extras and shared bills (what the user is out of pocket). */
    fun isExtra(record: FinanceRecord) = record.kind == MoneyKind.EXTRA || record.kind == MoneyKind.SHARED

    /** (week − spent this week before today) ÷ days left in the week, never below zero. */
    fun todayAllowance(weekCents: Long, spentBeforeToday: Long, today: LocalDate): Long =
        ((weekCents - spentBeforeToday) / daysLeftInWeek(today)).coerceAtLeast(0)

    fun summary(records: List<FinanceRecord>, limits: List<BudgetLimit>, savingPct: Int, legacyDailyCents: Long, today: LocalDate): FinanceSummary {
        val month = YearMonth.from(today)
        val inMonth = records.filter { YearMonth.from(dateOf(it)) == month }
        fun sum(rows: List<FinanceRecord>, vararg kinds: MoneyKind) = rows.filter { it.kind in kinds }.sumOf { it.amountCents }
        val fixedIncome = sum(inMonth, MoneyKind.INCOME_FIXED)
        val fixedCosts = sum(inMonth, MoneyKind.FIXED)
        val free = (fixedIncome - fixedCosts).coerceAtLeast(0)
        val pct = savingPct.coerceIn(0, 100)
        val saving = free * pct / 100
        val bag = free - saving

        val extras = records.filter(::isExtra)
        val weekFrom = weekStart(today)
        val extrasMonth = extras.filter { YearMonth.from(dateOf(it)) == month }.sumOf { it.amountCents }
        val weekBefore = extras.filter { val d = dateOf(it); !d.isBefore(weekFrom) && d.isBefore(today) }.sumOf { it.amountCents }
        val todaySpent = extras.filter { dateOf(it) == today }.sumOf { it.amountCents }

        val total = limits.filter { it.topic == BudgetLimit.TOTAL }.associateBy { it.period }
        // The week comes from its own limit, else seven days of the day limit, else the bag spread over the month.
        val dayLimit = total[BudgetPeriod.DAY]?.limitCents ?: legacyDailyCents.takeIf { it > 0 }
        val week = total[BudgetPeriod.WEEK]?.limitCents ?: dayLimit?.times(7) ?: (bag * 7 / today.lengthOfMonth())

        val topics = limits.filter { it.topic != BudgetLimit.TOTAL }.map { limit -> topicStatus(limit, extras, today) }
        val shared = records.filter { it.kind == MoneyKind.SHARED && it.pendingFromOthersCents > 0 }
        return FinanceSummary(
            date = today,
            fixedIncomeCents = fixedIncome,
            otherIncomeCents = sum(inMonth, MoneyKind.TRANSFER, MoneyKind.GIFT, MoneyKind.SERVICE),
            transfersCents = sum(inMonth, MoneyKind.TRANSFER),
            fixedCostsCents = fixedCosts,
            savingPct = pct,
            savingCents = saving,
            bagCents = total[BudgetPeriod.MONTH]?.limitCents ?: bag,
            bagLeftCents = (total[BudgetPeriod.MONTH]?.limitCents ?: bag) - extrasMonth,
            extrasMonthCents = extrasMonth,
            weekAllowanceCents = week,
            weekSpentCents = weekBefore + todaySpent,
            todayAllowanceCents = todayAllowance(week, weekBefore, today),
            todaySpentCents = todaySpent,
            pendingFromOthersCents = shared.sumOf { it.pendingFromOthersCents },
            sharedOpen = shared,
            topics = topics,
        )
    }

    private fun topicStatus(limit: BudgetLimit, extras: List<FinanceRecord>, today: LocalDate): TopicStatus {
        val mine = extras.filter { it.category.equals(limit.topic, ignoreCase = true) }
        val todaySpent = mine.filter { dateOf(it) == today }.sumOf { it.amountCents }
        val weekFrom = weekStart(today)
        val weekBefore = mine.filter { val d = dateOf(it); !d.isBefore(weekFrom) && d.isBefore(today) }.sumOf { it.amountCents }
        return when (limit.period) {
            BudgetPeriod.DAY -> TopicStatus(limit.topic, limit.period, limit.limitCents, todaySpent, todayAllowance(limit.limitCents * 7, weekBefore, today), todaySpent)
            BudgetPeriod.WEEK -> TopicStatus(limit.topic, limit.period, limit.limitCents, weekBefore + todaySpent, todayAllowance(limit.limitCents, weekBefore, today), todaySpent)
            BudgetPeriod.MONTH -> {
                val month = YearMonth.from(today)
                val before = mine.filter { val d = dateOf(it); YearMonth.from(d) == month && d.isBefore(today) }.sumOf { it.amountCents }
                val daysLeft = month.lengthOfMonth() - today.dayOfMonth + 1
                TopicStatus(limit.topic, limit.period, limit.limitCents, before + todaySpent, ((limit.limitCents - before) / daysLeft).coerceAtLeast(0), todaySpent)
            }
        }
    }

    /**
     * Whether a new expense of [amountCents] in [category] goes over what is left today (its topic's limit, or
     * the total): a meal bought outside the food budget is flagged so the UI can mark it as an extra treat.
     */
    fun wouldExceed(summary: FinanceSummary, category: String, amountCents: Long): Boolean {
        val topic = summary.topics.firstOrNull { it.topic.equals(category, ignoreCase = true) }
        val left = topic?.todayLeftCents ?: summary.todayLeftCents
        return amountCents > left
    }

    /** A shared bill after the others pay [paidCents]: both what is owed and what the user is out of pocket go down. */
    fun settled(record: FinanceRecord, paidCents: Long): FinanceRecord {
        val paid = paidCents.coerceIn(0, record.pendingFromOthersCents)
        return record.copy(pendingFromOthersCents = record.pendingFromOthersCents - paid, amountCents = record.amountCents - paid)
    }
}
