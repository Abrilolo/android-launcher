package com.adrian.minimallauncher.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.adrian.minimallauncher.feature.exercise.ExerciseDao
import com.adrian.minimallauncher.feature.exercise.GuidedExerciseDao
import com.adrian.minimallauncher.feature.exercise.GuidedExerciseRecord
import com.adrian.minimallauncher.feature.exercise.SetLog
import com.adrian.minimallauncher.feature.exercise.StackDao
import com.adrian.minimallauncher.feature.exercise.StackExercise
import com.adrian.minimallauncher.feature.finance.BudgetDao
import com.adrian.minimallauncher.feature.finance.BudgetLimit
import com.adrian.minimallauncher.feature.food.FoodDao
import com.adrian.minimallauncher.feature.food.Ingredient
import com.adrian.minimallauncher.feature.food.MealItem
import com.adrian.minimallauncher.feature.food.Recipe
import com.adrian.minimallauncher.feature.food.RecipeItem
import kotlinx.coroutines.flow.Flow

/** Everything the user records lives here; preferences stay in [SettingsRepository]. */
@Database(
    entities = [
        NoteRecord::class, ReminderRecord::class, FinanceRecord::class, MealRecord::class, BodyMetricRecord::class, DayCheck::class, SetLog::class,
        Recipe::class, Ingredient::class, RecipeItem::class, MealItem::class, BudgetLimit::class, PendingPayment::class, StackExercise::class,
        GuidedExerciseRecord::class,
    ],
    version = 6,
    exportSchema = true,
)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun personal(): PersonalDao
    abstract fun exercise(): ExerciseDao
    abstract fun food(): FoodDao
    abstract fun budgets(): BudgetDao
    abstract fun pending(): PendingDao
    abstract fun stack(): StackDao
    abstract fun guidedExercise(): GuidedExerciseDao

    companion object {
        @Volatile private var instance: LauncherDatabase? = null

        fun get(context: Context): LauncherDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, LauncherDatabase::class.java, "launcher.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .build()
                .also { instance = it }
        }

        /** Version 6 stores resumable guided sessions and editable exercise choices. */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `guided_exercise_records` (`id` TEXT NOT NULL, `payload` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        /** Version 2 adds pinned notes; existing notes start unpinned. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Version 3 (routines and linked records): new columns default so every saved row keeps its meaning. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_2_3_SQL.forEach(db::execSQL)
            }
        }

        /**
         * Version 4: reminders belong to a phase. The old space name is copied into phaseId as it was, and on
         * load the launcher maps it to the phase that came from that space (see PhaseReminders.phaseIdFor).
         * The table is rebuilt because SQLite only renames columns from Android 11.
         */
        /** Version 5: a meal can say which meal slot it was for; earlier meals have none. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `meals` ADD COLUMN `slotId` TEXT")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `reminders_new` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `hour` INTEGER NOT NULL, `minute` INTEGER NOT NULL, `phaseId` TEXT NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("INSERT INTO `reminders_new` (`id`, `title`, `hour`, `minute`, `phaseId`) SELECT `id`, `title`, `hour`, `minute`, `spaceName` FROM `reminders`")
                db.execSQL("DROP TABLE `reminders`")
                db.execSQL("ALTER TABLE `reminders_new` RENAME TO `reminders`")
            }
        }
    }
}

/** Copied from Room's exported schema 3 (app/schemas), so the migrated tables match what Room expects. */
internal val MIGRATION_2_3_SQL = listOf(
    // Notes get a done mark; finances a kind (old spending is EXTRA), category and what others owe; meals their totals.
    "ALTER TABLE `notes` ADD COLUMN `done` INTEGER NOT NULL DEFAULT 0",
    "ALTER TABLE `finances` ADD COLUMN `kind` TEXT NOT NULL DEFAULT 'EXTRA'",
    "ALTER TABLE `finances` ADD COLUMN `category` TEXT NOT NULL DEFAULT ''",
    "ALTER TABLE `finances` ADD COLUMN `pendingFromOthersCents` INTEGER NOT NULL DEFAULT 0",
    "ALTER TABLE `finances` ADD COLUMN `mealId` TEXT",
    "ALTER TABLE `meals` ADD COLUMN `recipeId` TEXT",
    "ALTER TABLE `meals` ADD COLUMN `servings` REAL NOT NULL DEFAULT 1",
    "ALTER TABLE `meals` ADD COLUMN `kcal` REAL NOT NULL DEFAULT 0",
    "ALTER TABLE `meals` ADD COLUMN `proteinG` REAL NOT NULL DEFAULT 0",
    "ALTER TABLE `meals` ADD COLUMN `carbsG` REAL NOT NULL DEFAULT 0",
    "ALTER TABLE `meals` ADD COLUMN `fatG` REAL NOT NULL DEFAULT 0",
    "ALTER TABLE `meals` ADD COLUMN `fiberG` REAL NOT NULL DEFAULT 0",
    "CREATE TABLE IF NOT EXISTS `recipes` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `photoUri` TEXT, `steps` TEXT NOT NULL, `prepMinutes` INTEGER NOT NULL, `servings` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
    "CREATE TABLE IF NOT EXISTS `ingredients` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `kcal` REAL NOT NULL, `proteinG` REAL NOT NULL, `carbsG` REAL NOT NULL, `fatG` REAL NOT NULL, `fiberG` REAL NOT NULL, `vitamins` TEXT NOT NULL, `portionGrams` REAL NOT NULL, `portionName` TEXT NOT NULL, `source` TEXT NOT NULL, `fdcId` INTEGER, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
    "CREATE TABLE IF NOT EXISTS `recipe_items` (`recipeId` TEXT NOT NULL, `ingredientId` TEXT NOT NULL, `grams` REAL NOT NULL, PRIMARY KEY(`recipeId`, `ingredientId`))",
    "CREATE TABLE IF NOT EXISTS `meal_items` (`mealId` TEXT NOT NULL, `ingredientId` TEXT NOT NULL, `grams` REAL NOT NULL, PRIMARY KEY(`mealId`, `ingredientId`))",
    "CREATE TABLE IF NOT EXISTS `budgets` (`id` TEXT NOT NULL, `topic` TEXT NOT NULL, `period` TEXT NOT NULL, `limitCents` INTEGER NOT NULL, PRIMARY KEY(`id`))",
    "CREATE TABLE IF NOT EXISTS `pending_payments` (`id` TEXT NOT NULL, `amountCents` INTEGER NOT NULL, `source` TEXT NOT NULL, `merchant` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `resolvedAt` INTEGER, `financeId` TEXT, PRIMARY KEY(`id`))",
    "CREATE TABLE IF NOT EXISTS `stack_exercises` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `muscleGroup` TEXT NOT NULL, `met` REAL NOT NULL, `minutes` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, `placeholder` INTEGER NOT NULL, PRIMARY KEY(`id`))",
)

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
    @Upsert suspend fun upsertFinance(row: FinanceRecord)
    @Upsert suspend fun upsertReminder(row: ReminderRecord)
    @Upsert suspend fun upsertMeal(row: MealRecord)

    @Query("DELETE FROM notes WHERE id = :id") suspend fun deleteNote(id: String)
    @Query("DELETE FROM reminders WHERE id = :id") suspend fun deleteReminder(id: String)
    @Query("DELETE FROM finances WHERE id = :id") suspend fun deleteFinance(id: String)
    @Query("DELETE FROM meals WHERE id = :id") suspend fun deleteMeal(id: String)
    @Query("DELETE FROM body_metrics WHERE id = :id") suspend fun deleteBodyMetric(id: String)
    @Query("DELETE FROM day_checks WHERE `key` = :key") suspend fun deleteCheck(key: String)
    @Query("DELETE FROM day_checks WHERE kind = :kind") suspend fun deleteChecksByKind(kind: String)
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
    @Query("UPDATE notes SET done = :done WHERE id = :id") suspend fun setNoteDone(id: String, done: Boolean)

    @Query("SELECT * FROM finances WHERE id = :id") suspend fun finance(id: String): FinanceRecord?
    @Query("UPDATE finances SET pendingFromOthersCents = :cents WHERE id = :id") suspend fun setPendingFromOthers(id: String, cents: Long)
    @Query("SELECT * FROM meals WHERE id = :id") suspend fun meal(id: String): MealRecord?
}
