package com.adrian.minimallauncher.ui

import android.app.AlarmManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adrian.minimallauncher.data.NoteRecord
import com.adrian.minimallauncher.data.ModuleSettings
import com.adrian.minimallauncher.data.MealKind
import com.adrian.minimallauncher.data.MealSlot
import com.adrian.minimallauncher.data.PersonalRecords
import com.adrian.minimallauncher.data.WidgetStackConfig
import com.adrian.minimallauncher.feature.finance.formatPesos
import com.adrian.minimallauncher.feature.CardFill
import com.adrian.minimallauncher.feature.CardLine
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.feature.exercise.ExerciseDay
import com.adrian.minimallauncher.feature.exercise.ExerciseWidget
import com.adrian.minimallauncher.feature.vault.VaultWidget
import com.adrian.minimallauncher.ui.components.DotMatrixText
import com.adrian.minimallauncher.ui.components.DotActivation
import com.adrian.minimallauncher.ui.components.DotIcons
import com.adrian.minimallauncher.ui.components.DotIcon
import androidx.compose.foundation.layout.BoxWithConstraints
import com.adrian.minimallauncher.ui.components.WidgetStackTile
import com.adrian.minimallauncher.ui.components.WIDGET_LABELS
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val spanish = Locale.forLanguageTag("es")
private val hhmm = DateTimeFormatter.ofPattern("HH:mm")
private val TileShape = RoundedCornerShape(26.dp)

/** A meal (see ModuleSettings.meals) checked off as a day habit, "<date>|meal_<id>". */
fun mealKey(date: LocalDate, id: String) = "$date|meal_$id"

/** Daily habits from the guide, checked off as "<date>|habit_<id>" (keys kept from the old insights page). */
val DIET_HABITS = listOf(
    "walk" to "Caminar 10-15 min después de comer",
    "no_extras" to "Sin refrescos, azúcar ni frituras",
    "protein" to "Meta de proteína (~1.6 g/kg)",
    "sleep" to "Dormir 7-9 h",
)
fun habitKey(date: LocalDate, id: String) = "$date|habit_$id"

/** Glass [glass] (1-based) of the day's water, stored as a day habit like the meals. */
fun waterKey(date: LocalDate, glass: Int) = "$date|water_$glass"

/** Every home tile, in grid order; "notes" is the full-width one at the end. */
val HOME_TILES = listOf("alarms", "exercise", "finance", "diet", "world", "vault", "notes")

/** Everything the widget catalog offers: the module tiles plus the big clock and the water glasses (part of Dieta). */
val WIDGET_IDS = listOf("clock", "water") + HOME_TILES

/**
 * Big dot-matrix clock and date, drawn bare (no card) like a native clock widget.
 * The dots shrink to fit when the widget is half width.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClockTile(now: LocalDateTime, modifier: Modifier, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    Column(
        modifier
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        DotMatrixText(now.format(hhmm), cell = 9.dp)
        SmallLabel(now.format(DateTimeFormatter.ofPattern("EEEE d MMM", spanish)).uppercase(spanish).replace(".", ""))
    }
}

/** Data the home tiles read; each tile opens the module page with the same id. */
data class HomeData(
    val records: PersonalRecords,
    val exercise: ExerciseDay,
    val settings: ModuleSettings,
    val vaultAppCount: Int,
)

/**
 * A widget page: clock and the page's widget stacks. Long press (anywhere, handled by the
 * pages host) turns on edit mode, where each stack shows its own controls.
 */
@Composable
fun HomeGrid(
    spaceName: String,
    now: LocalDateTime,
    data: HomeData,
    widgetLayout: List<WidgetStackConfig>,
    editMode: Boolean,
    onEnterEdit: () -> Unit,
    onOpenModule: (String) -> Unit,
    onSettings: () -> Unit,
    onMove: (stackId: String, col: Int, row: Int) -> Unit,
    onResize: (stackId: String, w: Int, h: Int) -> Unit,
    onMerge: (draggedId: String, targetId: String) -> Unit,
    onRemoveWidget: (stackId: String, widgetId: String) -> Unit,
    onAddToStack: (stackId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    val scroller = rememberEdgeScroller(scroll)
    Column(
        modifier
            .fillMaxSize()
            .onGloballyPositioned { scroller.viewport = it }
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SmallLabel(spaceName.uppercase(spanish), Modifier.weight(1f))
            // U+FE0E keeps the gear as a monochrome text glyph instead of a color emoji.
            IconButton(onClick = onSettings) { Text("⚙︎", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        // The big clock is a widget now ("Reloj"), so each page decides whether it has one.

        if (widgetLayout.isEmpty()) {
            SmallLabel(
                if (editMode) "PÁGINA VACÍA · TOCA WIDGETS ABAJO PARA AGREGAR" else "PÁGINA VACÍA · MANTÉN PRESIONADO PARA AGREGAR WIDGETS",
                Modifier.padding(vertical = 24.dp),
            )
        }
        WidgetGridView(
            stacks = widgetLayout,
            editMode = editMode,
            data = data,
            now = now,
            onOpenModule = onOpenModule,
            onEnterEdit = onEnterEdit,
            onMove = onMove,
            onResize = onResize,
            onMerge = onMerge,
            onRemoveWidget = onRemoveWidget,
            onAddToStack = onAddToStack,
            scroller = scroller,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Tile(
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxHeight()
            .background(CardFill, TileShape)
            .border(1.dp, CardLine, TileShape)
            .combinedClickable(
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        content = content,
    )
}

@Composable
private fun TileHeader(label: String, trailing: String? = null, dot: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SmallLabel(label, Modifier.weight(1f), maxLines = 1)
        if (trailing != null) SmallLabel(trailing, maxLines = 1)
        if (dot) Box(Modifier.size(6.dp).background(LauncherRed, CircleShape))
    }
}

@Composable
fun AlarmTile(now: LocalDateTime, modifier: Modifier, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    val next = nextSystemAlarm()
    Tile(modifier, onClick, onLongClick) {
        TileHeader("ALARMA", dot = next != null)
        DotMatrixText(next?.format(hhmm) ?: "--:--", cell = 4.dp)
        SmallLabel(next?.let { untilText(now, it) } ?: "SIN ALARMA", maxLines = 1)
    }
}

@Composable
fun ExpensesTile(records: PersonalRecords, today: LocalDate, budgetCents: Long, modifier: Modifier, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    val byDay = (6 downTo 0).map { back ->
        val date = today.minusDays(back.toLong())
        records.finances.filter { it.createdAt.toLocalDate() == date }.sumOf { it.amountCents }
    }
    val max = byDay.maxOrNull()?.takeIf { it > 0 } ?: 1L
    Tile(modifier, onClick, onLongClick) {
        TileHeader("GASTO HOY", trailing = if (budgetCents > 0) "DE ${formatPesos(budgetCents)}" else null)
        val over = budgetCents > 0 && byDay.last() > budgetCents
        DotMatrixText("$${byDay.last() / 100}", cell = 4.dp, color = if (over) LauncherRed else MaterialTheme.colorScheme.onSurface)
        Row(Modifier.height(20.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
            byDay.forEachIndexed { i, cents ->
                val color = if (i == byDay.lastIndex) MaterialTheme.colorScheme.onSurface else Color(0xFF3A3A3A)
                Box(Modifier.width(6.dp).fillMaxHeight((cents.toFloat() / max).coerceIn(0.25f, 1f)).background(color, RoundedCornerShape(3.dp)))
            }
        }
    }
}

/**
 * Dieta on the home grid, in the same dot language as its page. Half width: the next meal big, its time in red and the
 * meal count. Full width: every meal with its time, main meals bigger than snacks. Water has its own widget.
 */
@Composable
fun MealsTile(habits: Set<String>, now: LocalDateTime, meals: List<MealSlot>, modifier: Modifier, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    val date = now.toLocalDate()
    val done = meals.map { mealKey(date, it.id) in habits }
    val next = done.indexOf(false)
    val count = done.count { it }
    val red = mapOf('/' to LauncherRed)
    Tile(modifier, onClick, onLongClick) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            if (maxWidth > 220.dp) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        SmallLabel("COMIDAS", color = LauncherRed)
                        DotMatrixText("$count/${meals.size}", cell = 3.dp, accents = red, reveal = count)
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                        meals.forEachIndexed { i, meal ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                DotIcon(DotIcons.meal(meal.icon), done[i], null, size = if (meal.kind == MealKind.MAIN) 38.dp else 28.dp, activation = mealActivation(meal.icon))
                                SmallLabel(
                                    meal.time.format(hhmm),
                                    color = when {
                                        done[i] -> MaterialTheme.colorScheme.onSurface
                                        i == next -> LauncherRed
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        }
                    }
                }
            } else {
                val nextMeal = meals.getOrNull(next)
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        SmallLabel(nextMeal?.time?.format(hhmm) ?: "LISTO", color = LauncherRed)
                        DotMatrixText("$count/${meals.size}", cell = 3.dp, accents = red, reveal = count)
                    }
                    DotIcon(
                        DotIcons.meal(nextMeal?.icon ?: "comida"), nextMeal == null, null, size = 64.dp,
                        activation = mealActivation(nextMeal?.icon ?: "comida"),
                    )
                }
            }
        }
    }
}

/** How a container is called on screen and which icon it uses. */
fun waterIcon(container: String) = if (container == "termo") DotIcons.termo else DotIcons.vaso
fun waterUnit(container: String) = if (container == "termo") "TERMO" else "VASO"

/** "750 ML" or "1.5 L". */
fun formatMl(ml: Int) = if (ml >= 1000) "${"%.1f".format(Locale.US, ml / 1000f).removeSuffix(".0")} L" else "$ml ML"

/**
 * The day's water as its own widget: one glass (or thermos) per goal step, the count in dots and, underneath, how much
 * each one holds and how much is drunk so far. Tapping opens Dieta, where the glasses are marked.
 */
@Composable
fun WaterTile(habits: Set<String>, now: LocalDateTime, settings: ModuleSettings, modifier: Modifier, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    val date = now.toLocalDate()
    val goal = settings.waterGoal
    val water = (1..goal).count { waterKey(date, it) in habits }
    Tile(modifier, onClick, onLongClick) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // Two rows when the tile is half width, so the glasses stay big enough to read.
            val rows = if (maxWidth > 220.dp || goal <= 4) 1 else 2
            val perRow = (goal + rows - 1) / rows
            val gap = 4.dp
            val glass = minOf(30.dp, (maxWidth - gap * (perRow - 1)) / perRow)
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    SmallLabel("AGUA", color = LauncherRed)
                    DotMatrixText("$water/$goal", cell = 3.dp, accents = mapOf('/' to LauncherRed), reveal = water)
                }
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    (1..goal).chunked(perRow).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                            row.forEach { g -> DotIcon(waterIcon(settings.waterContainer), g <= water, null, size = glass, activation = DotActivation.Water) }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    SmallLabel(formatMl(settings.waterGlassMl))
                    SmallLabel(formatMl(water * settings.waterGlassMl), color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
fun WorldTile(zones: List<String>, modifier: Modifier, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    Tile(modifier, onClick, onLongClick) {
        TileHeader("MUNDO")
        val cities = remoteCities(zones).take(2)
        if (cities.isEmpty()) SmallLabel("ELIGE CIUDADES EN MÓDULOS")
        cities.forEach { city ->
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                SmallLabel(city.code, color = MaterialTheme.colorScheme.onSurface)
                DotMatrixText(ZonedDateTime.now(city.zone).format(hhmm), cell = 3.dp)
            }
        }
    }
}

@Composable
fun NoteTile(notes: List<NoteRecord>, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    Column(
        Modifier
            .fillMaxSize()
            .background(CardFill, TileShape)
            .border(1.dp, CardLine, TileShape)
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        TileHeader("NOTA FIJADA", trailing = if (notes.isEmpty()) null else "${notes.size} NOTAS")
        Text(
            (notes.firstOrNull { it.pinned } ?: notes.firstOrNull())?.text ?: "Sin notas",
            style = MaterialTheme.typography.bodyLarge,
            color = if (notes.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

data class WorldCity(val code: String, val name: String, val zone: ZoneId)

val WORLD_CITIES = listOf(
    WorldCity("CDMX", "Ciudad de México", ZoneId.of("America/Mexico_City")),
    WorldCity("NYC", "Nueva York", ZoneId.of("America/New_York")),
    WorldCity("LAX", "Los Ángeles", ZoneId.of("America/Los_Angeles")),
    WorldCity("BOG", "Bogotá", ZoneId.of("America/Bogota")),
    WorldCity("BUE", "Buenos Aires", ZoneId.of("America/Argentina/Buenos_Aires")),
    WorldCity("MAD", "Madrid", ZoneId.of("Europe/Madrid")),
    WorldCity("LON", "Londres", ZoneId.of("Europe/London")),
    WorldCity("PAR", "París", ZoneId.of("Europe/Paris")),
    WorldCity("TYO", "Tokio", ZoneId.of("Asia/Tokyo")),
    WorldCity("SEL", "Seúl", ZoneId.of("Asia/Seoul")),
)

fun remoteCities(zones: List<String>): List<WorldCity> {
    val here = ZonedDateTime.now().offset
    return zones.mapNotNull { id -> WORLD_CITIES.firstOrNull { it.zone.id == id } }
        .filter { ZonedDateTime.now(it.zone).offset != here }
}

@Composable
fun nextSystemAlarm(): LocalDateTime? {
    val alarmManager = LocalContext.current.getSystemService(AlarmManager::class.java)
    return alarmManager?.nextAlarmClock?.triggerTime?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()) }
}

fun untilText(now: LocalDateTime, target: LocalDateTime): String {
    val left = Duration.between(now, target)
    return when {
        left.isNegative -> "AHORA"
        left.toHours() >= 24 -> target.format(DateTimeFormatter.ofPattern("EEE HH:mm", spanish)).uppercase(spanish)
        left.toHours() > 0 -> "EN ${left.toHours()} H ${left.toMinutesPart()} MIN"
        else -> "EN ${left.toMinutes()} MIN"
    }
}

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
