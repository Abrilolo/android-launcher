package com.adrian.minimallauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.AppGroup
import com.adrian.minimallauncher.data.Space
import java.time.LocalTime

private data class EditableSpace(val source: Space, val start: String, val end: String, val groups: Set<AppGroup>)

@Composable
fun SettingsScreen(
    spaces: List<Space>, hiddenSections: Set<String>, sectionOrder: List<String>, pauseSeconds: Int,
    onSaveSpaces: (List<Space>) -> Unit, onSectionVisibility: (String, Boolean) -> Unit, onMoveSection: (String, Int) -> Unit,
    onPauseSeconds: (Int) -> Unit, onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    var drafts by remember(spaces) { mutableStateOf(spaces.map { EditableSpace(it, it.startsAt.toString(), it.endsAt.toString(), it.availableGroups) }) }
    var pauseText by remember(pauseSeconds) { mutableStateOf(pauseSeconds.toString()) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 24.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("CONFIGURACIÓN", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("LISTO") }
        }
        Text("HORARIOS Y APPS", Modifier.padding(top = 24.dp), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        drafts.forEachIndexed { index, draft ->
            Text(draft.source.name.uppercase(), Modifier.padding(top = 16.dp), style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(draft.start, { value -> drafts = drafts.toMutableList().also { it[index] = draft.copy(start = value.take(5)) } }, Modifier.weight(1f), label = { Text("Desde") }, singleLine = true)
                OutlinedTextField(draft.end, { value -> drafts = drafts.toMutableList().also { it[index] = draft.copy(end = value.take(5)) } }, Modifier.weight(1f), label = { Text("Hasta") }, singleLine = true)
            }
            AppGroup.entries.forEach { group ->
                val checked = group in draft.groups
                TextButton(onClick = {
                    val updated = if (checked) draft.groups - group else draft.groups + group
                    drafts = drafts.toMutableList().also { it[index] = draft.copy(groups = updated) }
                }) { Text("${if (checked) "●" else "○"} ${group.label()}") }
            }
        }
        TextButton(onClick = {
            val parsed = drafts.mapNotNull { draft -> runCatching {
                draft.source.copy(startsAt = LocalTime.parse(draft.start), endsAt = LocalTime.parse(draft.end), availableGroups = draft.groups)
            }.getOrNull() }
            if (parsed.size == drafts.size && (0 until 1_440).all { minute ->
                    parsed.count { it.contains(LocalTime.of(minute / 60, minute % 60)) } == 1
                }) onSaveSpaces(parsed)
        }, modifier = Modifier.padding(top = 8.dp)) { Text("GUARDAR ESPACIOS") }
        Text("PAUSA PARA APPS", Modifier.padding(top = 24.dp), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(pauseText, { pauseText = it.filter(Char::isDigit).take(3) }, label = { Text("Segundos (0–300)") }, singleLine = true)
        TextButton(onClick = { pauseText.toIntOrNull()?.takeIf { it in 0..300 }?.let(onPauseSeconds) }) { Text("GUARDAR PAUSA") }
        Text("APARTADOS", Modifier.padding(top = 24.dp), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        sectionOrder.forEachIndexed { index, id ->
            val visible = id !in hiddenSections
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(SECTION_TITLES[id] ?: id, Modifier.weight(1f))
                TextButton(onClick = { onMoveSection(id, -1) }, enabled = index > 0) { Text("↑") }
                TextButton(onClick = { onMoveSection(id, 1) }, enabled = index < sectionOrder.lastIndex) { Text("↓") }
                TextButton(onClick = { onSectionVisibility(id, !visible) }) { Text(if (visible) "VISIBLE" else "OCULTO") }
            }
        }
    }
}

private fun AppGroup.label(): String = when (this) {
    AppGroup.ESSENTIAL -> "Esenciales"
    AppGroup.OTHER -> "Otras apps"
    AppGroup.ENTERTAINMENT -> "Entretenimiento y juegos"
}
