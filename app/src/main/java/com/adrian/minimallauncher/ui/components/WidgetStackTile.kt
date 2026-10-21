package com.adrian.minimallauncher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adrian.minimallauncher.data.WidgetStackConfig
import com.adrian.minimallauncher.feature.CardFill
import com.adrian.minimallauncher.feature.CardLine
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.ui.AlarmTile
import com.adrian.minimallauncher.ui.ClockTile
import com.adrian.minimallauncher.ui.ExpensesTile
import com.adrian.minimallauncher.ui.HomeData
import com.adrian.minimallauncher.ui.MealsTile
import com.adrian.minimallauncher.ui.NoteTile
import com.adrian.minimallauncher.ui.Tile
import com.adrian.minimallauncher.ui.WorldTile
import com.adrian.minimallauncher.feature.exercise.ExerciseWidget
import com.adrian.minimallauncher.feature.vault.VaultWidget
import java.time.LocalDateTime

private val TileShape = RoundedCornerShape(26.dp)

val WIDGET_LABELS = mapOf(
    "clock" to "Reloj",
    "alarms" to "Alarma",
    "exercise" to "Ejercicio",
    "finance" to "Finanzas",
    "diet" to "Dieta",
    "world" to "Reloj Mundial",
    "vault" to "Bancos / Bóveda",
    "notes" to "Nota Fijada",
)

/**
 * A customizable tile or smart stack of tiles on the home screen.
 * Supports swiping through stacked widgets and edit mode controls for resizing/stacking.
 */
@OptIn(ExperimentalFoundationApi::class)
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
) {
    val borderColor by animateColorAsState(
        if (isEditMode) LauncherRed else CardLine,
        label = "tile_border",
    )
    // Shared by the stack and its edit controls, so ✕ removes the widget being shown.
    val pagerState = rememberPagerState(pageCount = { stack.widgetIds.size })
    val scope = rememberCoroutineScope()
    // The grid decides the size (stack.w × stack.h cells); the tile fills it.
    // The clock sits bare on the page, without a card, except while editing.
    val bare = stack.widgetIds == listOf("clock") && !isEditMode
    Box(
        modifier
            .fillMaxSize()
            .then(if (bare) Modifier else Modifier.background(CardFill, TileShape).border(if (isEditMode) 2.dp else 1.dp, borderColor, TileShape)),
    ) {
        // Dim the underlying widget content in edit mode so controls stand out clearly
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (isEditMode) 0.10f else 1f },
        ) {
            if (stack.widgetIds.size > 1) {
                Box(Modifier.fillMaxSize()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        // In edit mode a drag moves the whole stack instead of flipping through it.
                        userScrollEnabled = !isEditMode,
                    ) { page ->
                        val widgetId = stack.widgetIds.getOrNull(page) ?: return@HorizontalPager
                        WidgetContent(widgetId, data, now, onOpenModule, onLongClick)
                    }
                    Row(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        repeat(stack.widgetIds.size) { i ->
                            val active = pagerState.currentPage == i
                            Box(
                                Modifier
                                    .size(if (active) 6.dp else 4.dp)
                                    .background(if (active) LauncherRed else Color(0xFF555555), CircleShape),
                            )
                        }
                    }
                }
            } else {
                val widgetId = stack.widgetIds.firstOrNull() ?: return@Box
                WidgetContent(widgetId, data, now, onOpenModule, onLongClick)
            }
        }

        // Overlay edit mode controls with dark surface and high-contrast chips
        if (isEditMode) {
            val shown = pagerState.currentPage.coerceIn(0, (stack.widgetIds.size - 1).coerceAtLeast(0))
            val activeWidget = stack.widgetIds.getOrNull(shown) ?: ""
            val label = WIDGET_LABELS[activeWidget] ?: activeWidget.uppercase()
            // In a stack the title also says which one is shown ("1/2"); tapping it shows the next.
            val isStack = stack.widgetIds.size > 1

            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0C0C0C).copy(alpha = 0.94f), TileShape)
                    // Taps here never reach the widget underneath, so editing doesn't open the module.
                    .pointerInput(Unit) { detectTapGestures { } }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                // Top row: Title + Span Toggle + Delete
                Row(
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        Modifier
                            .weight(1f)
                            .then(
                                if (!isStack) Modifier else Modifier.clickable(role = Role.Button) {
                                    scope.launch { pagerState.animateScrollToPage((shown + 1) % stack.widgetIds.size) }
                                },
                            )
                            .padding(vertical = 8.dp)
                            .padding(end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // The name gives way to the counter when the tile is narrow.
                        Text(
                            label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF9A9A9A),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 10.sp,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (isStack) {
                            Text(
                                " ${shown + 1}/${stack.widgetIds.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false,
                                fontSize = 10.sp,
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        EditChip(
                            text = "✕",
                            textColor = LauncherRed,
                            onClick = { onRemoveWidget(activeWidget) },
                        )
                    }
                }

                // Bottom row: Stack + button & Move buttons
                Row(
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EditChip(
                        text = "+ PILA",
                        onClick = onAddWidgetToStack,
                    )
                    // Moving and resizing are gestures now: drag the tile, or its corner handle.
                    Text(
                        "${stack.w}×${stack.h}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9A9A9A),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(end = 30.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EditChip(
    text: String,
    textColor: Color = Color.White,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .background(Color(0xFF222222), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF333333), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun WidgetContent(
    widgetId: String,
    data: HomeData,
    now: LocalDateTime,
    onOpenModule: (String) -> Unit,
    onLongClick: () -> Unit,
) {
    val fillMod = Modifier.fillMaxSize()
    val onClick = { onOpenModule(widgetId) }
    when (widgetId) {
        // The clock has no module of its own; tapping it opens alarms, like the phone's clock.
        "clock" -> ClockTile(now, fillMod, { onOpenModule("alarms") }, onLongClick)
        "alarms" -> AlarmTile(now, fillMod, onClick, onLongClick)
        "exercise" -> Tile(fillMod, onClick, onLongClick) { ExerciseWidget(data.exercise, data.settings.exerciseStart) }
        "finance" -> ExpensesTile(data.records, now.toLocalDate(), data.settings.dailyBudgetCents, fillMod, onClick, onLongClick)
        "diet" -> MealsTile(data.records.completedHabits, now, data.settings.mealTimes, fillMod, onClick, onLongClick)
        "world" -> WorldTile(data.settings.worldZones, fillMod, onClick, onLongClick)
        "vault" -> Tile(fillMod, onClick, onLongClick) { VaultWidget(data.vaultAppCount) }
        "notes" -> NoteTile(data.records.notes, onClick, onLongClick)
        else -> {
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
