package com.adrian.minimallauncher.ui

import android.app.TimePickerDialog
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.AppGroup
import com.adrian.minimallauncher.data.LauncherApp
import com.adrian.minimallauncher.data.routine.ActionCatalog
import com.adrian.minimallauncher.data.routine.ActionKind
import com.adrian.minimallauncher.data.routine.PhaseMode
import com.adrian.minimallauncher.data.routine.Place
import com.adrian.minimallauncher.data.routine.Places
import com.adrian.minimallauncher.data.routine.RoutineAction
import com.adrian.minimallauncher.data.routine.RoutinePhase
import com.adrian.minimallauncher.data.routine.Trigger
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
import com.adrian.minimallauncher.viewmodel.LauncherViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

private val White = Color(0xFFF5F5F5)
private val Gray = Color(0xFF5C5C5C)
private val Muted = Color(0xFFB0B0B0)
private val Line = Color(0xFF222222)
private val Border = Color(0xFF333333)
private val Raised = Color(0xFF1C1C1C)
private val Card = Color(0xFF0B0B0B)
private val hhmm = DateTimeFormatter.ofPattern("HH:mm")

private val GroupLabels = listOf(AppGroup.ESSENTIAL to "ESENCIALES", AppGroup.OTHER to "OTRAS", AppGroup.ENTERTAINMENT to "ENTRETENIMIENTO")

/** The group an app belongs to under the phase rule; vault apps always open, so they belong to none. */
private fun LauncherApp.group(): AppGroup? = when {
    isVault -> null
    isEssential -> AppGroup.ESSENTIAL
    isEntertainment -> AppGroup.ENTERTAINMENT
    else -> AppGroup.OTHER
}

/**
 * Rutinas settings, behind the gear of the Rutinas view (concept: docs/design/concept/RutinasAjustesVisual.html).
 * Labels are fine here. Places (rename, remove, fix here), phase order, mode, allowed app groups and each action's
 * config. Name, trigger and adding/removing actions stay in the view's pencil. Test tools sit closed at the bottom.
 */
@Composable
fun RoutineSettingsScreen(viewModel: LauncherViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    val phases by viewModel.phases.collectAsState()
    val places by viewModel.places.collectAsState()
    val apps by viewModel.apps.collectAsState()

    var openPhaseId by remember { mutableStateOf<String?>(null) }
    var debugOpen by remember { mutableStateOf(false) }
    var appPicker by remember { mutableStateOf<Pair<String, RoutineAction>?>(null) }
    var message by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    val fixed = remember { mutableStateMapOf<String, Int>() }

    val askForeground = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refresh++; viewModel.registerPlaces() }
    val askBackground = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++; viewModel.registerPlaces() }

    BackHandler { if (appPicker != null) appPicker = null else onClose() }
    LaunchedEffect(message) { if (message.isNotEmpty()) { delay(1_600); message = "" } }

    Box(Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding().imePadding()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("AJUSTES · RUTINAS", style = MaterialTheme.typography.labelSmall, color = LauncherRed)
                Spacer(Modifier.weight(1f))
                CloseMark("Cerrar", onClose)
            }

            Section {
                SectionLabel("LUGARES")
                val granted = refresh.let { viewModel.locationGranted() }
                if (!granted) Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (Places.hasForeground(context)) "UBICACIÓN: FALTA “TODO EL TIEMPO”" else "SIN PERMISO DE UBICACIÓN",
                        Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = LauncherRed,
                    )
                    SettingsChip("PERMITIR", on = false, red = true) {
                        if (Places.hasForeground(context)) askBackground.launch(Places.BACKGROUND_PERMISSION)
                        else askForeground.launch(Places.FOREGROUND_PERMISSIONS)
                    }
                }
                Divider()
                places.forEach { place ->
                    PlaceRow(
                        place, fixed[place.id],
                        onRename = { viewModel.renamePlace(place.id, it) },
                        onFix = {
                            viewModel.setPlaceHere(place.id) {
                                if (it == null) message = "Sin ubicación"
                                else { fixed[place.id] = (fixed[place.id] ?: 0) + 1; message = "${place.name} guardada" }
                            }
                        },
                        onRemove = { viewModel.removePlace(place.id) },
                    )
                }
                NewPlaceRow { viewModel.addPlace(it) }
            }

            Section {
                SectionLabel("FASES")
                Divider()
                phases.forEachIndexed { i, phase ->
                    val open = phase.id == openPhaseId
                    PhaseRow(
                        phase, places, open, first = i == 0, last = i == phases.lastIndex,
                        onToggle = { openPhaseId = if (open) null else phase.id },
                        onMove = { viewModel.movePhase(phase.id, it) },
                    )
                    if (open) PhaseDetail(phase, apps, viewModel, onPickApp = { appPicker = phase.id to it })
                    if (i < phases.lastIndex) Box(Modifier.padding(vertical = 8.dp).fillMaxWidth().size(1.dp).background(Line))
                }
            }

            DebugSection(debugOpen, places, viewModel, refresh, onToggle = { debugOpen = !debugOpen; refresh++ }, onMessage = { message = it }) {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        appPicker?.let { (phaseId, action) ->
            AppSheet(
                title = action.kind.label.uppercase(),
                apps = apps.filterNot { it.isVault }.sortedBy { it.label.lowercase() },
                selected = action.packageName,
                onPick = { pkg ->
                    viewModel.setActionConfig(phaseId, action.id, action.config + (ActionCatalog.PACKAGE to pkg))
                    appPicker = null
                },
                onClose = { appPicker = null },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        if (message.isNotEmpty()) Text(
            message,
            Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp).clip(RoundedCornerShape(12.dp)).background(Raised)
                .border(1.dp, Border, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelSmall, color = White,
        )
    }
}

@Composable
private fun PlaceRow(place: Place, play: Int?, onRename: (String) -> Unit, onFix: () -> Unit, onRemove: () -> Unit) {
    var name by remember(place.id, place.name) { mutableStateOf(place.name) }
    var focused by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StrokeIcon("pin", if (!place.located) StrokeState.OFF else if (play != null) StrokeState.NOW else StrokeState.DONE, 24.dp, playKey = play)
        Column(Modifier.weight(1f)) {
            BasicTextField(
                name, { name = it.take(30) },
                Modifier.fillMaxWidth().semantics { contentDescription = "Nombre del lugar" }.onFocusChanged {
                    if (focused && !it.isFocused && name.isNotBlank() && name != place.name) onRename(name)
                    focused = it.isFocused
                },
                textStyle = MaterialTheme.typography.titleMedium.copy(color = White),
                singleLine = true,
                cursorBrush = SolidColor(LauncherRed),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank() && name != place.name) onRename(name) }),
            )
            SubLabel(if (place.located) "FIJADO" else "SIN FIJAR")
        }
        SettingsChip("FIJAR AQUÍ", on = false, onClick = onFix)
        CloseMark("Quitar lugar", onRemove)
    }
}

@Composable
private fun NewPlaceRow(onAdd: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    fun add() { if (name.isNotBlank()) onAdd(name); name = "" }
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = ::add).semantics { contentDescription = "Agregar lugar" }, contentAlignment = Alignment.Center) {
            DashedSlot(40)
            Text("+", style = MaterialTheme.typography.titleMedium, color = Gray)
        }
        TextField(name, { name = it.take(30) }, "Nuevo lugar", Modifier.weight(1f), onDone = ::add)
    }
}

@Composable
private fun PhaseRow(phase: RoutinePhase, places: List<Place>, open: Boolean, first: Boolean, last: Boolean, onToggle: () -> Unit, onMove: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onToggle).semantics { contentDescription = phase.name },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StrokeIcon(phaseIcon(phase), if (open) StrokeState.NOW else StrokeState.DONE, 32.dp, playKey = open.takeIf { it })
        Column(Modifier.weight(1f)) {
            Text(phase.name, style = MaterialTheme.typography.titleMedium, color = White)
            val trigger = when (val t = phase.trigger) {
                is Trigger.Between -> t.start.format(hhmm)
                is Trigger.Arrive -> "LLEGAR · ${places.firstOrNull { it.id == t.placeId }?.name ?: "—"}"
                is Trigger.Leave -> "SALIR · ${places.firstOrNull { it.id == t.placeId }?.name ?: "—"}"
            }
            SubLabel("$trigger · ${if (phase.mode == PhaseMode.ORDERED) "EN ORDEN" else "ELEGIR"}".uppercase())
        }
        Arrow("↑", "Subir", enabled = !first) { onMove(-1) }
        Arrow("↓", "Bajar", enabled = !last) { onMove(1) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhaseDetail(phase: RoutinePhase, apps: List<LauncherApp>, viewModel: LauncherViewModel, onPickApp: (RoutineAction) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(start = 44.dp, top = 6.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SubLabel("MODO")
            Row(Modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, Border, RoundedCornerShape(10.dp))) {
                listOf(PhaseMode.CHOOSER to "ELEGIR", PhaseMode.ORDERED to "EN ORDEN").forEach { (mode, label) ->
                    val on = phase.mode == mode
                    Text(
                        label,
                        Modifier.background(if (on) Raised else Color.Transparent).clickable(role = Role.Button) { viewModel.setPhaseMode(phase.id, mode) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall, color = if (on) White else Muted,
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val grouped = apps.mapNotNull { it.group() }
            val open = grouped.count { it in phase.groups }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                SubLabel("APPS", Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StrokeIcon("apps", StrokeState.DONE, 14.dp)
                    Text("$open", style = MaterialTheme.typography.labelSmall, color = White)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StrokeIcon("candado", StrokeState.NOW, 14.dp)
                    Text("${grouped.size - open}", style = MaterialTheme.typography.labelSmall, color = LauncherRed)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GroupLabels.forEach { (group, label) ->
                    val on = group in phase.groups
                    SettingsChip(label, on) { viewModel.setPhaseGroups(phase.id, if (on) phase.groups - group else phase.groups + group) }
                }
            }
        }

        SubLabel("ACCIONES")
        if (phase.actions.isEmpty()) SubLabel("SIN ACCIONES · AGRÉGALAS CON EL LÁPIZ")
        phase.actions.forEach { action -> ActionConfig(phase, action, apps, viewModel, onPickApp) }
    }
}

/** One action and only the settings its type uses: hour, minutes, app, or WhatsApp chat and text. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionConfig(phase: RoutinePhase, action: RoutineAction, apps: List<LauncherApp>, viewModel: LauncherViewModel, onPickApp: (RoutineAction) -> Unit) {
    val context = LocalContext.current
    val keys = action.kind.configKeys
    fun save(key: String, value: String) = viewModel.setActionConfig(phase.id, action.id, action.config + (key to value))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StrokeIcon(action.kind.icon, StrokeState.DONE, 24.dp)
            Text(action.kind.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = White)
            if (ActionCatalog.PHONE !in keys) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (ActionCatalog.MINUTES in keys) {
                    val minutes = action.minutes.takeIf { it > 0 } ?: 5
                    SettingsChip("−", on = false) { save(ActionCatalog.MINUTES, (minutes - 1).coerceAtLeast(1).toString()) }
                    Text("$minutes", Modifier.align(Alignment.CenterVertically).width(28.dp), style = MaterialTheme.typography.labelLarge, color = White, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    SettingsChip("+", on = false) { save(ActionCatalog.MINUTES, (minutes + 1).coerceAtMost(60).toString()) }
                    SubLabel("MIN", Modifier.align(Alignment.CenterVertically))
                }
                if (ActionCatalog.AT in keys) {
                    val at = action.at
                    SettingsChip(at?.format(hhmm) ?: "SIN HORA", on = at != null, icon = "reloj") {
                        val start = at ?: LocalTime.of(12, 0)
                        TimePickerDialog(context, { _, h, m -> save(ActionCatalog.AT, LocalTime.of(h, m).format(hhmm)) }, start.hour, start.minute, true).show()
                    }
                    if (at != null) CloseMark("Quitar hora") { save(ActionCatalog.AT, "") }
                }
                if (ActionCatalog.PACKAGE in keys) {
                    val app = apps.firstOrNull { it.packageName == action.packageName }
                    SettingsChip(app?.label?.uppercase() ?: if (action.packageName.isNotBlank()) action.packageName else "ELEGIR APP", on = action.packageName.isNotBlank()) { onPickApp(action) }
                }
                if (keys.isEmpty()) SubLabel("SIN AJUSTES", Modifier.align(Alignment.CenterVertically))
            }
        }
        if (ActionCatalog.PHONE in keys) Column(Modifier.padding(start = 36.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DebouncedField(action.config[ActionCatalog.PHONE].orEmpty(), "Teléfono (5215512345678) · vacío = general", KeyboardType.Phone) {
                save(ActionCatalog.PHONE, it.filter(Char::isDigit))
            }
            DebouncedField(action.config[ActionCatalog.TEXT].orEmpty(), "Mensaje · vacío = general") { save(ActionCatalog.TEXT, it) }
        }
    }
}

/** A field that saves a moment after the user stops typing, so each key press is not a write. */
@Composable
private fun DebouncedField(value: String, hint: String, keyboard: KeyboardType = KeyboardType.Text, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(value) }
    LaunchedEffect(text) {
        if (text == value) return@LaunchedEffect
        delay(700)
        onSave(text)
    }
    TextField(text, { text = it.take(200) }, hint, Modifier.fillMaxWidth(), keyboard)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DebugSection(open: Boolean, places: List<Place>, viewModel: LauncherViewModel, refresh: Int, onToggle: () -> Unit, onMessage: (String) -> Unit, onAccessibility: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Card).dashedBorder().padding(18.dp),
    ) {
        Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onToggle).semantics { contentDescription = "Pruebas" }, verticalAlignment = Alignment.CenterVertically) {
            Text("PRUEBAS", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = Gray)
            Text(if (open) "−" else "+", style = MaterialTheme.typography.labelSmall, color = Gray)
        }
        if (!open) return@Column
        Divider()
        places.forEach { place ->
            Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StrokeIcon("pin", StrokeState.OFF, 20.dp)
                Text(place.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = White)
                SettingsChip("LLEGAR", on = false) { viewModel.simulatePlace(place.id, entered = true); onMessage("Llegaste a ${place.name}") }
                SettingsChip("SALIR", on = false) { viewModel.simulatePlace(place.id, entered = false); onMessage("Saliste de ${place.name}") }
            }
        }
        Divider()
        // The general WhatsApp chat, used by any WhatsApp action left empty.
        val settings by viewModel.moduleSettings.collectAsState()
        Column(Modifier.padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SubLabel("WHATSAPP GENERAL")
            DebouncedField(settings.whatsappPhone, "Teléfono (5215512345678)", KeyboardType.Phone) { viewModel.setWhatsApp(it, settings.whatsappText) }
            DebouncedField(settings.whatsappText, "Mensaje") { viewModel.setWhatsApp(settings.whatsappPhone, it) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsChip("PAGO DE PRUEBA", on = false, icon = "pago") { viewModel.debugAddPendingPayment(); onMessage("Pago de prueba") }
            SettingsChip("PROBAR WHATSAPP", on = false, icon = "whatsapp") { if (!viewModel.openWhatsAppLeft()) onMessage("Falta el teléfono") }
            val active = refresh.let { viewModel.forcedServiceEnabled() }
            SettingsChip("ACCESIBILIDAD · ${if (active) "ACTIVO" else "APAGADO"}", on = active, onClick = onAccessibility)
        }
    }
}

/** The audio app list for a music or podcast action (labels: it is settings). */
@Composable
private fun AppSheet(title: String, apps: List<LauncherApp>, selected: String, onPick: (String) -> Unit, onClose: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    Column(
        modifier.fillMaxWidth().heightIn(max = 520.dp).clip(shape).background(Card).border(1.dp, Line, shape)
            .padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 32.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = LauncherRed)
            CloseMark("Cerrar", onClose)
        }
        LazyColumn(Modifier.padding(top = 12.dp)) {
            items(apps, key = { it.packageName }) { app ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(role = Role.Button) { onPick(app.packageName) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(app.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = White)
                    if (app.packageName == selected) Box(Modifier.size(6.dp).background(LauncherRed, CircleShape))
                }
            }
        }
    }
}

@Composable
private fun Section(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Card).border(1.dp, Line, RoundedCornerShape(24.dp)).padding(18.dp),
        content = content,
    )
}

@Composable
private fun SectionLabel(text: String) = Text(text, style = MaterialTheme.typography.labelSmall, color = Muted)

@Composable
private fun SubLabel(text: String, modifier: Modifier = Modifier) = Text(text, modifier, style = MaterialTheme.typography.labelSmall, color = Muted)

@Composable
private fun Divider() = Box(Modifier.padding(vertical = 14.dp).fillMaxWidth().size(1.dp).background(Line))

@Composable
private fun SettingsChip(label: String, on: Boolean, red: Boolean = false, icon: String? = null, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, if (red) LauncherRed else Border, RoundedCornerShape(10.dp))
            .background(if (on) Raised else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) StrokeIcon(icon, if (on) StrokeState.DONE else StrokeState.OFF, 14.dp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (red) LauncherRed else if (on) White else Muted)
    }
}

@Composable
private fun CloseMark(description: String, onClick: () -> Unit) {
    Text(
        "✕",
        Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = description }.padding(6.dp),
        style = MaterialTheme.typography.labelSmall, color = Muted,
    )
}

@Composable
private fun Arrow(glyph: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        glyph,
        Modifier.clip(CircleShape).clickable(enabled = enabled, role = Role.Button, onClick = onClick).semantics { contentDescription = description }.padding(8.dp),
        style = MaterialTheme.typography.titleMedium, color = if (enabled) Muted else Line,
    )
}

@Composable
private fun TextField(value: String, onValue: (String) -> Unit, hint: String, modifier: Modifier, keyboard: KeyboardType = KeyboardType.Text, onDone: (() -> Unit)? = null) {
    BasicTextField(
        value, onValue,
        modifier.semantics { contentDescription = hint },
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = White),
        singleLine = true,
        cursorBrush = SolidColor(LauncherRed),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        decorationBox = { inner ->
            Box(Modifier.fillMaxWidth().border(1.dp, Border, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 8.dp)) {
                if (value.isEmpty()) Text(hint, style = MaterialTheme.typography.bodyMedium, color = Gray)
                inner()
            }
        },
    )
}

@Composable
private fun DashedSlot(sizeDp: Int) {
    Canvas(Modifier.size(sizeDp.dp)) {
        val w = 1.5.dp.toPx()
        drawRoundRect(
            Color(0xFF3A3A3A), topLeft = Offset(w / 2, w / 2), size = Size(size.width - w, size.height - w),
            cornerRadius = CornerRadius(12.dp.toPx()),
            style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))),
        )
    }
}

/** The test section's dashed edge, so it reads as apart from the real settings. */
private fun Modifier.dashedBorder(): Modifier = drawBehind {
    val w = 1.dp.toPx()
    drawRoundRect(
        Border, topLeft = Offset(w / 2, w / 2), size = Size(size.width - w, size.height - w),
        cornerRadius = CornerRadius(24.dp.toPx()),
        style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx()))),
    )
}
