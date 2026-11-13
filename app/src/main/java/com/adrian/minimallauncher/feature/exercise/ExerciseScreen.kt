package com.adrian.minimallauncher.feature.exercise

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.BodyMetricRecord
import com.adrian.minimallauncher.data.ModuleSettings
import com.adrian.minimallauncher.feature.LauncherAccent
import com.adrian.minimallauncher.feature.DockItem
import com.adrian.minimallauncher.feature.ModuleTitle
import com.adrian.minimallauncher.feature.finance.FinanceDock
import androidx.compose.runtime.key
import java.time.LocalDate
import com.adrian.minimallauncher.feature.ModuleCard
import com.adrian.minimallauncher.feature.SettingsGear
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.feature.StatusDot
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
import kotlinx.coroutines.delay

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
    muscle != null -> muscle
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

/**
 * Ejercicio, with the look and the pieces of Finanzas. Three views behind the dock: Hoy (the day's routine, its hour
 * and what it works; the week to tap; consistency as the goal), Plan (this week and the next ones) and Cuerpo (weight
 * and measurements, only as a record). A day's routine, the session and the muscle map in big open over them.
 */
@Composable
fun ExerciseScreen(
    day: ExerciseDay,
    settings: ModuleSettings,
    bodyMetrics: List<BodyMetricRecord>,
    stack: List<StackExercise>,
    onAddBodyMetric: (String, Float) -> Unit,
    onSaveSettings: (ModuleSettings) -> Unit,
    onSaveStackExercise: (StackExercise) -> Unit,
    onDeleteStackExercise: (String) -> Unit,
    onClearPlaceholders: () -> Unit,
    onLogSet: (PlanExercise, Float, Int) -> Unit,
    onUndoSet: (PlanExercise, Int) -> Unit,
    onFinish: (Boolean) -> Unit,
    onSaveRoutine: (LocalDate, RoutineDraft, Boolean) -> Unit,
    onCopyPreviousWeek: (LocalDate) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** Where a widget sent Ejercicio: "start" opens today's session, "day:<date>" that day's routine. */
    route: String? = null,
    onRouteShown: () -> Unit = {},
) {
    var tab by rememberSaveable { mutableStateOf("hoy") }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    // The day whose routine is open, whether today's session runs, and the muscle map in big (over either).
    var routineDay by rememberSaveable { mutableStateOf<String?>(null) }
    var training by rememberSaveable { mutableStateOf(false) }
    var bigMap by remember { mutableStateOf<Triple<String, List<MapMove>, Int>?>(null) }
    val lastKg = { id: String -> day.lastLogs[id]?.weightKg }
    androidx.compose.runtime.LaunchedEffect(route) {
        when {
            route == "start" -> { routineDay = null; settingsOpen = false; training = !day.session.isRest() }
            route?.startsWith("day:") == true -> { training = false; settingsOpen = false; routineDay = route.removePrefix("day:") }
        }
        if (route != null) onRouteShown()
    }

    bigMap?.let { (title, moves, start) ->
        MuscleMapScreen(title, moves, start, { bigMap = null }, modifier)
        return
    }
    when {
        training -> {
            ExerciseSession(
                day, settings, lastKg, onLogSet, onUndoSet, onFinish,
                onOpenMap = { i -> bigMap = Triple("Músculos", day.session.exercises.map { MapMove(it.name, it.ask(lastKg(it.id) ?: it.startKg), it.muscles()) }, i) },
                onBack = { training = false },
                modifier = modifier,
            )
            return
        }
        settingsOpen -> {
            ExerciseSettings(settings, stack, onSaveSettings, onSaveStackExercise, onDeleteStackExercise, onClearPlaceholders, { settingsOpen = false }, modifier)
            return
        }
        routineDay != null -> {
            val date = LocalDate.parse(routineDay)
            val session = if (date == day.date) day.session else RoutinePlans.sessionFor(date, day.plans)
            RoutineView(
                date = date,
                today = day.date,
                session = session,
                lastKg = lastKg,
                started = date == day.date && day.sets.isNotEmpty(),
                done = date == day.date && day.completed,
                savedOnlyForDay = RoutinePlans.todayId(date) in day.plans,
                onSave = { draft, only -> onSaveRoutine(date, draft, only) },
                onStart = { routineDay = null; training = true },
                onOpenMap = { items -> bigMap = Triple("Músculos", items.map { MapMove(it.name, it.ask(), it.muscles()) }, -1) },
                onBack = { routineDay = null },
                modifier = modifier,
            )
            return
        }
    }

    BackHandler(onBack = onBack)
    Column(modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).padding(horizontal = 24.dp)) {
            ModuleTitle(
                when (tab) {
                    "plan" -> "Plan"
                    "cuerpo" -> "Cuerpo"
                    else -> "Ejercicio"
                },
                { settingsOpen = true },
            )
            key(tab) {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    when (tab) {
                        "plan" -> ExercisePlan(day, { routineDay = it.toString() }, onCopyPreviousWeek)
                        "cuerpo" -> BodyTab(day.date, settings, bodyMetrics, onAddBodyMetric)
                        else -> ExerciseHoy(
                            day = day,
                            settings = settings,
                            onStart = { training = true },
                            onOpenDay = { routineDay = it.toString() },
                        )
                    }
                }
            }
        }
        FinanceDock(
            listOf(
                DockItem("hoy", "Hoy", "gimnasio"),
                DockItem("plan", "Plan", "semana"),
                DockItem("cuerpo", "Cuerpo", "bascula"),
            ),
            tab,
            onOpen = { tab = it },
        )
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
internal fun StepperButton(symbol: String, onClick: () -> Unit) {
    Box(Modifier.size(40.dp).clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(symbol, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
internal fun IntervalBars() {
    Row(Modifier.fillMaxWidth().height(34.dp), Arrangement.spacedBy(2.dp), Alignment.Bottom) {
        Box(Modifier.weight(10f).height(10.dp).background(Color(0xFF5C5C5C), RoundedCornerShape(3.dp)))
        repeat(6) {
            Box(Modifier.weight(1f).height(34.dp).background(LauncherAccent, RoundedCornerShape(3.dp)))
            Box(Modifier.weight(3f).height(10.dp).background(Color(0xFF5C5C5C), RoundedCornerShape(3.dp)))
        }
        Box(Modifier.weight(10f).height(10.dp).background(Color(0xFF5C5C5C), RoundedCornerShape(3.dp)))
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
