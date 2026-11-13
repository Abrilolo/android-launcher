package com.adrian.minimallauncher.feature.food

import androidx.compose.ui.unit.sp
import com.adrian.minimallauncher.feature.finance.ActionColor
import com.adrian.minimallauncher.feature.finance.FinIcon
import com.adrian.minimallauncher.feature.finance.FinanceRangeSwitch
import com.adrian.minimallauncher.feature.finance.FinanceWhite
import com.adrian.minimallauncher.feature.finance.InfoIcon
import com.adrian.minimallauncher.feature.finance.LabelCream
import com.adrian.minimallauncher.feature.finance.MoneyLow
import com.adrian.minimallauncher.feature.finance.MoneyPlenty
import com.adrian.minimallauncher.feature.finance.SectionHead
import com.adrian.minimallauncher.feature.finance.SectionLabel
import com.adrian.minimallauncher.feature.finance.glass
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.adrian.minimallauncher.feature.ChartColumn
import com.adrian.minimallauncher.feature.ChartRow
import com.adrian.minimallauncher.feature.ChartShades
import com.adrian.minimallauncher.feature.ChartStack
import com.adrian.minimallauncher.feature.LauncherAccent
import com.adrian.minimallauncher.feature.LauncherDanger
import com.adrian.minimallauncher.feature.ModuleRow
import com.adrian.minimallauncher.feature.RangeSwitch
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.feature.StackChart
import com.adrian.minimallauncher.feature.shortDay
import com.adrian.minimallauncher.ui.components.DotMatrixText
import com.adrian.minimallauncher.ui.components.LocalReactions
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val White = Color(0xFFF5F5F5)
private val Gray = Color(0xFF5C5C5C)
private val Muted = Color(0xFFB0B0B0)
private val Track = Color(0xFF2A2A2A)

/** How many weight records the line shows. */
private const val WEIGHT_POINTS = 10
private const val RECENT_MEALS = 12
private val hhmm = DateTimeFormatter.ofPattern("HH:mm")

/**
 * The food week: kcal by day or week with what passed the goal in red, the protein and fiber days, weight against
 * its goal, the BMI (IMC) on a scale of its ranges and the latest meals.
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

    var range by rememberSaveable { mutableStateOf("dia") }
    val columns = remember(meals, range, today, settings.kcalGoal) { kcalColumns(meals, range, today, settings.kcalGoal.toFloat()) }
    val fedDays = meals.map { Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate() }.filter { !it.isBefore(today.minusDays(19)) }.distinct().size
    val average = if (fedDays > 0) meals.filter { !Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate().isBefore(today.minusDays(19)) }.sumOf { it.kcal.toDouble() }.toFloat() / fedDays else 0f
    val monday = today.with(DayOfWeek.MONDAY)
    val days = (0..6).map { monday.plusDays(it.toLong()) }
    val byDay = meals.groupBy { Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate() }

    // 1. Kcal al día, big, and the chart of days or weeks in its card.
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Promedio al día")
            InfoIcon("Lo que comes en promedio en los últimos 20 días con registros. En la gráfica, cada columna se parte en proteína, carbohidratos y grasa; lo que pasa tu meta cuelga en rosa. Toca una columna para ver su detalle.")
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("%,d".format(average.toInt()), style = MaterialTheme.typography.headlineLarge.copy(fontSize = 54.sp, lineHeight = 60.sp), color = FinanceWhite)
            Text("kcal", Modifier.padding(bottom = 10.dp), style = MaterialTheme.typography.titleMedium, color = LabelCream)
        }
    }
    Column(Modifier.fillMaxWidth().glass().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        FinanceRangeSwitch(listOf("dia" to "Por día", "semana" to "Por semana"), range) { range = it }
        StackChart(columns, downColor = LauncherDanger)
    }

    // 2. The week's goals, as one dot per day.
    if (settings.proteinGoalG > 0 || settings.fiberGoalG > 0) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHead("Metas de la semana", "Un punto por día: verde si llegaste a la meta, azul hoy mientras no llegas.")
        Column(Modifier.fillMaxWidth().glass().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (settings.proteinGoalG > 0) GoalDots("Proteína", days, today) { d -> byDay[d].orEmpty().sumOf { it.proteinG.toDouble() } >= settings.proteinGoalG }
            if (settings.fiberGoalG > 0) GoalDots("Fibra", days, today) { d -> byDay[d].orEmpty().sumOf { it.fiberG.toDouble() } >= settings.fiberGoalG }
        }
    }

    // 3. Peso, with its line.
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Peso")
            InfoIcon("Tus últimos pesos. La línea punteada es tu meta. Es el mismo peso que anotas en Ejercicio.")
            Box(Modifier.weight(1f))
            FinIcon("nueva", "Anotar peso", ActionColor.Add) { adding = !adding }
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(now?.oneDecimal() ?: "—", style = MaterialTheme.typography.headlineMedium, color = FinanceWhite)
            Text("kg", Modifier.padding(bottom = 4.dp), style = MaterialTheme.typography.bodyMedium, color = LabelCream)
            Box(Modifier.weight(1f))
            if (now != null && goal != null) {
                val gap = Math.abs(now - goal)
                Text(if (gap < 0.05f) "En tu meta" else "Faltan ${gap.oneDecimal()} kg", style = MaterialTheme.typography.bodyMedium, color = if (gap < 0.05f) MoneyPlenty else FinanceWhite)
            }
        }
        if (adding) {
            Row(Modifier.fillMaxWidth().glass().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                NumberField(input, { input = it }, "Peso en kg", 120.dp)
                Box(Modifier.weight(1f))
                FinIcon("listo", "Guardar peso", ActionColor.Add) {
                    input.toAmount()?.takeIf { it in 30f..250f }?.let { onAddWeight(it); reactions.tick(); input = ""; adding = false }
                }
                FinIcon("no", "Cancelar") { adding = false }
            }
        }
        if (weights.size >= 2) Column(Modifier.fillMaxWidth().glass().padding(16.dp)) { WeightLine(weights, goal, grow.value) }
    }

    // 4. IMC
    val bmi = FoodMath.bmi(now, settings.heightCm)
    val goalBmi = FoodMath.bmi(goal, settings.heightCm)
    if (bmi != null) {
        val healthy = bmi >= 18.5f && bmi < 25f
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHead("IMC", "Tu peso entre tu estatura al cuadrado. La franja clara es el rango saludable (18.5 a 25); la marca delgada es el IMC de tu meta.")
            Column(Modifier.fillMaxWidth().glass().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(bmi.oneDecimal(), style = MaterialTheme.typography.headlineMedium, color = FinanceWhite)
                    Text(if (healthy) "Saludable" else if (bmi < 18.5f) "Bajo" else "Alto", Modifier.padding(bottom = 4.dp), style = MaterialTheme.typography.titleMedium, color = if (healthy) MoneyPlenty else MoneyLow)
                }
                BmiScale(bmi, goalBmi, healthy)
            }
        }
    }

    // 5. Registros
    val recent = meals.sortedByDescending { it.createdAt }.take(RECENT_MEALS)
    if (recent.isNotEmpty()) Column {
        SectionLabel("Registros")
        recent.forEach { meal ->
            val at = Instant.ofEpochMilli(meal.createdAt).atZone(zone)
            ModuleRow(null, meal.description, "${shortDay(at.toLocalDate())} · ${at.toLocalTime().format(hhmm)}", "${meal.kcal.whole()} kcal", valueColor = FinanceWhite)
        }
    }
}

/** Each day or week: its kcal per day, split in the kcal of each macro; what passed the goal hangs in red. */
private fun kcalColumns(meals: List<MealRecord>, range: String, today: LocalDate, goal: Float): List<ChartColumn> {
    val zone = ZoneId.systemDefault()
    val weeks = range == "semana"
    val last = if (weeks) today.with(DayOfWeek.MONDAY) else today
    val keys = if (weeks) (11L downTo 0L).map { last.minusWeeks(it) } else (19L downTo 0L).map { last.minusDays(it) }
    val dated = meals.map { it to Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate() }
    return keys.map { key ->
        val rows = dated.filter { (_, d) -> (if (weeks) d.with(DayOfWeek.MONDAY) else d) == key }
        val count = rows.map { it.second }.distinct().size.coerceAtLeast(1)
        fun total(part: (MealRecord) -> Float) = rows.sumOf { part(it.first).toDouble() }.toFloat() / count
        val kcal = total { it.kcal }
        val protein = total { it.proteinG }
        val carbs = total { it.carbsG }
        val fat = total { it.fatG }
        val parts = listOf(protein * 4, carbs * 4, fat * 9, (kcal - protein * 4 - carbs * 4 - fat * 9).coerceAtLeast(0f))
        val over = if (goal > 0f) (kcal - goal).coerceAtLeast(0f) else 0f
        ChartColumn(
            if (weeks) "Semana ${shortDay(key)}" else shortDay(key), shortDay(key), "${kcal.whole()} kcal",
            parts.mapIndexedNotNull { i, value -> ChartStack(value, ChartShades[i * 2]).takeIf { value > 0.5f } }, over,
            buildList {
                add(ChartRow(ChartShades[0], "Proteína", "${protein.whole()} g"))
                add(ChartRow(ChartShades[2], "Carbohidratos", "${carbs.whole()} g"))
                add(ChartRow(ChartShades[4], "Grasa", "${fat.whole()} g"))
                if (over > 0f) add(ChartRow(LauncherDanger, "De más", "+${over.whole()}"))
            },
        )
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
        if (progress >= 1f) drawCircle(LauncherAccent, 3.5.dp.toPx(), Offset(x(weights.lastIndex), y(weights.last())))
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
        drawLine(if (healthy) White else LauncherDanger, Offset(x(bmi), 3.dp.toPx()), Offset(x(bmi), size.height - 3.dp.toPx()), 2.5.dp.toPx(), StrokeCap.Round)
    }
}

/** One dot per day: green when that day's goal was met, blue for today while it is not, faint otherwise. */
@Composable
private fun GoalDots(label: String, days: List<LocalDate>, today: LocalDate, met: (LocalDate) -> Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(76.dp), style = MaterialTheme.typography.labelLarge, color = LabelCream, maxLines = 1)
        days.forEach { d ->
            val ok = !d.isAfter(today) && met(d)
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(Modifier.size(10.dp).background(if (ok) MoneyPlenty else if (d == today) LauncherAccent else Track, CircleShape))
            }
        }
    }
}
