package com.adrian.minimallauncher.feature.finance

import com.adrian.minimallauncher.data.FinanceRecord
import com.adrian.minimallauncher.data.MoneyKind
import java.time.LocalDate

/** How long a chart looks at: one day, a week or the month. */
enum class Span { DAY, WEEK, MONTH }

enum class SectionKind { FREE, TOPIC, FIXED, MONTHLY, SAVING }

/**
 * One slice of the budget for a span: what was set aside for it and what went out in it. The pie gives each
 * section its share of the total; the slice lights up as [spentCents] fills [budgetCents].
 */
data class BudgetSection(
    val key: String,
    val label: String,
    val kind: SectionKind,
    val budgetCents: Long,
    val spentCents: Long,
    val period: BudgetPeriod? = null,
) {
    val leftCents: Long get() = budgetCents - spentCents
    val over: Boolean get() = spentCents > budgetCents
}

object BudgetSections {
    /** Whether [record] went out of [section]: fixed costs by name, topics by category, the rest is free spending. */
    fun belongs(section: BudgetSection, record: FinanceRecord, topicNames: Set<String>): Boolean = when (section.kind) {
        SectionKind.FIXED -> record.kind == MoneyKind.FIXED && Budget.categoryOf(record).equals(section.key.trim(), ignoreCase = true)
        SectionKind.TOPIC -> Budget.isExtra(record) && Budget.categoryOf(record).equals(section.key.trim(), ignoreCase = true)
        SectionKind.FREE -> Budget.isExtra(record) && Budget.categoryOf(record) !in topicNames
        SectionKind.MONTHLY -> record.kind == MoneyKind.MONTHLY
        SectionKind.SAVING -> false
    }

    fun topicNames(summary: FinanceSummary): Set<String> = summary.topics.map { it.topic.lowercase() }.toSet()

    /**
     * The budget split for [span] around [day]: the free allowance, then each topic and fixed cost that is counted
     * in that span (a day shows only daily ones, a week daily and weekly ones, the month all of them, its own period
     * first) and, for the month, the saving. A day is today's allowance (it carries the week) or a seventh of the
     * week for other days; a week is the week allowance; the month is the bag.
     */
    fun of(summary: FinanceSummary, records: List<FinanceRecord>, span: Span, day: LocalDate): List<BudgetSection> {
        val daysInMonth = summary.date.lengthOfMonth()
        val (from, to) = when (span) {
            Span.DAY -> day to day
            Span.WEEK -> Budget.weekStart(day) to Budget.weekStart(day).plusDays(6)
            Span.MONTH -> day.withDayOfMonth(1) to day.withDayOfMonth(day.lengthOfMonth())
        }
        val inSpan = records.filter { val d = Budget.dateOf(it); !d.isBefore(from) && !d.isAfter(to) }
        val names = topicNames(summary)
        val isToday = span == Span.DAY && day == summary.date

        fun weekly(cents: Long, period: BudgetPeriod) = when (period) {
            BudgetPeriod.DAY -> cents * 7
            BudgetPeriod.WEEK -> cents
            BudgetPeriod.MONTH -> cents * 7 / daysInMonth
        }
        fun budget(cents: Long, period: BudgetPeriod, todayAllowance: Long) = when (span) {
            Span.DAY -> if (isToday) todayAllowance else weekly(cents, period) / 7
            Span.WEEK -> weekly(cents, period)
            Span.MONTH -> FinancePlan.perMonth(cents, period, daysInMonth)
        }
        fun spent(section: BudgetSection) = inSpan.filter { belongs(section, it, names) }.sumOf { it.amountCents }

        val topicsMonth = summary.topics.sumOf { FinancePlan.perMonth(it.limitCents, it.period, daysInMonth) }
        val freeBudget = when (span) {
            Span.DAY -> if (isToday) summary.todayAllowanceCents else summary.weekAllowanceCents / 7
            Span.WEEK -> summary.weekAllowanceCents
            Span.MONTH -> (summary.bagCents - topicsMonth).coerceAtLeast(0)
        }
        val sections = buildList {
            add(BudgetSection("libre", "Libre", SectionKind.FREE, freeBudget, 0))
            summary.topics.forEach { add(BudgetSection(it.topic, it.topic.replaceFirstChar { c -> c.uppercase() }, SectionKind.TOPIC, budget(it.limitCents, it.period, it.todayAllowanceCents), 0, it.period)) }
            summary.fixed.filter { shows(span, it.period) }.forEach {
                add(BudgetSection(it.topic, it.topic.replaceFirstChar { c -> c.uppercase() }, SectionKind.FIXED, budget(it.limitCents, it.period, it.todayAllowanceCents), 0, it.period))
            }
        }.map { it.copy(spentCents = spent(it)) }
            // Its own period first: weekly ones lead the week, monthly ones the month.
            .sortedBy { if (it.kind == SectionKind.FREE) 0 else if (it.period == own(span)) 1 else 2 }
        // One-off payments of the month are their own slice: what they took is what they cost, there is no budget for them.
        val monthly = inSpan.filter { it.kind == MoneyKind.MONTHLY }.sumOf { it.amountCents }
        val monthlySlice = if (span == Span.MONTH && monthly > 0)
            listOf(BudgetSection("mensual", "Del mes", SectionKind.MONTHLY, monthly, monthly))
        else emptyList()
        val saving = if (span == Span.MONTH && summary.savingCents > 0)
            listOf(BudgetSection("ahorro", "Ahorro", SectionKind.SAVING, summary.savingCents, summary.savedCents))
        else emptyList()
        return (sections + monthlySlice + saving).filter { it.budgetCents > 0 || it.spentCents > 0 }
    }

    /** Whether a budget counted every [period] belongs in a chart of [span]. */
    fun shows(span: Span, period: BudgetPeriod): Boolean = when (span) {
        Span.DAY -> period == BudgetPeriod.DAY
        Span.WEEK -> period != BudgetPeriod.MONTH
        Span.MONTH -> true
    }

    private fun own(span: Span) = when (span) {
        Span.DAY -> BudgetPeriod.DAY
        Span.WEEK -> BudgetPeriod.WEEK
        Span.MONTH -> BudgetPeriod.MONTH
    }

    /** The first and last day of [span] around [day]. */
    fun range(span: Span, day: LocalDate): Pair<LocalDate, LocalDate> = when (span) {
        Span.DAY -> day to day
        Span.WEEK -> Budget.weekStart(day) to Budget.weekStart(day).plusDays(6)
        Span.MONTH -> day.withDayOfMonth(1) to day.withDayOfMonth(day.lengthOfMonth())
    }

    /**
     * A few plain sentences about one section in a span: how it is going, whether spending runs ahead of the days
     * gone by, what is left per day, its share of the budget, and its biggest and number of movements. [records] are
     * the section's movements in the span; [gone] and [total] are the span's days gone (today included) and in all.
     */
    fun insights(section: BudgetSection, records: List<FinanceRecord>, budgetTotal: Long, gone: Int, total: Int): List<String> = buildList {
        val left = section.leftCents
        if (section.kind == SectionKind.SAVING) {
            add(if (section.spentCents >= section.budgetCents) "Ya juntaste el ahorro del mes." else "Llevas ${wholePesos(section.spentCents)} de ahorro a esta fecha.")
        } else {
            val used = if (section.budgetCents > 0) (section.spentCents * 100 / section.budgetCents).toInt() else 0
            add(
                when {
                    section.spentCents <= 0 -> "Aún no gastas aquí: tienes ${wholePesos(section.budgetCents)}."
                    section.over -> "Te pasaste por ${wholePesos(-left)}."
                    used >= 80 -> "Te queda poco: ${wholePesos(left)} ($used% usado)."
                    used >= 50 -> "Vas a la mitad: quedan ${wholePesos(left)}."
                    else -> "Vas bien: quedan ${wholePesos(left)}."
                },
            )
            if (total > 1 && gone in 1 until total && section.budgetCents > 0) {
                val timePct = gone * 100 / total
                when {
                    used > timePct + 10 -> add("Gastas más rápido que el tiempo: $used% gastado con $timePct% del periodo.")
                    used + 10 < timePct -> add("Vas por debajo del ritmo: $used% gastado con $timePct% del periodo.")
                }
                if (left > 0) add("Te tocan ${wholePesos(left / (total - gone + 1))} por día lo que resta.")
            }
            records.maxByOrNull { it.amountCents }?.let { big ->
                val name = big.description.ifBlank { big.category }.ifBlank { "Sin nombre" }
                add("Lo más grande: $name, ${wholePesos(big.amountCents)}.")
            }
            if (records.size > 1) add("${records.size} movimientos, ${wholePesos(section.spentCents / records.size)} en promedio.")
        }
        if (budgetTotal > 0) add("Es el ${(section.budgetCents * 100 + budgetTotal / 2) / budgetTotal}% del presupuesto.")
    }
}
