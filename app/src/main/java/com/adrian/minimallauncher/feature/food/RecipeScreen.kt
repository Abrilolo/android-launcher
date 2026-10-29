package com.adrian.minimallauncher.feature.food

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.feature.ModuleTextField
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.ui.components.LocalReactions
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
import kotlinx.coroutines.launch

/** What the recipe page needs from the view model. */
class FoodActions(
    val search: (String, (List<Ingredient>) -> Unit) -> Unit,
    val addIngredient: (String, Nutrition, Float, String) -> Unit,
    val updateIngredient: (Ingredient) -> Unit,
    val newRecipe: (String) -> Recipe,
    val saveRecipe: (Recipe, Map<String, Float>) -> Unit,
    val deleteRecipe: (String) -> Unit,
    val recipeDetail: (String, (RecipeDetail?) -> Unit) -> Unit,
    val logRecipe: (String, Float) -> Unit,
    val logIngredients: (String, Map<String, Float>) -> Unit,
    val addWeight: (Float) -> Unit,
)

/**
 * A recipe, following ComidaVisual.html: photo, prep minutes, kcal, protein, fiber, vitamins, ingredients with grams
 * and the steps; ✓ logs one serving. The pencil edits everything (the camera changes the photo); a new recipe opens
 * in edit mode. [recipeId] null means a new one.
 */
@Composable
fun RecipeScreen(recipeId: String?, actions: FoodActions, onClose: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reactions = LocalReactions.current
    var detail by remember { mutableStateOf<RecipeDetail?>(null) }
    var editing by remember { mutableStateOf(recipeId == null) }
    var reload by remember { mutableStateOf(0) }
    var recipe by remember { mutableStateOf(actions.newRecipe("")) }
    var name by remember { mutableStateOf("") }
    var prep by remember { mutableStateOf("") }
    var servings by remember { mutableStateOf("1") }
    var steps by remember { mutableStateOf("") }
    val items = remember { mutableStateListOf<PickedItem>() }

    fun load(d: RecipeDetail) {
        detail = d; recipe = d.recipe; name = d.recipe.name; prep = d.recipe.prepMinutes.toString()
        servings = d.recipe.servings.toString(); steps = d.recipe.steps
        items.clear(); items += d.items.map { (ing, g) -> PickedItem(ing, g) }
    }
    LaunchedEffect(recipeId, reload) { if (recipeId != null) actions.recipeDetail(recipeId) { it?.let(::load) } }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { saveRecipePhoto(context, uri, recipe.id)?.let { recipe = recipe.copy(photoUri = it) } }
    }
    val save = {
        if (name.isNotBlank()) {
            val saved = recipe.copy(name = name.trim(), prepMinutes = prep.toAmount()?.toInt() ?: 0, servings = servings.toAmount()?.toInt()?.coerceAtLeast(1) ?: 1, steps = steps.trim())
            actions.saveRecipe(saved, items.groupBy { it.ingredient.id }.mapValues { (_, list) -> list.sumOf { it.grams.toDouble() }.toFloat() })
            reactions.tick()
            if (recipeId == null) onClose() else { editing = false; reload++ }
        }
    }

    Column(
        modifier.fillMaxSize().background(Color.Black).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.End, Alignment.CenterVertically) {
            if (editing) {
                if (recipeId != null) IconButton("Borrar receta", { actions.deleteRecipe(recipeId); onClose() }) { StrokeIcon("papelera", StrokeState.OFF, 20.dp) }
                IconButton("Cambiar foto", { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { StrokeIcon("camara", StrokeState.OFF, 22.dp) }
                IconButton("Listo", save) { StrokeIcon("listo", StrokeState.DONE, 22.dp, color = LauncherRed) }
            } else {
                IconButton("Editar receta", { editing = true }) { StrokeIcon("lapiz", StrokeState.OFF, 20.dp) }
            }
        }
        val hero = Modifier.fillMaxWidth().height(220.dp)
        RecipeHero(recipe.photoUri, if (editing) hero.clickable { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } else hero)
        if (editing) {
            ModuleTextField(name, { name = it.take(50) }, "Nombre", Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                StrokeIcon("reloj", StrokeState.DONE, 18.dp)
                NumberField(prep, { prep = it }, "Minutos de preparación", decimals = false)
                SmallLabel("MIN")
                SmallLabel("PORCIONES")
                NumberField(servings, { servings = it }, "Porciones", decimals = false)
            }
            IngredientBuilder(items, actions.search, actions.addIngredient, actions.updateIngredient)
            ModuleTextField(steps, { steps = it.take(2000) }, "Pasos, uno por línea", Modifier.fillMaxWidth(), minLines = 4)
        } else {
            val d = detail
            Text(name, style = MaterialTheme.typography.titleLarge)
            if (d != null) {
                val per = d.perServing
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    NutrientStat("reloj", recipe.prepMinutes.toString())
                    NutrientStat("plato", per.kcal.whole())
                    NutrientStat("huevo", per.proteinG.whole())
                    NutrientStat("hoja", per.fiberG.whole())
                }
                if (per.vitamins.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StrokeIcon("vitamina", StrokeState.DONE, 18.dp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            per.vitamins.entries.sortedBy { it.key }.forEach { (key, amount) ->
                                val (vit, unit) = key.split(' ').let { it[0] to it.getOrElse(1) { "" } }
                                Text(
                                    "$vit ${if (amount >= 10f) amount.whole() else amount.oneDecimal()}$unit",
                                    Modifier.border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFB0B0B0),
                                )
                            }
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF222222)))
                Column {
                    d.items.forEach { (ing, grams) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), Arrangement.SpaceBetween) {
                            Text(ing.name, style = MaterialTheme.typography.bodyMedium)
                            SmallLabel("${grams.whole()} g")
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    recipe.steps.lines().filter { it.isNotBlank() }.forEachIndexed { i, step ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SmallLabel("${i + 1}", color = LauncherRed)
                            Text(step.trim(), style = MaterialTheme.typography.bodyMedium, color = Color(0xFFDDDDDD))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    SmallLabel(if (recipe.servings > 1) "×${recipe.servings}" else "")
                    CheckButton("Comí esto", { actions.logRecipe(recipe.id, 1f); reactions.tick(); onClose() })
                }
            }
        }
    }
}
