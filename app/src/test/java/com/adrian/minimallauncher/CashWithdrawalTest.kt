package com.adrian.minimallauncher

import com.adrian.minimallauncher.data.*
import com.adrian.minimallauncher.feature.bank.*
import com.adrian.minimallauncher.feature.finance.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class CashWithdrawalTest {
    private val debit = BankAccount("debit", "Debit", BankKind.DEBIT, 100000)
    private val card = BankAccount("card", "Card", BankKind.CARD, 20000)
    private val today = LocalDate.of(2026, 11, 9)
    private fun record(kind: MoneyKind = MoneyKind.WITHDRAWAL, account: String = debit.id, amount: Long = 30000) =
        FinanceRecord("movement", amount, "Cash", today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(), kind, account = account)

    @Test fun withdrawalMovesDebitToCashWithoutDebtOrNetLoss() = runBlocking {
        val dao = MemoryBank(listOf(debit, card))
        val before = Bank.netMoney(dao.rows, emptyList())
        dao.applyMovement(record(), 1)
        assertEquals(70000L, dao.rows.single { it.id == debit.id }.balanceCents)
        assertEquals(30000L, Bank.cash(dao.rows))
        assertEquals(card, dao.rows.single { it.id == card.id })
        assertEquals(before, Bank.netMoney(dao.rows, emptyList()))
        val summary = Budget.summary(listOf(record()), emptyList(), 10, 0, today, FinancePlan(200000))
        assertEquals(0L, summary.spentMonthCents)
        assertEquals(180000L, summary.moneyLeftCents)
        assertFalse(MoneyKind.WITHDRAWAL.isExpense)
        assertFalse(MoneyKind.WITHDRAWAL.isIncome)
    }

    @Test fun editThenDeleteReversesBothSides() = runBlocking {
        val dao = MemoryBank(listOf(debit, card))
        dao.applyMovement(record(), 1)
        dao.applyMovement(record(), -1)
        dao.applyMovement(record(amount = 10000), 1)
        assertEquals(90000L, dao.rows.single { it.id == debit.id }.balanceCents)
        assertEquals(10000L, Bank.cash(dao.rows))
        dao.applyMovement(record(amount = 10000), -1)
        assertEquals(debit, dao.rows.single { it.id == debit.id })
        assertEquals(0L, Bank.cash(dao.rows))
    }

    @Test fun spendingCashIsRecordedOnlyWhenSpent() = runBlocking {
        val dao = MemoryBank(listOf(debit, card))
        dao.applyMovement(record(), 1)
        val purchase = record(MoneyKind.EXTRA, Bank.CASH_ACCOUNT_ID, 5000)
        dao.applyMovement(purchase, 1)
        assertEquals(25000L, Bank.cash(dao.rows))
        assertEquals(70000L, dao.rows.single { it.id == debit.id }.balanceCents)
        assertEquals(card, dao.rows.single { it.id == card.id })
        assertEquals(5000L, Budget.summary(listOf(record(), purchase), emptyList(), 0, 0, today).spentMonthCents)
    }

    @Test fun cardMissingAndCashSourcesAreRejectedWithoutWrites() = runBlocking {
        val dao = MemoryBank(listOf(debit, card))
        // Cash from a credit card (Stori "disposición", Plata at an ATM) is allowed since 2026-11-11.
        for (source in listOf("", "missing", Bank.CASH_ACCOUNT_ID)) {
            try { dao.applyMovement(record(account = source), 1); fail("Invalid source accepted") }
            catch (_: IllegalArgumentException) { }
            assertEquals(listOf(debit, card), dao.rows)
        }
    }

    @Test fun paymentOnlyTouchesSelectedAccount() = runBlocking {
        val dao = MemoryBank(listOf(debit, card))
        dao.applyMovement(record(MoneyKind.EXTRA, card.id, 5000), 1)
        assertEquals(debit, dao.rows.single { it.id == debit.id })
        assertEquals(25000L, dao.rows.single { it.id == card.id }.balanceCents)
        dao.applyMovement(record(MoneyKind.EXTRA, debit.id, 7000), 1)
        assertEquals(93000L, dao.rows.single { it.id == debit.id }.balanceCents)
        assertEquals(25000L, dao.rows.single { it.id == card.id }.balanceCents)
    }

    private class MemoryBank(initial: List<BankAccount>) : BankDao {
        val rows = initial.toMutableList()
        override fun accounts() = flowOf(rows.toList())
        override fun receivables() = flowOf(emptyList<BankReceivable>())
        override suspend fun allAccounts() = rows.toList()
        override suspend fun allReceivables() = emptyList<BankReceivable>()
        override suspend fun account(id: String) = rows.firstOrNull { it.id == id }
        override suspend fun upsertAccount(row: BankAccount) { rows.removeAll { it.id == row.id }; rows.add(row) }
        override suspend fun deleteAccount(id: String) { rows.removeAll { it.id == id } }
        override suspend fun insertAccounts(rows: List<BankAccount>) { rows.forEach { if (this.rows.none { a -> a.id == it.id }) this.rows.add(it) } }
        override suspend fun upsertReceivable(row: BankReceivable) = Unit
        override suspend fun deleteReceivable(id: String) = Unit
        override suspend fun insertReceivables(rows: List<BankReceivable>) = Unit
    }
}
