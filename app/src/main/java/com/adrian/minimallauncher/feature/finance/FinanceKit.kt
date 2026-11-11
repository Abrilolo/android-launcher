package com.adrian.minimallauncher.feature.finance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.adrian.minimallauncher.feature.LauncherAccent
import com.adrian.minimallauncher.feature.LauncherDanger
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
import kotlinx.coroutines.launch

/*
 * The pieces of Finanzas' planned-vs-real views (2026-11-11): see-through cards, little boxes that come out of what
 * was tapped, the (i) for context, and dots that tell a state without painting everything.
 */

/** See-through cards: a faint white over black, never a gradient. */
internal val GlassFill = Color.White.copy(alpha = 0.05f)
internal val GlassLine = Color.White.copy(alpha = 0.09f)
private val PopFill = Color(0xFF1B1B1B)
private val PopLine = Color.White.copy(alpha = 0.14f)

/** A see-through card; [dashed] marks what is planned and has not happened. */
internal fun Modifier.glass(radius: Dp = 18.dp, dashed: Boolean = false): Modifier =
    if (!dashed) background(GlassFill, RoundedCornerShape(radius)).border(1.dp, GlassLine, RoundedCornerShape(radius))
    else drawBehind {
        val r = radius.toPx()
        drawRoundRect(
            Color.White.copy(alpha = 0.32f), cornerRadius = CornerRadius(r),
            style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))),
        )
    }

/** Places the box under what was tapped, or over it when there is no room, always inside the screen. */
private class AnchorBelow(private val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val x = anchorBounds.left.coerceIn(gap, (windowSize.width - popupContentSize.width - gap).coerceAtLeast(gap))
        val below = anchorBounds.bottom + gap
        val y = if (below + popupContentSize.height <= windowSize.height - gap) below else (anchorBounds.top - popupContentSize.height - gap).coerceAtLeast(gap)
        return IntOffset(x, y)
    }
}

/** The box that comes out of something tapped: short rows, actions as icons. */
@Composable
internal fun PopBox(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Popup(popupPositionProvider = remember { AnchorBelow(24) }, onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
        Column(
            Modifier
                .widthIn(min = 200.dp, max = 300.dp)
                .background(PopFill, RoundedCornerShape(16.dp))
                .border(1.dp, PopLine, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

/** [content] that opens [pop] when tapped; the box closes with a tap outside it. */
@Composable
internal fun Pop(
    modifier: Modifier = Modifier,
    description: String? = null,
    pop: @Composable ColumnScope.(close: () -> Unit) -> Unit,
    content: @Composable () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    Box(
        modifier
            .clickable(remember { MutableInteractionSource() }, null, role = Role.Button) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                open = !open
            }
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
    ) {
        content()
        if (open) PopBox({ open = false }) { pop { open = false } }
    }
}

/** The (i): a tap shows what something means. */
@Composable
internal fun InfoIcon(text: String) {
    Pop(Modifier.size(36.dp), "Qué es", pop = { Text(text, style = MaterialTheme.typography.bodyMedium, color = FinanceWhite) }) {
        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(18.dp)) {
                val s = 1.5.dp.toPx()
                drawCircle(LabelCream, size.minDimension / 2 - s / 2, style = Stroke(s))
                drawLine(LabelCream, Offset(center.x, size.height * 0.45f), Offset(center.x, size.height * 0.72f), s)
                drawCircle(LabelCream, s * 0.7f, Offset(center.x, size.height * 0.3f))
            }
        }
    }
}

/** A row in a box: what it is on the left, the amount on the right, an optional dot first. */
@Composable
internal fun PopRow(label: String, value: String, valueColor: Color = FinanceWhite, dot: Color? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        dot?.let { Box(Modifier.size(7.dp).background(it, CircleShape)) }
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = FinanceWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor, maxLines = 1)
    }
}

@Composable
internal fun PopRule() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(GlassLine))
}

/** The state of something planned: hollow and dashed (to come), blue ring (today), check (done), pink (late). */
@Composable
internal fun StateDot(state: PlanState, size: Dp = 12.dp) {
    Canvas(Modifier.size(size)) {
        val s = 1.6.dp.toPx()
        val r = this.size.minDimension / 2 - s
        when (state) {
            PlanState.PLANNED -> drawCircle(FinanceWhite.copy(alpha = 0.7f), r, style = Stroke(s, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.5.dp.toPx(), 2.dp.toPx()))))
            PlanState.TODAY -> drawCircle(LauncherAccent, r, style = Stroke(s * 1.3f))
            PlanState.LATE -> drawCircle(LauncherDanger, r + s / 2)
            PlanState.DONE -> {
                val w = this.size.width
                val h = this.size.height
                drawLine(FinanceWhite, Offset(w * 0.12f, h * 0.55f), Offset(w * 0.4f, h * 0.82f), s * 1.2f)
                drawLine(FinanceWhite, Offset(w * 0.4f, h * 0.82f), Offset(w * 0.9f, h * 0.2f), s * 1.2f)
            }
        }
    }
}

/** A small round dot of [color], for a category or a state beside a name. */
@Composable
internal fun Dot(color: Color, size: Dp = 8.dp) {
    Box(Modifier.size(size).background(color, CircleShape))
}

/** A section's heading: its name, and the (i) and quick actions on the right. */
@Composable
internal fun SectionHead(title: String, info: String? = null, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
        SectionLabel(title)
        info?.let { InfoIcon(it) }
        Box(Modifier.weight(1f))
        trailing()
    }
}

/** A subtitle: small capitals in cream, so it reads apart from the view's title (white, big) and the content (white). */
@Composable
internal fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = LabelCream) {
    Text(
        text.uppercase(java.util.Locale.forLanguageTag("es")), modifier,
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = androidx.compose.ui.unit.TextUnit(1.4f, androidx.compose.ui.unit.TextUnitType.Sp)),
        color = color, maxLines = 1,
    )
}

/** The palette's accent for labels and dates (2026-11-09): subtitles are cream. */
internal val LabelCream = Color(0xFFF2F0C4)
/** Lighter blue of the data palette, readable as text on black (the accent itself is for actions). */
internal val TextBlue = Color(0xFF72B5EA)

/** What each action's icon is painted with: add blue, done green, delete pink, the rest white. */
internal object ActionColor {
    val Add = LauncherAccent
    val Done = MoneyPlenty
    val Delete = LauncherDanger
    val Neutral = FinanceWhite
}

/** A card tinted by what it holds: [color] faint behind, its line a little stronger; dashed when it is planned. */
internal fun Modifier.tinted(color: Color, radius: Dp = 18.dp, dashed: Boolean = false): Modifier =
    background(color.copy(alpha = 0.08f), RoundedCornerShape(radius)).then(
        if (!dashed) border(1.dp, color.copy(alpha = 0.32f), RoundedCornerShape(radius))
        else drawBehind {
            drawRoundRect(
                color.copy(alpha = 0.6f), cornerRadius = CornerRadius(radius.toPx()),
                style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))),
            )
        },
    )

/** An icon-only action in its color (see [ActionColor]), 44 dp to touch. */
@Composable
internal fun FinIcon(icon: String, description: String, color: Color = ActionColor.Neutral, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { StrokeIcon(icon, StrokeState.DONE, 22.dp, color = color) }
}

/** While a page slides, the one leaving and the one coming are blurred and dimmed by how far they are. */
@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.swipeBlur(state: androidx.compose.foundation.pager.PagerState, page: Int): Modifier = graphicsLayer {
    val off = kotlin.math.abs((state.currentPage - page) + state.currentPageOffsetFraction).coerceIn(0f, 1f)
    alpha = 1f - off * 0.5f
    if (off > 0.01f && android.os.Build.VERSION.SDK_INT >= 31) {
        val r = off * 18.dp.toPx()
        renderEffect = BlurEffect(r, r, TileMode.Decal)
    } else renderEffect = null
}

/** The same blur for a card in a sideways list: by how much of it is out of view. */
internal fun Modifier.edgeBlur(state: androidx.compose.foundation.lazy.LazyListState, index: Int): Modifier = graphicsLayer {
    val info = state.layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.index == index }
    val out = if (item == null) 0f else {
        val start = info.viewportStartOffset
        val end = info.viewportEndOffset
        val hidden = maxOf(0, start - item.offset) + maxOf(0, item.offset + item.size - end)
        (hidden.toFloat() / item.size).coerceIn(0f, 1f)
    }
    alpha = 1f - out * 0.6f
    if (out > 0.02f && android.os.Build.VERSION.SDK_INT >= 31) {
        val r = out * 14.dp.toPx()
        renderEffect = BlurEffect(r, r, TileMode.Decal)
    } else renderEffect = null
}

/** A chart with nothing to show yet: faint bars where the data will be, and one short line. */
@Composable
internal fun EmptyChart(text: String, height: Dp = 140.dp, bars: Int = 7) {
    Box(Modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
        Row(Modifier.fillMaxWidth().height(height), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
            val shape = listOf(0.35f, 0.55f, 0.3f, 0.7f, 0.45f, 0.6f, 0.4f)
            repeat(bars) { i ->
                Box(
                    Modifier.weight(1f).fillMaxHeight(shape[i % shape.size])
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)),
                )
            }
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, color = FinanceWhite)
    }
}

/** Pages to swipe sideways, named on top; the dots say where you are and take you there. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SwipePages(titles: List<String>, info: List<String?> = titles.map { null }, page: @Composable (Int) -> Unit) {
    val state = rememberPagerState { titles.size }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(titles[state.currentPage])
            info.getOrNull(state.currentPage)?.let { InfoIcon(it) }
            Box(Modifier.weight(1f))
            titles.indices.forEach { i ->
                Box(
                    Modifier.size(28.dp).clickable(role = Role.Tab) { scope.launch { state.animateScrollToPage(i) } }
                        .semantics { contentDescription = titles[i] },
                    contentAlignment = Alignment.Center,
                ) { Dot(if (i == state.currentPage) FinanceWhite else Color.White.copy(alpha = 0.25f), 7.dp) }
            }
        }
        HorizontalPager(state, Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, pageSpacing = 16.dp) {
            Box(Modifier.swipeBlur(state, it)) { page(it) }
        }
    }
}

/** An icon-only quick action inside a box. */
@Composable
internal fun PopAction(icon: String, description: String, color: Color = ActionColor.Neutral, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).background(color.copy(alpha = 0.12f), CircleShape).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { StrokeIcon(icon, StrokeState.DONE, 22.dp, color = color) }
}

/** The amount with its sign kept short: "$9,888", "-$1,057". */
internal fun short(cents: Long): String = wholePesos(cents)
