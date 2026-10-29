package com.adrian.minimallauncher.feature.food

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.BodyMetricRecord
import com.adrian.minimallauncher.data.MealRecord
import com.adrian.minimallauncher.data.ModuleSettings
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.ui.components.DotMatrixText
import com.adrian.minimallauncher.ui.components.LocalReactions
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val White = Color(0xFFF5F5F5)
private val Gray = Color(0xFF5C5C5C)
private val Muted = Color(0xFFB0B0B0)
private val Track = Color(0xFF2A2A2A)

/** How many weight records the line shows. */
private const val WEIGHT_POINTS = 10

/**
 * The food week (ComidaVisual.html, Semana): weight against its goal with the recent line, the BMI (IMC) on a scale
 * of its ranges, and each day's kcal against the goal with the protein and fiber days below. Lines and bars draw once
 * when it opens.
 */
@Composable
fun FoodWeek(today: LocalDate, meals: List<MealRecord>, metrics: List<BodyMetricRecord>, settings: ModuleSettings, onAddWeight: (Float) -> Unit) {
    val reactions = LocalReactions.current
    val zone = ZoneId.systemDefault()
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }

    val weights = metrics.filter { it.type == "WEIGHT" }.sortedBy { it.createdAt }.takeLast(WEIGHT_POINTS).map { it.value }
    val now = weights.lastOrNull()
    val goal = settings.weightGoalKg.takeIf { it > 0f }
    var adding by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                    StrokeIcon("bascula", if (now != null) StrokeState.DONE else StrokeState.OFF, 26.dp)
                    DotMatrixText(now?.oneDecimal() ?: "--.-", cell = 4.dp)
                    if (goal != null) SmallLabel(goal.oneDecimal())
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (now != null && goal != null && Math.abs(now - goal) >= 0.05f) {
                        SmallLabel((if (now > goal) "−" else "+") + Math.abs(now - goal).oneDecimal(), color = LauncherRed)
                    }
                    IconButton("Anotar peso", { adding = !adding }) { StrokeIcon("nueva", StrokeState.OFF, 22.dp) }
                }
            }
            if (adding) {
                Row(Modifier.fillMaxWidth(), Arrangement.End, Alignment.CenterVertically) {
                    NumberField(input, { input = it }, "Peso en kg", 72.dp)
                    SmallLabel("KG")
                    IconButton("Guardar peso", {
                        input.toAmount()?.takeIf { it in 30f..250f }?.let { onAddWeight(it); reactions.tick(); input = ""; adding = false }
                    }) { StrokeIcon("listo", StrokeState.DONE, 22.dp, color = LauncherRed) }
                }
            }
            if (weights.size >= 2) WeightLine(weights, goal, grow.value)
        }
        Separator()

        val bmi = FoodMath.bmi(now, settings.heightCm)
        val goalBmi = FoodMath.bmi(goal, settings.heightCm)
        if (bmi != null) {
            val healthy = bmi >= 18.5f && bmi < 25f
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text(bmi.oneDecimal(), style = MaterialTheme.typography.headlineSmall, color = if (healthy) White else LauncherRed)
                    if (goalBmi != null) SmallLabel(goalBmi.oneDecimal())
                }
                BmiScale(bmi, goalBmi, healthy)
            }
            Separator()
        }

        val monday = today.with(DayOfWeek.MONDAY)
        val days = (0..6).map { monday.plusDays(it.toLong()) }
        val byDay = meals.groupBy { Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate() }
        val kcal = days.map { d -> byDay[d].orEmpty().sumOf { it.kcal.toDouble() }.toFloat() }
        KcalBars(days, today, kcal, settings.kcalGoal.toFloat(), grow.value)
        if (settings.proteinGoalG > 0) GoalDots("huevo", days, today) { d -> byDay[d].orEmpty().sumOf { it.proteinG.toDouble() } >= settings.proteinGoalG }
        if (settings.fiberGoalG > 0) GoalDots("hoja", days, today) { d -> byDay[d].orEmpty().sumOf { it.fiberG.toDouble() } >= settings.fiberGoalG }
    }
}

@Composable
private fun Separator() = Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF222222)))

/** The last weights as one line that draws itself once; the goal is a dashed gray line, the latest point red. */
@Composable
private fun WeightLine(weights: List<Float>, goal: Float?, progress: Float) {
    val all = weights + listOfNotNull(goal)
    val min = all.min() - 0.5f
    val max = all.max() + 0.5f
    Canvas(Modifier.fillMaxWidth().height(120.dp).semantics { contentDescription = "Peso reciente" }) {
        val pad = 6.dp.toPx()
        fun x(i: Int) = pad + i * (size.width - 2 * pad) / (weights.size - 1)
        fun y(v: Float) = size.height - pad - (v - min) / (max - min) * (size.height - 2 * pad)
        if (goal != null) drawLine(Gray, Offset(0f, y(goal)), Offset(size.width, y(goal)), 1.5.dp.toPx(), StrokeCap.Round, PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 5.dp.toPx())))
        val path = Path().apply { weights.forEachIndexed { i, v -> if (i == 0) moveTo(x(i), y(v)) else lineTo(x(i), y(v)) } }
        val measure = PathMeasure().apply { setPath(path, false) }
        val part = Path().also { measure.getSegment(0f, measure.length * progress, it, true) }
        drawPath(part, White, style = Stroke(1.75.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        if (progress >= 1f) drawCircle(LauncherRed, 3.5.dp.toPx(), Offset(x(weights.lastIndex), y(weights.last())))
    }
}

/** BMI from 16 to 35 with its ranges; the healthy one (18.5–25) is lighter. The mark is white inside it, red outside. */
@Composable
private fun BmiScale(bmi: Float, goal: Float?, healthy: Boolean) {
    Canvas(Modifier.fillMaxWidth().height(30.dp).semantics { contentDescription = "IMC ${bmi.oneDecimal()}" }) {
        fun x(v: Float) = (v.coerceIn(16f, 35f) - 16f) / 19f * size.width
        val mid = size.height / 2
        val w = 3.dp.toPx()
        val gap = 3.dp.toPx()
        listOf(16f to 18.5f, 18.5f to 25f, 25f to 30f, 30f to 35f).forEach { (a, b) ->
            drawLine(if (a == 18.5f) Gray else Track, Offset(x(a) + gap, mid), Offset(x(b) - gap, mid), w, StrokeCap.Round)
        }
        if (goal != null) drawLine(Muted, Offset(x(goal), mid - 7.dp.toPx()), Offset(x(goal), mid + 7.dp.toPx()), 1.5.dp.toPx(), StrokeCap.Round)
        drawLine(if (healthy) White else LauncherRed, Offset(x(bmi), 3.dp.toPx()), Offset(x(bmi), size.height - 3.dp.toPx()), 2.5.dp.toPx(), StrokeCap.Round)
    }
}

/** Monday to Sunday: white up to the goal, red past it; days to come are empty outlines and today has a red ring. */
@Composable
private fun KcalBars(days: List<LocalDate>, today: LocalDate, kcal: List<Float>, goal: Float, progress: Float) {
    val top = maxOf(goal, kcal.maxOrNull() ?: 0f, 1f) * 1.1f
    Canvas(Modifier.fillMaxWidth().height(110.dp).semantics { contentDescription = "kcal de la semana" }) {
        val slot = size.width / 7
        val bar = 10.dp.toPx()
        val radius = CornerRadius(bar / 2)
        fun h(v: Float) = v / top * size.height
        if (goal > 0f) drawLine(Color(0xFF3A3A3A), Offset(0f, size.height - h(goal)), Offset(size.width, size.height - h(goal)), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())))
        days.forEachIndexed { i, d ->
            val left = slot * i + (slot - bar) / 2
            val k = kcal[i]
            when {
                d.isAfter(today) || k <= 0f -> {
                    val hh = h(if (goal > 0f) goal else top / 2)
                    drawRoundRect(Track, Offset(left, size.height - hh), Size(bar, hh), radius, style = Stroke(1.5.dp.toPx()))
                }
                else -> {
                    val base = if (goal > 0f) minOf(k, goal) else k
                    val over = if (goal > 0f) (k - goal).coerceAtLeast(0f) else 0f
                    val hb = h(base) * progress
                    drawRoundRect(White, Offset(left, size.height - hb), Size(bar, hb), radius)
                    if (over > 0f) {
                        val ho = h(over) * progress
                        drawRoundRect(LauncherRed, Offset(left, size.height - hb - ho - 3.dp.toPx()), Size(bar, ho), radius)
                    }
                }
            }
            if (d == today) {
                val r = 2.dp.toPx()
                drawCircle(LauncherRed, r, Offset(left + bar / 2, size.height + 0f - r))
            }
        }
    }
}

/** One dot per day: white when that day's goal was met, red for today while it is not, gray otherwise. */
@Composable
private fun GoalDots(icon: String, days: List<LocalDate>, today: LocalDate, met: (LocalDate) -> Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) { StrokeIcon(icon, StrokeState.OFF, 16.dp) }
        days.forEach { d ->
            val ok = !d.isAfter(today) && met(d)
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(Modifier.size(6.dp).background(if (ok) White else if (d == today) LauncherRed else Track, CircleShape))
            }
        }
    }
}
