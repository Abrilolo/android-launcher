package com.adrian.minimallauncher.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Base64
import java.time.LocalTime

val DEFAULT_SECTION_ORDER = listOf("apps", "entertainment", "games", "all")
private val Context.settings by preferencesDataStore(name = "settings")

/** Per-module preferences edited from the Módulos page (and each module's own settings). */
data class ModuleSettings(
    val exerciseStart: LocalTime = LocalTime.of(20, 35),
    val exerciseRestSeconds: Int = 90,
    val vaultLockSeconds: Int = 45,
    /** Home tiles switched off ("alarms", "exercise", "finance", "diet", "world", "vault", "notes"). */
    val hiddenTiles: Set<String> = emptySet(),
    /** One time per meal slot of the Tokyo guide, in slot order. */
    val mealTimes: List<LocalTime> = DEFAULT_MEAL_TIMES,
    val waterGoal: Int = 8,
    /** Daily spending limit; 0 means no budget. */
    val dailyBudgetCents: Long = 0,
    /** Cities shown on the world clock, in order; the first two go on the home tile. */
    val worldZones: List<String> = DEFAULT_WORLD_ZONES,
)

val DEFAULT_MEAL_TIMES: List<LocalTime> = listOf(LocalTime.of(13, 0), LocalTime.of(16, 30), LocalTime.of(18, 30), LocalTime.of(21, 30))
val DEFAULT_WORLD_ZONES = listOf("America/Mexico_City", "America/New_York", "Europe/Madrid", "Europe/London", "Asia/Tokyo")

class SettingsRepository(private val context: Context) {
    private val spacesKey = stringPreferencesKey("spaces")
    private val hiddenSectionsKey = stringSetPreferencesKey("hidden_sections")
    private val pauseSecondsKey = intPreferencesKey("pause_seconds")
    private val sectionOrderKey = stringPreferencesKey("section_order")
    private val exerciseStartKey = stringPreferencesKey("exercise_start")
    private val exerciseRestKey = intPreferencesKey("exercise_rest_seconds")
    private val vaultLockKey = intPreferencesKey("vault_lock_seconds")
    private val hiddenTilesKey = stringSetPreferencesKey("hidden_tiles")
    private val mealTimesKey = stringPreferencesKey("meal_times")
    private val waterGoalKey = intPreferencesKey("water_goal")
    private val dailyBudgetKey = longPreferencesKey("daily_budget_cents")
    private val worldZonesKey = stringPreferencesKey("world_zones")
    private val lastBackupKey = longPreferencesKey("last_backup_at")

    // Records stored here before the Room database; kept untouched as a backup once copied.
    private val notesKey = stringSetPreferencesKey("notes")
    private val remindersKey = stringSetPreferencesKey("reminders")
    private val financesKey = stringSetPreferencesKey("finance_records")
    private val mealsKey = stringSetPreferencesKey("meal_records")
    private val bodyMetricsKey = stringSetPreferencesKey("body_metrics")
    private val workoutsKey = stringSetPreferencesKey("completed_workouts")
    private val habitsKey = stringSetPreferencesKey("completed_habits")
    private val recordsMigratedKey = booleanPreferencesKey("records_migrated_to_room")

    val spaces = context.settings.data.map { prefs ->
        prefs[spacesKey]?.split(';')?.mapNotNull { row -> runCatching {
            val p = row.split('~')
            Space(decode(p[0]), LocalTime.parse(p[1]), LocalTime.parse(p[2]), p[3].split(',').filter(String::isNotBlank).map(AppGroup::valueOf).toSet())
        }.getOrNull() }?.takeIf { it.size == Space.all.size } ?: Space.all
    }

    val hiddenSections = context.settings.data.map { it[hiddenSectionsKey].orEmpty() }
    val pauseSeconds = context.settings.data.map { it[pauseSecondsKey] ?: 30 }
    val sectionOrder = context.settings.data.map { prefs -> normalizeOrder(prefs[sectionOrderKey]?.split(',').orEmpty()) }
    val moduleSettings = context.settings.data.map { prefs ->
        val default = ModuleSettings()
        ModuleSettings(
            exerciseStart = prefs[exerciseStartKey]?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: default.exerciseStart,
            exerciseRestSeconds = prefs[exerciseRestKey] ?: default.exerciseRestSeconds,
            vaultLockSeconds = prefs[vaultLockKey] ?: default.vaultLockSeconds,
            hiddenTiles = prefs[hiddenTilesKey].orEmpty(),
            mealTimes = prefs[mealTimesKey]?.split(',')?.mapNotNull { runCatching { LocalTime.parse(it) }.getOrNull() }
                ?.takeIf { it.size == default.mealTimes.size } ?: default.mealTimes,
            waterGoal = prefs[waterGoalKey] ?: default.waterGoal,
            dailyBudgetCents = prefs[dailyBudgetKey] ?: default.dailyBudgetCents,
            worldZones = prefs[worldZonesKey]?.split(',')?.filter(String::isNotBlank) ?: default.worldZones,
        )
    }

    /** When the last backup file was written, or null if never. */
    val lastBackupAt = context.settings.data.map { it[lastBackupKey] }

    // Drops unknown ids and appends sections added after the order was saved.
    private fun normalizeOrder(saved: List<String>) = saved.filter { it in DEFAULT_SECTION_ORDER }.distinct().let { it + (DEFAULT_SECTION_ORDER - it.toSet()) }
    private fun encode(value: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))
    private fun decode(value: String) = String(Base64.getUrlDecoder().decode(value), Charsets.UTF_8)

    suspend fun setSpaces(spaces: List<Space>) {
        context.settings.edit { prefs -> prefs[spacesKey] = spaces.joinToString(";") { space ->
            "${encode(space.name)}~${space.startsAt}~${space.endsAt}~${space.availableGroups.joinToString(",") { it.name }}"
        } }
    }

    suspend fun setSectionVisible(section: String, visible: Boolean) {
        context.settings.edit { prefs ->
            val hidden = prefs[hiddenSectionsKey].orEmpty().toMutableSet()
            if (visible) hidden.remove(section) else hidden.add(section)
            prefs[hiddenSectionsKey] = hidden
        }
    }

    suspend fun moveSection(section: String, delta: Int) {
        context.settings.edit { prefs ->
            val order = normalizeOrder(prefs[sectionOrderKey]?.split(',').orEmpty()).toMutableList()
            val from = order.indexOf(section)
            val to = from + delta
            if (from < 0 || to !in order.indices) return@edit
            order.add(to, order.removeAt(from))
            prefs[sectionOrderKey] = order.joinToString(",")
        }
    }

    suspend fun setPauseSeconds(seconds: Int) { context.settings.edit { it[pauseSecondsKey] = seconds.coerceIn(0, 300) } }

    suspend fun setLastBackupAt(millis: Long) { context.settings.edit { it[lastBackupKey] = millis } }

    suspend fun setModuleSettings(value: ModuleSettings) {
        context.settings.edit {
            it[exerciseStartKey] = value.exerciseStart.toString()
            it[exerciseRestKey] = value.exerciseRestSeconds
            it[vaultLockKey] = value.vaultLockSeconds
            it[hiddenTilesKey] = value.hiddenTiles
            it[mealTimesKey] = value.mealTimes.joinToString(",")
            it[waterGoalKey] = value.waterGoal
            it[dailyBudgetKey] = value.dailyBudgetCents
            it[worldZonesKey] = value.worldZones.joinToString(",")
        }
    }

    /** Records saved before the database existed, or null once they have been copied. */
    suspend fun legacyRecords(): PersonalRecords? {
        val prefs = context.settings.data.first()
        if (prefs[recordsMigratedKey] == true) return null
        return PersonalRecords(
            notes = prefs[notesKey].orEmpty().mapNotNull { row ->
                runCatching { val p = row.split('|'); NoteRecord(p[0], NoteCategory.valueOf(p[1]), decode(p[2]), p[3].toLong()) }.getOrNull()
            },
            reminders = prefs[remindersKey].orEmpty().mapNotNull { row ->
                runCatching { val p = row.split('|'); ReminderRecord(p[0], decode(p[1]), p[2].toInt(), p[3].toInt(), decode(p[4])) }.getOrNull()
            },
            finances = prefs[financesKey].orEmpty().mapNotNull { row ->
                runCatching { val p = row.split('|'); FinanceRecord(p[0], p[1].toLong(), decode(p[2]), p[3].toLong()) }.getOrNull()
            },
            meals = prefs[mealsKey].orEmpty().mapNotNull { row ->
                runCatching { val p = row.split('|'); MealRecord(p[0], decode(p[1]), p[2].toLong()) }.getOrNull()
            },
            bodyMetrics = prefs[bodyMetricsKey].orEmpty().mapNotNull { row ->
                runCatching { val p = row.split('|'); BodyMetricRecord(p[0], p[1], p[2].toFloat(), p[3].toLong()) }.getOrNull()
            },
            completedWorkouts = prefs[workoutsKey].orEmpty(),
            completedHabits = prefs[habitsKey].orEmpty(),
        )
    }

    suspend fun markRecordsMigrated() { context.settings.edit { it[recordsMigratedKey] = true } }
}
