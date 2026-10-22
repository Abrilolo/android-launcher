package com.adrian.minimallauncher.ui.components
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
// 7-row glyphs drawn as dots, in the style of Nothing's dot-matrix type.
private val glyphs = mapOf(
    '0' to listOf(".###.", "#...#", "#..##", "#.#.#", "##..#", "#...#", ".###."),
    '1' to listOf("..#..", ".##..", "..#..", "..#..", "..#..", "..#..", ".###."),
    '2' to listOf(".###.", "#...#", "....#", "...#.", "..#..", ".#...", "#####"),
    '3' to listOf("#####", "...#.", "..#..", "...#.", "....#", "#...#", ".###."),
    '4' to listOf("...#.", "..##.", ".#.#.", "#..#.", "#####", "...#.", "...#."),
    '5' to listOf("#####", "#....", "####.", "....#", "....#", "#...#", ".###."),
    '6' to listOf("..##.", ".#...", "#....", "####.", "#...#", "#...#", ".###."),
    '7' to listOf("#####", "....#", "...#.", "..#..", ".#...", ".#...", ".#..."),
    '8' to listOf(".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###."),
    '9' to listOf(".###.", "#...#", "#...#", ".####", "....#", "...#.", ".##.."),
    'A' to listOf(".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"),
    'B' to listOf("####.", "#...#", "####.", "#...#", "#...#", "#...#", "####."),
    'C' to listOf(".####", "#....", "#....", "#....", "#....", "#....", ".####"),
    'D' to listOf("####.", "#...#", "#...#", "#...#", "#...#", "#...#", "####."),
    'E' to listOf("#####", "#....", "####.", "#....", "#....", "#....", "#####"),
    'F' to listOf("#####", "#....", "####.", "#....", "#....", "#....", "#...."),
    'G' to listOf(".####", "#....", "#....", "#.###", "#...#", "#...#", ".####"),
    'H' to listOf("#...#", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"),
    'I' to listOf("#####", "..#..", "..#..", "..#..", "..#..", "..#..", "#####"),
    'J' to listOf("..###", "....#", "....#", "....#", "....#", "#...#", ".###."),
    'K' to listOf("#...#", "#..#.", "#.#..", "##...", "#.#..", "#..#.", "#...#"),
    'L' to listOf("#....", "#....", "#....", "#....", "#....", "#....", "#####"),
    'M' to listOf("#...#", "##.##", "#.#.#", "#...#", "#...#", "#...#", "#...#"),
    'N' to listOf("#...#", "##..#", "#.#.#", "#..##", "#...#", "#...#", "#...#"),
    'O' to listOf(".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."),
    'P' to listOf("####.", "#...#", "#...#", "####.", "#....", "#....", "#...."),
    'Q' to listOf(".###.", "#...#", "#...#", "#...#", "#.#.#", "#..#.", ".##.#"),
    'R' to listOf("####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#"),
    'S' to listOf(".####", "#....", ".###.", "....#", "....#", "#...#", ".###."),
    'T' to listOf("#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#.."),
    'U' to listOf("#...#", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."),
    'V' to listOf("#...#", "#...#", "#...#", "#...#", ".#.#.", ".#.#.", "..#.."),
    'W' to listOf("#...#", "#...#", "#...#", "#...#", "#.#.#", "##.##", "#...#"),
    'X' to listOf("#...#", "#...#", ".#.#.", "..#..", ".#.#.", "#...#", "#...#"),
    'Y' to listOf("#...#", "#...#", ".#.#.", "..#..", "..#..", "..#..", "..#.."),
    'Z' to listOf("#####", "....#", "...#.", "..#..", ".#...", "#....", "#####"),
    ':' to listOf(".", ".", "#", ".", "#", ".", "."),
    '%' to listOf("##...", "##..#", "...#.", "..#..", ".#...", "#..##", "...##"),
    '$' to listOf("..#..", ".####", "#.#..", ".###.", "..#.#", "####.", "..#.."),
    '+' to listOf(".....", "..#..", "..#..", "#####", "..#..", "..#..", "....."),
    '-' to listOf(".....", ".....", ".....", "#####", ".....", ".....", "....."),
    '.' to listOf(".....", ".....", ".....", ".....", ".....", "..##.", "..##."),
    '/' to listOf("....#", "...#.", "...#.", "..#..", ".#...", ".#...", "#...."),
    ' ' to listOf("..", "..", "..", "..", "..", "..", ".."),
)
private const val ROWS = 7
/**
 * [accents] paints some characters in another color, e.g. the decimal point in red. When [reveal] changes
 * (not on first show) the text redraws dot by dot, left to right, once.
 */
@Composable
fun DotMatrixText(
    text: String,
    modifier: Modifier = Modifier,
    cell: Dp = 4.dp,
    color: Color = MaterialTheme.colorScheme.onSurface,
    accents: Map<Char, Color> = emptyMap(),
    reveal: Any? = null,
) {
    val shown = remember { Animatable(1f) }
    val first = remember { booleanArrayOf(true) }
    val off = animationsOff()
    LaunchedEffect(reveal) {
        if (first[0]) { first[0] = false; return@LaunchedEffect }
        if (off) return@LaunchedEffect
        shown.snapTo(0f)
        shown.animateTo(1f, tween(360))
    }
    val upper = text.uppercase()
    val chars = upper.map { glyphs[it] ?: glyphs.getValue(' ') }
    val colors = upper.map { accents[it] ?: color }
    val columns = chars.sumOf { it[0].length } + (chars.size - 1).coerceAtLeast(0)
    BoxWithConstraints(modifier) {
        // Shrink the dots when the text would not fit the available width.
        val fitted = if (constraints.hasBoundedWidth) minOf(cell, maxWidth / columns.coerceAtLeast(1)) else cell
        DotGlyphs(chars, colors, columns, fitted) { shown.value }
    }
}
@Composable
private fun DotGlyphs(chars: List<List<String>>, colors: List<Color>, columns: Int, cell: Dp, shown: () -> Float = { 1f }) {
    Canvas(Modifier.size(cell * columns, cell * ROWS)) {
        val step = cell.toPx()
        val radius = step * 0.38f
        val upTo = shown() * columns
        var x = 0
        chars.forEachIndexed { index, glyph ->
            glyph.forEachIndexed { row, line ->
                line.forEachIndexed { col, c ->
                    if (c == '#' && x + col < upTo) drawCircle(colors[index], radius, Offset((x + col + 0.5f) * step, (row + 0.5f) * step))
                }
            }
            x += glyph[0].length + 1
        }
    }
}
