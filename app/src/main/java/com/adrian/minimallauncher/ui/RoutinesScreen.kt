package com.adrian.minimallauncher.ui

import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.routine.ActionKind
import com.adrian.minimallauncher.data.routine.ActionState
import com.adrian.minimallauncher.data.routine.Place
import com.adrian.minimallauncher.data.routine.PhaseMode
import com.adrian.minimallauncher.data.routine.RoutineEngine
import com.adrian.minimallauncher.data.routine.RoutinePhase
import com.adrian.minimallauncher.data.routine.Trigger
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.feature.SettingsGear
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
import com.adrian.minimallauncher.ui.components.rememberReactions
import com.adrian.minimallauncher.viewmodel.LauncherViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

private val White = Color(0xFFF5F5F5)
private val Gray = Color(0xFF5C5C5C)
private val Muted = Color(0xFFB0B0B0)
private val Line = Color(0xFF222222)
private val Card = Color(0xFF0B0B0B)
private val hhmm = DateTimeFormatter.ofPattern("HH:mm")

/** Phase icon by seed id; phases the user adds get the generic one. Also used by the Rutinas widget. */
internal fun phaseIcon(phase: RoutinePhase) = when (phase.id) {
    "inicio", "traslado", "trabajo", "salida", "casa" -> phase.id
    else -> "nueva"
}

/** Rutinas and, behind its gear, the routine settings panel (places, apps, test tools). */
@Composable
fun RoutineHost(viewModel: LauncherViewModel, onClose: () -> Unit) {
    var settingsOpen by remember { mutableStateOf(false) }
    if (settingsOpen) RoutineSettingsScreen(viewModel) { settingsOpen = false }
    else RoutinesScreen(viewModel, onSettings = { settingsOpen = true }, onClose = onClose)
}

/**
 * Rutinas (concept: docs/design/concept/RutinasVisual.html). No text while in use: phases and actions are icons,
 * hours and numbers; what comes next is red. The pencil shows names and lets the user rename, change the trigger
 * (hour or place), and add or remove actions and phases. The gear opens the routine settings (places, apps).
 */
@Composable
fun RoutinesScreen(viewModel: LauncherViewModel, onSettings: () -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val phases by viewModel.phases.collectAsState()
    val current by viewModel.currentPhase.collectAsState()
    val now by viewModel.currentTime.collectAsState()
    val done by viewModel.routineDone.collectAsState()
    val places by viewModel.places.collectAsState()
    val apps by viewModel.apps.collectAsState()
    val settings by viewModel.moduleSettings.collectAsState()
    val reactions = rememberReactions(settings.haptics)

    var selectedId by remember { mutableStateOf(current.id) }
    var editing by remember { mutableStateOf(false) }
    var catalogOpen by remember { mutableStateOf(false) }
    val selected = phases.firstOrNull { it.id == selectedId } ?: current
    val currentIndex = phases.indexOfFirst { it.id == current.id }

    BackHandler {
        when {
            catalogOpen -> catalogOpen = false
            editing -> editing = false
            else -> onClose()
        }
    }

    Box(modifier.fillMaxSize().background(Color.Black).safeDrawingPadding()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.End) { SettingsGear(onSettings) }

            Column {
                PhaseStrip(phases, currentIndex, selected.id, editing, onPick = { selectedId = it }, onAdd = {
                    selectedId = viewModel.addPhase("Nueva", Trigger.Between(LocalTime.of(20, 0), LocalTime.of(21, 0)))
                })
                Spacer(Modifier.height(18.dp))
                DayLine(phases, now.toLocalTime())
            }

            val index = phases.indexOfFirst { it.id == selected.id }
            val phaseState = when {
                index < currentIndex -> StrokeState.DONE
                index == currentIndex -> StrokeState.NOW
                else -> StrokeState.OFF
            }
            val states = RoutineEngine.actionStates(selected, now.toLocalDate(), done)
            PhaseCard(
                phase = selected,
                state = phaseState,
                isCurrent = selected.id == current.id,
                states = states,
                places = places,
                openApps = apps.count { selected.allows(it) },
                lockedApps = apps.count { !selected.allows(it) },
                editing = editing,
                viewModel = viewModel,
                onEdit = { editing = it },
                onAddAction = { catalogOpen = true },
                onDeletePhase = {
                    viewModel.deletePhase(selected.id)
                    selectedId = current.id
                    editing = false
                },
                onMarked = reactions::tick,
            )
        }

        if (catalogOpen) Catalog(
            have = selected.actions.map { it.type }.toSet(),
            onPick = { type -> viewModel.addAction(selected.id, type); catalogOpen = false },
            onClose = { catalogOpen = false },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun PhaseStrip(phases: List<RoutinePhase>, currentIndex: Int, selectedId: String, editing: Boolean, onPick: (String) -> Unit, onAdd: () -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        phases.forEachIndexed { i, phase ->
            val state = when {
                i < currentIndex -> StrokeState.DONE
                i == currentIndex -> StrokeState.NOW
                else -> StrokeState.OFF
            }
            Column(
                Modifier.weight(1f).clickable(role = Role.Button) { onPick(phase.id) }.semantics { contentDescription = phase.name },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StrokeIcon(phaseIcon(phase), state, 40.dp)
                TriggerMark(phase, color = when {
                    state == StrokeState.DONE -> White
                    i == currentIndex + 1 -> LauncherRed
                    else -> Muted
                })
                Box(Modifier.size(4.dp).background(if (phase.id == selectedId) White else Color.Transparent, CircleShape))
            }
        }
        if (editing) Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            AddSlot(40, onAdd, "Agregar fase")
        }
    }
}

/** The phase's hour, or a pin when a place starts it. */
@Composable
private fun TriggerMark(phase: RoutinePhase, color: Color) {
    Box(Modifier.height(16.dp), contentAlignment = Alignment.Center) {
        when (val t = phase.trigger) {
            is Trigger.Between -> Text(t.start.format(hhmm), style = MaterialTheme.typography.labelSmall, color = color)
            else -> StrokeIcon("pin", StrokeState.OFF, 14.dp, color = color)
        }
    }
}

/** The day as one line: gray where a routine holds, white what already passed, a red mark now. */
@Composable
private fun DayLine(phases: List<RoutinePhase>, now: LocalTime) {
    val nowH = now.hour + now.minute / 60f
    Canvas(Modifier.fillMaxWidth().height(14.dp).semantics { contentDescription = "El día; la marca roja es ahora" }) {
        val y = size.height / 2
        fun x(h: Float) = h / 24f * size.width
        drawLine(Color(0xFF2A2A2A), Offset(0f, y), Offset(size.width, y), 1.5.dp.toPx(), StrokeCap.Round)
        val thick = 3.dp.toPx()
        val gap = 2.dp.toPx()
        phases.mapNotNull { it.window }.forEach { w ->
            val start = w.start.hour + w.start.minute / 60f
            var end = w.end.hour + w.end.minute / 60f
            if (end <= start) end += 24f
            listOf(start to minOf(end, 24f), 0f to (end - 24f)).filter { (a, b) -> b > a }.forEach { (a, b) ->
                drawLine(Gray, Offset(x(a) + gap, y), Offset(x(b) - gap, y), thick, StrokeCap.Round)
                if (a < nowH) drawLine(White, Offset(x(a) + gap, y), Offset(x(minOf(b, nowH)) - gap, y), thick, StrokeCap.Round)
            }
        }
        drawLine(LauncherRed, Offset(x(nowH), 1.dp.toPx()), Offset(x(nowH), size.height - 1.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
    }
}

@Composable
private fun PhaseCard(
    phase: RoutinePhase,
    state: StrokeState,
    isCurrent: Boolean,
    states: List<ActionState>,
    places: List<Place>,
    openApps: Int,
    lockedApps: Int,
    editing: Boolean,
    viewModel: LauncherViewModel,
    onEdit: (Boolean) -> Unit,
    onAddAction: () -> Unit,
    onDeletePhase: () -> Unit,
    onMarked: () -> Unit,
) {
    var name by remember(phase.id) { mutableStateOf(phase.name) }
    val plays = remember(phase.id) { mutableStateMapOf<String, Int>() }
    var resting by remember(phase.id) { mutableStateOf<ActionState?>(null) }
    val next = RoutineEngine.nextStep(states)

    fun tap(s: ActionState) {
        if (editing) {
            viewModel.setActionEnabled(phase.id, s.action.id, !s.on)
            if (!s.on) plays[s.action.id] = (plays[s.action.id] ?: 0) + 1
            return
        }
        if (!isCurrent) return
        when (s.action.kind.kind) {
            ActionKind.AUDIO -> viewModel.openPhaseAudio(s.action.id)
            ActionKind.WHATSAPP -> viewModel.openWhatsAppFor(phase)
            ActionKind.PENDING -> Unit
            ActionKind.TIMER -> if (!s.done && RoutineEngine.canMark(phase, states, s.action.id)) { resting = s; return }
            ActionKind.DOT -> Unit
        }
        if (s.markable && RoutineEngine.canMark(phase, states, s.action.id)) {
            viewModel.markAction(s.action.id)
            onMarked()
            if (s.done) return
        }
        plays[s.action.id] = (plays[s.action.id] ?: 0) + 1
    }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Card).border(1.dp, Line, RoundedCornerShape(24.dp)).padding(18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (editing) {
                BasicTextField(
                    name, { name = it.take(30) },
                    Modifier.weight(1f).semantics { contentDescription = "Nombre de la fase" },
                    textStyle = MaterialTheme.typography.titleLarge.copy(color = White),
                    singleLine = true,
                    cursorBrush = SolidColor(LauncherRed),
                )
                IconButton("papelera", "Quitar fase", onDeletePhase)
                IconButton("listo", "Listo", { if (name.isNotBlank() && name != phase.name) viewModel.renamePhase(phase.id, name); onEdit(false) }, color = LauncherRed)
            } else {
                StrokeIcon(phaseIcon(phase), state, 36.dp)
                Spacer(Modifier.width(12.dp))
                TriggerMark(phase, if (state == StrokeState.NOW) White else Muted)
                Spacer(Modifier.weight(1f))
                IconButton("lapiz", "Editar fase", { name = phase.name; onEdit(true) })
            }
        }
        if (editing) TriggerEditor(phase, places, viewModel)

        if (lockedApps > 0) Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StrokeIcon("apps", StrokeState.DONE, 18.dp)
                Text("$openApps", style = MaterialTheme.typography.labelSmall, color = White)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StrokeIcon("candado", StrokeState.NOW, 18.dp)
                Text("$lockedApps", style = MaterialTheme.typography.labelSmall, color = LauncherRed)
            }
        }
        Box(Modifier.padding(top = 16.dp).fillMaxWidth().height(1.dp).background(Line))

        val shown = if (editing) states else states.filter { it.on }
        if (phase.mode == PhaseMode.ORDERED && !editing) {
            Chain(shown, next, plays, ::tap)
            resting?.let { s ->
                RestRing(s.action.minutes.coerceAtLeast(1)) {
                    viewModel.markAction(s.action.id)
                    onMarked()
                    plays[s.action.id] = (plays[s.action.id] ?: 0) + 1
                    resting = null
                }
            }
        } else {
            ActionGrid(shown, next, editing, plays, ::tap, onRemove = { viewModel.removeAction(phase.id, it) }, onAdd = onAddAction)
        }
    }
}

@Composable
private fun ColumnScope.TriggerEditor(phase: RoutinePhase, places: List<Place>, viewModel: LauncherViewModel) {
    val context = LocalContext.current
    val byTime = phase.trigger is Trigger.Between
    val fallback = phase.window ?: Trigger.Between(LocalTime.of(20, 0), LocalTime.of(21, 0))
    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip("reloj", "HORA", byTime) { if (!byTime) viewModel.setPhaseTrigger(phase.id, fallback) }
        Chip("pin", "LUGAR", !byTime) { if (byTime) viewModel.setPhaseTrigger(phase.id, Trigger.Arrive(places.firstOrNull()?.id ?: "casa"), phase.window) }
    }
    when (val t = phase.trigger) {
        is Trigger.Between -> Text(
            t.start.format(hhmm),
            Modifier.padding(top = 10.dp).border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
                .clickable(role = Role.Button) {
                    TimePickerDialog(context, { _, h, m ->
                        val start = LocalTime.of(h, m)
                        val length = java.time.Duration.between(t.start, t.end).let { if (it.isNegative || it.isZero) it.plusHours(24) else it }
                        viewModel.setPhaseTrigger(phase.id, Trigger.Between(start, start.plus(length)))
                    }, t.start.hour, t.start.minute, true).show()
                }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall, color = Muted,
        )
        is Trigger.Arrive, is Trigger.Leave -> {
            val placeId = if (t is Trigger.Arrive) t.placeId else (t as Trigger.Leave).placeId
            val leave = t is Trigger.Leave
            Row(Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                places.forEach { place ->
                    Chip(null, place.name.uppercase(), place.id == placeId) {
                        viewModel.setPhaseTrigger(phase.id, if (leave) Trigger.Leave(place.id) else Trigger.Arrive(place.id), phase.window)
                    }
                }
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(null, "LLEGAR", !leave) { viewModel.setPhaseTrigger(phase.id, Trigger.Arrive(placeId), phase.window) }
                Chip(null, "SALIR", leave) { viewModel.setPhaseTrigger(phase.id, Trigger.Leave(placeId), phase.window) }
            }
        }
    }
}

@Composable
private fun ActionGrid(
    states: List<ActionState>,
    next: ActionState?,
    editing: Boolean,
    plays: Map<String, Int>,
    onTap: (ActionState) -> Unit,
    onRemove: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val cells: List<ActionState?> = states + if (editing) listOf(null) else emptyList()
    Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        cells.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { s ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                        if (s == null) AddSlot(48, onAdd, "Agregar acción")
                        else ActionCell(s, next, editing, plays[s.action.id], onTap, onRemove)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ActionCell(s: ActionState, next: ActionState?, editing: Boolean, play: Int?, onTap: (ActionState) -> Unit, onRemove: (String) -> Unit) {
    val lit = if (editing) s.on else s.done
    Column(
        Modifier.clickable(role = Role.Button) { onTap(s) }.semantics { contentDescription = s.action.kind.label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box {
            StrokeIcon(s.action.kind.icon, if (lit) StrokeState.NOW else StrokeState.OFF, 48.dp, playKey = play)
            if (editing) Text(
                "✕",
                Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-8).dp).size(18.dp).clip(CircleShape)
                    .background(Color(0xFF1C1C1C)).border(1.dp, Color(0xFF333333), CircleShape)
                    .clickable(role = Role.Button) { onRemove(s.action.id) }.semantics { contentDescription = "Quitar" },
                style = MaterialTheme.typography.labelSmall, color = Muted, textAlign = TextAlign.Center,
            )
        }
        when {
            editing -> Text(s.action.kind.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Muted)
            s.action.at != null -> Text(
                s.action.at!!.format(hhmm), style = MaterialTheme.typography.labelSmall,
                color = when {
                    s.done -> White
                    next?.action?.id == s.action.id -> LauncherRed
                    else -> Muted
                },
            )
        }
    }
}

/** An ordered phase: its steps as a chain; a red dot marks the next one. */
@Composable
private fun Chain(states: List<ActionState>, next: ActionState?, plays: Map<String, Int>, onTap: (ActionState) -> Unit) {
    Row(Modifier.padding(top = 22.dp).fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
        states.forEachIndexed { i, s ->
            if (i > 0) Box(Modifier.width(14.dp).height(1.5.dp).background(if (states[i - 1].done && s.done) White else Color(0xFF333333)))
            Column(
                Modifier.clickable(role = Role.Button) { onTap(s) }.semantics { contentDescription = s.action.kind.label },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(6.dp).background(if (next?.action?.id == s.action.id) LauncherRed else Color.Transparent, CircleShape))
                Spacer(Modifier.height(6.dp))
                StrokeIcon(s.action.kind.icon, if (s.done) StrokeState.NOW else StrokeState.OFF, 40.dp, playKey = plays[s.action.id])
            }
        }
    }
}

/** The rest timer: a ring that shortens until it closes (red in the last quarter), then the step is marked. */
@Composable
private fun RestRing(minutes: Int, onDone: () -> Unit) {
    val total = minutes * 60
    var left by remember { mutableIntStateOf(total) }
    LaunchedEffect(Unit) {
        while (left > 0) { delay(1_000); left-- }
        onDone()
    }
    Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(44.dp).semantics { contentDescription = "Descanso" }) {
            val w = 2.dp.toPx()
            drawCircle(Color(0xFF1C1C1C), radius = size.minDimension / 2 - w, style = Stroke(w))
            val fraction = left / total.toFloat()
            drawArc(
                if (fraction < 0.25f) LauncherRed else White, -90f, 360f * fraction, false,
                topLeft = Offset(w, w), size = androidx.compose.ui.geometry.Size(size.width - 2 * w, size.height - 2 * w),
                style = Stroke(w, cap = StrokeCap.Round),
            )
        }
    }
}

/** The list of actions to add (labels are fine here: it is editing). The empty slot keeps room for new ones. */
@Composable
private fun Catalog(have: Set<String>, onPick: (String) -> Unit, onClose: () -> Unit, modifier: Modifier) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(Card)
            .border(1.dp, Line, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 32.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("AGREGAR", style = MaterialTheme.typography.labelSmall, color = LauncherRed)
            Spacer(Modifier.weight(1f))
            Text("✕", Modifier.clickable(role = Role.Button, onClick = onClose).padding(6.dp), style = MaterialTheme.typography.labelSmall, color = Muted)
        }
        val types: List<com.adrian.minimallauncher.data.routine.ActionType?> = com.adrian.minimallauncher.data.routine.ActionCatalog.all + listOf(null)
        Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            types.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { type ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                            if (type == null) DashedBox(36)
                            else Column(
                                Modifier.clickable(role = Role.Button) { onPick(type.key) }.semantics { contentDescription = type.label },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                StrokeIcon(type.icon, if (type.key in have) StrokeState.DONE else StrokeState.OFF, 36.dp)
                                Text(type.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Muted)
                            }
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun IconButton(icon: String, description: String, onClick: () -> Unit, color: Color = Muted) {
    Box(
        Modifier.size(36.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { StrokeIcon(icon, StrokeState.DONE, 20.dp, color = color) }
}

@Composable
private fun Chip(icon: String?, label: String, on: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, Color(0xFF333333), RoundedCornerShape(10.dp))
            .background(if (on) Color(0xFF1C1C1C) else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) StrokeIcon(icon, if (on) StrokeState.DONE else StrokeState.OFF, 16.dp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (on) White else Muted)
    }
}

@Composable
private fun AddSlot(sizeDp: Int, onClick: () -> Unit, description: String) {
    Box(
        Modifier.clip(RoundedCornerShape(14.dp)).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        DashedBox(sizeDp)
        Text("+", style = MaterialTheme.typography.titleMedium, color = Gray)
    }
}

@Composable
private fun DashedBox(sizeDp: Int) {
    Canvas(Modifier.size(sizeDp.dp)) {
        val w = 1.5.dp.toPx()
        drawRoundRect(
            Color(0xFF3A3A3A), topLeft = Offset(w / 2, w / 2), size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
            style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))),
        )
    }
}
