package com.adrian.minimallauncher

import com.adrian.minimallauncher.data.FinanceRecord
import com.adrian.minimallauncher.data.MoneyKind
import com.adrian.minimallauncher.feature.finance.*
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class MonthlyBudgetAccountTest {
    private val today = LocalDate.of(2026, 11, 9)
    private val plan = FinancePlan(2000000, listOf(PlanCost("Rent", 600000, BudgetPeriod.MONTH)))
    private fun row(kind: MoneyKind, cents: Long, category: String = "Rent", date: LocalDate = today) =
        FinanceRecord("$kind-$cents", cents, category, date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(), kind, category)

    private fun account(vararg rows: FinanceRecord): MonthlyBudgetAccount {
        val summary = Budget.summary(rows.toList(), emptyList(), 10, 0, today, plan)
        return monthlyBudgetAccount(summary).also { assertEquals(summary.moneyLeftCents, it.remaining) }
    }

    @Test fun emptyMonthReservesFixedCostsAndSaving() {
        val result = account()
        assertEquals(600000L, result.reserved)
        assertEquals(140000L, result.saving)
        assertEquals(1260000L, result.remaining)
    }

    @Test fun partiallyPaidRentMovesFromReservedToSpent() {
        val result = account(row(MoneyKind.FIXED, 200000), row(MoneyKind.EXTRA, 10000, "food"), row(MoneyKind.MONTHLY, 50000, "purchase"))
        assertEquals(260000L, result.spent)
        assertEquals(400000L, result.reserved)
        assertEquals(1200000L, result.remaining)
    }

    @Test fun fixedOverrunAndUnplannedCostAreNotHidden() {
        val result = account(row(MoneyKind.FIXED, 700000), row(MoneyKind.FIXED, 100000, "Repair"))
        assertEquals(0L, result.reserved)
        assertEquals(1060000L, result.remaining)
    }

    @Test fun previousMonthAndOccasionalIncomeDoNotChangeThisBudget() {
        val result = account(row(MoneyKind.EXTRA, 100000, "old", today.minusMonths(1)), row(MoneyKind.GIFT, 50000))
        assertEquals(account(), result)
    }

    @Test fun deficitIsRetainedInsteadOfClamped() {
        assertEquals(-1740000L, account(row(MoneyKind.MONTHLY, 3000000)).remaining)
    }
}
