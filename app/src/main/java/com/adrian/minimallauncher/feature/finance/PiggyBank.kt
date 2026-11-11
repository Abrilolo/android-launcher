package com.adrian.minimallauncher.feature.finance

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp

/** Behind the time (time). */
internal val SavingAmber = MoneyLow
/** Goal reached (done). */
internal val SavingGreen = FinanceSage
private val Outline = Color(0xFFF5F5F5)

/** Blue on pace, lavender behind schedule, and sage once the goal is reached. */
internal fun goalColor(goal: SavingGoal, today: java.time.LocalDate): Color = when {
    goal.reached -> SavingGreen
    goal.behind(today) -> MoneyLow
    else -> MoneyGoing
}

/**
 * A round piggy bank turned three quarters to the left, snout in front and a coin going into the slot, in our white
 * line. The body fills from the bottom with [color] up to [fraction]; the fill rises once when it appears.
 */
@Composable
internal fun PiggyBank(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    val level = remember { Animatable(0f) }
    LaunchedEffect(fraction) { level.animateTo(fraction.coerceIn(0f, 1f), tween(900)) }
    val bg = MaterialTheme.colorScheme.background
    Canvas(modifier) {
        // Drawn on a 120 × 100 grid, centered and scaled to fit.
        val u = minOf(size.width / 120f, size.height / 100f)
        val ox = (size.width - 120f * u) / 2f
        val oy = (size.height - 100f * u) / 2f
        fun p(x: Float, y: Float) = Offset(ox + x * u, oy + y * u)
        fun oval(cx: Float, cy: Float, rx: Float, ry: Float) = Rect(p(cx - rx, cy - ry), p(cx + rx, cy + ry))
        val line = 2.4.dp.toPx()
        val stroke = Stroke(line, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun path(vararg points: Pair<Float, Float>) = Path().apply {
            points.forEachIndexed { i, (x, y) -> val o = p(x, y); if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }
            close()
        }
        fun shape(r: Rect) { drawOval(bg, r.topLeft, r.size); drawOval(Outline, r.topLeft, r.size, style = stroke) }

        val body = oval(66f, 56f, 44f, 34f)
        val head = oval(36f, 52f, 26f, 26f)
        val snout = oval(16f, 60f, 11f, 9.5f)

        // Behind the body: the coin half in the slot, the four legs, the tail and the ears.
        shape(oval(74f, 18f, 13f, 13f))
        drawLine(Outline, p(69f, 13f), p(69f, 23f), 1.6.dp.toPx(), StrokeCap.Round)
        listOf(oval(30f, 84f, 7f, 7f), oval(52f, 89f, 7.5f, 7f), oval(86f, 88f, 7f, 7f), oval(103f, 78f, 6f, 6.5f)).forEach(::shape)
        drawPath(
            Path().apply {
                val a = p(109f, 52f); moveTo(a.x, a.y)
                val c1 = p(117f, 51f); val c2 = p(118f, 42f); val b = p(112f, 41f); cubicTo(c1.x, c1.y, c2.x, c2.y, b.x, b.y)
                val c3 = p(108f, 41f); val c4 = p(108f, 46f); val e = p(112f, 46f); cubicTo(c3.x, c3.y, c4.x, c4.y, e.x, e.y)
            },
            Outline, style = stroke,
        )
        // The body and head as one outline: a double line each, then their insides cover the inner half.
        drawOval(Outline, body.topLeft, body.size, style = Stroke(line * 2))
        drawOval(Outline, head.topLeft, head.size, style = Stroke(line * 2))
        listOf(path(21f to 33f, 15f to 14f, 35f to 25f), path(47f to 29f, 57f to 14f, 62f to 33f)).forEach { ear ->
            drawPath(ear, bg); drawPath(ear, Outline, style = stroke)
        }
        listOf(body, head, snout).forEach { drawOval(bg, it.topLeft, it.size) }

        // What is saved, rising from the bottom of the body, head and snout.
        val inside = Path().apply { addOval(body); addOval(head); addOval(snout) }
        clipPath(inside) {
            val bottom = body.bottom
            val top = bottom - (bottom - head.top) * level.value
            drawRect(color, Offset(0f, top), Size(size.width, bottom - top))
        }

        // In front: the slot, a shine on the back, the eyes and the snout with its nostrils.
        drawLine(Outline, p(66f, 24.5f), p(82f, 24.5f), line, StrokeCap.Round)
        drawPath(
            Path().apply { val a = p(88f, 31f); moveTo(a.x, a.y); val c = p(97f, 34f); val b = p(101f, 41f); quadraticTo(c.x, c.y, b.x, b.y) },
            Outline, style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round),
        )
        drawCircle(Outline, 2.6f * u, p(25f, 43f))
        drawCircle(Outline, 2.6f * u, p(45f, 45f))
        drawOval(Outline, snout.topLeft, snout.size, style = stroke)
        listOf(12f, 20f).forEach { x -> val r = oval(x, 60f, 1.7f, 2.8f); drawOval(Outline, r.topLeft, r.size) }
    }
}
