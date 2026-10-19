package com.adrian.minimallauncher.ui.components
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
    ':' to listOf(".", ".", "#", ".", "#", ".", "."),
    '%' to listOf("##...", "##..#", "...#.", "..#..", ".#...", "#..##", "...##"),
    '$' to listOf("..#..", ".####", "#.#..", ".###.", "..#.#", "####.", "..#.."),
    ' ' to listOf("..", "..", "..", "..", "..", "..", ".."),
)
private const val ROWS = 7
@Composable
fun DotMatrixText(text: String, modifier: Modifier = Modifier, cell: Dp = 4.dp, color: Color = MaterialTheme.colorScheme.onSurface) {
    val chars = text.map { glyphs[it] ?: glyphs.getValue(' ') }
    val columns = chars.sumOf { it[0].length } + (chars.size - 1).coerceAtLeast(0)
    BoxWithConstraints(modifier) {
        // Shrink the dots when the text would not fit the available width.
        val fitted = if (constraints.hasBoundedWidth) minOf(cell, maxWidth / columns.coerceAtLeast(1)) else cell
        DotGlyphs(chars, columns, fitted, color)
    }
}
@Composable
private fun DotGlyphs(chars: List<List<String>>, columns: Int, cell: Dp, color: Color) {
    Canvas(Modifier.size(cell * columns, cell * ROWS)) {
        val step = cell.toPx()
        val radius = step * 0.38f
        var x = 0
        chars.forEach { glyph ->
            glyph.forEachIndexed { row, line ->
                line.forEachIndexed { col, c ->
                    if (c == '#') drawCircle(color, radius, Offset((x + col + 0.5f) * step, (row + 0.5f) * step))
                }
            }
            x += glyph[0].length + 1
        }
    }
}
