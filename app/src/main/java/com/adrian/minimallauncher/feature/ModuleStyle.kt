package com.adrian.minimallauncher.feature

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
import java.time.LocalDate

// The look of the exercise scene (app/src/main/session/home.css) for the native modules.

private val Tile = Color(0xFF161616)
private val Rule = Color(0xFF161616)
private val Faint = Color(0xFFB0B0B0)
private val White = Color(0xFFF5F5F5)

/** Stacks in a chart: white down to dark gray, never a second accent. */
val ChartShades = listOf(0xFFF5F5F5, 0xFFC4C4C4, 0xFF9A9A9A, 0xFF767676, 0xFF5C5C5C, 0xFF474747, 0xFF363636, 0xFF2A2A2A).map(::Color)

private val MonthNames = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

/** "31 oct": the short date every list and chart shows. */
fun shortDay(date: LocalDate) = "${date.dayOfMonth} ${MonthNames[date.monthValue - 1]}"

/** The view's title and its gear. */
@Composable
fun ModuleTitle(title: String, onSettings: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(top = 12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), color = White, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        SettingsGear(onSettings)
    }
}

/** A black frame whose line only glints red on the corners. */
fun Modifier.cornerGlow(corner: Dp, strength: Float) = drawBehind {
    val line = 1.dp.toPx()
    val reach = 30.dp.toPx()
    listOf(Offset.Zero, Offset(size.width, 0f), Offset(0f, size.height), Offset(size.width, size.height)).forEach { at ->
        drawRoundRect(
            Brush.radialGradient(listOf(LauncherRed, Color.Transparent), at, reach),
            Offset(line / 2, line / 2), Size(size.width - line, size.height - line), CornerRadius(corner.toPx()), strength, Stroke(line),
        )
    }
}

/** The main way forward of a view, across its whole width: the red title and what it leads to. */
@Composable
fun ShineLink(title: String, detail: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val run by rememberInfiniteTransition(label = "edge").animateFloat(0f, 1f, infiniteRepeatable(tween(5200)), label = "edge")
    Row(
        modifier
            .fillMaxWidth()
            .drawBehind {
                // A short red line that crosses the top edge now and then.
                val t = (run / 0.6f).coerceAtMost(1f)
                if (t < 1f) {
                    val long = 60.dp.toPx()
                    val x = size.width + long - (size.width + 2 * long) * t
                    drawLine(LauncherRed, Offset(x, 0f), Offset(x + long, 0f), 1.dp.toPx(), alpha = 1f - kotlin.math.abs(2 * t - 1))
                }
            }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp),
        Arrangement.SpaceBetween, Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = LauncherRed, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            detail?.let { SmallLabel(it, maxLines = 1) }
        }
        Text("→", style = MaterialTheme.typography.bodyLarge, color = White)
    }
}

/** One record: an icon, what it is, when, and how much. */
@Composable
fun ModuleRow(icon: String?, title: String, detail: String?, value: String, modifier: Modifier = Modifier, valueColor: Color = Faint, onClick: (() -> Unit)? = null) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 58.dp).then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let { Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) { StrokeIcon(it, StrokeState.OFF, 22.dp) } }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                detail?.let { SmallLabel(it, maxLines = 1) }
            }
            Text(value, style = MaterialTheme.typography.labelLarge, color = valueColor)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Rule))
    }
}

/** Two or three ranges side by side, the chosen one lifted. */
@Composable
fun RangeSwitch(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.background(Tile, RoundedCornerShape(14.dp)).padding(3.dp)) {
        options.forEach { (key, label) ->
            val on = key == selected
            Box(
                Modifier
                    .defaultMinSize(minHeight = 34.dp)
                    .background(if (on) Color(0xFF2A2A2A) else Color.Transparent, RoundedCornerShape(11.dp))
                    .clickable(role = Role.Tab) { onSelect(key) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) { SmallLabel(label, color = if (on) White else Faint) }
        }
    }
}

data class ChartStack(val value: Float, val color: Color)
data class ChartRow(val color: Color, val name: String, val value: String)

/** One bar: [stacks] rise from the line, [down] hangs under it in red; [rows] are what a tap shows. */
data class ChartColumn(val label: String, val short: String, val headline: String, val stacks: List<ChartStack>, val down: Float, val rows: List<ChartRow>)

private val BarArea = 140.dp
private val TipWidth = 172.dp

/** Stacked bars in grays over a line, red under it. A tap lifts one bar and shows its detail; another tap lets go. */
@Composable
fun StackChart(columns: List<ChartColumn>, modifier: Modifier = Modifier) {
    var selected by remember(columns) { mutableIntStateOf(-1) }
    val haptics = LocalHapticFeedback.current
    val top = columns.maxOfOrNull { c -> c.stacks.sumOf { it.value.toDouble() }.toFloat() }?.coerceAtLeast(1f) ?: 1f
    val bottom = columns.maxOfOrNull { it.down }?.coerceAtLeast(1f) ?: 1f
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(176.dp)) {
            Box(Modifier.fillMaxWidth().offset(y = BarArea + 2.dp).height(1.dp).background(Color(0xFF262626)))
            Row(Modifier.fillMaxSize(), Arrangement.spacedBy(3.dp)) {
                columns.forEachIndexed { i, column ->
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .alpha(if (selected >= 0 && selected != i) 0.4f else 1f)
                            .then(if (selected == i) Modifier.border(1.dp, Color(0xFF5C5C5C), RoundedCornerShape(6.dp)) else Modifier)
                            .clickable(remember { MutableInteractionSource() }, null, role = Role.Button) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selected = if (selected == i) -1 else i
                            }
                            .semantics { contentDescription = "${column.label}: ${column.headline}" }
                            .padding(horizontal = 1.dp),
                    ) {
                        Column(Modifier.fillMaxWidth().height(BarArea).clipToBounds(), verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom)) {
                            column.stacks.asReversed().forEach { stack ->
                                Box(Modifier.fillMaxWidth().height((132.dp * (stack.value / top)).coerceAtLeast(4.dp)).background(stack.color, RoundedCornerShape(3.dp)))
                            }
                        }
                        if (column.down > 0f) Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(6.dp + 20.dp * (column.down / bottom)).background(LauncherRed.copy(alpha = 0.8f), RoundedCornerShape(3.dp)))
                    }
                }
            }
            columns.getOrNull(selected)?.let { picked ->
                val slot = maxWidth / columns.size
                val center = slot * (selected + 0.5f)
                val x = if (selected >= columns.size / 2) center - TipWidth - 12.dp else center + 12.dp
                Column(
                    Modifier.offset(x = x).width(TipWidth).background(Color(0xFF181818), RoundedCornerShape(14.dp)).border(1.dp, Color(0xFF333333), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        SmallLabel(picked.label, maxLines = 1)
                        SmallLabel(picked.headline, color = White, maxLines = 1)
                    }
                    picked.rows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).background(row.color, RoundedCornerShape(3.dp)))
                            SmallLabel(row.name, Modifier.weight(1f), maxLines = 1)
                            SmallLabel(row.value, color = White, maxLines = 1)
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            SmallLabel(columns.firstOrNull()?.short.orEmpty())
            SmallLabel(columns.lastOrNull()?.short.orEmpty())
        }
    }
}

data class DockItem(
    val key: String,
    val label: String,
    val icon: String,
    /** A red count on the tile; empty for a plain red dot. */
    val badge: String? = null,
    /** What a long press offers. */
    val menu: List<Pair<String, () -> Unit>> = emptyList(),
)

/** The modules' navigation, as the exercise scene has it: one bar edge to edge, each place named, where you are in red. */
@Composable
fun ModuleDock(items: List<DockItem>, active: String, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth()
            .drawBehind { drawRect(Color(0xFF1E1E1E), size = Size(size.width, 1.dp.toPx())) }
            .padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item -> DockTile(item, item.key == active) { onOpen(item.key) } }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.DockTile(item: DockItem, active: Boolean, onOpen: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, tween(120), label = "press")
    val tint by animateColorAsState(if (active) LauncherRed else Faint, tween(160), label = "tint")
    val hop = remember { Animatable(0f) }
    var hops by remember { mutableIntStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }
    LaunchedEffect(hops) {
        if (hops == 0) return@LaunchedEffect
        hop.animateTo(-5f, tween(100))
        hop.animateTo(0f, tween(120))
    }
    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .combinedClickable(
                    source, null, role = Role.Tab,
                    onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); hops++; onOpen() },
                    onLongClick = if (item.menu.isEmpty()) null else ({ haptics.performHapticFeedback(HapticFeedbackType.LongPress); menuOpen = true }),
                ),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(28.dp).graphicsLayer { scaleX = scale; scaleY = scale; translationY = hop.value * density.density }, contentAlignment = Alignment.Center) {
                StrokeIcon(item.icon, StrokeState.OFF, 24.dp, color = tint)
                item.badge?.let { badge ->
                    val round = Modifier.align(Alignment.TopEnd)
                    if (badge.isEmpty()) Box(round.offset(2.dp, (-2).dp).size(10.dp).background(LauncherRed, CircleShape))
                    else Box(round.offset(9.dp, (-5).dp).defaultMinSize(18.dp, 18.dp).background(LauncherRed, CircleShape).padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
                        Text(badge, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color.White)
                    }
                }
            }
            Text(item.label, color = tint, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), maxLines = 1)
        }
        if (menuOpen) {
            Popup(
                Alignment.BottomCenter,
                IntOffset(0, with(density) { -70.dp.roundToPx() }),
                onDismissRequest = { menuOpen = false },
                properties = PopupProperties(focusable = true),
            ) {
                Column(Modifier.width(170.dp).background(Color(0xFF181818), RoundedCornerShape(16.dp)).border(1.dp, Color(0xFF333333), RoundedCornerShape(16.dp)).padding(5.dp)) {
                    item.menu.forEach { (label, run) ->
                        Box(
                            Modifier.fillMaxWidth().heightIn(min = 46.dp).clickable(role = Role.Button) { menuOpen = false; run() }.padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) { Text(label, style = MaterialTheme.typography.bodyMedium, color = White) }
                    }
                }
            }
        }
    }
}
