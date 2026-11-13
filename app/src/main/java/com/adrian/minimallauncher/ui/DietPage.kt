package com.adrian.minimallauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.key
import androidx.compose.ui.unit.sp
import com.adrian.minimallauncher.feature.finance.ActionColor
import com.adrian.minimallauncher.feature.finance.FinIcon
import com.adrian.minimallauncher.feature.finance.FinanceDock
import com.adrian.minimallauncher.feature.finance.FinanceMuted
import com.adrian.minimallauncher.feature.finance.FinanceWhite
import com.adrian.minimallauncher.feature.finance.GlassLine
import com.adrian.minimallauncher.feature.finance.InfoIcon
import com.adrian.minimallauncher.feature.finance.LabelCream
import com.adrian.minimallauncher.feature.finance.MoneyPlenty
import com.adrian.minimallauncher.feature.finance.PlanState
import com.adrian.minimallauncher.feature.finance.Pop
import com.adrian.minimallauncher.feature.finance.PopRow
import com.adrian.minimallauncher.feature.finance.PopRule
import com.adrian.minimallauncher.feature.finance.SectionHead
import com.adrian.minimallauncher.feature.finance.SectionLabel
import com.adrian.minimallauncher.feature.finance.StateDot
import com.adrian.minimallauncher.feature.finance.SwipePages
import com.adrian.minimallauncher.feature.finance.TextBlue
import com.adrian.minimallauncher.feature.finance.TintFigure
import com.adrian.minimallauncher.feature.finance.glass
import com.adrian.minimallauncher.feature.finance.usageColor
import com.adrian.minimallauncher.feature.food.FoodGear
import java.time.LocalTime
import com.adrian.minimallauncher.data.MealRecord
import com.adrian.minimallauncher.data.ModuleSettings
import com.adrian.minimallauncher.data.PersonalRecordActions
import com.adrian.minimallauncher.data.PersonalRecords
import com.adrian.minimallauncher.feature.DockItem
import com.adrian.minimallauncher.feature.LauncherAccent
import com.adrian.minimallauncher.feature.LauncherDanger
import com.adrian.minimallauncher.feature.ModuleDock
import com.adrian.minimallauncher.feature.ModuleTitle
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.feature.SettingsGear
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
import com.adrian.minimallauncher.ui.components.LocalReactions
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
import com.adrian.minimallauncher.viewmodel.LauncherViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val hhmm = DateTimeFormatter.ofPattern("HH:mm")
private val spanish = Locale.forLanguageTag("es")
private val White = Color(0xFFF5F5F5)
private val Muted = Color(0xFFB0B0B0)

/** Cena lets its steam rise when marked, the chocolate bar gets bitten; the others light up bottom-up (home tile). */
fun mealActivation(icon: String) = when (icon) {
    "cena" -> DotActivation.Steam
    "snack" -> DotActivation.Bite
    else -> DotActivation.Sweep
}

/** "Sábado, 31 octubre". */
private fun dayTitle(date: LocalDate): String {
    val day = date.dayOfWeek.getDisplayName(TextStyle.FULL, spanish).replaceFirstChar { it.uppercase(spanish) }
    return "$day, ${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.FULL, spanish)}"
}

/** Stroke icon of each daily habit in [DIET_HABITS]. */
private val HABIT_STROKES = mapOf("walk" to "caminar", "no_extras" to "sinazucar", "protein" to "huevo", "sleep" to "dormir")

/** Short names of the same habits, under their icons. */
private val HABIT_NAMES = mapOf("walk" to "Caminar", "no_extras" to "Sin azúcar", "protein" to "Proteína", "sleep" to "Dormir")

/** Water containers per row; a bigger goal wraps to another row. */
private const val GLASSES_PER_ROW = 8

/**
 * Comida, with the look and the pieces of Finanzas (2026-11-11). Two views behind the dock: Hoy (the kcal left, protein
 * and fiber, the day's meals with their hour and state, then water and habits to swipe) and Semana. Recetas opens the
 * library; the gear opens the settings; + logs food from a recipe or from ingredients.
 */
@Composable
fun DietPage(
    now: LocalDateTime,
    records: PersonalRecords,
    actions: PersonalRecordActions,
    settings: ModuleSettings,
    vm: LauncherViewModel,
    onSetHabit: (String, Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    // Sheets: the log sheet (for a slot or loose), the recipe library, and one recipe ("new" for a new one).
    var logOpen by rememberSaveable { mutableStateOf(false) }
    var logSlot by rememberSaveable { mutableStateOf<String?>(null) }
    var libraryOpen by rememberSaveable { mutableStateOf(false) }
    var openRecipe by rememberSaveable { mutableStateOf<String?>(null) }

    if (settingsOpen) {
        FoodGear(settings, vm::updateModuleSettings, { settingsOpen = false }, modifier)
        return
    }
    BackHandler(onBack = onBack)

    val todayMeals = records.meals.filter { it.createdAt.toLocalDate() == date }.sortedBy { it.createdAt }
    val mealsLeft = settings.meals.count { mealKey(date, it.id) !in records.completedHabits }
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).padding(horizontal = 24.dp)) {
                ModuleTitle(if (tab == "hoy") "Comida" else "Semana", { settingsOpen = true })
                key(tab) {
                    Column(
                        Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 12.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(28.dp),
                    ) {
                        if (tab == "hoy") {
                            TodayView(now, settings, records, todayMeals, recipes, onSetHabit, actions) { slot -> logSlot = slot; logOpen = true }
                        } else {
                            FoodWeek(date, records.meals, records.bodyMetrics, settings, food.addWeight)
                        }
                    }
                }
            }
            FinanceDock(
                listOf(
                    DockItem("hoy", "Hoy", "hoy", badge = mealsLeft.takeIf { it > 0 }?.toString()),
                    DockItem("semana", "Semana", "semana"),
                    DockItem("recetas", "Recetas", "escuela"),
                ),
                tab,
                onOpen = { key -> if (key == "recetas") libraryOpen = true else tab = key },
            )
        }

        if (!logOpen && !libraryOpen && openRecipe == null && tab == "hoy") {
            AddMealButton(Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 92.dp)) { logSlot = null; logOpen = true }
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
                        FinIcon("nueva", "Nueva receta", ActionColor.Add) { libraryOpen = false; openRecipe = "new" }
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

/** The blue + over the dock, as in Finanzas: it opens the log for a meal that is not on the plan. */
@Composable
private fun AddMealButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .size(56.dp)
            .background(LauncherAccent, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Registrar comida" },
        contentAlignment = Alignment.Center,
    ) { StrokeIcon("nueva", StrokeState.DONE, 26.dp, color = Color.White) }
}

/** How a meal of the plan stands: done, the next one (pink once its hour passed), passed without marking, or to come. */
private fun mealState(done: Boolean, next: Boolean, time: LocalTime, now: LocalTime): PlanState = when {
    done -> PlanState.DONE
    now.isAfter(time.plusMinutes(45)) -> PlanState.LATE
    next -> PlanState.TODAY
    else -> PlanState.PLANNED
}

/**
 * Hoy, in reading order:
 * 1. Te quedan: the kcal left today, big, in its state color; a tap shows the sum and the macros.
 * 2. Proteína and Fibra, each with its bar toward the goal.
 * 3. Comidas: each meal with its state, hour, what was logged and its kcal. A tap logs; a long press marks it.
 * 4. Agua and Hábitos, to swipe.
 */
@Composable
private fun TodayView(
    now: LocalDateTime,
    settings: ModuleSettings,
    records: PersonalRecords,
    todayMeals: List<MealRecord>,
    recipes: List<Recipe>,
    onSetHabit: (String, Boolean) -> Unit,
    actions: PersonalRecordActions,
    onOpenSlot: (String?) -> Unit,
) {
    val date = now.toLocalDate()
    val reactions = LocalReactions.current
    val eaten = todayMeals.fold(Nutrition()) { acc, m -> acc + Nutrition(m.kcal, m.proteinG, m.carbsG, m.fatG, m.fiberG) }

    // 1. Te quedan
    val goal = settings.kcalGoal
    val left = goal - eaten.kcal.toInt()
    val color = if (goal > 0) usageColor(eaten.kcal.toLong(), goal.toLong()) else FinanceWhite
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(if (goal <= 0) "Comiste hoy" else if (left >= 0) "Te quedan" else "Te pasaste")
            InfoIcon("Las kcal que te quedan hoy para tu meta: verde con espacio, azul a la mitad, ámbar cerca y rosa si te pasaste. Toca el número para ver de dónde sale.")
        }
        Pop(description = "Ver cómo se calcula", pop = {
            if (goal > 0) PopRow("Meta", "%,d kcal".format(goal))
            PopRow("Comiste", "%,d kcal".format(eaten.kcal.toInt()), TextBlue)
            if (goal > 0) {
                PopRule()
                PopRow(if (left >= 0) "Te quedan" else "Te pasaste", "%,d kcal".format(kotlin.math.abs(left)), color)
            }
            PopRule()
            PopRow("Carbohidratos", "${eaten.carbsG.whole()} g")
            PopRow("Grasa", "${eaten.fatG.whole()} g")
        }) {
            Column {
                Text(
                    "%,d".format(if (goal > 0) kotlin.math.abs(left) else eaten.kcal.toInt()),
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 54.sp, lineHeight = 60.sp),
                    color = color,
                )
                Text(if (goal > 0) "kcal · comiste ${"%,d".format(eaten.kcal.toInt())}" else "kcal", style = MaterialTheme.typography.titleMedium, color = FinanceWhite)
            }
        }
    }

    // 2. Proteína · Fibra
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        NutrientFigure("Proteína", eaten.proteinG, settings.proteinGoalG, FoodProtein, Modifier.weight(1f))
        NutrientFigure("Fibra", eaten.fiberG, settings.fiberGoalG, FoodFiber, Modifier.weight(1f))
    }

    // 3. Comidas
    val photoOf = recipes.associate { it.id to it.photoUri }
    val meals = settings.meals
    val done = meals.map { mealKey(date, it.id) in records.completedHabits }
    val next = done.indexOf(false)
    val bySlot = todayMeals.groupBy { nearestSlot(it, settings)?.id }
    Column {
        SectionHead("Comidas", "Toca una comida para registrar qué comiste; mantén presionado para marcarla sin registrar. Azul es la que sigue, rosa si se pasó su hora.")
        meals.forEachIndexed { i, slot ->
            val logged = bySlot[slot.id].orEmpty()
            val state = mealState(done[i], i == next, slot.time, now.toLocalTime())
            MealRow(
                name = slot.name,
                detail = logged.joinToString(", ") { it.description }.takeIf { it.isNotEmpty() },
                time = slot.time.format(hhmm),
                state = state,
                photo = logged.firstNotNullOfOrNull { m -> m.recipeId?.let { photoOf[it] } },
                kcal = logged.takeIf { it.isNotEmpty() }?.sumOf { it.kcal.toDouble() }?.toFloat(),
                onClick = { onOpenSlot(slot.id) },
                onLongClick = {
                    val marking = !done[i]
                    onSetHabit(mealKey(date, slot.id), marking)
                    if (marking) { if (done.count { it } + 1 == meals.size) reactions.done() else reactions.tick() }
                },
            )
        }
        bySlot[null].orEmpty().forEach { m ->
            val time = Instant.ofEpochMilli(m.createdAt).atZone(ZoneId.systemDefault()).format(hhmm)
            MealRow(m.description, null, time, PlanState.DONE, m.recipeId?.let { photoOf[it] }, m.kcal, { onOpenSlot(null) }, null)
        }
    }

    // 4. Agua · Hábitos
    SwipePages(
        titles = listOf("Agua", "Hábitos"),
        info = listOf(
            "Toca un ${waterUnit(settings.waterContainer).lowercase()} para llenarlo hasta ahí; toca el último lleno para vaciarlo. La meta se cambia en el engrane.",
            "Lo que quieres cumplir cada día. Tócalo para marcarlo.",
        ),
    ) { page ->
        Column(Modifier.fillMaxWidth().glass().padding(16.dp)) {
            if (page == 0) WaterRow(date, settings, records.completedHabits, onSetHabit)
            else Habits(date, records.completedHabits, actions)
        }
    }
}

/** Protein and fiber colors, from the data palette. */
private val FoodProtein = Color(0xFF8AC6A3)
private val FoodFiber = Color(0xFF97CFE0)

/** A nutrient in its tinted card: grams eaten, a bar toward the goal and what is left. */
@Composable
private fun NutrientFigure(label: String, grams: Float, goal: Int, tint: Color, modifier: Modifier) {
    val left = goal - grams
    TintFigure(
        label, "${grams.whole()} g", tint, modifier,
        detail = when {
            goal <= 0 -> "Sin meta"
            left <= 0f -> "Meta cumplida"
            else -> "Faltan ${left.whole()} g"
        },
        bar = if (goal > 0) grams / goal else null,
    )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun MealRow(
    name: String,
    detail: String?,
    time: String,
    state: PlanState,
    photo: String?,
    kcal: Float?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics(mergeDescendants = true) { contentDescription = "$name $time" }
            .heightIn(min = 60.dp)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StateDot(state, 14.dp)
        Text(
            time, Modifier.width(48.dp), style = MaterialTheme.typography.labelLarge,
            color = when (state) {
                PlanState.TODAY -> TextBlue
                PlanState.LATE -> LauncherDanger
                else -> LabelCream
            },
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(name, style = MaterialTheme.typography.bodyMedium, color = if (state == PlanState.DONE) FinanceWhite else FinanceMuted, maxLines = 1)
            detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = FinanceMuted, maxLines = 1) }
        }
        if (photo != null) RecipePhoto(photo, 36.dp, dim = state != PlanState.DONE)
        Text(kcal?.whole() ?: "", style = MaterialTheme.typography.labelLarge, color = FinanceWhite)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(GlassLine))
}

/** The day's habits as icons with their name; a tap marks one. */
@Composable
private fun Habits(date: LocalDate, habits: Set<String>, actions: PersonalRecordActions) {
    val reactions = LocalReactions.current
    Row(Modifier.fillMaxWidth()) {
        DIET_HABITS.forEach { (id, label) ->
            val isDone = habitKey(date, id) in habits
            var play by remember { mutableIntStateOf(0) }
            Column(
                Modifier
                    .weight(1f)
                    .clickable(role = Role.Checkbox) {
                        actions.toggleHabit(habitKey(date, id))
                        if (!isDone) { play++; reactions.tick() }
                    }
                    .semantics { contentDescription = label }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StrokeIcon(HABIT_STROKES.getValue(id), if (isDone) StrokeState.NOW else StrokeState.OFF, 30.dp, playKey = play, color = if (isDone) MoneyPlenty else null)
                Text(HABIT_NAMES.getValue(id), style = MaterialTheme.typography.labelLarge, color = if (isDone) FinanceWhite else LabelCream, maxLines = 1)
            }
        }
    }
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
        val goalMl = goal * ml
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Llevas ${formatMl(count * ml).lowercase()}", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = if (count >= goal) MoneyPlenty else TextBlue)
            Text(if (count >= goal) "Meta cumplida" else "Faltan ${formatMl(goalMl - count * ml).lowercase()}", style = MaterialTheme.typography.bodyMedium, color = FinanceWhite)
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
                if (slotTime != null) SmallLabel(slotTime, color = if (slotDone) White else LauncherAccent)
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
