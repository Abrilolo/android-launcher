package com.adrian.minimallauncher.feature.food

import android.content.Context
import androidx.room.withTransaction
import com.adrian.minimallauncher.data.BodyMetricRecord
import com.adrian.minimallauncher.data.LauncherDatabase
import com.adrian.minimallauncher.data.MealRecord
import com.adrian.minimallauncher.data.ModuleSettings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/** Nutrition of an amount of food. Vitamins keep USDA's "name unit" keys ("C mg", "A µg"). */
data class Nutrition(
    val kcal: Float = 0f,
    val proteinG: Float = 0f,
    val carbsG: Float = 0f,
    val fatG: Float = 0f,
    val fiberG: Float = 0f,
    val vitamins: Map<String, Float> = emptyMap(),
) {
    operator fun plus(o: Nutrition) = Nutrition(
        kcal + o.kcal, proteinG + o.proteinG, carbsG + o.carbsG, fatG + o.fatG, fiberG + o.fiberG,
        (vitamins.keys + o.vitamins.keys).associateWith { (vitamins[it] ?: 0f) + (o.vitamins[it] ?: 0f) },
    )
    operator fun times(f: Float) = Nutrition(kcal * f, proteinG * f, carbsG * f, fatG * f, fiberG * f, vitamins.mapValues { it.value * f })
    operator fun div(f: Float) = if (f == 0f) this else times(1f / f)

    companion object {
        fun parseVitamins(text: String): Map<String, Float> = text.split(';').mapNotNull { pair ->
            val name = pair.substringBefore('=', "").trim()
            pair.substringAfter('=', "").toFloatOrNull()?.takeIf { name.isNotEmpty() }?.let { name to it }
        }.toMap()
    }
}

/** Nutrition of [grams] of this ingredient (its values are per 100 g). */
fun Ingredient.forGrams(grams: Float): Nutrition =
    Nutrition(kcal, proteinG, carbsG, fatG, fiberG, Nutrition.parseVitamins(vitamins)) * (grams / 100f)

/** A recipe with its ingredients and totals, ready for the recipe views. */
data class RecipeDetail(val recipe: Recipe, val items: List<Pair<Ingredient, Float>>) {
    val total: Nutrition get() = items.fold(Nutrition()) { acc, (ingredient, grams) -> acc + ingredient.forGrams(grams) }
    val perServing: Nutrition get() = total / recipe.servings.coerceAtLeast(1).toFloat()
}

/** The day's food against the goals in settings; [over] is how many kcal went past the goal (0 if none). */
data class FoodDay(
    val date: LocalDate = LocalDate.now(),
    val eaten: Nutrition = Nutrition(),
    val kcalGoal: Int = 0,
    val proteinGoalG: Int = 0,
    val carbsGoalG: Int = 0,
    val fatGoalG: Int = 0,
    val fiberGoalG: Int = 0,
    val weightKg: Float? = null,
    val weightGoalKg: Float = 0f,
    val bmi: Float? = null,
) {
    val over: Float get() = if (kcalGoal > 0) (eaten.kcal - kcalGoal).coerceAtLeast(0f) else 0f
    val kcalLeft: Float get() = if (kcalGoal > 0) kcalGoal - eaten.kcal else 0f
}

object FoodMath {
    /** Body mass index (IMC): kg ÷ m²; null without both values. */
    fun bmi(weightKg: Float?, heightCm: Float): Float? {
        if (weightKg == null || weightKg <= 0f || heightCm <= 0f) return null
        val m = heightCm / 100f
        return weightKg / (m * m)
    }

    /** Latest weight from the body metrics the exercise page already records ("WEIGHT"). */
    fun latestWeight(metrics: List<BodyMetricRecord>): Float? = metrics.filter { it.type == "WEIGHT" }.maxByOrNull { it.createdAt }?.value

    fun day(date: LocalDate, meals: List<MealRecord>, metrics: List<BodyMetricRecord>, s: ModuleSettings): FoodDay {
        val zone = ZoneId.systemDefault()
        val eaten = meals.filter { Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate() == date }
            .fold(Nutrition()) { acc, m -> acc + Nutrition(m.kcal, m.proteinG, m.carbsG, m.fatG, m.fiberG) }
        val weight = latestWeight(metrics)
        return FoodDay(date, eaten, s.kcalGoal, s.proteinGoalG, s.carbsGoalG, s.fatGoalG, s.fiberGoalG, weight, s.weightGoalKg, bmi(weight, s.heightCm))
    }
}

/** Recipes, ingredients and meals made of them. The USDA seed is copied once from assets/ingredients.json. */
class FoodRepository(private val context: Context, private val db: LauncherDatabase = LauncherDatabase.get(context)) {
    private val dao = db.food()
    private val personal = db.personal()

    val recipes = dao.recipes()
    val ingredients = dao.ingredients()
    val recipeItems = dao.recipeItems()

    private fun newId() = UUID.randomUUID().toString()
    private fun now() = System.currentTimeMillis()

    /**
     * Inserts the USDA rows that are missing (by id). Rows the user edited keep their changes, because the
     * seed never replaces an existing id.
     */
    suspend fun seedIfNeeded() = withContext(Dispatchers.IO) {
        val text = runCatching { context.assets.open(SEED_ASSET).use { String(it.readBytes(), Charsets.UTF_8) } }.getOrNull() ?: return@withContext
        val rows = JSONArray(text)
        if (dao.seededCount() >= rows.length()) return@withContext
        dao.insertIngredients((0 until rows.length()).map { i ->
            val o = rows.getJSONObject(i)
            Ingredient(
                id = o.getString("id"), name = o.getString("name"), kcal = o.getDouble("kcal").toFloat(), proteinG = o.getDouble("proteinG").toFloat(),
                carbsG = o.getDouble("carbsG").toFloat(), fatG = o.getDouble("fatG").toFloat(), fiberG = o.getDouble("fiberG").toFloat(),
                vitamins = o.optString("vitamins"), portionGrams = o.optDouble("portionGrams", 100.0).toFloat(), portionName = o.optString("portionName", "100 g"),
                source = Ingredient.SOURCE_USDA, fdcId = if (o.isNull("fdcId")) null else o.optLong("fdcId"), updatedAt = 0,
            )
        })
    }

    suspend fun search(query: String): List<Ingredient> = dao.searchIngredients(query.trim())

    /** Adds the user's own ingredient (values per 100 g) and returns its id. */
    suspend fun addIngredient(name: String, per100g: Nutrition, portionGrams: Float = 100f, portionName: String = "100 g"): String {
        val id = "user_" + newId()
        dao.upsertIngredient(Ingredient(id, name.trim(), per100g.kcal, per100g.proteinG, per100g.carbsG, per100g.fatG, per100g.fiberG, encodeVitamins(per100g.vitamins), portionGrams, portionName, Ingredient.SOURCE_USER, null, now()))
        return id
    }

    /** Saves an edited ingredient (a seeded one too: its usual portion, or its values). */
    suspend fun updateIngredient(ingredient: Ingredient) = dao.upsertIngredient(ingredient.copy(updatedAt = now()))
    suspend fun setPortion(ingredient: Ingredient, grams: Float, name: String) = updateIngredient(ingredient.copy(portionGrams = grams, portionName = name))
    suspend fun deleteIngredient(id: String) = dao.deleteUserIngredient(id)

    /** Creates or replaces a recipe and its ingredient list ([items] = ingredient id to grams); returns its id. */
    suspend fun saveRecipe(recipe: Recipe, items: Map<String, Float>): String = db.withTransaction {
        dao.upsertRecipe(recipe)
        dao.clearRecipeItems(recipe.id)
        dao.insertRecipeItems(items.filterValues { it > 0f }.map { (ingredientId, grams) -> RecipeItem(recipe.id, ingredientId, grams) })
        recipe.id
    }
    fun newRecipe(name: String) = Recipe(newId(), name.trim())
    suspend fun deleteRecipe(id: String) = db.withTransaction {
        dao.clearRecipeItems(id)
        dao.deleteRecipe(id)
    }

    suspend fun detail(recipeId: String): RecipeDetail? {
        val recipe = dao.recipe(recipeId) ?: return null
        val items = dao.itemsOf(recipeId)
        val byId = dao.ingredientsById(items.map { it.ingredientId }).associateBy { it.id }
        return RecipeDetail(recipe, items.mapNotNull { item -> byId[item.ingredientId]?.let { it to item.grams } })
    }

    /** Registers a meal of [servings] of a recipe; its totals are stored with the meal. Returns the meal id. */
    suspend fun logRecipe(recipeId: String, servings: Float = 1f, description: String? = null, slotId: String? = null): String? {
        val detail = detail(recipeId) ?: return null
        val n = detail.perServing * servings
        val id = newId()
        personal.insertMeals(listOf(MealRecord(id, description ?: detail.recipe.name, now(), recipeId, servings, n.kcal, n.proteinG, n.carbsG, n.fatG, n.fiberG, slotId)))
        return id
    }

    /** Registers a meal of loose ingredients ([items] = ingredient id to grams). Returns the meal id. */
    suspend fun logIngredients(description: String, items: Map<String, Float>, slotId: String? = null): String = db.withTransaction {
        val byId = dao.ingredientsById(items.keys.toList()).associateBy { it.id }
        val n = items.entries.fold(Nutrition()) { acc, (id, grams) -> byId[id]?.let { acc + it.forGrams(grams) } ?: acc }
        val id = newId()
        personal.insertMeals(listOf(MealRecord(id, description, now(), null, 1f, n.kcal, n.proteinG, n.carbsG, n.fatG, n.fiberG, slotId)))
        dao.insertMealItems(items.filterKeys { it in byId }.map { (ingredientId, grams) -> MealItem(id, ingredientId, grams) })
        id
    }

    private fun encodeVitamins(v: Map<String, Float>) = v.entries.joinToString(";") { "${it.key}=${it.value}" }

    companion object {
        const val SEED_ASSET = "ingredients.json"
    }
}
