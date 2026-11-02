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
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.feature.ModuleCard
import com.adrian.minimallauncher.feature.SettingsGear
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.feature.StatusDot
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
import kotlinx.coroutines.delay

private enum class ExerciseTab { TODAY, BODY }

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
 * Hoy, Semana, Tu rutina, Terminaste and the guided session run in one scene with the person. Cuerpo stays native;
 * [phaseId] is the routine phase the person acts out.
 */
@Composable
fun ExerciseScreen(
    day: ExerciseDay,
    settings: ModuleSettings,
    bodyMetrics: List<BodyMetricRecord>,
    stack: List<StackExercise>,
    kcalOver: Float,
    phaseId: String,
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
    val covered = showSettings || tab != ExerciseTab.TODAY
    // What was last on top, so it can fade out with its content after the tab is already back on Hoy.
    var cover by remember { mutableStateOf(ExerciseTab.BODY to false) }
    if (covered) cover = tab to showSettings

    Box(modifier.fillMaxSize()) {
        val weightKg = bodyMetrics.filter { it.type == "WEIGHT" }.maxByOrNull { it.createdAt }?.value ?: ExerciseBurn.DEFAULT_WEIGHT_KG
        // The scene stays alive under the native screens, so coming back is instant and nothing restarts.
        GuidedExerciseScreen(
            day = day,
            settings = settings,
            phaseId = phaseId,
            excess = excessOptions(day.session, stack, kcalOver, weightKg),
            onExit = onBack,
            onOpenNative = { target ->
                if (target == "goals") tab = ExerciseTab.BODY else showSettings = true
            },
            onStartTime = { onSaveSettings(settings.copy(exerciseStart = it)) },
            onRestSeconds = { onSaveSettings(settings.copy(exerciseRestSeconds = it)) },
            covered = covered,
        )
        BackHandler(enabled = covered) { if (showSettings) showSettings = false else tab = ExerciseTab.TODAY }
        AnimatedVisibility(covered, enter = fadeIn(tween(140)), exit = fadeOut(tween(120))) {
            val background = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).pointerInput(Unit) { detectTapGestures { } }
            val (shownTab, settingsOpen) = cover
            if (settingsOpen) ExerciseSettings(settings, stack, onSaveSettings, onSaveStackExercise, onDeleteStackExercise, onClearPlaceholders, { showSettings = false }, background)
            else Column(background.padding(horizontal = 24.dp)) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp).height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                        TabIcon("gimnasio", "Hoy", false) { tab = ExerciseTab.TODAY }
                        TabIcon("bascula", "Cuerpo", shownTab == ExerciseTab.BODY) { tab = ExerciseTab.BODY }
                    }
                    SettingsGear({ showSettings = true })
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 12.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    BodyTab(day.date, settings, bodyMetrics, onAddBodyMetric)
                }
            }
        }
    }
}

/** When food went over the goal: exercises of today's group that burn the excess, least work first, for the scene. */
private fun excessOptions(session: Session, stack: List<StackExercise>, kcalOver: Float, weightKg: Float): String {
    if (kcalOver <= 0f) return "[]"
    val options = stack.filter { it.muscleGroup.equals(ExerciseBurn.groupOf(session), ignoreCase = true) }
    val ranked = options.filter { ExerciseBurn.kcal(it, weightKg) >= kcalOver }.sortedBy { ExerciseBurn.kcal(it, weightKg) } +
        options.filter { ExerciseBurn.kcal(it, weightKg) < kcalOver }.sortedByDescending { it.met }
    return org.json.JSONArray(ranked.map { pick ->
        val perMinute = ExerciseBurn.perMinute(pick.met, weightKg)
        val minutes = if (ExerciseBurn.kcal(pick, weightKg) >= kcalOver) pick.minutes else kotlin.math.ceil(kcalOver / perMinute).toInt().coerceIn(pick.minutes, 90)
        org.json.JSONObject().put("over", kcalOver.toInt()).put("name", pick.name).put("minutes", minutes).put("kcal", (perMinute * minutes).toInt())
    }).toString()
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
            Box(Modifier.weight(1f).height(34.dp).background(LauncherRed, RoundedCornerShape(3.dp)))
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
