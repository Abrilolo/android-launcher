package com.adrian.minimallauncher.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * A payment or income seen outside the launcher (a bank notification, or a test one from debug) that still has
 * to be registered. While any row has no [resolvedAt], the forced registration flow keeps the phone on the launcher.
 * [financeId] (and the finance's meal) are filled when the flow saves it.
 */
@Entity(tableName = "pending_payments")
data class PendingPayment(
    @PrimaryKey val id: String,
    val amountCents: Long,
    /** Where it came from: a bank app's package, or "debug". */
    val source: String,
    val merchant: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null,
    val financeId: String? = null,
    /** [OUT] money spent, [IN] money received (asks what kind of income it was instead of "¿comida?"). */
    @ColumnInfo(defaultValue = OUT) val direction: String = OUT,
    /** The account the notice was matched to (card digits, or the bank's card or debit); empty when unknown. */
    @ColumnInfo(defaultValue = "") val account: String = "",
) {
    val isIncome: Boolean get() = direction == IN

    companion object {
        const val SOURCE_DEBUG = "debug"
        const val OUT = "OUT"
        const val IN = "IN"
        /**
         * A card said it received a payment (Plata "Recibimos tu pago"). Stored already closed, so the debit's own
         * notice of the same amount that comes after it is taken as that payment and not as spending.
         */
        const val CARD = "CARD"
        const val CARD_MATCH_MS = 3 * 24 * 60 * 60_000L
    }
}

@Dao
interface PendingDao {
    @Query("SELECT * FROM pending_payments WHERE resolvedAt IS NULL ORDER BY createdAt") fun open(): Flow<List<PendingPayment>>
    @Query("SELECT COUNT(*) FROM pending_payments WHERE resolvedAt IS NULL") suspend fun openCount(): Int
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(rows: List<PendingPayment>)
    @Query("UPDATE pending_payments SET resolvedAt = :at, financeId = :financeId WHERE id = :id") suspend fun resolve(id: String, financeId: String?, at: Long?)
    @Query("SELECT * FROM pending_payments") suspend fun all(): List<PendingPayment>
    /**
     * Rows of the same amount and direction from another app since [since]: a second notice of one payment
     * (wallet + bank). The same app twice is two real payments.
     */
    @Query("SELECT COUNT(*) FROM pending_payments WHERE amountCents = :amountCents AND direction = :direction AND source != :source AND createdAt >= :since")
    suspend fun countSimilar(amountCents: Long, direction: String, source: String, since: Long): Int
    /** The open other half of a move between the user's own accounts: same amount, other direction, another app, any time. */
    @Query("SELECT * FROM pending_payments WHERE resolvedAt IS NULL AND amountCents = :amountCents AND direction != :direction AND source != :source ORDER BY createdAt DESC LIMIT 1")
    suspend fun mirror(amountCents: Long, direction: String, source: String): PendingPayment?

    /** An open outgoing notice of [amountCents] from another app since [since]: the debit side of a card payment. */
    @Query("SELECT * FROM pending_payments WHERE resolvedAt IS NULL AND direction = 'OUT' AND amountCents = :amountCents AND source != :source AND createdAt >= :since ORDER BY createdAt DESC LIMIT 1")
    suspend fun openOut(amountCents: Long, source: String, since: Long): PendingPayment?

    /** A card payment notice of [amountCents] since [since] whose debit side has not come yet. */
    @Query("SELECT * FROM pending_payments WHERE direction = 'CARD' AND financeId IS NULL AND amountCents = :amountCents AND source != :source AND createdAt >= :since ORDER BY createdAt DESC LIMIT 1")
    suspend fun unmatchedCardPayment(amountCents: Long, source: String, since: Long): PendingPayment?

    @Query("UPDATE pending_payments SET financeId = :tag WHERE id = :id") suspend fun tag(id: String, tag: String)
}
