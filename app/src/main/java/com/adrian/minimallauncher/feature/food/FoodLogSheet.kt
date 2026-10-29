package com.adrian.minimallauncher.feature.food

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.feature.CardFill
import com.adrian.minimallauncher.feature.CardLine
import com.adrian.minimallauncher.feature.ModuleTextField
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
import kotlinx.coroutines.delay

private val Gray = Color(0xFF5C5C5C)
private val Muted = Color(0xFFB0B0B0)
private val Faint = Color(0xFF161616)

/** The bottom sheet frame every food sheet uses: dark card, rounded top, scrolls, back closes it. */
@Composable
fun FoodSheet(onClose: () -> Unit, modifier: Modifier = Modifier, header: @Composable () -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onClose)
    Box(Modifier.fillMaxWidth().then(modifier).background(Color(0xAA000000)).clickable(onClick = onClose, indication = null, interactionSource = null), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(CardFill)
                .border(1.dp, CardLine, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .clickable(onClick = {}, indication = null, interactionSource = null)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { header() }
                IconButton("Cerrar", onClose) { Text("✕", style = MaterialTheme.typography.labelLarge, color = Muted) }
            }
            content()
        }
    }
}

@Composable
fun IconButton(description: String, onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = description }.padding(6.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** The round white ✓ that saves or logs. */
@Composable
fun CheckButton(description: String, onClick: () -> Unit, filled: Boolean = true, size: Dp = 52.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (filled) Color(0xFFF5F5F5) else Color(0xFF1C1C1C))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { StrokeIcon("listo", StrokeState.DONE, 22.dp, color = if (filled) Color.Black else Color(0xFFF5F5F5)) }
}

/** A small number field with an underline, for grams and nutrition values. */
@Composable
fun NumberField(value: String, onChange: (String) -> Unit, description: String, width: Dp = 52.dp, decimals: Boolean = true) {
    BasicTextField(
        value, { text -> onChange(text.filter { it.isDigit() || (decimals && (it == '.' || it == ',')) }.take(6)) },
        Modifier.width(width).semantics { contentDescription = description },
        textStyle = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center),
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
        keyboardOptions = KeyboardOptions(keyboardType = if (decimals) KeyboardType.Decimal else KeyboardType.Number),
        decorationBox = { inner -> Column { inner(); Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF333333))) } },
    )
}

fun String.toAmount(): Float? = replace(',', '.').toFloatOrNull()

/** Icon tabs at the top of a sheet; the chosen one is white. */
@Composable
fun IconTabs(tabs: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(12.dp)).border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp))) {
        tabs.forEach { (key, icon) ->
            Box(
                Modifier
                    .background(if (key == selected) Color(0xFF1C1C1C) else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(key) }
                    .semantics { contentDescription = key }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) { StrokeIcon(icon, if (key == selected) StrokeState.DONE else StrokeState.OFF, 20.dp) }
        }
    }
}

/** A grid of recipe photos with their kcal (or prep minutes); tapping one picks it. */
@Composable
fun RecipeGrid(recipes: List<Recipe>, nutrition: Map<String, Nutrition>, showPrep: Boolean, onPick: (Recipe) -> Unit) {
    if (recipes.isEmpty()) {
        Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) { StrokeIcon("escuela", StrokeState.OFF, 36.dp) }
        return
    }
    recipes.chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { r ->
                Column(
                    Modifier.weight(1f).clickable(role = Role.Button) { onPick(r) }.semantics { contentDescription = r.name },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    RecipePhoto(r.photoUri, 84.dp, corner = 20.dp)
                    if (showPrep) NutrientStat("reloj", r.prepMinutes.toString(), color = Muted)
                    else SmallLabel(nutrition[r.id]?.kcal?.whole() ?: "0")
                }
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

/** One picked ingredient of a meal or recipe and its grams. */
class PickedItem(ingredient: Ingredient, grams: Float) {
    var ingredient by mutableStateOf(ingredient)
    var grams by mutableStateOf(grams)
}

/**
 * Builds food from the ingredient base: search (USDA seed and the user's own), tap to add one usual portion, − / +
 * by portions, grams editable, pencil to fix an ingredient's values or usual portion, + for a new ingredient.
 * The totals update live under the list.
 */
@Composable
fun IngredientBuilder(
    picked: MutableList<PickedItem>,
    search: (String, (List<Ingredient>) -> Unit) -> Unit,
    onAddIngredient: (String, Nutrition, Float, String) -> Unit,
    onUpdateIngredient: (Ingredient) -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<Ingredient>()) }
    var editing by remember { mutableStateOf<Ingredient?>(null) }
    var creating by remember { mutableStateOf(false) }
    LaunchedEffect(query) {
        delay(150)
        search(query) { results = it.take(if (query.isBlank()) 5 else 8) }
    }
    if (creating || editing != null) {
        IngredientForm(
            editing,
            onSave = { name, per100, grams, portion ->
                val old = editing
                if (old == null) onAddIngredient(name, per100, grams, portion)
                else {
                    val updated = old.copy(name = name, kcal = per100.kcal, proteinG = per100.proteinG, carbsG = per100.carbsG, fatG = per100.fatG, fiberG = per100.fiberG, portionGrams = grams, portionName = portion)
                    onUpdateIngredient(updated)
                    picked.filter { it.ingredient.id == old.id }.forEach { it.ingredient = updated }
                }
                creating = false; editing = null
                search(query) { results = it.take(8) }
            },
            onCancel = { creating = false; editing = null },
        )
        return
    }
    ModuleTextField(query, { query = it.take(40) }, "Buscar ingrediente", Modifier.fillMaxWidth())
    Column {
        results.forEach { ing ->
            Row(
                Modifier.fillMaxWidth().clickable(role = Role.Button) { picked += PickedItem(ing, ing.portionGrams) }.padding(vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(ing.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                PortionChip(ing.portionName)
                SmallLabel(ing.forGrams(ing.portionGrams).kcal.whole())
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Faint))
        }
        Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { creating = true }.semantics { contentDescription = "Ingrediente nuevo" }.padding(vertical = 9.dp)) {
            StrokeIcon("nueva", StrokeState.OFF, 20.dp)
        }
    }
    picked.forEachIndexed { i, item ->
        val ing = item.ingredient
        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(ing.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                SmallLabel(ing.forGrams(item.grams).kcal.whole(), color = MaterialTheme.colorScheme.onSurface)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundButton("−", "Menos") {
                    item.grams = (item.grams - ing.portionGrams).coerceAtLeast(0f)
                    if (item.grams <= 0f) picked.removeAt(i)
                }
                PortionChip(ing.portionName)
                RoundButton("+", "Más") { item.grams += ing.portionGrams }
                Spacer(Modifier.weight(1f))
                var text by remember(item, item.grams) { mutableStateOf(item.grams.whole()) }
                NumberField(text, { text = it; it.toAmount()?.let { g -> item.grams = g } }, "Gramos de ${ing.name}")
                SmallLabel("g")
                IconButton("Editar ${ing.name}", { editing = ing }) { StrokeIcon("lapiz", StrokeState.OFF, 18.dp) }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Faint))
    }
    if (picked.isNotEmpty()) {
        val total = picked.fold(Nutrition()) { acc, it -> acc + it.ingredient.forGrams(it.grams) }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            NutrientStat("plato", total.kcal.whole())
            NutrientStat("huevo", total.proteinG.whole())
            NutrientStat("hoja", total.fiberG.whole())
            trailing()
        }
    }
}

@Composable
private fun PortionChip(name: String) {
    Text(
        name,
        Modifier.border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        color = Muted,
        maxLines = 1,
    )
}

@Composable
private fun RoundButton(symbol: String, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(30.dp).clip(CircleShape).border(1.dp, Color(0xFF333333), CircleShape).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { Text(symbol, style = MaterialTheme.typography.bodyMedium) }
}

/** New ingredient or edit one: name, values per 100 g and the usual portion. Labels show here, it is editing. */
@Composable
private fun IngredientForm(edit: Ingredient?, onSave: (String, Nutrition, Float, String) -> Unit, onCancel: () -> Unit) {
    BackHandler(onBack = onCancel)
    var name by remember { mutableStateOf(edit?.name ?: "") }
    val fields = remember {
        mutableStateListOf(
            edit?.kcal?.oneDecimal() ?: "", edit?.proteinG?.oneDecimal() ?: "", edit?.carbsG?.oneDecimal() ?: "",
            edit?.fatG?.oneDecimal() ?: "", edit?.fiberG?.oneDecimal() ?: "", edit?.portionGrams?.whole() ?: "100",
        )
    }
    var portion by remember { mutableStateOf(edit?.portionName ?: "100 g") }
    val labels = listOf("KCAL", "PROTEÍNA G", "CARBOHIDRATOS G", "GRASA G", "FIBRA G", "PORCIÓN G")
    ModuleTextField(name, { name = it.take(40) }, "Nombre", Modifier.fillMaxWidth())
    SmallLabel("POR 100 G")
    labels.forEachIndexed { i, label ->
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            SmallLabel(label)
            NumberField(fields[i], { fields[i] = it }, label, width = 72.dp)
        }
    }
    ModuleTextField(portion, { portion = it.take(20) }, "Porción, p. ej. 1 pieza", Modifier.fillMaxWidth())
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        IconButton("Cancelar", onCancel) { Text("✕", style = MaterialTheme.typography.labelLarge, color = Muted) }
        CheckButton("Guardar ingrediente", {
            val v = fields.map { it.toAmount() ?: 0f }
            if (name.isNotBlank()) onSave(name.trim(), Nutrition(v[0], v[1], v[2], v[3], v[4]), v[5].takeIf { it > 0f } ?: 100f, portion.ifBlank { "100 g" })
        })
    }
}
