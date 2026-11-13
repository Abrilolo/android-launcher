package com.adrian.minimallauncher.feature.exercise

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
import com.adrian.minimallauncher.data.ModuleSettings
import com.adrian.minimallauncher.feature.ModuleTextField
import com.adrian.minimallauncher.feature.finance.ActionColor
import com.adrian.minimallauncher.feature.finance.ActionPill
import com.adrian.minimallauncher.feature.finance.FinIcon
import com.adrian.minimallauncher.feature.finance.FinanceBackHeader
import com.adrian.minimallauncher.feature.finance.FinanceCollapsible
import com.adrian.minimallauncher.feature.finance.FinanceMuted
import com.adrian.minimallauncher.feature.finance.FinanceReadMore
import com.adrian.minimallauncher.feature.finance.GearSection
import com.adrian.minimallauncher.feature.finance.NumberStepper
import com.adrian.minimallauncher.feature.finance.OptionChips
import com.adrian.minimallauncher.feature.finance.SectionLabel
import com.adrian.minimallauncher.ui.components.LocalReactions
import java.time.LocalTime
import java.util.UUID

/** Intensity levels the stack offers, as MET values (Compendium: light, moderate, vigorous). */
private val Levels = listOf("suave" to 3.5f, "media" to 5f, "fuerte" to 7f)
private fun levelOf(met: Float): String = when { met < 4.25f -> "suave"; met < 6f -> "media"; else -> "fuerte" }

/**
 * The gear of Ejercicio, like Finanzas': only what is set once. The routines live in Semana.
 * 1. Horario: the hour training starts and the rest between series.
 * 2. Cuerpo: height and weight goal, shared with Comida.
 * 3. Stack: what to add when food went over, by muscle group, each group folded.
 */
@Composable
internal fun ExerciseSettings(
    settings: ModuleSettings,
    stack: List<StackExercise>,
    onSaveSettings: (ModuleSettings) -> Unit,
    onSave: (StackExercise) -> Unit,
    onDelete: (String) -> Unit,
    onClearPlaceholders: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onClose)
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        FinanceBackHeader("Ajustes", onClose)
        ExerciseScheduleSection(settings, onSaveSettings)
        BodyGoalsSection(settings, onSaveSettings)
        GearSection(
            "Stack",
            "Si comes más de tu meta de kcal, Hoy te sugiere uno de estos del grupo que entrenas ese día, el que lo quema con menos trabajo.",
        ) {
            if (stack.any { it.placeholder }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Trae ejemplos. Cámbialos por los tuyos.", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = FinanceMuted)
                    FinIcon("papelera", "Borrar ejemplos", ActionColor.Delete, onClick = onClearPlaceholders)
                }
            }
            var open by rememberSaveable { mutableStateOf<String?>(null) }
            MuscleGroups.entries.forEachIndexed { i, (group, name) ->
                val rows = stack.filter { it.muscleGroup.equals(group, ignoreCase = true) }
                FinanceCollapsible(
                    title = name,
                    icon = groupIcon(group),
                    summaryText = null,
                    subtitle = if (rows.isEmpty()) "Sin ejercicios" else rows.joinToString(" · ") { it.name.ifBlank { "Sin nombre" } },
                    expanded = open == group,
                    onToggle = { open = if (open == group) null else group },
                    showRule = i < MuscleGroups.size - 1,
                ) {
                    rows.forEach { StackRow(it, onSave, onDelete) }
                    ActionPill("Agregar", "nueva", primary = false) {
                        onSave(StackExercise("u_${UUID.randomUUID()}", "", group, 5f, 10, (rows.maxOfOrNull { it.sortOrder } ?: -1) + 1))
                    }
                }
            }
        }
    }
}

/** The hour training starts (Hoy counts down to it) and the rest between series. */
@Composable
internal fun ExerciseScheduleSection(settings: ModuleSettings, onSave: (ModuleSettings) -> Unit) {
    val reactions = LocalReactions.current
    var start by rememberSaveable(settings.exerciseStart) { mutableStateOf(settings.exerciseStart.format(hhmm)) }
    val parsed = runCatching { LocalTime.parse(start) }.getOrNull()
    GearSection("Horario", "La hora a la que entrenas: Hoy te dice cuánto falta. El descanso es el tiempo entre series; al terminar vibra.") {
        SectionLabel("Hora de inicio")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            ModuleTextField(start, { start = it.take(5) }, "HH:mm", Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            ActionPill("Guardar", null, enabled = parsed != null && parsed != settings.exerciseStart) {
                parsed?.let { onSave(settings.copy(exerciseStart = it)); reactions.tick() }
            }
        }
        SectionLabel("Descanso")
        OptionChips(listOf(60, 90, 120, 180).map { "$it" to if (it < 120) "$it s" else "${it / 60} min" }, "${settings.exerciseRestSeconds}") {
            onSave(settings.copy(exerciseRestSeconds = it.toInt()))
        }
        FinanceReadMore(TokyoGuide.PROGRESSION + " " + TokyoGuide.INTENSITY)
    }
}

/** Height and weight goal: the BMI and the weight bar use them, here and in Comida. */
@Composable
internal fun BodyGoalsSection(settings: ModuleSettings, onSave: (ModuleSettings) -> Unit) {
    val reactions = LocalReactions.current
    var height by rememberSaveable(settings.heightCm) { mutableStateOf(if (settings.heightCm > 0f) "%.0f".format(settings.heightCm) else "") }
    var goal by rememberSaveable(settings.weightGoalKg) { mutableStateOf(if (settings.weightGoalKg > 0f) kgText(settings.weightGoalKg) else "") }
    val h = height.replace(',', '.').toFloatOrNull()?.takeIf { it in 100f..250f }
    val g = goal.replace(',', '.').toFloatOrNull()?.takeIf { it in 30f..250f }
    val dirty = (h != null && h != settings.heightCm) || (g != null && g != settings.weightGoalKg)
    GearSection("Cuerpo", "Tu estatura y tu peso meta. Son los mismos en Ejercicio y en Comida.") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ModuleTextField(height, { height = it.take(5) }, "Estatura cm", Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            ModuleTextField(goal, { goal = it.take(5) }, "Meta kg", Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            ActionPill("Guardar", null, enabled = dirty) {
                onSave(settings.copy(heightCm = h ?: settings.heightCm, weightGoalKg = g ?: settings.weightGoalKg))
                reactions.tick()
            }
        }
    }
}

/** One movement of the stack: its name, how hard and for how long; ✕ removes it. */
@Composable
private fun StackRow(exercise: StackExercise, onSave: (StackExercise) -> Unit, onDelete: (String) -> Unit) {
    // Local text so typing is not reset while each change is saved.
    var name by remember(exercise.id) { mutableStateOf(exercise.name) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ModuleTextField(name, { name = it.take(40); onSave(exercise.copy(name = name)) }, "Nombre", Modifier.weight(1f))
            FinIcon("papelera", "Quitar ${exercise.name}", ActionColor.Delete) { onDelete(exercise.id) }
        }
        OptionChips(Levels.map { (key, _) -> key to key.replaceFirstChar { it.uppercase() } }, levelOf(exercise.met)) { key ->
            onSave(exercise.copy(met = Levels.first { it.first == key }.second))
        }
        NumberStepper(
            "Minutos", "${exercise.minutes}", Modifier.fillMaxWidth(),
            onMinus = { onSave(exercise.copy(minutes = (exercise.minutes - 5).coerceAtLeast(5))) },
            onPlus = { onSave(exercise.copy(minutes = (exercise.minutes + 5).coerceAtMost(90))) },
        )
    }
}
