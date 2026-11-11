package com.adrian.minimallauncher.data

import com.adrian.minimallauncher.feature.bank.applyMovement
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.withTransaction
import com.adrian.minimallauncher.ForcedModeService
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** The forced registration flow, one payment at a time: ¿comida? → gasto → comida. */
enum class ForcedStep {
    /** Was this payment food? */
    ASK_FOOD,
    /** Confirm the expense (amount, concept, category). */
    EXPENSE,
    /** It was food: register the meal it paid for. */
    MEAL,
    /** Money came in: one tap says what kind of income it was. */
    INCOME,
    /** No payments left, but salida asked to register the day's meals that are still unmarked. */
    ROUTINE_MEALS,
}

/** What the launcher must show before letting the user go; null in [ForcedMode.state] means nothing is pending. */
data class ForcedState(
    val step: ForcedStep,
    val payment: PendingPayment? = null,
    /** Answer to "¿comida?" for [payment], once given. */
    val isFood: Boolean? = null,
    val openPayments: Int = 0,
)

object ForcedMode {
    /**
     * The step to show. A payment whose expense is saved but still open was food (non-food ones close on saving
     * the expense), so after a restart it resumes at [ForcedStep.MEAL].
     */
    fun state(open: List<PendingPayment>, foodAnswers: Map<String, Boolean>, routinePending: Boolean): ForcedState? {
        val payment = open.firstOrNull() ?: return if (routinePending) ForcedState(ForcedStep.ROUTINE_MEALS) else null
        val answer = foodAnswers[payment.id]
        if (payment.isIncome) return ForcedState(ForcedStep.INCOME, payment, false, open.size)
        val step = when {
            payment.financeId != null -> ForcedStep.MEAL
            answer == null -> ForcedStep.ASK_FOOD
            else -> ForcedStep.EXPENSE
        }
        return ForcedState(step, payment, if (payment.financeId != null) true else answer, open.size)
    }

    /** Whether the user turned on the accessibility service that brings the phone back to the flow. */
    fun serviceEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        val me = ComponentName(context, ForcedModeService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }
}

private val Context.forcedStore by preferencesDataStore(name = "forced")

/** Pending payments and the salida "register what is pending" mark. */
class ForcedRepository(context: Context, private val db: LauncherDatabase = LauncherDatabase.get(context)) {
    private val context: Context = context.applicationContext ?: context
    private val pending = db.pending()
    private val personal = db.personal()
    private val routinePendingKey = stringPreferencesKey("routine_pending_date")

    val openPayments: Flow<List<PendingPayment>> = pending.open()

    /** Salida asked to register the day's meals; it only counts on the day it was set. */
    val routinePending: Flow<Boolean> = context.forcedStore.data.map { it[routinePendingKey] == LocalDate.now().toString() }

    /** True while anything keeps the phone on the flow (what [ForcedModeService] watches). */
    val active: Flow<Boolean> = combine(openPayments, routinePending) { open, routine -> open.isNotEmpty() || routine }

    private fun newId() = UUID.randomUUID().toString()

    /**
     * A payment or income seen outside the launcher (see [com.adrian.minimallauncher.BankNotificationService]).
     * A fixed [id] makes a re-posted notification a no-op. Returns false when nothing was added: the same amount
     * in the same direction already came from another app in the last [DUPLICATE_WINDOW_MS] (wallet and bank both
     * notify), or it is the other half of an open transfer between the user's own accounts.
     */
    suspend fun addPayment(amountCents: Long, source: String, merchant: String = "", direction: String = PendingPayment.OUT, id: String = newId(), account: String = ""): Boolean {
        val now = System.currentTimeMillis()
        if (source != PendingPayment.SOURCE_DEBUG) {
            if (pending.countSimilar(amountCents, direction, source, now - DUPLICATE_WINDOW_MS) > 0) return false
            // The money that left the debit to pay a card that already said it got paid: not spending.
            if (direction == PendingPayment.OUT) pending.unmatchedCardPayment(amountCents, source, now - PendingPayment.CARD_MATCH_MS)?.let { card ->
                pending.tag(card.id, "matched:$id")
                takeFromDebit(account, amountCents)
                return false
            }
            // Sent from one bank and received in another, however long it took: money moved between own accounts,
            // still the user's, so neither spent nor earned. The open first half closes without a movement.
            pending.mirror(amountCents, direction, source)?.let { other ->
                pending.resolve(other.id, other.financeId, now)
                return false
            }
        }
        pending.insert(listOf(PendingPayment(id, amountCents, source, merchant, now, direction = direction, account = account)))
        return true
    }

    /** Debug stand-in for a bank notification. */
    suspend fun addTestPayment(amountCents: Long = 8500, merchant: String = "OXXO", direction: String = PendingPayment.OUT) =
        addPayment(amountCents, PendingPayment.SOURCE_DEBUG, merchant, direction)

    /** Saves a pending income as [kind] (fixed income, transfer, gift or service) and closes it. */
    suspend fun saveIncome(payment: PendingPayment, amountCents: Long, kind: MoneyKind): String = db.withTransaction {
        val financeId = newId()
        val row = FinanceRecord(financeId, amountCents, payment.merchant, System.currentTimeMillis(), kind, account = payment.account.ifBlank { BankApps.accountOf(payment.source) })
        personal.insertFinances(listOf(row))
        db.bank().applyMovement(row, 1)
        pending.resolve(payment.id, financeId, System.currentTimeMillis())
        financeId
    }

    /**
     * Saves the payment's expense. Not food: the payment is done. Food: it stays open with the expense linked
     * until [saveMeal]. Returns the finance id.
     */
    suspend fun saveExpense(payment: PendingPayment, amountCents: Long, description: String, category: String, isFood: Boolean, kind: MoneyKind = MoneyKind.EXTRA): String =
        db.withTransaction {
            val financeId = newId()
            val row = FinanceRecord(financeId, amountCents, description.ifBlank { payment.merchant }, System.currentTimeMillis(), kind, category.trim().lowercase(), account = payment.account.ifBlank { BankApps.accountOf(payment.source) })
            personal.insertFinances(listOf(row))
            db.bank().applyMovement(row, 1)
            // A food payment keeps resolvedAt empty: it stays open, now with its expense, until the meal is saved.
            pending.resolve(payment.id, financeId, if (isFood) null else System.currentTimeMillis())
            financeId
        }

    /** The meal for a food payment was registered: link it to the expense and close the payment. */
    suspend fun saveMeal(payment: PendingPayment, mealId: String) = db.withTransaction {
        payment.financeId?.let { id -> personal.finance(id)?.let { personal.upsertFinance(it.copy(mealId = mealId)) } }
        pending.resolve(payment.id, payment.financeId, System.currentTimeMillis())
    }

    /** Cash taken out (a bank notice): recorded at once, from [account] into the cash wallet; nothing to ask. */
    suspend fun saveWithdrawal(amountCents: Long, account: String, merchant: String, id: String) = db.withTransaction {
        if (personal.finance(id) != null) return@withTransaction
        val row = FinanceRecord(id, amountCents, merchant.ifBlank { "Retiro" }, System.currentTimeMillis(), MoneyKind.WITHDRAWAL, account = account)
        personal.insertFinances(listOf(row))
        db.bank().applyMovement(row, 1)
    }

    /**
     * A card said it got paid (Plata "Recibimos tu pago"): its debt and coming payment go down. The debit's notice of
     * the same amount, before or after this one (up to [PendingPayment.CARD_MATCH_MS]), is that payment's other half:
     * it is closed without spending and only lowers the debit.
     */
    suspend fun payCard(account: String, amountCents: Long, source: String, id: String) = db.withTransaction {
        val bank = db.bank()
        val card = bank.account(account)?.takeIf { it.kind == com.adrian.minimallauncher.feature.bank.BankKind.CARD } ?: return@withTransaction
        if (pending.all().any { it.id == id }) return@withTransaction
        bank.upsertAccount(com.adrian.minimallauncher.feature.bank.Bank.paid(card, amountCents))
        val now = System.currentTimeMillis()
        val debitSide = pending.openOut(amountCents, source, now - PendingPayment.CARD_MATCH_MS)
        if (debitSide != null) {
            pending.resolve(debitSide.id, null, now)
            takeFromDebit(debitSide.account, amountCents)
        }
        pending.insert(listOf(PendingPayment(id, amountCents, source, "", now, resolvedAt = now, financeId = debitSide?.let { "matched:${it.id}" }, direction = PendingPayment.CARD, account = account)))
    }

    /**
     * The register flow's "Tarjeta": the money that left [payment]'s debit account paid [cardId]. Money between the
     * user's own accounts, so no movement: the debit goes down, the card's debt and coming payment too.
     */
    suspend fun saveCardPayment(payment: PendingPayment, amountCents: Long, cardId: String) = db.withTransaction {
        val bank = db.bank()
        bank.account(cardId)?.takeIf { it.kind == com.adrian.minimallauncher.feature.bank.BankKind.CARD }?.let {
            bank.upsertAccount(com.adrian.minimallauncher.feature.bank.Bank.paid(it, amountCents))
        }
        takeFromDebit(payment.account, amountCents)
        pending.resolve(payment.id, null, System.currentTimeMillis())
    }

    private suspend fun takeFromDebit(account: String, amountCents: Long) {
        val bank = db.bank()
        bank.account(account)?.takeIf { it.kind == com.adrian.minimallauncher.feature.bank.BankKind.DEBIT }?.let {
            bank.upsertAccount(it.copy(balanceCents = it.balanceCents - amountCents))
        }
    }

    /** The card's payment reminder: what it says is owed by [dueDay] becomes the coming payment. */
    suspend fun setStatement(account: String, amountCents: Long, dueDay: Int?) {
        val bank = db.bank()
        val card = bank.account(account)?.takeIf { it.kind == com.adrian.minimallauncher.feature.bank.BankKind.CARD } ?: return
        val day = dueDay ?: card.payDay
        val due = com.adrian.minimallauncher.feature.bank.Bank.nextDate(day, LocalDate.now())
        bank.upsertAccount(card.copy(dueCents = amountCents, dueDate = due?.toString() ?: card.dueDate, payDay = if (card.payDay == 0) day else card.payDay))
    }

    companion object {
        const val DUPLICATE_WINDOW_MS = 3 * 60_000L
    }

    suspend fun setRoutinePending(on: Boolean) {
        context.forcedStore.edit { if (on) it[routinePendingKey] = LocalDate.now().toString() else it.remove(routinePendingKey) }
    }
}
