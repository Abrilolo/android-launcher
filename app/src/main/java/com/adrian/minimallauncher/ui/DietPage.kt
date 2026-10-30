package com.adrian.minimallauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.MealKind
import com.adrian.minimallauncher.data.MealRecord
import com.adrian.minimallauncher.data.ModuleSettings
import com.adrian.minimallauncher.data.PersonalRecordActions
import com.adrian.minimallauncher.data.PersonalRecords
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.feature.SettingsGear
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.feature.food.CheckButton
import com.adrian.minimallauncher.feature.food.FoodActions
import com.adrian.minimallauncher.feature.food.FoodSheet
import com.adrian.minimallauncher.feature.food.FoodWeek
import com.adrian.minimallauncher.feature.food.IconButton
import com.adrian.minimallauncher.feature.food.IconTabs
import com.adrian.minimallauncher.feature.food.IngredientBuilder
import com.adrian.minimallauncher.feature.food.Nutrition
import com.adrian.minimallauncher.feature.food.PickedItem
import com.adrian.minimallauncher.feature.food.Recipe
import com.adrian.minimallauncher.feature.food.RecipeGrid
import com.adrian.minimallauncher.feature.food.RecipePhoto
import com.adrian.minimallauncher.feature.food.RecipeScreen
import com.adrian.minimallauncher.feature.food.mealStrokeIcon
import com.adrian.minimallauncher.feature.food.recipeNutrition
import com.adrian.minimallauncher.feature.food.whole
import com.adrian.minimallauncher.ui.components.DotActivation
import com.adrian.minimallauncher.ui.components.DotMatrixText
import com.adrian.minimallauncher.ui.components.LocalReactions
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
import com.adrian.minimallauncher.viewmodel.LauncherViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val hhmm = DateTimeFormatter.ofPattern("HH:mm")
private val White = Color(0xFFF5F5F5)
private val Muted = Color(0xFFB0B0B0)

/** Cena lets its steam rise when marked, the chocolate bar gets bitten; the others light up bottom-up (home tile). */
fun mealActivation(icon: String) = when (icon) {
    "cena" -> DotActivation.Steam
    "snack" -> DotActivation.Bite
    else -> DotActivation.Sweep
}

/** Stroke icon of each daily habit in [DIET_HABITS]. */
private val HABIT_STROKES = mapOf("walk" to "caminar", "no_extras" to "sinazucar", "protein" to "huevo", "sleep" to "dormir")

/** Water containers per row; a bigger goal wraps to another row. */
private const val GLASSES_PER_ROW = 8

/**
 * Comida, following docs/design/concept/ComidaVisual.html. Two views behind icon tabs: Hoy (the kcal ring against the
 * goal, protein and fiber, the day's meals with their hour, recipe photo and kcal, water and habits) and Semana
 * (weight, BMI and the week's kcal). The book opens the recipes, the gear the settings and + logs food from a recipe
 * or from ingredients. No labels while using it; what comes next has its hour in red.
 */
@Composable
fun DietPage(
    now: LocalDateTime,
    records: PersonalRecords,
    actions: PersonalRecordActions,
    settings: ModuleSettings,
    vm: LauncherViewModel,
    onSetHabit: (String, Boolean) -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    val reactions = LocalReactions.current
    val date = now.toLocalDate()
    val recipes by vm.recipes.collectAsState()
    val recipeItems by vm.recipeItems.collectAsState()
    val ingredients by vm.ingredients.collectAsState()
    val nutrition = remember(recipes, recipeItems, ingredients) { recipeNutrition(recipes, recipeItems, ingredients) }
    val food = remember(vm) {
        FoodActions(
            search = vm::searchIngredients,
            addIngredient = vm::addIngredient,
            updateIngredient = vm::updateIngredient,
            newRecipe = vm::newRecipe,
            saveRecipe = vm::saveRecipe,
            deleteRecipe = vm::deleteRecipe,
            recipeDetail = vm::recipeDetail,
            logRecipe = { id, servings -> vm.logRecipeMeal(id, servings) },
            logIngredients = { description, items -> vm.logIngredientMeal(description, items) },
            addWeight = { vm.addBodyMetric("WEIGHT", it) },
        )
    }

    var tab by rememberSaveable { mutableStateOf("hoy") }
    // Sheets: the log sheet (for a slot or loose), the recipe library, and one recipe ("new" for a new one).
    var logOpen by rememberSaveable { mutableStateOf(false) }
    var logSlot by rememberSaveable { mutableStateOf<String?>(null) }
    var libraryOpen by rememberSaveable { mutableStateOf(false) }
    var openRecipe by rememberSaveable { mutableStateOf<String?>(null) }

    val todayMeals = records.meals.filter { it.createdAt.toLocalDate() == date }.sortedBy { it.createdAt }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row {
                    IconButton("Hoy", { tab = "hoy" }) { StrokeIcon("hoy", if (tab == "hoy") StrokeState.DONE else StrokeState.OFF, 22.dp) }
                    IconButton("Semana", { tab = "semana" }) { StrokeIcon("semana", if (tab == "semana") StrokeState.DONE else StrokeState.OFF, 22.dp) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton("Recetas", { libraryOpen = true }) { StrokeIcon("escuela", StrokeState.DONE, 22.dp, color = Muted) }
                    SettingsGear(onSettings)
                }
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                if (tab == "hoy") {
                    TodayView(date, settings, records, todayMeals, recipes, onSetHabit, actions) { slot -> logSlot = slot; logOpen = true }
                } else {
                    FoodWeek(date, records.meals, records.bodyMetrics, settings, food.addWeight)
                }
            }
        }
        if (tab == "hoy") {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 28.dp)
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(White)
                    .clickable(role = Role.Button) { logSlot = null; logOpen = true }
                    .semantics { contentDescription = "Registrar comida" },
                contentAlignment = Alignment.Center,
            ) { Text("+", style = MaterialTheme.typography.headlineSmall, color = Color.Black) }
        }

        if (logOpen) {
            val slot = settings.meals.firstOrNull { it.id == logSlot }
            LogSheet(
                slotTime = slot?.time?.format(hhmm),
                slotDone = slot != null && mealKey(date, slot.id) in records.completedHabits,
                slotMeals = slot?.let { s -> todayMeals.filter { nearestSlot(it, settings)?.id == s.id } }.orEmpty(),
                recipes = recipes,
                nutrition = nutrition,
                food = food,
                onToggleSlot = {
                    if (slot != null) {
                        val marking = mealKey(date, slot.id) !in records.completedHabits
                        onSetHabit(mealKey(date, slot.id), marking)
                        if (marking) reactions.tick()
                    }
                    logOpen = false
                },
                onDeleteMeal = actions.deleteMeal,
                onLogged = {
                    if (slot != null) onSetHabit(mealKey(date, slot.id), true)
                    reactions.tick()
                    logOpen = false
                },
                description = slot?.name ?: "Comida",
                onClose = { logOpen = false },
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (libraryOpen) {
            FoodSheet(
                onClose = { libraryOpen = false },
                modifier = Modifier.fillMaxSize(),
                header = {
                    Row(Modifier.fillMaxWidth(), Arrangement.End) {
                        IconButton("Nueva receta", { libraryOpen = false; openRecipe = "new" }) { StrokeIcon("nueva", StrokeState.DONE, 22.dp, color = Muted) }
                    }
                },
            ) {
                RecipeGrid(recipes, nutrition, showPrep = true) { libraryOpen = false; openRecipe = it.id }
            }
        }
        openRecipe?.let { id ->
            RecipeScreen(id.takeIf { it != "new" }, food, onClose = { openRecipe = null }, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun TodayView(
    date: LocalDate,
    settings: ModuleSettings,
    records: PersonalRecords,
    todayMeals: List<MealRecord>,
    recipes: List<Recipe>,
    onSetHabit: (String, Boolean) -> Unit,
    actions: PersonalRecordActions,
    onOpenSlot: (String?) -> Unit,
) {
    val reactions = LocalReactions.current
    val eaten = todayMeals.fold(Nutrition()) { acc, m -> acc + Nutrition(m.kcal, m.proteinG, m.carbsG, m.fatG, m.fiberG) }
    KcalRing(eaten.kcal, settings.kcalGoal)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        MacroBar("huevo", eaten.proteinG, settings.proteinGoalG, Modifier.weight(1f))
        MacroBar("hoja", eaten.fiberG, settings.fiberGoalG, Modifier.weight(1f))
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF222222)))

    // The plan: each meal slot with what was logged near its hour; meals with no slot at all show on their own.
    val photoOf = recipes.associate { it.id to it.photoUri }
    val meals = settings.meals
    val done = meals.map { mealKey(date, it.id) in records.completedHabits }
    val next = done.indexOf(false)
    val bySlot = todayMeals.groupBy { nearestSlot(it, settings)?.id }
    Column {
        meals.forEachIndexed { i, slot ->
            val logged = bySlot[slot.id].orEmpty()
            val time = slot.time.format(hhmm)
            var play by remember { mutableIntStateOf(0) }
            PlanRow(
                time = time,
                timeColor = when { done[i] -> White; i == next -> LauncherRed; else -> Muted },
                icon = { StrokeIcon(mealStrokeIcon(slot.icon), if (done[i]) StrokeState.NOW else StrokeState.OFF, if (slot.kind == MealKind.MAIN) 34.dp else 28.dp, playKey = play) },
                photo = logged.firstNotNullOfOrNull { m -> m.recipeId?.let { photoOf[it] } },
                hasMeal = logged.isNotEmpty(),
                dim = !done[i],
                kcal = logged.takeIf { it.isNotEmpty() }?.sumOf { it.kcal.toDouble() }?.toFloat(),
                description = "${slot.name} $time",
                onClick = { onOpenSlot(slot.id) },
                onLongClick = {
                    val marking = !done[i]
                    onSetHabit(mealKey(date, slot.id), marking)
                    if (marking) { play++; if (done.count { it } + 1 == meals.size) reactions.done() else reactions.tick() }
                },
            )
            LaunchedEffect(done[i]) { if (done[i]) play++ }
        }
        bySlot[null].orEmpty().forEach { m ->
            val time = Instant.ofEpochMilli(m.createdAt).atZone(ZoneId.systemDefault()).format(hhmm)
            PlanRow(time, White, { StrokeIcon("plato", StrokeState.DONE, 26.dp) }, m.recipeId?.let { photoOf[it] }, true, false, m.kcal, "${m.description} $time", { onOpenSlot(null) }, null)
        }
    }

    WaterRow(date, settings, records.completedHabits, onSetHabit)
    Row(Modifier.fillMaxWidth()) {
        DIET_HABITS.forEach { (id, label) ->
            val isDone = habitKey(date, id) in records.completedHabits
            var play by remember { mutableIntStateOf(0) }
            Box(
                Modifier
                    .weight(1f)
                    .clickable(role = Role.Checkbox) {
                        actions.toggleHabit(habitKey(date, id))
                        if (!isDone) { play++; reactions.tick() }
                    }
                    .semantics { contentDescription = label }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) { StrokeIcon(HABIT_STROKES.getValue(id), if (isDone) StrokeState.NOW else StrokeState.OFF, 30.dp, playKey = play) }
        }
    }
}

/** The kcal left out of the goal in a ring that fills white; past the goal the excess turns red and so does the number. */
@Composable
private fun KcalRing(eaten: Float, goal: Int) {
    val fill = remember { Animatable(0f) }
    val extra = remember { Animatable(0f) }
    val target = if (goal > 0) (eaten / goal).coerceIn(0f, 1f) else 0f
    val overTarget = if (goal > 0) ((eaten - goal) / goal).coerceIn(0f, 1f) else 0f
    LaunchedEffect(target, overTarget) {
        fill.animateTo(target, tween(900, easing = FastOutSlowInEasing))
        extra.animateTo(overTarget, tween(600, easing = FastOutSlowInEasing))
    }
    val over = goal > 0 && eaten > goal
    Box(
        Modifier.fillMaxWidth().height(200.dp).semantics {
            contentDescription = if (goal > 0) "Quedan ${(goal - eaten).whole()} kcal de $goal" else "${eaten.whole()} kcal"
        },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(200.dp)) {
            val w = 3.dp.toPx()
            val inset = w / 2 + 6.dp.toPx()
            val arc = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            drawCircle(Color(0xFF1C1C1C), arc.width / 2, center, style = Stroke(w))
            if (fill.value > 0f) drawArc(White, -90f, 360f * fill.value, false, topLeft, arc, style = Stroke(w, cap = StrokeCap.Round))
            if (extra.value > 0f) drawArc(LauncherRed, -90f, 360f * extra.value, false, topLeft, arc, style = Stroke(w, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val number = when {
                goal <= 0 -> eaten.whole()
                over -> "+" + (eaten - goal).whole()
                else -> (goal - eaten).whole()
            }
            DotMatrixText(number, cell = 5.dp, color = if (over) LauncherRed else White, reveal = number)
            if (goal > 0) SmallLabel(goal.toString())
        }
    }
}

/** A nutrient against its goal: icon (with its red detail once met), a thin bar and the grams. */
@Composable
private fun MacroBar(icon: String, value: Float, goal: Int, modifier: Modifier = Modifier) {
    val met = goal > 0 && value >= goal
    val width = remember { Animatable(0f) }
    LaunchedEffect(value, goal) { width.animateTo(if (goal > 0) (value / goal).coerceIn(0f, 1f) else 0f, tween(700)) }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StrokeIcon(icon, if (met) StrokeState.NOW else StrokeState.DONE, 22.dp)
        Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF1F1F1F))) {
            Box(Modifier.fillMaxWidth(width.value).height(3.dp).clip(RoundedCornerShape(2.dp)).background(White))
        }
        SmallLabel(value.whole(), color = if (met) White else Muted)
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PlanRow(
    time: String,
    timeColor: Color,
    icon: @Composable () -> Unit,
    photo: String?,
    hasMeal: Boolean,
    dim: Boolean,
    kcal: Float?,
    description: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics { contentDescription = description }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(time, Modifier.width(52.dp), style = MaterialTheme.typography.labelLarge, color = timeColor)
        Box(Modifier.width(44.dp), contentAlignment = Alignment.Center) { icon() }
        if (hasMeal || photo != null) RecipePhoto(photo, 40.dp, dim = dim) else Spacer(Modifier.size(40.dp))
        Spacer(Modifier.weight(1f))
        Text(kcal?.whole() ?: "", style = MaterialTheme.typography.labelLarge, color = if (dim) Muted else White)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF141414)))
}

/**
 * One glass (or thermos) per goal step; tapping fills up to it (each new one draws itself after the previous), tapping
 * the last full one empties it. Underneath, how much each holds and how much is drunk.
 */
@Composable
private fun WaterRow(date: LocalDate, settings: ModuleSettings, habits: Set<String>, onSetHabit: (String, Boolean) -> Unit) {
    val reactions = LocalReactions.current
    val goal = settings.waterGoal
    val ml = settings.waterGlassMl
    val icon = if (settings.waterContainer == "termo") "termo" else "agua"
    val unit = waterUnit(settings.waterContainer)
    val count = (1..goal).count { waterKey(date, it) in habits }
    val plays = remember { mutableStateMapOf<Int, Int>() }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val gap = 4.dp
            val perRow = minOf(goal, GLASSES_PER_ROW).coerceAtLeast(1)
            val glass = minOf(36.dp, (maxWidth - gap * (perRow - 1)) / perRow)
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                (1..goal).chunked(perRow).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        row.forEach { g ->
                            Box(
                                Modifier
                                    .size(glass)
                                    .clickable(role = Role.Button) {
                                        val target = if (g == count) g - 1 else g
                                        for (n in 1..goal) {
                                            val key = waterKey(date, n)
                                            val done = n <= target
                                            if ((key in habits) != done) onSetHabit(key, done)
                                        }
                                        if (target > count) {
                                            for (n in count + 1..target) plays[n] = (plays[n] ?: 0) + 1
                                            reactions.tick()
                                        }
                                    }
                                    .semantics { contentDescription = "${unit.lowercase().replaceFirstChar { it.uppercase() }} $g" },
                                contentAlignment = Alignment.Center,
                            ) {
                                WaterGlass(icon, g <= count, glass, plays[g] ?: 0, (g - 1 - (count - 1).coerceAtLeast(0)).coerceAtLeast(0) * 120L)
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            SmallLabel(formatMl(ml))
            SmallLabel(formatMl(count * ml), color = if (count > 0) White else Muted)
        }
    }
}

/** A glass that draws itself [delayMs] after a tap filled it, so several fill one after another. */
@Composable
private fun WaterGlass(icon: String, full: Boolean, size: androidx.compose.ui.unit.Dp, play: Int, delayMs: Long) {
    var key by remember { mutableIntStateOf(0) }
    LaunchedEffect(play) {
        if (play == 0) return@LaunchedEffect
        kotlinx.coroutines.delay(delayMs)
        key = play
    }
    StrokeIcon(icon, if (full) StrokeState.NOW else StrokeState.OFF, size * 0.85f, playKey = key)
}

/**
 * Log food: for a slot, ✓ marks it without logging and its logged meals can be removed; then a recipe (tap a photo)
 * or ingredients from the base.
 */
@Composable
private fun LogSheet(
    slotTime: String?,
    slotDone: Boolean,
    slotMeals: List<MealRecord>,
    recipes: List<Recipe>,
    nutrition: Map<String, Nutrition>,
    food: FoodActions,
    onToggleSlot: () -> Unit,
    onDeleteMeal: (String) -> Unit,
    onLogged: () -> Unit,
    description: String,
    onClose: () -> Unit,
    modifier: Modifier,
) {
    var mode by rememberSaveable { mutableStateOf("receta") }
    val picked = remember { mutableStateListOf<PickedItem>() }
    FoodSheet(
        onClose = onClose,
        modifier = modifier,
        header = {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                IconTabs(listOf("receta" to "escuela", "ingredientes" to "tazon"), mode) { mode = it }
                if (slotTime != null) SmallLabel(slotTime, color = if (slotDone) White else LauncherRed)
            }
        },
    ) {
        if (slotTime != null) {
            slotMeals.forEach { m ->
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text(m.description, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    SmallLabel(m.kcal.whole(), color = White)
                    IconButton("Borrar ${m.description}", { onDeleteMeal(m.id) }) { StrokeIcon("papelera", StrokeState.OFF, 18.dp) }
                }
            }
            Row(Modifier.fillMaxWidth(), Arrangement.End) {
                CheckButton(if (slotDone) "Desmarcar" else "Marcar sin registrar", onToggleSlot, filled = !slotDone, size = 44.dp)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF222222)))
        }
        if (mode == "receta") {
            RecipeGrid(recipes, nutrition, showPrep = false) { food.logRecipe(it.id, 1f); onLogged() }
        } else {
            IngredientBuilder(picked, food.search, food.addIngredient, food.updateIngredient) {
                CheckButton("Guardar comida", {
                    val items = picked.groupBy { it.ingredient.id }.mapValues { (_, l) -> l.sumOf { it.grams.toDouble() }.toFloat() }
                    if (items.isNotEmpty()) {
                        val name = if (slotTime != null) description else picked.joinToString(", ") { it.ingredient.name }.take(60)
                        food.logIngredients(name, items)
                        picked.clear()
                        onLogged()
                    }
                }, size = 48.dp)
            }
        }
    }
}

/** The meal slot whose hour is closest to when [meal] was logged; null when there are no slots. */
private fun nearestSlot(meal: MealRecord, settings: ModuleSettings) = settings.meals.minByOrNull { slot ->
    val t = Instant.ofEpochMilli(meal.createdAt).atZone(ZoneId.systemDefault()).toLocalTime()
    Math.abs(t.toSecondOfDay() - slot.time.toSecondOfDay())
}

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
