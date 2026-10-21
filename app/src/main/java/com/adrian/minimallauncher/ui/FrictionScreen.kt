package com.adrian.minimallauncher.ui

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.LauncherApp
import kotlinx.coroutines.delay

@Composable
fun FrictionScreen(
    app: LauncherApp,
    spaceName: String,
    pauseSeconds: Int,
    onCancel: () -> Unit,
    onOpenApp: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onCancel)
    var remainingSeconds by remember { mutableIntStateOf(pauseSeconds) }
    var reason by remember { mutableStateOf("") }
    var deadline by remember { mutableLongStateOf(SystemClock.elapsedRealtime() + pauseSeconds * 1_000L) }
    LaunchedEffect(app.packageName, pauseSeconds) {
        remainingSeconds = pauseSeconds
        deadline = SystemClock.elapsedRealtime() + pauseSeconds * 1_000L
        while (remainingSeconds > 0) {
            delay(250)
            remainingSeconds = ((deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0) + 999).div(1_000).toInt()
        }
    }
    Column(
        modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(spaceName.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(app.label, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 12.dp))
        Text(
            if (remainingSeconds > 0) "PAUSA  $remainingSeconds S" else "¿MOTIVO?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 24.dp),
        )
        OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            enabled = remainingSeconds == 0,
            placeholder = { Text("Escribe el motivo...") },
            minLines = 2,
        )
        Button(
            onClick = { onOpenApp(app.packageName) },
            enabled = remainingSeconds == 0 && reason.isNotBlank(),
            modifier = Modifier.padding(top = 16.dp),
        ) { Text("ABRIR APP") }
        androidx.compose.material3.TextButton(onClick = onCancel, modifier = Modifier.padding(top = 8.dp)) {
            Text("VOLVER")
        }
    }
}
