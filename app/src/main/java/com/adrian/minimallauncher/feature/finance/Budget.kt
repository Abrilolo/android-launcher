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
import org.json.JSONArray
import org.json.JSONObject

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

/** One planned fixed cost and how often it is paid. */
data class PlanCost(val name: String, val cents: Long, val period: BudgetPeriod)

/**
 * The real plan from Ajustes: the fixed monthly income (a floor under what was recorded this month) and the fixed
 * costs (they replace the recorded ones for sizing savings and the extras bag). Stored as JSON in the settings.
 */
data class FinancePlan(
    val incomeCents: Long = 0,
    val costs: List<PlanCost> = emptyList(),
    val payments: List<ScheduledPayment> = emptyList(),
    /** Day of the month the income is expected; 0 when not set. */
    val incomeDay: Int = 0,
) {
    fun costsPerMonth(daysInMonth: Int): Long = costs.sumOf { perMonth(it.cents, it.period, daysInMonth) }

    fun encode(): String = JSONObject()
        .put("income", incomeCents)
        .put("incomeDay", incomeDay)
        .put("costs", JSONArray(costs.map { JSONObject().put("name", it.name).put("cents", it.cents).put("period", it.period.name) }))
        .put("payments", JSONArray(payments.map { it.json() }))
        .toString()

    companion object {
        fun perMonth(cents: Long, period: BudgetPeriod, daysInMonth: Int): Long = when (period) {
            BudgetPeriod.DAY -> cents * daysInMonth
            BudgetPeriod.WEEK -> cents * daysInMonth / 7
            BudgetPeriod.MONTH -> cents
        }

        fun decode(text: String): FinancePlan = if (text.isBlank()) FinancePlan() else runCatching {
            val json = JSONObject(text)
            val list = json.optJSONArray("costs") ?: JSONArray()
            FinancePlan(
                json.optLong("income"),
                (0 until list.length()).map { i ->
                    val c = list.getJSONObject(i)
                    PlanCost(c.optString("name"), c.optLong("cents"), runCatching { BudgetPeriod.valueOf(c.optString("period")) }.getOrDefault(BudgetPeriod.MONTH))
                },
                ScheduledPayment.decode(json.optJSONArray("payments")),
                json.optInt("incomeDay"),
            )
        }.getOrDefault(FinancePlan())
    }
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
    /** The whole period's money (a day limit counts its week) and what went in it so far, today included. */
    val periodTotalCents: Long = limitCents,
    val periodSpentCents: Long = spentCents,
    /** Days of the period still to come after today. */
    val daysAfterToday: Int = 0,
) {
    val leftCents: Long get() = limitCents - spentCents
    val todayLeftCents: Long get() = todayAllowanceCents - todaySpentCents
    val over: Boolean get() = spentCents > limitCents || todaySpentCents > todayAllowanceCents
}

/** One kind of spending (fijos, extras, compartidos) and its parts by concept, biggest first. Amounts in cents. */
data class SpendGroup(val kind: MoneyKind, val cents: Long, val parts: List<Pair<String, Long>>)

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
    /** One-off payments of the month (a card bill, a subscription): they leave what is left of the month, not the day or week. */
    val monthlyCents: Long = 0,
    val weekAllowanceCents: Long = 0,
    val weekSpentCents: Long = 0,
    val todayAllowanceCents: Long = 0,
    val todaySpentCents: Long = 0,
    /** Today's plain share of the week (week ÷ 7): today's allowance minus this is what earlier days left over. */
    val dayShareCents: Long = 0,
    /** This month's saving goal so far: what the pace of the month has set aside, less what extras ate of it. */
    val savedCents: Long = 0,
    /** Where the saving should be by today if extras keep to their pace. */
    val savingPaceCents: Long = 0,
    /** Everything that went out this month: extras, shared bills and fixed costs paid. */
    val spentMonthCents: Long = 0,
    /**
     * What is left of this month's income once the saving, the fixed costs (planned, or paid when more) and the
     * extras are taken out.
     */
    val moneyLeftCents: Long = 0,
    /** Money others still owe from shared bills. */
    val pendingFromOthersCents: Long = 0,
    val sharedOpen: List<FinanceRecord> = emptyList(),
    val topics: List<TopicStatus> = emptyList(),
    /** One line per fixed cost of the plan, followed like a topic: what it took this period and today's share. */
    val fixed: List<TopicStatus> = emptyList(),
    /** Fixed costs already paid plus the unpaid part of planned reserves. */
    val fixedCommittedCents: Long = 0,
    val fixedPaidCents: Long = 0,
) {
    val todayLeftCents: Long get() = todayAllowanceCents - todaySpentCents
    val weekLeftCents: Long get() = weekAllowanceCents - weekSpentCents
    /** Positive when earlier days of the week spent less than their share, negative when they spent more. */
    val carriedCents: Long get() = todayAllowanceCents - dayShareCents
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

    fun categoryOf(record: FinanceRecord): String =
        (if (record.category.startsWith("recurring:")) record.description else record.category).trim().lowercase(java.util.Locale.ROOT)

    /** (week − spent this week before today) ÷ days left in the week, never below zero. */
    fun todayAllowance(weekCents: Long, spentBeforeToday: Long, today: LocalDate): Long =
        ((weekCents - spentBeforeToday) / daysLeftInWeek(today)).coerceAtLeast(0)

    /** [moneyCents] is what the user has in debit now: when given, it is the income the budget works from. */
    fun summary(records: List<FinanceRecord>, limits: List<BudgetLimit>, savingPct: Int, legacyDailyCents: Long, today: LocalDate, plan: FinancePlan = FinancePlan(), moneyCents: Long? = null): FinanceSummary {
        val month = YearMonth.from(today)
        val inMonth = records.filter { YearMonth.from(dateOf(it)) == month }
        fun sum(rows: List<FinanceRecord>, vararg kinds: MoneyKind) = rows.filter { it.kind in kinds }.sumOf { it.amountCents }
        // The income that counts is what came in this month (bank notices and manual entries); the fixed income
        // set in Ajustes is its floor, for a salary that has not arrived or is never detected.
        // Reconstruct the pre-spending bank basis only when no income budget is configured.
        // A linked debit/card movement already changed the bank balance; never deduct it twice.
        val linkedOut = inMonth.filter { it.kind.isExpense && it.account.isNotBlank() }
            .sumOf { it.amountCents - if (it.kind == MoneyKind.SHARED) it.pendingFromOthersCents else 0L }
        val incomeBudget = maxOf(plan.incomeCents, sum(inMonth, MoneyKind.INCOME_FIXED))
        val fixedIncome = if (incomeBudget > 0) incomeBudget else moneyCents?.plus(linkedOut) ?: 0L
        val fixedCosts = if (plan.costs.isNotEmpty()) plan.costsPerMonth(today.lengthOfMonth()) else sum(inMonth, MoneyKind.FIXED)
        val fixedPaid = inMonth.filter { it.kind == MoneyKind.FIXED }.groupBy(::categoryOf).mapValues { (_, rows) -> rows.sumOf { it.amountCents } }
        val fixedPlanned = plan.costs.groupBy { it.name.trim().lowercase(java.util.Locale.ROOT) }
            .mapValues { (_, costs) -> costs.sumOf { FinancePlan.perMonth(it.cents, it.period, today.lengthOfMonth()) } }
        val fixedCommitted = (fixedPaid.keys + fixedPlanned.keys).sumOf { name -> maxOf(fixedPaid[name] ?: 0L, fixedPlanned[name] ?: 0L) }
        val free = (fixedIncome - fixedCosts).coerceAtLeast(0)
        val pct = savingPct.coerceIn(0, 100)
        val saving = free * pct / 100
        val bag = free - saving

        val extras = records.filter(::isExtra)
        val weekFrom = weekStart(today)
        val days = today.lengthOfMonth()
        val extrasMonth = extras.filter { YearMonth.from(dateOf(it)) == month }.sumOf { it.amountCents }
        val monthly = sum(inMonth, MoneyKind.MONTHLY)
        val topicLimits = limits.filter { it.topic != BudgetLimit.TOTAL && it.limitCents > 0 }
            .groupBy { it.topic.trim().lowercase(java.util.Locale.ROOT) }
            .values.map { rows -> rows.minBy { it.period.ordinal }.let { it.copy(topic = it.topic.trim()) } }
        // A spending with its own budget (comida…) is taken from that budget only, never from the free allowance.
        val topicNames = topicLimits.map { it.topic.lowercase() }.toSet()
        val freeSpending = extras.filter { categoryOf(it) !in topicNames }
        val weekBefore = freeSpending.filter { val d = dateOf(it); !d.isBefore(weekFrom) && d.isBefore(today) }.sumOf { it.amountCents }
        val todaySpent = freeSpending.filter { dateOf(it) == today }.sumOf { it.amountCents }

        val total = limits.filter { it.topic == BudgetLimit.TOTAL }.associateBy { it.period }
        // The week comes from its own limit, else seven days of the day limit, else the bag spread over the month,
        // less what the topic budgets already hold of it.
        val dayLimit = total[BudgetPeriod.DAY]?.limitCents ?: legacyDailyCents.takeIf { it > 0 }
        val topicsMonth = topicLimits.sumOf { FinancePlan.perMonth(it.limitCents, it.period, days) }
        val week = total[BudgetPeriod.WEEK]?.limitCents ?: dayLimit?.times(7) ?: ((bag - topicsMonth).coerceAtLeast(0) * 7 / days)

        // Saving builds up with the month; extras running ahead of the bag's pace eat into it.
        val savingPace = saving * today.dayOfMonth / days
        val overPace = (extrasMonth - bag * today.dayOfMonth / days).coerceAtLeast(0)
        val saved = (savingPace - overPace).coerceIn(0, saving)

        val topics = topicLimits.map { limit -> topicStatus(limit, extras, today) }
        // A fixed cost is recorded as FIXED with its name as the category.
        val fixedRecords = records.filter { it.kind == MoneyKind.FIXED }
        val fixedLines = plan.costs.map { cost -> topicStatus(BudgetLimit("fixed|${cost.name}", cost.name, cost.period, cost.cents), fixedRecords, today) }
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
            monthlyCents = monthly,
            weekAllowanceCents = week,
            weekSpentCents = weekBefore + todaySpent,
            todayAllowanceCents = todayAllowance(week, weekBefore, today),
            todaySpentCents = todaySpent,
            dayShareCents = week / 7,
            savedCents = saved,
            savingPaceCents = savingPace,
            spentMonthCents = extrasMonth + sum(inMonth, MoneyKind.FIXED) + monthly,
            moneyLeftCents = fixedIncome - saving - fixedCommitted - extrasMonth - monthly,
            pendingFromOthersCents = shared.sumOf { it.pendingFromOthersCents },
            sharedOpen = shared,
            topics = topics,
            fixed = fixedLines,
            fixedCommittedCents = fixedCommitted,
            fixedPaidCents = sum(inMonth, MoneyKind.FIXED),
        )
    }

    private fun topicStatus(limit: BudgetLimit, extras: List<FinanceRecord>, today: LocalDate): TopicStatus {
        val mine = extras.filter { categoryOf(it).equals(limit.topic.trim(), ignoreCase = true) }
        val todaySpent = mine.filter { dateOf(it) == today }.sumOf { it.amountCents }
        val weekFrom = weekStart(today)
        val weekBefore = mine.filter { val d = dateOf(it); !d.isBefore(weekFrom) && d.isBefore(today) }.sumOf { it.amountCents }
        val weekAfter = daysLeftInWeek(today) - 1
        return when (limit.period) {
            BudgetPeriod.DAY -> TopicStatus(
                limit.topic, limit.period, limit.limitCents, todaySpent, todayAllowance(limit.limitCents * 7, weekBefore, today), todaySpent,
                limit.limitCents * 7, weekBefore + todaySpent, weekAfter,
            )
            BudgetPeriod.WEEK -> TopicStatus(
                limit.topic, limit.period, limit.limitCents, weekBefore + todaySpent, todayAllowance(limit.limitCents, weekBefore, today), todaySpent,
                limit.limitCents, weekBefore + todaySpent, weekAfter,
            )
            BudgetPeriod.MONTH -> {
                val month = YearMonth.from(today)
                val before = mine.filter { val d = dateOf(it); YearMonth.from(d) == month && d.isBefore(today) }.sumOf { it.amountCents }
                val daysLeft = month.lengthOfMonth() - today.dayOfMonth + 1
                TopicStatus(
                    limit.topic, limit.period, limit.limitCents, before + todaySpent, ((limit.limitCents - before) / daysLeft).coerceAtLeast(0), todaySpent,
                    limit.limitCents, before + todaySpent, daysLeft - 1,
                )
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

    /**
     * What is left of a fixed cost after paying [extraCents] more: today's share for a daily or weekly cost (it
     * already carries this week's over- or underspending), the rest of the month for a monthly one.
     */
    fun fixedLeft(status: TopicStatus, extraCents: Long = 0): Long =
        if (status.period == BudgetPeriod.MONTH) status.periodTotalCents - status.periodSpentCents - extraCents
        else status.todayLeftCents - extraCents

    /**
     * The new share for each day still to come in a daily or weekly fixed cost once [extraCents] is paid: paying
     * more today lowers the days after it, paying less raises them. Null for monthly costs or the period's last day.
     */
    fun nextDaysShare(status: TopicStatus, extraCents: Long = 0): Long? =
        if (status.period == BudgetPeriod.MONTH || status.daysAfterToday <= 0) null
        else ((status.periodTotalCents - status.periodSpentCents - extraCents) / status.daysAfterToday).coerceAtLeast(0)

    /**
     * What went out in [records], split by kind (fixed costs, extras, shared bills) and then by concept: the
     * category, else the description. Groups and parts come biggest first; empty ones are left out.
     */
    fun breakdown(records: List<FinanceRecord>): List<SpendGroup> =
        listOf(MoneyKind.FIXED, MoneyKind.EXTRA, MoneyKind.SHARED, MoneyKind.MONTHLY).mapNotNull { kind ->
            val rows = records.filter { it.kind == kind && it.amountCents > 0 }
            if (rows.isEmpty()) return@mapNotNull null
            val parts = rows.groupBy { concept(it) }.map { (name, list) -> name to list.sumOf { it.amountCents } }.sortedByDescending { it.second }
            SpendGroup(kind, parts.sumOf { it.second }, parts)
        }.sortedByDescending { it.cents }

    private fun concept(record: FinanceRecord): String =
        categoryOf(record).ifBlank { record.description.trim() }.ifBlank { if (record.kind == MoneyKind.FIXED) "fijo" else "otro" }.lowercase()

    /** A shared bill after the others pay [paidCents]: both what is owed and what the user is out of pocket go down. */
    fun settled(record: FinanceRecord, paidCents: Long): FinanceRecord {
        val paid = paidCents.coerceIn(0, record.pendingFromOthersCents)
        return record.copy(pendingFromOthersCents = record.pendingFromOthersCents - paid, amountCents = record.amountCents - paid)
    }
}
