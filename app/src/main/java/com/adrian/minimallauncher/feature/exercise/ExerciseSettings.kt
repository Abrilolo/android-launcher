package com.adrian.minimallauncher.feature.exercise

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.ModuleSettings
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
import java.time.LocalTime
import java.util.UUID

/** Intensity levels the stack editor offers, as MET values (Compendium: light, moderate, vigorous). */
private val Levels = listOf("SUAVE" to 3.5f, "MEDIA" to 5f, "FUERTE" to 7f)
private fun levelOf(met: Float): Int = when { met < 4.25f -> 0; met < 6f -> 1; else -> 2 }

/** Exercise settings, the one place with names: the stack by muscle group, body goals and the plan's hour and rest. */
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
    Column(modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp).height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            SmallLabel("AJUSTES · EJERCICIO", Modifier.weight(1f), color = LauncherRed)
            IconButton("listo", StrokeState.NOW, 22.dp, "Cerrar ajustes", onClick = onClose)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (stack.any { it.placeholder }) {
                Row(
                    Modifier.fillMaxWidth().border(1.dp, Color(0xFF3A3A3A), RoundedCornerShape(16.dp)).padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("El stack trae ejemplos. Llénalo con los tuyos.", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = NumberGray)
                    TextButton(onClick = onClearPlaceholders) { Text("BORRAR EJEMPLOS", style = MaterialTheme.typography.labelSmall) }
                }
            }
            SmallLabel("STACK · PARA QUEMAR EL EXCESO DE KCAL")
            MuscleGroups.forEach { (group, name) ->
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StrokeIcon(groupIcon(group), StrokeState.DONE, 22.dp)
                    SmallLabel(name.uppercase(), color = MaterialTheme.colorScheme.onSurface)
                }
                val rows = stack.filter { it.muscleGroup.equals(group, ignoreCase = true) }
                rows.forEach { StackRow(it, onSave, onDelete) }
                Text(
                    "+ ${name.uppercase()}",
                    Modifier
                        .border(1.dp, Color(0xFF333333), RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(role = Role.Button) {
                            onSave(StackExercise("u_${UUID.randomUUID()}", "", group, 5f, 10, (rows.maxOfOrNull { it.sortOrder } ?: -1) + 1))
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = NumberGray,
                )
            }

            BodyGoals(settings, onSaveSettings)
            PlanSettings(settings, onSaveSettings)
        }
    }
}

@Composable
private fun StackRow(exercise: StackExercise, onSave: (StackExercise) -> Unit, onDelete: (String) -> Unit) {
    // Local text so typing is not reset while each change is saved.
    var name by remember(exercise.id) { mutableStateOf(exercise.name) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            name, { name = it; onSave(exercise.copy(name = it)) }, Modifier.weight(1f), singleLine = true,
            placeholder = { Text("Nombre") },
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = if (exercise.placeholder) NumberGray else MaterialTheme.colorScheme.onSurface),
        )
        val level = levelOf(exercise.met)
        Chip(Levels[level].first, true) { onSave(exercise.copy(met = Levels[(level + 1) % Levels.size].second)) }
        StepperButton("−") { onSave(exercise.copy(minutes = (exercise.minutes - 5).coerceAtLeast(5))) }
        Num("${exercise.minutes}'", MaterialTheme.colorScheme.onSurface)
        StepperButton("+") { onSave(exercise.copy(minutes = (exercise.minutes + 5).coerceAtMost(90))) }
        Num("✕", modifier = Modifier.clip(CircleShape).clickable(role = Role.Button) { onDelete(exercise.id) }.padding(6.dp))
    }
}

@Composable
private fun BodyGoals(settings: ModuleSettings, onSave: (ModuleSettings) -> Unit) {
    var height by rememberSaveable(settings.heightCm) { mutableStateOf(if (settings.heightCm > 0f) "%.0f".format(settings.heightCm) else "") }
    var goal by rememberSaveable(settings.weightGoalKg) { mutableStateOf(if (settings.weightGoalKg > 0f) "%.1f".format(settings.weightGoalKg) else "") }
    SmallLabel("METAS · LAS MISMAS DE COMIDA", Modifier.padding(top = 12.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(height, { height = it.take(5) }, Modifier.weight(1f), label = { Text("Altura (cm)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        OutlinedTextField(goal, { goal = it.take(5) }, Modifier.weight(1f), label = { Text("Peso meta (kg)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
    }
    TextButton(onClick = {
        val h = height.replace(',', '.').toFloatOrNull()?.takeIf { it in 100f..250f } ?: settings.heightCm
        val g = goal.replace(',', '.').toFloatOrNull()?.takeIf { it in 30f..250f } ?: settings.weightGoalKg
        onSave(settings.copy(heightCm = h, weightGoalKg = g))
    }) { Text("GUARDAR METAS") }
}

@Composable
private fun PlanSettings(settings: ModuleSettings, onSave: (ModuleSettings) -> Unit) {
    var startText by rememberSaveable(settings.exerciseStart) { mutableStateOf(settings.exerciseStart.toString()) }
    SmallLabel("PLAN ACTIVO", Modifier.padding(top = 12.dp))
    Text("Físico de atleta · Guía Tokio, octubre 2026", style = MaterialTheme.typography.bodyLarge)
    Text("Mancuernas hasta 10 kg, caminadora, bicicleta, elíptica y tapete.", style = MaterialTheme.typography.bodySmall, color = NumberGray)
    Text(TokyoGuide.PROGRESSION, style = MaterialTheme.typography.bodySmall, color = NumberGray)
    Text(TokyoGuide.INTENSITY, style = MaterialTheme.typography.bodySmall, color = NumberGray)

    SmallLabel("HORA DE INICIO", Modifier.padding(top = 8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(startText, { startText = it.take(5) }, Modifier.weight(1f), label = { Text("HH:mm") }, singleLine = true)
        TextButton(onClick = {
            runCatching { LocalTime.parse(startText) }.getOrNull()?.let { onSave(settings.copy(exerciseStart = it)) }
        }) { Text("GUARDAR") }
    }

    SmallLabel("DESCANSO PREDETERMINADO", Modifier.padding(top = 8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(60, 90, 120).forEach { seconds ->
            Chip("$seconds S", seconds == settings.exerciseRestSeconds) { onSave(settings.copy(exerciseRestSeconds = seconds)) }
        }
    }
    Row(Modifier.width(1.dp).height(8.dp)) {}
    SmallLabel("Tus series quedan guardadas en el teléfono con peso y repeticiones.")
}
