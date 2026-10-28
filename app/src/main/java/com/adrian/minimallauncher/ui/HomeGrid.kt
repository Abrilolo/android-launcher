package com.adrian.minimallauncher.ui

import com.adrian.minimallauncher.ui.components.RoutineWidgetData
import android.app.AlarmManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adrian.minimallauncher.data.MealKind
import com.adrian.minimallauncher.data.MealSlot
import com.adrian.minimallauncher.data.ModuleSettings
import com.adrian.minimallauncher.data.NoteRecord
import com.adrian.minimallauncher.data.PersonalRecords
import com.adrian.minimallauncher.data.WidgetStackConfig
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.feature.ai.AiApp
import com.adrian.minimallauncher.feature.ai.AiAppState
import com.adrian.minimallauncher.feature.exercise.ExerciseDay
import com.adrian.minimallauncher.feature.finance.formatPesos
import com.adrian.minimallauncher.ui.components.DotActivation
import com.adrian.minimallauncher.ui.components.DotIcon
import com.adrian.minimallauncher.ui.components.DotIcons
import com.adrian.minimallauncher.ui.components.DotMatrixText
import com.adrian.minimallauncher.ui.components.LocalReactions
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

private val spanish = Locale.forLanguageTag("es")
private val hhmm = DateTimeFormatter.ofPattern("HH:mm")
private val TileShape = RoundedCornerShape(26.dp)

/** A meal (see [ModuleSettings.meals]) checked off as a day habit, "<date>|meal_<id>". */
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
val WIDGET_IDS = listOf("clock", "water") + HOME_TILES + "ai" + "routine"

/**
 * Big dot-matrix clock and date, drawn bare (no card) like a native clock widget.
 * The dots shrink to fit when the widget is half width.
 */
@Composable
fun ClockTile(now: LocalDateTime, modifier: Modifier, onClick: () -> Unit, onLongClick: (() -> Unit)? = null, padded: Boolean = false) {
    Column(
        modifier
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = if (padded) 16.dp else 0.dp, vertical = if (padded) 14.dp else 6.dp),
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
    /** Marks or clears a day habit (meals, water) straight from a widget. */
    val onSetHabit: (String, Boolean) -> Unit = { _, _ -> },
    /** Claude, ChatGPT and Gemini on this phone, for the Asistentes IA widgets. */
    val ai: Map<AiApp, AiAppState> = emptyMap(),
    /** The current routine phase, its reminders and the most urgent thing, for the Rutinas widget. */
    val routine: RoutineWidgetData? = null,
)

/**
 * A widget page: clock and the page's widget stacks. Long press (anywhere on empty space, handled by the
 * pages host) turns on edit mode, where each stack shows its own controls.
 */
@Composable
fun HomeGrid(
    spaceName: String,
    now: LocalDateTime,
    data: HomeData,
    widgetLayout: List<WidgetStackConfig>,
    editMode: Boolean,
    onOpenModule: (String) -> Unit,
    onSettings: () -> Unit,
    onMove: (stackId: String, col: Int, row: Int) -> Unit,
    onResize: (stackId: String, w: Int, h: Int) -> Unit,
    onMerge: (draggedId: String, targetId: String) -> Unit,
    onRemoveWidget: (stackId: String, widgetId: String) -> Unit,
    onAddToStack: (stackId: String) -> Unit,
    modifier: Modifier = Modifier,
    onRemoveStack: (stackId: String) -> Unit = {},
    onSetAlpha: (stackId: String, alpha: Float) -> Unit = { _, _ -> },
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
            onMove = onMove,
            onResize = onResize,
            onMerge = onMerge,
            onRemoveWidget = onRemoveWidget,
            onAddToStack = onAddToStack,
            onRemoveStack = onRemoveStack,
            onSetAlpha = onSetAlpha,
            scroller = scroller,
        )
    }
}

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
            // The card is drawn by the stack; clip so the press highlight keeps its rounded shape.
            .clip(TileShape)
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

/** A widget's header as a dot icon instead of its name, with an optional note on the right. */
@Composable
private fun IconHeader(icon: List<String>, name: String, trailing: String? = null, trailingColor: Color = MaterialTheme.colorScheme.onSurfaceVariant, dot: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        DotIcon(icon, done = true, contentDescription = name, size = 26.dp)
        Spacer(Modifier.weight(1f))
        if (trailing != null) SmallLabel(trailing, color = trailingColor, maxLines = 1)
        if (dot) Box(Modifier.size(6.dp).background(LauncherRed, CircleShape))
    }
}

@Composable
fun AlarmTile(now: LocalDateTime, modifier: Modifier, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    val next = nextSystemAlarm()
    val left = next?.let { Duration.between(now, it) }
    val lastHour = left != null && !left.isNegative && left.toMinutes() < 60
    Tile(modifier, onClick, onLongClick) {
        IconHeader(DotIcons.campana, "Alarma", trailing = next?.let { untilText(now, it) } ?: "SIN ALARMA", trailingColor = if (lastHour) LauncherRed else MaterialTheme.colorScheme.onSurfaceVariant)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
            val d = minOf(maxWidth, maxHeight)
            AlarmRing(left, Modifier.size(d))
            DotMatrixText(next?.format(hhmm) ?: "--:--", cell = d * 0.55f / 30, color = if (lastHour) LauncherRed else MaterialTheme.colorScheme.onSurface)
        }
    }
}

/**
 * What is left until the alarm, as a ring of dots: one per hour for the next day, or in the last hour one per five
 * minutes, in red. Lit dots are what is left; it stays still and changes as time passes.
 */
@Composable
private fun AlarmRing(left: Duration?, modifier: Modifier) {
    val lastHour = left != null && !left.isNegative && left.toMinutes() < 60
    val count = if (lastHour) 12 else 24
    val lit = when {
        left == null || left.isNegative -> 0
        lastHour -> ((left.toMinutes() + 4) / 5).toInt().coerceIn(1, 12)
        else -> ((left.toMinutes() + 59) / 60).toInt().coerceAtMost(24)
    }
    val on = if (lastHour) LauncherRed else MaterialTheme.colorScheme.onSurface
    Canvas(modifier) {
        val dot = size.minDimension * 0.028f
        val radius = size.minDimension / 2 - dot
        for (i in 0 until count) {
            // From the top, clockwise.
            val angle = Math.toRadians(i * 360.0 / count - 90)
            drawCircle(
                if (i < lit) on else Color(0xFF3A3A3A), dot,
                Offset(center.x + (radius * cos(angle)).toFloat(), center.y + (radius * sin(angle)).toFloat()),
            )
        }
    }
}

@Composable
fun ExpensesTile(records: PersonalRecords, today: LocalDate, budgetCents: Long, modifier: Modifier, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    val byDay = (6 downTo 0).map { back ->
        val date = today.minusDays(back.toLong())
        records.finances.filter { it.createdAt.toLocalDate() == date }.sumOf { it.amountCents }
    }
    val max = byDay.maxOrNull()?.takeIf { it > 0 } ?: 1L
    val over = budgetCents > 0 && byDay.last() > budgetCents
    Tile(modifier, onClick, onLongClick) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            SmallLabel("GASTO HOY", color = LauncherRed, maxLines = 1)
            if (budgetCents > 0) SmallLabel("DE ${formatPesos(budgetCents)}", color = if (over) LauncherRed else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        // Past the budget the total turns red; it redraws when it changes, like on the page.
        DotMatrixText("$${byDay.last() / 100}", cell = 4.dp, color = if (over) LauncherRed else MaterialTheme.colorScheme.onSurface, reveal = byDay.last())
        // The week as small dot columns, today white: the same picture as the page.
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Bottom) {
            byDay.forEachIndexed { i, cents ->
                val filled = if (cents == 0L) 0 else ((cents * 4 + max - 1) / max).toInt()
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    for (row in 4 downTo 1) {
                        val color = when {
                            row > filled -> Color(0xFF4A4A4A)
                            i == byDay.lastIndex -> MaterialTheme.colorScheme.onSurface
                            else -> Color(0xFFA8A8A8)
                        }
                        Box(Modifier.size(5.dp).background(color, CircleShape))
                    }
                }
            }
        }
    }
}

/**
 * Dieta on the home grid, in the same dot language as its page; no title or count, the icons say it. Half width: the
 * meal whose time has come, big and centered, its time below. Full width: the meals that fit, picked by the hour (see
 * [windowStart]), main meals bigger than snacks with a few dots where the two meet. Tapping a meal marks or clears it.
 */
@Composable
fun MealsTile(habits: Set<String>, now: LocalDateTime, meals: List<MealSlot>, onSetHabit: (String, Boolean) -> Unit, modifier: Modifier, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    val reactions = LocalReactions.current
    val date = now.toLocalDate()
    val done = meals.map { mealKey(date, it.id) in habits }
    val next = done.indexOf(false)
    val count = done.count { it }
    fun toggle(i: Int) {
        val marking = !done[i]
        onSetHabit(mealKey(date, meals[i].id), marking)
        if (marking) { if (count + 1 == meals.size) reactions.done() else reactions.tick() }
    }
    Tile(modifier, onClick, onLongClick) {
        if (meals.isEmpty()) { SmallLabel("SIN COMIDAS"); return@Tile }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // Room left for the icon once the time underneath is placed.
            val iconRoom = maxHeight - 26.dp
            if (maxWidth > 220.dp) {
                val shown = minOf(meals.size, MEALS_SHOWN)
                val first = windowStart(meals.map { it.time }, now.toLocalTime(), shown)
                val main = minOf(iconRoom, maxWidth / shown - 12.dp)
                Row(Modifier.fillMaxSize(), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
                    (first until first + shown).forEach { i ->
                        val meal = meals[i]
                        if (i > first && meals[i - 1].kind != meal.kind) MealDivider()
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            DotIcon(
                                DotIcons.meal(meal.icon), done[i], "${meal.name} ${meal.time.format(hhmm)}",
                                Modifier.combinedClickable(role = Role.Checkbox, onClick = { toggle(i) }, onLongClick = onLongClick),
                                size = if (meal.kind == MealKind.MAIN) main else main * 0.67f,
                                activation = mealActivation(meal.icon),
                            )
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
            } else {
                val i = meals.indexOfLast { it.time <= now.toLocalTime() }.coerceAtLeast(0)
                val meal = meals[i]
                val big = minOf(iconRoom, maxWidth, 108.dp)
                Column(Modifier.fillMaxSize(), Arrangement.spacedBy(10.dp, Alignment.CenterVertically), Alignment.CenterHorizontally) {
                    DotIcon(
                        DotIcons.meal(meal.icon), done[i], "${meal.name} ${meal.time.format(hhmm)}",
                        Modifier.combinedClickable(role = Role.Checkbox, onClick = { toggle(i) }, onLongClick = onLongClick),
                        size = if (meal.kind == MealKind.MAIN) big else big * 0.78f,
                        activation = mealActivation(meal.icon),
                    )
                    SmallLabel(meal.time.format(hhmm), color = if (done[i]) MaterialTheme.colorScheme.onSurface else LauncherRed)
                }
            }
        }
    }
}

/** Meals that fit on the full-width Dieta widget. */
private const val MEALS_SHOWN = 4

/** Three small gray dots between main meals and snacks; lifted to sit beside the icons, not their times. */
@Composable
private fun MealDivider() {
    Column(Modifier.padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(3) { Box(Modifier.size(3.dp).background(Color(0xFF3A3A3A), CircleShape)) }
    }
}

/**
 * Where a widget's run of [shown] timed items starts: one before the item whose time has come (so a late one can
 * still be marked), then the ones coming next. As the day goes on the first ones drop off; at the end it keeps the last.
 */
fun windowStart(times: List<LocalTime>, now: LocalTime, shown: Int): Int {
    val current = times.indexOfLast { it <= now }.coerceAtLeast(0)
    return (current - 1).coerceIn(0, (times.size - shown).coerceAtLeast(0))
}

/** How a container is called on screen and which icon it uses. */
fun waterIcon(container: String) = if (container == "termo") DotIcons.termo else DotIcons.vaso
fun waterUnit(container: String) = if (container == "termo") "TERMO" else "VASO"

/** "750 ML" or "1.5 L". */
fun formatMl(ml: Int) = if (ml >= 1000) "${"%.1f".format(Locale.US, ml / 1000f).removeSuffix(".0")} L" else "$ml ML"

/** Each container's turn: spread evenly from [ModuleSettings.waterFrom] to [ModuleSettings.waterTo]. */
fun waterTimes(settings: ModuleSettings): List<LocalTime> {
    val goal = settings.waterGoal
    val span = Duration.between(settings.waterFrom, settings.waterTo).toMinutes().coerceAtLeast(0)
    return (0 until goal).map { settings.waterFrom.plusMinutes(if (goal > 1) span * it / (goal - 1) else 0) }
}

/**
 * The day's water as its own widget: no title or count, the containers say it. Only the ones whose turn is near (see
 * [windowStart]), as big as the tile allows; underneath, how much each holds and how much is drunk so far. Tapping one
 * fills up to it (each new one plays the water animation), tapping the last full one empties it.
 */
@Composable
fun WaterTile(habits: Set<String>, now: LocalDateTime, settings: ModuleSettings, onSetHabit: (String, Boolean) -> Unit, modifier: Modifier, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    val reactions = LocalReactions.current
    val date = now.toLocalDate()
    val goal = settings.waterGoal
    val ml = settings.waterGlassMl
    val unit = waterUnit(settings.waterContainer).lowercase().replaceFirstChar { it.uppercase() }
    val count = (1..goal).count { waterKey(date, it) in habits }
    // Count before the last tap, so the newly filled ones fill one after another.
    var tapFrom by remember { mutableIntStateOf(count) }
    val grid = remember(settings.waterContainer) { DotIcons.crop(waterIcon(settings.waterContainer)) }
    Tile(modifier, onClick, onLongClick) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val shown = minOf(goal, if (maxWidth > 220.dp) 6 else 4)
            val first = windowStart(waterTimes(settings), now.toLocalTime(), shown)
            val gap = 14.dp
            // The footer line and its spacing come off the height; the icons stay a bit under the room they have.
            val (cols, fit) = fitGrid(shown, grid.maxOf { it.length }, grid.size, maxWidth, maxHeight - 24.dp, gap)
            val cell = fit * 0.85f
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f).fillMaxWidth(), Arrangement.spacedBy(gap, Alignment.CenterVertically), Alignment.CenterHorizontally) {
                    (first + 1..first + shown).chunked(cols).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                            row.forEach { g ->
                                DotIcon(
                                    grid, g <= count, "$unit $g de $goal",
                                    Modifier.combinedClickable(
                                        role = Role.Button,
                                        onClick = {
                                            tapFrom = count
                                            val target = if (g == count) g - 1 else g
                                            for (n in 1..goal) onSetHabit(waterKey(date, n), n <= target)
                                            if (target > count) { if (target == goal) reactions.done() else reactions.tick() }
                                        },
                                        onLongClick = onLongClick,
                                    ),
                                    activation = DotActivation.Water,
                                    startDelayMs = (g - 1 - maxOf(tapFrom, first)).coerceAtLeast(0) * 90,
                                    cell = cell,
                                )
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    SmallLabel(formatMl(ml))
                    SmallLabel(formatMl(count * ml), color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

/**
 * Columns and dot size that make [n] icons of [gw]x[gh] dots as big as possible in [width]x[height]; on a tie, the
 * layout with fewer empty slots in its last row.
 */
private fun fitGrid(n: Int, gw: Int, gh: Int, width: Dp, height: Dp, gap: Dp): Pair<Int, Dp> {
    var best = 1 to 0.dp
    var bestEmpty = Int.MAX_VALUE
    for (cols in 1..n) {
        val rows = (n + cols - 1) / cols
        val cell = minOf((width - gap * (cols - 1)) / (cols * gw), (height - gap * (rows - 1)) / (rows * gh))
        val empty = cols * rows - n
        if (cell > best.second + 0.1.dp || ((cell - best.second).value.absoluteValue <= 0.1f && empty < bestEmpty)) {
            best = cols to cell
            bestEmpty = empty
        }
    }
    return best.first to best.second.coerceAtLeast(1.dp)
}

@Composable
fun WorldTile(zones: List<String>, modifier: Modifier, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    Tile(modifier, onClick, onLongClick) {
        IconHeader(DotIcons.globo, "Mundo")
        val cities = remoteCities(zones).take(2)
        if (cities.isEmpty()) SmallLabel("ELIGE CIUDADES EN MÓDULOS")
        val here = ZonedDateTime.now().offset.totalSeconds
        cities.forEach { city ->
            val time = ZonedDateTime.now(city.zone)
            // Sun or moon: whether it is day or night there; then how many hours ahead or behind.
            val diff = (time.offset.totalSeconds - here) / 3600
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                DotIcon(if (time.hour in 6..17) DotIcons.sol else DotIcons.luna, done = true, contentDescription = null, size = 24.dp)
                Column(Modifier.weight(1f)) {
                    SmallLabel(city.code, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                    SmallLabel(if (diff >= 0) "+$diff H" else "−${-diff} H", maxLines = 1)
                }
                DotMatrixText(time.format(hhmm), cell = 3.dp)
            }
        }
    }
}

@Composable
fun NoteTile(notes: List<NoteRecord>, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    Column(
        Modifier
            .fillMaxSize()
            .clip(TileShape)
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        IconHeader(DotIcons.alfiler, "Nota fijada", trailing = if (notes.isEmpty()) null else "${notes.size} NOTAS")
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
        left.toHours() > 0 -> "EN ${left.toHours()} H ${left.toMinutes() % 60} MIN"
        else -> "EN ${left.toMinutes()} MIN"
    }
}

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
