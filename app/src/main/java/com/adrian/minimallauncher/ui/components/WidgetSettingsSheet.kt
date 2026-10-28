package com.adrian.minimallauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.adrian.minimallauncher.data.GRID_COLUMNS
import com.adrian.minimallauncher.data.WidgetStackConfig
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.ui.GRID_CELL_HEIGHT
import com.adrian.minimallauncher.ui.GRID_GAP
import com.adrian.minimallauncher.ui.HomeData
import java.time.LocalDateTime
import kotlin.math.roundToInt

private val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

/**
 * A widget's settings, opened from the gear in edit mode. TRANSPARENCIA goes from 0% (no card, only the
 * content over the page) to 100% (solid card); the preview follows the slider and GUARDAR keeps it.
 */
@Composable
fun WidgetSettingsSheet(
    stack: WidgetStackConfig,
    data: HomeData,
    now: LocalDateTime,
    onSave: (Float) -> Unit,
    onDismiss: () -> Unit,
    /** Opens the widget's own settings (Asistentes IA: which apps, their order, names). */
    onMore: (() -> Unit)? = null,
) {
    var alpha by remember(stack.id) { mutableFloatStateOf(stack.bgAlpha) }
    val first = stack.widgetIds.firstOrNull().orEmpty()
    val title = if (stack.widgetIds.size > 1) "PILA · ${stack.widgetIds.size}" else (WIDGET_LABELS[first] ?: first).uppercase()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { onDismiss() } },
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111111), SheetShape)
                    .border(1.dp, Color(0xFF222222), SheetShape)
                    // Taps inside the sheet must not close it.
                    .pointerInput(Unit) { detectTapGestures { } }
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SmallLabel(title, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)

                // Preview at the widget's own proportions, over the page's black.
                val rows = stack.h.coerceIn(1, 3)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(Color.Black, RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(stack.w.coerceIn(1, GRID_COLUMNS) / GRID_COLUMNS.toFloat())
                            .height(GRID_CELL_HEIGHT * rows + GRID_GAP * (rows - 1))
                            .widgetCard(alpha),
                    ) {
                        WidgetContent(first, data, now, {}, {}, padded = alpha > 0f || stack.widgetIds.size > 1)
                    }
                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    SmallLabel("TRANSPARENCIA", Modifier.weight(1f))
                    SmallLabel("${(alpha * 100).roundToInt()}%", color = if (alpha == 0f) LauncherRed else MaterialTheme.colorScheme.onSurface)
                }
                Slider(
                    value = alpha,
                    onValueChange = { alpha = (it * 20).roundToInt() / 20f },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color(0xFF3A3A3A),
                    ),
                )

                if (onMore != null) {
                    Row(
                        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onMore).padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SmallLabel("APPS Y ORDEN", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                        SmallLabel("›", color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SheetButton("CANCELAR", filled = false, onClick = onDismiss, modifier = Modifier.weight(1f))
                    SheetButton("GUARDAR", filled = true, onClick = { onSave(alpha) }, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun SheetButton(label: String, filled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier
            .background(if (filled) Color.White else Color.Transparent, shape)
            .border(1.dp, if (filled) Color.White else Color(0xFF444444), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (filled) Color.Black else Color.White)
    }
}
