package com.adrian.minimallauncher.ui

import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.ui.components.DotIcons
import com.adrian.minimallauncher.ui.components.DotIcon
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adrian.minimallauncher.data.AppGroup
import com.adrian.minimallauncher.data.AppSection
import com.adrian.minimallauncher.data.LauncherApp
import com.adrian.minimallauncher.data.Space
import com.adrian.minimallauncher.feature.ModuleChip
import com.adrian.minimallauncher.feature.ModuleTextField
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.ui.components.AppList
import com.adrian.minimallauncher.ui.components.appItemHeight
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val spanish = Locale.forLanguageTag("es")
private val hhmm = DateTimeFormatter.ofPattern("HH:mm")

val SECTION_TITLES = mapOf(
    "apps" to "Apps",
    "entertainment" to "Entretenimiento",
    "games" to "Juegos",
    "all" to "Todas",
)

private val APP_SECTIONS = mapOf(
    "apps" to AppSection.ESSENTIAL,
    "entertainment" to AppSection.ENTERTAINMENT,
    "games" to AppSection.GAMES,
)

/**
 * The main home page: its widgets on top (the big clock by default), then every app as text,
 * as in docs/design/concept/Apps.dc.html. Tabs show only the sections the current space allows;
 * "Todas" has the search, which covers all apps (restricted ones dimmed, they go through the pause),
 * and the letter index.
 */
@Composable
fun AppsPage(
    apps: List<LauncherApp>,
    isAllowed: (LauncherApp) -> Boolean,
    onOpenApp: (String) -> Unit,
    onRestrictedApp: (LauncherApp) -> Unit,
    hiddenSections: Set<String>,
    sectionOrder: List<String>,
    space: Space,
    spaces: List<Space>,
    now: LocalDateTime,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    /** The page's own widgets (the big clock by default), drawn above the list. */
    widgets: @Composable () -> Unit = {},
    /** Whether the widgets include the clock; if not, the header keeps showing the time. */
    hasClock: Boolean = false,
    editMode: Boolean = false,
    onExitEdit: () -> Unit = {},
) {
    var tab by rememberSaveable { mutableStateOf("apps") }
    var query by rememberSaveable { mutableStateOf("") }

    val bySection = apps.filter(isAllowed).groupBy { it.section }
    val tabs = sectionOrder.filter { id ->
        id !in hiddenSections && APP_SECTIONS[id].let { section -> section == null || bySection[section].orEmpty().isNotEmpty() }
    }
    val current = tab.takeIf { it in tabs } ?: tabs.firstOrNull() ?: "all"
    // The search box only lives in "Todas"; elsewhere the tabs are enough.
    val searching = current == "all" && query.isNotBlank()
    val open: (LauncherApp) -> Unit = { app -> if (isAllowed(app)) onOpenApp(app.packageName) else onRestrictedApp(app) }
    val normal = MaterialTheme.colorScheme.onSurface
    val dimmed = MaterialTheme.colorScheme.onSurfaceVariant

    val shown = when {
        // Bank apps never show up here; they only open from the Bancos widget.
        searching -> apps.filter { it.label.contains(query.trim(), ignoreCase = true) && !it.isVault }.sortedBy { it.label.lowercase() }
        current == "all" -> bySection[AppSection.OTHER].orEmpty()
        else -> bySection[APP_SECTIONS.getValue(current)].orEmpty()
    }

    Column(modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            SmallLabel(if (hasClock) space.name.uppercase(spanish) else "${now.format(hhmm)} · ${space.name.uppercase(spanish)}", Modifier.weight(1f))
            // U+FE0E keeps the gear as a monochrome text glyph instead of a color emoji.
            IconButton(onClick = onSettings) { Text("⚙︎", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        widgets()
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(
                Modifier.fillMaxSize().graphicsLayer { alpha = if (editMode) 0.35f else 1f },
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tabs.forEach { id ->
                        ModuleChip(SECTION_TITLES.getValue(id).uppercase(spanish), id == current) {
                            tab = id
                            if (id != "all") query = ""
                        }
                    }
                }
                if (current == "all") {
                    ModuleTextField(
                        query, { query = it }, "Buscar en todas", Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    )
                }
                // As many rows as fit, so the list fills the page and scrolls inside it.
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val rows = (maxHeight / appItemHeight()).toInt().coerceAtLeast(3)
                    AppList(
                        shown, rows, open,
                        colorFor = { app -> if (isAllowed(app)) normal else dimmed },
                        lockedFor = { app -> !isAllowed(app) },
                        alphabetIndex = !searching && current == "all",
                        emptyText = if (searching) "Sin resultados" else "Nada disponible en este espacio",
                    )
                }
                val backAt = if (searching) null else leisureBackAt(space, spaces, now)
                if (searching && shown.any { !isAllowed(it) }) {
                    SmallLabel("FUERA DE HORARIO = PAUSA ANTES DE ABRIR", Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp))
                } else if (backAt != null) {
                    // An hourglass and the hour in red: when leisure and games come back.
                    Row(
                        Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp)
                            .semantics(mergeDescendants = true) { contentDescription = "Ocio y juegos vuelven a las ${backAt.format(hhmm)}" },
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DotIcon(DotIcons.relojArena, done = true, contentDescription = null, size = 22.dp)
                        SmallLabel("OCIO")
                        SmallLabel(backAt.format(hhmm), color = LauncherRed)
                    }
                }
            }
            // While editing only the widgets respond; a tap on the list leaves edit mode.
            if (editMode) Box(Modifier.matchParentSize().pointerInput(Unit) { detectTapGestures { onExitEdit() } })
        }
    }
}

/** When entertainment opens again, or null if the current space already allows it. */
private fun leisureBackAt(space: Space, spaces: List<Space>, now: LocalDateTime): LocalTime? {
    if (AppGroup.ENTERTAINMENT in space.availableGroups) return null
    val time = now.toLocalTime()
    return spaces.filter { AppGroup.ENTERTAINMENT in it.availableGroups }
        .map { it.startsAt }
        .minByOrNull { start -> Duration.between(time, start).let { if (it.isNegative) it.plusDays(1) else it } }
}
