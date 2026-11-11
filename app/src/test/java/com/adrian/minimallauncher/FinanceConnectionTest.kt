package com.adrian.minimallauncher

import com.adrian.minimallauncher.data.FinanceRecord
import com.adrian.minimallauncher.data.MoneyKind
import com.adrian.minimallauncher.feature.finance.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class FinanceConnectionTest {
    private val today = LocalDate.of(2026, 11, 6)
    private fun expense(category: String, cents: Long, date: LocalDate = today, kind: MoneyKind = MoneyKind.EXTRA, account: String = "debit") =
        FinanceRecord("$category-$date", cents, category, date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), kind, category, account = account)
    private val food = BudgetLimit("food", "comida", BudgetPeriod.DAY, 10000)

    @Test fun linkedFoodPurchaseDoesNotShrinkFreeAllowanceOrDoubleCount() {
        val before = Budget.summary(emptyList(), listOf(food), 0, 0, today, moneyCents = 1000000)
        val after = Budget.summary(listOf(expense("comida", 5000)), listOf(food), 0, 0, today, moneyCents = 995000)
        assertEquals(before.fixedIncomeCents, after.fixedIncomeCents)
        assertEquals(before.todayLeftCents, after.todayLeftCents)
        assertEquals(before.topics.single().todayLeftCents - 5000, after.topics.single().todayLeftCents)
        assertEquals(before.moneyLeftCents - 5000, after.moneyLeftCents)
    }

    @Test fun plannedIncomeIsIndependentFromCurrentBankBalance() {
        val summary = Budget.summary(emptyList(), emptyList(), 0, 0, today, FinancePlan(2000000), moneyCents = 500000)
        assertEquals(2000000L, summary.fixedIncomeCents)
    }

    @Test fun monthlyFoodBudgetRemainsVisibleAndIsolatedToday() {
        val limit = food.copy(period = BudgetPeriod.MONTH, limitCents = 300000)
        val rows = listOf(expense(" Comida ", 5000))
        val summary = Budget.summary(rows, listOf(limit), 0, 0, today, FinancePlan(2000000))
        val sections = BudgetSections.of(summary, rows, Span.DAY, today)
        assertEquals(0L, summary.todaySpentCents)
        assertEquals(5000L, sections.single { it.kind == SectionKind.TOPIC }.spentCents)
        assertEquals(5000L, dailyFinance(summary, rows, today).spentCents)
    }

    @Test fun widgetRetainsLatestExpenseAfterMidnight() {
        val rows = listOf(expense("comida", 5000, today.minusDays(1)))
        val summary = Budget.summary(rows, listOf(food), 0, 0, today, FinancePlan(2000000))
        val widget = dailyFinance(summary, rows, today)
        assertEquals(0L, widget.spentCents)
        assertEquals(rows.single().id, widget.latest?.id)
        assertEquals(BudgetSections.of(summary, rows, Span.DAY, today).filter { it.kind == SectionKind.FREE || it.kind == SectionKind.TOPIC }.sumOf { it.leftCents }, widget.remainingCents)
    }

    @Test fun schedulingDoesNotCreateAnExpenseOrReduceBudget() {
        val payment = ScheduledPayment("music", "Music", 9900, "card", 10)
        val plan = FinancePlan(2000000, payments = listOf(payment))
        val summary = Budget.summary(emptyList(), emptyList(), 0, 0, today, plan)
        assertEquals(0L, summary.spentMonthCents)
        assertEquals(2000000L, summary.moneyLeftCents)
        assertEquals(LocalDate.of(2026, 11, 10), payment.due(emptyList(), today))
    }

    @Test fun paymentIsLinkedByIdAndOnlyAdvancesAfterActualCharge() {
        val payment = ScheduledPayment("music", "Music", 9900, "card", 31)
        val february = LocalDate.of(2027, 2, 28)
        assertEquals(february, payment.due(emptyList(), february))
        val charge = expense(payment.category, 9900, february, MoneyKind.FIXED, "card").copy(description = "Music")
        assertTrue(payment.paid(listOf(charge), february))
        assertEquals(LocalDate.of(2027, 3, 31), payment.due(listOf(charge), february))
        assertFalse(payment.paid(listOf(charge), LocalDate.of(2027, 3, 1)))
        assertEquals("music", Budget.categoryOf(charge))
    }

    @Test fun payingPlannedFixedCostUsesTheReserveOnlyOnce() {
        val plan = FinancePlan(2000000, listOf(PlanCost("Music", 9900, BudgetPeriod.MONTH)))
        val charge = expense("recurring:music", 9900, kind = MoneyKind.FIXED, account = "card").copy(description = "Music")
        val before = Budget.summary(emptyList(), emptyList(), 0, 0, today, plan)
        val after = Budget.summary(listOf(charge), emptyList(), 0, 0, today, plan)
        assertEquals(before.moneyLeftCents, after.moneyLeftCents)
        assertEquals(9900L, after.fixed.single().spentCents)
        assertEquals(9900L, after.spentMonthCents)
    }

    @Test fun unrelatedSubscriptionDoesNotConsumeTheRentReserve() {
        val plan = FinancePlan(2000000, listOf(PlanCost("Renta", 600000, BudgetPeriod.MONTH)))
        val music = expense("recurring:music", 9900, kind = MoneyKind.FIXED).copy(description = "Music")
        val after = Budget.summary(listOf(music), emptyList(), 0, 0, today, plan)
        assertEquals(2000000L - 600000L - 9900L, after.moneyLeftCents)
        assertEquals(0L, after.fixed.single().spentCents)
    }

    @Test fun largeFixedPaymentDoesNotConsumeDailyBudget() {
        val plan = FinancePlan(3000000, listOf(PlanCost("Renta", 1000000, BudgetPeriod.MONTH)))
        val before = Budget.summary(emptyList(), emptyList(), 0, 50000, today, plan)
        val record = expense("renta", 1000000, kind = MoneyKind.FIXED)
        val after = Budget.summary(listOf(record), emptyList(), 0, 50000, today, plan)
        assertEquals(before.todayLeftCents, after.todayLeftCents)
        assertEquals(0L, after.todaySpentCents)
        assertEquals(1000000L, after.spentMonthCents)
        assertEquals(before.moneyLeftCents, after.moneyLeftCents)
    }

    @Test fun monthlyPurchaseIsVisibleWithoutDailyPenalty() {
        val plan = FinancePlan(3000000)
        val before = Budget.summary(emptyList(), emptyList(), 0, 50000, today, plan)
        val record = expense("Laptop", 800000, kind = MoneyKind.MONTHLY)
        val after = Budget.summary(listOf(record), emptyList(), 0, 50000, today, plan)
        assertEquals(before.todayLeftCents, after.todayLeftCents)
        assertEquals(before.moneyLeftCents - 800000, after.moneyLeftCents)
        assertEquals(800000L, after.spentMonthCents)
    }
}
