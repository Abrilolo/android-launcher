package com.adrian.minimallauncher

import com.adrian.minimallauncher.feature.bank.Bank
import com.adrian.minimallauncher.feature.bank.BankAccount
import com.adrian.minimallauncher.feature.bank.BankKind
import com.adrian.minimallauncher.feature.bank.BankReceivable
import org.junit.Assert.assertEquals
import org.junit.Test

class MyMoneyTest {
    private val accounts = listOf(
        BankAccount("nu", "Nu", BankKind.CARD, 950_482, 653_072),
        BankAccount("revolut", "Revolut", BankKind.CARD, 988_419),
        BankAccount("debito", "Revolut", BankKind.DEBIT, 1_780_838),
    )
    private val receivables = listOf(
        BankReceivable("a", "Pendiente 8,000", 800_000, 1),
        BankReceivable("b", "Pendiente 12,000", 1_200_000, 2),
    )

    @Test fun myMoneyIsDebitPlusWhatIsToComeIn() {
        // debit 17,808.38 + 6,530.72 owed by others + 8,000 + 12,000
        assertEquals(4_433_910, Bank.myMoney(accounts, receivables))
    }

    @Test fun toComeCountsOnlyIncomingMoney() {
        assertEquals(2_653_072, Bank.toCome(accounts, receivables))
        assertEquals(1_780_838, Bank.debit(accounts))
    }

    @Test fun differenceIsMyMoneyLessWhatTheCardsOwe() {
        // 4,433,910 − (950,482 + 988,419)
        assertEquals(2_495_009, Bank.netMoney(accounts, receivables))
    }
}
