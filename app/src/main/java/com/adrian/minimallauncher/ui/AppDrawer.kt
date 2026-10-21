package com.adrian.minimallauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.AppSection
import com.adrian.minimallauncher.data.LauncherApp
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.ui.components.AppList
import com.adrian.minimallauncher.ui.components.ExpandableSection
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

private const val VISIBLE_ROWS = 8
private const val VISIBLE_ROWS_ALL = 8

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

/** What the apps icon on the home opens: the app sections, search and alphabet index. The handle or back closes it. */
@Composable
fun AppDrawer(
    apps: List<LauncherApp>,
    isAllowed: (LauncherApp) -> Boolean,
    onOpenApp: (String) -> Unit,
    onRestrictedApp: (LauncherApp) -> Unit,
    hiddenSections: Set<String>,
    sectionOrder: List<String>,
    spaceName: String,
    now: LocalDateTime,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onClose)
    Column(modifier.fillMaxSize().padding(horizontal = 28.dp)) {
        // Handle and time, centered; tapping it closes the drawer like back does.
        Column(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClose)
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.width(36.dp).height(4.dp).background(Color(0xFF444444), RoundedCornerShape(2.dp)))
            SmallLabel("${now.format(DateTimeFormatter.ofPattern("HH:mm"))} · ${spaceName.uppercase(Locale.forLanguageTag("es"))}")
        }
        SectionsColumn(
            apps, isAllowed,
            onOpenApp = { packageName -> onOpenApp(packageName); onClose() },
            onRestrictedApp = { app -> onRestrictedApp(app); onClose() },
            hiddenSections, sectionOrder, Modifier.weight(1f),
        )
    }
}

@Composable
private fun SectionsColumn(
    apps: List<LauncherApp>,
    isAllowed: (LauncherApp) -> Boolean,
    onOpenApp: (String) -> Unit,
    onRestrictedApp: (LauncherApp) -> Unit,
    hiddenSections: Set<String>,
    sectionOrder: List<String>,
    modifier: Modifier,
) {
    // Apps starts open: it is what the drawer is for most of the time.
    var selected by rememberSaveable { mutableStateOf<String?>("apps") }
    var query by rememberSaveable { mutableStateOf("") }

    val bySection = apps.filter(isAllowed).groupBy { it.section }
    val searchResults = if (query.isBlank()) emptyList()
    else apps.filter {
        // Bank apps never show up here; they only open from the Bancos tile.
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
            ExpandableSection(SECTION_TITLES[id] ?: id, id == selected, {
                selected = if (selected == id) null else id
                listState.requestScrollToItem(0)
            }, itemModifier) {
                when (id) {
                    in APP_SECTIONS -> AppList(bySection[APP_SECTIONS.getValue(id)].orEmpty(), VISIBLE_ROWS, open, { normal })
                    "all" -> {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).onFocusChanged { searchFocused = it.isFocused },
                            placeholder = { Text("Buscar en todas") },
                            singleLine = true,
                        )
                        if (query.isBlank()) {
                            AppList(bySection[AppSection.OTHER].orEmpty(), VISIBLE_ROWS_ALL, open, { dimmed }, alphabetIndex = true)
                        } else {
                            AppList(searchResults, VISIBLE_ROWS_ALL, open, { app -> if (isAllowed(app)) normal else dimmed }, emptyText = "Sin resultados")
                        }
                    }
                }
            }
        }
    }
}
