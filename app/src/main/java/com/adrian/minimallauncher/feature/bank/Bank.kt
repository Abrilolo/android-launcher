package com.adrian.minimallauncher.feature.bank

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import com.adrian.minimallauncher.data.FinanceRecord
import com.adrian.minimallauncher.data.MoneyKind
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

enum class BankKind { CARD, DEBIT, CASH }

/**
 * An account or a card. A debit account's [balanceCents] is what is in it; a card's is what it has charged and is
 * owed. [owedByOthersCents] is the part of a card's charges that someone else still owes the user.
 */
@Entity(tableName = "bank_accounts")
data class BankAccount(
    @PrimaryKey val id: String,
    val name: String,
    val kind: BankKind,
    val balanceCents: Long,
    @ColumnInfo(defaultValue = "0") val owedByOthersCents: Long = 0,
    /** Day of the month the statement closes, and the day the bill is paid; 0 when not set. */
    @ColumnInfo(defaultValue = "0") val cutDay: Int = 0,
    @ColumnInfo(defaultValue = "0") val payDay: Int = 0,
    /** Last 4 digits of the cards tied to this account, comma separated ("1234,5678"): notices are matched by them. */
    @ColumnInfo(defaultValue = "") val cards: String = "",
    /** A credit card's limit; 0 when not set. */
    @ColumnInfo(defaultValue = "0") val limitCents: Long = 0,
    /** What a card must pay on [dueDate] (the closed cut) and what is gathering for the payment after it. */
    @ColumnInfo(defaultValue = "0") val dueCents: Long = 0,
    @ColumnInfo(defaultValue = "") val dueDate: String = "",
    @ColumnInfo(defaultValue = "0") val nextDueCents: Long = 0,
) {
    /** The cards' last 4 digits as a list. */
    val cardDigits: List<String> get() = cards.split(',', ' ').map { it.filter(Char::isDigit) }.filter { it.length == 4 }
    val due: LocalDate? get() = runCatching { LocalDate.parse(dueDate) }.getOrNull()
}

/** Money expected to come in (not yet in an account): [concept] is what it is for. */
@Entity(tableName = "bank_receivables")
data class BankReceivable(
    @PrimaryKey val id: String,
    val concept: String,
    val amountCents: Long,
    val createdAt: Long,
)

@Dao
interface BankDao {
    @Query("SELECT * FROM bank_accounts ORDER BY kind, name") fun accounts(): Flow<List<BankAccount>>
    @Query("SELECT * FROM bank_receivables ORDER BY createdAt") fun receivables(): Flow<List<BankReceivable>>
    @Upsert suspend fun upsertAccount(row: BankAccount)
    @Query("DELETE FROM bank_accounts WHERE id = :id") suspend fun deleteAccount(id: String)
    @Upsert suspend fun upsertReceivable(row: BankReceivable)
    @Query("DELETE FROM bank_receivables WHERE id = :id") suspend fun deleteReceivable(id: String)
    @Query("SELECT * FROM bank_accounts") suspend fun allAccounts(): List<BankAccount>
    @Query("SELECT * FROM bank_receivables") suspend fun allReceivables(): List<BankReceivable>
    @Query("SELECT * FROM bank_accounts WHERE id = :id") suspend fun account(id: String): BankAccount?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAccounts(rows: List<BankAccount>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertReceivables(rows: List<BankReceivable>)
}

/** One card or account as the bank view shows it: its balance, what others owe, and what was spent with it this month. */
data class AccountLine(val account: BankAccount, val spentMonthCents: Long) {
    /** What the user owes on the card after what others pay back. */
    val ownCents: Long get() = account.balanceCents - account.owedByOthersCents
}

/** The bank in totals. Amounts in cents. */
data class BankSummary(
    val debitCents: Long,
    val cardsCents: Long,
    val owedByOthersCents: Long,
    val receivablesCents: Long,
    /** Debit, plus what is to come in, minus what the cards hold. */
    val netCents: Long,
    val lines: List<AccountLine>,
    val receivables: List<BankReceivable>,
    val cashCents: Long = 0,
)

object Bank {
    const val CASH_ACCOUNT_ID = "wallet:cash"
    fun cashAccount() = BankAccount(CASH_ACCOUNT_ID, "Efectivo", BankKind.CASH, 0)
    fun cash(accounts: List<BankAccount>): Long = accounts.filter { it.kind == BankKind.CASH }.sumOf { it.balanceCents }

    fun validateMovement(accounts: List<BankAccount>, record: FinanceRecord) {
        require(accounts.none { it.id == CASH_ACCOUNT_ID && it.kind != BankKind.CASH }) { "Cash wallet identifier is reserved" }
        require(record.amountCents > 0) { "Amount must be positive" }
        if (record.kind == MoneyKind.WITHDRAWAL) {
            require(accounts.any { it.id == record.account && (it.kind == BankKind.DEBIT || it.kind == BankKind.CARD) }) { "A withdrawal requires a debit or card source" }
            require(record.pendingFromOthersCents == 0L) { "A withdrawal cannot create a receivable" }
        }
    }

    /** What the user has: the money in debit plus what is still to come in (receivables and what others owe the cards). */
    fun myMoney(accounts: List<BankAccount>, receivables: List<BankReceivable>): Long =
        debit(accounts) + cash(accounts) + toCome(accounts, receivables)

    /** What is still to come in: receivables and what others owe the cards. */
    fun toCome(accounts: List<BankAccount>, receivables: List<BankReceivable>): Long =
        accounts.filter { it.kind == BankKind.CARD }.sumOf { it.owedByOthersCents } + receivables.sumOf { it.amountCents }

    /** What the budget works from: mi dinero less what the cards owe. */
    fun netMoney(accounts: List<BankAccount>, receivables: List<BankReceivable>): Long =
        myMoney(accounts, receivables) - accounts.filter { it.kind == BankKind.CARD }.sumOf { it.balanceCents }

    fun debit(accounts: List<BankAccount>): Long = accounts.filter { it.kind == BankKind.DEBIT }.sumOf { it.balanceCents }

    /** The account after [record] moved through it (see [applyMovement]); null when the movement does not touch it. */
    fun applied(account: BankAccount, record: FinanceRecord, sign: Long): BankAccount? {
        require(sign == 1L || sign == -1L)
        if (record.kind == MoneyKind.WITHDRAWAL) return when {
            account.id == record.account && account.kind == BankKind.DEBIT -> account.copy(balanceCents = account.balanceCents - sign * record.amountCents)
            // Cash taken out with a credit card (Stori "disposición", Plata at an ATM): the card owes it.
            account.id == record.account && account.kind == BankKind.CARD -> charged(account, sign * record.amountCents, dateOf(record))
            account.id == CASH_ACCOUNT_ID && account.kind == BankKind.CASH -> account.copy(balanceCents = account.balanceCents + sign * record.amountCents)
            else -> null
        }
        if (account.id != record.account) return null
        val spending = record.kind.isExpense
        val amount = sign * record.amountCents
        return when {
            account.kind == BankKind.CARD && spending -> charged(account, amount, dateOf(record)).copy(
                owedByOthersCents = (account.owedByOthersCents + sign * record.pendingFromOthersCents).coerceAtLeast(0),
            )
            account.kind != BankKind.CARD && spending -> account.copy(balanceCents = account.balanceCents - amount)
            account.kind != BankKind.CARD && record.kind.isIncome -> account.copy(balanceCents = account.balanceCents + amount)
            else -> null
        }
    }

    private fun dateOf(record: FinanceRecord): LocalDate =
        java.time.Instant.ofEpochMilli(record.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()

    /**
     * A card after [cents] were charged (negative: taken back) on [date]. The debt grows, and so does the payment it
     * lands in: the coming one when the next cut closes before that payment is due, else the one after it.
     * Nu (cut 14, pay 24): bought on the 11th, paid on the 24th. Revolut (cut 3, pay 23): bought on the 11th, paid next
     * month's 23rd.
     */
    fun charged(account: BankAccount, cents: Long, date: LocalDate): BankAccount {
        val card = rolled(account, date)
        val due = card.due ?: nextDate(card.payDay, date)
        val cut = nextDate(card.cutDay, date)
        val intoDue = due == null || cut == null || cut.isBefore(due)
        return card.copy(
            balanceCents = card.balanceCents + cents,
            dueCents = if (intoDue) (card.dueCents + cents).coerceAtLeast(0) else card.dueCents,
            nextDueCents = if (intoDue) card.nextDueCents else (card.nextDueCents + cents).coerceAtLeast(0),
            dueDate = card.dueDate.ifBlank { due?.toString().orEmpty() },
        )
    }

    /**
     * A card once its payment day has gone by: what was gathering becomes the payment to make on the next payment day.
     * Whatever of the old payment was not paid stays in it (it is late).
     */
    fun rolled(account: BankAccount, today: LocalDate): BankAccount {
        if (account.kind != BankKind.CARD || account.payDay !in 1..31) return account
        var due = account.due ?: return account
        var dueCents = account.dueCents
        var next = account.nextDueCents
        while (due.isBefore(today)) {
            dueCents += next
            next = 0
            due = nextDate(account.payDay, due.plusDays(1)) ?: return account
        }
        return if (due == account.due) account else account.copy(dueCents = dueCents, nextDueCents = next, dueDate = due.toString())
    }

    /** A payment made to a card: the debt and the coming payment go down, then the one after it. */
    fun paid(account: BankAccount, cents: Long): BankAccount {
        val fromDue = minOf(cents, account.dueCents)
        return account.copy(
            balanceCents = (account.balanceCents - cents).coerceAtLeast(0),
            dueCents = account.dueCents - fromDue,
            nextDueCents = (account.nextDueCents - (cents - fromDue)).coerceAtLeast(0),
        )
    }

    /** The bank a name belongs to, to match a bank app's notices: "Mercado Pago" -> "mercadopago". */
    fun bankKey(name: String): String =
        java.text.Normalizer.normalize(name.lowercase(), java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").replace(Regex("[^a-z0-9]"), "")

    /**
     * Which account a bank notice belongs to. The card's last 4 digits decide when the notice has them; otherwise the
     * bank's credit card when [credit] (purchases on credit, cash from a card) and its debit account when not.
     */
    fun resolve(accounts: List<BankAccount>, bank: String, digits: String?, credit: Boolean): BankAccount? {
        digits?.let { d -> accounts.firstOrNull { d in it.cardDigits }?.let { return it } }
        if (bank.isBlank()) return null
        val mine = accounts.filter { bankKey(it.name).startsWith(bank) }
        val wanted = if (credit) BankKind.CARD else BankKind.DEBIT
        return mine.firstOrNull { it.kind == wanted } ?: mine.firstOrNull()
    }

    /** The next date that falls on [day] of the month, today included; null when there is no such day. */
    fun nextDate(day: Int, today: LocalDate): LocalDate? {
        if (day !in 1..31) return null
        val month = YearMonth.from(today)
        val this_ = month.atDay(minOf(day, month.lengthOfMonth()))
        if (!this_.isBefore(today)) return this_
        val next = month.plusMonths(1)
        return next.atDay(minOf(day, next.lengthOfMonth()))
    }

    /** Totals of [accounts] and [receivables], and what each card took this [month] from the movements in [records]. */
    fun summary(accounts: List<BankAccount>, receivables: List<BankReceivable>, records: List<FinanceRecord>, month: YearMonth, dateOf: (FinanceRecord) -> java.time.LocalDate): BankSummary {
        val debit = accounts.filter { it.kind == BankKind.DEBIT }.sumOf { it.balanceCents }
        val cards = accounts.filter { it.kind == BankKind.CARD }
        val cardsTotal = cards.sumOf { it.balanceCents }
        val owed = cards.sumOf { it.owedByOthersCents }
        val receivable = receivables.sumOf { it.amountCents }
        val spentKinds = setOf(MoneyKind.EXTRA, MoneyKind.SHARED, MoneyKind.FIXED, MoneyKind.MONTHLY)
        val inMonth = records.filter { it.kind in spentKinds && YearMonth.from(dateOf(it)) == month }
        return BankSummary(
            debitCents = debit,
            cardsCents = cardsTotal,
            owedByOthersCents = owed,
            receivablesCents = receivable,
            netCents = debit + cash(accounts) + owed + receivable - cardsTotal,
            lines = accounts.map { a -> AccountLine(a, inMonth.filter { it.account == a.id }.sumOf { it.amountCents }) },
            receivables = receivables,
            cashCents = cash(accounts),
        )
    }

    /** One account as the user gave it on 2026-11-11 (amounts in cents; the card's payment dates as ISO dates). */
    private class Start(
        val id: String, val name: String, val kind: BankKind, val balance: Long,
        val limit: Long = 0, val cut: Int = 0, val pay: Int = 0, val due: Long = 0, val dueDate: String = "", val next: Long = 0,
    )

    private val START_2026_11 = listOf(
        Start("revolut", "Revolut", BankKind.CARD, 1414067, limit = 1500000, cut = 3, pay = 23, due = 988819, dueDate = "2026-11-23", next = 382209),
        Start("debito", "Revolut", BankKind.DEBIT, 2336625),
        Start("nu", "Nu", BankKind.CARD, 987741, limit = 3740000, cut = 14, pay = 24, due = 987741, dueDate = "2026-11-24"),
        Start("nu-debito", "Nu", BankKind.DEBIT, 88),
        // Oct 17 ($290.89) and Nov 5 ($1,820.07), both in the cut of Nov 12, paid by Dec 2.
        Start("stori", "Stori", BankKind.CARD, 211096, limit = 800000, cut = 12, pay = 2, due = 211096, dueDate = "2026-12-02"),
        Start("stori-debito", "Stori", BankKind.DEBIT, 0),
        Start("plata", "Plata", BankKind.CARD, 134479, limit = 600000, cut = 27, pay = 26, due = 26286, dueDate = "2026-11-26", next = 108193),
        Start("mercadopago", "Mercado Pago", BankKind.CARD, 0, limit = 330000, cut = 13, pay = 3, dueDate = "2026-12-03"),
        Start("mercadopago-debito", "Mercado Pago", BankKind.DEBIT, 372),
        Start(CASH_ACCOUNT_ID, "Efectivo", BankKind.CASH, 0),
    )

    /**
     * The clean start of 2026-11-11 (database version 11): only these accounts stay, set to what the user gave. A card
     * that was there keeps what others owe it.
     */
    val RESET_2026_11_SQL: List<String> = buildList {
        add("DELETE FROM `bank_accounts` WHERE `id` NOT IN (" + START_2026_11.joinToString { "'${it.id}'" } + ")")
        START_2026_11.forEach { a ->
            add("INSERT OR IGNORE INTO `bank_accounts` (`id`, `name`, `kind`, `balanceCents`) VALUES ('${a.id}', '${a.name}', '${a.kind.name}', 0)")
            add(
                "UPDATE `bank_accounts` SET `name` = '${a.name}', `kind` = '${a.kind.name}', `balanceCents` = ${a.balance}, " +
                    "`limitCents` = ${a.limit}, `cutDay` = ${a.cut}, `payDay` = ${a.pay}, `dueCents` = ${a.due}, `dueDate` = '${a.dueDate}', " +
                    "`nextDueCents` = ${a.next}, `cards` = '' WHERE `id` = '${a.id}'",
            )
        }
    }

    /**
     * The first version of the bank, as the user had it: Nu, Revolut, Plata and Stori owed these amounts, Nu's part of
     * them owed by someone else, the debit account held the rest, and two incomes were still to come.
     */
    val SEED_SQL: List<String> = listOf(
        "INSERT OR IGNORE INTO `bank_accounts` (`id`, `name`, `kind`, `balanceCents`, `owedByOthersCents`) VALUES " +
            "('nu', 'Nu', 'CARD', 950482, 653072), " +
            "('revolut', 'Revolut', 'CARD', 988419, 0), " +
            "('plata', 'Plata', 'CARD', 26286, 0), " +
            "('stori', 'Stori', 'CARD', 26380, 0), " +
            "('debito', 'Débito', 'DEBIT', 1780838, 0)",
        "INSERT OR IGNORE INTO `bank_receivables` (`id`, `concept`, `amountCents`, `createdAt`) VALUES " +
            "('pendiente-8000', 'Pendiente 8,000', 800000, CAST(strftime('%s', 'now') AS INTEGER) * 1000), " +
            "('pendiente-12000', 'Pendiente 12,000', 1200000, CAST(strftime('%s', 'now') AS INTEGER) * 1000 + 1)",
    )
}

/**
 * Moves the balances with one movement. Spending raises a card's debt (and what others owe it, for a shared bill) and
 * lowers a debit account; income raises a debit account. Income into a card is not a change of debt. [sign] 1 applies
 * the movement, -1 takes it back (before an edit or a delete).
 */
suspend fun BankDao.applyMovement(record: FinanceRecord, sign: Long) {
    var accounts = allAccounts()
    Bank.validateMovement(accounts, record)
    if (record.account.isBlank()) return
    if (record.kind == MoneyKind.WITHDRAWAL || record.account == Bank.CASH_ACCOUNT_ID) {
        if (accounts.none { it.id == Bank.CASH_ACCOUNT_ID }) {
            insertAccounts(listOf(Bank.cashAccount()))
            accounts = allAccounts()
        }
    }
    accounts.forEach { account -> Bank.applied(account, record, sign)?.let { upsertAccount(it) } }
}
