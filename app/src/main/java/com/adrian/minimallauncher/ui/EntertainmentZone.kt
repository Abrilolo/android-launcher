package com.adrian.minimallauncher.ui
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.LauncherApp
@Composable
fun EntertainmentZone(apps: List<LauncherApp>, onOpenApp: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), Arrangement.spacedBy(12.dp)) {
        Text("Entretenimiento", style = MaterialTheme.typography.headlineMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(apps, key = { it.packageName }) { app -> TextButton(onClick = { onOpenApp(app.packageName) }) { Text(app.label) } }
        }
    }
}
