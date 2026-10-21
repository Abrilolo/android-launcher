package com.adrian.minimallauncher.ui

import android.content.Intent
import android.provider.AlarmClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.ModuleSettings
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.feature.ModuleChip
import com.adrian.minimallauncher.feature.ModuleHeader
import com.adrian.minimallauncher.feature.ModuleSwitch
import com.adrian.minimallauncher.feature.ModuleTextField
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.feature.finance.formatPesos
import java.math.BigDecimal
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val hhmm = DateTimeFormatter.ofPattern("HH:mm")

/** Home tile ids with the name shown in Módulos, in the same order as the home grid. */
private val MODULE_NAMES = mapOf(
    "alarms" to "Alarma", "exercise" to "Ejercicio", "finance" to "Finanzas", "diet" to "Dieta",
    "world" to "Mundo", "vault" to "Bancos", "notes" to "Notas",
)

/**
 * What the gear opens: every module with a switch for its home tile, and its settings when
 * the name is tapped. "Apps y espacios" leads to the existing schedule and sections settings.
 */
@Composable
fun ModulesScreen(
    settings: ModuleSettings,
    onUpdate: ((ModuleSettings) -> ModuleSettings) -> Unit,
    onAppsAndSpaces: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    val current = editing
    Column(modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        if (current == null) {
            ModuleHeader("INICIO", onBack, Modifier.padding(top = 8.dp))
            ModuleList(settings, onUpdate, onEdit = { editing = it }, onAppsAndSpaces)
        } else {
            ModuleHeader("MÓDULOS", { editing = null }, Modifier.padding(top = 8.dp))
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SmallLabel("EDITAR")
                Text(MODULE_NAMES.getValue(current), style = MaterialTheme.typography.headlineSmall)
                ModuleEditor(current, settings, onUpdate)
            }
        }
    }
}

@Composable
private fun ColumnScope.ModuleList(
    settings: ModuleSettings,
    onUpdate: ((ModuleSettings) -> ModuleSettings) -> Unit,
    onEdit: (String) -> Unit,
    onAppsAndSpaces: () -> Unit,
) {
    val shown = HOME_TILES.count { it !in settings.hiddenTiles }
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 12.dp, bottom = 24.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.Bottom) {
            Text("Módulos", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
            SmallLabel("$shown EN INICIO")
        }
        Divider()
        HOME_TILES.forEach { id ->
            val visible = id !in settings.hiddenTiles
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier.weight(1f).clickable(role = Role.Button) { onEdit(id) }.padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        MODULE_NAMES.getValue(id),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (visible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SmallLabel(summary(id, settings))
                }
                ModuleSwitch(visible, { show ->
                    onUpdate { it.copy(hiddenTiles = if (show) it.hiddenTiles - id else it.hiddenTiles + id) }
                }, "${MODULE_NAMES.getValue(id)} en el inicio")
            }
            Divider()
        }
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onAppsAndSpaces).padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Apps y espacios", style = MaterialTheme.typography.bodyLarge)
                SmallLabel("HORARIOS · PAUSA · APARTADOS")
            }
            SmallLabel("›")
        }
        Divider()
        SmallLabel("INTERRUPTOR = TARJETA EN INICIO · TOCA EL NOMBRE PARA EDITAR", Modifier.padding(top = 16.dp))
    }
}

private fun summary(id: String, s: ModuleSettings): String = when (id) {
    "alarms" -> "RELOJ DEL TELÉFONO"
    "exercise" -> "GUÍA TOKIO · ${s.exerciseStart.format(hhmm)}"
    "finance" -> if (s.dailyBudgetCents > 0) "PRESUPUESTO ${formatPesos(s.dailyBudgetCents)} AL DÍA" else "SIN PRESUPUESTO"
    "diet" -> "4 TIEMPOS · DESDE ${s.mealTimes.first().format(hhmm)} · ${s.waterGoal} VASOS"
    "world" -> "${s.worldZones.size} CIUDADES"
    "vault" -> "HUELLA O PIN · ${s.vaultLockSeconds} S"
    else -> "FIJA UNA NOTA PARA VERLA AQUÍ"
}

@Composable
private fun ModuleEditor(id: String, settings: ModuleSettings, onUpdate: ((ModuleSettings) -> ModuleSettings) -> Unit) {
    when (id) {
        "alarms" -> {
            val context = LocalContext.current
            Text("Las alarmas se crean en el Reloj del teléfono. El inicio muestra la siguiente.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ActionLabel("ABRIR RELOJ") { runCatching { context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
        }
        "exercise" -> {
            SmallLabel("HORA DE INICIO")
            TimeField(settings.exerciseStart) { time -> onUpdate { it.copy(exerciseStart = time) } }
            SmallLabel("DESCANSO")
            ChipRow(listOf(60, 90, 120), settings.exerciseRestSeconds, { "$it S" }) { seconds -> onUpdate { it.copy(exerciseRestSeconds = seconds) } }
        }
        "finance" -> {
            var budget by remember(settings.dailyBudgetCents) {
                mutableStateOf(if (settings.dailyBudgetCents > 0) (settings.dailyBudgetCents / 100).toString() else "")
            }
            SmallLabel("PRESUPUESTO DIARIO")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ModuleTextField(
                    budget, { value -> budget = value.filter { it.isDigit() || it == '.' }.take(8) }, "Sin presupuesto",
                    Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                ActionLabel("GUARDAR") {
                    val cents = if (budget.isBlank()) 0L else runCatching { BigDecimal(budget).movePointRight(2).longValueExact() }.getOrNull()
                    if (cents != null && cents >= 0) onUpdate { it.copy(dailyBudgetCents = cents) }
                }
            }
            SmallLabel("VACÍO = SIN PRESUPUESTO. AL PASARLO, EL GASTO DE HOY SE PONE EN ROJO.")
        }
        "diet" -> {
            var drafts by remember(settings.mealTimes) { mutableStateOf(settings.mealTimes.map { it.format(hhmm) }) }
            SmallLabel("HORARIOS")
            MEAL_NAMES.forEachIndexed { index, name ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    ModuleTextField(
                        drafts[index], { value -> drafts = drafts.toMutableList().also { it[index] = value.take(5) } }, "HH:mm",
                        Modifier.weight(0.6f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
            }
            val parsed = drafts.map { runCatching { LocalTime.parse(it) }.getOrNull() }
            if (parsed.all { it != null }) {
                ActionLabel("GUARDAR HORARIOS") { onUpdate { it.copy(mealTimes = parsed.filterNotNull()) } }
            } else {
                SmallLabel("USA EL FORMATO HH:mm", color = LauncherRed)
            }
            SmallLabel("META DE AGUA · VASOS AL DÍA", Modifier.padding(top = 8.dp))
            ChipRow(listOf(6, 8, 10, 12), settings.waterGoal, { "$it" }) { goal -> onUpdate { it.copy(waterGoal = goal) } }
        }
        "world" -> {
            SmallLabel("LAS DOS PRIMERAS VAN EN LA TARJETA DEL INICIO")
            WORLD_CITIES.forEach { city ->
                val position = settings.worldZones.indexOf(city.zone.id)
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(city.name, style = MaterialTheme.typography.bodyLarge)
                        SmallLabel(if (position in 0..1) "${city.code} · EN EL INICIO" else city.code)
                    }
                    // Newly added cities go to the end, so the order is the order they were picked.
                    ModuleSwitch(position >= 0, { add ->
                        onUpdate { s -> s.copy(worldZones = if (add) s.worldZones + city.zone.id else s.worldZones - city.zone.id) }
                    }, city.name)
                }
            }
        }
        "vault" -> {
            SmallLabel("VOLVER A BLOQUEAR A LOS")
            ChipRow(listOf(30, 45, 60, 120), settings.vaultLockSeconds, { "$it S" }) { seconds -> onUpdate { it.copy(vaultLockSeconds = seconds) } }
            Text("Siempre pide huella o PIN para abrir.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        else -> Text("Sin ajustes. Fija una nota desde Notas para verla en el inicio.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TimeField(value: LocalTime, onSave: (LocalTime) -> Unit) {
    var text by remember(value) { mutableStateOf(value.format(hhmm)) }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        ModuleTextField(text, { text = it.take(5) }, "HH:mm", Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        ActionLabel("GUARDAR") { runCatching { LocalTime.parse(text) }.getOrNull()?.let(onSave) }
    }
}

@Composable
private fun <T> ChipRow(options: List<T>, selected: T, label: (T) -> String, onPick: (T) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option -> ModuleChip(label(option), option == selected) { onPick(option) } }
    }
}

@Composable
private fun ActionLabel(text: String, onClick: () -> Unit) {
    SmallLabel(text, Modifier.clickable(role = Role.Button, onClick = onClick).padding(vertical = 12.dp, horizontal = 4.dp), color = MaterialTheme.colorScheme.onSurface)
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF1A1A1A)))
}
