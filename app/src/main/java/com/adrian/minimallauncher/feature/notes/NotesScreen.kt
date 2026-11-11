package com.adrian.minimallauncher.feature.notes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adrian.minimallauncher.data.NoteCategory
import com.adrian.minimallauncher.data.NoteRecord
import com.adrian.minimallauncher.data.routine.WidgetSelector
import com.adrian.minimallauncher.feature.LauncherAccent
import com.adrian.minimallauncher.feature.ModuleTextField
import com.adrian.minimallauncher.feature.ModuleTitle
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
import com.adrian.minimallauncher.ui.components.rememberReactions
import com.adrian.minimallauncher.viewmodel.LauncherViewModel
import kotlinx.coroutines.delay
import java.util.Locale

private val spanish = Locale.forLanguageTag("es")
private val White = Color(0xFFF5F5F5)
private val Muted = Color(0xFFB0B0B0)
private val Gray = Color(0xFF5C5C5C)
private val Rule = Color(0xFF161616)

/** One column of the strip: an aspect, or (null category) every note outside the five aspects. */
private class Aspect(val category: NoteCategory?, val icon: String, val name: String) {
    fun holds(note: NoteRecord) = if (category == null) note.category !in NoteCategory.ASPECTS else note.category == category
}

private val aspectIcons = mapOf(
    NoteCategory.WORK to "trabajo", NoteCategory.SCHOOL to "escuela", NoteCategory.EXERCISE to "gimnasio",
    NoteCategory.FOOD to "plato", NoteCategory.DIET to "hoja",
)
private val aspects = NoteCategory.ASPECTS.map { Aspect(it, aspectIcons.getValue(it), it.label) }
private val others = Aspect(null, "pendientes", "Otras")

/**
 * Notas. The aspects are a row of icons with their names under them; the open one is underlined. Above each icon,
 * one dot per pending note; the current phase's aspect is red. The open aspect is a table of its notes (tap the
 * circle to mark it). EDITAR turns the table into FIJAR and BORRAR words per note.
 */
@Composable
fun NotesScreen(viewModel: LauncherViewModel, onSettings: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val notes by viewModel.notes.collectAsState()
    val phase by viewModel.currentPhase.collectAsState()
    val settings by viewModel.moduleSettings.collectAsState()
    val reactions = rememberReactions(settings.haptics)
    val focus = WidgetSelector.focusOf(phase.id)

    val columns = if (notes.any { others.holds(it) }) aspects + others else aspects
    var selectedKey by rememberSaveable { mutableStateOf((focus ?: NoteCategory.WORK).name) }
    val selected = columns.firstOrNull { (it.category?.name ?: "OTHER") == selectedKey } ?: columns.first()
    var editing by remember { mutableStateOf(false) }
    var plays by remember { mutableStateOf(mapOf<String, Int>()) }
    var iconPlays by remember { mutableStateOf(mapOf<String, Int>()) }
    // Dots shown in the first moments come in one after another; later ones just pop in.
    var opening by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(120); opening = false }

    // Not saveable on purpose: if the page is recreated, onDispose has already saved the draft.
    var draft by remember { mutableStateOf("") }
    val saveDraft = {
        if (draft.isNotBlank()) viewModel.addNote(selected.category ?: NoteCategory.GENERAL, draft.trim())
        draft = ""
    }
    val latestSave by rememberUpdatedState(saveDraft)
    DisposableEffect(Unit) { onDispose { latestSave() } }

    BackHandler { if (editing) editing = false else onBack() }

    Column(
        modifier.fillMaxSize().background(Color.Black).safeDrawingPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ModuleTitle("Notas", onSettings)

        Column {
            var offset = 0
            Row(Modifier.fillMaxWidth().heightIn(min = 80.dp), verticalAlignment = Alignment.Bottom) {
                columns.forEach { a ->
                    val pending = notes.count { a.holds(it) && !it.done }
                    PipStack(pending, if (a.category != null && a.category == focus) LauncherAccent else White, if (opening) offset else -1, Modifier.weight(1f))
                    offset += pending
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                columns.forEach { a ->
                    val key = a.category?.name ?: "OTHER"
                    val pending = notes.count { a.holds(it) && !it.done }
                    val state = when {
                        pending == 0 -> StrokeState.OFF
                        a.category != null && a.category == focus -> StrokeState.NOW
                        else -> StrokeState.DONE
                    }
                    val open = a === selected
                    Column(
                        Modifier.weight(1f).clickable(role = Role.Tab) {
                            selectedKey = key
                            iconPlays = iconPlays + (key to (iconPlays[key] ?: 0) + 1)
                        }.semantics { contentDescription = a.name },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        StrokeIcon(a.icon, state, 34.dp, playKey = iconPlays[key])
                        Text(
                            a.name.uppercase(spanish),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = if (open) White else Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                        Box(Modifier.width(20.dp).height(2.dp).background(if (open) White else Color.Transparent))
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(selected.name.uppercase(spanish), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = LauncherAccent)
                TextAction(if (editing) "LISTO" else "EDITAR", if (editing) LauncherAccent else Muted) { editing = !editing }
            }
            val shown = notes.filter { selected.holds(it) }
                .sortedWith(compareBy<NoteRecord> { it.done }.thenByDescending { it.pinned }.thenByDescending { it.createdAt })
            shown.forEach { note ->
                NoteLine(
                    note, editing, plays[note.id],
                    onToggle = {
                        if (!note.done) reactions.done() else reactions.tick()
                        if (!note.done) plays = plays + (note.id to (plays[note.id] ?: 0) + 1)
                        viewModel.setNoteDone(note.id, !note.done)
                    },
                    onPin = { viewModel.setNotePinned(note.id, !note.pinned) },
                    onDelete = { viewModel.deleteNote(note.id) },
                )
            }
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModuleTextField(
                    draft, { draft = it }, "Nueva nota en ${selected.name}", Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (draft.isNotBlank()) reactions.tick(); saveDraft() }),
                )
            }
        }
    }
}

/**
 * One dot per pending note, stacked up from the icon. With [stagger] >= 0 the dots come in one by one (that is
 * their order across the strip); a dot that goes away shrinks first.
 */
@Composable
private fun PipStack(count: Int, color: Color, stagger: Int, modifier: Modifier) {
    var shown by remember { mutableIntStateOf(count) }
    LaunchedEffect(count) {
        if (count < shown) delay(PIP_MS.toLong())
        shown = count
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        // Top of the column first: index (n-1) down to 0, so a new note adds a dot on top.
        for (i in maxOf(count, shown) - 1 downTo 0) {
            Pip(visible = i < count, color = color, delayMs = if (stagger >= 0) (stagger + i) * 55 else 0)
        }
    }
}

@Composable
private fun Pip(visible: Boolean, color: Color, delayMs: Int) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(delayMs.toLong()); started = true }
    val scale by animateFloatAsState(if (visible && started) 1f else 0f, tween(PIP_MS), label = "pip")
    Box(Modifier.size(6.dp).scale(scale).background(color, CircleShape))
}

/**
 * A note as a table row: its circle marks it done (the check draws once), the text. In edit mode the row says
 * FIJAR (or QUITAR FIJA) and BORRAR; a pinned note not in edit mode says FIJA.
 */
@Composable
private fun NoteLine(note: NoteRecord, editing: Boolean, play: Int?, onToggle: () -> Unit, onPin: () -> Unit, onDelete: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.clip(CircleShape).clickable(role = Role.Checkbox, onClick = onToggle)
                    .semantics { contentDescription = if (note.done) "Marcar pendiente" else "Marcar hecha" },
            ) {
                if (note.done) StrokeIcon("nota_hecha", StrokeState.OFF, 22.dp, playKey = play)
                else StrokeIcon("circulo", StrokeState.DONE, 22.dp, color = if (note.pinned) White else Muted)
            }
            Text(note.text, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = if (note.done) Gray else White)
            if (editing) {
                TextAction(if (note.pinned) "QUITAR FIJA" else "FIJAR", if (note.pinned) LauncherAccent else White, onPin)
                TextAction("BORRAR", Muted, onDelete)
            } else if (note.pinned) {
                Text("FIJA", style = MaterialTheme.typography.labelSmall, color = LauncherAccent)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Rule))
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

private const val PIP_MS = 280
