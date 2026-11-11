package com.adrian.minimallauncher.feature.finance

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adrian.minimallauncher.data.FinanceRecord
import com.adrian.minimallauncher.feature.LauncherAccent
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt
import kotlinx.coroutines.launch
import java.time.LocalDate

private val White = Color(0xFFF5F5F5)
private val Track = inactiveFinanceColor(FinanceBlue)
private val Muted = FinanceMuted

/** How dim a slice looks while nothing went out of it. */
private const val OFF_ALPHA = 0.42f

/** A big amount in the regular typeface, as tall as [height], in [color] (the money's state where it has one). */
@Composable
internal fun BigAmount(cents: Long, height: Dp, color: Color = White) {
    FinanceAmount(cents, size = (height.value * 1.3f).toInt(), color = color)
}

/** Match data roles and category hues across the daily and period charts. */
internal fun sectionColors(sections: List<BudgetSection>): List<Color> =
    sections.map { section ->
        when (section.kind) {
            SectionKind.FREE -> FinanceTeal
            SectionKind.TOPIC -> financeCategoryColor(section.key)
            SectionKind.FIXED -> FinanceBlue
            SectionKind.MONTHLY -> FinanceMist
            SectionKind.SAVING -> SavingGreen
        }
    }

internal fun sectionIcon(section: BudgetSection): String = when (section.kind) {
    SectionKind.FREE -> "extra"
    SectionKind.SAVING -> "ahorro"
    SectionKind.FIXED -> "fijos"
    SectionKind.MONTHLY -> "mes"
    SectionKind.TOPIC -> categoryIcon(section.key)
}

/** The pages after the pie: every section that can be spent from (the saving only shows in the pie). */
internal fun ringSections(sections: List<BudgetSection>) = sections.filter { it.kind != SectionKind.SAVING }

/** What the charts look at: the span around [day], all movements and today, for the insights. */
internal data class ChartScope(val span: Span, val day: LocalDate, val today: LocalDate, val money: List<FinanceRecord>, val topicNames: Set<String>)

/**
 * The budget charts, swiped sideways: first the pie, each section with its share of the budget, dim until money
 * goes out of it and lighting up as it does; then one ring per section. Tapping a segment shows what it means
 * (insights); tapping a row jumps to its ring. Dots under it mark the page.
 */
@Composable
internal fun SectionCharts(title: String, sections: List<BudgetSection>, pager: PagerState, scope: ChartScope, carriedCents: Long = 0) {
    val colors = remember(sections) { sectionColors(sections) }
    val rings = ringSections(sections)
    val coroutines = rememberCoroutineScope()
    val jump = { section: BudgetSection -> rings.indexOf(section).takeIf { it >= 0 }?.let { i -> coroutines.launch { pager.animateScrollToPage(i + 1) } }; Unit }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        HorizontalPager(pager, Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) { index ->
            if (index == 0) BudgetPie(title, sections, colors, scope, jump)
            else {
                val section = rings[index - 1]
                BudgetRing(section, colors[sections.indexOf(section)], if (section.kind == SectionKind.FREE) carriedCents else 0)
            }
        }
        if (rings.isNotEmpty()) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), Arrangement.Center, Alignment.CenterVertically) {
            (0..rings.size).forEach { i ->
                val on = i == pager.currentPage
                Box(
                    Modifier.size(48.dp).clickable(role = Role.Tab) { coroutines.launch { pager.animateScrollToPage(i) } }
                        .semantics { selected = on; contentDescription = if (i == 0) "Distribución" else rings[i - 1].label },
                    contentAlignment = Alignment.Center,
                ) { Box(Modifier.size(if (on) 7.dp else 5.dp).background(if (on) White else Muted, CircleShape)) }
            }
        }
    }
}

@Composable
private fun PageHeader(icon: String, label: String, period: BudgetPeriod?) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        StrokeIcon(icon, StrokeState.DONE, 20.dp)
        Text(label, style = MaterialTheme.typography.titleMedium, color = White)
        period?.let { StrokeIcon(periodIcon(it), StrokeState.OFF, 16.dp) }
    }
}

/** A compact allocation ring accompanies the available amount; labeled rows open each category. */
@Composable
private fun BudgetPie(title: String, sections: List<BudgetSection>, colors: List<Color>, scope: ChartScope, onPick: (BudgetSection) -> Unit) {
    var picked by remember(sections) { mutableStateOf<String?>(null) }
    val pickedIndex = sections.indexOfFirst { it.key == picked }
    val total = sections.sumOf { it.budgetCents }.coerceAtLeast(1)
    val spendable = sections.filter { it.kind != SectionKind.SAVING }
    val spent = spendable.sumOf { it.spentCents }
    val budget = spendable.sumOf { it.budgetCents }
    val left = budget - spent
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val stack = maxWidth < 320.dp || LocalDensity.current.fontScale > 1.2f
            val amount: @Composable () -> Unit = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (budget <= 0) "Sin presupuesto" else if (left < 0) "Sobre el presupuesto" else if (title == "Hoy") "Presupuestos de hoy" else "Disponible · $title",
                        style = MaterialTheme.typography.bodyMedium, color = Muted)
                    FinanceAmount(if (left < 0) -left else left, color = if (left < 0) MoneyOver else White)
                    Text("Gastado ${wholePesos(spent)}", style = MaterialTheme.typography.bodySmall, color = Muted)
                    Text("Presupuesto ${wholePesos(budget)}", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
            }
            val chart: @Composable () -> Unit = {
                Canvas(
                    Modifier.size(112.dp)
                        .semantics { contentDescription = "Distribución de $title. Gastado ${wholePesos(spent)} de ${wholePesos(budget)}. Detalles en las categorías." }
                        .pointerInput(sections) {
                            detectTapGestures { tap ->
                                val c = Offset(size.width / 2f, size.height / 2f)
                                val d = hypot(tap.x - c.x, tap.y - c.y)
                                if (d < size.width * 0.30f) { picked = null; return@detectTapGestures }
                                if (d > size.width / 2f) return@detectTapGestures
                                val angle = (Math.toDegrees(atan2((tap.y - c.y).toDouble(), (tap.x - c.x).toDouble())) + 450) % 360
                                var start = 0.0
                                sections.forEach { section ->
                                    val end = start + 360.0 * section.budgetCents / total
                                    if (section.budgetCents > 0 && angle >= start && angle < end) {
                                        picked = if (picked == section.key) null else section.key
                                        return@detectTapGestures
                                    }
                                    start = end
                                }
                            }
                        },
                ) {
                    val stroke = 12.dp.toPx()
                    val inset = Offset(stroke / 2, stroke / 2)
                    val arc = Size(size.width - stroke, size.height - stroke)
                    if (sections.isEmpty()) drawArc(Track, 0f, 360f, false, inset, arc, style = Stroke(stroke))
                    var start = -90f
                    sections.forEachIndexed { i, section ->
                        val sweep = 360f * section.budgetCents / total
                        if (sweep > 0f) {
                            val color = if (section.over) MoneyOver else colors[i]
                            val fade = if (pickedIndex >= 0 && pickedIndex != i) 0.3f else 1f
                            val segment = (sweep - 3f).coerceAtLeast(0f)
                            drawArc(color.copy(alpha = OFF_ALPHA * fade), start, segment, false, inset, arc, style = Stroke(stroke))
                            val fraction = (section.spentCents.toFloat() / section.budgetCents).coerceIn(0f, 1f)
                            drawArc(color.copy(alpha = fade), start, segment * fraction, false, inset, arc, style = Stroke(stroke))
                        }
                        start += sweep
                    }
                }
            }
            if (budget <= 0 && spent <= 0) amount()
            else if (stack) Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { amount(); chart() }
            else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { amount() }
                chart()
            }
        }
        if (budget <= 0) Text("Define tus importes en Presupuesto para ver cuánto puedes gastar.", style = MaterialTheme.typography.bodySmall, color = Muted)
        sections.getOrNull(pickedIndex)?.let { Insights(it, colors[pickedIndex], scope, total) }
        Column {
            sections.forEachIndexed { i, section ->
                Row(
                    Modifier.fillMaxWidth().clickable(role = Role.Button) {
                        if (section.kind == SectionKind.SAVING) picked = if (picked == section.key) null else section.key else onPick(section)
                    }.heightIn(min = 56.dp).padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(7.dp).background(colors[i], CircleShape))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(section.label, style = MaterialTheme.typography.bodyMedium, color = White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(if (section.kind == SectionKind.SAVING) "Reservado" else "De ${wholePesos(section.budgetCents)}" , style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                    Text(wholePesos(section.spentCents), style = MaterialTheme.typography.titleSmall, color = if (section.over) MoneyOver else White)
                    Text("›", color = Muted, style = MaterialTheme.typography.titleMedium)
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(FinanceRule))
            }
        }
    }
}

/** What a picked slice means, in a few plain lines under its name and color. */
@Composable
private fun Insights(section: BudgetSection, color: Color, scope: ChartScope, budgetTotal: Long) {
    val (from, to) = BudgetSections.range(scope.span, scope.day)
    val records = scope.money.filter { val d = Budget.dateOf(it); !d.isBefore(from) && !d.isAfter(to) && BudgetSections.belongs(section, it, scope.topicNames) }
    val total = (java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1).toInt()
    val gone = (java.time.temporal.ChronoUnit.DAYS.between(from, scope.today) + 1).toInt().coerceIn(0, total)
    val lines = BudgetSections.insights(section, records, budgetTotal, gone, total)
    Column(
        Modifier.fillMaxWidth().background(Color(0xFF141414), androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(color, CircleShape))
            StrokeIcon(sectionIcon(section), StrokeState.DONE, 18.dp)
            Text(section.label, style = MaterialTheme.typography.titleMedium, color = White, modifier = Modifier.weight(1f))
            Text(wholePesos(section.spentCents), style = MaterialTheme.typography.titleMedium, color = if (section.kind == SectionKind.SAVING) SavingGreen else usageColor(section.spentCents, section.budgetCents))
        }
        lines.forEachIndexed { i, line ->
            Text(line, style = MaterialTheme.typography.bodyMedium, color = if (i == 0) White else Muted)
        }
    }
}

/**
 * One section: the track in its own color, dim; the fill shows how it is going with what went out against its
 * budget (green plenty, yellow going, orange little left, red past it). The middle says what is left in that same
 * color; under it, what went out and the budget, and for free spending today what earlier days left or took.
 */
@Composable
private fun BudgetRing(section: BudgetSection, color: Color, carriedCents: Long) {
    val left = section.leftCents
    val over = section.over
    val state = if (section.over) MoneyOver else color
    val target = when {
        section.spentCents <= 0 -> 0f
        section.budgetCents <= 0 -> 1f
        else -> (section.spentCents.toFloat() / section.budgetCents).coerceAtMost(1f)
    }
    val fill = remember { Animatable(0f) }
    LaunchedEffect(target) { fill.animateTo(target, tween(220)) }
    Column(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            contentDescription = "${section.label}: quedan ${wholePesos(left)} de ${wholePesos(section.budgetCents)}"
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PageHeader(sectionIcon(section), section.label, section.period)
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(200.dp)) {
                val ring = size.minDimension * 0.12f
                val inset = ring / 2
                val arc = Size(size.width - ring, size.height - ring)
                drawArc(color.copy(alpha = OFF_ALPHA), 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(ring))
                if (fill.value > 0f) drawArc(
                    state, -90f, 360f * fill.value, false, Offset(inset, inset), arc,
                    style = Stroke(ring, cap = if (fill.value < 1f) StrokeCap.Round else StrokeCap.Butt),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SmallLabel(if (over) "De más" else "Queda")
                BigAmount(left, 26.dp, state)
            }
        }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
            Figure("Gastado", section.spentCents, White)
            Figure("Presupuesto", section.budgetCents, Muted)
        }
        if (abs(carriedCents) >= 100) Text(
            (if (carriedCents > 0) "+" else "−") + wholePesos(abs(carriedCents)) + " de días anteriores",
            style = MaterialTheme.typography.labelMedium,
            color = if (carriedCents > 0) MoneyPlenty else MoneyOver,
        )
    }
}

@Composable
private fun Figure(label: String, cents: Long, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(wholePesos(cents), style = MaterialTheme.typography.titleMedium, color = color)
        SmallLabel(label)
    }
}
