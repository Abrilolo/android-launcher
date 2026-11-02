package com.adrian.minimallauncher.feature.finance

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.FinanceRecord
import com.adrian.minimallauncher.data.MoneyKind
import com.adrian.minimallauncher.feature.CardLine
import com.adrian.minimallauncher.feature.ChartColumn
import com.adrian.minimallauncher.feature.ChartRow
import com.adrian.minimallauncher.feature.ChartShades
import com.adrian.minimallauncher.feature.ChartStack
import com.adrian.minimallauncher.feature.DockItem
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.feature.ModuleDock
import com.adrian.minimallauncher.feature.ModuleRow
import com.adrian.minimallauncher.feature.ModuleTitle
import com.adrian.minimallauncher.feature.RangeSwitch
import com.adrian.minimallauncher.feature.ShineLink
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.feature.StackChart
import com.adrian.minimallauncher.feature.shortDay
import com.adrian.minimallauncher.ui.components.DotMatrixText
import com.adrian.minimallauncher.ui.components.LocalReactions
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

private val spanish = Locale.forLanguageTag("es")
private val White = Color(0xFFF5F5F5)
private val Gray = Color(0xFF5C5C5C)
private val EmptyDot = Color(0xFF2E2E2E)
private const val TODAY_DOTS = 20
private const val TOPIC_DOTS = 10
private const val WEEK_ROWS = 10
private const val MOVEMENTS = 12

/** Pesos without cents when they are round. No thousands comma: the dot-matrix glyphs have none. */
fun formatPesos(cents: Long): String {
    val whole = cents / 100
    return if (cents % 100 == 0L) "$$whole" else "$$whole.%02d".format(cents % 100)
}

/** Whole pesos, rounded, with a minus when negative: allowances split by days rarely come out round. */
internal fun wholePesos(cents: Long): String = (if (cents < 0) "-$" else "$") + (abs(cents) + 50) / 100

/** The categories the entry sheet offers, plus any topic the user gave a budget. */
internal val BASE_CATEGORIES = listOf("comida", "transporte", "otro")

internal fun categoryIcon(category: String): String = when (category.lowercase(spanish)) {
    "comida" -> "colacion"
    "transporte" -> "traslado"
    else -> "otro"
}

internal fun periodIcon(period: BudgetPeriod): String = when (period) {
    BudgetPeriod.DAY -> "dia"
    BudgetPeriod.WEEK -> "semana"
    BudgetPeriod.MONTH -> "mes"
}

internal fun kindIcon(kind: MoneyKind): String = when (kind) {
    MoneyKind.INCOME_FIXED -> "ingreso"
    MoneyKind.TRANSFER -> "transferencia"
    MoneyKind.GIFT -> "regalo"
    MoneyKind.SERVICE -> "servicio"
    MoneyKind.FIXED -> "fijos"
    MoneyKind.EXTRA -> "extra"
    MoneyKind.SHARED -> "compartido"
}

/**
 * Finanzas, three views behind the dock. Hoy: what is left today, the month and the week, budgets per topic and the
 * shared bills still owed. Semana: columns of dots (overspending lowers the days to come) and its movements. Mes:
 * where the extras bag comes from, spending by day or week, and its movements.
 */
@Composable
fun FinanceScreen(
    summary: FinanceSummary,
    money: List<FinanceRecord>,
    dailyLimitCents: Long,
    onAdd: (kind: MoneyKind, cents: Long, description: String, category: String, pendingFromOthers: Long) -> Unit,
    onSettle: (id: String, paidCents: Long) -> Unit,
    onSetBudget: (topic: String, period: BudgetPeriod, cents: Long) -> Unit,
    onSavingPct: (Int) -> Unit,
    onDailyLimit: (Long) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var entryOpen by rememberSaveable { mutableStateOf(false) }
    var tab by rememberSaveable { mutableStateOf("hoy") }
    // Bumped to draw an icon once: the day after an extra, the shared bills after one is settled.
    var dayPlay by remember { mutableIntStateOf(0) }
    var sharedPlay by remember { mutableIntStateOf(0) }
    val reactions = LocalReactions.current

    if (settingsOpen) {
        FinanceSettings(summary, money, dailyLimitCents, onSetBudget, onSavingPct, onDailyLimit, onDelete, { settingsOpen = false }, modifier)
        return
    }
    BackHandler(onBack = onBack)
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).padding(horizontal = 24.dp)) {
                ModuleTitle(when (tab) { "semana" -> "Semana"; "mes" -> monthTitle(summary); else -> dayTitle(summary) }, { settingsOpen = true })
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    when (tab) {
                        "semana" -> {
                            WeekColumns(summary, money)
                            Movements(money.filter { Budget.isExtra(it) && !Budget.dateOf(it).isBefore(Budget.weekStart(summary.date)) })
                        }
                        "mes" -> {
                            IncomeBar(summary)
                            SpendingChart(summary, money)
                            Movements(money.filter { YearMonth.from(Budget.dateOf(it)) == YearMonth.from(summary.date) })
                        }
                        else -> {
                            TodayBlock(summary, dayPlay)
                            ShineLink("Registrar", "Quedan ${wholePesos(summary.weekLeftCents)} en la semana", { entryOpen = true })
                            PeriodStats(summary)
                            if (summary.topics.isNotEmpty()) Topics(summary.topics)
                            if (summary.sharedOpen.isNotEmpty()) SharedCard(summary, sharedPlay) { record ->
                                onSettle(record.id, record.pendingFromOthersCents)
                                sharedPlay++
                                reactions.tick()
                            }
                        }
                    }
                }
            }
            ModuleDock(
                listOf(
                    DockItem("hoy", "Hoy", "hoy", badge = summary.sharedOpen.size.takeIf { it > 0 }?.toString()),
                    DockItem("semana", "Semana", "semana"),
                    DockItem("mes", "Mes", "mes"),
                    DockItem("ajustes", "Ajustes", "gear", apart = true),
                ),
                tab,
                onOpen = { key -> if (key == "ajustes") settingsOpen = true else tab = key },
            )
        }
        AnimatedVisibility(
            entryOpen,
            Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            EntrySheet(summary, onClose = { entryOpen = false }) { kind, cents, description, category, pending ->
                val over = (kind == MoneyKind.EXTRA || kind == MoneyKind.SHARED) && Budget.wouldExceed(summary, category, cents)
                onAdd(kind, cents, description, category, pending)
                if (kind == MoneyKind.EXTRA || kind == MoneyKind.SHARED) dayPlay++
                if (kind == MoneyKind.SHARED && pending > 0) sharedPlay++
                if (over) reactions.reject() else reactions.tick()
                entryOpen = false
            }
        }
    }
}

private fun dayTitle(summary: FinanceSummary): String {
    val date = summary.date
    val day = date.dayOfWeek.getDisplayName(TextStyle.FULL, spanish).replaceFirstChar { it.uppercase(spanish) }
    return "$day, ${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.FULL, spanish)}"
}

private fun monthTitle(summary: FinanceSummary) = summary.date.month.getDisplayName(TextStyle.FULL, spanish).replaceFirstChar { it.uppercase(spanish) }

private val spentKinds = setOf(MoneyKind.EXTRA, MoneyKind.SHARED, MoneyKind.FIXED)

/** Each record with its kind's icon; money in gets a plus. */
@Composable
private fun Movements(records: List<FinanceRecord>) {
    val latest = records.sortedByDescending { it.createdAt }.take(MOVEMENTS)
    if (latest.isEmpty()) return
    Column {
        SmallLabel("Movimientos")
        latest.forEach { record ->
            val out = record.kind in spentKinds
            ModuleRow(
                if (Budget.isExtra(record)) categoryIcon(record.category) else kindIcon(record.kind),
                record.description.ifBlank { record.category.replaceFirstChar { it.uppercase(spanish) } },
                shortDay(Budget.dateOf(record)),
                (if (out) "" else "+") + wholePesos(record.amountCents),
            )
        }
    }
}

/** Extras by day or week, stacked by category; what passed the allowance hangs in red. */
@Composable
private fun SpendingChart(summary: FinanceSummary, money: List<FinanceRecord>) {
    var range by rememberSaveable { mutableStateOf("dia") }
    val columns = remember(money, range, summary.date, summary.weekAllowanceCents) { spendingColumns(money, range, summary.date, summary.weekAllowanceCents) }
    val from = summary.date.minusDays(19)
    val total = money.filter { Budget.isExtra(it) && !Budget.dateOf(it).isBefore(from) }.sumOf { it.amountCents }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                Text(wholePesos(total), style = MaterialTheme.typography.headlineMedium, color = White)
                SmallLabel("en 20 días")
            }
            RangeSwitch(listOf("dia" to "Día", "semana" to "Semana"), range, { range = it })
        }
        StackChart(columns)
    }
}

private fun spendingColumns(money: List<FinanceRecord>, range: String, today: LocalDate, weekAllowance: Long): List<ChartColumn> {
    val weeks = range == "semana"
    val keys = if (weeks) (11L downTo 0L).map { today.with(DayOfWeek.MONDAY).minusWeeks(it) } else (19L downTo 0L).map { today.minusDays(it) }
    val extras = money.filter(Budget::isExtra).map { it to Budget.dateOf(it) }
    val categories = (BASE_CATEGORIES + extras.map { it.first.category.lowercase(spanish) }).distinct()
    val limit = if (weeks) weekAllowance else weekAllowance / 7
    return keys.map { key ->
        val rows = extras.filter { (_, d) -> (if (weeks) d.with(DayOfWeek.MONDAY) else d) == key }.map { it.first }
        val parts = categories.mapIndexed { i, category -> Triple(category, ChartShades[i.coerceAtMost(ChartShades.lastIndex)], rows.filter { it.category.lowercase(spanish) == category }.sumOf { it.amountCents }) }.filter { it.third > 0 }
        val total = rows.sumOf { it.amountCents }
        val over = if (limit > 0) (total - limit).coerceAtLeast(0) else 0
        ChartColumn(
            if (weeks) "Semana ${shortDay(key)}" else shortDay(key), shortDay(key), wholePesos(total),
            parts.map { ChartStack(it.third.toFloat(), it.second) }, over.toFloat(),
            parts.map { ChartRow(it.second, it.first.replaceFirstChar { c -> c.uppercase(spanish) }, wholePesos(it.third)) } +
                if (over > 0) listOf(ChartRow(LauncherRed, "De más", wholePesos(over))) else emptyList(),
        )
    }
}

/** What can still be spent today, big, and today's dots: white spent, gray left, all red once past it. */
@Composable
private fun TodayBlock(summary: FinanceSummary, play: Int) {
    val left = summary.todayLeftCents
    val over = left < 0
    Column(
        Modifier.semantics(mergeDescendants = true) { contentDescription = "Hoy quedan ${wholePesos(left)}" },
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SmallLabel(if (over) "De más hoy" else "Quedan hoy")
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Bottom) {
            DotMatrixText(
                wholePesos(left), cell = 8.dp,
                color = if (over) LauncherRed else MaterialTheme.colorScheme.onSurface,
                accents = if (over) emptyMap() else mapOf('$' to LauncherRed),
                reveal = left,
            )
            StrokeIcon("dia", StrokeState.NOW, 32.dp, playKey = play)
        }
        DotBar(summary.todaySpentCents, summary.todayAllowanceCents, TODAY_DOTS, Modifier.fillMaxWidth())
    }
}

/** [count] dots: lit white as [spent] fills [allowance], every lit dot red once past it. */
@Composable
private fun DotBar(spent: Long, allowance: Long, count: Int, modifier: Modifier = Modifier) {
    val over = spent > allowance
    val lit = when {
        spent <= 0 -> 0
        allowance <= 0 -> count
        else -> ((spent * count + allowance - 1) / allowance).toInt().coerceAtMost(count)
    }
    Row(modifier, Arrangement.spacedBy(4.dp)) {
        for (i in 0 until count) {
            val color = when {
                i >= lit -> EmptyDot
                over -> LauncherRed
                else -> White
            }
            Box(Modifier.size(7.dp).background(color, CircleShape))
        }
    }
}

/** Left in the month (the bag), left in the week, and the bag itself. */
@Composable
private fun PeriodStats(summary: FinanceSummary) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Stat("mes", "Mes", StrokeState.DONE, summary.bagLeftCents, "Quedan en el mes")
        Stat("semana", "Semana", StrokeState.DONE, summary.weekLeftCents, "Quedan en la semana")
        Stat("bolsa", "Bolsa", StrokeState.OFF, summary.bagCents, "Bolsa de extras")
    }
}

@Composable
private fun Stat(icon: String, label: String, state: StrokeState, cents: Long, description: String) {
    Column(
        Modifier.semantics(mergeDescendants = true) { contentDescription = "$description ${wholePesos(cents)}" },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            StrokeIcon(icon, state, 20.dp)
            Text(wholePesos(cents), style = MaterialTheme.typography.labelLarge, color = if (cents < 0) LauncherRed else MaterialTheme.colorScheme.onSurface)
        }
        SmallLabel(label)
    }
}

/**
 * Monday to Sunday. Each column is what that day could spend (ring dots), filled with what it spent (gray, today
 * white) and topped in red with what went over. Days to come share what is left of the week, so they shrink after
 * an expensive day. Tomorrow's letter is red.
 */
@Composable
private fun WeekColumns(summary: FinanceSummary, money: List<FinanceRecord>) {
    val today = summary.date
    val start = Budget.weekStart(today)
    val todayIndex = today.dayOfWeek.value - 1
    val extras = money.filter(Budget::isExtra)
    val spent = (0..6).map { d -> val day = start.plusDays(d.toLong()); extras.filter { Budget.dateOf(it) == day }.sumOf { it.amountCents } }
    val week = summary.weekAllowanceCents
    val throughToday = spent.take(todayIndex + 1).sum()
    val allow = (0..6).map { d ->
        when {
            d <= todayIndex -> Budget.todayAllowance(week, spent.take(d).sum(), start.plusDays(d.toLong()))
            else -> ((week - throughToday) / (6 - todayIndex)).coerceAtLeast(0)
        }
    }
    val unit = ((0..6).maxOf { maxOf(allow[it], spent[it]) } / WEEK_ROWS).coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SmallLabel("Quedan ${wholePesos(summary.weekLeftCents)}")
        Row(Modifier.fillMaxWidth().height(13.dp * (WEEK_ROWS + 2)), verticalAlignment = Alignment.Bottom) {
            for (d in 0..6) {
                val cap = ((allow[d] + unit / 2) / unit).toInt()
                val fill = minOf(cap, ((spent[d] + unit / 2) / unit).toInt())
                val extra = (((spent[d] - allow[d]) + unit / 2) / unit).toInt().coerceIn(0, 2)
                val lit = if (d == todayIndex) White else Color(0xFFA8A8A8)
                Column(
                    Modifier.weight(1f).semantics { contentDescription = "${wholePesos(spent[d])} de ${wholePesos(allow[d])}" },
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    repeat(extra) { Box(Modifier.size(9.dp).background(LauncherRed, CircleShape)) }
                    for (i in cap - 1 downTo 0) {
                        if (i < fill) Box(Modifier.size(9.dp).background(lit, CircleShape))
                        else Box(Modifier.size(9.dp).border(1.5.dp, Color(0xFF3A3A3A), CircleShape))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            for (d in 0..6) {
                val letter = start.plusDays(d.toLong()).dayOfWeek.getDisplayName(TextStyle.NARROW, spanish).uppercase(spanish)
                Text(
                    letter, Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = when (d) {
                        todayIndex -> White
                        todayIndex + 1 -> LauncherRed
                        else -> Gray
                    },
                )
            }
        }
    }
}

/**
 * The fixed income as one bar: fixed costs (gray), savings (hatched), the extras bag (white as it is spent).
 * Transfers, gifts and services sit apart on the right: received, never budgeted.
 */
@Composable
private fun IncomeBar(summary: FinanceSummary) {
    val total = maxOf(summary.fixedIncomeCents, summary.fixedCostsCents + summary.savingCents + summary.bagCents).coerceAtLeast(1)
    val spentBag = minOf(summary.bagCents, summary.extrasMonthCents).coerceAtLeast(0)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Canvas(Modifier.fillMaxWidth().height(10.dp)) {
            val w = size.width
            val h = size.height
            val gap = 2.dp.toPx()
            drawRoundRect(EmptyDot, size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2))
            var x = 0f
            fun seg(cents: Long, draw: (Float, Float) -> Unit) {
                if (cents <= 0) return
                val width = w * cents / total
                draw(x, (width - gap).coerceAtLeast(1f))
                x += width
            }
            seg(summary.fixedCostsCents) { at, width -> drawRect(Gray, Offset(at, 0f), Size(width, h)) }
            seg(summary.savingCents) { at, width ->
                clipRect(at, 0f, at + width, h) {
                    var line = at - h
                    while (line < at + width) {
                        drawLine(White, Offset(line, h), Offset(line + h, 0f), strokeWidth = 2.dp.toPx())
                        line += 5.dp.toPx()
                    }
                }
            }
            seg(spentBag) { at, width -> drawRect(White, Offset(at, 0f), Size(width, h)) }
        }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
            Leg("ingreso", "Ingreso", StrokeState.DONE, summary.fixedIncomeCents, "Ingreso fijo")
            Leg("fijos", "Fijos", StrokeState.OFF, summary.fixedCostsCents, "Gastos fijos")
            Leg("ahorro", "Ahorro", StrokeState.DONE, summary.savingCents, "Ahorro")
            Leg("bolsa", "Bolsa", StrokeState.NOW, summary.bagCents, "Bolsa de extras")
            Box(Modifier.width(1.dp).height(40.dp).background(CardLine))
            Leg("transferencia", "Aparte", StrokeState.OFF, summary.otherIncomeCents, "Ingresos aparte")
        }
    }
}

@Composable
private fun Leg(icon: String, label: String, state: StrokeState, cents: Long, description: String) {
    Column(
        Modifier.semantics(mergeDescendants = true) { contentDescription = "$description ${wholePesos(cents)}" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StrokeIcon(icon, state, 20.dp)
        Text(
            ((cents + 50) / 100).toString(),
            style = MaterialTheme.typography.labelSmall,
            color = if (state == StrokeState.OFF) Gray else MaterialTheme.colorScheme.onSurface,
        )
        SmallLabel(label)
    }
}

/** One row per budget topic: its icon (red detail once over), today's dots, what is left today and its period. */
@Composable
private fun Topics(topics: List<TopicStatus>) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        topics.forEach { topic ->
            val left = topic.todayLeftCents
            Row(
                Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "${topic.topic}: hoy quedan ${wholePesos(left)}" },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StrokeIcon(categoryIcon(topic.topic), if (topic.over) StrokeState.NOW else StrokeState.DONE, 24.dp)
                SmallLabel(topic.topic.replaceFirstChar { it.uppercase(spanish) }, Modifier.width(72.dp), maxLines = 1)
                DotBar(topic.todaySpentCents, topic.todayAllowanceCents, TOPIC_DOTS, Modifier.weight(1f))
                Text(wholePesos(left), style = MaterialTheme.typography.labelLarge, color = if (left < 0) LauncherRed else MaterialTheme.colorScheme.onSurface)
                StrokeIcon(periodIcon(topic.period), StrokeState.OFF, 16.dp)
            }
        }
    }
}

/** Por cobrar: the total others owe in red; tapping a bill marks it paid back. */
@Composable
private fun SharedCard(summary: FinanceSummary, play: Int, onSettle: (FinanceRecord) -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "Por cobrar ${wholePesos(summary.pendingFromOthersCents)}" },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                StrokeIcon("compartido", StrokeState.NOW, 24.dp, playKey = play)
                SmallLabel("Por cobrar")
            }
            Text(wholePesos(summary.pendingFromOthersCents), style = MaterialTheme.typography.titleMedium, color = LauncherRed)
        }
        summary.sharedOpen.forEach { record ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { onSettle(record) }
                    .semantics(mergeDescendants = true) { contentDescription = "Marcar cobrada ${record.description}" }
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StrokeIcon(categoryIcon(record.category), StrokeState.OFF, 18.dp)
                SmallLabel(record.description.ifBlank { record.category }, Modifier.weight(1f), maxLines = 1)
                Text(wholePesos(record.amountCents), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(wholePesos(record.pendingFromOthersCents), Modifier.width(60.dp), style = MaterialTheme.typography.labelMedium, color = LauncherRed, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                StrokeIcon("listo", StrokeState.OFF, 16.dp)
            }
        }
    }
}
