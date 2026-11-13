package com.adrian.minimallauncher.feature.exercise

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.feature.LauncherAccent
import com.adrian.minimallauncher.feature.LauncherDanger
import com.adrian.minimallauncher.feature.ModuleTextField
import com.adrian.minimallauncher.feature.SearchField
import com.adrian.minimallauncher.feature.finance.ActionPill
import com.adrian.minimallauncher.feature.finance.FinanceBackHeader
import com.adrian.minimallauncher.feature.finance.FinanceMuted
import com.adrian.minimallauncher.feature.finance.FinanceWhite
import com.adrian.minimallauncher.feature.finance.GlassLine
import com.adrian.minimallauncher.feature.finance.InfoIcon
import com.adrian.minimallauncher.feature.finance.LabelCream
import com.adrian.minimallauncher.feature.finance.MoneyPlenty
import com.adrian.minimallauncher.feature.finance.OptionChips
import com.adrian.minimallauncher.feature.finance.SectionLabel
import com.adrian.minimallauncher.feature.finance.TextBlue
import com.adrian.minimallauncher.feature.finance.tinted
import com.adrian.minimallauncher.ui.components.LocalReactions
import java.time.LocalDate
import java.util.UUID

/** "3 × 10 · 8 kg", "2 × 30 s" or "25 min" for an item being edited. */
internal fun RoutineItem.ask(): String = when {
    group == "cardio" -> "$reps min"
    timed -> "$sets × $reps s"
    else -> "$sets × $reps" + if (kg > 0f) " · ${kgText(kg)} kg" else ""
}

/** The color that marks a muscle group in lists. */
internal fun groupColor(group: String): Color = when (group) {
    "pecho" -> Color(0xFF72B5EA)
    "espalda" -> Color(0xFFB29AE8)
    "piernas" -> Color(0xFFFFCF7D)
    "hombros" -> Color(0xFF809FDD)
    "brazos" -> Color(0xFF62CDBD)
    "core" -> Color(0xFF8AC6A3)
    else -> Color(0xFF97CFE0)
}

/**
 * A day's routine, in reading order:
 * 1. Its name and length; "Cambiar" opens them.
 * 2. Whether changes are for this day only or every week on that weekday.
 * 3. Qué trabajas: the body with what the routine moves; a tap on a muscle lights the movements that work it.
 * 4. The movements, one short row each; a tap opens a short sheet to change it. Agregar opens a search.
 * 5. Guardar and, today, Iniciar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RoutineView(
    date: LocalDate,
    today: LocalDate,
    session: Session,
    lastKg: (String) -> Float?,
    started: Boolean,
    done: Boolean,
    savedOnlyForDay: Boolean,
    onSave: (RoutineDraft, Boolean) -> Unit,
    onStart: () -> Unit,
    onOpenMap: (List<RoutineItem>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reactions = LocalReactions.current
    val saved = remember(session, date) { RoutinePlans.draftOf(session, lastKg) }
    var draft by remember(saved) { mutableStateOf(saved) }
    var onlyThisDay by rememberSaveable(date.toString()) { mutableStateOf(savedOnlyForDay) }
    var renaming by rememberSaveable { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    val dirty = draft != saved || onlyThisDay != savedOnlyForDay
    val dayName = DayNames[date.dayOfWeek.value - 1]

    if (adding) {
        AddExercises(
            have = draft.items.map { it.id }.toSet(),
            onAdd = { picks -> draft = draft.copy(items = draft.items + picks); adding = false; reactions.tick() },
            onNew = {
                val item = RoutineItem("u_${UUID.randomUUID()}", "", "pecho", 3, 10, 0f, false)
                draft = draft.copy(items = draft.items + item)
                adding = false
                editing = item.id
            },
            onBack = { adding = false },
            modifier = modifier,
        )
        return
    }
    BackHandler(onBack = onBack)

    Column(modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            FinanceBackHeader("$dayName ${date.dayOfMonth}", onBack)

            // 1. Name and length
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(draft.name.ifBlank { "Sin nombre" }, style = MaterialTheme.typography.headlineSmall, color = FinanceWhite)
                        val meta = listOfNotNull(
                            draft.items.size.takeIf { it > 0 }?.let { if (it == 1) "1 ejercicio" else "$it ejercicios" } ?: "Descanso",
                            draft.minutes.takeIf { it > 0 }?.let { "$it min" },
                        ).joinToString(" · ")
                        Text(meta, style = MaterialTheme.typography.bodySmall, color = LabelCream)
                    }
                    com.adrian.minimallauncher.feature.finance.FinIcon(if (renaming) "listo" else "lapiz", if (renaming) "Listo" else "Cambiar nombre y duración") { renaming = !renaming }
                }
                if (renaming) {
                    ModuleTextField(draft.name, { draft = draft.copy(name = it.take(40)) }, "Nombre de la rutina", Modifier.fillMaxWidth())
                    MiniStepper("Minutos", if (draft.minutes > 0) "${draft.minutes}" else "—", Modifier.fillMaxWidth(),
                        { draft = draft.copy(minutes = (draft.minutes - 5).coerceAtLeast(0)) },
                        { draft = draft.copy(minutes = (draft.minutes + 5).coerceAtMost(180)) })
                }
            }

            // 2. For whom
            Row(Modifier.fillMaxWidth().selectableGroup().border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp)).padding(4.dp)) {
                listOf(false to "Todos los ${dayName.lowercase()}", true to "Solo este día").forEach { (only, label) ->
                    val on = onlyThisDay == only
                    Box(
                        Modifier.weight(1f).heightIn(min = 44.dp)
                            .then(if (on) Modifier.border(1.dp, LauncherAccent, RoundedCornerShape(12.dp)) else Modifier)
                            .selectable(on, role = Role.RadioButton) { onlyThisDay = only },
                        contentAlignment = Alignment.Center,
                    ) { Text(label, style = MaterialTheme.typography.labelLarge, color = if (on) FinanceWhite else LabelCream, maxLines = 1) }
                }
            }

            // 3. Qué trabajas
            if (draft.items.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("Qué trabajas")
                    InfoIcon("Azul claro: lo que más trabaja la rutina. Azul oscuro: lo que ayuda. Gris: descansa. Toca un músculo para ver qué ejercicios lo trabajan.")
                }
                val heat = heatOf(draft.items.map { it.muscles() })
                Row(
                    Modifier.fillMaxWidth().tinted(TextBlue, radius = 20.dp).padding(horizontal = 12.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
                ) {
                    MuscleMap(heat, height = 168.dp, selected = filter, labels = true) { k -> filter = if (filter == k) null else k }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val f = filter
                        if (f == null) Text("Toca un músculo para ver qué ejercicios lo trabajan.", style = MaterialTheme.typography.bodySmall, color = LabelCream)
                        else {
                            val count = draft.items.count { (it.muscles()[f] ?: 0) > 0 }
                            Text(MuscleNames[f] ?: f, style = MaterialTheme.typography.titleMedium, color = FinanceWhite)
                            Text(if (count == 0) "Descansa" else if (count == 1) "1 ejercicio" else "$count ejercicios", style = MaterialTheme.typography.bodySmall, color = LabelCream)
                            TextLink("Quitar filtro") { filter = null }
                        }
                    }
                }
                ActionPill("Ver músculos en grande", null, Modifier.fillMaxWidth(), primary = false) { onOpenMap(draft.items) }
            }

            // 4. Movements
            Column {
                SectionLabel("Ejercicios")
                if (draft.items.isEmpty()) Text("Día de descanso", Modifier.padding(vertical = 12.dp), style = MaterialTheme.typography.bodyMedium, color = FinanceMuted)
                draft.items.forEach { item ->
                    val level = filter?.let { item.muscles()[it] ?: 0 } ?: PRIMARY
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Button) { editing = item.id }
                            .semantics(mergeDescendants = true) { contentDescription = "${item.name}, ${item.ask()}" }
                            .then(if (filter != null && level == 0) Modifier.background(Color.Transparent) else Modifier),
                        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val dim = filter != null && level == 0
                        Box(
                            Modifier.size(8.dp).then(
                                when {
                                    filter == null -> Modifier.background(groupColor(item.group), CircleShape)
                                    level >= PRIMARY -> Modifier.background(MusclePrimary, CircleShape)
                                    level == HELPS -> Modifier.border(1.5.dp, MusclePrimary, CircleShape)
                                    else -> Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
                                },
                            ),
                        )
                        Text(item.name.ifBlank { "Sin nombre" }, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
                            color = if (dim) FinanceMuted.copy(alpha = 0.45f) else FinanceWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(item.ask(), style = MaterialTheme.typography.bodyMedium, color = if (dim) LabelCream.copy(alpha = 0.4f) else LabelCream)
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(GlassLine))
                }
                Box(
                    Modifier.padding(top = 12.dp).fillMaxWidth().height(48.dp).tinted(TextBlue, radius = 16.dp, dashed = true)
                        .clickable(role = Role.Button) { adding = true },
                    contentAlignment = Alignment.Center,
                ) { Text("+ Agregar ejercicios", style = MaterialTheme.typography.labelLarge, color = TextBlue) }
            }
        }

        // 5. Guardar · Iniciar
        Box(Modifier.fillMaxWidth().height(1.dp).background(GlassLine))
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionPill(if (dirty) "Guardar" else "Guardado", null, Modifier.weight(1f), primary = false, enabled = dirty && draft.items.none { it.name.isBlank() }) {
                onSave(draft, onlyThisDay)
                reactions.tick()
            }
            if (date == today && draft.items.isNotEmpty()) ActionPill(
                when {
                    done -> "Ver sesión"
                    started -> "Seguir"
                    else -> "Iniciar"
                },
                "gimnasio", Modifier.weight(1.4f),
            ) {
                if (dirty) onSave(draft, onlyThisDay)
                onStart()
            }
        }
    }

    editing?.let { id ->
        val item = draft.items.firstOrNull { it.id == id }
        if (item == null) editing = null
        else ModalBottomSheet(
            onDismissRequest = { editing = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF0E0E0E),
            scrimColor = com.adrian.minimallauncher.feature.finance.SheetScrim,
            tonalElevation = 0.dp,
        ) {
            val index = draft.items.indexOfFirst { it.id == id }
            ItemSheet(
                item = item,
                lastKg = lastKg(item.id),
                canUp = index > 0,
                canDown = index < draft.items.lastIndex,
                onChange = { update -> draft = draft.copy(items = draft.items.map { if (it.id == id) update(it) else it }) },
                onMove = { step ->
                    val list = draft.items.toMutableList()
                    val to = (index + step).coerceIn(0, list.lastIndex)
                    list.add(to, list.removeAt(index))
                    draft = draft.copy(items = list)
                },
                onDelete = { draft = draft.copy(items = draft.items.filter { it.id != id }); editing = null },
                onDone = { editing = null },
            )
        }
    }
}

/** A short number control: its name, the number big, − and +. */
@Composable
internal fun MiniStepper(label: String, value: String, modifier: Modifier = Modifier, onMinus: () -> Unit, onPlus: () -> Unit) {
    Column(
        modifier.border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp)).padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SectionLabel(label)
        Text(value, style = MaterialTheme.typography.headlineSmall, color = FinanceWhite, textAlign = TextAlign.Center, maxLines = 1)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("−" to onMinus, "+" to onPlus).forEach { (symbol, action) ->
                Box(
                    Modifier.size(width = 44.dp, height = 36.dp).border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button, onClick = action).semantics { contentDescription = (if (symbol == "+") "Más " else "Menos ") + label },
                    contentAlignment = Alignment.Center,
                ) { Text(symbol, style = MaterialTheme.typography.titleLarge, color = FinanceWhite) }
            }
        }
    }
}

/** The sheet of one movement: its name, the numbers in one row, how it is measured, what it works, and order. */
@Composable
private fun ItemSheet(
    item: RoutineItem,
    lastKg: Float?,
    canUp: Boolean,
    canDown: Boolean,
    onChange: ((RoutineItem) -> RoutineItem) -> Unit,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit,
    onDone: () -> Unit,
) {
    val cardio = item.group == "cardio"
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ModuleTextField(item.name, { v -> onChange { it.copy(name = v.take(40)) } }, "Nombre del ejercicio", Modifier.weight(1f))
            if (item.how.isNotBlank()) InfoIcon(listOf(item.how, item.care).filter { it.isNotBlank() }.distinct().joinToString("\n\n"))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!cardio) MiniStepper("Series", "${item.sets}", Modifier.weight(1f),
                { onChange { it.copy(sets = (it.sets - 1).coerceAtLeast(1)) } }, { onChange { it.copy(sets = (it.sets + 1).coerceAtMost(10)) } })
            val step = if (cardio || item.timed) 5 else 1
            MiniStepper(if (cardio) "Min" else if (item.timed) "Seg" else "Reps", "${item.reps}", Modifier.weight(1f),
                { onChange { it.copy(reps = (it.reps - step).coerceAtLeast(1)) } }, { onChange { it.copy(reps = (it.reps + step).coerceAtMost(if (item.timed) 600 else 100)) } })
            if (!cardio && !item.timed) MiniStepper("Kg", kgText(item.kg), Modifier.weight(1f),
                { onChange { it.copy(kg = (it.kg - 1f).coerceAtLeast(0f)) } }, { onChange { it.copy(kg = (it.kg + 1f).coerceAtMost(300f)) } })
        }
        if (!cardio) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("Se mide por")
            OptionChips(listOf("reps" to "Repeticiones", "time" to "Tiempo"), if (item.timed) "time" else "reps") { mode ->
                onChange { it.copy(timed = mode == "time", reps = if ((mode == "time") != it.timed) (if (mode == "time") 30 else 10) else it.reps) }
            }
        }
        val muscles = item.muscles()
        Row(
            Modifier.fillMaxWidth().tinted(TextBlue, radius = 18.dp).padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            MuscleMap(muscles, height = 84.dp, labels = false)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionLabel("Trabaja")
                namesAt(muscles, PRIMARY).takeIf { it.isNotEmpty() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MusclePrimary) }
                namesAt(muscles, HELPS).takeIf { it.isNotEmpty() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = FinanceWhite) }
            }
        }
        if (item.id.startsWith("u_")) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("Grupo")
            OptionChips(MuscleGroups.map { (key, name) -> key to name }, item.group) { group ->
                onChange { it.copy(group = group, timed = if (group == "cardio") true else it.timed, sets = if (group == "cardio") 1 else it.sets) }
            }
        }
        lastKg?.takeIf { it > 0f && !item.timed }?.let {
            Text("La última vez usaste ${kgText(it)} kg.", style = MaterialTheme.typography.bodySmall, color = FinanceMuted)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Quitar",
                Modifier.heightIn(min = 52.dp).border(1.dp, LauncherDanger.copy(alpha = 0.7f), RoundedCornerShape(26.dp))
                    .clickable(role = Role.Button, onClick = onDelete).padding(horizontal = 20.dp, vertical = 15.dp),
                style = MaterialTheme.typography.labelLarge, color = LauncherDanger,
            )
            if (canUp) Text(
                "Subir",
                Modifier.heightIn(min = 52.dp).border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(26.dp))
                    .clickable(role = Role.Button) { onMove(-1) }.padding(horizontal = 16.dp, vertical = 15.dp),
                style = MaterialTheme.typography.labelLarge, color = FinanceWhite,
            )
            if (canDown) Text(
                "Bajar",
                Modifier.heightIn(min = 52.dp).border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(26.dp))
                    .clickable(role = Role.Button) { onMove(1) }.padding(horizontal = 18.dp, vertical = 15.dp),
                style = MaterialTheme.typography.labelLarge, color = FinanceWhite,
            )
            ActionPill("Listo", null, Modifier.weight(1f), onClick = onDone)
        }
    }
}

/**
 * Agregar: a search, the groups as chips, and the movements as tiles in two columns; several can be picked at once.
 * "Crear uno nuevo" adds a blank one to fill.
 */
@Composable
private fun AddExercises(have: Set<String>, onAdd: (List<RoutineItem>) -> Unit, onNew: () -> Unit, onBack: () -> Unit, modifier: Modifier) {
    BackHandler(onBack = onBack)
    var query by rememberSaveable { mutableStateOf("") }
    var group by rememberSaveable { mutableStateOf("todos") }
    val picked = remember { mutableStateListOf<String>() }
    val shown = ExerciseCatalog.filter { it.id !in have }
        .filter { group == "todos" || it.group == group }
        .filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }
    Column(modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 24.dp).padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FinanceBackHeader("Agregar", onBack)
            SearchField(query, { query = it.take(40) }, Modifier.fillMaxWidth())
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (listOf("todos" to "Todos") + MuscleGroups.map { (k, v) -> k to v }).forEach { (key, name) ->
                val on = key == group
                Text(
                    name,
                    Modifier.border(1.dp, if (on) LauncherAccent else Color.White.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
                        .clickable(role = Role.RadioButton) { group = key }.padding(horizontal = 14.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge, color = if (on) FinanceWhite else LabelCream,
                )
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (shown.isEmpty()) Text("No hay ejercicios con ese nombre", Modifier.padding(vertical = 12.dp), style = MaterialTheme.typography.bodyMedium, color = FinanceMuted)
            shown.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { ex ->
                        val on = ex.id in picked
                        Column(
                            Modifier.weight(1f).heightIn(min = 84.dp)
                                .border(1.dp, if (on) MoneyPlenty else Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                                .clickable(role = Role.Checkbox) { if (on) picked.remove(ex.id) else picked.add(ex.id) }
                                .semantics { contentDescription = ex.name + if (on) ", elegido" else "" }
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(ex.name, style = MaterialTheme.typography.bodyMedium, color = FinanceWhite, maxLines = 2)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                                Box(Modifier.size(8.dp).background(groupColor(ex.group), CircleShape))
                                Text(MuscleGroups[ex.group] ?: ex.group, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = FinanceMuted)
                                Text(if (on) "✓" else "+", style = MaterialTheme.typography.titleMedium, color = if (on) MoneyPlenty else TextBlue)
                            }
                        }
                    }
                    if (pair.size == 1) Box(Modifier.weight(1f))
                }
            }
            TextLink("+ Crear uno nuevo", onClick = onNew)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(GlassLine))
        ActionPill(
            when (picked.size) {
                0 -> "Elige uno o varios"
                1 -> "Agregar 1 ejercicio"
                else -> "Agregar ${picked.size} ejercicios"
            },
            null, Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), enabled = picked.isNotEmpty(),
        ) {
            onAdd(picked.mapNotNull { id -> ExerciseCatalog.firstOrNull { it.id == id } }.map { ex ->
                RoutineItem(ex.id, ex.name, ex.group, ex.sets, ex.reps, 0f, ex.timed, ex.how)
            })
        }
    }
}
