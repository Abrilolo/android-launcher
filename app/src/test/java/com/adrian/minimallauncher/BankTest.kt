package com.adrian.minimallauncher

import com.adrian.minimallauncher.data.FinanceRecord
import com.adrian.minimallauncher.data.MoneyKind
import com.adrian.minimallauncher.feature.bank.Bank
import com.adrian.minimallauncher.feature.bank.BankAccount
import com.adrian.minimallauncher.feature.bank.BankKind
import com.adrian.minimallauncher.feature.bank.BankReceivable
import com.adrian.minimallauncher.feature.finance.Budget
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class BankTest {
    private val today = LocalDate.of(2026, 11, 5)
    private fun at(day: LocalDate) = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private val accounts = listOf(
        BankAccount("nu", "Nu", BankKind.CARD, 950_482, 653_072),
        BankAccount("revolut", "Revolut", BankKind.CARD, 988_419),
        BankAccount("plata", "Plata", BankKind.CARD, 26_286),
        BankAccount("stori", "Stori", BankKind.CARD, 26_380),
        BankAccount("debito", "Débito", BankKind.DEBIT, 1_780_838),
    )
    private val receivables = listOf(
        BankReceivable("a", "Pendiente 8,000", 800_000, 1),
        BankReceivable("b", "Pendiente 12,000", 1_200_000, 2),
    )

    @Test fun seedMatchesWhatTheUserHad() {
        val s = Bank.summary(accounts, receivables, emptyList(), YearMonth.from(today), Budget::dateOf)
        assertEquals(1_780_838, s.debitCents)
        assertEquals(1_991_567, s.cardsCents)
        // 6,530.72 from Nu plus 8,000 and 12,000
        assertEquals(2_653_072, s.owedByOthersCents + s.receivablesCents)
        // 17,808.38 + 26,530.72 − 19,915.67
        assertEquals(2_442_343, s.netCents)
    }

    @Test fun cardLineSeparatesOwnPartAndThisMonth() {
        val nuCharge = FinanceRecord("1", 100_000, "super", at(today), MoneyKind.EXTRA, "comida", account = "nu")
        val old = FinanceRecord("2", 50_000, "vieja", at(today.minusMonths(1)), MoneyKind.EXTRA, "comida", account = "nu")
        val s = Bank.summary(accounts, receivables, listOf(nuCharge, old), YearMonth.from(today), Budget::dateOf)
        val nu = s.lines.first { it.account.id == "nu" }
        assertEquals(100_000, nu.spentMonthCents)
        assertEquals(950_482 - 653_072, nu.ownCents)
    }

    @Test fun monthlyPaymentLeavesTheMonthNotTheDay() {
        val monthly = FinanceRecord("m", 300_000, "tarjeta", at(today), MoneyKind.MONTHLY)
        val extra = FinanceRecord("e", 10_000, "taco", at(today), MoneyKind.EXTRA, "comida")
        val s = Budget.summary(listOf(monthly, extra), emptyList(), 0, 100_000, today, com.adrian.minimallauncher.feature.finance.FinancePlan(1_000_000))
        assertEquals(300_000, s.monthlyCents)
        assertEquals(10_000, s.extrasMonthCents)
        assertEquals(10_000, s.todaySpentCents)
        assertEquals(310_000, s.spentMonthCents)
    }
}
