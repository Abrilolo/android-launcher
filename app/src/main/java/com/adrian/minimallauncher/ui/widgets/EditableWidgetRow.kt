package com.adrian.minimallauncher.ui.widgets
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.WidgetPlacement
import kotlin.math.roundToInt
private const val GAP = 16f
private const val MIN_HEIGHT = 120
private const val SNAP_DP = 10
private const val SNAP_FRACTION = 0.05f
private const val HANDLE_DP = 48f
private enum class DragMode { NONE, MOVE, RESIZE }
private fun snap(value: Float) = (value / SNAP_DP).roundToInt() * SNAP_DP
private fun snapFraction(value: Float) = (value / SNAP_FRACTION).roundToInt() * SNAP_FRACTION
// Keeps the widget inside the right column: 30-60% of the row width and within the available height.
private fun WidgetPlacement.fitTo(areaHeight: Float): WidgetPlacement {
    val height = heightDp.coerceIn(MIN_HEIGHT, areaHeight.toInt().coerceAtLeast(MIN_HEIGHT))
    val top = topDp.coerceIn(0, (areaHeight.toInt() - height).coerceAtLeast(0))
    return WidgetPlacement(widthFraction.coerceIn(0.3f, 0.6f), height, top)
}
/**
 * Two-column row: [left] content and the widget stack on the right.
 * Long-pressing the widget enters edit mode: drag it to move it vertically,
 * drag its bottom-left corner to stretch it, tap anywhere to finish.
 */
@Composable
fun EditableWidgetRow(
    placement: WidgetPlacement,
    onPlacementChange: (WidgetPlacement) -> Unit,
    records: com.adrian.minimallauncher.data.PersonalRecords,
    homeWidgets: HomeWidgets,
    modifier: Modifier = Modifier,
    left: @Composable (Modifier) -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(placement) }
    LaunchedEffect(placement) { if (!editing) draft = placement }
    BoxWithConstraints(modifier) {
        val rowWidth = maxWidth.value
        val areaHeight = maxHeight.value
        val fitted = draft.fitTo(areaHeight)
        Row(Modifier.fillMaxSize(), Arrangement.spacedBy(GAP.dp)) {
            left(Modifier.weight(1f - fitted.widthFraction).fillMaxHeight())
            Box(Modifier.weight(fitted.widthFraction).fillMaxHeight()) {
                Box(Modifier.offset(y = fitted.topDp.dp).fillMaxWidth().height(fitted.heightDp.dp)) {
                    WidgetStack(records, homeWidgets, editing, { editing = true }, Modifier.fillMaxSize())
                    if (editing) EditDecorations()
                }
            }
        }
        if (editing) {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(rowWidth, areaHeight) {
                        detectTapGestures(onTap = {
                            editing = false
                            onPlacementChange(draft.fitTo(areaHeight))
                        })
                    }
                    .pointerInput(rowWidth, areaHeight) {
                        var mode = DragMode.NONE
                        var rawTop = 0f
                        var rawHeight = 0f
                        var rawWidth = 0f
                        val columnsWidth = rowWidth - GAP
                        detectDragGestures(
                            onDragStart = { position ->
                                val current = draft.fitTo(areaHeight)
                                val x = columnsWidth * (1f - current.widthFraction) + GAP
                                val width = columnsWidth * current.widthFraction
                                val top = current.topDp.toFloat()
                                val bottom = top + current.heightDp
                                val px = position.x.toDp().value
                                val py = position.y.toDp().value
                                mode = when {
                                    px in (x - 16f)..(x + HANDLE_DP) && py in (bottom - HANDLE_DP)..(bottom + 16f) -> DragMode.RESIZE
                                    px in x..(x + width) && py in top..bottom -> DragMode.MOVE
                                    else -> DragMode.NONE
                                }
                                rawTop = top
                                rawHeight = current.heightDp.toFloat()
                                rawWidth = width
                            },
                            onDragEnd = { if (mode != DragMode.NONE) onPlacementChange(draft.fitTo(areaHeight)) },
                        ) { change, drag ->
                            if (mode == DragMode.NONE) return@detectDragGestures
                            change.consume()
                            val dx = drag.x.toDp().value
                            val dy = drag.y.toDp().value
                            draft = when (mode) {
                                DragMode.MOVE -> {
                                    rawTop += dy
                                    draft.copy(topDp = snap(rawTop))
                                }
                                else -> {
                                    rawHeight += dy
                                    rawWidth -= dx // The handle is on the left edge: dragging left widens.
                                    draft.copy(heightDp = snap(rawHeight), widthFraction = snapFraction(rawWidth / columnsWidth))
                                }
                            }.fitTo(areaHeight)
                        }
                    },
            )
        }
    }
}
@Composable
private fun EditDecorations() {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f), CardShape)) {
        Text("ARRASTRA", Modifier.align(Alignment.Center), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("LISTO", Modifier.align(Alignment.TopEnd).padding(14.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
        Box(Modifier.align(Alignment.BottomStart).offset((-6).dp, 6.dp).size(16.dp).background(MaterialTheme.colorScheme.onSurface, CircleShape))
    }
}
