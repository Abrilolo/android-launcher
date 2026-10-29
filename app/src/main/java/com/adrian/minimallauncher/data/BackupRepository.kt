package com.adrian.minimallauncher.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.adrian.minimallauncher.data.routine.RoutineRepository
import com.adrian.minimallauncher.feature.exercise.SetLog
import com.adrian.minimallauncher.feature.exercise.GuidedExerciseRecord
import com.adrian.minimallauncher.feature.exercise.StackExercise
import com.adrian.minimallauncher.feature.finance.BudgetLimit
import com.adrian.minimallauncher.feature.finance.BudgetPeriod
import com.adrian.minimallauncher.feature.food.Ingredient
import com.adrian.minimallauncher.feature.food.MealItem
import com.adrian.minimallauncher.feature.food.Recipe
import com.adrian.minimallauncher.feature.food.RecipeItem
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
 *
 * Format 2 (database version 3) adds money kinds, meal totals, recipes, ingredients, budgets, pending payments,
 * the exercise stack and the routine; a format 1 file still imports, its rows taking the same defaults the
 * migration gives. Field names are stable because the web view reads this file as it is. Times are epoch millis.
 */
class BackupRepository(private val context: Context, private val db: LauncherDatabase, private val settings: SettingsRepository) {
    private val personal = db.personal()
    private val exercise = db.exercise()
    private val food = db.food()
    private val budgets = db.budgets()
    private val pending = db.pending()
    private val stack = db.stack()
    private val routine = RoutineRepository(context, settings)

    suspend fun export(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        val root = snapshot()
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(root.toString(2).toByteArray(Charsets.UTF_8)) }
            ?: error("No se pudo escribir el archivo")
        settings.setLastBackupAt(System.currentTimeMillis())
        BackupResult(RECORD_ARRAYS.sumOf { root.optJSONArray(it)?.length() ?: 0 })
    }

    /** The whole backup as JSON, the same object [export] writes. */
    suspend fun snapshot(): JSONObject = withContext(Dispatchers.IO) {
        val notes = personal.allNotes()
        val reminders = personal.allReminders()
        val finances = personal.allFinances()
        val meals = personal.allMeals()
        val metrics = personal.allBodyMetrics()
        val checks = personal.allChecks()
        val sets = exercise.allSets()
        JSONObject()
            .put("app", APP_ID)
            .put("format", FORMAT)
            .put("exportedAt", System.currentTimeMillis())
            .put("notes", JSONArray(notes.map { JSONObject().put("id", it.id).put("category", it.category.name).put("text", it.text).put("createdAt", it.createdAt).put("pinned", it.pinned).put("done", it.done) }))
            .put("reminders", JSONArray(reminders.map { JSONObject().put("id", it.id).put("title", it.title).put("hour", it.hour).put("minute", it.minute).put("phaseId", it.phaseId) }))
            .put("finances", JSONArray(finances.map {
                JSONObject().put("id", it.id).put("amountCents", it.amountCents).put("description", it.description).put("createdAt", it.createdAt)
                    .put("kind", it.kind.name).put("category", it.category).put("pendingFromOthersCents", it.pendingFromOthersCents).put("mealId", it.mealId.orNull())
            }))
            .put("meals", JSONArray(meals.map {
                JSONObject().put("id", it.id).put("description", it.description).put("createdAt", it.createdAt).put("recipeId", it.recipeId.orNull())
                    .put("servings", it.servings.toDouble()).put("kcal", it.kcal.toDouble()).put("proteinG", it.proteinG.toDouble())
                    .put("carbsG", it.carbsG.toDouble()).put("fatG", it.fatG.toDouble()).put("fiberG", it.fiberG.toDouble())
                    .put("slotId", it.slotId.orNull())
            }))
            .put("bodyMetrics", JSONArray(metrics.map { JSONObject().put("id", it.id).put("type", it.type).put("value", it.value.toDouble()).put("createdAt", it.createdAt) }))
            .put("dayChecks", JSONArray(checks.map { JSONObject().put("key", it.key).put("kind", it.kind) }))
            .put("setLogs", JSONArray(sets.map { JSONObject().put("date", it.date).put("exerciseId", it.exerciseId).put("setIndex", it.setIndex).put("weightKg", it.weightKg.toDouble()).put("reps", it.reps).put("createdAt", it.createdAt) }))
            .put("recipes", JSONArray(food.allRecipes().map { JSONObject().put("id", it.id).put("name", it.name).put("photoUri", it.photoUri.orNull()).put("steps", it.steps).put("prepMinutes", it.prepMinutes).put("servings", it.servings).put("createdAt", it.createdAt) }))
            .put("ingredients", JSONArray(food.allIngredients().map {
                JSONObject().put("id", it.id).put("name", it.name).put("kcal", it.kcal.toDouble()).put("proteinG", it.proteinG.toDouble()).put("carbsG", it.carbsG.toDouble())
                    .put("fatG", it.fatG.toDouble()).put("fiberG", it.fiberG.toDouble()).put("vitamins", it.vitamins).put("portionGrams", it.portionGrams.toDouble())
                    .put("portionName", it.portionName).put("source", it.source).put("fdcId", it.fdcId ?: JSONObject.NULL).put("updatedAt", it.updatedAt)
            }))
            .put("recipeItems", JSONArray(food.allRecipeItems().map { JSONObject().put("recipeId", it.recipeId).put("ingredientId", it.ingredientId).put("grams", it.grams.toDouble()) }))
            .put("mealItems", JSONArray(food.allMealItems().map { JSONObject().put("mealId", it.mealId).put("ingredientId", it.ingredientId).put("grams", it.grams.toDouble()) }))
            .put("budgets", JSONArray(budgets.all().map { JSONObject().put("id", it.id).put("topic", it.topic).put("period", it.period.name).put("limitCents", it.limitCents) }))
            .put("pendingPayments", JSONArray(pending.all().map {
                JSONObject().put("id", it.id).put("amountCents", it.amountCents).put("source", it.source).put("merchant", it.merchant).put("createdAt", it.createdAt)
                    .put("resolvedAt", it.resolvedAt ?: JSONObject.NULL).put("financeId", it.financeId.orNull())
            }))
            .put("exerciseStack", JSONArray(stack.all().map { JSONObject().put("id", it.id).put("name", it.name).put("muscleGroup", it.muscleGroup).put("met", it.met.toDouble()).put("minutes", it.minutes).put("sortOrder", it.sortOrder).put("placeholder", it.placeholder) }))
            .put("moduleSettings", settings.moduleSettings.first().toJson())
            .put("guidedExercise", JSONArray(db.guidedExercise().all().map { JSONObject().put("id", it.id).put("payload", it.payload).put("updatedAt", it.updatedAt) }))
            .put("routine", routine.toJson())
    }

    suspend fun import(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use { String(it.readBytes(), Charsets.UTF_8) } ?: error("No se pudo leer el archivo")
        val root = JSONObject(text)
        require(root.optString("app") == APP_ID) { "El archivo no es un respaldo del launcher" }

        val before = count()
        val sets = root.array("setLogs").map { SetLog(date = it.getString("date"), exerciseId = it.getString("exerciseId"), setIndex = it.getInt("setIndex"), weightKg = it.getDouble("weightKg").toFloat(), reps = it.getInt("reps"), createdAt = it.getLong("createdAt")) }
        db.withTransaction {
            root.array("guidedExercise").forEach {
                val id = it.getString("id")
                val payload = it.getString("payload")
                require(payload.length <= 2_000_000)
                JSONObject(payload)
                if (db.guidedExercise().get(id) == null) db.guidedExercise().put(GuidedExerciseRecord(id, payload, it.getLong("updatedAt")))
            }
            personal.insertNotes(root.array("notes").map { NoteRecord(it.getString("id"), runCatching { NoteCategory.valueOf(it.getString("category")) }.getOrDefault(NoteCategory.GENERAL), it.getString("text"), it.getLong("createdAt"), it.optBoolean("pinned"), it.optBoolean("done")) })
            // Formats 1 and 2 stored the space name; the launcher maps it to a phase on load.
            personal.insertReminders(root.array("reminders").map { ReminderRecord(it.getString("id"), it.getString("title"), it.getInt("hour"), it.getInt("minute"), it.optString("phaseId").ifBlank { it.optString("spaceName") }) })
            // Format 1 rows have no kind: they were all day-to-day spending.
            personal.insertFinances(root.array("finances").map {
                FinanceRecord(
                    it.getString("id"), it.getLong("amountCents"), it.getString("description"), it.getLong("createdAt"),
                    kind = runCatching { MoneyKind.valueOf(it.getString("kind")) }.getOrDefault(MoneyKind.EXTRA),
                    category = it.optString("category"), pendingFromOthersCents = it.optLong("pendingFromOthersCents"), mealId = it.stringOrNull("mealId"),
                )
            })
            personal.insertMeals(root.array("meals").map {
                MealRecord(
                    it.getString("id"), it.getString("description"), it.getLong("createdAt"), recipeId = it.stringOrNull("recipeId"),
                    servings = it.optDouble("servings", 1.0).toFloat(), kcal = it.optDouble("kcal", 0.0).toFloat(), proteinG = it.optDouble("proteinG", 0.0).toFloat(),
                    carbsG = it.optDouble("carbsG", 0.0).toFloat(), fatG = it.optDouble("fatG", 0.0).toFloat(), fiberG = it.optDouble("fiberG", 0.0).toFloat(),
                    slotId = it.stringOrNull("slotId"),
                )
            })
            personal.insertBodyMetrics(root.array("bodyMetrics").map { BodyMetricRecord(it.getString("id"), it.getString("type"), it.getDouble("value").toFloat(), it.getLong("createdAt")) })
            personal.insertChecks(root.array("dayChecks").map { DayCheck(it.getString("key"), it.getString("kind")) })
            personal.keepNewestPin()
            // Set ids are local counters, so a set counts as the same when its day, exercise and position match.
            sets.forEach { set -> if (exercise.countSet(set.date, set.exerciseId, set.setIndex) == 0) exercise.insert(set) }
            food.insertRecipes(root.array("recipes").map { Recipe(it.getString("id"), it.getString("name"), it.stringOrNull("photoUri"), it.optString("steps"), it.optInt("prepMinutes"), it.optInt("servings", 1), it.optLong("createdAt")) })
            // An ingredient edited on the phone wins over the file's copy unless the file's is newer.
            val localIngredients = food.allIngredients().associateBy { it.id }
            root.array("ingredients").map {
                Ingredient(
                    it.getString("id"), it.getString("name"), it.getDouble("kcal").toFloat(), it.getDouble("proteinG").toFloat(), it.getDouble("carbsG").toFloat(),
                    it.getDouble("fatG").toFloat(), it.getDouble("fiberG").toFloat(), it.optString("vitamins"), it.optDouble("portionGrams", 100.0).toFloat(),
                    it.optString("portionName", "100 g"), it.optString("source", Ingredient.SOURCE_USER), if (it.isNull("fdcId")) null else it.optLong("fdcId"), it.optLong("updatedAt"),
                )
            }.forEach { row -> if ((localIngredients[row.id]?.updatedAt ?: Long.MIN_VALUE) < row.updatedAt) food.upsertIngredient(row) }
            food.insertRecipeItems(root.array("recipeItems").map { RecipeItem(it.getString("recipeId"), it.getString("ingredientId"), it.getDouble("grams").toFloat()) })
            food.insertMealItems(root.array("mealItems").map { MealItem(it.getString("mealId"), it.getString("ingredientId"), it.getDouble("grams").toFloat()) })
            budgets.insert(root.array("budgets").mapNotNull { runCatching { BudgetLimit(it.getString("id"), it.getString("topic"), BudgetPeriod.valueOf(it.getString("period")), it.getLong("limitCents")) }.getOrNull() })
            pending.insert(root.array("pendingPayments").map {
                PendingPayment(
                    it.getString("id"), it.getLong("amountCents"), it.optString("source"), it.optString("merchant"), it.optLong("createdAt"),
                    if (it.isNull("resolvedAt")) null else it.optLong("resolvedAt"), it.stringOrNull("financeId"),
                )
            })
            stack.insert(root.array("exerciseStack").map { StackExercise(it.getString("id"), it.getString("name"), it.getString("muscleGroup"), it.getDouble("met").toFloat(), it.getInt("minutes"), it.optInt("sortOrder"), it.optBoolean("placeholder")) })
        }
        root.optJSONObject("moduleSettings")?.let { json ->
            val imported = json.toModuleSettings()
            settings.setModuleSettings(imported)
            settings.setMealPlans(imported.meals.mapNotNull { slot -> slot.recipeId?.let { slot.id to it } }.toMap())
        }
        root.optJSONObject("routine")?.let { routine.fromJson(it) }
        val total = RECORD_ARRAYS.sumOf { key -> root.optJSONArray(key)?.length() ?: 0 }
        BackupResult(records = total, added = count() - before)
    }

    private suspend fun count(): Int = personal.allNotes().size + personal.allReminders().size + personal.allFinances().size +
        personal.allMeals().size + personal.allBodyMetrics().size + personal.allChecks().size + exercise.allSets().size +
        food.allRecipes().size + food.allIngredients().size + food.allRecipeItems().size + food.allMealItems().size +
        budgets.all().size + pending.all().size + stack.all().size + db.guidedExercise().all().size

    private fun String?.orNull(): Any = this ?: JSONObject.NULL
    private fun JSONObject.stringOrNull(name: String): String? = if (!has(name) || isNull(name)) null else optString(name)

    private fun JSONObject.array(name: String): List<JSONObject> {
        val array = optJSONArray(name) ?: return emptyList()
        return (0 until array.length()).map { array.getJSONObject(it) }
    }

    private fun ModuleSettings.toJson() = JSONObject()
        .put("exerciseStart", exerciseStart.toString())
        .put("exerciseRestSeconds", exerciseRestSeconds)
        .put("vaultLockSeconds", vaultLockSeconds)
        .put("hiddenTiles", JSONArray(hiddenTiles.toList()))
        .put("meals", JSONArray(meals.map { JSONObject().put("id", it.id).put("name", it.name).put("time", it.time.toString()).put("kind", it.kind.name).put("icon", it.icon).put("recipeId", it.recipeId ?: JSONObject.NULL) }))
        .put("waterGoal", waterGoal)
        .put("waterGlassMl", waterGlassMl)
        .put("waterContainer", waterContainer)
        .put("waterFrom", waterFrom.toString())
        .put("waterTo", waterTo.toString())
        .put("dailyBudgetCents", dailyBudgetCents)
        .put("worldZones", JSONArray(worldZones))
        .put("haptics", haptics)
        .put("aiApps", JSONArray(aiApps))
        .put("savingPct", savingPct)
        .put("kcalGoal", kcalGoal)
        .put("proteinGoalG", proteinGoalG)
        .put("carbsGoalG", carbsGoalG)
        .put("fatGoalG", fatGoalG)
        .put("fiberGoalG", fiberGoalG)
        .put("weightGoalKg", weightGoalKg.toDouble())
        .put("heightCm", heightCm.toDouble())
        .put("whatsappPhone", whatsappPhone)
        .put("whatsappText", whatsappText)

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
                array("meals").map { MealSlot(it.getString("id"), it.getString("name"), LocalTime.parse(it.getString("time")), MealKind.valueOf(it.getString("kind")), it.getString("icon"), if (it.isNull("recipeId")) null else it.optString("recipeId").ifBlank { null }) }
            }.getOrNull()?.takeIf { it.isNotEmpty() }
                ?: strings("mealTimes")?.mapNotNull { runCatching { LocalTime.parse(it) }.getOrNull() }?.takeIf { it.size == DEFAULT_MEAL_TIMES.size }?.let { defaultMeals(it) }
                ?: default.meals,
            waterGoal = optInt("waterGoal", default.waterGoal),
            waterGlassMl = optInt("waterGlassMl", default.waterGlassMl),
            waterContainer = optString("waterContainer", default.waterContainer),
            waterFrom = runCatching { LocalTime.parse(getString("waterFrom")) }.getOrDefault(default.waterFrom),
            waterTo = runCatching { LocalTime.parse(getString("waterTo")) }.getOrDefault(default.waterTo),
            dailyBudgetCents = optLong("dailyBudgetCents", default.dailyBudgetCents),
            worldZones = strings("worldZones") ?: default.worldZones,
            haptics = optBoolean("haptics", default.haptics),
            aiApps = strings("aiApps") ?: default.aiApps,
            savingPct = optInt("savingPct", default.savingPct),
            kcalGoal = optInt("kcalGoal", default.kcalGoal),
            proteinGoalG = optInt("proteinGoalG", default.proteinGoalG),
            carbsGoalG = optInt("carbsGoalG", default.carbsGoalG),
            fatGoalG = optInt("fatGoalG", default.fatGoalG),
            fiberGoalG = optInt("fiberGoalG", default.fiberGoalG),
            weightGoalKg = optDouble("weightGoalKg", default.weightGoalKg.toDouble()).toFloat(),
            heightCm = optDouble("heightCm", default.heightCm.toDouble()).toFloat(),
            whatsappPhone = optString("whatsappPhone", default.whatsappPhone),
            whatsappText = optString("whatsappText", default.whatsappText),
        )
    }

    companion object {
        private const val APP_ID = "minimal-launcher"
        private const val FORMAT = 5
        /** The arrays that hold records, for the counts shown after an export or import. */
        private val RECORD_ARRAYS = listOf(
            "notes", "reminders", "finances", "meals", "bodyMetrics", "dayChecks", "setLogs",
            "recipes", "ingredients", "recipeItems", "mealItems", "budgets", "pendingPayments", "exerciseStack", "guidedExercise",
        )
    }
}
