package com.adrian.minimallauncher.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.adrian.minimallauncher.feature.exercise.ExerciseDao
import com.adrian.minimallauncher.feature.exercise.SetLog
import kotlinx.coroutines.flow.Flow

/** Everything the user records lives here; preferences stay in [SettingsRepository]. */
@Database(
    entities = [NoteRecord::class, ReminderRecord::class, FinanceRecord::class, MealRecord::class, BodyMetricRecord::class, DayCheck::class, SetLog::class],
    version = 2,
    exportSchema = false,
)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun personal(): PersonalDao
    abstract fun exercise(): ExerciseDao

    companion object {
        @Volatile private var instance: LauncherDatabase? = null

        fun get(context: Context): LauncherDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, LauncherDatabase::class.java, "launcher.db")
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { instance = it }
        }

        /** Version 2 adds pinned notes; existing notes start unpinned. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}

@Dao
interface PersonalDao {
    @Query("SELECT * FROM notes ORDER BY createdAt DESC") fun notes(): Flow<List<NoteRecord>>
    @Query("SELECT * FROM reminders ORDER BY hour, minute") fun reminders(): Flow<List<ReminderRecord>>
    @Query("SELECT * FROM finances ORDER BY createdAt DESC") fun finances(): Flow<List<FinanceRecord>>
    @Query("SELECT * FROM meals ORDER BY createdAt DESC") fun meals(): Flow<List<MealRecord>>
    @Query("SELECT * FROM body_metrics ORDER BY createdAt DESC") fun bodyMetrics(): Flow<List<BodyMetricRecord>>
    @Query("SELECT `key` FROM day_checks WHERE kind = :kind") fun checks(kind: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertNotes(rows: List<NoteRecord>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertReminders(rows: List<ReminderRecord>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertFinances(rows: List<FinanceRecord>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertMeals(rows: List<MealRecord>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertBodyMetrics(rows: List<BodyMetricRecord>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertChecks(rows: List<DayCheck>)

    @Query("DELETE FROM notes WHERE id = :id") suspend fun deleteNote(id: String)
    @Query("DELETE FROM reminders WHERE id = :id") suspend fun deleteReminder(id: String)
    @Query("DELETE FROM finances WHERE id = :id") suspend fun deleteFinance(id: String)
    @Query("DELETE FROM meals WHERE id = :id") suspend fun deleteMeal(id: String)
    @Query("DELETE FROM body_metrics WHERE id = :id") suspend fun deleteBodyMetric(id: String)
    @Query("DELETE FROM day_checks WHERE `key` = :key") suspend fun deleteCheck(key: String)
    @Query("SELECT COUNT(*) FROM day_checks WHERE `key` = :key") suspend fun hasCheck(key: String): Int

    // One-shot reads for the backup file.
    @Query("SELECT * FROM notes") suspend fun allNotes(): List<NoteRecord>
    @Query("SELECT * FROM reminders") suspend fun allReminders(): List<ReminderRecord>
    @Query("SELECT * FROM finances") suspend fun allFinances(): List<FinanceRecord>
    @Query("SELECT * FROM meals") suspend fun allMeals(): List<MealRecord>
    @Query("SELECT * FROM body_metrics") suspend fun allBodyMetrics(): List<BodyMetricRecord>
    @Query("SELECT * FROM day_checks") suspend fun allChecks(): List<DayCheck>

    /** After an import two notes may be pinned; keep only the most recent one. */
    @Query("UPDATE notes SET pinned = (id = (SELECT id FROM notes WHERE pinned = 1 ORDER BY createdAt DESC LIMIT 1))")
    suspend fun keepNewestPin()

    /** Pins [id] and unpins every other note in one statement. */
    @Query("UPDATE notes SET pinned = (id = :id)") suspend fun pinOnly(id: String)
    @Query("UPDATE notes SET pinned = 0 WHERE id = :id") suspend fun unpin(id: String)
}
