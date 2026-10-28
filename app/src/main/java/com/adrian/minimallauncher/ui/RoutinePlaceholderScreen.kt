package com.adrian.minimallauncher.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.routine.ActionCatalog
import com.adrian.minimallauncher.data.routine.ActionKind
import com.adrian.minimallauncher.data.routine.PhaseMode
import com.adrian.minimallauncher.data.routine.RoutineEngine
import com.adrian.minimallauncher.data.routine.Trigger
import java.time.LocalTime
import com.adrian.minimallauncher.data.routine.Places
import com.adrian.minimallauncher.ui.theme.LauncherBlack
import com.adrian.minimallauncher.ui.theme.LauncherTheme
import com.adrian.minimallauncher.viewmodel.LauncherViewModel

/**
 * PLACEHOLDER. A plain test panel for the routine backend (phase, dots, audio, places, WhatsApp, forced mode)
 * until the real views exist. Opened from the debug quick settings tile "Rutina".
 */
@Composable
fun RoutinePlaceholderScreen(viewModel: LauncherViewModel, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val phase by viewModel.currentPhase.collectAsState()
    val checks by viewModel.phaseChecks.collectAsState()
    val phases by viewModel.phases.collectAsState()
    val places by viewModel.places.collectAsState()
    val settings by viewModel.moduleSettings.collectAsState()
    val summary by viewModel.financeSummary.collectAsState()
    val foodDay by viewModel.foodDay.collectAsState()
    val adjustment by viewModel.exerciseAdjustment.collectAsState()
    var message by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    val askForeground = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refresh++; viewModel.registerPlaces() }
    val askBackground = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++; viewModel.registerPlaces() }

    LauncherTheme {
        Column(
            Modifier.fillMaxSize().background(LauncherBlack).safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("PLACEHOLDER · RUTINA", style = MaterialTheme.typography.labelLarge)
            Text("FASES", style = MaterialTheme.typography.labelLarge)
            phases.forEach { p ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text((if (p.id == phase.id) "● " else "○ ") + p.name, Modifier.weight(1f).padding(top = 12.dp))
                    TextButton({ viewModel.movePhase(p.id, -1) }) { Text("↑") }
                    TextButton({ viewModel.movePhase(p.id, 1) }) { Text("↓") }
                    TextButton({ viewModel.deletePhase(p.id) }) { Text("✕") }
                }
            }
            var newPhase by remember { mutableStateOf("") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(newPhase, { newPhase = it }, Modifier.weight(1f), label = { Text("Nueva fase") })
                OutlinedButton({
                    val now = LocalTime.now().withSecond(0).withNano(0)
                    viewModel.addPhase(newPhase.ifBlank { "Fase" }, Trigger.Between(now, now.plusHours(1)))
                    newPhase = ""
                }) { Text("+") }
            }

            Text("${phase.name.uppercase()} · ${phase.window?.let { "${it.start}–${it.end}" } ?: "por lugar"}", style = MaterialTheme.typography.titleMedium)
            // Progress as dots, never as a fraction.
            Text(checks.filter { it.on && it.markable }.joinToString(" ") { if (it.done) "●" else "○" })
            OutlinedButton({ viewModel.setPhaseMode(phase.id, if (phase.mode == PhaseMode.CHOOSER) PhaseMode.ORDERED else PhaseMode.CHOOSER) }) {
                Text(if (phase.mode == PhaseMode.CHOOSER) "Modo: elegir" else "Modo: en orden")
            }
            // What starts this phase: an hour window, or arriving at / leaving any place of the list.
            var start by remember(phase.id) { mutableStateOf(phase.window?.start?.toString() ?: "") }
            var end by remember(phase.id) { mutableStateOf(phase.window?.end?.toString() ?: "") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(start, { start = it }, Modifier.weight(1f), label = { Text("Desde") })
                OutlinedTextField(end, { end = it }, Modifier.weight(1f), label = { Text("Hasta") })
            }
            val window = runCatching { Trigger.Between(LocalTime.parse(start.trim()), LocalTime.parse(end.trim())) }.getOrNull()
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ window?.let { viewModel.setPhaseTrigger(phase.id, it, it) } }, enabled = window != null) { Text("Por hora") }
                places.forEach { place ->
                    OutlinedButton({ viewModel.setPhaseTrigger(phase.id, Trigger.Arrive(place.id), window) }) { Text("Llegar ${place.name}") }
                    OutlinedButton({ viewModel.setPhaseTrigger(phase.id, Trigger.Leave(place.id), window) }) { Text("Salir ${place.name}") }
                }
            }
            checks.forEach { state ->
                val action = state.action
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (state.markable) Checkbox(state.done, { viewModel.markAction(action.id) }, enabled = RoutineEngine.canMark(phase, checks, action.id))
                    Text("${action.kind.label} ${action.at ?: ""}${if (action.minutes > 0) " ${action.minutes} min" else ""}", Modifier.weight(1f).padding(top = 12.dp))
                    if (phase.mode == PhaseMode.CHOOSER) Switch(state.on, { viewModel.setActionEnabled(phase.id, action.id, it) })
                    TextButton({ viewModel.moveAction(phase.id, action.id, -1) }) { Text("↑") }
                    TextButton({ viewModel.removeAction(phase.id, action.id) }) { Text("✕") }
                }
                if (action.kind.kind == ActionKind.AUDIO) {
                    var pkg by remember(action.id) { mutableStateOf(action.packageName) }
                    OutlinedTextField(pkg, { pkg = it }, Modifier.fillMaxWidth(), label = { Text("App (paquete)") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton({ viewModel.setActionConfig(phase.id, action.id, action.config + (ActionCatalog.PACKAGE to pkg.trim())) }) { Text("Guardar") }
                        OutlinedButton({ if (!viewModel.openPhaseAudio(action.id)) message = "Sin app de audio" }) { Text("Abrir") }
                    }
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                viewModel.actionCatalog.forEach { type -> OutlinedButton({ viewModel.addAction(phase.id, type.key) }) { Text("+ ${type.label}") } }
            }

            Text("LUGARES", style = MaterialTheme.typography.labelLarge)
            refresh.let { Text(if (viewModel.locationGranted()) "Ubicación: todo el tiempo" else if (Places.hasForeground(context)) "Falta: permitir todo el tiempo" else "Sin permiso de ubicación") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ askForeground.launch(Places.FOREGROUND_PERMISSIONS) }) { Text("Ubicación") }
                OutlinedButton({ askBackground.launch(Places.BACKGROUND_PERMISSION) }) { Text("Todo el tiempo") }
            }
            places.forEach { place ->
                Text("${place.name}: ${if (place.located) "%.5f, %.5f".format(place.lat, place.lng) else "sin fijar"}")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ viewModel.setPlaceHere(place.id) { message = if (it == null) "Sin ubicación" else "${place.name} guardada" } }) { Text("Fijar aquí") }
                    OutlinedButton({ viewModel.simulatePlace(place.id, entered = true) }) { Text("Simular llegar") }
                    OutlinedButton({ viewModel.simulatePlace(place.id, entered = false) }) { Text("Simular salir") }
                    OutlinedButton({ viewModel.removePlace(place.id) }) { Text("✕") }
                }
            }
            var newPlace by remember { mutableStateOf("") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(newPlace, { newPlace = it }, Modifier.weight(1f), label = { Text("Nuevo lugar") })
                OutlinedButton({ if (newPlace.isNotBlank()) viewModel.addPlace(newPlace); newPlace = "" }) { Text("+") }
            }

            Text("WHATSAPP", style = MaterialTheme.typography.labelLarge)
            var phone by remember(settings.whatsappPhone) { mutableStateOf(settings.whatsappPhone) }
            var text by remember(settings.whatsappText) { mutableStateOf(settings.whatsappText) }
            OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text("Teléfono (5215512345678)") })
            OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(), label = { Text("Mensaje") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ viewModel.setWhatsApp(phone, text) }) { Text("Guardar") }
                OutlinedButton({ if (!viewModel.openWhatsAppLeft()) message = "Falta el teléfono" }) { Text("Probar") }
            }

            Text("MODO FORZOSO", style = MaterialTheme.typography.labelLarge)
            refresh.let { Text(if (viewModel.forcedServiceEnabled()) "Servicio de accesibilidad: activo" else "Servicio de accesibilidad: apagado") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) { Text("Accesibilidad") }
                OutlinedButton({ viewModel.debugAddPendingPayment() }) { Text("Pago de prueba") }
            }

            Text("REGISTROS", style = MaterialTheme.typography.labelLarge)
            Text("Hoy puedes gastar $${"%.2f".format(summary.todayLeftCents / 100.0)} · semana $${"%.2f".format(summary.weekLeftCents / 100.0)}")
            Text("Comida: ${foodDay.eaten.kcal.toInt()} kcal${if (foodDay.kcalGoal > 0) " · quedan ${foodDay.kcalLeft.toInt()}" else ""}${foodDay.bmi?.let { " · IMC %.1f".format(it) } ?: ""}")
            adjustment?.let { Text("Para quemar ${it.excessKcal.toInt()} kcal: ${it.exercise.name} ${it.minutes} min") }

            if (message.isNotEmpty()) Text(message)
            OutlinedButton(onClose) { Text("Cerrar") }
        }
    }
}
