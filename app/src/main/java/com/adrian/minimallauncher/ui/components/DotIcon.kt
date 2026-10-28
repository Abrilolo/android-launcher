package com.adrian.minimallauncher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.feature.LauncherRed
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay

/**
 * Dot-matrix icons, the same language as docs/design/concept/DietaVisual.html and docs/design/PLAN_VISUAL.md.
 * '#' white · 'r' red · 's' red steam · 'w' white steam · 'o' inner fill (only when marked) · '.' empty ·
 * 'b' a piece that is gone once marked (bitten off) · 'k' a dot that only shows once marked (the bite's edge) ·
 * 'u' a padlock's shackle, which lifts when it opens.
 * Not marked, every dot is a visible gray; marked, white with its red details.
 * Comida, snack and cena were drawn by the user.
 */
object DotIcons {
    val comida = listOf(
        "...............",
        "..#.#.#..##....",
        "..#.#.#.###....",
        "..#.#.#.###....",
        "..#.#.#.###....",
        "...###..###....",
        "....#....##....",
        "....#.....#....",
        "....#.....#....",
        "....#.....#....",
        "....#.....#....",
        "....#.....#....",
        "....#.....#....",
        "....#.....#....",
        "....#.....#....",
    )
    /** A bitten chocolate bar in its torn wrapper ('b' is the piece the bite takes, 'k' the bitten edge left behind). */
    val snack = listOf(
        "...#######bbb..",
        "...#..#..#k.b..",
        "...#..#..#.kb..",
        "...##########..",
        "...#..#..#..#..",
        "...#..#..#.rr..",
        "...#..#.rrr.r..",
        "...#.rrr....r..",
        "...rr.......r..",
        "...r........r..",
        "...r.######.r..",
        "...r........r..",
        "...rrrrrrrrrr..",
        "...r.r.r.r.r...",
        "...............",
    )
    val colacion = listOf(
        ".......#.......",
        "......#.rrrr...",
        "...####.####...",
        "..#....#....#..",
        ".#...........#.",
        ".#...........#.",
        ".#...........#.",
        ".#...........#.",
        ".#...........#.",
        ".#...........#.",
        "..#.........#..",
        "...#.......#...",
        "....##...##....",
        "......###......",
        "...............",
    )
    val cena = listOf(
        "...s...........",
        "...s...........",
        "s..w.......s..s",
        "s.....rrr..s..w",
        ".w....rrr..w.s.",
        ".s..#######..w.",
        "...#########.s.",
        "..##.......##..",
        ".##.........##.",
        ".#...........#.",
        ".#...........#.",
        "##...........##",
        "###############",
        ".rrrrrrrrrrrrr.",
        "...............",
    )
    val vaso = listOf(
        "##...........##",
        ".#ooooooooooo#.",
        ".#ooooooooooo#.",
        "..#ooooooooo#..",
        "..#ooooooooo#..",
        "..#ooooooooo#..",
        "..#ooooooooo#..",
        "...#ooooooo#...",
        "...#ooooooo#...",
        "...#ooooooo#...",
        "...#ooooooo#...",
        "....#ooooo#....",
        "....#ooooo#....",
        "....#######....",
        "...............",
    )
    /** Cap, red bands and a side handle, scaled down from the user's 32×32 drawing. */
    val termo = listOf(
        ".....##........",
        "....####.......",
        "...######......",
        "...rrrrrr......",
        "...#oooo#......",
        "...#oooo###....",
        "...#oooo#.#....",
        "...rrrrrr.#....",
        "...#oooo#.#....",
        "...#oooo#.#....",
        "...rrrrrr.#....",
        "...#oooo###....",
        "...#oooo#......",
        "...rrrrrr......",
        "....####.......",
    )
    val cartera = listOf(".........", "#######..", "#......#.", "#....####", "#....#rr#", "#....####", "#......#.", "########.", ".........")
    // Widget headers and small signs (9×9).
    val campana = listOf("....#....", "...###...", "..#...#..", "..#...#..", "..#...#..", ".#.....#.", "#########", "....r....", ".........")
    val globo = listOf("..#####..", ".#.#.#.#.", "#..#.#..#", "#########", "#..#.#..#", "#########", "#..#.#..#", ".#.#.#.#.", "..#####..")
    /** The Bancos widget's padlock at full size, so its dots match the other widgets' icons; 'u' is the shackle. */
    val candadoGrande = listOf(
        "...............",
        ".....uuuuu.....",
        "....uu...uu....",
        "....u.....u....",
        "....u.....u....",
        "....u.....u....",
        "..###########..",
        "..#.........#..",
        "..#.........#..",
        "..#...rrr...#..",
        "..#...rrr...#..",
        "..#....r....#..",
        "..#....r....#..",
        "..###########..",
        "...............",
    )
    val candado = listOf("...###...", "..#...#..", "..#...#..", ".#######.", ".#.....#.", ".#..r..#.", ".#..r..#.", ".#.....#.", ".#######.")
    val alfiler = listOf("..#####..", "...#.#...", "...#.#...", "..#...#..", ".#######.", "....r....", "....r....", "....r....", ".........")
    val pesa = listOf(".........", ".#.....#.", "##.....##", "##.....##", "#########", "##.....##", "##.....##", ".#.....#.", ".........")
    val relojArena = listOf("#########", ".#.....#.", "..#rrr#..", "...#r#...", "....#....", "...#.#...", "..#.r.#..", ".#rrrrr#.", "#########")
    val sol = listOf("....#....", ".#.....#.", "...###...", "..#####..", "#.#####.#", "..#####..", "...###...", ".#.....#.", "....#....")
    val luna = listOf("..####...", ".##......", "##.......", "#........", "#........", "##......#", ".##....##", "..######.", ".........")
    val pasos = listOf(".##......", "#oo#.....", "#oo#.....", ".##......", "......##.", ".....#oo#", ".....#oo#", "......##.", ".........")
    val sinAzucar = listOf("..#####..", ".#.....#.", "#.....#.#", "#....#..#", "#...#...#", "#..#....#", "#.#.....#", ".#.....#.", "..#####..")
    val huevo = listOf("...###...", "..#ooo#..", ".#ooooo#.", ".#ooooo#.", "#ooooooo#", "#ooooooo#", "#ooooooo#", ".#ooooo#.", "..#####..")
    val cama = listOf(".........", "#........", "#.##.....", "#.##oooo.", "#########", "#.......#", "#.......#", ".........", ".........")

    /** Icons a meal can use, by the name stored in its settings. */
    val meals = mapOf("comida" to comida, "snack" to snack, "colacion" to colacion, "cena" to cena)
    fun meal(name: String) = meals[name] ?: comida

    /** [grid] without its empty border, so it can be drawn as big as its own shape allows (see DotIcon's cell). */
    fun crop(grid: List<String>): List<String> {
        val rows = grid.indices.filter { y -> grid[y].any { it != '.' } }
        if (rows.isEmpty()) return grid
        val cols = grid.flatMap { line -> line.indices.filter { line[it] != '.' } }
        return grid.subList(rows.first(), rows.last() + 1).map { it.substring(cols.min(), cols.max() + 1) }
    }
}

/** A visible gray for whatever is not marked yet. */
val DotPending = Color(0xFF5C5C5C)
/** An unlit dot during an animation ("black"). */
private val DotOff = Color(0xFF1C1C1C)
private val DotWhite = Color(0xFFF5F5F5)

/**
 * The one short animation an icon plays when it gets marked; afterwards it stays still.
 * [Sweep] lights the dots bottom-up with the red details last, [Steam] then makes the steam rise in two waves
 * (lit / black), [Water] fills the glass with a wavy, moving surface and lights the red details once full, [Bite] lights up like [Sweep] and then takes a
 * bite: the 'b' dots go black and vanish, leaving the bitten edge. [Unlock] lifts the 'u' shackle two dots, as if the
 * padlock opened, and holds it there until the animation ends.
 */
enum class DotActivation(val durationMs: (rows: Int) -> Int) {
    Sweep({ rows -> rows * 28 + rows * 22 + 160 }),
    Steam({ 22 * 70 }),
    Water({ 16 * 60 }),
    Bite({ rows -> rows * 28 + rows * 22 + BITE_GONE_MS + 160 }),
    Unlock({ 700 }),
}

/** How many rows the shackle is lifted [elapsed] ms into [DotActivation.Unlock]: one dot at a time, like the matrix. */
private fun unlockLift(elapsed: Long) = when {
    elapsed < 60 -> 0
    elapsed < 140 -> 1
    else -> 2
}

private const val BITE_BLACK_MS = 120
private const val BITE_GONE_MS = 300

private fun isRed(c: Char) = c == 'r' || c == 's'
private fun marked(c: Char) = when {
    c == 'b' -> null
    isRed(c) -> LauncherRed
    else -> DotWhite
}
private fun pending(c: Char) = if (c == 'o' || c == 'k') null else DotPending

/** Color of one dot, or null to leave it out; [elapsed] is null when no animation is playing. */
private fun dotColor(c: Char, x: Int, y: Int, rows: Int, done: Boolean, activation: DotActivation, elapsed: Long?): Color? {
    if (c == '.') return null
    if (elapsed == null) return if (done) marked(c) else pending(c)
    return when (activation) {
        DotActivation.Sweep -> {
            val delay = (rows - 1 - y) * 28 + if (isRed(c)) rows * 22 else 0
            if (elapsed >= delay) marked(c) else pending(c)
        }
        DotActivation.Steam -> {
            val f = (elapsed / 70).toInt()
            if (c == 's' || c == 'w') {
                val p = (f - 5) - (7 - y)
                if (p >= 0 && p % 4 < 2) marked(c) else DotOff
            } else if (f >= (rows - 1 - y) * 0.35f) marked(c) else pending(c)
        }
        DotActivation.Bite -> {
            val sweepEnd = rows * 28 + rows * 22
            when {
                c == 'b' && elapsed >= sweepEnd + BITE_GONE_MS -> null
                c == 'b' && elapsed >= sweepEnd + BITE_BLACK_MS -> DotOff
                c == 'b' -> if (elapsed >= (rows - 1 - y) * 28) DotWhite else DotPending
                c == 'k' -> if (elapsed >= sweepEnd + BITE_GONE_MS) DotWhite else null
                else -> {
                    val delay = (rows - 1 - y) * 28 + if (isRed(c)) rows * 22 else 0
                    if (elapsed >= delay) marked(c) else pending(c)
                }
            }
        }
        DotActivation.Unlock -> marked(c)
        DotActivation.Water -> {
            val f = (elapsed / 60).toInt()
            // Red details (the thermos bands) wait until the water has reached the top.
            if (isRed(c)) return if (f >= 9) marked(c) else pending(c)
            if (c != 'o') return marked(c)
            val level = 12f * min(1f, (f + 1) / 9f)
            val amp = if (f < 9) 1f else max(0f, (15 - f) / 6f) * 1.5f
            val wave = (sin(f * 1.4f + x * 0.8f) * amp).roundToInt()
            if ((13 - y) <= level + wave) DotWhite else DotOff
        }
    }
}

/**
 * Draws a [DotIcons] grid: gray while not [done], white and red once it is. When [done] turns true (not on first
 * show) it plays [activation] once, after [startDelayMs], unless the phone has animations off; it plays again whenever
 * [replay] changes while [done].
 * With [cell] each dot gets that much room and the icon takes its grid's own shape instead of a [size] square.
 */
@Composable
fun DotIcon(
    grid: List<String>,
    done: Boolean,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    activation: DotActivation = DotActivation.Sweep,
    startDelayMs: Int = 0,
    cell: Dp? = null,
    replay: Int = 0,
) {
    var elapsed by remember { mutableStateOf<Long?>(null) }
    val first = remember { booleanArrayOf(true) }
    val off = animationsOff()
    LaunchedEffect(done, replay) {
        if (first[0]) { first[0] = false; return@LaunchedEffect }
        if (!done || off) { elapsed = null; return@LaunchedEffect }
        elapsed = 0
        delay(startDelayMs.toLong())
        val total = activation.durationMs(grid.size)
        val start = withFrameMillis { it }
        while (true) {
            val now = withFrameMillis { it } - start
            elapsed = now
            if (now >= total) break
        }
        elapsed = null
    }
    val rows = grid.size
    val cols = grid.maxOf { it.length }
    Canvas(
        modifier
            .then(if (cell != null) Modifier.size(cell * cols, cell * rows) else Modifier.size(size))
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier),
    ) {
        val step = if (cell != null) cell.toPx() else this.size.width / max(rows, cols)
        val radius = step * 0.38f
        val time = elapsed
        grid.forEachIndexed { y, line ->
            line.forEachIndexed { x, c ->
                // The shackle may lift above the icon's top edge; the canvas does not clip it.
                val lift = if (c == 'u' && time != null && activation == DotActivation.Unlock) unlockLift(time) else 0
                dotColor(c, x, y, rows, done, activation, time)?.let { drawCircle(it, radius, Offset((x + 0.5f) * step, (y - lift + 0.5f) * step)) }
            }
        }
    }
}
