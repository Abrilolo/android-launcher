package com.adrian.minimallauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.adrian.minimallauncher.data.GRID_COLUMNS
import com.adrian.minimallauncher.data.WidgetStackConfig
import com.adrian.minimallauncher.data.allowedSizes
import com.adrian.minimallauncher.data.fits
import com.adrian.minimallauncher.data.usedRows
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.ui.components.LocalReactions
import com.adrian.minimallauncher.ui.components.WidgetStackTile
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withTimeoutOrNull

/** Height of one grid row; a 2×2 widget ends up close to the old fixed tile height. */
val GRID_CELL_HEIGHT = 72.dp
val GRID_GAP = 10.dp

/** Scrolls a widget page while a widget is held near its top or bottom edge, so it can reach rows off screen. */
class EdgeScroller(val state: ScrollState) {
    /** The page's visible area. */
    var viewport: LayoutCoordinates? = null
    /** Finger of the widget being held, in window coordinates; null when nothing is held. */
    var finger by mutableStateOf<Offset?>(null)
}

@Composable
fun rememberEdgeScroller(state: ScrollState): EdgeScroller {
    val scroller = remember(state) { EdgeScroller(state) }
    val edge = with(LocalDensity.current) { 72.dp.toPx() }
    val step = with(LocalDensity.current) { 10.dp.toPx() }
    LaunchedEffect(scroller) {
        snapshotFlow { scroller.finger != null }.collectLatest { holding ->
            while (holding) {
                withFrameNanos { }
                val finger = scroller.finger ?: break
                val area = scroller.viewport?.takeIf { it.isAttached }?.boundsInWindow() ?: continue
                when {
                    finger.y > area.bottom - edge -> state.scrollBy(step)
                    finger.y < area.top + edge -> state.scrollBy(-step)
                }
            }
        }
    }
    return scroller
}

/** Where a dragged or resized widget would land, and whether that is allowed. */
private data class DropPreview(val col: Int, val row: Int, val w: Int, val h: Int, val valid: Boolean, val merge: Boolean)

/**
 * A widget page's 4-column grid.
 * - Long-pressing a widget selects it in place (showing the One UI corner resize frame + 3-option popup).
 * - Long-pressing empty space on the page enters full-page [editMode].
 */
@Composable
fun WidgetGridView(
    stacks: List<WidgetStackConfig>,
    editMode: Boolean,
    data: HomeData,
    now: LocalDateTime,
    onOpenModule: (String) -> Unit,
    onMove: (stackId: String, col: Int, row: Int) -> Unit,
    onResize: (stackId: String, w: Int, h: Int) -> Unit,
    onMerge: (draggedId: String, targetId: String) -> Unit,
    onRemoveWidget: (stackId: String, widgetId: String) -> Unit,
    onAddToStack: (stackId: String) -> Unit,
    onRemoveStack: (stackId: String) -> Unit = {},
    onSetAlpha: (stackId: String, alpha: Float) -> Unit = { _, _ -> },
    scroller: EdgeScroller? = null,
) {
    var selectedStackId by LocalSelectedStack.current
    val reactions = LocalReactions.current
    BackHandler(enabled = selectedStackId != null) { selectedStackId = null }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cellW = (maxWidth - GRID_GAP * (GRID_COLUMNS - 1)) / GRID_COLUMNS
        val stepX = cellW + GRID_GAP
        val stepY = GRID_CELL_HEIGHT + GRID_GAP
        // While editing or resizing a selected widget, two empty rows below give room to drag widgets down.
        val rows = stacks.usedRows() + if (editMode || selectedStackId != null) 2 else 0
        var preview by remember { mutableStateOf<DropPreview?>(null) }
        var gridCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
        val density = LocalDensity.current

        // A widget carried from the catalog over this page: the cell under the finger, centered on it.
        val pageId = LocalPageId.current
        val carry = LocalWidgetCarry.current?.takeIf { pageId != null && it.pageId == pageId }
        // Read so the landing cell follows while the page scrolls under a still finger.
        scroller?.state?.value
        val carried = carry?.let { c ->
            val coords = gridCoords?.takeIf { it.isAttached } ?: return@let null
            val at = coords.windowToLocal(c.window)
            val (w, h) = c.size
            with(density) {
                val col = ((at.x - spanSize(cellW, w).toPx() / 2) / stepX.toPx()).roundToInt().coerceIn(0, GRID_COLUMNS - w)
                val row = ((at.y - spanSize(GRID_CELL_HEIGHT, h).toPx() / 2) / stepY.toPx()).roundToInt().coerceAtLeast(0)
                val onTop = stacks.firstOrNull { it.col == col && it.row == row && it.w == w && it.h == h }
                if (onTop != null || stacks.fits(col, row, w, h)) {
                    DropPreview(col, row, w, h, valid = true, merge = onTop != null) to onTop?.id
                } else {
                    // Taken: the free cell closest to the finger, so a drop a bit off still lands where meant.
                    val (c, r) = (0..stacks.usedRows()).flatMap { r -> (0..GRID_COLUMNS - w).map { c -> c to r } }
                        .filter { (c, r) -> stacks.fits(c, r, w, h) }
                        .minBy { (c, r) -> abs(c - col) + abs(r - row) }
                    DropPreview(c, r, w, h, valid = true, merge = false) to null
                }
            }
        }
        val feeding = remember { booleanArrayOf(false) }
        SideEffect {
            if (carry != null && pageId != null) {
                carry.target = carried?.let { (p, stackId) -> CarryTarget(pageId, p.col, p.row, p.valid, stackId) }
                scroller?.finger = carry.window
                feeding[0] = true
            } else if (feeding[0]) {
                scroller?.finger = null
                feeding[0] = false
            }
        }

        val activePreview = preview ?: carried?.first
        Box(
            Modifier
                .fillMaxWidth()
                .height(if (rows == 0) 0.dp else stepY * rows - GRID_GAP)
                .onGloballyPositioned { gridCoords = it },
        ) {
            if (editMode) EmptyCells(rows, cellW, stepX, stepY)
            activePreview?.let { p ->
                Box(
                    Modifier
                        .offset(stepX * p.col, stepY * p.row)
                        .size(spanSize(cellW, p.w), spanSize(GRID_CELL_HEIGHT, p.h))
                        .border(2.dp, if (p.valid) Color.White else LauncherRed, RoundedCornerShape(26.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (p.merge) SmallLabel("PILA", color = Color.White)
                }
            }
            stacks.forEach { stack ->
                key(stack.id) {
                    val isMergeTarget = activePreview?.let { it.merge && it.col == stack.col && it.row == stack.row } == true
                    val isSelected = selectedStackId == stack.id
                    GridItem(
                        stack = stack,
                        stacks = stacks,
                        editMode = editMode,
                        isSelected = isSelected,
                        isMergeTarget = isMergeTarget,
                        cellW = cellW,
                        stepX = stepX,
                        stepY = stepY,
                        data = data,
                        now = now,
                        onOpenModule = { id ->
                            if (selectedStackId != null) selectedStackId = null else onOpenModule(id)
                        },
                        onSelectWidget = {
                            reactions.tick()
                            selectedStackId = stack.id
                        },
                        onDismissSelect = { selectedStackId = null },
                        onPreview = { preview = it },
                        onMove = { col, row -> onMove(stack.id, col, row) },
                        onResize = { w, h -> onResize(stack.id, w, h) },
                        onMerge = { targetId ->
                            selectedStackId = null
                            onMerge(stack.id, targetId)
                        },
                        onRemoveWidget = { widgetId -> onRemoveWidget(stack.id, widgetId) },
                        onRemoveStack = {
                            selectedStackId = null
                            onRemoveStack(stack.id)
                        },
                        onAddToStack = { onAddToStack(stack.id) },
                        onSetAlpha = { alpha -> onSetAlpha(stack.id, alpha) },
                        scroller = scroller,
                    )
                }
            }
        }
    }
}

private fun spanSize(cell: Dp, cells: Int): Dp = cell * cells + GRID_GAP * (cells - 1)

/** Faint dashed cells that show where widgets can go while editing. */
@Composable
private fun EmptyCells(rows: Int, cellW: Dp, stepX: Dp, stepY: Dp) {
    for (row in 0 until rows) for (col in 0 until GRID_COLUMNS) {
        Canvas(Modifier.offset(stepX * col, stepY * row).size(cellW, GRID_CELL_HEIGHT)) {
            drawRoundRect(
                Color(0xFF262626),
                cornerRadius = CornerRadius(16.dp.toPx()),
                style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))),
            )
        }
    }
}

@Composable
private fun GridItem(
    stack: WidgetStackConfig,
    stacks: List<WidgetStackConfig>,
    editMode: Boolean,
    isSelected: Boolean,
    isMergeTarget: Boolean,
    cellW: Dp,
    stepX: Dp,
    stepY: Dp,
    data: HomeData,
    now: LocalDateTime,
    onOpenModule: (String) -> Unit,
    onSelectWidget: () -> Unit,
    onDismissSelect: () -> Unit,
    onPreview: (DropPreview?) -> Unit,
    onMove: (Int, Int) -> Unit,
    onResize: (Int, Int) -> Unit,
    onMerge: (String) -> Unit,
    onRemoveWidget: (String) -> Unit,
    onRemoveStack: () -> Unit,
    onAddToStack: () -> Unit,
    onSetAlpha: (Float) -> Unit = {},
    scroller: EdgeScroller?,
) {
    val density = LocalDensity.current
    val reactions = LocalReactions.current
    val touchOnWidget = LocalTouchOnWidget.current
    val stepXPx = with(density) { stepX.toPx() }
    val stepYPx = with(density) { stepY.toPx() }
    var drag by remember { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf(false) }
    var resizing by remember { mutableStateOf(false) }
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    // Page scroll when the drag started: the widget follows the finger while the page scrolls under it.
    var scrollAtStart by remember { mutableStateOf(0) }
    val scrolled = if (dragging && scroller != null) scroller.state.value - scrollAtStart else 0
    val held = drag + Offset(0f, scrolled.toFloat())
    // The gesture detectors outlive recompositions; they read the latest layout through these.
    val current by rememberUpdatedState(stack)
    val all by rememberUpdatedState(stacks)
    val latestSelect by rememberUpdatedState(onSelectWidget)
    val latestMove by rememberUpdatedState(onMove)
    val latestMerge by rememberUpdatedState(onMerge)
    val isSelectedState = rememberUpdatedState(isSelected)
    val canManipulate = editMode || isSelected

    fun dropTarget(): DropPreview {
        val s = current
        val shift = drag + Offset(0f, scroller?.let { (it.state.value - scrollAtStart).toFloat() } ?: 0f)
        val col = ((s.col * stepXPx + shift.x) / stepXPx).roundToInt().coerceIn(0, GRID_COLUMNS - s.w)
        val row = ((s.row * stepYPx + shift.y) / stepYPx).roundToInt().coerceAtLeast(0)
        val onTop = all.firstOrNull { it.id != s.id && it.col == col && it.row == row && it.w == s.w && it.h == s.h }
        return DropPreview(col, row, s.w, s.h, valid = onTop != null || all.fits(col, row, s.w, s.h, ignoreId = s.id), merge = onTop != null)
    }

    Box(
        Modifier
            .offset(stepX * stack.col, stepY * stack.row)
            .size(spanSize(cellW, stack.w), spanSize(GRID_CELL_HEIGHT, stack.h))
            .zIndex(if (dragging || isSelected) 1f else 0f)
            .graphicsLayer {
                translationX = held.x
                translationY = held.y
                if (dragging) {
                    scaleX = 1.05f
                    scaleY = 1.05f
                    alpha = 0.92f
                    shadowElevation = 22.dp.toPx()
                    shape = RoundedCornerShape(26.dp)
                }
            }
            .onGloballyPositioned { coords = it }
            // Marks that the touch started inside a widget (so HomePagesHost doesn't enter full-page edit mode),
            // and on long-press selects the widget in place (allowing immediate drag in the same hold).
            .pointerInput(stack.id, editMode) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    touchOnWidget[0] = true
                    if (editMode || isSelectedState.value) return@awaitEachGesture
                    val releasedOrMoved = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed) return@withTimeoutOrNull true
                            if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) return@withTimeoutOrNull true
                        }
                    }
                    if (releasedOrMoved == null) {
                        latestSelect()
                        var moved = false
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            event.changes.forEach { it.consume() }
                            if (!change.pressed) break
                            val delta = change.position - change.previousPosition
                            if (!moved && (change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                moved = true
                                scrollAtStart = scroller?.state?.value ?: 0
                                dragging = true
                            }
                            if (moved) {
                                drag += delta
                                coords?.takeIf { it.isAttached }?.let { scroller?.finger = it.localToWindow(change.position) }
                                onPreview(dropTarget())
                            }
                        }
                        if (moved) {
                            val target = dropTarget()
                            val s = current
                            when {
                                target.merge -> all.firstOrNull { it.id != s.id && it.col == target.col && it.row == target.row }?.let { latestMerge(it.id) }
                                target.valid && (target.col != s.col || target.row != s.row) -> latestMove(target.col, target.row)
                            }
                            drag = Offset.Zero
                            dragging = false
                            scroller?.finger = null
                            onPreview(null)
                        }
                    }
                }
            }
            .then(
                if (!canManipulate) Modifier else Modifier.pointerInput(stack.id, canManipulate) {
                    detectDragGestures(
                        onDragStart = {
                            scrollAtStart = scroller?.state?.value ?: 0
                            dragging = true
                            reactions.tick()
                        },
                        onDragEnd = {
                            val target = dropTarget()
                            val s = current
                            when {
                                target.merge -> all.firstOrNull { it.id != s.id && it.col == target.col && it.row == target.row }?.let { latestMerge(it.id) }
                                target.valid && (target.col != s.col || target.row != s.row) -> latestMove(target.col, target.row)
                            }
                            drag = Offset.Zero
                            dragging = false
                            scroller?.finger = null
                            onPreview(null)
                        },
                        onDragCancel = { drag = Offset.Zero; dragging = false; scroller?.finger = null; onPreview(null) },
                    ) { change, amount ->
                        change.consume()
                        drag += amount
                        coords?.takeIf { it.isAttached }?.let { scroller?.finger = it.localToWindow(change.position) }
                        onPreview(dropTarget())
                    }
                },
            ),
    ) {
        WidgetStackTile(
            stack = stack,
            data = data,
            now = now,
            isEditMode = editMode,
            onOpenModule = onOpenModule,
            onLongClick = onSelectWidget,
            onRemoveWidget = onRemoveWidget,
            onRemoveStack = onRemoveStack,
            onAddWidgetToStack = onAddToStack,
            onSetAlpha = onSetAlpha,
            isSelected = isSelected,
            showPopup = isSelected && !dragging && !resizing,
            onDismissSelect = onDismissSelect,
            isMergeTarget = isMergeTarget,
            modifier = Modifier.fillMaxSize(),
        )
        if (canManipulate && !dragging) {
            // All 4 white corners can be dragged to resize the widget, except top-left in full editMode where the '-' badge sits.
            if (!editMode) {
                CornerResizeHandle(Modifier.align(Alignment.TopStart), signX = -1f, signY = -1f, stack, all, stepXPx, stepYPx, { resizing = it }, onPreview, onResize)
            }
            CornerResizeHandle(Modifier.align(Alignment.TopEnd), signX = 1f, signY = -1f, stack, all, stepXPx, stepYPx, { resizing = it }, onPreview, onResize)
            CornerResizeHandle(Modifier.align(Alignment.BottomStart), signX = -1f, signY = 1f, stack, all, stepXPx, stepYPx, { resizing = it }, onPreview, onResize)
            CornerResizeHandle(Modifier.align(Alignment.BottomEnd), signX = 1f, signY = 1f, stack, all, stepXPx, stepYPx, { resizing = it }, onPreview, onResize)
        }
    }
}

/** Invisible touch target over each white corner bracket: dragging it resizes the widget between its allowed sizes. */
@Composable
private fun CornerResizeHandle(
    modifier: Modifier,
    signX: Float,
    signY: Float,
    stack: WidgetStackConfig,
    stacks: List<WidgetStackConfig>,
    stepXPx: Float,
    stepYPx: Float,
    onResizing: (Boolean) -> Unit,
    onPreview: (DropPreview?) -> Unit,
    onResize: (Int, Int) -> Unit,
) {
    val current by rememberUpdatedState(stack)
    val all by rememberUpdatedState(stacks)
    var pull by remember { mutableStateOf(Offset.Zero) }

    fun chosen(): DropPreview {
        val s = current
        val wantW = (s.w + signX * pull.x / stepXPx)
        val wantH = (s.h + signY * pull.y / stepYPx)
        val best = s.allowedSizes()
            .filter { s.col + it.w <= GRID_COLUMNS && all.fits(s.col, s.row, it.w, it.h, ignoreId = s.id) }
            .minByOrNull { abs(it.w - wantW) + abs(it.h - wantH) }
        return DropPreview(s.col, s.row, best?.w ?: s.w, best?.h ?: s.h, valid = best != null, merge = false)
    }

    Box(
        modifier
            .size(32.dp)
            .semantics { contentDescription = "Cambiar tamaño" }
            .pointerInput(stack.id, signX, signY) {
                detectDragGestures(
                    onDragStart = { onResizing(true) },
                    onDragEnd = {
                        val target = chosen()
                        if (target.valid && (target.w != current.w || target.h != current.h)) onResize(target.w, target.h)
                        pull = Offset.Zero
                        onResizing(false)
                        onPreview(null)
                    },
                    onDragCancel = {
                        pull = Offset.Zero
                        onResizing(false)
                        onPreview(null)
                    },
                ) { change, amount ->
                    change.consume()
                    pull += amount
                    onPreview(chosen())
                }
            },
    )
}
