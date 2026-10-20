package com.adrian.minimallauncher.feature.exercise

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** One finished set: what was lifted and how many reps (or seconds for timed exercises). */
@Entity(tableName = "set_logs", indices = [Index("date"), Index("exerciseId")])
data class SetLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val exerciseId: String,
    val setIndex: Int,
    val weightKg: Float,
    val reps: Int,
    val createdAt: Long = System.currentTimeMillis(),
)

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM set_logs WHERE date = :date ORDER BY exerciseId, setIndex")
    fun setsOn(date: String): Flow<List<SetLog>>

    /** The most recent set of every exercise before [date], to suggest the next load. */
    @Query("SELECT * FROM set_logs WHERE id IN (SELECT MAX(id) FROM set_logs WHERE date < :date GROUP BY exerciseId)")
    fun lastBefore(date: String): Flow<List<SetLog>>

    @Query("SELECT DISTINCT date FROM set_logs WHERE date >= :from")
    fun activeDates(from: String): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM set_logs WHERE date = :date")
    suspend fun countOn(date: String): Int

    @Insert
    suspend fun insert(log: SetLog)

    @Query("DELETE FROM set_logs WHERE date = :date AND exerciseId = :exerciseId AND setIndex >= :fromIndex")
    suspend fun deleteFrom(date: String, exerciseId: String, fromIndex: Int)
}
