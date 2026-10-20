package com.adrian.minimallauncher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import com.adrian.minimallauncher.data.AppSection
import com.adrian.minimallauncher.data.LauncherApp
import com.adrian.minimallauncher.data.WidgetPlacement
import com.adrian.minimallauncher.data.PersonalRecords
import com.adrian.minimallauncher.data.PersonalRecordActions
import com.adrian.minimallauncher.ui.components.AppList
import com.adrian.minimallauncher.ui.components.ClockHeader
import com.adrian.minimallauncher.ui.components.ExpandableSection
import com.adrian.minimallauncher.ui.widgets.EditableWidgetRow
import com.adrian.minimallauncher.ui.widgets.HomeWidgets

@Composable
fun MainZone(
    apps: List<LauncherApp>,
    isAllowed: (LauncherApp) -> Boolean,
    onOpenApp: (String) -> Unit,
    onRestrictedApp: (LauncherApp) -> Unit,
    widgetPlacement: WidgetPlacement,
    onWidgetPlacementChange: (WidgetPlacement) -> Unit,
    spaceName: String,
    now: LocalDateTime,
    personalRecords: PersonalRecords,
    personalActions: PersonalRecordActions,
    hiddenSections: Set<String>,
    sectionOrder: List<String>,
    homeWidgets: HomeWidgets,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(horizontal = 28.dp).padding(top = 32.dp, bottom = 16.dp)) {
        Box(Modifier.fillMaxWidth().padding(bottom = 40.dp)) {
            ClockHeader(spaceName, now)
            // U+FE0E keeps the gear as a monochrome text glyph instead of a color emoji.
            IconButton(onClick = onSettings, Modifier.align(Alignment.TopEnd).offset(x = 12.dp, y = (-12).dp)) {
                Text("⚙︎", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        EditableWidgetRow(widgetPlacement, onWidgetPlacementChange, personalRecords, homeWidgets, Modifier.weight(1f)) { columnModifier ->
            SectionsColumn(apps, isAllowed, onOpenApp, onRestrictedApp, personalRecords, personalActions, hiddenSections, sectionOrder, spaceName, columnModifier)
        }
    }
}

private const val VISIBLE_ROWS = 5
private const val VISIBLE_ROWS_ALL = 8

val SECTION_TITLES = mapOf(
    "reminders" to "Recordatorios",
    "notes" to "Notas",
    "finance" to "Finanzas",
    "diet" to "Dieta",
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

@Composable
private fun SectionsColumn(
    apps: List<LauncherApp>,
    isAllowed: (LauncherApp) -> Boolean,
    onOpenApp: (String) -> Unit,
    onRestrictedApp: (LauncherApp) -> Unit,
    personalRecords: PersonalRecords,
    personalActions: PersonalRecordActions,
    hiddenSections: Set<String>,
    sectionOrder: List<String>,
    spaceName: String,
    modifier: Modifier,
) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }

    val bySection = apps.filter(isAllowed).groupBy { it.section }
    val searchResults = if (query.isBlank()) emptyList()
    else apps.filter {
        // Bank apps never show up here; they only open from the Bancos widget.
        it.label.contains(query.trim(), ignoreCase = true) && !it.isVault
    }.sortedBy { it.label.lowercase() }

    val open: (LauncherApp) -> Unit = { app -> if (isAllowed(app)) onOpenApp(app.packageName) else onRestrictedApp(app) }
    val dimmed = MaterialTheme.colorScheme.onSurfaceVariant
    val normal = MaterialTheme.colorScheme.onSurface

    val visible = sectionOrder.filter { id ->
        id !in hiddenSections && APP_SECTIONS[id].let { section -> section == null || bySection[section].orEmpty().isNotEmpty() }
    }
    val shown = visible.dropWhile { it != selected }.ifEmpty { visible }
    val listState = rememberLazyListState()
    var searchFocused by remember { mutableStateOf(false) }

    LaunchedEffect(searchFocused, query) {
        if (!searchFocused) return@LaunchedEffect
        delay(250)
        shown.indexOf("all").takeIf { it >= 0 }?.let { listState.animateScrollToItem(it) }
    }

    LazyColumn(modifier, listState, contentPadding = PaddingValues(bottom = 32.dp)) {
        items(shown, key = { it }) { id ->
            val itemModifier = Modifier.animateItem(fadeInSpec = tween(300), placementSpec = tween(350), fadeOutSpec = tween(200))
            val isSelected = id == selected

            ExpandableSection(SECTION_TITLES[id] ?: id, isSelected, {
                selected = if (selected == id) null else id
                listState.requestScrollToItem(0)
            }, itemModifier) {
                when (id) {
                    in APP_SECTIONS -> AppList(bySection[APP_SECTIONS.getValue(id)].orEmpty(), VISIBLE_ROWS, open, { normal })
                    "all" -> {
                        OutlinedTextField(
                            query, { query = it },
                            Modifier.fillMaxWidth().padding(bottom = 8.dp).onFocusChanged { searchFocused = it.isFocused },
                            label = { Text("Buscar") },
                            singleLine = true,
                        )
                        if (query.isBlank()) {
                            AppList(
                                bySection[AppSection.OTHER].orEmpty(),
                                VISIBLE_ROWS_ALL, open, { dimmed }, alphabetIndex = true,
                            )
                        } else {
                            AppList(searchResults, VISIBLE_ROWS_ALL, open, { app -> if (isAllowed(app)) normal else dimmed }, emptyText = "Sin resultados")
                        }
                    }
                    else -> PersonalSectionBody(id, personalRecords, personalActions, spaceName)
                }
            }
        }
    }
}
