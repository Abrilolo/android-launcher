package com.adrian.minimallauncher.ui
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.LauncherApp
@Composable
fun MainZone(apps: List<LauncherApp>, onOpenApp: (String) -> Unit) {
    val quickActions = listOf("Alimentación", "Gastos", "Ejercicio", "Tareas")
    Column(Modifier.fillMaxSize().padding(24.dp), Arrangement.spacedBy(12.dp)) {
        Text("Mi día", style = MaterialTheme.typography.headlineMedium)
        Text("Acciones rápidas")
        quickActions.forEach { action -> Button(onClick = { }) { Text(action) } }
        Text("Apps esenciales")
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(apps, key = { it.packageName }) { app -> Button(onClick = { onOpenApp(app.packageName) }) { Text(app.label) } }
        }
    }
}
