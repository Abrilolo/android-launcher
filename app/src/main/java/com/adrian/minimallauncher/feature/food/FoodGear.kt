package com.adrian.minimallauncher.feature.food

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.MealKind
import com.adrian.minimallauncher.data.MealSlot
import com.adrian.minimallauncher.data.ModuleSettings
import com.adrian.minimallauncher.feature.ModuleTextField
import com.adrian.minimallauncher.feature.exercise.BodyGoalsSection
import com.adrian.minimallauncher.feature.finance.ActionColor
import com.adrian.minimallauncher.feature.finance.ActionPill
import com.adrian.minimallauncher.feature.finance.FinIcon
import com.adrian.minimallauncher.feature.finance.FinanceBackHeader
import com.adrian.minimallauncher.feature.finance.FinanceCollapsible
import com.adrian.minimallauncher.feature.finance.FinanceMuted
import com.adrian.minimallauncher.feature.finance.FinanceWhite
import com.adrian.minimallauncher.feature.finance.GearSection
import com.adrian.minimallauncher.feature.finance.NumberStepper
import com.adrian.minimallauncher.feature.finance.OptionChips
import com.adrian.minimallauncher.feature.finance.SectionLabel
import com.adrian.minimallauncher.ui.components.LocalReactions
import com.adrian.minimallauncher.ui.formatMl
import com.adrian.minimallauncher.ui.waterUnit
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val hhmm = DateTimeFormatter.ofPattern("HH:mm")

/** The icons a meal can take, with a short name. */
private val MealIcons = listOf("comida" to "Plato", "snack" to "Snack", "colacion" to "Colación", "cena" to "Cena")

/**
 * The gear of Comida, like Finanzas': only what is set once. Recipes live in Recetas.
 * 1. Metas del día: kcal, protein and fiber.
 * 2. Comidas: each meal folded (name, hour, kind and icon); a new one at the end.
 * 3. Agua: what you drink from, how much it holds, how many a day and between which hours.
 * 4. Cuerpo: height and weight goal, shared with Ejercicio.
 */
@Composable
fun FoodGear(settings: ModuleSettings, onUpdate: ((ModuleSettings) -> ModuleSettings) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler(onBack = onBack)
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        FinanceBackHeader("Ajustes", onBack)
        FoodGoalsSection(settings, onUpdate)
        MealsSection(settings.meals) { meals -> onUpdate { it.copy(meals = meals) } }
        WaterSection(settings, onUpdate)
        BodyGoalsSection(settings) { saved -> onUpdate { it.copy(heightCm = saved.heightCm, weightGoalKg = saved.weightGoalKg) } }
    }
}

/** The day's goals: Hoy counts what is left of each. */
@Composable
fun FoodGoalsSection(settings: ModuleSettings, onUpdate: ((ModuleSettings) -> ModuleSettings) -> Unit) {
    val reactions = LocalReactions.current
    var kcal by rememberSaveable(settings.kcalGoal) { mutableStateOf(if (settings.kcalGoal > 0) settings.kcalGoal.toString() else "") }
    var protein by rememberSaveable(settings.proteinGoalG) { mutableStateOf(if (settings.proteinGoalG > 0) settings.proteinGoalG.toString() else "") }
    var fiber by rememberSaveable(settings.fiberGoalG) { mutableStateOf(if (settings.fiberGoalG > 0) settings.fiberGoalG.toString() else "") }
    val k = kcal.toIntOrNull()?.coerceIn(0, 15_000) ?: 0
    val p = protein.toIntOrNull()?.coerceIn(0, 1_000) ?: 0
    val f = fiber.toIntOrNull()?.coerceIn(0, 500) ?: 0
    val dirty = k != settings.kcalGoal || p != settings.proteinGoalG || f != settings.fiberGoalG
    GearSection("Metas del día", "Lo que quieres comer al día. Hoy te dice cuánto te queda; vacío es sin meta.") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModuleTextField(kcal, { kcal = it.filter(Char::isDigit).take(5) }, "Kcal", Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            ModuleTextField(protein, { protein = it.filter(Char::isDigit).take(4) }, "Proteína g", Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            ModuleTextField(fiber, { fiber = it.filter(Char::isDigit).take(3) }, "Fibra g", Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            ActionPill("Guardar", null, enabled = dirty) {
                onUpdate { it.copy(kcalGoal = k, proteinGoalG = p, fiberGoalG = f) }
                reactions.tick()
            }
        }
    }
}

/** A meal being edited; the hour stays text until it is saved. */
private data class MealDraft(val id: String, val name: String, val time: String, val kind: MealKind, val icon: String)

/** Every meal of the day, folded: a tap opens its name, hour, kind and icon. Saving keeps them sorted by hour. */
@Composable
fun MealsSection(meals: List<MealSlot>, onSave: (List<MealSlot>) -> Unit) {
    val reactions = LocalReactions.current
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    GearSection("Comidas", "Las comidas de tu día con su hora. Hoy marca en azul la que sigue; si se pasa la hora sin marcarla, en rosa.") {
        meals.forEach { slot ->
            FinanceCollapsible(
                title = slot.name,
                subtitle = slot.time.format(hhmm) + " · " + if (slot.kind == MealKind.MAIN) "Fuerte" else "Snack",
                icon = mealStrokeIcon(slot.icon),
                expanded = open == slot.id,
                onToggle = { open = if (open == slot.id) null else slot.id },
            ) {
                MealEditor(
                    MealDraft(slot.id, slot.name, slot.time.format(hhmm), slot.kind, slot.icon),
                    onSave = { saved -> onSave((meals.filter { it.id != saved.id } + saved.copy(recipeId = slot.recipeId)).sortedBy { it.time }); reactions.tick(); open = null },
                    onDelete = { onSave(meals.filter { it.id != slot.id }); open = null },
                )
            }
        }
        FinanceCollapsible(
            title = "Nueva comida",
            icon = "nueva",
            expanded = open == "new",
            onToggle = { open = if (open == "new") null else "new" },
            showRule = false,
        ) {
            MealEditor(
                MealDraft("m${System.currentTimeMillis()}", "", "", MealKind.SNACK, "snack"),
                onSave = { saved -> onSave((meals + saved).sortedBy { it.time }); reactions.tick(); open = null },
                onDelete = null,
            )
        }
    }
}

@Composable
private fun MealEditor(initial: MealDraft, onSave: (MealSlot) -> Unit, onDelete: (() -> Unit)?) {
    var draft by remember(initial.id) { mutableStateOf(initial) }
    val time = runCatching { LocalTime.parse(draft.time) }.getOrNull()
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        ModuleTextField(draft.name, { draft = draft.copy(name = it.take(30)) }, "Nombre", Modifier.weight(1f))
        ModuleTextField(draft.time, { draft = draft.copy(time = it.take(5)) }, "HH:mm", Modifier.weight(0.5f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
    }
    SectionLabel("Tipo")
    OptionChips(listOf("main" to "Fuerte", "snack" to "Snack"), if (draft.kind == MealKind.MAIN) "main" else "snack") {
        draft = draft.copy(kind = if (it == "main") MealKind.MAIN else MealKind.SNACK)
    }
    SectionLabel("Ícono")
    OptionChips(MealIcons, draft.icon) { draft = draft.copy(icon = it) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (onDelete != null) FinIcon("papelera", "Quitar ${draft.name}", ActionColor.Delete, onClick = onDelete)
        Text(if (draft.time.isNotEmpty() && time == null) "Usa HH:mm" else "", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = FinanceMuted)
        ActionPill("Guardar", null, enabled = time != null && draft.name.isNotBlank() && draft != initial) {
            onSave(MealSlot(draft.id, draft.name.trim(), time!!, draft.kind, draft.icon))
        }
    }
}

/** Water: what it is drunk from, how much it holds, how many a day and the hours the widget spreads them over. */
@Composable
fun WaterSection(settings: ModuleSettings, onUpdate: ((ModuleSettings) -> ModuleSettings) -> Unit) {
    GearSection("Agua", "Cuánta agua quieres tomar al día. El widget reparte los vasos entre las horas que elijas.") {
        SectionLabel("Recipiente")
        OptionChips(listOf("vaso", "termo").map { it to waterUnit(it).lowercase().replaceFirstChar { c -> c.uppercase() } }, settings.waterContainer) { c -> onUpdate { it.copy(waterContainer = c) } }
        SectionLabel("Le caben")
        OptionChips(listOf(250, 355, 500, 750, 1000).map { "$it" to formatMl(it).lowercase() }, "${settings.waterGlassMl}") { ml -> onUpdate { it.copy(waterGlassMl = ml.toInt()) } }
        NumberStepper(
            "Al día", "${settings.waterGoal}", Modifier.fillMaxWidth(),
            onMinus = { onUpdate { it.copy(waterGoal = (it.waterGoal - 1).coerceAtLeast(1)) } },
            onPlus = { onUpdate { it.copy(waterGoal = (it.waterGoal + 1).coerceAtMost(20)) } },
        )
        Text("Son ${formatMl(settings.waterGoal * settings.waterGlassMl).lowercase()} al día", style = MaterialTheme.typography.bodyMedium, color = FinanceWhite)
        SectionLabel("Desde")
        OptionChips((6..10).map { LocalTime.of(it, 0) }.map { it.toString() to it.format(hhmm) }, settings.waterFrom.toString()) { t -> onUpdate { it.copy(waterFrom = LocalTime.parse(t)) } }
        SectionLabel("Hasta")
        OptionChips((19..23).map { LocalTime.of(it, 0) }.map { it.toString() to it.format(hhmm) }, settings.waterTo.toString()) { t -> onUpdate { it.copy(waterTo = LocalTime.parse(t)) } }
    }
}
