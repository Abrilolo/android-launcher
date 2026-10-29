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
import com.adrian.minimallauncher.feature.CardFill
import com.adrian.minimallauncher.feature.CardLine
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.feature.SettingsGear
import com.adrian.minimallauncher.ui.components.DotMatrixText
import com.adrian.minimallauncher.ui.components.LocalReactions
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
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
 * Finanzas (docs/design/concept/FinanzasVisual.html): what is left today, the month and the week, the week as
 * columns of dots (overspending lowers the days to come), where the extras bag comes from, budgets per topic and
 * the shared bills still owed. Only icons and numbers; names live in the settings behind the gear.
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
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.End) { SettingsGear({ settingsOpen = true }) }
            TodayBlock(summary, dayPlay)
            PeriodStats(summary)
            WeekColumns(summary, money)
            Box(Modifier.fillMaxWidth().height(1.dp).background(CardLine))
            IncomeBar(summary)
            if (summary.topics.isNotEmpty()) Topics(summary.topics)
            if (summary.sharedOpen.isNotEmpty()) SharedCard(summary, sharedPlay) { record ->
                onSettle(record.id, record.pendingFromOthersCents)
                sharedPlay++
                reactions.tick()
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp)
                .size(52.dp)
                .background(White, CircleShape)
                .clickable(role = Role.Button) { entryOpen = true }
                .semantics { contentDescription = "Registrar" },
            contentAlignment = Alignment.Center,
        ) {
            Text("+", style = MaterialTheme.typography.headlineSmall, color = Color.Black)
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

/** What can still be spent today, big, and today's dots: white spent, gray left, all red once past it. */
@Composable
private fun TodayBlock(summary: FinanceSummary, play: Int) {
    val left = summary.todayLeftCents
    val over = left < 0
    Column(
        Modifier.semantics(mergeDescendants = true) { contentDescription = "Hoy quedan ${wholePesos(left)}" },
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
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
        Stat("mes", StrokeState.DONE, summary.bagLeftCents, "Quedan en el mes")
        Stat("semana", StrokeState.DONE, summary.weekLeftCents, "Quedan en la semana")
        Stat("bolsa", StrokeState.OFF, summary.bagCents, "Bolsa de extras")
    }
}

@Composable
private fun Stat(icon: String, state: StrokeState, cents: Long, description: String) {
    Row(
        Modifier.semantics(mergeDescendants = true) { contentDescription = "$description ${wholePesos(cents)}" },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StrokeIcon(icon, state, 20.dp)
        Text(wholePesos(cents), style = MaterialTheme.typography.labelLarge, color = if (cents < 0) LauncherRed else MaterialTheme.colorScheme.onSurface)
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
            Leg("ingreso", StrokeState.DONE, summary.fixedIncomeCents, "Ingreso fijo")
            Leg("fijos", StrokeState.OFF, summary.fixedCostsCents, "Gastos fijos")
            Leg("ahorro", StrokeState.DONE, summary.savingCents, "Ahorro")
            Leg("bolsa", StrokeState.NOW, summary.bagCents, "Bolsa de extras")
            Box(Modifier.width(1.dp).height(40.dp).background(CardLine))
            Leg("transferencia", StrokeState.OFF, summary.otherIncomeCents, "Ingresos aparte")
        }
    }
}

@Composable
private fun Leg(icon: String, state: StrokeState, cents: Long, description: String) {
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
        Modifier.fillMaxWidth().background(CardFill, RoundedCornerShape(24.dp)).border(1.dp, CardLine, RoundedCornerShape(24.dp)).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "Por cobrar ${wholePesos(summary.pendingFromOthersCents)}" },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StrokeIcon("compartido", StrokeState.NOW, 24.dp, playKey = play)
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
                Spacer(Modifier.weight(1f))
                Text(wholePesos(record.amountCents), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(wholePesos(record.pendingFromOthersCents), Modifier.width(60.dp), style = MaterialTheme.typography.labelMedium, color = LauncherRed, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                StrokeIcon("listo", StrokeState.OFF, 16.dp)
            }
        }
    }
}
