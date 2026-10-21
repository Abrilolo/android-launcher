package com.adrian.minimallauncher.ui

import android.app.AlarmManager
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.adrian.minimallauncher.data.PersonalRecords
import com.adrian.minimallauncher.feature.finance.formatPesos
import com.adrian.minimallauncher.feature.CardFill
import com.adrian.minimallauncher.feature.CardLine
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.feature.exercise.ExerciseDay
import com.adrian.minimallauncher.feature.exercise.ExerciseWidget
import com.adrian.minimallauncher.feature.vault.VaultWidget
import com.adrian.minimallauncher.ui.components.DotMatrixText
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

/** The four meal slots of the Tokyo guide; times come from the Dieta settings. Checked off as day habits ("<date>|meal_<i>"). */
val MEAL_NAMES = listOf("Comida completa", "Proteína + energía", "Colación preentreno", "Cena post entreno")
fun mealKey(date: LocalDate, index: Int) = "$date|meal_$index"

/** Glass [glass] (1-based) of the day's water, stored as a day habit like the meals. */
fun waterKey(date: LocalDate, glass: Int) = "$date|water_$glass"

/** Every home tile, in grid order; "notes" is the full-width one at the end. */
val HOME_TILES = listOf("alarms", "exercise", "finance", "diet", "world", "vault", "notes")

/** Data the home tiles read; each tile opens the module page with the same id. */
data class HomeData(
    val records: PersonalRecords,
    val exercise: ExerciseDay,
    val settings: ModuleSettings,
    val vaultAppCount: Int,
)

/**
 * Home: clock and a grid of tiles that answer a question without opening anything.
 * Tapping a tile opens its page; the dot-grid icon at the bottom opens the app drawer.
 */
@Composable
fun HomeGrid(
    spaceName: String,
    now: LocalDateTime,
    data: HomeData,
    onOpenModule: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SmallLabel(spaceName.uppercase(spanish), Modifier.weight(1f))
            // U+FE0E keeps the gear as a monochrome text glyph instead of a color emoji.
            IconButton(onClick = onSettings) { Text("⚙︎", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        // Dot-matrix clock; DotMatrixText shrinks the dots if the time would not fit the width.
        DotMatrixText(now.format(hhmm), Modifier.padding(vertical = 6.dp), cell = 9.dp)
        SmallLabel(now.format(DateTimeFormatter.ofPattern("EEEE d MMM", spanish)).uppercase(spanish).replace(".", ""))
        Spacer(Modifier.height(8.dp))

        // Tiles switched off in Módulos leave no gap: the rest move up in pairs.
        val hidden = data.settings.hiddenTiles
        HOME_TILES.filter { it != "notes" && it !in hidden }.chunked(2).forEach { pair ->
            TileRow {
                pair.forEach { id -> SmallTile(id, now, data, Modifier.weight(1f)) { onOpenModule(id) } }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if ("notes" !in hidden) NoteTile(data.records.notes) { onOpenModule("notes") }

        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            IconButton(onClick = onOpenDrawer) { AppsIcon(MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

/** 3×3 dots: the "all apps" glyph, in the same gray as the settings gear. */
@Composable
private fun AppsIcon(color: Color) {
    Canvas(Modifier.size(20.dp).semantics { contentDescription = "Apps" }) {
        val step = size.width / 3f
        for (row in 0 until 3) for (col in 0 until 3) {
            drawCircle(color, step * 0.22f, Offset((col + 0.5f) * step, (row + 0.5f) * step))
        }
    }
}

@Composable
private fun SmallTile(id: String, now: LocalDateTime, data: HomeData, modifier: Modifier, onClick: () -> Unit) {
    when (id) {
        "alarms" -> AlarmTile(now, modifier, onClick)
        "exercise" -> Tile(modifier, onClick) { ExerciseWidget(data.exercise, data.settings.exerciseStart) }
        "finance" -> ExpensesTile(data.records, now.toLocalDate(), data.settings.dailyBudgetCents, modifier, onClick)
        "diet" -> MealsTile(data.records.completedHabits, now, data.settings.mealTimes, modifier, onClick)
        "world" -> WorldTile(data.settings.worldZones, modifier, onClick)
        "vault" -> Tile(modifier, onClick) { VaultWidget(data.vaultAppCount) }
    }
}

@Composable
private fun TileRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().height(128.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), content = content)
}

@Composable
private fun Tile(modifier: Modifier, onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxHeight()
            .background(CardFill, TileShape)
            .border(1.dp, CardLine, TileShape)
            .clickable(role = Role.Button, onClick = onClick)
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
private fun AlarmTile(now: LocalDateTime, modifier: Modifier, onClick: () -> Unit) {
    val next = nextSystemAlarm()
    Tile(modifier, onClick) {
        TileHeader("ALARMA", dot = next != null)
        DotMatrixText(next?.format(hhmm) ?: "--:--", cell = 4.dp)
        SmallLabel(next?.let { untilText(now, it) } ?: "SIN ALARMA", maxLines = 1)
    }
}

@Composable
private fun ExpensesTile(records: PersonalRecords, today: LocalDate, budgetCents: Long, modifier: Modifier, onClick: () -> Unit) {
    val byDay = (6 downTo 0).map { back ->
        val date = today.minusDays(back.toLong())
        records.finances.filter { it.createdAt.toLocalDate() == date }.sumOf { it.amountCents }
    }
    val max = byDay.maxOrNull()?.takeIf { it > 0 } ?: 1L
    Tile(modifier, onClick) {
        TileHeader("GASTO HOY", trailing = if (budgetCents > 0) "DE ${formatPesos(budgetCents)}" else null)
        // Red only once the day's budget is passed.
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

@Composable
private fun MealsTile(habits: Set<String>, now: LocalDateTime, mealTimes: List<LocalTime>, modifier: Modifier, onClick: () -> Unit) {
    val date = now.toLocalDate()
    val done = MEAL_NAMES.indices.map { mealKey(date, it) in habits }
    val next = done.indexOf(false)
    Tile(modifier, onClick) {
        TileHeader("COMIDAS", trailing = mealTimes.getOrNull(next)?.format(hhmm))
        DotMatrixText("${done.count { it }}/4", cell = 4.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            done.forEachIndexed { i, isDone ->
                val ring = when {
                    isDone -> MaterialTheme.colorScheme.onSurface
                    i == next -> LauncherRed
                    else -> Color(0xFF555555)
                }
                Box(Modifier.size(12.dp).background(if (isDone) ring else Color.Transparent, CircleShape).border(1.5.dp, ring, CircleShape))
            }
        }
    }
}

@Composable
private fun WorldTile(zones: List<String>, modifier: Modifier, onClick: () -> Unit) {
    Tile(modifier, onClick) {
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
private fun NoteTile(notes: List<NoteRecord>, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .height(84.dp)
            .background(CardFill, TileShape)
            .border(1.dp, CardLine, TileShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        TileHeader("NOTA FIJADA", trailing = if (notes.isEmpty()) null else "${notes.size} NOTAS")
        Text(
            // The pinned note wins; with none pinned, the latest one.
            (notes.firstOrNull { it.pinned } ?: notes.firstOrNull())?.text ?: "Sin notas",
            style = MaterialTheme.typography.bodyLarge,
            color = if (notes.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

data class WorldCity(val code: String, val name: String, val zone: ZoneId)

/** Cities that can be picked for the world clock in Módulos. */
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

/** The chosen cities, in their order, minus those on the phone's own time: that one is already the big clock. */
fun remoteCities(zones: List<String>): List<WorldCity> {
    val here = ZonedDateTime.now().offset
    return zones.mapNotNull { id -> WORLD_CITIES.firstOrNull { it.zone.id == id } }
        .filter { ZonedDateTime.now(it.zone).offset != here }
}

/** Next alarm set in the phone's clock app, if any. */
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
