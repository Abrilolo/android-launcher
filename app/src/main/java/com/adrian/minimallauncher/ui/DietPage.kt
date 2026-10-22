package com.adrian.minimallauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.MealKind
import com.adrian.minimallauncher.data.ModuleSettings
import com.adrian.minimallauncher.data.PersonalRecordActions
import com.adrian.minimallauncher.data.PersonalRecords
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.feature.ModuleTextField
import com.adrian.minimallauncher.feature.SettingsGear
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.ui.components.DotActivation
import com.adrian.minimallauncher.ui.components.DotIcon
import com.adrian.minimallauncher.ui.components.DotIcons
import com.adrian.minimallauncher.ui.components.DotMatrixText
import com.adrian.minimallauncher.ui.components.LocalReactions
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val hhmm = DateTimeFormatter.ofPattern("HH:mm")

/** Cena lets its steam rise when marked, the chocolate bar gets bitten; the others light up bottom-up. */
fun mealActivation(icon: String) = when (icon) {
    "cena" -> DotActivation.Steam
    "snack" -> DotActivation.Bite
    else -> DotActivation.Sweep
}

/** Icon of each daily habit in [DIET_HABITS]. */
val HABIT_ICONS = mapOf("walk" to DotIcons.pasos, "no_extras" to DotIcons.sinAzucar, "protein" to DotIcons.huevo, "sleep" to DotIcons.cama)

/** Meals per row on the Dieta page; more meals wrap to another row. */
private const val MEALS_PER_ROW = 4

/**
 * Dieta, following docs/design/concept/DietaVisual.html: no header (back is the phone's gesture, the gear opens its
 * settings), the day's count in dots, the meals as icons with their time (the next one's in red; main meals bigger than
 * snacks), water glasses, habits, and "what you ate" behind the + button. Everything not marked is gray; marking plays
 * one short animation and then stays still.
 */
@Composable
fun DietPage(
    now: LocalDateTime,
    records: PersonalRecords,
    actions: PersonalRecordActions,
    settings: ModuleSettings,
    onSetHabit: (String, Boolean) -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    val reactions = LocalReactions.current
    val date = now.toLocalDate()
    val meals = settings.meals
    val done = meals.map { mealKey(date, it.id) in records.completedHabits }
    val next = done.indexOf(false)
    val count = done.count { it }
    val todayMeals = records.meals.filter { it.createdAt.toLocalDate() == date }

    Column(modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 24.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    SmallLabel("COMIDAS", color = LauncherRed)
                    SettingsGear(onSettings)
                }
                DotMatrixText("$count/${meals.size}", cell = 7.dp, accents = mapOf('/' to LauncherRed), reveal = count)
            }
            if (meals.isEmpty()) SmallLabel("SIN COMIDAS · AGRÉGALAS CON EL ENGRANE")
            meals.indices.chunked(MEALS_PER_ROW).forEach { row ->
                // Bottom-aligned, so big and small icons share the line their times sit on.
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    row.forEach { i ->
                        val meal = meals[i]
                        val time = meal.time.format(hhmm)
                        Column(
                            Modifier
                                .weight(1f)
                                .clickable(role = Role.Checkbox) {
                                    val marking = !done[i]
                                    actions.toggleHabit(mealKey(date, meal.id))
                                    if (marking) { if (count + 1 == meals.size) reactions.done() else reactions.tick() }
                                }
                                .semantics { contentDescription = "${meal.name} $time" }
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            DotIcon(
                                DotIcons.meal(meal.icon), done[i], null,
                                size = if (meal.kind == MealKind.MAIN) 60.dp else 44.dp,
                                activation = mealActivation(meal.icon),
                            )
                            SmallLabel(
                                time,
                                color = when {
                                    done[i] -> MaterialTheme.colorScheme.onSurface
                                    i == next -> LauncherRed
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                    repeat(MEALS_PER_ROW - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            if (meals.isNotEmpty()) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (next >= 0) {
                        Text(meals[next].name, style = MaterialTheme.typography.bodyLarge)
                        SmallLabel(meals[next].time.format(hhmm), color = LauncherRed)
                    } else {
                        SmallLabel("DÍA COMPLETO", color = LauncherRed)
                    }
                }
            }
            WaterCard(date, settings, records.completedHabits, onSetHabit)
            SmallLabel("HÁBITOS", color = LauncherRed)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DIET_HABITS.forEach { (id, label) ->
                    val isDone = habitKey(date, id) in records.completedHabits
                    Column(
                        Modifier
                            .weight(1f)
                            .clickable(role = Role.Checkbox) {
                                actions.toggleHabit(habitKey(date, id))
                                if (!isDone) reactions.tick()
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DotIcon(HABIT_ICONS.getValue(id), isDone, null, size = 44.dp)
                        Text(
                            label,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDone) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            if (todayMeals.isNotEmpty()) {
                Column {
                    todayMeals.forEach { meal ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(meal.description, style = MaterialTheme.typography.bodyLarge)
                                SmallLabel(Instant.ofEpochMilli(meal.createdAt).atZone(ZoneId.systemDefault()).format(hhmm))
                            }
                            Text(
                                "×",
                                Modifier
                                    .clickable(role = Role.Button) { actions.deleteMeal(meal.id) }
                                    .semantics { contentDescription = "Borrar ${meal.description}" }
                                    .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF1A1A1A)))
                    }
                }
            }
        }
        MealEntry(todayMeals.size) { text ->
            actions.addMeal(text)
            reactions.tick()
        }
    }
}

/** Glasses per row in the water section; a bigger goal wraps to another row. */
private const val GLASSES_PER_ROW = 10

/**
 * One glass (or thermos) per goal step; tapping fills up to it (each new one plays the water animation), tapping the
 * last full one empties it. Underneath, how much each holds and how much is drunk out of the day's total.
 */
@Composable
private fun WaterCard(date: LocalDate, settings: ModuleSettings, habits: Set<String>, onSetHabit: (String, Boolean) -> Unit) {
    val reactions = LocalReactions.current
    val goal = settings.waterGoal
    val ml = settings.waterGlassMl
    val unit = waterUnit(settings.waterContainer)
    val count = (1..goal).count { waterKey(date, it) in habits }
    // Count before the last tap, so the newly filled glasses fill one after another.
    var tapFrom by remember { mutableIntStateOf(count) }
    Column(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF222222)))
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                SmallLabel("AGUA", color = LauncherRed)
                DotMatrixText("$count/$goal", cell = 3.5.dp, accents = mapOf('/' to LauncherRed), reveal = count)
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val gap = 4.dp
                val perRow = minOf(goal, GLASSES_PER_ROW)
                val glass = minOf(36.dp, (maxWidth - gap * (perRow - 1)) / perRow)
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    (1..goal).chunked(perRow).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                            row.forEach { g ->
                                DotIcon(
                                    waterIcon(settings.waterContainer),
                                    done = g <= count,
                                    contentDescription = "${unit.lowercase().replaceFirstChar { it.uppercase() }} $g de $goal",
                                    modifier = Modifier.clickable(role = Role.Button) {
                                        tapFrom = count
                                        val target = if (g == count) g - 1 else g
                                        for (n in 1..goal) onSetHabit(waterKey(date, n), n <= target)
                                        if (target > count) reactions.tick()
                                    },
                                    size = glass,
                                    activation = DotActivation.Water,
                                    startDelayMs = (g - 1 - tapFrom).coerceAtLeast(0) * 90,
                                )
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                SmallLabel("$unit DE ${formatMl(ml)}")
                SmallLabel("${formatMl(count * ml)} / ${formatMl(goal * ml)}", color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF222222)))
    }
}

/** Only the count and the white + until tapped; then a field for what you ate. Saving folds it away again; back closes it. */
@Composable
private fun MealEntry(loggedToday: Int, onAdd: (String) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    var text by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val close = { open = false; text = "" }
    val save = {
        if (text.isNotBlank()) { onAdd(text.trim()); close() }
    }
    BackHandler(enabled = open, onBack = close)
    LaunchedEffect(open) { if (open) focus.requestFocus() }
    Row(
        Modifier.fillMaxWidth().padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (open) {
            ModuleTextField(
                text, { text = it.take(60) }, "¿Qué comiste?",
                Modifier.weight(1f).focusRequester(focus),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { save() }),
            )
        } else {
            SmallLabel(if (loggedToday == 1) "1 ANOTADA" else "$loggedToday ANOTADAS", Modifier.weight(1f))
        }
        Box(
            Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.onSurface, CircleShape)
                .clickable(role = Role.Button) { if (open) save() else open = true }
                .semantics { contentDescription = if (open) "Guardar comida" else "Anotar qué comiste" },
            contentAlignment = Alignment.Center,
        ) {
            Text("+", style = MaterialTheme.typography.headlineSmall, color = Color.Black)
        }
    }
}

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
