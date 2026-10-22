package com.adrian.minimallauncher.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.adrian.minimallauncher.feature.exercise.SetLog
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** How many records a backup wrote or an import added. */
data class BackupResult(val records: Int, val added: Int = records)

/**
 * Everything recorded in the launcher as one JSON file the user keeps (Drive, Downloads…).
 * Importing never deletes: rows already on the phone (same id) are kept and only missing ones are added,
 * so importing the same file twice changes nothing.
 */
class BackupRepository(private val context: Context, private val db: LauncherDatabase, private val settings: SettingsRepository) {
    private val personal = db.personal()
    private val exercise = db.exercise()

    suspend fun export(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        val notes = personal.allNotes()
        val reminders = personal.allReminders()
        val finances = personal.allFinances()
        val meals = personal.allMeals()
        val metrics = personal.allBodyMetrics()
        val checks = personal.allChecks()
        val sets = exercise.allSets()
        val root = JSONObject()
            .put("app", APP_ID)
            .put("format", FORMAT)
            .put("exportedAt", System.currentTimeMillis())
            .put("notes", JSONArray(notes.map { JSONObject().put("id", it.id).put("category", it.category.name).put("text", it.text).put("createdAt", it.createdAt).put("pinned", it.pinned) }))
            .put("reminders", JSONArray(reminders.map { JSONObject().put("id", it.id).put("title", it.title).put("hour", it.hour).put("minute", it.minute).put("spaceName", it.spaceName) }))
            .put("finances", JSONArray(finances.map { JSONObject().put("id", it.id).put("amountCents", it.amountCents).put("description", it.description).put("createdAt", it.createdAt) }))
            .put("meals", JSONArray(meals.map { JSONObject().put("id", it.id).put("description", it.description).put("createdAt", it.createdAt) }))
            .put("bodyMetrics", JSONArray(metrics.map { JSONObject().put("id", it.id).put("type", it.type).put("value", it.value.toDouble()).put("createdAt", it.createdAt) }))
            .put("dayChecks", JSONArray(checks.map { JSONObject().put("key", it.key).put("kind", it.kind) }))
            .put("setLogs", JSONArray(sets.map { JSONObject().put("date", it.date).put("exerciseId", it.exerciseId).put("setIndex", it.setIndex).put("weightKg", it.weightKg.toDouble()).put("reps", it.reps).put("createdAt", it.createdAt) }))
            .put("moduleSettings", settings.moduleSettings.first().toJson())
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(root.toString(2).toByteArray(Charsets.UTF_8)) }
            ?: error("No se pudo escribir el archivo")
        settings.setLastBackupAt(System.currentTimeMillis())
        BackupResult(notes.size + reminders.size + finances.size + meals.size + metrics.size + checks.size + sets.size)
    }

    suspend fun import(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use { String(it.readBytes(), Charsets.UTF_8) } ?: error("No se pudo leer el archivo")
        val root = JSONObject(text)
        require(root.optString("app") == APP_ID) { "El archivo no es un respaldo del launcher" }

        val before = count()
        val sets = root.array("setLogs").map { SetLog(date = it.getString("date"), exerciseId = it.getString("exerciseId"), setIndex = it.getInt("setIndex"), weightKg = it.getDouble("weightKg").toFloat(), reps = it.getInt("reps"), createdAt = it.getLong("createdAt")) }
        var setsAdded = 0
        db.withTransaction {
            personal.insertNotes(root.array("notes").map { NoteRecord(it.getString("id"), runCatching { NoteCategory.valueOf(it.getString("category")) }.getOrDefault(NoteCategory.GENERAL), it.getString("text"), it.getLong("createdAt"), it.optBoolean("pinned")) })
            personal.insertReminders(root.array("reminders").map { ReminderRecord(it.getString("id"), it.getString("title"), it.getInt("hour"), it.getInt("minute"), it.getString("spaceName")) })
            personal.insertFinances(root.array("finances").map { FinanceRecord(it.getString("id"), it.getLong("amountCents"), it.getString("description"), it.getLong("createdAt")) })
            personal.insertMeals(root.array("meals").map { MealRecord(it.getString("id"), it.getString("description"), it.getLong("createdAt")) })
            personal.insertBodyMetrics(root.array("bodyMetrics").map { BodyMetricRecord(it.getString("id"), it.getString("type"), it.getDouble("value").toFloat(), it.getLong("createdAt")) })
            personal.insertChecks(root.array("dayChecks").map { DayCheck(it.getString("key"), it.getString("kind")) })
            personal.keepNewestPin()
            // Set ids are local counters, so a set counts as the same when its day, exercise and position match.
            sets.forEach { set ->
                if (exercise.countSet(set.date, set.exerciseId, set.setIndex) == 0) {
                    exercise.insert(set)
                    setsAdded++
                }
            }
        }
        root.optJSONObject("moduleSettings")?.let { settings.setModuleSettings(it.toModuleSettings()) }
        val total = root.keys().asSequence().sumOf { key -> root.optJSONArray(key)?.length() ?: 0 }
        BackupResult(records = total, added = count() - before)
    }

    private suspend fun count(): Int = personal.allNotes().size + personal.allReminders().size + personal.allFinances().size +
        personal.allMeals().size + personal.allBodyMetrics().size + personal.allChecks().size + exercise.allSets().size

    private fun JSONObject.array(name: String): List<JSONObject> {
        val array = optJSONArray(name) ?: return emptyList()
        return (0 until array.length()).map { array.getJSONObject(it) }
    }

    private fun ModuleSettings.toJson() = JSONObject()
        .put("exerciseStart", exerciseStart.toString())
        .put("exerciseRestSeconds", exerciseRestSeconds)
        .put("vaultLockSeconds", vaultLockSeconds)
        .put("hiddenTiles", JSONArray(hiddenTiles.toList()))
        .put("meals", JSONArray(meals.map { JSONObject().put("id", it.id).put("name", it.name).put("time", it.time.toString()).put("kind", it.kind.name).put("icon", it.icon) }))
        .put("waterGoal", waterGoal)
        .put("waterGlassMl", waterGlassMl)
        .put("waterContainer", waterContainer)
        .put("dailyBudgetCents", dailyBudgetCents)
        .put("worldZones", JSONArray(worldZones))
        .put("haptics", haptics)

    private fun JSONObject.toModuleSettings(): ModuleSettings {
        val default = ModuleSettings()
        fun strings(name: String) = optJSONArray(name)?.let { a -> (0 until a.length()).map { a.getString(it) } }
        return ModuleSettings(
            exerciseStart = runCatching { LocalTime.parse(getString("exerciseStart")) }.getOrDefault(default.exerciseStart),
            exerciseRestSeconds = optInt("exerciseRestSeconds", default.exerciseRestSeconds),
            vaultLockSeconds = optInt("vaultLockSeconds", default.vaultLockSeconds),
            hiddenTiles = strings("hiddenTiles")?.toSet() ?: default.hiddenTiles,
            // Older backups only have the four meal times.
            meals = runCatching {
                array("meals").map { MealSlot(it.getString("id"), it.getString("name"), LocalTime.parse(it.getString("time")), MealKind.valueOf(it.getString("kind")), it.getString("icon")) }
            }.getOrNull()?.takeIf { it.isNotEmpty() }
                ?: strings("mealTimes")?.mapNotNull { runCatching { LocalTime.parse(it) }.getOrNull() }?.takeIf { it.size == DEFAULT_MEAL_TIMES.size }?.let { defaultMeals(it) }
                ?: default.meals,
            waterGoal = optInt("waterGoal", default.waterGoal),
            waterGlassMl = optInt("waterGlassMl", default.waterGlassMl),
            waterContainer = optString("waterContainer", default.waterContainer),
            dailyBudgetCents = optLong("dailyBudgetCents", default.dailyBudgetCents),
            worldZones = strings("worldZones") ?: default.worldZones,
            haptics = optBoolean("haptics", default.haptics),
        )
    }

    companion object {
        private const val APP_ID = "minimal-launcher"
        private const val FORMAT = 1
    }
}
