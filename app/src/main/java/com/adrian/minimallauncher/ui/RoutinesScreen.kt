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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adrian.minimallauncher.data.routine.ActionKind
import com.adrian.minimallauncher.data.routine.ActionState
import com.adrian.minimallauncher.data.routine.Place
import com.adrian.minimallauncher.data.routine.RoutineEngine
import com.adrian.minimallauncher.data.routine.RoutinePhase
import com.adrian.minimallauncher.data.routine.Trigger
import com.adrian.minimallauncher.feature.LauncherAccent
import com.adrian.minimallauncher.feature.SettingsGear
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
import com.adrian.minimallauncher.ui.components.rememberReactions
import com.adrian.minimallauncher.viewmodel.LauncherViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

private val White = Color(0xFFF5F5F5)
private val Gray = Color(0xFF5C5C5C)
private val Muted = Color(0xFFB0B0B0)
private val Line = Color(0xFF222222)
private val Rule = Color(0xFF161616)
private val hhmm = DateTimeFormatter.ofPattern("HH:mm")
private val spanish = Locale.forLanguageTag("es")

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
 * Rutinas (concept: docs/design/concept/RutinasVisual.html). Plain text labels: the phases are named under their icons,
 * the selected one is underlined; the open phase is a table of its actions with the hour and the words HECHO or SIGUE.
 * No cards. EDITAR turns the phase into a form (name, trigger, actions on or off, quitar, agregar). The gear opens the
 * routine settings (places, apps).
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
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("RUTINAS", style = MaterialTheme.typography.titleLarge, color = White)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TextAction("REINICIAR", LauncherAccent, viewModel::resetRoutine)
                    SettingsGear(onSettings)
                }
            }

            Column {
                PhaseStrip(phases, places, currentIndex, selected.id, editing, onPick = { selectedId = it; editing = false }, onAdd = {
                    selectedId = viewModel.addPhase("Nueva", Trigger.Between(LocalTime.of(20, 0), LocalTime.of(21, 0)))
                })
                Spacer(Modifier.height(16.dp))
                DayLine(phases, now.toLocalTime())
            }

            val index = phases.indexOfFirst { it.id == selected.id }
            val phaseState = when {
                index < currentIndex -> StrokeState.DONE
                index == currentIndex -> StrokeState.NOW
                else -> StrokeState.OFF
            }
            val states = RoutineEngine.actionStates(selected, now.toLocalDate(), done)
            PhaseDetail(
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

/** The phases in a row: icon, name, and the trigger (hour or place); the open one is underlined. */
@Composable
private fun PhaseStrip(
    phases: List<RoutinePhase>,
    places: List<Place>,
    currentIndex: Int,
    selectedId: String,
    editing: Boolean,
    onPick: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Row(Modifier.fillMaxWidth()) {
        phases.forEachIndexed { i, phase ->
            val state = when {
                i < currentIndex -> StrokeState.DONE
                i == currentIndex -> StrokeState.NOW
                else -> StrokeState.OFF
            }
            val open = phase.id == selectedId
            Column(
                Modifier.weight(1f).clickable(role = Role.Tab) { onPick(phase.id) }.semantics { contentDescription = phase.name },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StrokeIcon(phaseIcon(phase), state, 36.dp)
                Text(
                    phase.name.uppercase(spanish),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = if (open) White else Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Text(
                    triggerText(phase, places, short = true),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = when {
                        state == StrokeState.DONE -> White
                        i == currentIndex + 1 -> LauncherAccent
                        else -> Muted
                    },
                    maxLines = 1,
                )
                Box(Modifier.width(20.dp).height(2.dp).background(if (open) White else Color.Transparent))
            }
        }
        if (editing) Box(Modifier.weight(1f).clickable(role = Role.Button, onClick = onAdd).semantics { contentDescription = "Agregar fase" }) {
            Text("+ FASE", Modifier.padding(top = 10.dp), style = MaterialTheme.typography.labelSmall, color = White)
        }
    }
}

/** What starts a phase: its hour, or the place with arrive or leave (short form for the strip). */
private fun triggerText(phase: RoutinePhase, places: List<Place>, short: Boolean = false): String = when (val t = phase.trigger) {
    is Trigger.Between -> t.start.format(hhmm)
    is Trigger.Arrive -> if (short) "LUGAR" else "LLEGA · ${placeName(places, t.placeId)}"
    is Trigger.Leave -> if (short) "LUGAR" else "SALE · ${placeName(places, t.placeId)}"
}

private fun placeName(places: List<Place>, id: String): String =
    places.firstOrNull { it.id == id }?.name?.uppercase(spanish) ?: id.uppercase(spanish)

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
        drawLine(LauncherAccent, Offset(x(nowH), 1.dp.toPx()), Offset(x(nowH), size.height - 1.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
    }
}

/** The open phase: its name and trigger as text, its actions as a table, and the rest timer when one runs. */
@Composable
private fun PhaseDetail(
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

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (editing) {
                BasicTextField(
                    name, { name = it.take(30) },
                    Modifier.weight(1f).semantics { contentDescription = "Nombre de la fase" },
                    textStyle = MaterialTheme.typography.titleLarge.copy(color = White),
                    singleLine = true,
                    cursorBrush = SolidColor(LauncherAccent),
                )
                TextAction("QUITAR", Muted, onDeletePhase)
                TextAction("LISTO", LauncherAccent) {
                    if (name.isNotBlank() && name != phase.name) viewModel.renamePhase(phase.id, name)
                    onEdit(false)
                }
            } else {
                Text(
                    phase.name.uppercase(spanish),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (state == StrokeState.NOW) White else Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(triggerText(phase, places), style = MaterialTheme.typography.labelLarge, color = if (state == StrokeState.NOW) LauncherAccent else Muted)
                Spacer(Modifier.width(12.dp))
                TextAction("EDITAR", LauncherAccent) { name = phase.name; onEdit(true) }
            }
        }
        if (editing) TriggerEditor(phase, places, viewModel)

        if (!editing && lockedApps > 0) Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("ABIERTAS $openApps", style = MaterialTheme.typography.labelSmall, color = White)
            Text("BLOQUEADAS $lockedApps", style = MaterialTheme.typography.labelSmall, color = LauncherAccent)
        }
        Box(Modifier.padding(top = 12.dp).fillMaxWidth().height(1.dp).background(Line))

        val shown = if (editing) states else states.filter { it.on }
        ActionTable(
            shown, next, editing, plays, ::tap,
            onRemove = { viewModel.removeAction(phase.id, it) },
            onAdd = onAddAction,
        )
        if (phase.mode == com.adrian.minimallauncher.data.routine.PhaseMode.ORDERED && !editing) {
            resting?.let { s ->
                RestRing(s.action.minutes.coerceAtLeast(1)) {
                    viewModel.markAction(s.action.id)
                    onMarked()
                    plays[s.action.id] = (plays[s.action.id] ?: 0) + 1
                    resting = null
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.TriggerEditor(phase: RoutinePhase, places: List<Place>, viewModel: LauncherViewModel) {
    val context = LocalContext.current
    val byTime = phase.trigger is Trigger.Between
    val fallback = phase.window ?: Trigger.Between(LocalTime.of(20, 0), LocalTime.of(21, 0))
    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip("HORA", byTime) { if (!byTime) viewModel.setPhaseTrigger(phase.id, fallback) }
        Chip("LUGAR", !byTime) { if (byTime) viewModel.setPhaseTrigger(phase.id, Trigger.Arrive(places.firstOrNull()?.id ?: "casa"), phase.window) }
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
                    Chip(place.name.uppercase(spanish), place.id == placeId) {
                        viewModel.setPhaseTrigger(phase.id, if (leave) Trigger.Leave(place.id) else Trigger.Arrive(place.id), phase.window)
                    }
                }
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("LLEGAR", !leave) { viewModel.setPhaseTrigger(phase.id, Trigger.Arrive(placeId), phase.window) }
                Chip("SALIR", leave) { viewModel.setPhaseTrigger(phase.id, Trigger.Leave(placeId), phase.window) }
            }
        }
    }
}

/**
 * The actions as a table: icon, name, then the hour (red when it is next) and HECHO or SIGUE. Tapping a row marks it.
 * While editing, each row says ACTIVA or APAGADA (tap toggles) and QUITAR removes it; AGREGAR ACCIÓN adds one.
 */
@Composable
private fun ActionTable(
    states: List<ActionState>,
    next: ActionState?,
    editing: Boolean,
    plays: Map<String, Int>,
    onTap: (ActionState) -> Unit,
    onRemove: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        states.forEach { s ->
            val lit = if (editing) s.on else s.done
            val isNext = next?.action?.id == s.action.id
            Row(
                Modifier.fillMaxWidth().clickable(role = Role.Button) { onTap(s) }.padding(vertical = 12.dp)
                    .semantics { contentDescription = s.action.kind.label },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                StrokeIcon(s.action.kind.icon, if (lit) StrokeState.NOW else StrokeState.OFF, 28.dp, playKey = plays[s.action.id])
                Text(
                    s.action.kind.label.uppercase(spanish),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        lit -> White
                        isNext -> LauncherAccent
                        else -> Muted
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (editing) {
                    Text(if (s.on) "ACTIVA" else "APAGADA", style = MaterialTheme.typography.labelSmall, color = if (s.on) White else Muted)
                    TextAction("QUITAR", Muted) { onRemove(s.action.id) }
                } else {
                    s.action.at?.let { at ->
                        Text(
                            at.format(hhmm), style = MaterialTheme.typography.labelLarge,
                            color = when {
                                s.done -> White
                                isNext -> LauncherAccent
                                else -> Muted
                            },
                        )
                    }
                    when {
                        s.done -> Text("HECHO", style = MaterialTheme.typography.labelSmall, color = White)
                        isNext -> Text("SIGUE", style = MaterialTheme.typography.labelSmall, color = LauncherAccent)
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Rule))
        }
        if (editing) Box(
            Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onAdd).padding(vertical = 14.dp),
        ) { Text("+ AGREGAR ACCIÓN", style = MaterialTheme.typography.labelSmall, color = White) }
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
                if (fraction < 0.25f) LauncherAccent else White, -90f, 360f * fraction, false,
                topLeft = Offset(w, w), size = androidx.compose.ui.geometry.Size(size.width - 2 * w, size.height - 2 * w),
                style = Stroke(w, cap = StrokeCap.Round),
            )
        }
    }
}

/** The list of actions to add, as a table of names. Actions already in the phase show white. */
@Composable
private fun Catalog(have: Set<String>, onPick: (String) -> Unit, onClose: () -> Unit, modifier: Modifier) {
    Column(
        modifier.fillMaxWidth().background(Color.Black).border(1.dp, Line, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("AGREGAR ACCIÓN", style = MaterialTheme.typography.labelLarge, color = LauncherAccent)
            Spacer(Modifier.weight(1f))
            TextAction("CERRAR", Muted, onClose)
        }
        val types = com.adrian.minimallauncher.data.routine.ActionCatalog.all
        Column(Modifier.padding(top = 8.dp)) {
            types.forEach { type ->
                val on = type.key in have
                Row(
                    Modifier.fillMaxWidth().clickable(role = Role.Button) { onPick(type.key) }.padding(vertical = 12.dp)
                        .semantics { contentDescription = type.label },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    StrokeIcon(type.icon, if (on) StrokeState.DONE else StrokeState.OFF, 24.dp)
                    Text(type.label.uppercase(spanish), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = if (on) White else Muted)
                    Text(if (on) "YA ESTÁ" else "AGREGAR", style = MaterialTheme.typography.labelSmall, color = if (on) Muted else LauncherAccent)
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Rule))
            }
        }
    }
}

/** A plain text button: the words are the control. */
@Composable
private fun TextAction(label: String, color: Color, onClick: () -> Unit) {
    Text(
        label,
        Modifier.clickable(role = Role.Button, onClick = onClick).padding(horizontal = 4.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelLarge,
        color = color,
    )
}

@Composable
private fun Chip(label: String, on: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
            .background(if (on) Color(0xFF1C1C1C) else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (on) White else Muted)
    }
}
