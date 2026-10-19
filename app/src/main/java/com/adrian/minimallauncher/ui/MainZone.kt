package com.adrian.minimallauncher.ui
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.LauncherApp
import com.adrian.minimallauncher.ui.components.AppTextItem
import com.adrian.minimallauncher.ui.components.ClockHeader
import com.adrian.minimallauncher.ui.components.ExpandableSection
// Placeholder until reminders are persisted (Phase 6).
private val sampleReminders = listOf("Desayunar")
@Composable
fun MainZone(apps: List<LauncherApp>, onOpenApp: (String) -> Unit) {
    var remindersExpanded by rememberSaveable { mutableStateOf(true) }
    var appsExpanded by rememberSaveable { mutableStateOf(true) }
    var allAppsExpanded by rememberSaveable { mutableStateOf(false) }
    val (essentialApps, otherApps) = apps.partition { it.isEssential }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 28.dp, vertical = 32.dp)) {
        item {
            ClockHeader()
            Spacer(Modifier.height(40.dp))
        }
        item {
            ExpandableSection("Recordatorios", remindersExpanded, { remindersExpanded = !remindersExpanded }) {
                sampleReminders.forEach { reminder ->
                    Text(reminder, Modifier.padding(vertical = 6.dp), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        item {
            ExpandableSection("Apps", appsExpanded, { appsExpanded = !appsExpanded }) {
                essentialApps.forEach { app -> AppTextItem(app.label, { onOpenApp(app.packageName) }) }
            }
        }
        item {
            ExpandableSection("Todas las apps", allAppsExpanded, { allAppsExpanded = !allAppsExpanded }) {
                otherApps.forEach { app ->
                    AppTextItem(app.label, { onOpenApp(app.packageName) }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
