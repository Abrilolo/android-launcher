package com.adrian.minimallauncher

import com.adrian.minimallauncher.data.FinanceRecord
import com.adrian.minimallauncher.data.MoneyKind
import com.adrian.minimallauncher.feature.bank.Bank
import com.adrian.minimallauncher.feature.bank.BankAccount
import com.adrian.minimallauncher.feature.bank.BankKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BankLedgerTest {
    private val nu = BankAccount("nu", "Nu", BankKind.CARD, 950_482, 653_072)
    private val debit = BankAccount("debito", "Débito", BankKind.DEBIT, 1_780_838)

    private fun spend(kind: MoneyKind, cents: Long, account: String, pending: Long = 0) =
        FinanceRecord("r", cents, "x", 1, kind, "comida", pending, null, account)

    @Test fun cardChargeRaisesDebt() {
        val after = Bank.applied(nu, spend(MoneyKind.EXTRA, 10_000, "nu"), 1)!!
        assertEquals(960_482, after.balanceCents)
    }

    @Test fun takingBackAChargeLowersDebt() {
        val after = Bank.applied(nu, spend(MoneyKind.EXTRA, 10_000, "nu"), -1)!!
        assertEquals(940_482, after.balanceCents)
    }

    @Test fun sharedChargeAddsWhatOthersOwe() {
        val after = Bank.applied(nu, spend(MoneyKind.SHARED, 10_000, "nu", pending = 6_000), 1)!!
        assertEquals(653_072 + 6_000, after.owedByOthersCents)
        assertEquals(960_482, after.balanceCents)
    }

    @Test fun monthlyCardPaymentRaisesDebt() {
        val after = Bank.applied(nu, spend(MoneyKind.MONTHLY, 30_000, "nu"), 1)!!
        assertEquals(980_482, after.balanceCents)
    }

    @Test fun debitSpendingLowersTheAccount() {
        assertEquals(1_770_838, Bank.applied(debit, spend(MoneyKind.EXTRA, 10_000, "debito"), 1)!!.balanceCents)
    }

    @Test fun debitIncomeRaisesTheAccount() {
        val income = FinanceRecord("i", 50_000, "nomina", 1, MoneyKind.INCOME_FIXED, "", 0, null, "debito")
        assertEquals(1_830_838, Bank.applied(debit, income, 1)!!.balanceCents)
    }

    @Test fun incomeIntoACardDoesNotChangeDebt() {
        val income = FinanceRecord("i", 50_000, "reembolso", 1, MoneyKind.GIFT, "", 0, null, "nu")
        assertNull(Bank.applied(nu, income, 1))
    }
}
