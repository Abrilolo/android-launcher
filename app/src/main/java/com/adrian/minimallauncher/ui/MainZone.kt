package com.adrian.minimallauncher.ui
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
        EditableWidgetRow(widgetPlacement, onWidgetPlacementChange, Modifier.weight(1f)) { columnModifier ->
            SectionsColumn(apps, isAllowed, onOpenApp, onRestrictedApp, personalRecords, personalActions, hiddenSections, spaceName, columnModifier)
        }
    }
}
private const val VISIBLE_ROWS = 5
private const val VISIBLE_ROWS_ALL = 8
@Composable
private fun SectionsColumn(
    apps: List<LauncherApp>,
    isAllowed: (LauncherApp) -> Boolean,
    onOpenApp: (String) -> Unit,
    onRestrictedApp: (LauncherApp) -> Unit,
    personalRecords: PersonalRecords,
    personalActions: PersonalRecordActions,
    hiddenSections: Set<String>,
    spaceName: String,
    modifier: Modifier,
) {
    var appsExpanded by rememberSaveable { mutableStateOf(true) }
    var entertainmentExpanded by rememberSaveable { mutableStateOf(false) }
    var gamesExpanded by rememberSaveable { mutableStateOf(false) }
    var allAppsExpanded by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    // Only apps allowed in the current space are listed; anything else is reachable only by searching.
    val bySection = apps.filter(isAllowed).groupBy { it.section }
    val searchResults = if (query.isBlank()) emptyList()
        else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }.sortedBy { it.label.lowercase() }
    val open: (LauncherApp) -> Unit = { app -> if (isAllowed(app)) onOpenApp(app.packageName) else onRestrictedApp(app) }
    val dimmed = MaterialTheme.colorScheme.onSurfaceVariant
    val normal = MaterialTheme.colorScheme.onSurface
    val sections = listOf(
        Triple("Apps", "apps", AppSection.ESSENTIAL),
        Triple("Entretenimiento", "entertainment", AppSection.ENTERTAINMENT),
        Triple("Juegos", "games", AppSection.GAMES),
    )
    val expanded = mapOf(
        "apps" to (appsExpanded to { appsExpanded = !appsExpanded }),
        "entertainment" to (entertainmentExpanded to { entertainmentExpanded = !entertainmentExpanded }),
        "games" to (gamesExpanded to { gamesExpanded = !gamesExpanded }),
    )
    val listState = rememberLazyListState()
    var searchFocused by remember { mutableStateOf(false) }
    // The keyboard shrinks the list; keep the search field and its results above it.
    LaunchedEffect(searchFocused, query) {
        if (!searchFocused) return@LaunchedEffect
        delay(250) // Let the keyboard finish resizing the list first.
        listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
    }
    LazyColumn(modifier, listState, contentPadding = PaddingValues(bottom = 32.dp)) {
        item { PersonalSections(personalRecords, personalActions, spaceName, hiddenSections) }
        sections.forEach { (title, id, section) ->
            val sectionApps = bySection[section].orEmpty()
            if (id !in hiddenSections && sectionApps.isNotEmpty()) item(key = id) {
                val (isExpanded, toggle) = expanded.getValue(id)
                ExpandableSection(title, isExpanded, toggle) { AppList(sectionApps, VISIBLE_ROWS, open, { normal }) }
            }
        }
        if ("all" !in hiddenSections) item(key = "all") {
            ExpandableSection("Todas", allAppsExpanded, { allAppsExpanded = !allAppsExpanded }) {
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(bottom = 8.dp).onFocusChanged { searchFocused = it.isFocused }, label = { Text("Buscar") }, singleLine = true)
                if (query.isBlank()) {
                    AppList(bySection[AppSection.OTHER].orEmpty(), VISIBLE_ROWS_ALL, open, { dimmed }, alphabetIndex = true)
                } else {
                    AppList(searchResults, VISIBLE_ROWS_ALL, open, { app -> if (isAllowed(app)) normal else dimmed }, emptyText = "Sin resultados")
                }
            }
        }
    }
}
