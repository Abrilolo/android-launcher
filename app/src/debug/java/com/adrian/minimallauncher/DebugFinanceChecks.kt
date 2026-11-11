package com.adrian.minimallauncher

import android.content.Context
import androidx.room.Room
import com.adrian.minimallauncher.data.*
import com.adrian.minimallauncher.feature.bank.*
import com.adrian.minimallauncher.feature.finance.*
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.UUID

/** Opt-in emulator check using a separate disposable database, never the launcher's database. */
internal suspend fun verifyFinancePersistence(context: Context): String {
    val name = "finance-verification-${UUID.randomUUID()}.db"
    fun open() = Room.databaseBuilder(context, LauncherDatabase::class.java, name).build()
    var db = open()
    try {
        val repo = PersonalRepository(db)
        repo.saveAccount(BankAccount("test-card", "Test card", BankKind.CARD, 0))
        val payment = ScheduledPayment("test-payment", "Test subscription", 9900, "test-card", 10)
        val plan = FinancePlan(2000000, listOf(PlanCost("Rent", 600000, BudgetPeriod.MONTH)), listOf(payment))
        check(FinancePlan.decode(plan.encode()) == plan) { "Plan/payment JSON round-trip failed" }
        val first = repo.addMoney(MoneyKind.FIXED, payment.cents, payment.name, payment.category, account = payment.accountId)
        val second = repo.addMoney(MoneyKind.FIXED, payment.cents, payment.name, payment.category, account = payment.accountId)
        check(first == second) { "Duplicate recurring charge ID" }
        check(repo.bankAccounts.first().single().balanceCents == 9900L) { "Balance charged twice" }
        db.close()
        db = open()
        val reopened = PersonalRepository(db)
        val rows = reopened.money.first()
        check(rows.size == 1 && rows.single().id == first) { "Expense did not survive reopening" }
        check(payment.paid(rows, LocalDate.now())) { "Payment link did not survive reopening" }
        check(reopened.bankAccounts.first().single().balanceCents == 9900L) { "Balance did not persist" }
        reopened.deleteFinance(first)
        check(reopened.bankAccounts.first().single().balanceCents == 0L) { "Deleting charge did not reverse balance" }
        check(!payment.paid(reopened.money.first(), LocalDate.now())) { "Deleted charge still marks payment paid" }
        reopened.saveAccount(BankAccount("test-debit", "Test debit", BankKind.DEBIT, 100000))
        val withdrawalId = reopened.addMoney(MoneyKind.WITHDRAWAL, 30000, "ATM", account = "test-debit")
        check(reopened.bankAccounts.first().single { it.id == "test-debit" }.balanceCents == 70000L)
        check(reopened.bankAccounts.first().single { it.id == Bank.CASH_ACCOUNT_ID }.balanceCents == 30000L)
        check(reopened.bankAccounts.first().single { it.id == "test-card" }.balanceCents == 0L)
        val withdrawal = reopened.money.first().single { it.id == withdrawalId }
        val invalid = runCatching { reopened.updateMoney(withdrawal.copy(account = "test-card")) }
        check(invalid.isFailure) { "Withdrawal from credit was accepted" }
        check(reopened.bankAccounts.first().single { it.id == "test-debit" }.balanceCents == 70000L) { "Invalid edit did not roll back" }
        reopened.updateMoney(withdrawal.copy(amountCents = 20000))
        db.close()
        db = open()
        val cashRepo = PersonalRepository(db)
        check(cashRepo.money.first().single { it.id == withdrawalId }.kind == MoneyKind.WITHDRAWAL)
        check(cashRepo.bankAccounts.first().single { it.id == Bank.CASH_ACCOUNT_ID }.balanceCents == 20000L)
        cashRepo.deleteFinance(withdrawalId)
        check(cashRepo.bankAccounts.first().single { it.id == "test-debit" }.balanceCents == 100000L)
        check(cashRepo.bankAccounts.first().single { it.id == Bank.CASH_ACCOUNT_ID }.balanceCents == 0L)
        return "PASS: recurring charge idempotence; Room reopen; withdrawal persistence; atomic invalid-edit rollback; debit/cash reversal; credit unaffected."
    } finally { db.close() }
}
