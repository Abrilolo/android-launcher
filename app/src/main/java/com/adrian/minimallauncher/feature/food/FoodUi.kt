package com.adrian.minimallauncher.feature.food

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Stroke icon of a meal slot, by the icon name its settings keep (the old dot icon names). */
fun mealStrokeIcon(icon: String) = when (icon) {
    "snack" -> "snack"
    "colacion" -> "colacion"
    "cena" -> "cena"
    else -> "plato"
}

/** Per-serving nutrition of every recipe, from the recipe, item and ingredient tables. */
fun recipeNutrition(recipes: List<Recipe>, items: List<RecipeItem>, ingredients: List<Ingredient>): Map<String, Nutrition> {
    val byId = ingredients.associateBy { it.id }
    val byRecipe = items.groupBy { it.recipeId }
    return recipes.associate { r ->
        val total = byRecipe[r.id].orEmpty().fold(Nutrition()) { acc, item -> byId[item.ingredientId]?.let { acc + it.forGrams(item.grams) } ?: acc }
        r.id to total / r.servings.coerceAtLeast(1).toFloat()
    }
}

fun Float.whole(): String = Math.round(this).toString()
fun Float.oneDecimal(): String = String.format(Locale.US, "%.1f", this)

/** An icon and its number, e.g. the egg and the protein grams. */
@Composable
fun NutrientStat(icon: String, value: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        StrokeIcon(icon, StrokeState.DONE, 18.dp)
        Text(value, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

/**
 * A recipe's photo, cropped to [size] with [corner] rounding; without one, a gray outline with the plate. [dim] shows
 * it gray, for meals still to come.
 */
@Composable
fun RecipePhoto(uri: String?, size: Dp, modifier: Modifier = Modifier, corner: Dp = size / 2, dim: Boolean = false) =
    PhotoBox(uri, modifier.size(size), RoundedCornerShape(corner), size * 0.45f, dim)

/** The big photo at the top of a recipe; [modifier] sets its size. */
@Composable
fun RecipeHero(uri: String?, modifier: Modifier) = PhotoBox(uri, modifier, RoundedCornerShape(24.dp), 48.dp, false)

@Composable
private fun PhotoBox(uri: String?, modifier: Modifier, shape: RoundedCornerShape, iconSize: Dp, dim: Boolean) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, uri) { value = uri?.let { loadPhoto(context, it, 1080) } }
    val image = bitmap
    if (image == null) {
        Box(modifier.border(1.5.dp, Color(0xFF2A2A2A), shape), contentAlignment = Alignment.Center) {
            StrokeIcon("plato", StrokeState.OFF, iconSize)
        }
    } else {
        Image(
            image, null, modifier.clip(shape), contentScale = ContentScale.Crop,
            colorFilter = if (dim) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null,
            alpha = if (dim) 0.55f else 1f,
        )
    }
}

private suspend fun loadPhoto(context: Context, uri: String, maxSide: Int): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching { decode(context, uri, maxSide)?.asImageBitmap() }.getOrNull()
}

private fun open(context: Context, uri: String) =
    if (uri.startsWith("/")) File(uri).inputStream() else context.contentResolver.openInputStream(Uri.parse(uri))

private fun decode(context: Context, uri: String, maxSide: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    open(context, uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
    return open(context, uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
}

/**
 * Copies a picked photo into the app's files (scaled down), so the recipe keeps it without the picker's permission.
 * Returns the new file's path; older photos of the same recipe are removed.
 */
suspend fun saveRecipePhoto(context: Context, source: Uri, recipeId: String): String? = withContext(Dispatchers.IO) {
    runCatching {
        val bitmap = decode(context, source.toString(), 1080) ?: return@runCatching null
        val dir = File(context.filesDir, "recipes").apply { mkdirs() }
        dir.listFiles { f -> f.name.startsWith("$recipeId-") }?.forEach { it.delete() }
        val file = File(dir, "$recipeId-${System.currentTimeMillis()}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        file.absolutePath
    }.getOrNull()
}
