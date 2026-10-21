package com.adrian.minimallauncher.feature.notes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adrian.minimallauncher.data.NoteCategory
import com.adrian.minimallauncher.data.NoteRecord
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.feature.ModuleHeader
import com.adrian.minimallauncher.feature.ModuleTextField
import com.adrian.minimallauncher.feature.SmallLabel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val spanish = Locale.forLanguageTag("es")

/** "HOY", "AYER", the weekday within a week, or the date. */
fun NoteRecord.whenLabel(today: LocalDate = LocalDate.now()): String {
    val date = Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
    return when {
        date == today -> "HOY"
        date == today.minusDays(1) -> "AYER"
        date.isAfter(today.minusDays(7)) -> date.format(DateTimeFormatter.ofPattern("EEE", spanish)).uppercase(spanish).trimEnd('.')
        else -> date.format(DateTimeFormatter.ofPattern("d MMM", spanish)).uppercase(spanish).trimEnd('.')
    }
}

/**
 * Pinned note on a white card, tag filter, the list, and a field that saves on its own:
 * with the keyboard's done key or when leaving the page. New notes take the selected tag.
 */
@Composable
fun NotesScreen(
    notes: List<NoteRecord>,
    onAdd: (NoteCategory, String) -> Unit,
    onDelete: (String) -> Unit,
    onSetPinned: (String, Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var filter by rememberSaveable { mutableStateOf<NoteCategory?>(null) }
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    // Not saveable on purpose: if the page is recreated, onDispose has already saved the draft.
    var draft by remember { mutableStateOf("") }
    val pinned = notes.firstOrNull { it.pinned }
    val shown = notes.filter { !it.pinned && (filter == null || it.category == filter) }
    val newCategory = filter ?: NoteCategory.GENERAL

    val saveDraft = {
        if (draft.isNotBlank()) onAdd(newCategory, draft.trim())
        draft = ""
    }
    // Leaving the page keeps what was typed instead of dropping it.
    val latestSave by rememberUpdatedState(saveDraft)
    DisposableEffect(Unit) { onDispose { latestSave() } }

    Column(modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        ModuleHeader("NOTAS", onBack, Modifier.padding(top = 8.dp))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (pinned != null) PinnedCard(pinned) { onSetPinned(pinned.id, false) }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TagChip("TODAS", filter == null) { filter = null }
                NoteCategory.entries.forEach { category ->
                    TagChip(category.label.uppercase(spanish), filter == category) { filter = category }
                }
            }
            if (shown.isEmpty()) {
                SmallLabel(if (notes.isEmpty()) "SIN NOTAS TODAVÍA" else "SIN NOTAS EN ESTA ETIQUETA")
            } else {
                Column {
                    shown.forEach { note ->
                        NoteRow(
                            note,
                            isOpen = openId == note.id,
                            onToggle = { openId = if (openId == note.id) null else note.id },
                            onPin = { onSetPinned(note.id, true); openId = null },
                            onDelete = { onDelete(note.id); openId = null },
                        )
                    }
                }
            }
        }
        ModuleTextField(
            draft, { draft = it }, "Nueva nota · ${newCategory.label.lowercase(spanish)}",
            Modifier.fillMaxWidth().padding(bottom = 16.dp),
            minLines = 2,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { saveDraft() }),
        )
    }
}

@Composable
private fun PinnedCard(note: NoteRecord, onUnpin: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(26.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            SmallLabel("FIJADA", Modifier.weight(1f), color = Color.Black)
            SmallLabel("${note.category.label.uppercase(spanish)} · ${note.whenLabel()}", color = Color(0xFF555555))
        }
        Text(note.text, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp), color = Color.Black)
        SmallLabel("QUITAR", Modifier.clickable(role = Role.Button, onClick = onUnpin).padding(vertical = 4.dp), color = Color(0xFF555555))
    }
}

@Composable
private fun TagChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        Modifier
            .background(if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent, shape)
            .border(1.dp, if (selected) MaterialTheme.colorScheme.onSurface else Color(0xFF333333), shape)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        SmallLabel(label, color = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface)
    }
}

/** Tapping a note shows its actions: pin it to the top or delete it. */
@Composable
private fun NoteRow(note: NoteRecord, isOpen: Boolean, onToggle: () -> Unit, onPin: () -> Unit, onDelete: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onToggle).padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SmallLabel("${note.category.label.uppercase(spanish)} · ${note.whenLabel()}")
        Text(note.text, style = MaterialTheme.typography.bodyLarge)
        AnimatedVisibility(isOpen) {
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                SmallLabel("FIJAR", Modifier.clickable(role = Role.Button, onClick = onPin).padding(vertical = 6.dp), color = MaterialTheme.colorScheme.onSurface)
                SmallLabel("BORRAR", Modifier.clickable(role = Role.Button, onClick = onDelete).padding(vertical = 6.dp), color = LauncherRed)
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF1A1A1A)))
}
