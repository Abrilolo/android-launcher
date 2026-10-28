package com.adrian.minimallauncher.feature.exercise

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.BodyMetricRecord
import com.adrian.minimallauncher.data.ModuleSettings
import com.adrian.minimallauncher.feature.CardLine
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.feature.ModuleCard
import com.adrian.minimallauncher.feature.ModuleHeader
import com.adrian.minimallauncher.feature.ModuleTab
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.feature.StatusDot
import com.adrian.minimallauncher.ui.components.DotMatrixText
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay

private val spanish = Locale.forLanguageTag("es")
private enum class ExerciseTab { TODAY, WEEK, SETTINGS }

/** Big dot-matrix tag for a session; the glyph set has no "~" or "·", so non-strength days get a word. */
fun Session.displayCode(): String = when (kind) {
    SessionKind.STRENGTH -> code
    SessionKind.CARDIO -> "CARDIO"
    SessionKind.INTERVALS -> "HIIT"
    SessionKind.REST -> "OFF"
}

@Composable
fun ExerciseScreen(
    day: ExerciseDay,
    settings: ModuleSettings,
    bodyMetrics: List<BodyMetricRecord>,
    onLogSet: (PlanExercise, Float, Int) -> Unit,
    onUndoSet: (PlanExercise, Int) -> Unit,
    onSetCompleted: (Boolean) -> Unit,
    onAddBodyMetric: (String, Float) -> Unit,
    onSaveSettings: (ModuleSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableStateOf(ExerciseTab.TODAY) }
    var restLeft by rememberSaveable { mutableIntStateOf(0) }
    var restLength by rememberSaveable(settings.exerciseRestSeconds) { mutableIntStateOf(settings.exerciseRestSeconds) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(restLeft) {
        if (restLeft > 0) {
            delay(1_000L)
            restLeft--
            if (restLeft == 0) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Column(modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        ModuleHeader("EJERCICIO", onBack, Modifier.padding(top = 8.dp)) {
            ModuleTab("HOY", tab == ExerciseTab.TODAY) { tab = ExerciseTab.TODAY }
            ModuleTab("SEMANA", tab == ExerciseTab.WEEK) { tab = ExerciseTab.WEEK }
            ModuleTab("AJUSTES", tab == ExerciseTab.SETTINGS) { tab = ExerciseTab.SETTINGS }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 12.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            when (tab) {
                ExerciseTab.TODAY -> TodayTab(day, settings, onLogSet = { ex, kg, reps -> onLogSet(ex, kg, reps); restLeft = restLength }, onUndoSet, onSetCompleted)
                ExerciseTab.WEEK -> WeekTab(day, bodyMetrics, onAddBodyMetric)
                ExerciseTab.SETTINGS -> SettingsTab(settings, onSaveSettings)
            }
        }
        if (tab == ExerciseTab.TODAY && day.session.kind == SessionKind.STRENGTH) {
            RestTimer(restLeft, restLength, onPick = { restLength = it; restLeft = 0 }, onToggle = { restLeft = if (restLeft > 0) 0 else restLength })
        }
    }
}

@Composable
private fun TodayTab(
    day: ExerciseDay,
    settings: ModuleSettings,
    onLogSet: (PlanExercise, Float, Int) -> Unit,
    onUndoSet: (PlanExercise, Int) -> Unit,
    onSetCompleted: (Boolean) -> Unit,
) {
    val session = day.session
    val weekday = day.date.dayOfWeek.getDisplayName(TextStyle.FULL, spanish).uppercase(spanish)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SmallLabel("HOY · $weekday")
            Text(session.title, style = MaterialTheme.typography.headlineSmall)
            val time = if (session.kind == SessionKind.REST) session.duration else "${settings.exerciseStart} · ${session.duration}"
            SmallLabel(time, color = MaterialTheme.colorScheme.onSurface)
        }
        DotMatrixText(session.displayCode(), cell = if (session.kind == SessionKind.STRENGTH) 9.dp else 4.dp)
    }

    when (session.kind) {
        SessionKind.STRENGTH -> {
            SetsProgress(day.doneSets, session.totalSets)
            WarmupCard(day.date)
            val current = session.exercises.indexOfFirst { day.sets[it.id].orEmpty().size < it.sets }
            var openId by rememberSaveable(day.date) { mutableStateOf(session.exercises.getOrNull(current.coerceAtLeast(0))?.id) }
            session.exercises.forEachIndexed { index, exercise ->
                ExerciseCard(
                    number = index + 1,
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
            CompleteButton(day.completed, onSetCompleted)
        }
        SessionKind.CARDIO, SessionKind.INTERVALS -> {
            ModuleCard {
                Text(session.note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                if (session.kind == SessionKind.INTERVALS) IntervalBars()
            }
            CompleteButton(day.completed, onSetCompleted)
        }
        SessionKind.REST -> ModuleCard { Text(session.note, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun SetsProgress(done: Int, total: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            SmallLabel("SERIES")
            SmallLabel("$done / $total")
        }
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(3.dp)) {
            repeat(total) { i ->
                val color = when {
                    i < done -> MaterialTheme.colorScheme.onSurface
                    i == done -> LauncherRed
                    else -> Color(0xFF2A2A2A)
                }
                Box(Modifier.weight(1f).height(6.dp).background(color, RoundedCornerShape(3.dp)))
            }
        }
    }
}

@Composable
private fun WarmupCard(date: LocalDate) {
    var done by rememberSaveable(date) { mutableStateOf(false) }
    ModuleCard(Modifier.clickable(role = Role.Checkbox) { done = !done }) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            SmallLabel("CALENTAMIENTO · 5-7 MIN")
            SmallLabel(if (done) "HECHO" else "PENDIENTE", color = if (done) MaterialTheme.colorScheme.onSurface else LauncherRed)
        }
        Text(TokyoGuide.warmup.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ExerciseCard(
    number: Int,
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
    val finished = done.size >= exercise.sets
    ModuleCard(border = if (isCurrent) Color(0xFF3A3A3A) else CardLine) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).clickable(role = Role.Button, onClick = onToggleOpen).padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SmallLabel("%02d".format(number))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        exercise.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (finished) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                    SmallLabel(if (exercise.core) "${exercise.scheme} · ABDOMEN" else exercise.scheme)
                }
            }
            repeat(exercise.sets) { setIndex ->
                val filled = setIndex < done.size
                StatusDot(
                    filled = filled,
                    next = isCurrent && setIndex == done.size,
                    size = 24.dp,
                    modifier = Modifier
                        .clickable(role = Role.Checkbox) {
                            when {
                                filled -> onUndoSet(setIndex)
                                setIndex == done.size -> onLogSet(kg, reps)
                            }
                        }
                        .padding(4.dp),
                )
            }
        }
        AnimatedVisibility(isOpen) {
            Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF1F1F1F)))
                Text(exercise.how, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallLabel("CUIDA", color = LauncherRed)
                    Text(exercise.care, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!exercise.timed) {
                        Stepper("KG", if (kg == 0f) "—" else "%.0f".format(kg), Modifier.weight(1f), { kg = (kg - 1f).coerceAtLeast(0f) }, { kg += 1f })
                    }
                    Stepper(
                        if (exercise.timed) "SEG" else "REPS", reps.toString(), Modifier.weight(1f),
                        { reps = (reps - if (exercise.timed) 5 else 1).coerceAtLeast(1) },
                        { reps += if (exercise.timed) 5 else 1 },
                    )
                }
                val lastText = last?.let { if (exercise.timed) "${it.reps} S" else if (it.weightKg > 0f) "%.0f KG × %d".format(it.weightKg, it.reps) else "${it.reps} REPS" }
                SmallLabel(if (lastText != null) "ÚLTIMA VEZ · $lastText" else "PRIMERA VEZ · TOCA UN PUNTO PARA REGISTRAR LA SERIE")
            }
        }
    }
}

@Composable
private fun Stepper(label: String, value: String, modifier: Modifier, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        modifier.border(1.dp, Color(0xFF333333), RoundedCornerShape(16.dp)).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton("−", onMinus)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            SmallLabel(label, maxLines = 1)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
        StepperButton("+", onPlus)
    }
}

// Narrower than a TextButton so the label between the two fits on one line.
@Composable
private fun StepperButton(symbol: String, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(symbol, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun CompleteButton(completed: Boolean, onSetCompleted: (Boolean) -> Unit) {
    TextButton(
        onClick = { onSetCompleted(!completed) },
        modifier = Modifier.fillMaxWidth().border(1.dp, if (completed) MaterialTheme.colorScheme.onSurface else Color(0xFF333333), RoundedCornerShape(26.dp)),
    ) {
        Text(if (completed) "SESIÓN HECHA" else "MARCAR SESIÓN COMO HECHA", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun IntervalBars() {
    Row(Modifier.fillMaxWidth().height(34.dp), Arrangement.spacedBy(2.dp), Alignment.Bottom) {
        Box(Modifier.weight(10f).height(12.dp).background(Color(0xFF5A5A5A), RoundedCornerShape(3.dp)))
        repeat(6) {
            Box(Modifier.weight(1f).height(34.dp).background(LauncherRed, RoundedCornerShape(3.dp)))
            Box(Modifier.weight(3f).height(12.dp).background(Color(0xFF5A5A5A), RoundedCornerShape(3.dp)))
        }
        Box(Modifier.weight(10f).height(12.dp).background(Color(0xFF5A5A5A), RoundedCornerShape(3.dp)))
    }
}

@Composable
private fun RestTimer(left: Int, length: Int, onPick: (Int) -> Unit, onToggle: () -> Unit) {
    val shown = if (left > 0) left else length
    Row(
        Modifier.fillMaxWidth().padding(bottom = 16.dp).background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(26.dp)).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SmallLabel(if (left > 0) "DESCANSANDO" else "DESCANSO", color = Color.Black)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(60, 90, 120).forEach { seconds ->
                    val selected = seconds == length
                    Text(
                        "${seconds}S",
                        Modifier
                            .background(if (selected) Color.Black else Color.Transparent, RoundedCornerShape(14.dp))
                            .border(1.dp, Color.Black, RoundedCornerShape(14.dp))
                            .clickable(role = Role.RadioButton) { onPick(seconds) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) Color.White else Color.Black,
                    )
                }
            }
        }
        DotMatrixText(
            "%d:%02d".format(shown / 60, shown % 60),
            Modifier.clickable(role = Role.Button, onClick = onToggle).padding(8.dp),
            cell = 4.dp,
            color = Color.Black,
        )
    }
}

@Composable
private fun WeekTab(day: ExerciseDay, bodyMetrics: List<BodyMetricRecord>, onAddBodyMetric: (String, Float) -> Unit) {
    val monday = day.date.minusDays(day.date.dayOfWeek.value - 1L)
    var picked by rememberSaveable(day.date) { mutableIntStateOf(day.date.dayOfWeek.value - 1) }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SmallLabel("ESTA SEMANA · GUÍA TOKIO")
            Text("3 fuerza · 2 condición · 2 descanso", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    Column {
        TokyoGuide.week.forEachIndexed { index, session ->
            val date = monday.plusDays(index.toLong())
            val isToday = date == day.date
            val done = date in day.weekDone
            val rest = session.kind == SessionKind.REST
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (index == picked) Color(0xFF111111) else Color.Transparent)
                    .clickable(role = Role.Tab) { picked = index }
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SmallLabel(date.dayOfWeek.getDisplayName(TextStyle.NARROW, spanish).uppercase(spanish), Modifier.width(16.dp), color = if (isToday) LauncherRed else MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(session.title, style = MaterialTheme.typography.bodyLarge, color = if (rest || done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                    SmallLabel(if (session.kind == SessionKind.STRENGTH) "${session.code} · ${session.duration}" else session.duration)
                }
                StatusDot(filled = done, next = isToday && !done, size = 10.dp)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF1A1A1A)))
        }
    }

    val pickedSession = TokyoGuide.week[picked]
    ModuleCard {
        SmallLabel(pickedSession.title.uppercase(spanish))
        if (pickedSession.exercises.isNotEmpty()) {
            pickedSession.exercises.forEach { ex ->
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text(ex.name, style = MaterialTheme.typography.bodySmall)
                    SmallLabel(ex.scheme)
                }
            }
        } else {
            Text(pickedSession.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (pickedSession.kind == SessionKind.INTERVALS) IntervalBars()
        }
    }

    BodyTracking(day.date, bodyMetrics, onAddBodyMetric)

    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(22.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SmallLabel("REGLA DE PROGRESIÓN", color = Color.Black)
        Text(TokyoGuide.PROGRESSION, style = MaterialTheme.typography.bodyMedium, color = Color.Black)
        Text(TokyoGuide.INTENSITY, style = MaterialTheme.typography.bodySmall, color = Color(0xFF444444))
    }
}

@Composable
private fun BodyTracking(today: LocalDate, metrics: List<BodyMetricRecord>, onAdd: (String, Float) -> Unit) {
    val weekStart = today.minusDays(today.dayOfWeek.value - 1L)
    val weights = metrics.filter { it.type == "WEIGHT" && !it.createdAt.toLocalDate().isBefore(weekStart) }
    val waist = metrics.firstOrNull { it.type == "WAIST" }
    var weightInput by rememberSaveable { mutableStateOf("") }
    var waistInput by rememberSaveable { mutableStateOf("") }

    SmallLabel("SEGUIMIENTO")
    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
        ModuleCard(Modifier.weight(1f)) {
            SmallLabel("PESO · PROMEDIO")
            DotMatrixText(if (weights.isEmpty()) "--.-" else "%.1f".format(weights.map { it.value }.average()), cell = 3.5.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(3) { i -> StatusDot(filled = i < weights.size, next = i == weights.size, size = 10.dp) }
                SmallLabel("  3 MAÑANAS")
            }
        }
        ModuleCard(Modifier.weight(1f)) {
            SmallLabel("CINTURA")
            DotMatrixText(waist?.let { "%.0f".format(it.value) } ?: "--", cell = 3.5.dp)
            SmallLabel("1 VEZ POR SEMANA")
        }
    }
    MetricInput("Peso de hoy (kg) · al despertar", weightInput, { weightInput = it }) {
        weightInput.replace(',', '.').toFloatOrNull()?.takeIf { it in 30f..250f }?.let { onAdd("WEIGHT", it); weightInput = "" }
    }
    MetricInput("Cintura (cm) · a la altura del ombligo", waistInput, { waistInput = it }) {
        waistInput.replace(',', '.').toFloatOrNull()?.takeIf { it in 40f..200f }?.let { onAdd("WAIST", it); waistInput = "" }
    }
}

@Composable
private fun MetricInput(label: String, value: String, onValue: (String) -> Unit, onSave: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(value, onValue, Modifier.weight(1f), label = { Text(label) }, singleLine = true)
        TextButton(onClick = onSave) { Text("GUARDAR") }
    }
}

@Composable
private fun SettingsTab(settings: ModuleSettings, onSave: (ModuleSettings) -> Unit) {
    var startText by rememberSaveable(settings.exerciseStart) { mutableStateOf(settings.exerciseStart.toString()) }
    SmallLabel("PLAN ACTIVO")
    Text("Físico de atleta · Guía Tokio, octubre 2026", style = MaterialTheme.typography.bodyLarge)
    Text("Mancuernas hasta 10 kg, caminadora, bicicleta, elíptica y tapete.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

    SmallLabel("HORA DE INICIO")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(startText, { startText = it.take(5) }, Modifier.weight(1f), label = { Text("HH:mm") }, singleLine = true)
        TextButton(onClick = {
            runCatching { LocalTime.parse(startText) }.getOrNull()?.let { onSave(settings.copy(exerciseStart = it)) }
        }) { Text("GUARDAR") }
    }

    SmallLabel("DESCANSO PREDETERMINADO")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(60, 90, 120).forEach { seconds ->
            val selected = seconds == settings.exerciseRestSeconds
            Text(
                "${seconds} S",
                Modifier
                    .background(if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent, RoundedCornerShape(18.dp))
                    .border(1.dp, if (selected) MaterialTheme.colorScheme.onSurface else Color(0xFF333333), RoundedCornerShape(18.dp))
                    .clickable(role = Role.RadioButton) { onSave(settings.copy(exerciseRestSeconds = seconds)) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
    Spacer(Modifier.size(4.dp))
    SmallLabel("Tus series quedan guardadas en el teléfono con peso y repeticiones.")
}

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
