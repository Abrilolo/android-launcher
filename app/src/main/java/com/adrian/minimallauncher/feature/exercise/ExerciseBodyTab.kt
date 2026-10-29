package com.adrian.minimallauncher.feature.exercise

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.BodyMetricRecord
import com.adrian.minimallauncher.data.ModuleSettings
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.feature.ModuleCard
import com.adrian.minimallauncher.ui.components.DotMatrixText
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The week as session icons: done white, today with its red detail, the rest gray. Tap a day to see its plan. */
@Composable
internal fun WeekTab(day: ExerciseDay) {
    val monday = day.date.minusDays(day.date.dayOfWeek.value - 1L)
    var picked by rememberSaveable(day.date) { mutableIntStateOf(day.date.dayOfWeek.value - 1) }

    Row(Modifier.fillMaxWidth()) {
        TokyoGuide.week.forEachIndexed { index, session ->
            val date = monday.plusDays(index.toLong())
            val done = date in day.weekDone
            val isToday = date == day.date
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Tab) { picked = index }
                    .semantics { contentDescription = session.title }.padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val icon = if (session.kind == SessionKind.STRENGTH) "gimnasio" else session.icons().first()
                StrokeIcon(icon, when { done -> StrokeState.DONE; isToday -> StrokeState.NOW; else -> StrokeState.OFF }, 30.dp)
                Dot(
                    when {
                        index == picked -> MaterialTheme.colorScheme.onSurface
                        isToday -> LauncherRed
                        else -> Color.Transparent
                    },
                )
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.semantics { contentDescription = "Días entrenados esta semana" }) {
        StrokeIcon("fuego", if (day.weekDone.isEmpty()) StrokeState.OFF else StrokeState.NOW, 20.dp)
        Num(day.weekDone.size.toString(), MaterialTheme.colorScheme.onSurface)
    }

    val session = TokyoGuide.week[picked]
    ModuleCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            session.icons().forEach { StrokeIcon(it, StrokeState.DONE, 30.dp) }
            Spacer(Modifier.weight(1f))
            session.minutes()?.let { Num("$it'") }
        }
        session.exercises.forEach { ex ->
            Row(
                Modifier.fillMaxWidth().semantics { contentDescription = ex.name },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StrokeIcon(groupIcon(ex.group()), StrokeState.OFF, 22.dp)
                Num(ex.numbers(), modifier = Modifier.weight(1f))
                day.lastLogs[ex.id]?.takeIf { it.weightKg > 0f }?.let { Num("%.0f kg".format(it.weightKg)) }
            }
        }
        if (session.kind == SessionKind.INTERVALS) IntervalBars()
    }
}

/** The measurements the body tab follows: type stored in body metrics, icon, and what screen readers say. */
private val Measurements = listOf(
    Triple("WAIST", "cinta", "Cintura"),
    Triple("CHEST", "pecho", "Pecho"),
    Triple("ARM", "brazos", "Brazo"),
    Triple("THIGH", "piernas", "Muslo"),
)

/** Weight against its goal, BMI on a scale, and body measurements; a tap on any opens a number entry. */
@Composable
internal fun BodyTab(today: LocalDate, settings: ModuleSettings, metrics: List<BodyMetricRecord>, onAdd: (String, Float) -> Unit) {
    var entering by rememberSaveable { mutableStateOf<String?>(null) }
    val byType = metrics.groupBy { it.type }.mapValues { (_, list) -> list.sortedBy { it.createdAt }.map { it.value } }
    val weights = byType["WEIGHT"].orEmpty()
    val weight = weights.lastOrNull()
    val goal = settings.weightGoalKg.takeIf { it > 0f }
    val heightM = settings.heightCm.takeIf { it > 0f }?.div(100f)
    val weekStart = today.minusDays(today.dayOfWeek.value - 1L)
    val mornings = metrics.count { it.type == "WEIGHT" && !it.createdAt.toLocalDate().isBefore(weekStart) }

    // Weight: a line from the first weight to the goal, the red mark is now; three dots for this week's mornings.
    ModuleCard(Modifier.clickable(role = Role.Button) { entering = "WEIGHT" }.semantics { contentDescription = "Peso" }) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StrokeIcon("bascula", StrokeState.NOW, 32.dp)
            DotMatrixText(weight?.let { "%.1f".format(it) } ?: "--.-", cell = 4.dp)
            Spacer(Modifier.weight(1f))
            Trend(weights)
        }
        val start = weights.firstOrNull()
        val progress = if (weight != null && start != null && goal != null && start != goal) ((start - weight) / (start - goal)).coerceIn(0f, 1f) else 0f
        Scale(listOf(Triple(0f, progress, MaterialTheme.colorScheme.onSurface)), listOf(progress to LauncherRed))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Num(start?.let { "%.1f".format(it) } ?: "", modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                repeat(3) { i -> Dot(if (i < mornings) MaterialTheme.colorScheme.onSurface else if (i == mornings) LauncherRed else EmptyGray, 8.dp) }
            }
            if (goal != null) {
                StrokeIcon("meta", StrokeState.DONE, 16.dp)
                Num(" %.1f".format(goal), MaterialTheme.colorScheme.onSurface)
            }
        }
    }

    // BMI: white is the healthy range, the red mark is now and the white tick the BMI of the goal weight.
    val bmi = if (weight != null && heightM != null) weight / (heightM * heightM) else null
    val goalBmi = if (goal != null && heightM != null) goal / (heightM * heightM) else null
    fun x(v: Float) = ((v.coerceIn(15f, 35f) - 15f) / 20f)
    ModuleCard(Modifier.semantics { contentDescription = "IMC" }) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            DotMatrixText(bmi?.let { "%.1f".format(it) } ?: "--.-", cell = 3.5.dp, modifier = Modifier.weight(1f))
            if (goalBmi != null) {
                StrokeIcon("meta", StrokeState.DONE, 16.dp)
                Num(" %.1f".format(goalBmi), MaterialTheme.colorScheme.onSurface)
            }
        }
        Scale(
            listOf(Triple(x(18.5f), x(25f), MaterialTheme.colorScheme.onSurface)),
            listOfNotNull(goalBmi?.let { x(it) to MaterialTheme.colorScheme.onSurface }, bmi?.let { x(it) to LauncherRed }),
        )
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Num("18.5"); Num("25"); Num("30") }
    }

    // Measurements with the icon of their zone; the waist goal is half the height.
    Measurements.chunked(2).forEach { pair ->
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
            pair.forEach { (type, icon, name) ->
                val values = byType[type].orEmpty()
                ModuleCard(Modifier.weight(1f).clickable(role = Role.Button) { entering = type }.semantics { contentDescription = name }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StrokeIcon(icon, StrokeState.DONE, 26.dp)
                        Column(Modifier.weight(1f)) {
                            Num(values.lastOrNull()?.let { "%.0f".format(it) } ?: "--", MaterialTheme.colorScheme.onSurface)
                            if (type == "WAIST" && settings.heightCm > 0f) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StrokeIcon("meta", StrokeState.OFF, 12.dp)
                                    Num(" %.0f".format(settings.heightCm / 2f))
                                }
                            }
                        }
                        Trend(values)
                    }
                }
            }
        }
    }

    AnimatedVisibility(entering != null) {
        val type = entering ?: "WEIGHT"
        MetricEntry(
            icon = if (type == "WEIGHT") "bascula" else Measurements.first { it.first == type }.second,
            valid = if (type == "WEIGHT") 30f..250f else 10f..200f,
            onSave = { onAdd(type, it); entering = null },
            onCancel = { entering = null },
        )
    }
}

@Composable
private fun MetricEntry(icon: String, valid: ClosedFloatingPointRange<Float>, onSave: (Float) -> Unit, onCancel: () -> Unit) {
    var text by rememberSaveable(icon) { mutableStateOf("") }
    val value = text.replace(',', '.').toFloatOrNull()?.takeIf { it in valid }
    ModuleCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StrokeIcon(icon, StrokeState.NOW, 26.dp)
            OutlinedTextField(
                text, { text = it.take(5) }, Modifier.weight(1f), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { value?.let(onSave) }),
            )
            IconButton("listo", if (value != null) StrokeState.DONE else StrokeState.OFF, 26.dp, "Guardar") { value?.let(onSave) }
            Num("✕", modifier = Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onCancel).padding(8.dp))
        }
    }
}

/** ↑ in red when it went up, ↓ in white when it went down, · when unchanged. */
@Composable
private fun Trend(values: List<Float>) {
    if (values.size < 2) return
    val (previous, current) = values.takeLast(2)
    when {
        current > previous -> Num("↑", LauncherRed)
        current < previous -> Num("↓", MaterialTheme.colorScheme.onSurface)
        else -> Num("·")
    }
}

/** A gray track with [segments] (from, to, color) as fractions of the width and [marks] (at, color) as ticks. */
@Composable
private fun Scale(segments: List<Triple<Float, Float, Color>>, marks: List<Pair<Float, Color>>, height: Dp = 18.dp) {
    BoxWithConstraints(Modifier.fillMaxWidth().height(height)) {
        val w = maxWidth
        Box(Modifier.align(Alignment.CenterStart).fillMaxWidth().height(4.dp).background(EmptyGray, RoundedCornerShape(2.dp)))
        segments.forEach { (from, to, color) ->
            Box(Modifier.align(Alignment.CenterStart).offset(x = w * from).width(w * (to - from).coerceAtLeast(0f)).height(4.dp).background(color, RoundedCornerShape(2.dp)))
        }
        marks.forEach { (at, color) ->
            Box(Modifier.align(Alignment.CenterStart).offset(x = (w * at - 1.dp).coerceAtLeast(0.dp)).width(2.dp).height(height).background(color, RoundedCornerShape(1.dp)))
        }
    }
}

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
