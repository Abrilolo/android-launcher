package com.adrian.minimallauncher.ui
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import com.adrian.minimallauncher.data.LauncherApp
import com.adrian.minimallauncher.data.WidgetPlacement
import com.adrian.minimallauncher.data.PersonalRecords
import com.adrian.minimallauncher.data.PersonalRecordActions
import com.adrian.minimallauncher.ui.components.AppTextItem
import com.adrian.minimallauncher.ui.components.ClockHeader
import com.adrian.minimallauncher.ui.components.ExpandableSection
import com.adrian.minimallauncher.ui.widgets.EditableWidgetRow
@Composable
fun MainZone(
    apps: List<LauncherApp>,
    restrictedApps: List<LauncherApp>,
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
        ClockHeader(spaceName, now, Modifier.padding(bottom = 40.dp))
        TextButton(onClick = onSettings, Modifier.padding(bottom = 8.dp)) { Text("CONFIGURAR") }
        EditableWidgetRow(widgetPlacement, onWidgetPlacementChange, Modifier.weight(1f)) { columnModifier ->
            SectionsColumn(apps, restrictedApps, onOpenApp, onRestrictedApp, personalRecords, personalActions, hiddenSections, spaceName, columnModifier)
        }
    }
}
@Composable
private fun SectionsColumn(
    apps: List<LauncherApp>,
    restrictedApps: List<LauncherApp>,
    onOpenApp: (String) -> Unit,
    onRestrictedApp: (LauncherApp) -> Unit,
    personalRecords: PersonalRecords,
    personalActions: PersonalRecordActions,
    hiddenSections: Set<String>,
    spaceName: String,
    modifier: Modifier,
) {
    var appsExpanded by rememberSaveable { mutableStateOf(true) }
    var allAppsExpanded by rememberSaveable { mutableStateOf(false) }
    var restrictedExpanded by rememberSaveable { mutableStateOf(false) }
    val (essentialApps, otherApps) = apps.partition { it.isEssential }
    LazyColumn(modifier, contentPadding = PaddingValues(bottom = 32.dp)) {
        item { PersonalSections(personalRecords, personalActions, spaceName, hiddenSections) }
        if ("apps" !in hiddenSections) item {
            ExpandableSection("Apps", appsExpanded, { appsExpanded = !appsExpanded }) {
                essentialApps.forEach { app -> AppTextItem(app.label, { onOpenApp(app.packageName) }) }
            }
        }
        if ("all" !in hiddenSections) item {
            ExpandableSection("Todas", allAppsExpanded, { allAppsExpanded = !allAppsExpanded }) {
                otherApps.forEach { app ->
                    AppTextItem(app.label, { onOpenApp(app.packageName) }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if ("restricted" !in hiddenSections) item {
            ExpandableSection("Restringidas", restrictedExpanded, { restrictedExpanded = !restrictedExpanded }) {
                restrictedApps.forEach { app ->
                    AppTextItem(app.label, { onRestrictedApp(app) }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
