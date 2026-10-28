package com.adrian.minimallauncher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.routine.ActionState
import com.adrian.minimallauncher.data.routine.PhaseReminder
import com.adrian.minimallauncher.data.routine.RoutinePhase
import com.adrian.minimallauncher.data.routine.Trigger
import com.adrian.minimallauncher.data.routine.WidgetPick
import com.adrian.minimallauncher.data.routine.WidgetSelector
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.feature.finance.formatPesos
import com.adrian.minimallauncher.ui.Tile
import com.adrian.minimallauncher.ui.phaseIcon
import java.time.format.DateTimeFormatter

/** What the Rutinas widget reads; the launcher fills it from the ViewModel's routine flows. */
data class RoutineWidgetData(
    val phases: List<RoutinePhase>,
    val currentIndex: Int,
    /** Most urgent first ([WidgetSelector.pick]). */
    val picks: List<WidgetPick>,
    /** The current phase's timed actions and linked reminders, by hour. */
    val reminders: List<PhaseReminder>,
    /** The current phase's actions with today's marks, for an ordered phase's chain. */
    val checks: List<ActionState>,
    val ordered: Boolean,
    val onMarkAction: (String) -> Unit,
    val onToggleReminder: (String) -> Unit,
)

private val White = Color(0xFFF5F5F5)
private val Muted = Color(0xFFB0B0B0)
private val hhmm = DateTimeFormatter.ofPattern("HH:mm")

/** The urgent item as the widget draws it: an icon, an optional hour or amount, and a ring for "coming soon". */
private class Shown(val key: String, val icon: String, val number: String?, val red: Boolean, val ring: Float?, val description: String)

private fun shown(pick: WidgetPick?, data: RoutineWidgetData): Shown {
    val soon = WidgetSelector.SOON.toMinutes().toFloat()
    return when (pick) {
        is WidgetPick.PendingPayment -> Shown("pay", "pago", formatPesos(pick.amountCents), true, null, "Pago sin registrar")
        is WidgetPick.MealSoon -> Shown("meal${pick.meal.id}", "plato", pick.meal.time.format(hhmm), true, (1f - pick.minutesLeft / soon).coerceIn(0f, 1f), "Comida")
        is WidgetPick.Training -> Shown("train", "gimnasio", pick.at.format(hhmm), true, (1f - pick.minutesLeft / soon).coerceIn(0f, 1f), "Entrenar")
        is WidgetPick.PhaseStep -> Shown("step${pick.next.id}", pick.next.icon, pick.next.at.format(hhmm), false, null, pick.next.title ?: "Recordatorio")
        is WidgetPick.ChecklistStep -> Shown("check${pick.step.action.id}", pick.step.action.kind.icon, null, false, null, pick.step.action.kind.label)
        else -> {
            val phase = data.phases.getOrNull(data.currentIndex)
            Shown("phase${phase?.id}", phase?.let(::phaseIcon) ?: "nueva", (phase?.trigger as? Trigger.Between)?.start?.format(hhmm), false, null, phase?.name ?: "Rutinas")
        }
    }
}

/**
 * Rutinas widget (concept: docs/design/concept/RutinasWidget.html). It changes by itself with the hour: the most
 * urgent thing first (payment, meal or training coming, the phase's next reminder). 2×2 shows only that; 4×2 adds
 * the current phase's reminders by hour (or its chain when ordered); 4×1 shows the day's phases. No text.
 */
@Composable
fun RoutineTile(data: RoutineWidgetData?, modifier: Modifier, onOpenModule: (String) -> Unit, onLongClick: () -> Unit) {
    Tile(modifier, { onOpenModule("routine") }, onLongClick) {
        if (data == null || data.phases.isEmpty()) return@Tile
        val pick = data.picks.firstOrNull { it !is WidgetPick.PendingNotes }
        val item = shown(pick, data)
        val onPick: () -> Unit = {
            when (pick) {
                is WidgetPick.MealSoon -> onOpenModule("diet")
                is WidgetPick.Training -> onOpenModule("exercise")
                is WidgetPick.PhaseStep ->
                    if (pick.next.source == PhaseReminder.Source.REMINDER) data.onToggleReminder(pick.next.id) else data.onMarkAction(pick.next.id)
                is WidgetPick.ChecklistStep -> data.onMarkAction(pick.step.action.id)
                else -> onOpenModule("routine")
            }
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val urgentWidth = maxWidth * 0.36f
            when {
                maxHeight < 96.dp -> Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    PhaseRow(data)
                    Spacer(Modifier.weight(1f))
                    Row(
                        Modifier.pickClick(onPick, onLongClick, item.description),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        StrokeIcon(item.icon, StrokeState.NOW, 26.dp, playKey = item.key)
                        item.number?.let { Number(it, item.red) }
                    }
                }
                maxWidth > maxHeight * 1.6f -> Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(urgentWidth), contentAlignment = Alignment.Center) { Urgent(item, 44.dp, onPick, onLongClick) }
                    if (data.ordered) Chain(data) else Reminders(data.reminders)
                }
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Urgent(item, 56.dp, onPick, onLongClick) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.pickClick(onClick: () -> Unit, onLongClick: () -> Unit, description: String) =
    combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick).semantics { contentDescription = description }

@Composable
private fun Urgent(item: Shown, iconSize: Dp, onClick: () -> Unit, onLongClick: () -> Unit) {
    Column(Modifier.pickClick(onClick, onLongClick, item.description), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(iconSize + 28.dp), contentAlignment = Alignment.Center) {
            item.ring?.let { Ring(it) }
            StrokeIcon(item.icon, StrokeState.NOW, iconSize, playKey = item.key)
        }
        item.number?.let { Number(it, item.red) }
    }
}

/** Closes while the meal or the training comes closer; red in the last quarter. */
@Composable
private fun Ring(fraction: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val w = 2.dp.toPx()
        val inset = w / 2 + 1.dp.toPx()
        val arcSize = Size(size.width - 2 * inset, size.height - 2 * inset)
        val topLeft = Offset(inset, inset)
        drawArc(Color(0xFF1C1C1C), 0f, 360f, false, topLeft, arcSize, style = Stroke(w))
        drawArc(if (fraction > 0.75f) LauncherRed else White, -90f, 360f * fraction, false, topLeft, arcSize, style = Stroke(w, cap = StrokeCap.Round))
    }
}

@Composable
private fun Number(text: String, red: Boolean) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = if (red) LauncherRed else White)
}

/** Up to four of the phase's reminders by hour, from the last one done; the next one's hour is red. */
@Composable
private fun Reminders(list: List<PhaseReminder>) {
    if (list.isEmpty()) return
    val firstOpen = list.indexOfFirst { !it.done }.let { if (it < 0) list.size else it }
    val from = (firstOpen - 1).coerceIn(0, maxOf(0, list.size - 4))
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        list.drop(from).take(4).forEachIndexed { i, r ->
            val isNext = from + i == firstOpen
            Column(
                Modifier.semantics { contentDescription = r.title ?: r.icon },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StrokeIcon(r.icon, if (r.done) StrokeState.NOW else if (isNext) StrokeState.DONE else StrokeState.OFF, 26.dp, playKey = r.done)
                Text(
                    r.at.format(hhmm), style = MaterialTheme.typography.labelSmall,
                    color = when {
                        r.done -> White
                        isNext -> LauncherRed
                        else -> Muted
                    },
                )
            }
        }
    }
}

/** An ordered phase (casa): its steps in a row; a red dot over the next one. */
@Composable
private fun Chain(data: RoutineWidgetData) {
    val steps = data.checks.filter { it.on && it.markable }
    val next = steps.firstOrNull { !it.done }
    Row(verticalAlignment = Alignment.CenterVertically) {
        steps.forEachIndexed { i, s ->
            if (i > 0) Box(Modifier.width(8.dp).height(1.5.dp).background(if (steps[i - 1].done && s.done) White else Color(0xFF333333)))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(5.dp).background(if (s == next) LauncherRed else Color.Transparent, CircleShape))
                Spacer(Modifier.height(4.dp))
                StrokeIcon(s.action.kind.icon, if (s.done) StrokeState.NOW else StrokeState.OFF, 24.dp, playKey = s.done)
            }
        }
    }
}

/** The day's phases: done white, the current one with its red, the rest gray. */
@Composable
private fun PhaseRow(data: RoutineWidgetData) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        data.phases.forEachIndexed { i, phase ->
            val state = when {
                i < data.currentIndex -> StrokeState.DONE
                i == data.currentIndex -> StrokeState.NOW
                else -> StrokeState.OFF
            }
            StrokeIcon(phaseIcon(phase), state, 24.dp)
        }
    }
}
