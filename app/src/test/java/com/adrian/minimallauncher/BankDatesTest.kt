package com.adrian.minimallauncher

import com.adrian.minimallauncher.data.BankApps
import com.adrian.minimallauncher.feature.bank.Bank
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BankDatesTest {
    @Test fun nextDateIsThisMonthWhenStillAhead() {
        assertEquals(LocalDate.of(2026, 11, 20), Bank.nextDate(20, LocalDate.of(2026, 11, 5)))
    }

    @Test fun nextDateRollsToNextMonthWhenPassed() {
        assertEquals(LocalDate.of(2026, 12, 3), Bank.nextDate(3, LocalDate.of(2026, 11, 5)))
    }

    @Test fun nextDateTodayCounts() {
        assertEquals(LocalDate.of(2026, 11, 5), Bank.nextDate(5, LocalDate.of(2026, 11, 5)))
    }

    @Test fun dayPastTheMonthEndUsesTheLastDay() {
        assertEquals(LocalDate.of(2026, 2, 28), Bank.nextDate(31, LocalDate.of(2026, 2, 1)))
    }

    @Test fun noDayMeansNoDate() {
        assertNull(Bank.nextDate(0, LocalDate.of(2026, 11, 5)))
    }

    @Test fun noticesGoToTheirBank() {
        assertEquals("nu", BankApps.accountOf("com.nu.production"))
        assertEquals("revolut", BankApps.accountOf("com.revolut.revolut"))
        assertEquals("", BankApps.accountOf("com.gbm.app"))
    }
}
