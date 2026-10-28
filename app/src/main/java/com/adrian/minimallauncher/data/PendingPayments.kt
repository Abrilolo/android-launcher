package com.adrian.minimallauncher.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * A payment seen outside the launcher (a bank notification, or a test one from debug) that still has to be
 * registered. While any row has no [resolvedAt], the forced registration flow keeps the phone on the launcher.
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
) {
    companion object {
        const val SOURCE_DEBUG = "debug"
    }
}

@Dao
interface PendingDao {
    @Query("SELECT * FROM pending_payments WHERE resolvedAt IS NULL ORDER BY createdAt") fun open(): Flow<List<PendingPayment>>
    @Query("SELECT COUNT(*) FROM pending_payments WHERE resolvedAt IS NULL") suspend fun openCount(): Int
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(rows: List<PendingPayment>)
    @Query("UPDATE pending_payments SET resolvedAt = :at, financeId = :financeId WHERE id = :id") suspend fun resolve(id: String, financeId: String?, at: Long?)
    @Query("SELECT * FROM pending_payments") suspend fun all(): List<PendingPayment>
}
