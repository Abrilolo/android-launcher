package com.adrian.minimallauncher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.feature.LauncherAccent
import com.adrian.minimallauncher.feature.LauncherDanger
import com.adrian.minimallauncher.ui.theme.LauncherGray
import com.adrian.minimallauncher.ui.theme.LauncherWhite
import kotlin.math.atan2
import kotlin.math.roundToInt

/** One slice, column or bar: a label, its value and the color it is drawn in. */
data class ChartPart(val label: String, val value: Float, val color: Color)

/** Una línea de un recuadro de datos: etiqueta en gris y cifra en su color. */
data class InsightLine(val label: String, val value: String, val color: Color)

/** Ancho fijo del recuadro de datos, para colocarlo justo donde se tocó. */
val InsightBubbleWidth = 188.dp

/**
 * Recuadro de datos que aparece encima de la gráfica, en el punto tocado (diseño 2026-11-10).
 * No reserva espacio: flota sobre la gráfica y se va al volver a tocar.
 */
@Composable
fun InsightBubble(title: String, lines: List<InsightLine>, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .width(InsightBubbleWidth)
            .shadow(12.dp, shape)
            .background(Color(0xFF15222F), shape)
            .border(1.dp, LauncherAccent.copy(alpha = 0.55f), shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = LauncherWhite, maxLines = 1)
        lines.forEach { line ->
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(line.label, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = LauncherGray, maxLines = 1)
                Text(line.value, style = MaterialTheme.typography.labelLarge, color = line.color, maxLines = 1)
            }
        }
    }
}

/**
 * Anillo con partes de colores; el centro dice el total. Con [onSelect], tocar una parte la elige
 * (devuelve su índice y el punto tocado, para poner ahí el recuadro de datos).
 */
@Composable
fun DonutChart(
    parts: List<ChartPart>,
    center: String,
    modifier: Modifier = Modifier,
    diameter: Dp = 180.dp,
    thickness: Dp = 22.dp,
    onSelect: ((Int, Offset) -> Unit)? = null,
) {
    val total = parts.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1f)
    Box(
        modifier
            .size(diameter)
            .let { m ->
                if (onSelect == null) m else m.pointerInput(parts) {
                    detectTapGestures { pos ->
                        val c = Offset(size.width / 2f, size.height / 2f)
                        val deg = Math.toDegrees(atan2((pos.y - c.y).toDouble(), (pos.x - c.x).toDouble())).toFloat()
                        val angle = (deg + 90f + 360f) % 360f
                        var acc = 0f
                        val hit = parts.indexOfFirst { p -> acc += 360f * p.value / total; angle < acc }
                        if (hit >= 0) onSelect(hit, pos)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = thickness.toPx()
            val box = Size(this.size.width - w, this.size.height - w)
            var start = -90f
            parts.forEach { p ->
                val sweep = 360f * p.value / total
                drawArc(p.color, start, sweep, useCenter = false, topLeft = Offset(w / 2f, w / 2f), size = box, style = Stroke(width = w))
                start += sweep
            }
        }
        Text(center, style = MaterialTheme.typography.headlineMedium, color = LauncherWhite)
    }
}

/** Columnas verticales: la elegida en blanco y las demás se apagan; rosa = se pasó. */
@Composable
fun ColumnChart(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    over: Set<Int> = emptySet(),
    height: Dp = 140.dp,
    selected: Int = -1,
    onSelect: ((Int) -> Unit)? = null,
) {
    val top = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    Row(modifier.fillMaxWidth().height(height + 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
        values.forEachIndexed { i, v ->
            val base = if (i in over) LauncherDanger else LauncherAccent
            Column(
                Modifier.weight(1f).fillMaxHeight().let { m -> if (onSelect != null) m.clickable(role = Role.Button) { onSelect(i) } else m },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(
                    Modifier.fillMaxWidth().height((height * (v / top)).coerceAtLeast(4.dp))
                        .background(
                            when {
                                v <= 0f -> Color.White.copy(alpha = 0.08f)
                                selected == i -> if (i in over) LauncherDanger else LauncherWhite
                                selected >= 0 -> base.copy(alpha = 0.3f)
                                else -> base
                            },
                            RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                        ),
                )
                Text(labels.getOrElse(i) { "" }, style = MaterialTheme.typography.labelSmall, color = if (selected == i) LauncherWhite else LauncherGray)
            }
        }
    }
}

/**
 * Barras horizontales: nombre y monto arriba (el monto en el color de su barra) y la barra debajo.
 * Con [onSelect], cada fila se toca; las demás se apagan mientras una está elegida.
 */
@Composable
fun HorizontalBarChart(
    parts: List<ChartPart>,
    amountOf: (ChartPart) -> String,
    modifier: Modifier = Modifier,
    selected: Int = -1,
    onSelect: ((Int) -> Unit)? = null,
) {
    val top = parts.maxOfOrNull { it.value }?.coerceAtLeast(1f) ?: 1f
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        parts.forEachIndexed { i, p ->
            val lit = selected == -1 || selected == i
            Column(
                Modifier.fillMaxWidth()
                    .height(BarRowHeight)
                    .let { m -> if (onSelect != null) m.clickable(role = Role.Button) { onSelect(i) } else m },
                verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
            ) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text(p.label, style = MaterialTheme.typography.titleSmall, color = if (lit) LauncherWhite else LauncherGray, maxLines = 1)
                    Text(amountOf(p), style = MaterialTheme.typography.titleSmall, color = if (lit) p.color else LauncherGray)
                }
                Box(Modifier.fillMaxWidth().height(10.dp).background(Color(0xFF16283A), RoundedCornerShape(5.dp))) {
                    Box(Modifier.fillMaxWidth(p.value / top).fillMaxHeight().background(if (lit) p.color else p.color.copy(alpha = 0.25f), RoundedCornerShape(5.dp)))
                }
            }
        }
    }
}

/** Alto de cada fila de [HorizontalBarChart] (más su separación de 10 dp), para poner el recuadro a su altura. */
val BarRowHeight = 40.dp

/** Línea continua y suave con área tenue. Tocar elige el día más cercano: guía vertical y punto marcado. */
@Composable
fun SmoothLineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    selected: Int = -1,
    onSelect: ((Int) -> Unit)? = null,
) {
    if (values.size < 2) return
    val top = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    val n = values.size
    Canvas(
        modifier.fillMaxWidth().height(height).let { m ->
            if (onSelect == null) m else m.pointerInput(n) {
                detectTapGestures { pos -> onSelect((pos.x / size.width * (n - 1)).roundToInt().coerceIn(0, n - 1)) }
            }
        },
    ) {
        val pts = values.mapIndexed { i, v -> Offset(this.size.width * i / (n - 1), this.size.height * (1f - v / top)) }
        val line = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until n - 1) {
                val mid = Offset((pts[i].x + pts[i + 1].x) / 2f, (pts[i].y + pts[i + 1].y) / 2f)
                quadraticBezierTo(pts[i].x, pts[i].y, mid.x, mid.y)
            }
            lineTo(pts.last().x, pts.last().y)
        }
        val area = Path().apply {
            addPath(line)
            lineTo(this@Canvas.size.width, this@Canvas.size.height)
            lineTo(0f, this@Canvas.size.height)
            close()
        }
        drawPath(area, LauncherAccent.copy(alpha = 0.18f))
        drawPath(line, LauncherAccent, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        if (selected in pts.indices) {
            val p = pts[selected]
            drawLine(LauncherWhite.copy(alpha = 0.45f), Offset(p.x, 0f), Offset(p.x, this.size.height), 1.dp.toPx())
            drawCircle(LauncherAccent, 7.dp.toPx(), p)
            drawCircle(LauncherWhite, 7.dp.toPx(), p, style = Stroke(2.dp.toPx()))
        } else {
            drawCircle(LauncherWhite, 4.dp.toPx(), pts.last())
        }
    }
}
