package com.adrian.minimallauncher.feature.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.FinanceRecord
import com.adrian.minimallauncher.feature.LauncherAccent
import com.adrian.minimallauncher.feature.shortDay
import com.adrian.minimallauncher.ui.components.BarRowHeight
import com.adrian.minimallauncher.ui.components.ChartPart
import com.adrian.minimallauncher.ui.components.ColumnChart
import com.adrian.minimallauncher.ui.components.DonutChart
import com.adrian.minimallauncher.ui.components.HorizontalBarChart
import com.adrian.minimallauncher.ui.components.InsightBubble
import com.adrian.minimallauncher.ui.components.InsightBubbleWidth
import com.adrian.minimallauncher.ui.components.InsightLine
import com.adrian.minimallauncher.ui.components.SmoothLineChart
import com.adrian.minimallauncher.ui.theme.LauncherGray
import com.adrian.minimallauncher.ui.theme.LauncherWhite
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val DayInitials = listOf("L", "M", "X", "J", "V", "S", "D")
private val PageTitles = listOf("Gasto por día", "Por categoría", "Por concepto", "Acumulado")
private val Track = Color(0xFF16283A)
private val Danger = Color(0xFFFF2962)
private val Success = Color(0xFF4DD402)

/**
 * Paleta de datos (diseño 2026-11-10): cada cosa distinta lleva su propio color, siempre en el
 * mismo orden, para que la misma categoría o concepto se reconozca entre gráficas.
 */
internal val ChartPalette = listOf(
    Color(0xFF1A77B5), // azul
    Color(0xFFF2F0C4), // crema
    Color(0xFFC2D806), // lima
    Color(0xFFFFCF7D), // ámbar
    Color(0xFF7FC4F0), // azul claro
    Color(0xFF4DD402), // verde
)

/**
 * Gráficas del período, una a la vez: se cambian deslizando y los puntos de abajo dicen en cuál vas.
 * Tocar cualquier parte de una gráfica abre, justo ahí, un recuadro con sus datos; otro toque lo cierra.
 * Las gráficas no reservan espacio para los datos: el recuadro flota encima.
 */
@Composable
internal fun FinanceChartsPager(
    spent: Long,
    groups: List<SpendGroup>,
    records: List<FinanceRecord>,
    days: List<LocalDate>,
    spentByDay: Map<LocalDate, Long>,
    allowance: Long,
) {
    val pager = rememberPagerState(pageCount = { PageTitles.size })
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val daily = days.map { (spentByDay[it] ?: 0L).toFloat() }
    val weekly = days.size > 10
    val buckets: List<List<LocalDate>> = if (weekly) days.chunked(7) else days.map { listOf(it) }
    val columns = buckets.map { b -> b.sumOf { spentByDay[it] ?: 0L }.toFloat() }
    val labels = if (weekly) buckets.indices.map { "S${it + 1}" } else days.map { DayInitials[it.dayOfWeek.value - 1] }
    val over = columns.indices.filter { columns[it] > allowance }.toSet()
    val parts = groups.mapIndexed { i, g -> ChartPart(expenseName(g.kind), g.cents.toFloat(), ChartPalette[i % ChartPalette.size]) }
    val total = parts.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1f)
    val conceptKinds = groups.flatMap { g -> g.parts.map { (name, cents) -> Triple(name, cents, g.kind) } }
        .sortedByDescending { it.second }.take(5)
    val concepts = conceptKinds.mapIndexed { i, (name, cents, _) -> ChartPart(name, cents.toFloat(), ChartPalette[i % ChartPalette.size]) }
    val cumulative = daily.runningFold(0f) { acc, v -> acc + v }.drop(1)

    val key = days.firstOrNull()?.toString()
    var column by rememberSaveable(key) { mutableIntStateOf(-1) }
    var slice by rememberSaveable(key) { mutableIntStateOf(-1) }
    var sliceAt by remember(key) { mutableStateOf(Offset.Zero) }
    var concept by rememberSaveable(key) { mutableIntStateOf(-1) }
    var point by rememberSaveable(key) { mutableIntStateOf(-1) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalPager(pager, Modifier.fillMaxWidth().height(280.dp), verticalAlignment = Alignment.Top) { page ->
            Column(Modifier.fillMaxWidth().swipeBlur(pager, page), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                val figure: Pair<String, Color> = when (page) {
                    0 -> wholePesos(columns.sum().toLong()) to ChartPalette[0]
                    1 -> wholePesos(spent) to ChartPalette[0]
                    2 -> wholePesos(concepts.sumOf { it.value.toDouble() }.toLong()) to ChartPalette[0]
                    else -> wholePesos(cumulative.lastOrNull()?.toLong() ?: 0L) to ChartPalette[0]
                }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    SectionLabel(PageTitles[page])
                    Text(figure.first, style = MaterialTheme.typography.titleLarge, color = figure.second)
                }
                if (columns.sum() <= 0f) EmptyChart("Sin gastos en este periodo", height = 200.dp, bars = 7)
                else when (page) {
                    0 -> BoxWithConstraints(Modifier.fillMaxWidth()) {
                        ColumnChart(
                            columns, labels, over = over, height = 180.dp, selected = column,
                            onSelect = { i -> column = if (column == i) -1 else i },
                        )
                        if (column in buckets.indices) {
                            val bucket = buckets[column]
                            val recs = records.filter { Budget.dateOf(it) in bucket }
                            val diff = allowance - columns[column].toLong()
                            Callout(
                                x = maxWidth * ((column + 0.5f) / columns.size), y = 0.dp, width = maxWidth, height = maxHeight,
                                title = if (weekly) "Semana ${column + 1} · ${wholePesos(columns[column].toLong())}" else "${shortDay(bucket.first())} · ${wholePesos(columns[column].toLong())}",
                                lines = buildList {
                                    add(InsightLine("Pagos", "${recs.size}", LauncherWhite))
                                    if (recs.isNotEmpty()) add(InsightLine("Promedio", wholePesos(recs.sumOf { it.amountCents } / recs.size), ChartPalette[0]))
                                    recs.maxByOrNull { it.amountCents }?.let { add(InsightLine(nameOf(it), wholePesos(it.amountCents), ChartPalette[3])) }
                                    if (allowance > 0) add(if (diff >= 0) InsightLine("Te quedó", wholePesos(diff), Success) else InsightLine("Te pasaste", wholePesos(-diff), Danger))
                                },
                            )
                        }
                    }
                    1 -> BoxWithConstraints(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            val shown = parts.mapIndexed { i, p -> if (slice == -1 || slice == i) p else p.copy(color = p.color.copy(alpha = 0.18f)) }
                            DonutChart(
                                shown,
                                center = if (slice in parts.indices) "${(parts[slice].value / total * 100).roundToInt()}%" else "100%",
                                diameter = 150.dp, thickness = 20.dp,
                                onSelect = { i, at -> sliceAt = at; slice = if (slice == i) -1 else i },
                            )
                            Column {
                                parts.forEachIndexed { i, p ->
                                    Row(
                                        Modifier
                                            .clickable(role = Role.Button) {
                                                sliceAt = with(density) { Offset(150.dp.toPx() + 30.dp.toPx(), (i * 34).dp.toPx()) }
                                                slice = if (slice == i) -1 else i
                                            }
                                            .padding(vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Box(Modifier.size(10.dp).background(p.color, CircleShape))
                                        Text(p.label, style = MaterialTheme.typography.titleSmall, color = if (slice == -1 || slice == i) LauncherWhite else LauncherGray)
                                    }
                                }
                            }
                        }
                        if (slice in groups.indices) {
                            val recs = records.filter { it.kind == groups[slice].kind }
                            Callout(
                                x = with(density) { sliceAt.x.toDp() }, y = with(density) { sliceAt.y.toDp() } + 12.dp, width = maxWidth, height = maxHeight,
                                title = "${parts[slice].label} · ${wholePesos(groups[slice].cents)}",
                                lines = kindInsights(recs, groups[slice].cents, total, parts[slice].color),
                            )
                        }
                    }
                    2 -> BoxWithConstraints(Modifier.fillMaxWidth()) {
                        HorizontalBarChart(
                            parts = concepts, amountOf = { wholePesos(it.value.toLong()) },
                            selected = concept, onSelect = { i -> concept = if (concept == i) -1 else i },
                        )
                        if (concept in conceptKinds.indices) {
                            val (name, cents, kind) = conceptKinds[concept]
                            Callout(
                                x = maxWidth, y = (BarRowHeight + 10.dp) * concept + BarRowHeight, width = maxWidth, height = maxHeight,
                                title = name,
                                lines = listOf(
                                    InsightLine("Del gasto", "${(cents / total * 100).roundToInt()}%", concepts[concept].color),
                                    InsightLine("Tipo", expenseName(kind), LauncherWhite),
                                    InsightLine("Pagos", "${records.count { nameOf(it).equals(name, ignoreCase = true) }}", LauncherWhite),
                                ),
                            )
                        }
                    }
                    else -> BoxWithConstraints(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SmoothLineChart(cumulative, height = 180.dp, selected = point, onSelect = { i -> point = if (point == i) -1 else i })
                            val marks = if (days.isEmpty()) emptyList() else listOf(0, days.size / 3, 2 * days.size / 3, days.lastIndex).distinct()
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                marks.forEach { i -> Text(days[i].dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall, color = LauncherGray) }
                            }
                        }
                        if (point in days.indices && cumulative.size > 1) {
                            val sofar = cumulative[point].toLong()
                            val elapsed = point + 1
                            Callout(
                                x = maxWidth * (point.toFloat() / (cumulative.size - 1)), y = 0.dp, width = maxWidth, height = maxHeight,
                                title = "${shortDay(days[point])} · ${wholePesos(sofar)}",
                                lines = buildList {
                                    add(InsightLine("Ese día", wholePesos(daily[point].toLong()), if (daily[point] > allowance && allowance > 0) Danger else ChartPalette[0]))
                                    add(InsightLine("Promedio al día", wholePesos(sofar / elapsed), ChartPalette[3]))
                                    if (allowance > 0 && !weekly) {
                                        val pace = allowance * elapsed - sofar
                                        add(if (pace >= 0) InsightLine("Vas bajo", wholePesos(pace), Success) else InsightLine("Vas arriba", wholePesos(-pace), Danger))
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
        // Puntos de página: el azul es la gráfica que se ve; tocar uno salta a esa gráfica.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
            PageTitles.indices.forEach { i ->
                Box(
                    Modifier
                        .size(if (pager.currentPage == i) 10.dp else 8.dp)
                        .background(if (pager.currentPage == i) LauncherAccent else Track, CircleShape)
                        .clickable(role = Role.Tab) { scope.launch { pager.animateScrollToPage(i) } }
                        .semantics { contentDescription = PageTitles[i] },
                )
            }
        }
    }
}

/** El recuadro de datos, centrado en [x] y sin salirse de [width] ni de [height]. */
@Composable
private fun Callout(x: Dp, y: Dp, width: Dp, height: Dp, title: String, lines: List<InsightLine>) {
    val left = (x - InsightBubbleWidth / 2).coerceIn(0.dp, (width - InsightBubbleWidth).coerceAtLeast(0.dp))
    // Alto aproximado del recuadro: título y hasta cuatro líneas; así nunca se corta abajo.
    val top = y.coerceIn(0.dp, (height - (30 + 22 * lines.size).dp).coerceAtLeast(0.dp))
    InsightBubble(title, lines, Modifier.offset(left, top))
}

private fun nameOf(record: FinanceRecord) = record.description.ifBlank { record.category }.ifBlank { "Pago" }

/** Datos de una categoría: su parte del gasto, cuántos pagos, el promedio y el más grande. */
private fun kindInsights(recs: List<FinanceRecord>, cents: Long, total: Float, color: Color) = buildList {
    add(InsightLine("Del gasto", "${(cents / total * 100).roundToInt()}%", color))
    add(InsightLine("Pagos", "${recs.size}", LauncherWhite))
    if (recs.isNotEmpty()) add(InsightLine("Promedio", wholePesos(recs.sumOf { it.amountCents } / recs.size), ChartPalette[0]))
    recs.maxByOrNull { it.amountCents }?.let { add(InsightLine(nameOf(it), wholePesos(it.amountCents), ChartPalette[3])) }
}
