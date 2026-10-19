package com.adrian.minimallauncher.ui
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.LauncherApp
import com.adrian.minimallauncher.ui.components.AppTextItem
@Composable
fun EntertainmentZone(apps: List<LauncherApp>, onOpenApp: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 28.dp, vertical = 32.dp)) {
        item {
            Text(
                "ENTRETENIMIENTO",
                Modifier.padding(bottom = 16.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(apps, key = { it.packageName }) { app ->
            AppTextItem(app.label, { onOpenApp(app.packageName) }, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
