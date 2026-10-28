package com.adrian.minimallauncher.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.adrian.minimallauncher.data.WidgetStackConfig
import com.adrian.minimallauncher.data.defaultBgAlpha
import com.adrian.minimallauncher.feature.CardFill
import com.adrian.minimallauncher.feature.CardLine
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.feature.ai.AI_STRIP
import com.adrian.minimallauncher.feature.ai.AiApp
import com.adrian.minimallauncher.feature.ai.AiWidget
import com.adrian.minimallauncher.feature.ai.LocalAiReplay
import com.adrian.minimallauncher.feature.exercise.ExerciseWidget
import com.adrian.minimallauncher.feature.vault.VaultWidget
import com.adrian.minimallauncher.ui.AlarmTile
import com.adrian.minimallauncher.ui.ClockTile
import com.adrian.minimallauncher.ui.ExpensesTile
import com.adrian.minimallauncher.ui.GRID_CELL_HEIGHT
import com.adrian.minimallauncher.ui.GRID_GAP
import com.adrian.minimallauncher.ui.HomeData
import com.adrian.minimallauncher.ui.MealsTile
import com.adrian.minimallauncher.ui.NoteTile
import com.adrian.minimallauncher.ui.Tile
import com.adrian.minimallauncher.ui.WaterTile
import com.adrian.minimallauncher.ui.WorldTile
import java.time.LocalDateTime
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val TileShape = RoundedCornerShape(26.dp)

/** How long the Bancos padlock shows itself opening before the fingerprint prompt comes up. */
private const val VAULT_OPEN_MS = 450L

val WIDGET_LABELS = mapOf(
    "clock" to "Reloj",
    "water" to "Agua",
    "alarms" to "Alarma",
    "exercise" to "Ejercicio",
    "finance" to "Finanzas",
    "diet" to "Dieta",
    "world" to "Reloj Mundial",
    "vault" to "Bancos",
    "notes" to "Nota Fijada",
    AI_STRIP to "Asistentes IA",
    "routine" to "Rutinas",
) + AiApp.entries.associate { it.widgetId to it.label }

/**
 * A customizable tile or smart stack of tiles on the home screen.
 * Always shows its real live preview (even in edit mode).
 * - Long-pressing the widget on the normal screen selects it in place ([isSelected]): shows the
 *   One UI resize frame with 4 white corner brackets and the 3-action popup (Editar pila, Eliminar, Ajustes).
 * - Full-page edit mode ([isEditMode]) keeps the live preview visible with a minus badge and frame.
 */
@Composable
fun WidgetStackTile(
    stack: WidgetStackConfig,
    data: HomeData,
    now: LocalDateTime,
    isEditMode: Boolean,
    onOpenModule: (String) -> Unit,
    onLongClick: () -> Unit,
    onRemoveWidget: (String) -> Unit,
    onAddWidgetToStack: () -> Unit,
    modifier: Modifier = Modifier,
    onRemoveStack: () -> Unit = {},
    onSetAlpha: (Float) -> Unit = {},
    isSelected: Boolean = false,
    showPopup: Boolean = false,
    onDismissSelect: () -> Unit = {},
    isMergeTarget: Boolean = false,
) {
    val borderColor by animateColorAsState(
        when {
            isMergeTarget -> Color.White
            else -> CardLine
        },
        label = "tile_border",
    )
    // Opens slightly when another widget hovers over it; snaps together when the widget lands in the stack.
    val hoverScale by animateFloatAsState(
        if (isMergeTarget) 1.05f else 1f,
        label = "tile_merge_hover",
    )
    val mergeBounce = remember { Animatable(1f) }
    val prevCount = remember { mutableIntStateOf(stack.widgetIds.size) }
    val reactions = LocalReactions.current
    val off = animationsOff()
    LaunchedEffect(stack.widgetIds.size) {
        val grew = stack.widgetIds.size > prevCount.intValue
        prevCount.intValue = stack.widgetIds.size
        if (grew) {
            reactions.tick()
            if (!off) {
                mergeBounce.snapTo(0.90f)
                mergeBounce.animateTo(1.06f, tween(130))
                mergeBounce.animateTo(1f, tween(180))
            }
        }
    }

    var showSettings by remember { mutableStateOf(false) }
    var showStackEditor by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(pageCount = { stack.widgetIds.size })
    // Swiping the stack onto a widget counts as it coming into view (assistant icons animate then).
    val outerReplay = LocalAiReplay.current
    val shows = remember { mutableStateMapOf<Int, Int>() }
    val firstSettle = remember { booleanArrayOf(true) }
    LaunchedEffect(pagerState.settledPage) {
        if (firstSettle[0]) { firstSettle[0] = false; return@LaunchedEffect }
        shows[pagerState.settledPage] = (shows[pagerState.settledPage] ?: 0) + 1
    }

    val shownIndex = pagerState.currentPage.coerceIn(0, (stack.widgetIds.size - 1).coerceAtLeast(0))
    val activeWidget = stack.widgetIds.getOrNull(shownIndex) ?: ""
    val activeLabel = WIDGET_LABELS[activeWidget] ?: activeWidget.uppercase()
    val padded = stack.widgetIds.size > 1 || stack.bgAlpha > 0f

    Box(
        modifier
            .fillMaxSize()
            .graphicsLayer {
                val s = hoverScale * mergeBounce.value
                scaleX = s
                scaleY = s
            },
    ) {
        // Real live widget preview (never covered by a gray overlay).
        Box(
            Modifier
                .fillMaxSize()
                .background(CardFill.copy(alpha = stack.bgAlpha), TileShape)
                .border(if (isMergeTarget) 2.dp else 1.dp, borderColor.copy(alpha = if (isMergeTarget) 1f else stack.bgAlpha), TileShape),
        ) {
            if (stack.widgetIds.size > 1) {
                Box(Modifier.fillMaxSize()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = !isEditMode && !isSelected,
                    ) { page ->
                        val widgetId = stack.widgetIds.getOrNull(page) ?: return@HorizontalPager
                        CompositionLocalProvider(LocalAiReplay provides outerReplay + (shows[page] ?: 0)) {
                            WidgetContent(widgetId, data, now, onOpenModule, onLongClick, padded = padded)
                        }
                    }
                    // The page dots show while swiping or when selected/editing.
                    var dotsShown by remember { mutableStateOf(false) }
                    LaunchedEffect(pagerState.isScrollInProgress) {
                        if (pagerState.isScrollInProgress) dotsShown = true else { delay(1_000L); dotsShown = false }
                    }
                    val dotsAlpha by animateFloatAsState(if (dotsShown || isSelected || isEditMode) 1f else 0f, label = "stack_dots")
                    Row(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp)
                            .graphicsLayer { alpha = dotsAlpha },
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        repeat(stack.widgetIds.size) { i ->
                            val active = pagerState.currentPage == i
                            Box(
                                Modifier
                                    .size(if (active) 6.dp else 5.dp)
                                    .background(if (active) Color.White else Color(0xFF666666), CircleShape),
                            )
                        }
                    }
                }
            } else {
                val widgetId = stack.widgetIds.firstOrNull() ?: return@Box
                WidgetContent(widgetId, data, now, onOpenModule, onLongClick, padded = padded)
            }
        }

        // In full-page edit mode or when selected in place, intercept taps on the widget so it doesn't open the module.
        if (isEditMode || isSelected) {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(isEditMode, isSelected) {
                        detectTapGestures(
                            onTap = {
                                if (isEditMode) onLongClick() else onDismissSelect()
                            },
                        )
                    },
            )
            // One UI style resize frame: thin gray border + 4 thick white corner L-brackets.
            ResizeCornerFrame(Modifier.fillMaxSize())
        }

        // In full-page edit mode, also show the white circle with red minus at the top-left corner.
        if (isEditMode) {
            MinusBadge(
                contentDescription = "Quitar ${activeLabel.lowercase()}",
                onClick = { onRemoveWidget(activeWidget) },
                modifier = Modifier.align(Alignment.TopStart).offset((-6).dp, (-6).dp),
            )
        }

        // One UI floating popup menu (Image 1) when the widget is long-pressed on the page.
        if (showPopup) {
            WidgetActionPopup(
                title = activeLabel,
                isStack = stack.widgetIds.size > 1,
                onEditStack = {
                    onDismissSelect()
                    showStackEditor = true
                },
                onDelete = {
                    onDismissSelect()
                    onRemoveStack()
                },
                onSettings = {
                    onDismissSelect()
                    showSettings = true
                },
                onDismiss = onDismissSelect,
            )
        }
    }

    // Stack editor modal (Image 2): carousel of live widget previews with '-' badge and '+' card at the end.
    if (showStackEditor) {
        StackEditorDialog(
            stack = stack,
            data = data,
            now = now,
            onRemoveWidget = { widgetId ->
                onRemoveWidget(widgetId)
                if (stack.widgetIds.size <= 1) showStackEditor = false
            },
            onAddWidget = {
                showStackEditor = false
                onAddWidgetToStack()
            },
            onDismiss = { showStackEditor = false },
        )
    }

    if (showSettings) {
        WidgetSettingsSheet(
            stack = stack,
            data = data,
            now = now,
            // The strip's apps, order and names live in its module settings.
            onMore = if (AI_STRIP in stack.widgetIds) ({ showSettings = false; onOpenModule("settings:ai") }) else null,
            onSave = { alpha -> onSetAlpha(alpha); showSettings = false },
            onDismiss = { showSettings = false },
        )
    }
}

/**
 * One UI resize frame around a selected widget: thin gray outline with 4 thick white rounded corner brackets.
 */
@Composable
private fun ResizeCornerFrame(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val r = 26.dp.toPx().coerceAtMost(minOf(w, h) / 2f)
        val thin = 1.5.dp.toPx()
        val thick = 3.6.dp.toPx()
        val arm = 18.dp.toPx()
        val gray = Color(0xFF6E6E6E)
        val white = Color(0xFFF2F2F2)

        // Subtle full rounded outline.
        drawRoundRect(gray, cornerRadius = CornerRadius(r, r), style = Stroke(thin))

        // Top-Left (180° -> 270°)
        val tl = Path().apply {
            moveTo(0f, r + arm * 0.4f)
            lineTo(0f, r)
            arcTo(Rect(0f, 0f, 2 * r, 2 * r), 180f, 90f, false)
            lineTo(r + arm * 0.4f, 0f)
        }
        // Top-Right (270° -> 360°)
        val tr = Path().apply {
            moveTo(w - r - arm * 0.4f, 0f)
            lineTo(w - r, 0f)
            arcTo(Rect(w - 2 * r, 0f, w, 2 * r), 270f, 90f, false)
            lineTo(w, r + arm * 0.4f)
        }
        // Bottom-Right (0° -> 90°)
        val br = Path().apply {
            moveTo(w, h - r - arm * 0.4f)
            lineTo(w, h - r)
            arcTo(Rect(w - 2 * r, h - 2 * r, w, h), 0f, 90f, false)
            lineTo(w - r - arm * 0.4f, h)
        }
        // Bottom-Left (90° -> 180°)
        val bl = Path().apply {
            moveTo(r + arm * 0.4f, h)
            lineTo(r, h)
            arcTo(Rect(0f, h - 2 * r, 2 * r, h), 90f, 90f, false)
            lineTo(0f, h - r - arm * 0.4f)
        }

        val bracketStroke = Stroke(thick, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(tl, white, style = bracketStroke)
        drawPath(tr, white, style = bracketStroke)
        drawPath(br, white, style = bracketStroke)
        drawPath(bl, white, style = bracketStroke)
    }
}

/** White circle with a red horizontal minus bar, as in One UI's stack editor. */
@Composable
fun MinusBadge(contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(26.dp)
            .shadow(4.dp, CircleShape)
            .background(Color(0xFFF2F2F2), CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .width(12.dp)
                .height(2.5.dp)
                .background(LauncherRed, RoundedCornerShape(2.dp)),
        )
    }
}

/**
 * Floating One UI popup menu anchored below (or above if near the bottom of the screen) the widget.
 * Shows the widget title and the 3 actions: Editar pila, Eliminar / Eliminar todo, Ajustes.
 */
@Composable
private fun WidgetActionPopup(
    title: String,
    isStack: Boolean,
    onEditStack: () -> Unit,
    onDelete: () -> Unit,
    onSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    var placeAbove by remember { mutableStateOf(false) }
    var arrowX by remember { mutableStateOf(0.5f) }
    val positionProvider = remember {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val margin = 24
                val x = (anchorBounds.center.x - popupContentSize.width / 2)
                    .coerceIn(margin, (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin))
                arrowX = if (popupContentSize.width > 0) {
                    ((anchorBounds.center.x - x).toFloat() / popupContentSize.width).coerceIn(0.15f, 0.85f)
                } else 0.5f

                val gap = 16
                val belowY = anchorBounds.bottom + gap
                val aboveY = anchorBounds.top - popupContentSize.height - gap
                val above = belowY + popupContentSize.height > windowSize.height - 48 && aboveY >= 48
                placeAbove = above
                return IntOffset(x, if (above) aboveY else belowY)
            }
        }
    }

    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false, dismissOnClickOutside = false),
    ) {
        AnimatedVisibility(
            visible = appeared,
            enter = scaleIn(
                animationSpec = tween(180),
                initialScale = 0.82f,
                transformOrigin = TransformOrigin(arrowX, if (placeAbove) 1f else 0f),
            ) + fadeIn(tween(150)),
            exit = scaleOut(tween(120)) + fadeOut(tween(120)),
        ) {
            val bubbleColor = Color(0xFF222226)
            val borderColor = Color(0xFF34343A)
            Column(
                Modifier.width(268.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (!placeAbove) {
                    PopupArrow(up = true, arrowX = arrowX, color = bubbleColor)
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .shadow(18.dp, RoundedCornerShape(26.dp))
                        .background(bubbleColor, RoundedCornerShape(26.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(26.dp)),
                ) {
                    // Header: Widget title centered
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 15.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            title,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(borderColor))
                    // 3 actions: Editar pila, Eliminar (todo), Ajustes
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PopupActionItem(
                            label = "Editar pila",
                            onClick = onEditStack,
                            modifier = Modifier.weight(1f),
                        ) { PencilLineIcon(Color.White) }

                        PopupActionItem(
                            label = if (isStack) "Eliminar todo" else "Eliminar",
                            onClick = onDelete,
                            modifier = Modifier.weight(1f),
                        ) { TrashLineIcon(Color.White) }

                        PopupActionItem(
                            label = "Ajustes",
                            onClick = onSettings,
                            modifier = Modifier.weight(1f),
                        ) { GearLineIcon(Color.White, Modifier.size(20.dp)) }
                    }
                }
                if (placeAbove) {
                    PopupArrow(up = false, arrowX = arrowX, color = bubbleColor)
                }
            }
        }
    }
}

@Composable
private fun PopupArrow(up: Boolean, arrowX: Float, color: Color) {
    Canvas(Modifier.fillMaxWidth().height(8.dp)) {
        val cx = size.width * arrowX
        val w = 14.dp.toPx()
        val h = size.height
        val path = Path().apply {
            if (up) {
                moveTo(cx - w / 2f, h)
                lineTo(cx, 0f)
                lineTo(cx + w / 2f, h)
            } else {
                moveTo(cx - w / 2f, 0f)
                lineTo(cx, h)
                lineTo(cx + w / 2f, 0f)
            }
            close()
        }
        drawPath(path, color)
    }
}

@Composable
private fun PopupActionItem(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        icon()
        Text(
            label,
            color = Color(0xFFEAEAEA),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Stack editor modal (Image 2): rounded dark container showing a horizontal carousel of the stack's
 * widgets as live previews (each with a top-left white/red minus badge to remove it) and a '+' card
 * at the end to add another widget to the stack, plus page indicator dots at the bottom.
 */
@Composable
private fun StackEditorDialog(
    stack: WidgetStackConfig,
    data: HomeData,
    now: LocalDateTime,
    onRemoveWidget: (String) -> Unit,
    onAddWidget: () -> Unit,
    onDismiss: () -> Unit,
) {
    val totalPages = stack.widgetIds.size + 1
    val pagerState = rememberPagerState(pageCount = { totalPages })
    val rows = stack.h.coerceIn(1, 3)
    val previewHeight = GRID_CELL_HEIGHT * rows + GRID_GAP * (rows - 1)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .pointerInput(Unit) { detectTapGestures { onDismiss() } },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier
                    .fillMaxWidth(0.90f)
                    .fillMaxHeight(0.60f)
                    .background(Color(0xFF28282C), RoundedCornerShape(36.dp))
                    .border(1.dp, Color(0xFF3C3C42), RoundedCornerShape(36.dp))
                    // Consume taps inside the card so they don't dismiss the dialog.
                    .pointerInput(Unit) { detectTapGestures { } }
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(Modifier.height(12.dp))

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 34.dp),
                    pageSpacing = 16.dp,
                ) { page ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (page < stack.widgetIds.size) {
                            val widgetId = stack.widgetIds[page]
                            val label = WIDGET_LABELS[widgetId] ?: widgetId
                            val bg = if (widgetId == "clock" && stack.bgAlpha == 0f) 0f else stack.bgAlpha.coerceAtLeast(defaultBgAlpha(listOf(widgetId)))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF606066), RoundedCornerShape(30.dp))
                                    .padding(8.dp),
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(previewHeight)
                                        .widgetCard(bg),
                                ) {
                                    WidgetContent(widgetId, data, now, {}, {}, padded = bg > 0f || stack.widgetIds.size > 1)
                                    // Swallow taps inside the preview so it doesn't open the module while editing the stack.
                                    Box(Modifier.matchParentSize().pointerInput(Unit) { detectTapGestures { } })
                                }
                                MinusBadge(
                                    contentDescription = "Quitar $label de la pila",
                                    onClick = { onRemoveWidget(widgetId) },
                                    modifier = Modifier.align(Alignment.TopStart).offset((-12).dp, (-12).dp),
                                )
                            }
                        } else {
                            // Final card: '+' to add another widget to this stack.
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF606066), RoundedCornerShape(30.dp))
                                    .padding(8.dp),
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(previewHeight)
                                        .clip(TileShape)
                                        .background(Color(0xFF18181C), TileShape)
                                        .clickable(role = Role.Button, onClick = onAddWidget)
                                        .semantics { contentDescription = "Agregar widget a la pila" },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Canvas(Modifier.size(36.dp)) {
                                        val stroke = 2.5.dp.toPx()
                                        val m = size.width * 0.2f
                                        drawLine(Color.White, Offset(m, center.y), Offset(size.width - m, center.y), stroke, StrokeCap.Round)
                                        drawLine(Color.White, Offset(center.x, m), Offset(center.x, size.height - m), stroke, StrokeCap.Round)
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom page indicator dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(totalPages) { i ->
                        val active = pagerState.currentPage == i
                        Box(
                            Modifier
                                .size(7.dp)
                                .background(if (active) Color.White else Color(0xFF6E6E74), CircleShape),
                        )
                    }
                }
            }
        }
    }
}

/** The card behind a widget: fill and outline at [bgAlpha] (0 = only the content shows, 1 = solid). */
fun Modifier.widgetCard(bgAlpha: Float): Modifier =
    background(CardFill.copy(alpha = bgAlpha), TileShape).border(1.dp, CardLine.copy(alpha = bgAlpha), TileShape)

/** 2-dp line gear icon with rounded teeth. */
@Composable
fun GearLineIcon(color: Color, modifier: Modifier = Modifier.size(18.dp)) {
    Canvas(modifier) {
        val stroke = 1.6.dp.toPx()
        val r = size.minDimension * 0.28f
        val outer = size.minDimension * 0.45f
        drawCircle(color, radius = r, style = Stroke(stroke))
        for (i in 0 until 6) {
            val a = Math.toRadians(i * 60.0)
            val c = cos(a).toFloat()
            val s = sin(a).toFloat()
            drawLine(
                color,
                Offset(center.x + c * r, center.y + s * r),
                Offset(center.x + c * outer, center.y + s * outer),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** 2-dp line pencil icon for "Editar pila". */
@Composable
private fun PencilLineIcon(color: Color, modifier: Modifier = Modifier.size(20.dp)) {
    Canvas(modifier) {
        val stroke = 1.6.dp.toPx()
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.16f, h * 0.84f)
            lineTo(w * 0.22f, h * 0.62f)
            lineTo(w * 0.68f, h * 0.16f)
            lineTo(w * 0.84f, h * 0.32f)
            lineTo(w * 0.38f, h * 0.78f)
            close()
        }
        drawPath(path, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** 2-dp line trash icon for "Eliminar" / "Eliminar todo". */
@Composable
private fun TrashLineIcon(color: Color, modifier: Modifier = Modifier.size(20.dp)) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.6.dp.toPx()
        drawLine(color, Offset(w * 0.16f, h * 0.24f), Offset(w * 0.84f, h * 0.24f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.38f, h * 0.12f), Offset(w * 0.62f, h * 0.12f), stroke, StrokeCap.Round)
        val body = Path().apply {
            moveTo(w * 0.24f, h * 0.24f)
            lineTo(w * 0.30f, h * 0.88f)
            lineTo(w * 0.70f, h * 0.88f)
            lineTo(w * 0.76f, h * 0.24f)
        }
        drawPath(body, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color, Offset(w * 0.43f, h * 0.40f), Offset(w * 0.43f, h * 0.72f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.57f, h * 0.40f), Offset(w * 0.57f, h * 0.72f), stroke, StrokeCap.Round)
    }
}

@Composable
fun WidgetContent(
    widgetId: String,
    data: HomeData,
    now: LocalDateTime,
    onOpenModule: (String) -> Unit,
    onLongClick: () -> Unit,
    padded: Boolean = false,
) {
    val fillMod = Modifier.fillMaxSize()
    val onClick = { onOpenModule(widgetId) }
    when (widgetId) {
        "clock" -> ClockTile(now, fillMod, { onOpenModule("alarms") }, onLongClick, padded = padded)
        "alarms" -> AlarmTile(now, fillMod, onClick, onLongClick)
        "exercise" -> Tile(fillMod, onClick, onLongClick) { ExerciseWidget(data.exercise, data.settings.exerciseStart) }
        "finance" -> ExpensesTile(data.records, now.toLocalDate(), data.settings.dailyBudgetCents, fillMod, onClick, onLongClick)
        "diet" -> MealsTile(data.records.completedHabits, now, data.settings.meals, data.onSetHabit, fillMod, onClick, onLongClick)
        "water" -> WaterTile(data.records.completedHabits, now, data.settings, data.onSetHabit, fillMod, { onOpenModule("diet") }, onLongClick)
        "world" -> WorldTile(data.settings.worldZones, fillMod, onClick, onLongClick)
        "vault" -> {
            // Tapping plays the padlock opening, then asks for the fingerprint.
            var opening by remember { mutableIntStateOf(0) }
            val scope = rememberCoroutineScope()
            Tile(fillMod, { opening++; scope.launch { delay(VAULT_OPEN_MS); onClick() } }, onLongClick) { VaultWidget(opening) }
        }
        "notes" -> NoteTile(data.records.notes, onClick, onLongClick)
        "routine" -> RoutineTile(data.routine, fillMod, onOpenModule, onLongClick)
        // Each assistant opens its own app, so the widget gets the launcher's routes ("app:…") instead of a module.
        AI_STRIP -> AiWidget(widgetId, data.settings, data.ai, onOpenModule, onLongClick, fillMod)
        else -> if (AiApp.fromWidget(widgetId) != null) AiWidget(widgetId, data.settings, data.ai, onOpenModule, onLongClick, fillMod) else {
            Tile(fillMod, onClick, onLongClick) {
                Text(
                    WIDGET_LABELS[widgetId] ?: widgetId.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
