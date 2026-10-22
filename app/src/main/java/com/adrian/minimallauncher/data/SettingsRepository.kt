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
    /** The day's meals, in order; the user can add, remove and edit them. */
    val meals: List<MealSlot> = DEFAULT_MEALS,
    /** How many containers of water a day, how much each holds and what it is ("vaso" or "termo"). */
    val waterGoal: Int = 8,
    val waterGlassMl: Int = 250,
    val waterContainer: String = "vaso",
    /** Daily spending limit; 0 means no budget. */
    val dailyBudgetCents: Long = 0,
    /** Cities shown on the world clock, in order; the first two go on the home tile. */
    val worldZones: List<String> = DEFAULT_WORLD_ZONES,
    /** Short vibrations when marking, saving or finishing something. */
    val haptics: Boolean = true,
) {
    val mealTimes: List<LocalTime> get() = meals.map { it.time }
}

/** Main meals are drawn bigger than snacks. */
enum class MealKind { MAIN, SNACK }

/**
 * One meal of the day. [id] keys its "done" mark ("<date>|meal_<id>"), so renaming or moving it keeps the history;
 * the four default ids are "0".."3", the keys the meals had when they were fixed. [icon] is a DotIcons name.
 */
data class MealSlot(val id: String, val name: String, val time: LocalTime, val kind: MealKind, val icon: String)

val DEFAULT_MEAL_TIMES: List<LocalTime> = listOf(LocalTime.of(13, 0), LocalTime.of(16, 30), LocalTime.of(18, 30), LocalTime.of(21, 30))

/** The Tokyo guide's four meals, with the user's names. */
fun defaultMeals(times: List<LocalTime> = DEFAULT_MEAL_TIMES) = listOf(
    MealSlot("0", "Comida completa", times[0], MealKind.MAIN, "comida"),
    MealSlot("1", "Proteína + energía", times[1], MealKind.SNACK, "snack"),
    MealSlot("2", "Colación preentreno", times[2], MealKind.SNACK, "colacion"),
    MealSlot("3", "Cena post entreno", times[3], MealKind.MAIN, "cena"),
)
val DEFAULT_MEALS = defaultMeals()
val DEFAULT_WORLD_ZONES = listOf("America/Mexico_City", "America/New_York", "Europe/Madrid", "Europe/London", "Asia/Tokyo")

/**
 * A widget, or a stack of widgets swiped through, on a widget page. It sits at [col],[row] of the
 * 4-column grid and covers [w]×[h] cells; -1/0 mean "not placed yet" (layouts saved before the grid).
 * [span] (1 = half width, 2 = full width) is only read to place those old layouts.
 */
data class WidgetStackConfig(
    val id: String,
    val widgetIds: List<String>,
    val span: Int = 1,
    val col: Int = -1,
    val row: Int = -1,
    val w: Int = 0,
    val h: Int = 0,
)

val DEFAULT_WIDGET_LAYOUT = listOf(
    WidgetStackConfig("stack_alarms", listOf("alarms"), span = 1),
    WidgetStackConfig("stack_exercise", listOf("exercise"), span = 1),
    WidgetStackConfig("stack_finance", listOf("finance"), span = 1),
    WidgetStackConfig("stack_diet", listOf("diet"), span = 1),
    WidgetStackConfig("stack_world", listOf("world"), span = 1),
    WidgetStackConfig("stack_vault", listOf("vault"), span = 1),
    WidgetStackConfig("stack_notes", listOf("notes"), span = 2),
)

/** What a home page shows: the week summary, the app list, or a grid of widgets. */
enum class PageKind {
    WEEK, APPS, WIDGETS;

    /** Apps has widgets above its list; Semana is a fixed summary. */
    val hasWidgets: Boolean get() = this != WEEK
}

data class HomePage(val id: String, val kind: PageKind)

/** The home pages from left to right, and [mainId], the one the home button goes back to. */
data class HomePages(val pages: List<HomePage>, val mainId: String) {
    val mainIndex: Int get() = pages.indexOfFirst { it.id == mainId }.coerceAtLeast(0)
}

const val FIRST_WIDGET_PAGE = "widgets_1"
const val APPS_PAGE = "apps"
val DEFAULT_HOME_PAGES = HomePages(
    listOf(HomePage("week", PageKind.WEEK), HomePage(APPS_PAGE, PageKind.APPS), HomePage(FIRST_WIDGET_PAGE, PageKind.WIDGETS)),
    mainId = APPS_PAGE,
)

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
    private val mealSlotsKey = stringPreferencesKey("meals")
    private val waterGlassMlKey = intPreferencesKey("water_glass_ml")
    private val waterContainerKey = stringPreferencesKey("water_container")
    private val dailyBudgetKey = longPreferencesKey("daily_budget_cents")
    private val worldZonesKey = stringPreferencesKey("world_zones")
    private val hapticsKey = booleanPreferencesKey("haptics")
    private val lastBackupKey = longPreferencesKey("last_backup_at")
    // Single-page layout from before home pages existed; read once as the first widget page.
    private val widgetLayoutKey = stringPreferencesKey("widget_layout")
    private val widgetLayoutsKey = stringPreferencesKey("widget_layouts")
    private val homePagesKey = stringPreferencesKey("home_pages")
    private val homeMainKey = stringPreferencesKey("home_main")

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
    val homePages = context.settings.data.map { prefs ->
        val pages = prefs[homePagesKey]?.split(',')?.mapNotNull { entry ->
            val (id, kind) = entry.split(':').takeIf { it.size == 2 } ?: return@mapNotNull null
            runCatching { HomePage(id, PageKind.valueOf(kind)) }.getOrNull()
        }?.takeIf { pages -> pages.any { it.kind == PageKind.APPS } } ?: DEFAULT_HOME_PAGES.pages
        val main = prefs[homeMainKey]?.takeIf { id -> pages.any { it.id == id } } ?: DEFAULT_HOME_PAGES.mainId
        HomePages(pages, main)
    }

    /** Widget stacks of every page that has widgets, by page id. A page with no entry is empty, except Apps, which starts with the clock. */
    val widgetLayouts = context.settings.data.map { prefs ->
        val saved = prefs[widgetLayoutsKey]
        val layouts = if (saved == null) {
            mapOf(FIRST_WIDGET_PAGE to decodeWidgetLayout(prefs[widgetLayoutKey]))
        } else {
            saved.split('|').filter(String::isNotBlank).associate { line ->
                val pageId = decode(line.substringBefore('='))
                pageId to decodeStacks(line.substringAfter('=', ""))
            }
        }
        if (APPS_PAGE in layouts) layouts else layouts + (APPS_PAGE to APPS_DEFAULT_LAYOUT)
    }
    val moduleSettings = context.settings.data.map { prefs ->
        val default = ModuleSettings()
        ModuleSettings(
            exerciseStart = prefs[exerciseStartKey]?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: default.exerciseStart,
            exerciseRestSeconds = prefs[exerciseRestKey] ?: default.exerciseRestSeconds,
            vaultLockSeconds = prefs[vaultLockKey] ?: default.vaultLockSeconds,
            hiddenTiles = prefs[hiddenTilesKey].orEmpty(),
            // Before meals could be edited only their four times were saved; those become the default meals' times.
            meals = prefs[mealSlotsKey]?.let(::decodeMeals)
                ?: prefs[mealTimesKey]?.split(',')?.mapNotNull { runCatching { LocalTime.parse(it) }.getOrNull() }
                    ?.takeIf { it.size == DEFAULT_MEAL_TIMES.size }?.let { defaultMeals(it) }
                ?: default.meals,
            waterGoal = prefs[waterGoalKey] ?: default.waterGoal,
            waterGlassMl = prefs[waterGlassMlKey] ?: default.waterGlassMl,
            waterContainer = prefs[waterContainerKey] ?: default.waterContainer,
            dailyBudgetCents = prefs[dailyBudgetKey] ?: default.dailyBudgetCents,
            worldZones = prefs[worldZonesKey]?.split(',')?.filter(String::isNotBlank) ?: default.worldZones,
            haptics = prefs[hapticsKey] ?: default.haptics,
        )
    }

    /** When the last backup file was written, or null if never. */
    val lastBackupAt = context.settings.data.map { it[lastBackupKey] }

    // Drops unknown ids and appends sections added after the order was saved.
    private fun normalizeOrder(saved: List<String>) = saved.filter { it in DEFAULT_SECTION_ORDER }.distinct().let { it + (DEFAULT_SECTION_ORDER - it.toSet()) }
    private fun encode(value: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))
    private fun decode(value: String) = String(Base64.getUrlDecoder().decode(value), Charsets.UTF_8)

    private fun encodeMeals(meals: List<MealSlot>) =
        meals.joinToString("|") { "${encode(it.id)}~${encode(it.name)}~${it.time}~${it.kind}~${it.icon}" }

    private fun decodeMeals(encoded: String): List<MealSlot>? = runCatching {
        encoded.split('|').filter(String::isNotBlank).map { row ->
            val p = row.split('~')
            MealSlot(decode(p[0]), decode(p[1]), LocalTime.parse(p[2]), MealKind.valueOf(p[3]), p[4])
        }
    }.getOrNull()

    private fun encodeWidgetLayout(layout: List<WidgetStackConfig>): String {
        return layout.joinToString(";") { stack ->
            "${encode(stack.id)}~${stack.span}~${stack.widgetIds.joinToString(",") { encode(it) }}~${stack.col}~${stack.row}~${stack.w}~${stack.h}"
        }
    }

    private fun decodeWidgetLayout(encoded: String?): List<WidgetStackConfig> =
        decodeStacks(encoded.orEmpty()).takeIf { it.isNotEmpty() } ?: DEFAULT_WIDGET_LAYOUT.placed()

    private fun decodeStacks(encoded: String): List<WidgetStackConfig> {
        if (encoded.isBlank()) return emptyList()
        return runCatching {
            encoded.split(';').mapNotNull { row ->
                val p = row.split('~')
                if (p.size < 3) null
                else {
                    val id = decode(p[0])
                    val span = p[1].toIntOrNull() ?: 1
                    val widgets = p[2].split(',').filter(String::isNotBlank).map { decode(it) }
                    // Grid position and size; missing in layouts saved before the grid.
                    val grid = p.drop(3).map { it.toIntOrNull() }
                    if (widgets.isEmpty()) null
                    else WidgetStackConfig(id, widgets, span, grid.getOrNull(0) ?: -1, grid.getOrNull(1) ?: -1, grid.getOrNull(2) ?: 0, grid.getOrNull(3) ?: 0)
                }
            }.placed()
        }.getOrNull().orEmpty()
    }

    suspend fun setWidgetLayouts(layouts: Map<String, List<WidgetStackConfig>>) {
        context.settings.edit { prefs ->
            // One "<pageId>=<stacks>" entry per page; '|' never appears in Base64url ids or stack encoding.
            prefs[widgetLayoutsKey] = layouts.entries.joinToString("|") { (pageId, stacks) -> "${encode(pageId)}=${encodeWidgetLayout(stacks)}" }
        }
    }

    suspend fun setHomePages(value: HomePages) {
        context.settings.edit { prefs ->
            prefs[homePagesKey] = value.pages.joinToString(",") { "${it.id}:${it.kind.name}" }
            prefs[homeMainKey] = value.mainId
        }
    }

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
            it[mealSlotsKey] = encodeMeals(value.meals)
            it[waterGoalKey] = value.waterGoal
            it[waterGlassMlKey] = value.waterGlassMl
            it[waterContainerKey] = value.waterContainer
            it[dailyBudgetKey] = value.dailyBudgetCents
            it[worldZonesKey] = value.worldZones.joinToString(",")
            it[hapticsKey] = value.haptics
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
