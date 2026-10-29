package com.adrian.minimallauncher.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adrian.minimallauncher.data.ForcedState
import com.adrian.minimallauncher.data.ForcedStep
import com.adrian.minimallauncher.data.MealKind
import com.adrian.minimallauncher.data.MealSlot
import com.adrian.minimallauncher.data.PendingPayment
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.feature.food.Ingredient
import com.adrian.minimallauncher.feature.food.Recipe
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
import com.adrian.minimallauncher.ui.theme.LauncherBlack
import com.adrian.minimallauncher.ui.theme.LauncherTheme
import com.adrian.minimallauncher.viewmodel.LauncherViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val White = Color(0xFFF5F5F5)
private val Gray = Color(0xFF5C5C5C)
private val Muted = Color(0xFFB0B0B0)
private val Line = Color(0xFF222222)
private val Card = Color(0xFF0B0B0B)
private val hhmm = DateTimeFormatter.ofPattern("HH:mm")

/** How long a tapped icon draws itself before the flow moves on. */
private const val DRAW_WAIT_MS = 900L

/** One answer to "¿fue comida?" and where the money came from (docs/design/concept/RegistroVisual.html). */
private enum class PaymentChoice(val icon: String, val isFood: Boolean, val category: String, val label: String) {
    FOOD_BUDGET("plato", true, "comida", "Comida, del presupuesto de comida"),
    EXTRAS_BAG("bolsa", true, "antojo", "Comida, de la bolsa de extras"),
    TRANSFER("transferencia", false, "transferencia", "Transferencia"),
    GIFT("regalo", false, "regalo", "Regalo"),
    SERVICE("servicio", false, "servicio", "Servicio externo"),
}

/**
 * The forced registration flow (docs/design/concept/RegistroVisual.html): the amount and, on one screen, whether it
 * was food (food budget or extras bag) or not (transfer, gift, service); food then asks for the meal. Without
 * payments, salida's unmarked meals. No text: icons, amounts and hours. Back does nothing; only saving leaves.
 */
@Composable
fun ForcedEntryScreen(state: ForcedState, viewModel: LauncherViewModel) {
    BackHandler { }
    LauncherTheme {
        Column(Modifier.fillMaxSize().background(LauncherBlack).safeDrawingPadding().imePadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
            Row(Modifier.fillMaxWidth().height(28.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                // Pending payments as dots (the current one red), never as a count.
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.semantics { contentDescription = "Pagos pendientes" }) {
                    if (state.openPayments > 1) repeat(state.openPayments) { i ->
                        Box(Modifier.size(6.dp).clip(CircleShape).background(if (i == 0) LauncherRed else Color(0xFF333333)))
                    }
                }
                StrokeIcon("candado", StrokeState.OFF, 20.dp)
            }
            val payment = state.payment
            when {
                payment == null -> RoutineMeals(viewModel)
                state.step == ForcedStep.MEAL -> MealStep(payment, viewModel)
                else -> PaymentStep(state, payment, viewModel)
            }
        }
    }
}

@Composable
private fun ColumnScope.PaymentStep(state: ForcedState, payment: PendingPayment, viewModel: LauncherViewModel) {
    var amountCents by remember(payment.id) { mutableLongStateOf(payment.amountCents) }
    var editing by remember(payment.id) { mutableStateOf(false) }
    var choice by remember(payment.id) { mutableStateOf<PaymentChoice?>(null) }
    val summary by viewModel.financeSummary.collectAsState()

    // The answer is given after the icon draws; once the flow reaches the expense step it is saved with that choice.
    LaunchedEffect(payment.id, choice, state.step, state.isFood) {
        val c = choice ?: return@LaunchedEffect
        if (state.step == ForcedStep.ASK_FOOD) {
            delay(DRAW_WAIT_MS)
            viewModel.forcedAnswerFood(c.isFood)
        } else if (state.step == ForcedStep.EXPENSE) {
            if (state.isFood != c.isFood) viewModel.forcedAnswerFood(c.isFood) else viewModel.forcedSaveExpense(amountCents, payment.merchant, c.category)
        }
    }

    if (editing) {
        AmountKeypad(onDone = { cents -> cents?.let { amountCents = it }; editing = false })
        return
    }
    Amount(amountCents, big = true, modifier = Modifier.fillMaxWidth().padding(top = 40.dp).clickable(role = Role.Button) { if (choice == null) editing = true }
        .semantics { contentDescription = "Corregir monto" })

    val foodTopic = summary.topics.firstOrNull { it.topic.equals(PaymentChoice.FOOD_BUDGET.category, ignoreCase = true) }
    val overFood = viewModel.wouldExceedBudget(PaymentChoice.FOOD_BUDGET.category, amountCents)
    Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            // The suggested source (food budget, or the bag when the payment passes what is left of it) shows its red detail.
            ChoiceButton(PaymentChoice.FOOD_BUDGET, choice, 72.dp, suggested = !overFood, number = foodTopic?.todayLeftCents) { choice = it }
            ChoiceButton(PaymentChoice.EXTRAS_BAG, choice, 72.dp, suggested = overFood, number = summary.bagLeftCents) { choice = it }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            listOf(PaymentChoice.TRANSFER, PaymentChoice.GIFT, PaymentChoice.SERVICE).forEach { ChoiceButton(it, choice, 52.dp) { c -> choice = c } }
        }
    }
}

@Composable
private fun ChoiceButton(option: PaymentChoice, chosen: PaymentChoice?, size: Dp, suggested: Boolean = false, number: Long? = null, onPick: (PaymentChoice) -> Unit) {
    val iconState = when {
        chosen == option -> StrokeState.NOW
        chosen != null -> StrokeState.OFF
        suggested -> StrokeState.NOW
        else -> StrokeState.DONE
    }
    Column(
        Modifier.clickable(enabled = chosen == null, role = Role.Button) { onPick(option) }.semantics { contentDescription = option.label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StrokeIcon(option.icon, iconState, size, playKey = chosen == option)
        if (number != null) Text(
            "${number / 100}", style = MaterialTheme.typography.labelSmall,
            color = if (suggested && chosen == null) LauncherRed else Muted,
        )
    }
}

/** The amount, "$" and cents small. */
@Composable
private fun Amount(cents: Long, big: Boolean, modifier: Modifier = Modifier) {
    val main = if (big) 72.sp else 40.sp
    val small = if (big) 28.sp else 18.sp
    Row(modifier, horizontalArrangement = Arrangement.Center) {
        Text("$", style = TextStyle(fontSize = small, color = Muted), modifier = Modifier.padding(top = 8.dp, end = 4.dp))
        Text("${cents / 100}", style = MaterialTheme.typography.displayLarge.copy(fontSize = main), color = White)
        Text(".%02d".format(cents % 100), style = TextStyle(fontSize = small, color = Muted), modifier = Modifier.padding(top = 8.dp))
    }
}

/** A number pad to correct an amount the bank read wrong; ✓ with nothing typed keeps the old one. */
@Composable
private fun ColumnScope.AmountKeypad(onDone: (Long?) -> Unit) {
    var typed by remember { mutableStateOf("") }
    Text(
        typed.ifEmpty { "0" }, style = MaterialTheme.typography.displayLarge, color = LauncherRed,
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp), textAlign = TextAlign.Center,
    )
    Spacer(Modifier.weight(1f))
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "<")
    keys.chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth()) {
            row.forEach { k ->
                Box(
                    Modifier.weight(1f).height(64.dp).clip(RoundedCornerShape(16.dp)).clickable(role = Role.Button) {
                        typed = when {
                            k == "<" -> typed.dropLast(1)
                            k == "." -> if ('.' in typed) typed else typed + k
                            Regex("\\.\\d\\d$").containsMatchIn(typed) -> typed
                            else -> typed + k
                        }
                    }.semantics { contentDescription = if (k == "<") "Borrar" else k },
                    contentAlignment = Alignment.Center,
                ) {
                    if (k == "<") StrokeIcon("borrar", StrokeState.DONE, 26.dp) else Text(k, style = TextStyle(fontSize = 28.sp, color = White))
                }
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.clickable(role = Role.Button) { onDone(typed.toDoubleOrNull()?.takeIf { it > 0 }?.let { Math.round(it * 100) }) }
            .semantics { contentDescription = "Confirmar monto" }.padding(8.dp)) {
            StrokeIcon("listo", StrokeState.NOW, 36.dp)
        }
    }
}

/** It was food: one of the recipes (most used first), loose ingredients, or ✓ to mark it as a meal without detail. */
@Composable
private fun ColumnScope.MealStep(payment: PendingPayment, viewModel: LauncherViewModel) {
    val recipes by viewModel.recipes.collectAsState()
    val meals by viewModel.meals.collectAsState()
    var busy by remember(payment.id) { mutableStateOf(false) }
    var pickedRecipe by remember(payment.id) { mutableStateOf<String?>(null) }
    var quickTap by remember(payment.id) { mutableIntStateOf(0) }
    var ingredientsOpen by remember(payment.id) { mutableStateOf(false) }
    val ordered = remember(recipes, meals) {
        val uses = meals.groupingBy { it.recipeId }.eachCount()
        recipes.sortedByDescending { uses[it.id] ?: 0 }
    }

    LaunchedEffect(pickedRecipe) {
        val id = pickedRecipe ?: return@LaunchedEffect
        delay(DRAW_WAIT_MS / 2)
        viewModel.forcedSaveRecipeMeal(id)
    }
    LaunchedEffect(quickTap) {
        if (quickTap == 0) return@LaunchedEffect
        delay(DRAW_WAIT_MS)
        viewModel.forcedSaveMeal(payment.merchant.ifBlank { "Comida" })
    }

    if (ingredientsOpen) {
        IngredientPicker(viewModel, onClose = { ingredientsOpen = false }) { description, items ->
            if (!busy) { busy = true; viewModel.forcedSaveMeal(description, items) }
        }
        return
    }

    Amount(payment.amountCents, big = false, modifier = Modifier.fillMaxWidth().padding(top = 24.dp))
    LazyVerticalGrid(
        GridCells.Fixed(3), Modifier.weight(1f).fillMaxWidth().padding(top = 36.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(ordered, key = { it.id }) { recipe ->
            RecipeCircle(recipe, picked = pickedRecipe == recipe.id) {
                if (!busy) { busy = true; pickedRecipe = recipe.id }
            }
        }
        item {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(72.dp).clip(CircleShape).border(1.5.dp, Color(0xFF333333), CircleShape)
                        .clickable(enabled = !busy, role = Role.Button) { ingredientsOpen = true }.semantics { contentDescription = "Con ingredientes" },
                    contentAlignment = Alignment.Center,
                ) { StrokeIcon("hoja", StrokeState.NOW, 32.dp) }
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.clickable(enabled = !busy, role = Role.Button) { busy = true; quickTap++ }.semantics { contentDescription = "Solo marcar comida" }.padding(8.dp)) {
            StrokeIcon("listo", if (quickTap > 0) StrokeState.NOW else StrokeState.DONE, 32.dp, playKey = quickTap)
        }
    }
}

/** A recipe as its photo in a circle; without a photo, its initial. The name stays for accessibility. */
@Composable
private fun RecipeCircle(recipe: Recipe, picked: Boolean, onPick: () -> Unit) {
    val context = LocalContext.current
    val photo by produceState<Bitmap?>(null, recipe.photoUri) {
        value = recipe.photoUri?.let { uri ->
            withContext(Dispatchers.IO) {
                runCatching {
                    val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                    context.contentResolver.openInputStream(Uri.parse(uri))?.use { BitmapFactory.decodeStream(it, null, opts) }
                }.getOrNull()
            }
        }
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).border(1.5.dp, if (picked) White else Color(0xFF333333), CircleShape)
                .clickable(role = Role.Button, onClick = onPick).semantics { contentDescription = recipe.name },
            contentAlignment = Alignment.Center,
        ) {
            val bitmap = photo
            if (bitmap != null) Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Text(recipe.name.take(1).uppercase(), style = TextStyle(fontSize = 22.sp, color = if (picked) White else Muted))
        }
    }
}

/** Search ingredients, tap to add one portion; tapping the grams steps them by 50 (up to 300, then 50 again). */
@Composable
private fun ColumnScope.IngredientPicker(viewModel: LauncherViewModel, onClose: () -> Unit, onSave: (String, Map<String, Float>) -> Unit) {
    val all by viewModel.ingredients.collectAsState()
    var query by remember { mutableStateOf("") }
    val picked = remember { mutableStateMapOf<String, Float>() }
    val order = remember { mutableStateListOf<Ingredient>() }
    val results = remember(all, query) { all.filter { it.name.contains(query.trim(), ignoreCase = true) }.take(40) }

    Column(
        Modifier.weight(1f).fillMaxWidth().padding(top = 16.dp).clip(RoundedCornerShape(24.dp)).background(Card)
            .border(1.dp, Line, RoundedCornerShape(24.dp)).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            StrokeIcon("hoja", StrokeState.NOW, 24.dp)
            Text("✕", style = MaterialTheme.typography.labelSmall, color = Muted,
                modifier = Modifier.clickable(role = Role.Button, onClick = onClose).semantics { contentDescription = "Cerrar" }.padding(8.dp))
        }
        BasicTextField(
            query, { query = it }, Modifier.fillMaxWidth().semantics { contentDescription = "Buscar ingrediente" },
            textStyle = TextStyle(fontSize = 20.sp, color = White), cursorBrush = SolidColor(LauncherRed), singleLine = true,
            decorationBox = { inner -> Column { inner(); Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(1.dp).background(Color(0xFF333333))) } },
        )
        if (order.isNotEmpty()) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Only the last three stay visible; the rest are still in the meal.
            order.takeLast(3).forEach { ing ->
                Row(
                    Modifier.border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(ing.name, style = TextStyle(fontSize = 13.sp, color = White), maxLines = 1)
                    Text(
                        "${picked[ing.id]?.toInt() ?: 0} g", style = MaterialTheme.typography.labelSmall, color = LauncherRed,
                        modifier = Modifier.clickable(role = Role.Button) {
                            val g = picked[ing.id] ?: 0f
                            picked[ing.id] = if (g >= 300f) 50f else g + 50f
                        }.semantics { contentDescription = "Cambiar gramos" },
                    )
                }
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            items(results, key = { it.id }) { ing ->
                Row(
                    Modifier.fillMaxWidth().clickable {
                        if (ing.id in picked) picked[ing.id] = picked.getValue(ing.id) + ing.portionGrams
                        else { picked[ing.id] = ing.portionGrams; order.add(ing) }
                    }.padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(ing.name, style = TextStyle(fontSize = 16.sp, color = if (ing.id in picked) White else Muted))
                    Text("+", style = MaterialTheme.typography.labelSmall, color = Muted)
                }
            }
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier.clickable(enabled = picked.isNotEmpty(), role = Role.Button) { onSave(order.joinToString(", ") { it.name }, picked.toMap()) }
                    .semantics { contentDescription = "Guardar comida" }.padding(8.dp),
            ) { StrokeIcon("listo", if (picked.isEmpty()) StrokeState.OFF else StrokeState.NOW, 36.dp) }
        }
    }
}

/** Salida asked to register the day: the meals that already passed, with their hour; tap marks, ✓ finishes. */
@Composable
private fun ColumnScope.RoutineMeals(viewModel: LauncherViewModel) {
    val unmarked by viewModel.unmarkedMeals.collectAsState()
    val habits by viewModel.completedHabits.collectAsState()
    // Marked meals leave unmarkedMeals; keep them on screen so a wrong tap can be undone.
    val shown = remember { mutableStateListOf<MealSlot>() }
    LaunchedEffect(unmarked) { unmarked.filter { u -> shown.none { it.id == u.id } }.forEach { shown.add(it) } }
    val date = LocalDate.now()
    val sorted = shown.sortedBy { it.time }
    val next = sorted.firstOrNull { "$date|meal_${it.id}" !in habits }

    Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            sorted.forEach { slot ->
                val key = "$date|meal_${slot.id}"
                val done = key in habits
                Column(
                    Modifier.clickable(role = Role.Button) { viewModel.setHabit(key, !done) }.semantics { contentDescription = "${slot.name} ${slot.time.format(hhmm)}" },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StrokeIcon(if (slot.kind == MealKind.SNACK) "colacion" else "plato", if (done) StrokeState.DONE else StrokeState.OFF, 64.dp, playKey = done)
                    Text(
                        slot.time.format(hhmm), style = MaterialTheme.typography.labelSmall,
                        color = when { done -> White; slot == next -> LauncherRed; else -> Muted },
                    )
                }
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.clickable(role = Role.Button) { viewModel.finishRoutineRegistration() }.semantics { contentDescription = "Listo" }.padding(8.dp)) {
            StrokeIcon("listo", StrokeState.NOW, 36.dp)
        }
    }
}
