package com.adrian.minimallauncher.feature.finance

import com.adrian.minimallauncher.data.FinanceRecord
import com.adrian.minimallauncher.data.MoneyKind
import com.adrian.minimallauncher.feature.bank.Bank
import com.adrian.minimallauncher.feature.bank.BankAccount
import com.adrian.minimallauncher.feature.bank.BankKind
import java.time.LocalDate
import java.time.YearMonth

/**
 * Finanzas in three layers that never mix (concept: the "Finanzas: planeado vs. real" canvas, 2026-11-11):
 * - Real: what already happened. The money in debit and cash, each recorded movement, what the cards owe.
 * - Planeado: what has a date but has not happened. Card payments, scheduled payments, the expected income. It is not
 *   spending until its charge arrives (or it is marked done).
 * - Topes: how much the user wants to spend on something. A guide compared against what is available; never taken out
 *   of the income.
 * Dinero disponible = money in the accounts − what is still to pay this month.
 */
enum class PlanKind { CARD, SCHEDULED, INCOME }

enum class PlanState { PLANNED, TODAY, DONE, LATE }

/** One planned thing of the month: a card's payment, a scheduled payment or the income. */
data class PlanItem(
    val key: String,
    val name: String,
    val cents: Long,
    val due: LocalDate,
    val kind: PlanKind,
    val state: PlanState,
    val accountId: String = "",
    val payment: ScheduledPayment? = null,
) {
    val open: Boolean get() = state != PlanState.DONE
}

/** A spending cap and how it is going in its period. */
data class Tope(val topic: String, val period: BudgetPeriod, val limitCents: Long, val spentCents: Long, val monthCents: Long, val spentMonthCents: Long = 0) {
    val leftCents: Long get() = limitCents - spentCents
    /** What is still meant to go to it this month (its month share less what already went). */
    val leftMonthCents: Long get() = (monthCents - spentMonthCents).coerceAtLeast(0)
}

data class MoneyView(
    val today: LocalDate,
    /** Debit and cash accounts, and what they hold. */
    val accounts: List<BankAccount>,
    val haveCents: Long,
    /** This month's planned things, by date; done ones included. */
    val plan: List<PlanItem>,
    /** Planned things after this month (a card's next payment). */
    val later: List<PlanItem>,
    val toPayCents: Long,
    val incomingCents: Long,
    val spentTodayCents: Long,
    val spentMonthCents: Long,
    val topes: List<Tope>,
    /** Every card and what it owes in total. */
    val cards: List<BankAccount>,
) {
    val availableCents: Long get() = haveCents - toPayCents
    /** Days of the month left, today included. */
    val daysLeft: Int get() = today.lengthOfMonth() - today.dayOfMonth + 1
    val perDayCents: Long get() = if (availableCents <= 0) 0 else availableCents / daysLeft
    val topesMonthCents: Long get() = topes.sumOf { it.monthCents }
    /** What the caps still mean to spend this month. */
    val topesLeftCents: Long get() = topes.sumOf { it.leftMonthCents }
    /** Disponible plus income still to come, less what the caps still mean to spend: no destination yet. */
    val unassignedCents: Long get() = availableCents + incomingCents - topesLeftCents
    val owedCents: Long get() = cards.sumOf { it.balanceCents }
    val toPay: List<PlanItem> get() = plan.filter { it.open && it.kind != PlanKind.INCOME }
}

object FinanceMoney {
    /**
     * The scheduled payment a charge of [cents] on [date] most likely is: not paid this month, an amount within 10%
     * (or $20) and a day within a week. Null when none fits.
     */
    fun matchPlanned(plan: FinancePlan, records: List<FinanceRecord>, cents: Long, date: LocalDate): ScheduledPayment? {
        val month = YearMonth.from(date)
        return plan.payments
            .filter { !it.paid(records, date) }
            .filter { kotlin.math.abs(it.cents - cents) <= maxOf(it.cents / 10, 2000) }
            .filter { kotlin.math.abs(java.time.temporal.ChronoUnit.DAYS.between(date, month.atDay(it.day.coerceIn(1, month.lengthOfMonth())))) <= 7 }
            .minByOrNull { kotlin.math.abs(it.cents - cents) }
    }

    fun of(accounts: List<BankAccount>, plan: FinancePlan, summary: FinanceSummary, records: List<FinanceRecord>, today: LocalDate): MoneyView {
        val month = YearMonth.from(today)
        val end = month.atEndOfMonth()
        fun state(due: LocalDate, done: Boolean) = when {
            done -> PlanState.DONE
            due == today -> PlanState.TODAY
            due.isBefore(today) -> PlanState.LATE
            else -> PlanState.PLANNED
        }

        val scheduled = plan.payments.map { p ->
            val due = month.atDay(p.day.coerceIn(1, month.lengthOfMonth()))
            PlanItem("pay:${p.id}", p.name, p.cents, due, PlanKind.SCHEDULED, state(due, p.paid(records, today)), p.accountId, p)
        }
        val cards = accounts.filter { it.kind == BankKind.CARD }.map { Bank.rolled(it, today) }
        val cardNow = cards.mapNotNull { card ->
            val due = card.due ?: return@mapNotNull null
            if (card.dueCents <= 0) null else PlanItem("card:${card.id}", card.name, card.dueCents, due, PlanKind.CARD, state(due, false), card.id)
        }
        val cardNext = cards.mapNotNull { card ->
            val due = card.due ?: return@mapNotNull null
            val after = Bank.nextDate(card.payDay, due.plusDays(1)) ?: return@mapNotNull null
            if (card.nextDueCents <= 0) null else PlanItem("next:${card.id}", card.name, card.nextDueCents, after, PlanKind.CARD, PlanState.PLANNED, card.id)
        }
        val income = if (plan.incomeCents > 0 && plan.incomeDay in 1..31) {
            val due = month.atDay(plan.incomeDay.coerceAtMost(month.lengthOfMonth()))
            val came = records.any { it.kind == MoneyKind.INCOME_FIXED && YearMonth.from(Budget.dateOf(it)) == month }
            listOf(PlanItem("income", "Ingreso", plan.incomeCents, due, PlanKind.INCOME, state(due, came)))
        } else emptyList()

        val all = (scheduled + cardNow + cardNext + income)
        val thisMonth = all.filter { !it.due.isAfter(end) }.sortedBy { it.due }
        val later = all.filter { it.due.isAfter(end) }.sortedBy { it.due }
        val toPay = thisMonth.filter { it.open && it.kind != PlanKind.INCOME }.sumOf { it.cents }

        val have = accounts.filter { it.kind == BankKind.DEBIT || it.kind == BankKind.CASH }
        val expenses = records.filter { it.kind.isExpense }
        val days = today.lengthOfMonth()
        return MoneyView(
            today = today,
            accounts = have,
            haveCents = have.sumOf { it.balanceCents },
            plan = thisMonth,
            later = later,
            toPayCents = toPay,
            incomingCents = thisMonth.filter { it.open && it.kind == PlanKind.INCOME }.sumOf { it.cents },
            spentTodayCents = expenses.filter { Budget.dateOf(it) == today }.sumOf { it.amountCents },
            spentMonthCents = expenses.filter { YearMonth.from(Budget.dateOf(it)) == month }.sumOf { it.amountCents },
            topes = summary.topics.map { t ->
                val spentMonth = records.filter { Budget.isExtra(it) && Budget.categoryOf(it).equals(t.topic.trim(), ignoreCase = true) && YearMonth.from(Budget.dateOf(it)) == month }.sumOf { it.amountCents }
                Tope(t.topic, t.period, t.limitCents, t.spentCents, FinancePlan.perMonth(t.limitCents, t.period, days), spentMonth)
            },
            cards = cards,
        )
    }
}
