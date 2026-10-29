package com.adrian.minimallauncher.ui

import android.content.Intent
import android.provider.AlarmClock
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.PersonalRecordActions
import com.adrian.minimallauncher.data.ModuleSettings
import com.adrian.minimallauncher.data.PersonalRecords
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.feature.ModuleCard
import com.adrian.minimallauncher.feature.ModuleHeader
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.feature.StatusDot
import com.adrian.minimallauncher.ui.components.DotMatrixText
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.adrian.minimallauncher.data.ReminderRecord
import com.adrian.minimallauncher.data.routine.RoutinePhase
import com.adrian.minimallauncher.data.routine.Trigger
import com.adrian.minimallauncher.feature.ModuleTextField
import com.adrian.minimallauncher.feature.SettingsGear
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
import com.adrian.minimallauncher.ui.components.rememberReactions
import com.adrian.minimallauncher.viewmodel.LauncherViewModel
import java.time.LocalTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val hhmm = DateTimeFormatter.ofPattern("HH:mm")

/** Shared frame for the pages a home tile opens: back label, then scrolling content. */
@Composable
fun ModulePage(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        ModuleHeader(title, onBack, Modifier.padding(top = 8.dp))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

/**
 * Recordatorios (concept: docs/design/concept/NotasRecordatoriosVisual.html). The next clock alarm on top (tap opens
 * the clock app), the routine phases with one mark per reminder, and the selected phase's reminders as a bell
 * with its hour. A reminder is marked done for today only during its phase. The pencil shows each title and lets
 * the user change the hour, move it to another phase, delete it, or add one.
 */
@Composable
fun AlarmsPage(viewModel: LauncherViewModel, onSettings: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val phases by viewModel.phases.collectAsState()
    val current by viewModel.currentPhase.collectAsState()
    val reminders by viewModel.reminders.collectAsState()
    val doneToday by viewModel.remindersDoneToday.collectAsState()
    val settings by viewModel.moduleSettings.collectAsState()
    val reactions = rememberReactions(settings.haptics)
    val next = nextSystemAlarm()

    var selectedId by remember { mutableStateOf(current.id) }
    var editing by remember { mutableStateOf(false) }
    var plays by remember { mutableStateOf(mapOf<String, Int>()) }
    var clockPlay by remember { mutableIntStateOf(0) }
    val selected = phases.firstOrNull { it.id == selectedId } ?: current
    val currentIndex = phases.indexOfFirst { it.id == current.id }
    fun of(phaseId: String) = reminders.filter { it.phaseId == phaseId }.sortedBy { it.hour * 60 + it.minute }
    val nextId = of(current.id).firstOrNull { it.id !in doneToday }?.id

    BackHandler { if (editing) editing = false else onBack() }

    Column(
        modifier.fillMaxSize().background(Color.Black).safeDrawingPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Alarms belong to the phone's clock app; the launcher shows the next one and opens the app to edit.
            Row(
                Modifier.weight(1f).clickable(role = Role.Button) {
                    clockPlay++
                    runCatching { context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }.semantics { contentDescription = "Siguiente alarma del reloj; abre el reloj" },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StrokeIcon("reloj", if (next != null) StrokeState.DONE else StrokeState.OFF, 28.dp, playKey = clockPlay)
                DotMatrixText(next?.format(hhmm) ?: "--:--", cell = 4.dp)
            }
            SettingsGear(onSettings)
        }

        Row(Modifier.fillMaxWidth()) {
            phases.forEachIndexed { i, phase ->
                val state = when {
                    i < currentIndex -> StrokeState.DONE
                    i == currentIndex -> StrokeState.NOW
                    else -> StrokeState.OFF
                }
                Column(
                    Modifier.weight(1f).clickable(role = Role.Tab) { selectedId = phase.id }.semantics { contentDescription = phase.name },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StrokeIcon(phaseIcon(phase), state, 40.dp)
                    // One mark per reminder: white when done today, red the next one, gray the rest.
                    Row(Modifier.height(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        of(phase.id).forEach { r ->
                            val color = when {
                                r.id in doneToday -> Color(0xFFF5F5F5)
                                r.id == nextId -> LauncherRed
                                else -> Color(0xFF5C5C5C)
                            }
                            Box(Modifier.size(4.dp).background(color, CircleShape))
                        }
                    }
                    Box(Modifier.size(4.dp).background(if (phase.id == selected.id) Color(0xFFF5F5F5) else Color.Transparent, CircleShape))
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().background(Color(0xFF0B0B0B), RoundedCornerShape(24.dp))
                .border(1.dp, Color(0xFF222222), RoundedCornerShape(24.dp)).padding(18.dp),
        ) {
            val live = selected.id == current.id
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (editing) SmallLabel(selected.name.uppercase())
                    else {
                        val index = phases.indexOf(selected)
                        StrokeIcon(phaseIcon(selected), if (index < currentIndex) StrokeState.DONE else if (live) StrokeState.NOW else StrokeState.OFF, 32.dp)
                        val start = (selected.trigger as? Trigger.Between)?.start ?: selected.window?.start
                        if (start != null) SmallLabel(start.format(hhmm), color = if (live) Color(0xFFF5F5F5) else Color(0xFFB0B0B0))
                    }
                }
                Box(
                    Modifier.size(36.dp).clip(CircleShape).clickable(role = Role.Button) { editing = !editing }
                        .semantics { contentDescription = if (editing) "Listo" else "Editar recordatorios" },
                    contentAlignment = Alignment.Center,
                ) { StrokeIcon(if (editing) "listo" else "lapiz", StrokeState.DONE, 20.dp, color = if (editing) LauncherRed else Color(0xFFB0B0B0)) }
            }
            Box(Modifier.fillMaxWidth().padding(top = 16.dp).height(1.dp).background(Color(0xFF222222)))

            if (editing) {
                of(selected.id).forEach { r -> ReminderEditor(r, phases, viewModel) }
                Box(
                    Modifier.padding(top = 16.dp).size(48.dp).clip(RoundedCornerShape(14.dp))
                        .border(1.5.dp, Color(0xFF3A3A3A), RoundedCornerShape(14.dp))
                        .clickable(role = Role.Button) {
                            val at = LocalTime.now().plusHours(1)
                            viewModel.addReminder("", at.hour, 0, selected.id)
                        }.semantics { contentDescription = "Agregar recordatorio" },
                    contentAlignment = Alignment.Center,
                ) { Text("+", style = MaterialTheme.typography.titleMedium, color = Color(0xFF5C5C5C)) }
            } else {
                // Bells in rows of three, like the routine's actions.
                of(selected.id).chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(top = 18.dp)) {
                        row.forEach { r ->
                            val done = r.id in doneToday
                            val state = when {
                                done -> StrokeState.DONE
                                r.id == nextId -> StrokeState.NOW
                                else -> StrokeState.OFF
                            }
                            Column(
                                Modifier.weight(1f).clickable(role = Role.Checkbox) {
                                    // Only during its phase: it rings and is marked then.
                                    if (!live) { reactions.reject(); return@clickable }
                                    if (!done) { reactions.done(); plays = plays + (r.id to (plays[r.id] ?: 0) + 1) } else reactions.tick()
                                    viewModel.toggleReminderDone(r.id)
                                }.semantics { contentDescription = r.title.ifBlank { "Recordatorio" } },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                StrokeIcon("campana", state, 44.dp, playKey = plays[r.id])
                                SmallLabel(
                                    LocalTime.of(r.hour, r.minute).format(hhmm),
                                    color = when {
                                        done -> Color(0xFFF5F5F5)
                                        r.id == nextId -> LauncherRed
                                        else -> Color(0xFFB0B0B0)
                                    },
                                )
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

/** Edit mode row: the title (saved on done or when the row leaves), the hour, the phase it belongs to, delete. */
@Composable
private fun ReminderEditor(r: ReminderRecord, phases: List<RoutinePhase>, viewModel: LauncherViewModel) {
    val context = LocalContext.current
    var title by remember(r.id) { mutableStateOf(r.title) }
    val save by rememberUpdatedState { if (title.trim() != r.title) viewModel.updateReminder(r.id, title.trim(), r.hour, r.minute) }
    DisposableEffect(r.id) { onDispose { save() } }
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ModuleTextField(
                title, { title = it }, "Recordatorio", Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { save() }),
            )
            Text(
                LocalTime.of(r.hour, r.minute).format(hhmm),
                Modifier.clip(RoundedCornerShape(8.dp)).border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button) {
                        TimePickerDialog(context, { _, h, m -> viewModel.updateReminder(r.id, title.trim(), h, m) }, r.hour, r.minute, true).show()
                    }
                    .semantics { contentDescription = "Cambiar hora" }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFB0B0B0),
            )
            Box(
                Modifier.size(32.dp).clip(CircleShape).clickable(role = Role.Button) { viewModel.deleteReminder(r.id) }
                    .semantics { contentDescription = "Borrar recordatorio" },
                contentAlignment = Alignment.Center,
            ) { StrokeIcon("papelera", StrokeState.DONE, 18.dp, color = Color(0xFFB0B0B0)) }
        }
        // Tap another phase's icon to move the reminder there.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            phases.forEach { p ->
                Box(
                    Modifier.size(28.dp).clip(CircleShape).clickable(role = Role.RadioButton) { if (p.id != r.phaseId) viewModel.moveReminder(r.id, p.id) }
                        .semantics { contentDescription = "Mover a ${p.name}" },
                    contentAlignment = Alignment.Center,
                ) { StrokeIcon(phaseIcon(p), if (p.id == r.phaseId) StrokeState.DONE else StrokeState.OFF, 20.dp) }
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF161616)))
}

@Composable
fun WorldClockPage(now: LocalDateTime, zones: List<String>, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val here = ZonedDateTime.now()
    ModulePage("MUNDO", onBack, modifier) {
        SmallLabel("AQUÍ")
        DotMatrixText(now.format(hhmm), cell = 9.dp)
        remoteCities(zones).forEach { city ->
            val time = ZonedDateTime.now(city.zone)
            val diff = (time.offset.totalSeconds - here.offset.totalSeconds) / 3600
            val day = when (time.toLocalDate().compareTo(here.toLocalDate())) {
                0 -> "HOY"
                in 1..Int.MAX_VALUE -> "MAÑANA"
                else -> "AYER"
            }
            ModuleCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(city.name, style = MaterialTheme.typography.bodyLarge)
                        SmallLabel("${if (diff >= 0) "+" else ""}$diff H · $day")
                    }
                    DotMatrixText(time.format(hhmm), cell = 4.dp)
                }
                // One bar per hour of the city's day: white while it is daytime there, red for the current hour.
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(24) { hour ->
                        val color = when {
                            hour == time.hour -> LauncherRed
                            hour in 7..19 -> Color(0xFFCFCFCF)
                            else -> Color(0xFF262626)
                        }
                        Box(Modifier.weight(1f).height(5.dp).background(color, RoundedCornerShape(3.dp)))
                    }
                }
            }
        }
    }
}

@Composable
private fun OutlineButton(label: String, onClick: () -> Unit) {
    Text(
        label,
        Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF333333), RoundedCornerShape(26.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 16.dp),
        style = MaterialTheme.typography.labelSmall,
        textAlign = TextAlign.Center,
    )
}
