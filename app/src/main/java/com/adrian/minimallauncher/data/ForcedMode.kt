package com.adrian.minimallauncher.data

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

    /** A payment seen outside the launcher; the bank notification parser will call this later. */
    suspend fun addPayment(amountCents: Long, source: String, merchant: String = ""): String {
        val id = newId()
        pending.insert(listOf(PendingPayment(id, amountCents, source, merchant)))
        return id
    }

    /** Debug stand-in for a bank notification. */
    suspend fun addTestPayment(amountCents: Long = 8500, merchant: String = "OXXO") = addPayment(amountCents, PendingPayment.SOURCE_DEBUG, merchant)

    /**
     * Saves the payment's expense. Not food: the payment is done. Food: it stays open with the expense linked
     * until [saveMeal]. Returns the finance id.
     */
    suspend fun saveExpense(payment: PendingPayment, amountCents: Long, description: String, category: String, isFood: Boolean, kind: MoneyKind = MoneyKind.EXTRA): String =
        db.withTransaction {
            val financeId = newId()
            personal.insertFinances(listOf(FinanceRecord(financeId, amountCents, description.ifBlank { payment.merchant }, System.currentTimeMillis(), kind, category.trim().lowercase())))
            // A food payment keeps resolvedAt empty: it stays open, now with its expense, until the meal is saved.
            pending.resolve(payment.id, financeId, if (isFood) null else System.currentTimeMillis())
            financeId
        }

    /** The meal for a food payment was registered: link it to the expense and close the payment. */
    suspend fun saveMeal(payment: PendingPayment, mealId: String) = db.withTransaction {
        payment.financeId?.let { id -> personal.finance(id)?.let { personal.upsertFinance(it.copy(mealId = mealId)) } }
        pending.resolve(payment.id, payment.financeId, System.currentTimeMillis())
    }

    suspend fun setRoutinePending(on: Boolean) {
        context.forcedStore.edit { if (on) it[routinePendingKey] = LocalDate.now().toString() else it.remove(routinePendingKey) }
    }
}
