package com.adrian.minimallauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adrian.minimallauncher.data.GRID_COLUMNS
import com.adrian.minimallauncher.data.GridSize
import com.adrian.minimallauncher.data.HomePage
import com.adrian.minimallauncher.data.sizesFor
import com.adrian.minimallauncher.data.HomePages
import com.adrian.minimallauncher.data.PageKind
import com.adrian.minimallauncher.data.WidgetStackConfig
import com.adrian.minimallauncher.feature.CardFill
import com.adrian.minimallauncher.feature.CardLine
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.feature.ModuleHeader
import com.adrian.minimallauncher.feature.ModuleTextField
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.ui.components.WIDGET_LABELS
import com.adrian.minimallauncher.ui.components.WidgetContent
import java.time.LocalDateTime
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** What a page needs from the host to draw itself. */
class PageScope(
    val editMode: Boolean,
    val enterEdit: () -> Unit,
    /** Opens the widget catalog; with a stack id, the chosen widget joins that stack. */
    val openCatalog: (stackId: String?) -> Unit,
    val goToMain: () -> Unit,
    val exitEdit: () -> Unit,
)

/** A widget being dragged out of the catalog onto a page, as in One UI. */
class WidgetCarry(val widgetId: String, val size: GridSize) {
    /** Finger position in window coordinates. */
    var window by mutableStateOf(Offset.Zero)
    /** Page being looked at, the one the widget would drop on. */
    var pageId by mutableStateOf<String?>(null)
    /** Cell under the finger, written by that page's grid. */
    var target: CarryTarget? = null
}

/** Where a carried widget would land; [stackId] when it would join a stack of the same size. */
data class CarryTarget(val pageId: String, val col: Int, val row: Int, val valid: Boolean, val stackId: String?)

/** The widget being carried, if any; widget grids show where it would land. */
val LocalWidgetCarry = compositionLocalOf<WidgetCarry?> { null }

/** Id of the page a composable is drawn in. */
val LocalPageId = compositionLocalOf<String?> { null }

/**
 * The home screen as a row of pages, Samsung style. The main page is shown first and the
 * home button returns to it. Long press anywhere enters edit mode: pages shrink, each gets
 * a house (make it the main page) and a trash can, both ends get a "+" to add a widget page,
 * and a bottom bar offers Widgets and Ajustes.
 */
@Composable
fun HomePagesHost(
    pages: HomePages,
    layouts: Map<String, List<WidgetStackConfig>>,
    homePressed: SharedFlow<Unit>,
    data: HomeData,
    now: LocalDateTime,
    onAddPage: (atStart: Boolean) -> String,
    onDeletePage: (String) -> Unit,
    onSetMain: (String) -> Unit,
    onRestoreWeek: () -> Unit,
    onAddWidget: (pageId: String, widgetId: String, stackId: String?, size: GridSize, at: Pair<Int, Int>?) -> Unit,
    onSettings: () -> Unit,
    pageContent: @Composable (HomePage, PageScope) -> Unit,
) {
    var editMode by rememberSaveable { mutableStateOf(false) }
    var catalogTarget by remember { mutableStateOf<CatalogTarget?>(null) }
    // The page being looked at, by id, so it stays put when edit mode adds the two "+" pages.
    var currentId by rememberSaveable { mutableStateOf(pages.mainId) }
    val list = pages.pages
    val offset = if (editMode) 1 else 0
    val pagerState = rememberPagerState(initialPage = pages.mainIndex) { list.size + 2 * offset }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var carry by remember { mutableStateOf<WidgetCarry?>(null) }
    var hostCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var hostWidth by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val pageCount by rememberUpdatedState(list.size)

    LaunchedEffect(pagerState.currentPage, editMode, list) {
        list.getOrNull(pagerState.currentPage - offset)?.let { currentId = it.id }
    }
    LaunchedEffect(editMode, list) {
        val index = list.indexOfFirst { it.id == currentId }.takeIf { it >= 0 } ?: pages.mainIndex
        if (pagerState.currentPage != index + offset) pagerState.scrollToPage(index + offset)
    }
    LaunchedEffect(homePressed) {
        homePressed.collect {
            editMode = false
            catalogTarget = null
            currentId = pages.mainId
            pagerState.animateScrollToPage(pages.mainIndex)
        }
    }
    BackHandler(enabled = editMode && catalogTarget == null) { editMode = false }
    SideEffect { carry?.pageId = currentId }
    // Holding a carried widget at the screen edge turns the page, like dragging an icon on Samsung.
    LaunchedEffect(carry) {
        val held = carry ?: return@LaunchedEffect
        val edge = with(density) { 40.dp.toPx() }
        var side = 0
        var waited = 0
        while (true) {
            delay(100)
            val now = when {
                held.window.x < edge -> -1
                hostWidth > 0 && held.window.x > hostWidth - edge -> 1
                else -> 0
            }
            if (now != side) { side = now; waited = 0; continue }
            if (side == 0) continue
            waited += 100
            if (waited >= 600) {
                // Only real pages, never the "+" slots at both ends.
                val next = (pagerState.currentPage + side).coerceIn(1, pageCount)
                if (next != pagerState.currentPage) pagerState.animateScrollToPage(next)
                waited = 0
            }
        }
    }

    val enterEdit = {
        if (!editMode) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        editMode = true
    }
    val goToMain: () -> Unit = { scope.launch { pagerState.animateScrollToPage(pages.mainIndex + offset) } }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .onGloballyPositioned { hostCoords = it }
            .onSizeChanged { hostWidth = it.width },
    ) {
      CompositionLocalProvider(LocalWidgetCarry provides carry) {
        // In edit mode the page chrome and toolbar sit outside the pages, so they need the system bar insets themselves.
        Column(if (editMode) Modifier.fillMaxSize().safeDrawingPadding() else Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                contentPadding = if (editMode) PaddingValues(horizontal = 36.dp) else PaddingValues(0.dp),
                pageSpacing = if (editMode) 12.dp else 0.dp,
                key = { index -> list.getOrNull(index - offset)?.id ?: if (index == 0) "add_start" else "add_end" },
            ) { index ->
                val page = list.getOrNull(index - offset)
                CompositionLocalProvider(LocalPageId provides page?.id) {
                when {
                    page == null -> AddPageSlot(atStart = index == 0) {
                        val id = onAddPage(index == 0)
                        currentId = id
                    }
                    editMode -> EditablePage(
                        page = page,
                        isMain = page.id == pages.mainId,
                        onSetMain = { onSetMain(page.id) },
                        onDelete = {
                            // Stay next to the deleted page, as One UI does, instead of jumping to the main one.
                            val index = list.indexOf(page)
                            list.getOrNull(index - 1)?.let { currentId = it.id } ?: list.getOrNull(index + 1)?.let { currentId = it.id }
                            onDeletePage(page.id)
                        },
                        // Only widget pages stay interactive (their stacks have edit controls); a tap on the others leaves edit mode.
                        // Pages with widgets stay interactive (their stacks have edit controls; Apps covers its list itself).
                        // A tap on Semana leaves edit mode.
                        onTapPage = if (page.kind.hasWidgets) null else ({ editMode = false }),
                    ) {
                        pageContent(page, PageScope(true, enterEdit, { stack -> catalogTarget = CatalogTarget(page.id, stack) }, goToMain, { editMode = false }))
                    }
                    else -> Box(Modifier.fillMaxSize().longPressAnywhere(enterEdit)) {
                        pageContent(page, PageScope(false, enterEdit, { stack -> catalogTarget = CatalogTarget(page.id, stack) }, goToMain, { editMode = false }))
                    }
                }
                }
            }
            AnimatedVisibility(editMode) {
                EditToolbar(
                    weekDeleted = list.none { it.kind == PageKind.WEEK },
                    onWidgets = {
                        val current = list.firstOrNull { it.id == currentId }
                        val pageId = current?.takeIf { it.kind.hasWidgets }?.id
                            ?: list.firstOrNull { it.kind == PageKind.WIDGETS }?.id
                            ?: onAddPage(false)
                        catalogTarget = CatalogTarget(pageId, null)
                    },
                    onSettings = { editMode = false; onSettings() },
                    onRestoreWeek = onRestoreWeek,
                    onDone = { editMode = false },
                )
            }
        }
      }
        catalogTarget?.let { target ->
            WidgetCatalog(
                data = data,
                now = now,
                addingToStack = target.stackId != null,
                // While a widget is carried the catalog stays composed (it owns the drag) but is invisible.
                hidden = carry != null,
                onAdd = { widgetId, size ->
                    onAddWidget(target.pageId, widgetId, target.stackId, size, null)
                    currentId = target.pageId
                    catalogTarget = null
                },
                onPick = { widgetId, size, window ->
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    editMode = true
                    currentId = target.pageId
                    carry = WidgetCarry(widgetId, size).also { it.window = window; it.pageId = target.pageId }
                },
                onCarry = { delta -> carry?.let { it.window += delta } },
                onDrop = {
                    carry?.let { held ->
                        val page = list.firstOrNull { it.id == held.pageId }
                        // On a page with widgets it lands on the cell under the finger, or the closest free spot if it
                        // does not fit there. Dropped on Semana, the catalog comes back.
                        if (page?.kind?.hasWidgets == true) {
                            val spot = held.target?.takeIf { it.pageId == page.id && it.valid }
                            onAddWidget(page.id, held.widgetId, spot?.stackId, held.size, spot?.let { it.col to it.row })
                            catalogTarget = null
                        }
                    }
                    carry = null
                },
                onCancel = { carry = null },
                onClose = { catalogTarget = null },
            )
        }
        carry?.let { held ->
            val origin = hostCoords?.takeIf { it.isAttached }?.windowToLocal(held.window) ?: held.window
            // Roughly the size it will have on the shrunken page.
            val width = with(density) { (hostWidth * 0.8f * held.size.w / GRID_COLUMNS).toDp() }
            val height = (GRID_CELL_HEIGHT * held.size.h + GRID_GAP * (held.size.h - 1)) * 0.9f
            Box(
                Modifier
                    .offset { IntOffset((origin.x - width.toPx() / 2).roundToInt(), (origin.y - height.toPx() / 2).roundToInt()) }
                    .width(width)
                    .height(height)
                    // See-through, so the outline of where it will land shows underneath.
                    .graphicsLayer { alpha = 0.6f; scaleX = 1.04f; scaleY = 1.04f }
                    .border(2.dp, Color.White, RoundedCornerShape(26.dp)),
            ) {
                WidgetContent(held.widgetId, data, now, {}, {})
            }
        }
    }
}

private data class CatalogTarget(val pageId: String, val stackId: String?)

/**
 * Long press anywhere on the page, even over a list item or a widget. Once it fires, the rest of
 * the touch is swallowed so the item under the finger is not also opened on release.
 */
fun Modifier.longPressAnywhere(onLongPress: () -> Unit): Modifier = pointerInput(Unit) {
    val latest = onLongPress
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        // Null means the finger stayed down and still past the timeout: a long press.
        val released = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            var ended = false
            while (!ended) {
                val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id }
                ended = change == null || !change.pressed || (change.position - down.position).getDistance() > viewConfiguration.touchSlop
            }
            true
        }
        if (released == null) {
            latest()
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                event.changes.forEach { it.consume() }
            } while (event.changes.any { it.pressed })
        }
    }
}

@Composable
private fun EditablePage(
    page: HomePage,
    isMain: Boolean,
    onSetMain: () -> Unit,
    onDelete: () -> Unit,
    onTapPage: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            HouseIcon(isMain, Modifier.clickable(role = Role.Button, onClick = onSetMain).padding(12.dp).semantics {
                contentDescription = if (isMain) "Página principal" else "Hacer página principal"
            })
            SmallLabel(
                when (page.kind) { PageKind.APPS -> "APPS"; PageKind.WEEK -> "SEMANA"; PageKind.WIDGETS -> "WIDGETS" },
                Modifier.weight(1f),
                color = if (isMain) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // The app list can't be deleted: apps only live there.
            if (page.kind != PageKind.APPS) {
                TrashIcon(Modifier.clickable(role = Role.Button, onClick = onDelete).padding(12.dp).semantics { contentDescription = "Borrar página" })
            }
        }
        val shape = RoundedCornerShape(28.dp)
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(shape)
                .border(1.dp, if (isMain) MaterialTheme.colorScheme.onSurface else Color(0xFF333333), shape)
                .background(MaterialTheme.colorScheme.background),
        ) {
            Box(Modifier.fillMaxSize().graphicsLayer { scaleX = 0.96f; scaleY = 0.96f }) { content() }
            if (onTapPage != null) {
                Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onTap = { onTapPage() }) })
            }
        }
    }
}

@Composable
private fun AddPageSlot(atStart: Boolean, onAdd: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(top = 56.dp)) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .clickable(role = Role.Button, onClick = onAdd)
                .semantics { contentDescription = if (atStart) "Agregar página a la izquierda" else "Agregar página a la derecha" },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawRoundRect(
                    Color(0xFF444444),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx()),
                    style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f))),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("+", fontSize = 56.sp, color = MaterialTheme.colorScheme.onSurface)
                SmallLabel("NUEVA PÁGINA")
            }
        }
    }
}

@Composable
private fun EditToolbar(weekDeleted: Boolean, onWidgets: () -> Unit, onSettings: () -> Unit, onRestoreWeek: () -> Unit, onDone: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolbarAction("WIDGETS", onWidgets)
        ToolbarAction("AJUSTES", onSettings)
        if (weekDeleted) ToolbarAction("+ SEMANA", onRestoreWeek)
        ToolbarAction("LISTO", onDone, highlight = true)
    }
}

@Composable
private fun ToolbarAction(label: String, onClick: () -> Unit, highlight: Boolean = false) {
    SmallLabel(
        label,
        Modifier.clickable(role = Role.Button, onClick = onClick).padding(horizontal = 12.dp, vertical = 14.dp),
        color = if (highlight) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Outline house; filled when it is the main page. */
@Composable
private fun HouseIcon(filled: Boolean, modifier: Modifier = Modifier) {
    val color = if (filled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.08f)
            lineTo(w * 0.94f, h * 0.48f)
            lineTo(w * 0.8f, h * 0.48f)
            lineTo(w * 0.8f, h * 0.92f)
            lineTo(w * 0.2f, h * 0.92f)
            lineTo(w * 0.2f, h * 0.48f)
            lineTo(w * 0.06f, h * 0.48f)
            close()
        }
        if (filled) drawPath(path, color) else drawPath(path, color, style = Stroke(1.5.dp.toPx()))
    }
}

@Composable
private fun TrashIcon(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.5.dp.toPx()
        drawLine(color, Offset(w * 0.12f, h * 0.22f), Offset(w * 0.88f, h * 0.22f), stroke)
        drawLine(color, Offset(w * 0.38f, h * 0.1f), Offset(w * 0.62f, h * 0.1f), stroke)
        val body = Path().apply {
            moveTo(w * 0.22f, h * 0.22f)
            lineTo(w * 0.28f, h * 0.92f)
            lineTo(w * 0.72f, h * 0.92f)
            lineTo(w * 0.78f, h * 0.22f)
        }
        drawPath(body, color, style = Stroke(stroke))
        drawLine(color, Offset(w * 0.42f, h * 0.38f), Offset(w * 0.42f, h * 0.78f), stroke)
        drawLine(color, Offset(w * 0.58f, h * 0.38f), Offset(w * 0.58f, h * 0.78f), stroke)
    }
}

/**
 * Widget catalog: search, widgets in alphabetical order with how many layouts each has, and an
 * accordion with a live preview of every layout. Tapping a layout selects it; AÑADIR places it.
 */
@Composable
private fun WidgetCatalog(
    data: HomeData,
    now: LocalDateTime,
    addingToStack: Boolean,
    hidden: Boolean,
    onAdd: (widgetId: String, size: GridSize) -> Unit,
    onPick: (widgetId: String, size: GridSize, window: Offset) -> Unit,
    onCarry: (Offset) -> Unit,
    onDrop: () -> Unit,
    onCancel: () -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    var query by rememberSaveable { mutableStateOf("") }
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<Pair<String, GridSize>?>(null) }
    val widgets = WIDGET_IDS
        .map { it to (WIDGET_LABELS[it] ?: it) }
        .filter { (_, label) -> label.contains(query.trim(), ignoreCase = true) }
        .sortedBy { (_, label) -> label.lowercase() }
    val latestAdd by rememberUpdatedState(onAdd)
    val latestPick by rememberUpdatedState(onPick)
    val latestCarry by rememberUpdatedState(onCarry)
    val latestDrop by rememberUpdatedState(onDrop)
    val latestCancel by rememberUpdatedState(onCancel)

    Column(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = if (hidden) 0f else 1f }
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp),
    ) {
        ModuleHeader(if (addingToStack) "AGREGAR A LA PILA" else "WIDGETS", onClose, Modifier.padding(top = 8.dp))
        ModuleTextField(query, { query = it }, "Buscar widgets", Modifier.fillMaxWidth().padding(vertical = 8.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (widgets.isEmpty()) SmallLabel("SIN RESULTADOS", Modifier.padding(vertical = 16.dp))
            widgets.forEach { (id, label) ->
                // Joining a stack takes the stack's size, so there is only one layout to pick.
                val sizes = if (addingToStack) sizesFor(id).take(1) else sizesFor(id)
                Row(
                    Modifier.fillMaxWidth().clickable(role = Role.Button) { openId = if (openId == id) null else id }.padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    SmallLabel("${sizes.size}")
                    SmallLabel(if (openId == id) "  ▾" else "  ▸")
                }
                AnimatedVisibility(openId == id) {
                    Column(Modifier.padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        sizes.forEach { size ->
                            val isSelected = selected == (id to size)
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent, RoundedCornerShape(30.dp))
                                    .padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                // Same proportions as on the page: w of 4 columns, h rows of GRID_CELL_HEIGHT.
                                var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                                Box(
                                    Modifier
                                        .fillMaxWidth(size.w / GRID_COLUMNS.toFloat())
                                        .height(GRID_CELL_HEIGHT * size.h + GRID_GAP * (size.h - 1))
                                        .onGloballyPositioned { coords = it },
                                ) {
                                    WidgetContent(id, data, now, {}, {})
                                    // Live preview under a cover: a tap selects the layout; holding it picks the widget
                                    // up to drop it on a page (not when joining a stack, which already has its place).
                                    Box(
                                        Modifier
                                            .matchParentSize()
                                            .pointerInput(id, size) { detectTapGestures(onTap = { selected = id to size }) }
                                            .then(
                                                if (addingToStack) Modifier else Modifier.pointerInput(id, size) {
                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = { at -> coords?.let { latestPick(id, size, it.localToWindow(at)) } },
                                                        onDragEnd = { latestDrop() },
                                                        onDragCancel = { latestCancel() },
                                                    ) { change, amount ->
                                                        change.consume()
                                                        latestCarry(amount)
                                                    }
                                                },
                                            ),
                                    )
                                }
                                SmallLabel(size.label, color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(CardLine))
            }
        }
        val choice = selected
        Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .height(52.dp)
                .background(if (choice != null) MaterialTheme.colorScheme.onSurface else CardFill, RoundedCornerShape(26.dp))
                .clickable(enabled = choice != null, role = Role.Button) { choice?.let { latestAdd(it.first, it.second) } },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (choice == null) "ELIGE UN DISEÑO" else "AÑADIR ${(WIDGET_LABELS[choice.first] ?: choice.first).uppercase()} · ${choice.second.label}",
                style = MaterialTheme.typography.labelSmall,
                color = if (choice != null) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
