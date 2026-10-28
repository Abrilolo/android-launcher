package com.adrian.minimallauncher.feature.food

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** A dish the user cooks; its nutrition comes from its [RecipeItem]s. [steps] are separated by new lines. */
@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey val id: String,
    val name: String,
    val photoUri: String? = null,
    val steps: String = "",
    val prepMinutes: Int = 0,
    /** How many plates the ingredients make; a meal of one serving takes 1/[servings] of the totals. */
    val servings: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * One food with its nutrition per 100 g. [source] is "usda" for the seeded FoodData Central rows and "user"
 * for the user's own; editing a seeded row keeps its id so the seed never overwrites it again.
 * [portionGrams]/[portionName] are the usual serving ("1 pieza", 120 g), editable by the user.
 * [vitamins] is "name=amount" pairs separated by ';' (mg or µg as USDA reports them), e.g. "C mg=53.2;A µg=3".
 */
@Entity(tableName = "ingredients")
data class Ingredient(
    @PrimaryKey val id: String,
    val name: String,
    val kcal: Float,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
    val fiberG: Float,
    val vitamins: String = "",
    val portionGrams: Float = 100f,
    val portionName: String = "100 g",
    val source: String = SOURCE_USER,
    val fdcId: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val SOURCE_USDA = "usda"
        const val SOURCE_USER = "user"
    }
}

@Entity(tableName = "recipe_items", primaryKeys = ["recipeId", "ingredientId"])
data class RecipeItem(val recipeId: String, val ingredientId: String, val grams: Float)

/** An ingredient eaten as part of a meal that was not a recipe. */
@Entity(tableName = "meal_items", primaryKeys = ["mealId", "ingredientId"])
data class MealItem(val mealId: String, val ingredientId: String, val grams: Float)

@Dao
interface FoodDao {
    @Query("SELECT * FROM recipes ORDER BY name") fun recipes(): Flow<List<Recipe>>
    @Query("SELECT * FROM ingredients ORDER BY name") fun ingredients(): Flow<List<Ingredient>>
    @Query("SELECT * FROM recipe_items") fun recipeItems(): Flow<List<RecipeItem>>
    @Query("SELECT * FROM meal_items") fun mealItems(): Flow<List<MealItem>>

    @Query("SELECT * FROM ingredients WHERE name LIKE '%' || :query || '%' ORDER BY source DESC, name LIMIT :limit")
    suspend fun searchIngredients(query: String, limit: Int = 30): List<Ingredient>
    @Query("SELECT * FROM ingredients WHERE id IN (:ids)") suspend fun ingredientsById(ids: List<String>): List<Ingredient>
    @Query("SELECT * FROM recipes WHERE id = :id") suspend fun recipe(id: String): Recipe?
    @Query("SELECT * FROM recipe_items WHERE recipeId = :recipeId") suspend fun itemsOf(recipeId: String): List<RecipeItem>
    @Query("SELECT COUNT(*) FROM ingredients WHERE source = 'usda'") suspend fun seededCount(): Int

    @Upsert suspend fun upsertRecipe(recipe: Recipe)
    @Upsert suspend fun upsertIngredient(ingredient: Ingredient)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertRecipes(rows: List<Recipe>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertIngredients(rows: List<Ingredient>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertRecipeItems(rows: List<RecipeItem>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertMealItems(rows: List<MealItem>)

    @Query("DELETE FROM recipes WHERE id = :id") suspend fun deleteRecipe(id: String)
    @Query("DELETE FROM recipe_items WHERE recipeId = :recipeId") suspend fun clearRecipeItems(recipeId: String)
    @Query("DELETE FROM meal_items WHERE mealId = :mealId") suspend fun clearMealItems(mealId: String)
    @Query("DELETE FROM ingredients WHERE id = :id AND source = 'user'") suspend fun deleteUserIngredient(id: String)

    // One-shot reads for the backup file.
    @Query("SELECT * FROM recipes") suspend fun allRecipes(): List<Recipe>
    @Query("SELECT * FROM ingredients") suspend fun allIngredients(): List<Ingredient>
    @Query("SELECT * FROM recipe_items") suspend fun allRecipeItems(): List<RecipeItem>
    @Query("SELECT * FROM meal_items") suspend fun allMealItems(): List<MealItem>
}
