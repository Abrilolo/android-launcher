package com.adrian.minimallauncher.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.AlarmClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import com.adrian.minimallauncher.data.MealKind
import com.adrian.minimallauncher.data.MealSlot
import com.adrian.minimallauncher.data.ModuleSettings
import com.adrian.minimallauncher.data.NoteRecord
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.feature.ModuleChip
import com.adrian.minimallauncher.feature.ModuleHeader
import com.adrian.minimallauncher.feature.ModuleSwitch
import com.adrian.minimallauncher.feature.ModuleTextField
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.feature.finance.formatPesos
import com.adrian.minimallauncher.ui.components.DotIcon
import com.adrian.minimallauncher.ui.components.DotIcons
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import java.time.format.DateTimeFormatter

private val hhmm = DateTimeFormatter.ofPattern("HH:mm")

/** Home tile ids with the name shown in Módulos, in the same order as the home grid. */
private val MODULE_NAMES = mapOf(
    "alarms" to "Alarma", "exercise" to "Ejercicio", "finance" to "Finanzas", "diet" to "Dieta",
    "world" to "Mundo", "vault" to "Bancos", "notes" to "Notas",
)

/**
 * What the gear opens: every module with a switch for its home tile, and its settings when
 * the name is tapped. "Apps y espacios" leads to the existing schedule and sections settings;
 * "Respaldo" exports and imports every record as a file.
 */
@Composable
fun ModulesScreen(
    settings: ModuleSettings,
    onUpdate: ((ModuleSettings) -> ModuleSettings) -> Unit,
    backup: BackupUi,
    onAppsAndSpaces: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** Opened from a module's gear: show its settings right away, and back leaves instead of going to the list. */
    initialEditing: String? = null,
) {
    var editing by rememberSaveable { mutableStateOf(initialEditing) }
    val current = editing
    val closeEditor = { if (initialEditing != null) onBack() else editing = null }
    BackHandler(onBack = if (current != null) closeEditor else onBack)
    Column(modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        if (current == null) {
            ModuleHeader("INICIO", onBack, Modifier.padding(top = 8.dp))
            ModuleList(settings, onUpdate, backup.lastAt, onEdit = { editing = it }, onAppsAndSpaces)
        } else {
            ModuleHeader("MÓDULOS", closeEditor, Modifier.padding(top = 8.dp))
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SmallLabel("EDITAR")
                Text(MODULE_NAMES[current] ?: "Respaldo", style = MaterialTheme.typography.headlineSmall)
                if (current == BACKUP) BackupEditor(backup) else ModuleEditor(current, settings, onUpdate)
            }
        }
    }
}

@Composable
private fun ColumnScope.ModuleList(
    settings: ModuleSettings,
    onUpdate: ((ModuleSettings) -> ModuleSettings) -> Unit,
    lastBackupAt: Long?,
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
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Vibración", style = MaterialTheme.typography.bodyLarge)
                SmallLabel("AL MARCAR, GUARDAR O TERMINAR")
            }
            ModuleSwitch(settings.haptics, { on -> onUpdate { it.copy(haptics = on) } }, "Vibración")
        }
        Divider()
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
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button) { onEdit(BACKUP) }.padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Respaldo", style = MaterialTheme.typography.bodyLarge)
                SmallLabel(lastBackupAt?.let { "ÚLTIMO · ${backupDate(it)}" } ?: "SIN RESPALDO", color = if (lastBackupAt == null) LauncherRed else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SmallLabel("›")
        }
        Divider()
        SmallLabel("INTERRUPTOR = TARJETA EN INICIO · TOCA EL NOMBRE PARA EDITAR", Modifier.padding(top = 16.dp))
    }
}

private const val BACKUP = "backup"
private val spanish = Locale.forLanguageTag("es")

private fun backupDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM · HH:mm", spanish)).uppercase(spanish).replace(".", "")

/** What the backup page needs: when it last ran, the outcome to show, and the two file actions. */
data class BackupUi(val lastAt: Long?, val message: String?, val onExport: (Uri) -> Unit, val onImport: (Uri) -> Unit)

/** The system file picker chooses where the file goes (Drive, Downloads…), so no storage permission is needed. */
@Composable
private fun BackupEditor(backup: BackupUi) {
    val exportFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(backup.onExport) }
    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(backup.onImport) }
    Text(
        "Guarda en un archivo tus notas, gastos, comidas, hábitos, series de ejercicio, medidas, recordatorios y los ajustes de los módulos.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    SmallLabel(backup.lastAt?.let { "ÚLTIMO RESPALDO · ${backupDate(it)}" } ?: "TODAVÍA NO HAY RESPALDO", color = MaterialTheme.colorScheme.onSurface)
    ActionLabel("EXPORTAR") { exportFile.launch("launcher-respaldo-${LocalDate.now()}.json") }
    ActionLabel("IMPORTAR") { importFile.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
    backup.message?.let { SmallLabel(it, color = if (it.startsWith("NO SE PUDO")) LauncherRed else MaterialTheme.colorScheme.onSurface) }
    SmallLabel("IMPORTAR NO BORRA NADA: SOLO AGREGA LO QUE FALTE. LOS HORARIOS Y APARTADOS DE APPS NO VAN EN EL RESPALDO.", Modifier.padding(top = 8.dp))
}

private fun summary(id: String, s: ModuleSettings): String = when (id) {
    "alarms" -> "RELOJ DEL TELÉFONO"
    "exercise" -> "GUÍA TOKIO · ${s.exerciseStart.format(hhmm)}"
    "finance" -> if (s.dailyBudgetCents > 0) "PRESUPUESTO ${formatPesos(s.dailyBudgetCents)} AL DÍA" else "SIN PRESUPUESTO"
    "diet" -> listOfNotNull(
        "${s.meals.size} COMIDAS",
        s.mealTimes.minOrNull()?.let { "DESDE ${it.format(hhmm)}" },
        "${s.waterGoal} × ${formatMl(s.waterGlassMl)}",
    ).joinToString(" · ")
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
            MealsEditor(settings.meals) { meals -> onUpdate { it.copy(meals = meals) } }
            WaterEditor(settings, onUpdate)
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

/** Widgets that ask for their setup right after being placed on a page. */
val WIDGETS_WITH_SETUP = setOf("world", "notes")

/** Setup shown after placing a widget: Mundo asks which cities, Notas which note to show. */
@Composable
fun WidgetSetupScreen(
    widgetId: String,
    settings: ModuleSettings,
    notes: List<NoteRecord>,
    onUpdate: ((ModuleSettings) -> ModuleSettings) -> Unit,
    onPinNote: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        ModuleHeader(if (widgetId == "world") "RELOJ MUNDIAL" else "NOTA FIJADA", onDone, Modifier.padding(top = 8.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (widgetId == "world") {
                ModuleEditor("world", settings, onUpdate)
            } else if (notes.isEmpty()) {
                Text("Todavía no hay notas. Escribe una en Notas y fíjala para verla aquí.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                SmallLabel("ELIGE LA NOTA QUE SE VE EN EL WIDGET")
                notes.sortedByDescending { it.createdAt }.forEach { note ->
                    Row(
                        Modifier.fillMaxWidth().clickable(role = Role.Button) { onPinNote(note.id) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(note.text, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            SmallLabel(note.category.label.uppercase())
                        }
                        if (note.pinned) SmallLabel("FIJADA", Modifier.padding(start = 12.dp), color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .height(52.dp)
                .background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(26.dp))
                .clickable(role = Role.Button, onClick = onDone),
            contentAlignment = Alignment.Center,
        ) {
            Text("LISTO", style = MaterialTheme.typography.labelSmall, color = Color.Black)
        }
    }
}

/** A meal being edited; the time stays text until it is saved. */
private data class MealDraft(val id: String, val name: String, val time: String, val kind: MealKind, val icon: String)

/**
 * Every meal of the day: name, time, whether it is a main meal (drawn bigger) or a snack, and its icon (tap it to
 * change). Meals can be added and removed; saving sorts them by time.
 */
@Composable
private fun MealsEditor(meals: List<MealSlot>, onSave: (List<MealSlot>) -> Unit) {
    var drafts by remember(meals) { mutableStateOf(meals.map { MealDraft(it.id, it.name, it.time.format(hhmm), it.kind, it.icon) }) }
    val icons = DotIcons.meals.keys.toList()
    fun change(index: Int, update: (MealDraft) -> MealDraft) {
        drafts = drafts.toMutableList().also { it[index] = update(it[index]) }
    }
    SmallLabel("COMIDAS · TOCA EL ICONO PARA CAMBIARLO")
    drafts.forEachIndexed { index, draft ->
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                DotIcon(
                    DotIcons.meal(draft.icon), done = true, contentDescription = "Icono de ${draft.name}",
                    modifier = Modifier.clickable(role = Role.Button) {
                        change(index) { it.copy(icon = icons[(icons.indexOf(it.icon) + 1) % icons.size]) }
                    },
                    size = if (draft.kind == MealKind.MAIN) 40.dp else 30.dp,
                )
                ModuleTextField(draft.name, { value -> change(index) { it.copy(name = value.take(30)) } }, "Nombre", Modifier.weight(1f))
                ModuleTextField(
                    draft.time, { value -> change(index) { it.copy(time = value.take(5)) } }, "HH:mm",
                    Modifier.weight(0.45f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Text(
                    "×",
                    Modifier
                        .clickable(role = Role.Button) { drafts = drafts.filterIndexed { i, _ -> i != index } }
                        .padding(8.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ChipRow(listOf(MealKind.MAIN, MealKind.SNACK), draft.kind, { if (it == MealKind.MAIN) "FUERTE" else "SNACK" }) { kind ->
                change(index) { it.copy(kind = kind) }
            }
        }
        Divider()
    }
    ActionLabel("+ AGREGAR COMIDA") {
        drafts = drafts + MealDraft("m${System.currentTimeMillis()}", "", "", MealKind.SNACK, "snack")
    }
    val parsed = drafts.map { d -> runCatching { LocalTime.parse(d.time) }.getOrNull()?.let { MealSlot(d.id, d.name.trim(), it, d.kind, d.icon) } }
    when {
        parsed.any { it == null } -> SmallLabel("USA EL FORMATO HH:mm", color = LauncherRed)
        parsed.any { it!!.name.isBlank() } -> SmallLabel("CADA COMIDA NECESITA NOMBRE", color = LauncherRed)
        else -> ActionLabel("GUARDAR COMIDAS") { onSave(parsed.filterNotNull().sortedBy { it.time }) }
    }
}

/** Water: what it is drunk from, how much that holds and how many a day; the day's total is shown below. */
@Composable
private fun WaterEditor(settings: ModuleSettings, onUpdate: ((ModuleSettings) -> ModuleSettings) -> Unit) {
    SmallLabel("AGUA · RECIPIENTE", Modifier.padding(top = 8.dp))
    ChipRow(listOf("vaso", "termo"), settings.waterContainer, ::waterUnit) { container -> onUpdate { it.copy(waterContainer = container) } }
    SmallLabel("LE CABEN · ML")
    ChipRow(listOf(250, 355, 500, 750, 1000), settings.waterGlassMl, { "$it" }) { ml -> onUpdate { it.copy(waterGlassMl = ml) } }
    SmallLabel("CUÁNTOS AL DÍA")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        ModuleChip("−", false) { onUpdate { it.copy(waterGoal = (it.waterGoal - 1).coerceAtLeast(1)) } }
        Text("${settings.waterGoal}", style = MaterialTheme.typography.titleLarge)
        ModuleChip("+", false) { onUpdate { it.copy(waterGoal = (it.waterGoal + 1).coerceAtMost(20)) } }
        SmallLabel("= ${formatMl(settings.waterGoal * settings.waterGlassMl)} AL DÍA", color = MaterialTheme.colorScheme.onSurface)
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
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
