package com.adrian.minimallauncher.feature.exercise

import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.ModuleSettings
import com.adrian.minimallauncher.feature.LauncherAccent
import com.adrian.minimallauncher.feature.finance.ActionColor
import com.adrian.minimallauncher.feature.finance.ActionPill
import com.adrian.minimallauncher.feature.finance.FinanceBackHeader
import com.adrian.minimallauncher.feature.finance.FinanceMuted
import com.adrian.minimallauncher.feature.finance.FinanceWhite
import com.adrian.minimallauncher.feature.finance.GlassFill
import com.adrian.minimallauncher.feature.finance.GlassLine
import com.adrian.minimallauncher.feature.finance.InfoIcon
import com.adrian.minimallauncher.feature.finance.LabelCream
import com.adrian.minimallauncher.feature.finance.MoneyPlenty
import com.adrian.minimallauncher.feature.finance.NumberStepper
import com.adrian.minimallauncher.feature.finance.PlanState
import com.adrian.minimallauncher.feature.finance.Pop
import com.adrian.minimallauncher.feature.finance.PopAction
import com.adrian.minimallauncher.feature.finance.PopRow
import com.adrian.minimallauncher.feature.finance.SectionHead
import com.adrian.minimallauncher.feature.finance.SectionLabel
import com.adrian.minimallauncher.feature.finance.StateDot
import com.adrian.minimallauncher.feature.finance.TextBlue
import com.adrian.minimallauncher.feature.finance.TintFigure
import com.adrian.minimallauncher.feature.finance.glass
import com.adrian.minimallauncher.feature.finance.tinted
import com.adrian.minimallauncher.ui.components.LocalReactions
import kotlinx.coroutines.delay

/**
 * The session, one movement at a time, in reading order:
 * 1. What is now: its name, what it asks and its series as dots (a done one can be undone from its box).
 * 2. Weight and reps to log, or the rest ring after a series.
 * 3. Serie hecha.
 * 4. Every movement of the day, to jump to another; Terminar closes the day early.
 * When everything is logged, the day says so in green.
 */
@Composable
internal fun ExerciseSession(
    day: ExerciseDay,
    settings: ModuleSettings,
    lastKg: (String) -> Float?,
    onLogSet: (PlanExercise, Float, Int) -> Unit,
    onUndoSet: (PlanExercise, Int) -> Unit,
    onFinish: (Boolean) -> Unit,
    onOpenMap: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val view = LocalView.current
    val reactions = LocalReactions.current
    // The screen stays on while training; the rest alert only vibrates from the background when this is closed.
    DisposableEffect(view) {
        view.keepScreenOn = true
        ExerciseTimerAlert.sceneVisible = true
        onDispose {
            view.keepScreenOn = false
            ExerciseTimerAlert.sceneVisible = false
        }
    }
    val session = day.session
    val exercises = session.exercises
    fun doneOf(ex: PlanExercise) = day.sets[ex.id].orEmpty().size.coerceAtMost(ex.sets)
    val firstOpen = exercises.indexOfFirst { doneOf(it) < it.sets }.coerceAtLeast(0)
    var current by rememberSaveable { mutableIntStateOf(firstOpen) }
    var restEnd by rememberSaveable { mutableLongStateOf(0L) }
    val finished = day.completed || (exercises.isNotEmpty() && exercises.all { doneOf(it) >= it.sets })

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        FinanceBackHeader(session.title, onBack)

        if (finished) {
            Finished(day, onBack) { onFinish(false) }
            return@Column
        }
        if (exercises.isEmpty()) {
            // Cardio and intervals of the guide: one block, done when you say so.
            Column(Modifier.fillMaxWidth().glass().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                session.minutes()?.let { Text("$it min", style = MaterialTheme.typography.headlineMedium, color = FinanceWhite) }
                Text(session.note, style = MaterialTheme.typography.bodyMedium, color = FinanceWhite)
            }
            ActionPill("Terminé", "listo", Modifier.fillMaxWidth()) { onFinish(true); reactions.done() }
            return@Column
        }

        val index = current.coerceIn(0, exercises.lastIndex)
        val ex = exercises[index]
        val logs = day.sets[ex.id].orEmpty()
        val done = doneOf(ex)
        val cardio = ex.group() == "cardio"

        // 1. Now: the movement and, beside it, what it works (a tap opens the map in big).
        val muscles = ex.muscles()
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel(MuscleGroups[ex.group()] ?: ex.group())
                    val help = listOf(ex.how, ex.care).filter { it.isNotBlank() }.distinct().joinToString("\n\n")
                    if (help.isNotBlank()) InfoIcon(help)
                }
                Text(ex.name, style = MaterialTheme.typography.headlineSmall, color = FinanceWhite, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(ex.ask(lastKg(ex.id) ?: ex.startKg), style = MaterialTheme.typography.titleMedium, color = LabelCream)
                namesAt(muscles, PRIMARY).takeIf { it.isNotEmpty() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MusclePrimary) }
                namesAt(muscles, HELPS).takeIf { it.isNotEmpty() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = FinanceWhite) }
            }
            Column(
                Modifier.tinted(TextBlue, radius = 18.dp).clickable(role = Role.Button) { onOpenMap(index) }
                    .semantics { contentDescription = "Ver músculos en grande" }.padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                MuscleMap(muscles, height = 104.dp, labels = false)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(ex.sets) { i ->
                if (i < done) {
                    val log = logs.getOrNull(i)
                    Pop(Modifier.size(36.dp), "Serie ${i + 1} hecha", pop = { close ->
                        PopRow("Serie ${i + 1}", log?.let { if (ex.timed) "${it.reps} s" else "${it.reps} × ${kgText(it.weightKg)} kg" } ?: "", MoneyPlenty, dot = MoneyPlenty)
                        PopAction("papelera", "Deshacer desde esta serie", ActionColor.Delete) { close(); onUndoSet(ex, i); restEnd = 0L }
                    }) { Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) { StateDot(PlanState.DONE, 20.dp) } }
                } else Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    StateDot(if (i == done) PlanState.TODAY else PlanState.PLANNED, 20.dp)
                }
            }
        }

        // 2. What to log, or the rest.
        val resting = restEnd > System.currentTimeMillis()
        if (resting) {
            Rest(restEnd, settings.exerciseRestSeconds, settings.haptics) { restEnd = 0L }
        } else if (done < ex.sets) {
            val lastToday = logs.lastOrNull()
            var kg by remember(ex.id, done) { mutableFloatStateOf(lastToday?.weightKg ?: lastKg(ex.id) ?: ex.startKg ?: 0f) }
            var reps by remember(ex.id, done) { mutableIntStateOf(lastToday?.reps ?: ex.minTarget) }
            val step = if (cardio || ex.timed) 5 else 1
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!ex.timed) NumberStepper("Peso kg", kgText(kg), Modifier.weight(1f),
                    onMinus = { kg = (kg - 1f).coerceAtLeast(0f) }, onPlus = { kg = (kg + 1f).coerceAtMost(300f) })
                NumberStepper(if (cardio) "Segundos" else if (ex.timed) "Segundos" else "Reps", "$reps", Modifier.weight(1f),
                    onMinus = { reps = (reps - step).coerceAtLeast(1) }, onPlus = { reps = (reps + step).coerceAtMost(if (ex.timed) 7200 else 100) })
            }
            // 3. Serie hecha
            ActionPill("Serie hecha", "listo", Modifier.fillMaxWidth()) {
                onLogSet(ex, kg, reps)
                val last = done + 1 >= ex.sets
                val all = exercises.all { other -> if (other.id == ex.id) last else doneOf(other) >= other.sets }
                if (all) reactions.done() else {
                    reactions.tick()
                    restEnd = System.currentTimeMillis() + settings.exerciseRestSeconds * 1000L
                    if (settings.haptics) ExerciseTimerAlert.arm(context, restEnd)
                    if (last) exercises.indexOfFirst { other -> other.id != ex.id && doneOf(other) < other.sets }.takeIf { it >= 0 }?.let { current = it }
                }
            }
        } else {
            Text("Este ya quedó", style = MaterialTheme.typography.bodyMedium, color = MoneyPlenty)
        }

        // 4. What comes next, then every movement of the day.
        exercises.withIndex().firstOrNull { (i, other) -> i != index && doneOf(other) < other.sets }?.let { (i, next) ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Sigue")
                Row(
                    Modifier.fillMaxWidth().tinted(com.adrian.minimallauncher.feature.finance.MoneyLow, radius = 16.dp, dashed = true)
                        .clickable(role = Role.Button) { current = i; restEnd = 0L }.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(next.name, style = MaterialTheme.typography.bodyLarge, color = FinanceWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(next.ask(lastKg(next.id) ?: next.startKg), style = MaterialTheme.typography.bodySmall, color = LabelCream)
                    }
                    MuscleMap(next.muscles(), height = 52.dp, labels = false)
                }
            }
        }
        Column {
            SectionHead("Ejercicios", "Toca uno para ir a él. Lo hecho queda con palomita; si te equivocaste, toca la serie para deshacerla.")
            exercises.forEachIndexed { i, other ->
                val otherDone = doneOf(other)
                val state = when {
                    otherDone >= other.sets -> PlanState.DONE
                    i == index -> PlanState.TODAY
                    else -> PlanState.PLANNED
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .then(if (i == index) Modifier.tinted(LauncherAccent, radius = 14.dp) else Modifier)
                        .clickable(role = Role.Button) { current = i; restEnd = 0L }
                        .semantics(mergeDescendants = true) { contentDescription = other.name }
                        .heightIn(min = 56.dp)
                        .padding(horizontal = if (i == index) 12.dp else 0.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StateDot(state, 14.dp)
                    Text(other.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = if (state == PlanState.DONE) MoneyPlenty else FinanceWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(other.ask(lastKg(other.id) ?: other.startKg), style = MaterialTheme.typography.bodySmall, color = LabelCream)
                }
                if (i != index) Box(Modifier.fillMaxWidth().height(1.dp).background(GlassLine))
            }
        }
        ActionPill("Ver músculos en grande", null, Modifier.fillMaxWidth(), primary = false) { onOpenMap(index) }
        ActionPill("Terminar por hoy", "listo", Modifier.fillMaxWidth(), primary = false) { onFinish(true); reactions.done() }
    }
}

/** The rest as a ring that empties, its seconds in the middle; a buzz when it ends. */
@Composable
private fun Rest(endAt: Long, seconds: Int, haptics: Boolean, onSkip: () -> Unit) {
    val context = LocalContext.current
    val now by produceState(System.currentTimeMillis(), endAt) {
        while (value < endAt) { delay(200); value = System.currentTimeMillis() }
    }
    val left = ((endAt - now) / 1000L).coerceAtLeast(0L)
    LaunchedEffect(endAt) {
        delay((endAt - System.currentTimeMillis()).coerceAtLeast(0L))
        if (haptics) context.getSystemService(Vibrator::class.java)?.let { v -> SessionHaptics.effect("success", v)?.let(v::vibrate) }
        ExerciseTimerAlert.arm(context, 0)
        onSkip()
    }
    Row(Modifier.fillMaxWidth().glass().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(96.dp)) {
                val w = 6.dp.toPx()
                val box = Size(size.width - w, size.height - w)
                drawArc(GlassFill, 0f, 360f, false, Offset(w / 2, w / 2), box, style = Stroke(w))
                val frac = ((endAt - now).toFloat() / (seconds * 1000f)).coerceIn(0f, 1f)
                drawArc(TextBlue, -90f, 360f * frac, false, Offset(w / 2, w / 2), box, style = Stroke(w, cap = StrokeCap.Round))
            }
            Text("$left", style = MaterialTheme.typography.headlineMedium, color = FinanceWhite)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Descanso", style = MaterialTheme.typography.titleMedium, color = TextBlue)
            ActionPill("Saltar", null, primary = false) { ExerciseTimerAlert.arm(context, 0); onSkip() }
        }
    }
}

/** The day done: in green, with its series and the weight moved; undo reopens it. */
@Composable
private fun Finished(day: ExerciseDay, onBack: () -> Unit, onReopen: () -> Unit) {
    val logs = day.sets.values.flatten()
    val volume = logs.sumOf { (it.weightKg * it.reps).toDouble() }.toInt()
    Column(Modifier.fillMaxWidth().tinted(MoneyPlenty).padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Terminaste", style = MaterialTheme.typography.headlineMedium, color = MoneyPlenty)
        Text(day.session.title, style = MaterialTheme.typography.bodyMedium, color = FinanceWhite)
    }
    if (logs.isNotEmpty()) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TintFigure("Series", "${logs.size}", TextBlue, Modifier.weight(1f))
        TintFigure("Peso movido", "%,d kg".format(volume), TextBlue, Modifier.weight(1f))
    }
    ActionPill("Listo", "listo", Modifier.fillMaxWidth(), onClick = onBack)
    Text(
        "Reabrir la sesión",
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onReopen).padding(vertical = 12.dp),
        style = MaterialTheme.typography.bodyMedium, color = FinanceMuted,
    )
}
