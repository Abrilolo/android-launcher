package com.adrian.minimallauncher.feature.exercise

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.BodyMetricRecord
import com.adrian.minimallauncher.data.ModuleSettings
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.feature.ModuleCard
import com.adrian.minimallauncher.feature.SettingsGear
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.feature.StatusDot
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
import kotlinx.coroutines.delay

private enum class ExerciseTab { TODAY, WEEK, BODY }

internal val NumberGray = Color(0xFFB0B0B0)
internal val EmptyGray = Color(0xFF2A2A2A)

/** Big dot-matrix tag for a session; the glyph set has no "~" or "·", so non-strength days get a word. */
fun Session.displayCode(): String = when (kind) {
    SessionKind.STRENGTH -> code
    SessionKind.CARDIO -> "CARDIO"
    SessionKind.INTERVALS -> "HIIT"
    SessionKind.REST -> "OFF"
}

/** Spanish names of the stack's muscle groups, in the order the settings list them. */
internal val MuscleGroups = linkedMapOf(
    "pecho" to "Pecho", "espalda" to "Espalda", "piernas" to "Piernas", "hombros" to "Hombros",
    "brazos" to "Brazos", "core" to "Abdomen", "cardio" to "Cardio",
)

/** Stroke icon of a muscle group; cardio is the bike. */
internal fun groupIcon(group: String): String = if (group == "cardio") "bici" else group

/** The muscle group a movement of the plan works. */
internal fun PlanExercise.group(): String = when {
    core -> "core"
    id in setOf("push_up", "floor_press") -> "pecho"
    id == "db_row" -> "espalda"
    id in setOf("shoulder_press", "lateral_raise") -> "hombros"
    id in setOf("biceps_curl", "hammer_curl", "triceps_ext") -> "brazos"
    else -> "piernas"
}

/** The icons that stand for a session: its muscle groups on strength days, else the kind of day. */
internal fun Session.icons(): List<String> = when (kind) {
    SessionKind.STRENGTH -> {
        val t = title.lowercase()
        listOf("pecho", "espalda", "piernas", "hombros", "brazos").filter { it in t }.take(2).ifEmpty { listOf("gimnasio") }
    }
    SessionKind.CARDIO -> listOf("bici")
    SessionKind.INTERVALS -> listOf("hiit")
    SessionKind.REST -> listOf("dormir")
}

/** Upper minutes of the session ("45-50 MIN" → 50); null on rest days. */
internal fun Session.minutes(): Int? = if (kind == SessionKind.REST) null else Regex("\\d+").findAll(duration).lastOrNull()?.value?.toInt()

/** "2 × 8-12 POR LADO" → "2 × 8-12": the view shows numbers only. */
internal fun PlanExercise.numbers(): String = scheme.replace(Regex("[\\sA-ZÁÉÍÓÚ.]+$"), "") + if (timed) "\"" else ""

@Composable
fun ExerciseScreen(
    day: ExerciseDay,
    settings: ModuleSettings,
    bodyMetrics: List<BodyMetricRecord>,
    stack: List<StackExercise>,
    kcalOver: Float,
    onLogSet: (PlanExercise, Float, Int) -> Unit,
    onUndoSet: (PlanExercise, Int) -> Unit,
    onSetCompleted: (Boolean) -> Unit,
    onAddBodyMetric: (String, Float) -> Unit,
    onSaveSettings: (ModuleSettings) -> Unit,
    onSaveStackExercise: (StackExercise) -> Unit,
    onDeleteStackExercise: (String) -> Unit,
    onClearPlaceholders: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableStateOf(ExerciseTab.TODAY) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var restLeft by rememberSaveable { mutableIntStateOf(0) }
    var restLength by rememberSaveable(settings.exerciseRestSeconds) { mutableIntStateOf(settings.exerciseRestSeconds) }
    val haptics = LocalHapticFeedback.current

    BackHandler { if (showSettings) showSettings = false else onBack() }

    LaunchedEffect(restLeft) {
        if (restLeft > 0) {
            delay(1_000L)
            restLeft--
            if (restLeft == 0) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    if (showSettings) {
        ExerciseSettings(settings, stack, onSaveSettings, onSaveStackExercise, onDeleteStackExercise, onClearPlaceholders, { showSettings = false }, modifier)
        return
    }

    Column(modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp).height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                TabIcon("gimnasio", "Hoy", tab == ExerciseTab.TODAY) { tab = ExerciseTab.TODAY }
                TabIcon("semana", "Semana", tab == ExerciseTab.WEEK) { tab = ExerciseTab.WEEK }
                TabIcon("bascula", "Cuerpo", tab == ExerciseTab.BODY) { tab = ExerciseTab.BODY }
            }
            SettingsGear({ showSettings = true })
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 12.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            val weightKg = bodyMetrics.filter { it.type == "WEIGHT" }.maxByOrNull { it.createdAt }?.value ?: ExerciseBurn.DEFAULT_WEIGHT_KG
            when (tab) {
                ExerciseTab.TODAY -> TodayTab(
                    day, settings, stack, kcalOver, weightKg,
                    onLogSet = { ex, kg, reps -> onLogSet(ex, kg, reps); restLeft = restLength }, onUndoSet, onSetCompleted,
                )
                ExerciseTab.WEEK -> WeekTab(day)
                ExerciseTab.BODY -> BodyTab(day.date, settings, bodyMetrics, onAddBodyMetric)
            }
        }
        if (tab == ExerciseTab.TODAY && day.session.kind == SessionKind.STRENGTH) {
            RestTimer(restLeft, restLength, onPick = { restLength = it; restLeft = 0 }, onToggle = { restLeft = if (restLeft > 0) 0 else restLength })
        }
    }
}

@Composable
private fun TabIcon(icon: String, description: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.clip(CircleShape).clickable(role = Role.Tab, onClick = onClick).semantics { contentDescription = description }.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        StrokeIcon(icon, if (selected) StrokeState.NOW else StrokeState.OFF, 24.dp, playKey = selected)
        Box(Modifier.size(4.dp).background(if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape))
    }
}

/** A stroke icon that is also a button; its line redraws once each time [playKey] changes. */
@Composable
internal fun IconButton(icon: String, state: StrokeState, size: Dp, description: String, playKey: Any? = null, onClick: () -> Unit) {
    Box(
        Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = description }.padding(4.dp),
        contentAlignment = Alignment.Center,
    ) { StrokeIcon(icon, state, size, playKey = playKey) }
}

@Composable
internal fun Num(text: String, color: Color = NumberGray, modifier: Modifier = Modifier, big: Boolean = false) {
    Text(text, modifier, style = if (big) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.labelSmall, color = color)
}

@Composable
private fun TodayTab(
    day: ExerciseDay,
    settings: ModuleSettings,
    stack: List<StackExercise>,
    kcalOver: Float,
    weightKg: Float,
    onLogSet: (PlanExercise, Float, Int) -> Unit,
    onUndoSet: (PlanExercise, Int) -> Unit,
    onSetCompleted: (Boolean) -> Unit,
) {
    val session = day.session
    val started = day.doneSets > 0 || day.completed
    // Screen readers still get the session's name.
    Row(Modifier.fillMaxWidth().semantics { contentDescription = session.title }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        session.icons().forEach { StrokeIcon(it, if (day.completed) StrokeState.DONE else StrokeState.NOW, 40.dp) }
        Spacer(Modifier.weight(1f))
        session.minutes()?.let { minutes ->
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Num(settings.exerciseStart.toString(), if (started) MaterialTheme.colorScheme.onSurface else LauncherRed)
                Num("$minutes'")
            }
        }
    }

    when (session.kind) {
        SessionKind.STRENGTH -> {
            SetsBar(day.doneSets, session.totalSets)
            val current = session.exercises.indexOfFirst { day.sets[it.id].orEmpty().size < it.sets }
            var openId by rememberSaveable(day.date) { mutableStateOf<String?>(null) }
            ModuleCard {
                WarmupRow(day)
                session.exercises.forEachIndexed { index, exercise ->
                    ExerciseRow(
                        exercise = exercise,
                        done = day.sets[exercise.id].orEmpty(),
                        last = day.lastLogs[exercise.id],
                        isCurrent = index == current,
                        isOpen = openId == exercise.id,
                        onToggleOpen = { openId = if (openId == exercise.id) null else exercise.id },
                        onLogSet = { kg, reps -> onLogSet(exercise, kg, reps) },
                        onUndoSet = { setIndex -> onUndoSet(exercise, setIndex) },
                    )
                }
            }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                IconButton("listo", if (day.completed) StrokeState.DONE else StrokeState.OFF, 36.dp, "Sesión hecha", playKey = day.completed) {
                    onSetCompleted(!day.completed)
                }
            }
        }
        SessionKind.CARDIO, SessionKind.INTERVALS -> ModuleCard {
            if (session.kind == SessionKind.INTERVALS) IntervalBars() else Box(Modifier.fillMaxWidth().height(4.dp).background(Color(0xFF5C5C5C), RoundedCornerShape(2.dp)))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Num("${session.minutes()}'", modifier = Modifier.weight(1f))
                IconButton("listo", if (day.completed) StrokeState.DONE else StrokeState.OFF, 32.dp, "Sesión hecha", playKey = day.completed) {
                    onSetCompleted(!day.completed)
                }
            }
        }
        SessionKind.REST -> Unit
    }

    ExcessCard(day, stack, kcalOver, weightKg)
}

@Composable
private fun SetsBar(done: Int, total: Int) {
    Row(Modifier.fillMaxWidth().semantics { contentDescription = "Series" }, Arrangement.spacedBy(3.dp)) {
        repeat(total) { i ->
            val color = when {
                i < done -> MaterialTheme.colorScheme.onSurface
                i == done -> LauncherRed
                else -> EmptyGray
            }
            Box(Modifier.weight(1f).height(5.dp).background(color, RoundedCornerShape(3.dp)))
        }
    }
}

@Composable
private fun WarmupRow(day: ExerciseDay) {
    var done by rememberSaveable(day.date) { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconButton("calentar", if (done) StrokeState.DONE else StrokeState.NOW, 28.dp, "Calentamiento", playKey = done) { done = !done }
        Num("5-7'")
    }
}

@Composable
private fun ExerciseRow(
    exercise: PlanExercise,
    done: List<SetLog>,
    last: SetLog?,
    isCurrent: Boolean,
    isOpen: Boolean,
    onToggleOpen: () -> Unit,
    onLogSet: (Float, Int) -> Unit,
    onUndoSet: (Int) -> Unit,
) {
    // Starts from the last load used, or bodyweight and the low end of the range the first time.
    var kg by rememberSaveable(exercise.id, last?.id) { mutableFloatStateOf(last?.weightKg ?: 0f) }
    var reps by rememberSaveable(exercise.id, last?.id) { mutableIntStateOf(last?.reps ?: exercise.minTarget) }
    var showInfo by rememberSaveable(exercise.id) { mutableStateOf(false) }
    val finished = done.size >= exercise.sets
    val load = when {
        exercise.timed -> "$reps\""
        kg > 0f -> "%.0f×%d".format(kg, reps)
        else -> reps.toString()
    }
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF161616)))
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(
                groupIcon(exercise.group()),
                when { finished -> StrokeState.DONE; isCurrent -> StrokeState.NOW; else -> StrokeState.OFF },
                28.dp, exercise.name, playKey = finished, onClick = onToggleOpen,
            )
            Num(load, if (finished) NumberGray else MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
            repeat(exercise.sets) { setIndex ->
                val filled = setIndex < done.size
                StatusDot(
                    filled = filled,
                    next = isCurrent && setIndex == done.size,
                    size = 22.dp,
                    modifier = Modifier
                        .clickable(role = Role.Checkbox) {
                            when {
                                filled -> onUndoSet(setIndex)
                                setIndex == done.size -> onLogSet(kg, reps)
                            }
                        }
                        .padding(3.dp),
                )
            }
        }
        AnimatedVisibility(isOpen) {
            Column(Modifier.padding(start = 40.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!exercise.timed) {
                        Stepper(if (kg == 0f) "—" else "%.0f".format(kg), "kg", Modifier.weight(1f), { kg = (kg - 1f).coerceAtLeast(0f) }, { kg += 1f })
                    }
                    Stepper(
                        reps.toString(), if (exercise.timed) "segundos" else "repeticiones", Modifier.weight(1f),
                        { reps = (reps - if (exercise.timed) 5 else 1).coerceAtLeast(1) },
                        { reps += if (exercise.timed) 5 else 1 },
                    )
                    IconButton("info", if (showInfo) StrokeState.NOW else StrokeState.OFF, 22.dp, "Cómo se hace") { showInfo = !showInfo }
                }
                // Names and instructions only on request (no text in the view).
                AnimatedVisibility(showInfo) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(exercise.name, style = MaterialTheme.typography.bodyMedium)
                        Text(exercise.how, style = MaterialTheme.typography.bodySmall, color = NumberGray)
                        Text(exercise.care, style = MaterialTheme.typography.bodySmall, color = LauncherRed)
                    }
                }
            }
        }
    }
}

@Composable
private fun Stepper(value: String, description: String, modifier: Modifier, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        modifier.border(1.dp, Color(0xFF333333), RoundedCornerShape(16.dp)).semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton("−", onMinus)
        Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        StepperButton("+", onPlus)
    }
}

@Composable
internal fun StepperButton(symbol: String, onClick: () -> Unit) {
    Box(Modifier.size(40.dp).clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(symbol, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * When today's food went over the kcal goal: an exercise of today's muscle group from the stack that burns the
 * excess. ⟳ moves to the next option of the same group; a tap marks it done for today.
 */
@Composable
private fun ExcessCard(day: ExerciseDay, stack: List<StackExercise>, kcalOver: Float, weightKg: Float) {
    if (kcalOver <= 0f) return
    val group = ExerciseBurn.groupOf(day.session)
    val options = stack.filter { it.muscleGroup.equals(group, ignoreCase = true) }
    if (options.isEmpty()) return
    // Those that burn the excess in their usual bout first (least work first), then the rest, strongest first.
    val ranked = options.filter { ExerciseBurn.kcal(it, weightKg) >= kcalOver }.sortedBy { ExerciseBurn.kcal(it, weightKg) } +
        options.filter { ExerciseBurn.kcal(it, weightKg) < kcalOver }.sortedByDescending { it.met }
    var index by rememberSaveable(day.date) { mutableIntStateOf(0) }
    var done by rememberSaveable(day.date) { mutableStateOf(false) }
    val pick = ranked[index % ranked.size]
    val perMinute = ExerciseBurn.perMinute(pick.met, weightKg)
    val minutes = if (ExerciseBurn.kcal(pick, weightKg) >= kcalOver) pick.minutes else kotlin.math.ceil(kcalOver / perMinute).toInt().coerceIn(pick.minutes, 90)

    ModuleCard(border = Color(0xFF3A1416)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StrokeIcon("fuego", StrokeState.NOW, 26.dp)
            Num("+${kcalOver.toInt()}", LauncherRed, Modifier.weight(1f), big = true)
            if (ranked.size > 1) {
                Text(
                    "⟳",
                    Modifier.clip(CircleShape).clickable(role = Role.Button) { index++; done = false }.semantics { contentDescription = "Otra opción" }.padding(8.dp),
                    color = NumberGray,
                )
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(groupIcon(group), if (done) StrokeState.DONE else StrokeState.NOW, 36.dp, pick.name, playKey = done) { done = !done }
            Num("$minutes'", MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
            Num("≈${(perMinute * minutes).toInt()}", if (done) MaterialTheme.colorScheme.onSurface else NumberGray)
        }
    }
}

@Composable
internal fun IntervalBars() {
    Row(Modifier.fillMaxWidth().height(34.dp), Arrangement.spacedBy(2.dp), Alignment.Bottom) {
        Box(Modifier.weight(10f).height(10.dp).background(Color(0xFF5C5C5C), RoundedCornerShape(3.dp)))
        repeat(6) {
            Box(Modifier.weight(1f).height(34.dp).background(LauncherRed, RoundedCornerShape(3.dp)))
            Box(Modifier.weight(3f).height(10.dp).background(Color(0xFF5C5C5C), RoundedCornerShape(3.dp)))
        }
        Box(Modifier.weight(10f).height(10.dp).background(Color(0xFF5C5C5C), RoundedCornerShape(3.dp)))
    }
}

/** The rest between sets: a ring that empties, red in the last 10 seconds; tap to start or stop. */
@Composable
private fun RestTimer(left: Int, length: Int, onPick: (Int) -> Unit, onToggle: () -> Unit) {
    val shown = if (left > 0) left else length
    val ringColor = when {
        left in 1..10 -> LauncherRed
        left > 0 -> MaterialTheme.colorScheme.onSurface
        else -> Color(0xFF5C5C5C)
    }
    val fraction = if (left > 0) left / length.toFloat() else 1f
    Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            Modifier.size(64.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onToggle).semantics { contentDescription = "Descanso" },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(56.dp)) {
                val stroke = 2.dp.toPx()
                drawCircle(Color(0xFF1C1C1C), style = Stroke(stroke))
                drawArc(ringColor, -90f, 360f * fraction, false, Offset(stroke / 2, stroke / 2), size.copy(size.width - stroke, size.height - stroke), style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Num("%d:%02d".format(shown / 60, shown % 60), if (left > 0) MaterialTheme.colorScheme.onSurface else NumberGray)
        }
        listOf(60, 90, 120).forEach { seconds -> Chip(seconds.toString(), seconds == length) { onPick(seconds) } }
    }
}

@Composable
internal fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        Modifier
            .border(1.dp, if (selected) MaterialTheme.colorScheme.onSurface else Color(0xFF333333), RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelSmall,
        color = if (selected) MaterialTheme.colorScheme.onSurface else NumberGray,
    )
}

@Composable
internal fun Dot(color: Color, size: Dp = 4.dp, modifier: Modifier = Modifier) {
    Box(modifier.width(size).height(size).background(color, CircleShape))
}
