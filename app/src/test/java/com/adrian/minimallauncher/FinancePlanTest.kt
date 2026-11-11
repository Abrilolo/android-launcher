package com.adrian.minimallauncher

import com.adrian.minimallauncher.feature.finance.Budget
import com.adrian.minimallauncher.feature.finance.BudgetPeriod
import com.adrian.minimallauncher.feature.finance.FinancePlan
import com.adrian.minimallauncher.feature.finance.PlanCost
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class FinancePlanTest {
    private val november = LocalDate.of(2026, 11, 4) // 30 days

    @Test fun costsAddUpPerMonth() {
        val plan = FinancePlan(0, listOf(PlanCost("Renta", 800_000, BudgetPeriod.MONTH), PlanCost("Metro", 3_000, BudgetPeriod.DAY), PlanCost("Súper", 70_000, BudgetPeriod.WEEK)))
        // 8,000 + 30 × 30 + 700 × 30 / 7 = 8,000 + 900 + 3,000
        assertEquals(1_190_000, plan.costsPerMonth(30))
    }

    @Test fun planSizesTheBag() {
        val plan = FinancePlan(2_000_000, listOf(PlanCost("Renta", 1_000_000, BudgetPeriod.MONTH)))
        val summary = Budget.summary(emptyList(), emptyList(), 10, 0, november, plan)
        assertEquals(2_000_000, summary.fixedIncomeCents)
        assertEquals(1_000_000, summary.fixedCostsCents)
        assertEquals(100_000, summary.savingCents)
        assertEquals(900_000, summary.bagCents)
    }

    private fun fixedPaid(name: String, cents: Long, date: LocalDate) = com.adrian.minimallauncher.data.FinanceRecord(
        "$name$date$cents", cents, name, date.atTime(12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
        com.adrian.minimallauncher.data.MoneyKind.FIXED, name,
    )

    @Test fun payingMoreOnADailyFixedCostLowersTheComingDays() {
        // Wednesday; Metro is $30 a day, so $210 for the week. Monday and Tuesday took $30 each.
        val plan = FinancePlan(2_000_000, listOf(PlanCost("Metro", 3_000, BudgetPeriod.DAY)))
        val records = listOf(fixedPaid("Metro", 3_000, november.minusDays(2)), fixedPaid("Metro", 3_000, november.minusDays(1)))
        val metro = Budget.summary(records, emptyList(), 10, 0, november, plan).fixed.single()
        assertEquals(3_000, Budget.fixedLeft(metro))
        // Paying $60 today: $30 past today's share, and the 4 days left share $90 → $22.50 each.
        assertEquals(-3_000, Budget.fixedLeft(metro, 6_000))
        assertEquals(2_250L, Budget.nextDaysShare(metro, 6_000))
    }

    @Test fun monthlyFixedCostShowsWhatIsLeftOfTheMonth() {
        val plan = FinancePlan(2_000_000, listOf(PlanCost("Renta", 800_000, BudgetPeriod.MONTH)))
        val rent = Budget.summary(listOf(fixedPaid("Renta", 800_000, november)), emptyList(), 10, 0, november, plan).fixed.single()
        assertEquals(0, Budget.fixedLeft(rent))
        assertEquals(null, Budget.nextDaysShare(rent))
    }

    @Test fun detectedIncomeCountsAndThePlanIsAFloor() {
        val salary = com.adrian.minimallauncher.data.FinanceRecord(
            "in", 1_500_000, "Nu", november.atTime(9, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
            com.adrian.minimallauncher.data.MoneyKind.INCOME_FIXED,
        )
        assertEquals(1_500_000, Budget.summary(listOf(salary), emptyList(), 10, 0, november, FinancePlan()).fixedIncomeCents)
        assertEquals(2_000_000, Budget.summary(listOf(salary), emptyList(), 10, 0, november, FinancePlan(2_000_000)).fixedIncomeCents)
        assertEquals(1_500_000, Budget.summary(listOf(salary), emptyList(), 10, 0, november, FinancePlan(1_000_000)).fixedIncomeCents)
    }

    private fun extra(category: String, cents: Long, date: LocalDate) = com.adrian.minimallauncher.data.FinanceRecord(
        "$category$date$cents", cents, category, date.atTime(13, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
        com.adrian.minimallauncher.data.MoneyKind.EXTRA, category,
    )

    @Test fun foodWithItsOwnBudgetLeavesTheFreeAllowanceAlone() {
        // Bag $18,000; comida holds $100 a day ($3,000 of November), so the free week is $15,000 × 7 / 30.
        val plan = FinancePlan(2_000_000)
        val food = com.adrian.minimallauncher.feature.finance.BudgetLimit("comida|DAY", "comida", BudgetPeriod.DAY, 10_000)
        val records = listOf(extra("comida", 5_000, november), extra("otro", 2_000, november))
        val summary = Budget.summary(records, listOf(food), 10, 0, november, plan)
        assertEquals(350_000, summary.weekAllowanceCents)
        assertEquals(2_000, summary.todaySpentCents)
        assertEquals(5_000, summary.topics.single().todaySpentCents)
        // The bag still counts every extra.
        assertEquals(1_800_000 - 7_000, summary.bagLeftCents)
    }

    @Test fun savingBuildsWithTheMonthAndExtrasAheadOfPaceEatIt() {
        val plan = FinancePlan(3_000_000) // saving $3,000, bag $27,000
        val calm = Budget.summary(emptyList(), emptyList(), 10, 0, november, plan)
        assertEquals(40_000, calm.savingPaceCents) // 4 of 30 days
        assertEquals(40_000, calm.savedCents)
        // The bag's pace by the 4th is $3,600; $3,800 spent takes $200 of the saving.
        val loose = Budget.summary(listOf(extra("otro", 380_000, november)), emptyList(), 10, 0, november, plan)
        assertEquals(20_000, loose.savedCents)
    }

    @Test fun earlierDaysLeftoverShowsOnToday() {
        // Wednesday with nothing spent: the week spreads over 5 days instead of 7.
        val summary = Budget.summary(emptyList(), emptyList(), 0, 1_000, november)
        assertEquals(7_000, summary.weekAllowanceCents)
        assertEquals(1_400 - 1_000, summary.carriedCents)
    }

    @Test fun breakdownSplitsByKindThenConcept() {
        val records = listOf(
            extra("comida", 5_000, november), extra("comida", 3_000, november), extra("otro", 2_000, november),
            fixedPaid("Renta", 800_000, november),
        )
        val groups = Budget.breakdown(records)
        assertEquals(listOf(com.adrian.minimallauncher.data.MoneyKind.FIXED, com.adrian.minimallauncher.data.MoneyKind.EXTRA), groups.map { it.kind })
        assertEquals(listOf("comida" to 8_000L, "otro" to 2_000L), groups[1].parts)
        assertEquals(10_000, groups[1].cents)
    }

    @Test fun savingGoalPaceAndMonthlyShare() {
        val goal = com.adrian.minimallauncher.feature.finance.SavingGoal(1_200_000, LocalDate.of(2026, 11, 1), LocalDate.of(2027, 11, 1), 0)
        // 6 months in, half should be there.
        val may = LocalDate.of(2027, 5, 3)
        assertEquals(true, goal.behind(may))
        assertEquals(false, goal.copy(savedCents = 700_000).behind(may))
        // A year-long goal started this month: twelve monthly shares of $1,000.
        assertEquals(12L, goal.monthsLeft(november))
        assertEquals(100_000L, goal.perMonthCents(november))
        assertEquals(true, goal.copy(savedCents = 1_200_000).reached)
    }

    @Test fun pieSlicesFollowTheBudgetAndLightWithSpending() {
        val plan = FinancePlan(2_000_000, listOf(PlanCost("Renta", 500_000, BudgetPeriod.MONTH)))
        val food = com.adrian.minimallauncher.feature.finance.BudgetLimit("comida|DAY", "comida", BudgetPeriod.DAY, 10_000)
        val records = listOf(extra("comida", 5_000, november), extra("otro", 2_000, november), fixedPaid("Renta", 500_000, november))
        val summary = Budget.summary(records, listOf(food), 10, 0, november, plan)
        val month = com.adrian.minimallauncher.feature.finance.BudgetSections.of(summary, records, com.adrian.minimallauncher.feature.finance.Span.MONTH, november)
        // Income $20,000 − rent $5,000 − 10% saving ($1,500) = bag $13,500; comida holds $3,000, libre the other $10,500.
        // The month leads with what is counted monthly (rent), then the daily comida.
        assertEquals(listOf("libre" to 1_050_000L, "Renta" to 500_000L, "comida" to 300_000L, "ahorro" to 150_000L), month.map { it.key to it.budgetCents })
        assertEquals(listOf(2_000L, 500_000L, 5_000L), month.take(3).map { it.spentCents })
        // Income $20,000 − saving $1,500 − rent $5,000 − extras $70.
        assertEquals(1_350_000L - 7_000L, summary.moneyLeftCents)
        assertEquals(507_000L, summary.spentMonthCents)
        // A day shows only what is counted daily: the monthly rent stays out.
        val day = com.adrian.minimallauncher.feature.finance.BudgetSections.of(summary, records, com.adrian.minimallauncher.feature.finance.Span.DAY, november)
        assertEquals(listOf("libre", "comida"), day.map { it.key })
    }

    @Test fun moneyStateColors() {
        val f = com.adrian.minimallauncher.feature.finance.FinanceColorsProbe
        assertEquals(listOf("plenty", "plenty", "going", "low", "low", "over"), listOf(0L, 4_900L, 5_000L, 8_000L, 10_000L, 10_001L).map { f.state(it, 10_000) })
    }

    @Test fun pieInsightsReadThePace() {
        val s = com.adrian.minimallauncher.feature.finance.BudgetSection("comida", "Comida", com.adrian.minimallauncher.feature.finance.SectionKind.TOPIC, 70_000, 49_000, BudgetPeriod.WEEK)
        // 70% spent by Tuesday (2 of 7 days): ahead of time, $210 left over 6 days.
        val lines = com.adrian.minimallauncher.feature.finance.BudgetSections.insights(s, listOf(extra("comida", 30_000, november), extra("comida", 19_000, november)), 140_000, 2, 7)
        assertEquals("Vas a la mitad: quedan $210.", lines[0])
        assertEquals("Gastas más rápido que el tiempo: 70% gastado con 28% del periodo.", lines[1])
        assertEquals("Te tocan $35 por día lo que resta.", lines[2])
        assertEquals("Es el 50% del presupuesto.", lines.last())
    }

    @Test fun withoutPlanIncomeIsZero() {
        val summary = Budget.summary(emptyList(), emptyList(), 10, 0, november)
        assertEquals(0, summary.fixedIncomeCents)
        assertEquals(0, summary.bagCents)
    }
}
