package com.adrian.minimallauncher.data

/** Widget pages are a grid of this many columns; rows grow as needed. */
const val GRID_COLUMNS = 4

/** A widget size in grid cells, written like One UI does: "2×2", "4×1". */
data class GridSize(val w: Int, val h: Int) {
    val label: String get() = "$w×$h"
}

/** Sizes a widget may take; resizing snaps between these. Notes reads best as a strip, the clock big. */
fun sizesFor(widgetId: String): List<GridSize> = when (widgetId) {
    "notes" -> listOf(GridSize(4, 1), GridSize(4, 2))
    "clock" -> listOf(GridSize(4, 2), GridSize(2, 2))
    else -> listOf(GridSize(2, 2), GridSize(4, 2))
}

/** The Apps page starts with the big clock on top of the app list. */
val APPS_DEFAULT_LAYOUT = listOf(WidgetStackConfig("stack_clock", listOf("clock"), span = 2, col = 0, row = 0, w = 4, h = 2))

/** Sizes allowed for a stack: the ones every widget in it accepts. */
fun WidgetStackConfig.allowedSizes(): List<GridSize> =
    widgetIds.map { sizesFor(it).toSet() }.reduceOrNull { a, b -> a intersect b }?.toList()?.takeIf { it.isNotEmpty() }
        ?: sizesFor(widgetIds.firstOrNull().orEmpty())

val WidgetStackConfig.isPlaced: Boolean get() = col >= 0 && row >= 0 && w > 0 && h > 0
val WidgetStackConfig.size: GridSize get() = GridSize(w, h)

private fun WidgetStackConfig.overlaps(col: Int, row: Int, w: Int, h: Int): Boolean =
    this.col < col + w && col < this.col + this.w && this.row < row + h && row < this.row + this.h

/** Whether a [w]×[h] block at [col],[row] is inside the grid and free, not counting [ignoreId]. */
fun List<WidgetStackConfig>.fits(col: Int, row: Int, w: Int, h: Int, ignoreId: String? = null): Boolean =
    col >= 0 && row >= 0 && col + w <= GRID_COLUMNS && none { it.id != ignoreId && it.isPlaced && it.overlaps(col, row, w, h) }

/** First free top-left cell for a [w]×[h] block, reading row by row like One UI's "Añadir". */
fun List<WidgetStackConfig>.firstFreeSpot(w: Int, h: Int): Pair<Int, Int> {
    var row = 0
    while (true) {
        for (col in 0..(GRID_COLUMNS - w)) if (fits(col, row, w, h)) return col to row
        row++
    }
}

/** Rows in use, so the page knows how tall to draw the grid. */
fun List<WidgetStackConfig>.usedRows(): Int = maxOfOrNull { it.row + it.h } ?: 0

/**
 * Gives every stack a cell. Layouts saved before the grid existed only had a 1/2-column span;
 * they are laid out in their saved order: half width becomes 2×2, full width 4×2, notes 4×1.
 */
fun List<WidgetStackConfig>.placed(): List<WidgetStackConfig> {
    val result = mutableListOf<WidgetStackConfig>()
    forEach { stack ->
        if (stack.isPlaced && result.fits(stack.col, stack.row, stack.w, stack.h)) {
            result += stack
        } else {
            val size = when {
                stack.isPlaced -> stack.size
                stack.span == 2 && stack.widgetIds == listOf("notes") -> GridSize(4, 1)
                stack.span == 2 -> GridSize(4, 2)
                else -> GridSize(2, 2)
            }
            val (col, row) = result.firstFreeSpot(size.w, size.h)
            result += stack.copy(col = col, row = row, w = size.w, h = size.h)
        }
    }
    return result
}
