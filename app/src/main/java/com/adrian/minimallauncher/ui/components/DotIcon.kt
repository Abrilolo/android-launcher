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
 * 'b' a piece that is gone once marked (bitten off) · 'k' a dot that only shows once marked (the bite's edge).
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
    val termo = listOf(
        ".....#####.....",
        "....#######....",
        "...#########...",
        "...#ooooooo#...",
        "...#ooooooo#...",
        "...#ooooooo#...",
        "...#ooooooo#...",
        "...#rrrrrrr#...",
        "...#ooooooo#...",
        "...#ooooooo#...",
        "...#ooooooo#...",
        "...#ooooooo#...",
        "...#ooooooo#...",
        "...#########...",
        "...............",
    )
    val pasos = listOf(".##......", "#oo#.....", "#oo#.....", ".##......", "......##.", ".....#oo#", ".....#oo#", "......##.", ".........")
    val sinAzucar = listOf("..#####..", ".#.....#.", "#.....#.#", "#....#..#", "#...#...#", "#..#....#", "#.#.....#", ".#.....#.", "..#####..")
    val huevo = listOf("...###...", "..#ooo#..", ".#ooooo#.", ".#ooooo#.", "#ooooooo#", "#ooooooo#", "#ooooooo#", ".#ooooo#.", "..#####..")
    val cama = listOf(".........", "#........", "#.##.....", "#.##oooo.", "#########", "#.......#", "#.......#", ".........", ".........")

    /** Icons a meal can use, by the name stored in its settings. */
    val meals = mapOf("comida" to comida, "snack" to snack, "colacion" to colacion, "cena" to cena)
    fun meal(name: String) = meals[name] ?: comida
}

/** A visible gray for whatever is not marked yet. */
val DotPending = Color(0xFF5C5C5C)
/** An unlit dot during an animation ("black"). */
private val DotOff = Color(0xFF1C1C1C)
private val DotWhite = Color(0xFFF5F5F5)

/**
 * The one short animation an icon plays when it gets marked; afterwards it stays still.
 * [Sweep] lights the dots bottom-up with the red details last, [Steam] then makes the steam rise in two waves
 * (lit / black), [Water] fills the glass with a wavy, moving surface, [Bite] lights up like [Sweep] and then takes a
 * bite: the 'b' dots go black and vanish, leaving the bitten edge.
 */
enum class DotActivation(val durationMs: (rows: Int) -> Int) {
    Sweep({ rows -> rows * 28 + rows * 22 + 160 }),
    Steam({ 22 * 70 }),
    Water({ 16 * 60 }),
    Bite({ rows -> rows * 28 + rows * 22 + BITE_GONE_MS + 160 }),
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
        DotActivation.Water -> {
            if (c != 'o') return marked(c)
            val f = (elapsed / 60).toInt()
            val level = 12f * min(1f, (f + 1) / 9f)
            val amp = if (f < 9) 1f else max(0f, (15 - f) / 6f) * 1.5f
            val wave = (sin(f * 1.4f + x * 0.8f) * amp).roundToInt()
            if ((13 - y) <= level + wave) DotWhite else DotOff
        }
    }
}

/**
 * Draws a [DotIcons] grid: gray while not [done], white and red once it is. When [done] turns true (not on first
 * show) it plays [activation] once, after [startDelayMs], unless the phone has animations off.
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
) {
    var elapsed by remember { mutableStateOf<Long?>(null) }
    val first = remember { booleanArrayOf(true) }
    val off = animationsOff()
    LaunchedEffect(done) {
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
    Canvas(
        modifier
            .size(size)
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier),
    ) {
        val step = this.size.width / max(rows, grid.maxOf { it.length })
        val radius = step * 0.38f
        val time = elapsed
        grid.forEachIndexed { y, line ->
            line.forEachIndexed { x, c ->
                dotColor(c, x, y, rows, done, activation, time)?.let { drawCircle(it, radius, Offset((x + 0.5f) * step, (y + 0.5f) * step)) }
            }
        }
    }
}
