package com.adrian.minimallauncher.feature.finance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adrian.minimallauncher.data.FinanceRecord
import com.adrian.minimallauncher.feature.LauncherAccent
import com.adrian.minimallauncher.feature.LauncherDanger
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.feature.bank.BankReceivable
import com.adrian.minimallauncher.ui.Tile
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * Finanzas widgets (concept: the "Widgets de finanzas" canvas, 2026-11-11). One widget per part of Finanzas, each in
 * 4×2, 2×2 and 4×1. No words and no counters: amounts, stroke icons, bars and dots, painted by how the money is going
 * (see [usageColor]). Tapping opens Finanzas; the + on Hoy opens the register.
 */
object FinanceWidgets {
    const val TODAY = "finance"
    const val WEEK = "finance_week"
    const val BUDGET = "finance_budget"
    const val PAYMENTS = "finance_payments"
    const val GOAL = "finance_goal"
    const val BAG = "finance_bag"
    const val OWED = "finance_owed"
    val ids = listOf(TODAY, WEEK, BUDGET, PAYMENTS, GOAL, BAG, OWED)
    val labels = mapOf(
        TODAY to "Finanzas · Disponible",
        WEEK to "Finanzas · Semana y mes",
        BUDGET to "Finanzas · Presupuesto",
        PAYMENTS to "Finanzas · Pagos",
        GOAL to "Finanzas · Meta",
        BAG to "Finanzas · Bolsa",
        OWED to "Finanzas · Por cobrar",
    )
    /** The short tag each widget wears so it is clear what it shows (pedido 2026-11-11). */
    val tags = mapOf(
        TODAY to "DISPONIBLE", WEEK to "SEMANA", BUDGET to "PRESUPUESTO", PAYMENTS to "PAGOS",
        GOAL to "META", BAG to "BOLSA", OWED to "POR COBRAR",
    )
    /** Opens Finanzas with the register sheet up. */
    const val ADD_ROUTE = "finance:add"
}

/** What the Finanzas widgets read. */
data class FinanceWidgetData(
    val summary: FinanceSummary,
    val money: List<FinanceRecord>,
    val plan: FinancePlan,
    val goalText: String,
    val receivables: List<BankReceivable>,
    val today: LocalDate,
    /** Cards and accounts: a card's coming payment shows with the scheduled ones. */
    val accounts: List<com.adrian.minimallauncher.feature.bank.BankAccount> = emptyList(),
)

private val Track = Color(0xFF1E1E1E)
private val Stub = Color(0xFF2A2A2A)
private val Muted = Color(0xFFB0B0B0)
private val Dim = Color(0xFF5C5C5C)
private val White = Color(0xFFF8F9F7)

@Composable
fun FinanceWidget(id: String, data: FinanceWidgetData, modifier: Modifier, onOpen: (String) -> Unit, onLongClick: () -> Unit) {
    Tile(modifier, { onOpen("finance") }, onLongClick) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val shape = when {
                maxHeight < 96.dp -> Shape.STRIP
                maxWidth > maxHeight * 1.6f -> Shape.WIDE
                else -> Shape.SQUARE
            }
            val tag = FinanceWidgets.tags[id] ?: FinanceWidgets.tags.getValue(FinanceWidgets.TODAY)
            val content: @Composable () -> Unit = {
                when (id) {
                    FinanceWidgets.WEEK -> WeekWidget(data, shape)
                    FinanceWidgets.BUDGET -> BudgetWidget(data, shape)
                    FinanceWidgets.PAYMENTS -> PaymentsWidget(data, shape)
                    FinanceWidgets.GOAL -> GoalWidget(data, shape)
                    FinanceWidgets.BAG -> BagWidget(data, shape)
                    FinanceWidgets.OWED -> OwedWidget(data, shape)
                    else -> TodayWidget(data, shape, { onOpen(FinanceWidgets.ADD_ROUTE) }, onLongClick)
                }
            }
            if (shape == Shape.STRIP) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SmallLabel(tag, color = Muted, maxLines = 1)
                    Box(Modifier.weight(1f).fillMaxHeight()) { content() }
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallLabel(tag, color = Muted, maxLines = 1)
                    Box(Modifier.weight(1f).fillMaxWidth()) { content() }
                }
            }
        }
    }
}

private enum class Shape { SQUARE, WIDE, STRIP }

// ---------- Hoy ----------

@Composable
private fun TodayWidget(data: FinanceWidgetData, shape: Shape, onAdd: () -> Unit, onLongClick: () -> Unit) {
    // Dinero disponible: what is in the accounts less what is still to pay this month (FinanceMoney).
    val view = FinanceMoney.of(data.accounts, data.plan, data.summary, data.money, data.today)
    val day = dailyFinance(data.summary, data.money, data.today)
    val left = view.availableCents
    val total = view.haveCents.coerceAtLeast(1)
    val color = if (left < 0) MoneyOver else leftColor(left, total)
    val fraction = if (left < 0) 1f else (left.toFloat() / total).coerceIn(0f, 1f)
    when (shape) {
        Shape.SQUARE -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Ring(fraction, color, 6.dp, Modifier.size(100.dp))
            Amount(left, color, 20.sp)
        }
        Shape.WIDE -> Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                Ring(fraction, color, 7.dp, Modifier.fillMaxSize())
                Amount(left, color, 19.sp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DayBars(weekBars(data), Modifier.fillMaxWidth().height(44.dp), barWidth = 12.dp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    day.latest?.let {
                        StrokeIcon(categoryIcon(Budget.categoryOf(it)), StrokeState.DONE, 20.dp, color = Muted)
                        Mono(wholePesos(it.amountCents), Color(0xFFE8E8E8))
                    }
                    Spacer(Modifier.weight(1f))
                    AddButton(onAdd, onLongClick)
                }
            }
        }
        Shape.STRIP -> Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Amount(left, color, 20.sp)
            LineBar(fraction, color, Modifier.weight(1f))
            AddButton(onAdd, onLongClick)
        }
    }
}

// ---------- Semana y mes ----------

@Composable
private fun WeekWidget(data: FinanceWidgetData, shape: Shape) {
    val s = data.summary
    val weekLeft = s.weekLeftCents
    val weekColor = leftColor(weekLeft, s.weekAllowanceCents)
    val monthLeft = s.moneyLeftCents
    val monthTotal = maxOf(monthLeft + s.spentMonthCents, 1)
    val monthColor = leftColor(monthLeft, monthTotal)
    val bars = weekBars(data)
    when (shape) {
        Shape.SQUARE -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Amount(weekLeft, weekColor, 24.sp)
            DayBars(bars, Modifier.fillMaxWidth().height(56.dp), barWidth = 12.dp)
        }
        Shape.WIDE -> Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                Amount(weekLeft, weekColor, 21.sp)
                DayBars(bars, Modifier.fillMaxWidth().height(56.dp), barWidth = 12.dp)
            }
            Box(Modifier.width(1.dp).fillMaxHeight().background(Track))
            Column(Modifier.weight(1.1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                Amount(monthLeft, monthColor, 21.sp)
                MonthLine(monthCurve(data, monthTotal), data.today, monthColor, Modifier.fillMaxWidth().height(56.dp))
            }
        }
        Shape.STRIP -> Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Amount(weekLeft, weekColor, 20.sp)
            DayBars(bars, Modifier.width(60.dp).height(28.dp), barWidth = 6.dp)
            LineBar(s.spentMonthCents.toFloat() / monthTotal, monthColor, Modifier.weight(1f), tick = data.today.dayOfMonth.toFloat() / data.today.lengthOfMonth())
        }
    }
}

// ---------- Presupuesto ----------

@Composable
private fun BudgetWidget(data: FinanceWidgetData, shape: Shape) {
    val sections = BudgetSections.of(data.summary, data.money, Span.MONTH, data.today)
        .filter { it.kind == SectionKind.FREE || it.kind == SectionKind.TOPIC }
    if (sections.isEmpty()) return Empty("presupuesto")
    val colors = sectionColors(sections)
    when (shape) {
        Shape.SQUARE -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val left = sections.sumOf { it.leftCents }
            val budget = sections.sumOf { it.budgetCents }
            Donut(sections, colors, Modifier.size(100.dp))
            Amount(left, leftColor(left, budget), 18.sp)
        }
        Shape.WIDE -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)) {
            sections.take(4).forEachIndexed { i, section ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StrokeIcon(sectionIcon(section), StrokeState.DONE, 20.dp, color = colors[i])
                    LineBar(usedFraction(section), colors[i], Modifier.weight(1f))
                    Mono(wholePesos(section.leftCents), usageColor(section.spentCents, section.budgetCents), Modifier.width(72.dp), end = true)
                }
            }
        }
        Shape.STRIP -> Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
            sections.take(4).forEachIndexed { i, section ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StrokeIcon(sectionIcon(section), StrokeState.DONE, 22.dp, color = colors[i])
                    LineBar(usedFraction(section), usageColor(section.spentCents, section.budgetCents), Modifier.width(44.dp), thickness = 4.dp)
                }
            }
        }
    }
}

private fun usedFraction(section: BudgetSection) =
    if (section.budgetCents <= 0) (if (section.spentCents > 0) 1f else 0f) else (section.spentCents.toFloat() / section.budgetCents).coerceIn(0f, 1f)

// ---------- Pagos programados ----------

/** One payment to make: a scheduled one from the plan or a card's coming payment. */
private class PayItem(val cents: Long, val due: LocalDate, val paid: Boolean, val icon: String)

/** What is planned to pay (scheduled and cards), open ones by date, then the ones already done. */
private fun payItems(data: FinanceWidgetData): List<PayItem> {
    val view = FinanceMoney.of(data.accounts, data.plan, data.summary, data.money, data.today)
    return (view.plan + view.later).filter { it.kind != PlanKind.INCOME }
        .map { PayItem(it.cents, it.due, !it.open, if (it.kind == PlanKind.CARD) "banco" else "pago") }
        .sortedWith(compareBy<PayItem> { it.paid }.thenBy { it.due })
}

@Composable
private fun PaymentsWidget(data: FinanceWidgetData, shape: Shape) {
    val all = payItems(data)
    val payments = all.filter { !it.paid }
    if (payments.isEmpty()) return Empty("pago")
    fun urgent(p: PayItem) = !p.due.isAfter(data.today)
    val next = payments.first()
    val nextColor = if (urgent(next)) LauncherDanger else White
    when (shape) {
        Shape.SQUARE -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                StrokeIcon(next.icon, StrokeState.DONE, 24.dp, color = White)
                Spacer(Modifier.weight(1f))
                if (urgent(next)) Box(Modifier.size(8.dp).background(LauncherDanger, CircleShape))
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Amount(next.cents, nextColor, 22.sp)
                Mono(next.due.dayOfMonth.toString(), Muted)
            }
        }
        Shape.WIDE -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
            payments.take(4).forEach { p ->
                val red = urgent(p)
                Row(Modifier.height(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    StrokeIcon(p.icon, StrokeState.DONE, 20.dp, color = if (red) White else Muted)
                    Mono(p.due.dayOfMonth.toString(), if (red) LauncherDanger else Muted, Modifier.width(26.dp))
                    Spacer(Modifier.weight(1f))
                    Amount(p.cents, if (red) LauncherDanger else Color(0xFFE8E8E8), 16.sp)
                }
            }
        }
        Shape.STRIP -> Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            MonthDots(all, data.today, Modifier.weight(1f).height(14.dp))
            StrokeIcon(next.icon, StrokeState.DONE, 20.dp, color = White)
            Amount(next.cents, nextColor, 18.sp)
        }
    }
}

/** The month as a line with a dot on each payment day: paid white, due or late red, still to come gray. */
@Composable
private fun MonthDots(payments: List<PayItem>, today: LocalDate, modifier: Modifier) {
    val length = today.lengthOfMonth()
    Canvas(modifier.semantics { contentDescription = "Pagos del mes" }) {
        val y = size.height / 2
        fun x(day: Int) = (day - 1f) / (length - 1f) * (size.width - 12.dp.toPx()) + 6.dp.toPx()
        drawLine(Stub, Offset(0f, y), Offset(size.width, y), 2.dp.toPx())
        drawLine(Dim, Offset(0f, y), Offset(x(today.dayOfMonth), y), 2.dp.toPx())
        payments.filter { YearMonth.from(it.due) == YearMonth.from(today) }.forEach { p ->
            val day = p.due.dayOfMonth
            val paid = p.paid
            val late = !paid && day <= today.dayOfMonth
            val color = when { paid -> White; late -> LauncherDanger; else -> Dim }
            drawCircle(color, (if (late) 6.dp else 4.dp).toPx(), Offset(x(day), y))
        }
    }
}

// ---------- Meta de ahorro ----------

@Composable
private fun GoalWidget(data: FinanceWidgetData, shape: Shape) {
    val goal = SavingGoal.decode(data.goalText)?.takeIf { it.targetCents > 0 } ?: return Empty("meta")
    val color = goalColor(goal, data.today)
    val fraction = (goal.savedCents.toFloat() / goal.targetCents).coerceIn(0f, 1f)
    val expected = (goal.expectedCents(data.today).toFloat() / goal.targetCents).coerceIn(0f, 1f)
    when (shape) {
        Shape.SQUARE -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Arc(fraction, color, Modifier.size(104.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StrokeIcon("meta", StrokeState.DONE, 18.dp, color = Muted)
                Amount(goal.savedCents, color, 18.sp)
            }
        }
        Shape.WIDE -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StrokeIcon("meta", StrokeState.DONE, 20.dp, color = Muted)
                Amount(goal.savedCents, color, 21.sp)
                Spacer(Modifier.weight(1f))
                Mono(wholePesos(goal.targetCents), Muted)
            }
            LineBar(fraction, color, Modifier.fillMaxWidth(), tick = expected, thickness = 8.dp)
            MonthMarks(goal, data.today, Modifier.fillMaxWidth().height(10.dp))
        }
        Shape.STRIP -> Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            StrokeIcon("meta", StrokeState.DONE, 20.dp, color = Muted)
            LineBar(fraction, color, Modifier.weight(1f), tick = expected)
            Amount(goal.savedCents, color, 20.sp)
        }
    }
}

/** One dot per month of the goal: gone white, this one blue, still to come gray. */
@Composable
private fun MonthMarks(goal: SavingGoal, today: LocalDate, modifier: Modifier) {
    val first = YearMonth.from(goal.start)
    val months = (ChronoUnit.MONTHS.between(first, YearMonth.from(goal.end)) + 1).toInt().coerceIn(1, 36)
    val now = ChronoUnit.MONTHS.between(first, YearMonth.from(today)).toInt()
    Canvas(modifier) {
        val r = 3.dp.toPx()
        val step = if (months > 1) (size.width - 2 * r) / (months - 1) else 0f
        for (i in 0 until months) {
            val color = when { i < now -> White; i == now -> LauncherAccent; else -> Stub }
            drawCircle(color, r, Offset(r + i * step, size.height / 2))
        }
    }
}

// ---------- Bolsa de extras ----------

@Composable
private fun BagWidget(data: FinanceWidgetData, shape: Shape) {
    val s = data.summary
    if (s.bagCents <= 0) return Empty("bolsa")
    val left = s.bagLeftCents
    val color = leftColor(left, s.bagCents)
    val fraction = (left.toFloat() / s.bagCents).coerceIn(0f, 1f)
    when (shape) {
        Shape.SQUARE -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)) {
            Jar(fraction, color, Modifier.size(60.dp))
            Amount(left, color, 20.sp)
        }
        Shape.WIDE -> Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Jar(fraction, color, Modifier.size(58.dp))
                Amount(left, color, 18.sp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                monthExtras(data).take(4).forEach { record ->
                    Row(Modifier.height(22.dp), verticalAlignment = Alignment.CenterVertically) {
                        StrokeIcon(categoryIcon(Budget.categoryOf(record)), StrokeState.DONE, 18.dp, color = Muted)
                        Spacer(Modifier.weight(1f))
                        Mono(wholePesos(record.amountCents), Muted)
                    }
                }
            }
        }
        Shape.STRIP -> Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Jar(fraction, color, Modifier.size(40.dp))
            MonthLine(bagCurve(data), data.today, color, Modifier.weight(1f).height(32.dp), projection = false)
            Amount(left, color, 20.sp)
        }
    }
}

private fun monthExtras(data: FinanceWidgetData) = data.money
    .filter { Budget.isExtra(it) && YearMonth.from(Budget.dateOf(it)) == YearMonth.from(data.today) }
    .sortedByDescending { it.createdAt }

/** What was left in the bag at the end of each day of the month so far, as a share of the bag. */
private fun bagCurve(data: FinanceWidgetData): List<Float> {
    val bag = data.summary.bagCents.coerceAtLeast(1)
    val byDay = monthExtras(data).groupBy { Budget.dateOf(it).dayOfMonth }.mapValues { (_, rows) -> rows.sumOf { it.amountCents } }
    var spent = 0L
    return (1..data.today.dayOfMonth).map { day -> spent += byDay[day] ?: 0; (1f - spent.toFloat() / bag).coerceIn(0f, 1f) }
}

/** A jar in the white line, filled from the bottom with [color] up to [fraction]. */
@Composable
private fun Jar(fraction: Float, color: Color, modifier: Modifier) {
    Canvas(modifier.semantics { contentDescription = "Bolsa" }) {
        val scale = size.minDimension / 48f
        withTransform({ scale(scale, scale, Offset.Zero) }) {
            clipPath(JarBody) {
                val top = 12f + (42f - 12f) * (1f - fraction)
                drawRect(color, Offset(0f, top), Size(48f, 48f - top))
            }
            drawPath(JarLine, White, style = Stroke(1.5f / scale * 1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

private val JarBody: Path = PathParser().parsePathString("M18 12c-4 2-6 6-6 10v16a4 4 0 0 0 4 4h16a4 4 0 0 0 4-4V22c0-4-2-8-6-10z").toPath()
private val JarLine: Path = PathParser().parsePathString("M16 6h16M18 6v6c-4 2-6 6-6 10v16a4 4 0 0 0 4 4h16a4 4 0 0 0 4-4V22c0-4-2-8-6-10V6").toPath()

// ---------- Por cobrar ----------

private class Owed(val cents: Long, val since: LocalDate)

@Composable
private fun OwedWidget(data: FinanceWidgetData, shape: Shape) {
    val owed = (data.receivables.map { Owed(it.amountCents, java.time.Instant.ofEpochMilli(it.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()) } +
        data.summary.sharedOpen.map { Owed(it.pendingFromOthersCents, Budget.dateOf(it)) })
        .filter { it.cents > 0 }
        .sortedByDescending { it.cents }
    if (owed.isEmpty()) return Empty("compartido")
    val total = owed.sumOf { it.cents }
    fun hue(i: Int) = FinanceChartColors[(i + 2) % FinanceChartColors.size]
    when (shape) {
        Shape.SQUARE -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            StrokeIcon("compartido", StrokeState.DONE, 24.dp, color = FinanceTeal)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Amount(total, FinanceTeal, 22.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    owed.take(6).indices.forEach { i -> Box(Modifier.size(10.dp).background(hue(i), CircleShape)) }
                }
            }
        }
        Shape.WIDE -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
            owed.take(4).forEachIndexed { i, item ->
                val weeks = (ChronoUnit.DAYS.between(item.since, data.today) / 7).toInt().coerceIn(0, 4)
                Row(Modifier.height(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    StrokeIcon("compartido", StrokeState.DONE, 20.dp, color = hue(i))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(weeks) { Box(Modifier.size(6.dp).background(if (weeks >= 3) MoneyLow else Dim, CircleShape)) }
                    }
                    Spacer(Modifier.weight(1f))
                    Mono(wholePesos(item.cents), FinanceTeal)
                }
            }
        }
        Shape.STRIP -> Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            StrokeIcon("compartido", StrokeState.DONE, 22.dp, color = FinanceTeal)
            Row(Modifier.weight(1f).height(8.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                owed.take(6).forEachIndexed { i, item ->
                    Box(Modifier.weight(item.cents.toFloat()).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(hue(i)))
                }
            }
            Amount(total, FinanceTeal, 20.sp)
        }
    }
}

// ---------- Shared pieces ----------

private class Bar(val fraction: Float, val color: Color, val today: Boolean)

/** This week, Monday first: each day's extras against its share of the week; days to come are short gray stubs. */
private fun weekBars(data: FinanceWidgetData): List<Bar> {
    val start = Budget.weekStart(data.today)
    val share = (data.summary.weekAllowanceCents / 7).coerceAtLeast(1)
    val spent = (0..6).map { i ->
        val day = start.plusDays(i.toLong())
        data.money.filter { Budget.isExtra(it) && Budget.dateOf(it) == day }.sumOf { it.amountCents }
    }
    val top = maxOf(spent.maxOrNull() ?: 0, share * 3 / 2).toFloat()
    return spent.mapIndexed { i, cents ->
        val day = start.plusDays(i.toLong())
        if (day.isAfter(data.today)) Bar(0f, Stub, false)
        else Bar((cents / top).coerceIn(0.08f, 1f), usageColor(cents, share), day == data.today)
    }
}

/** What is left of the month at the end of each day so far, as a share of the month's money. */
private fun monthCurve(data: FinanceWidgetData, total: Long): List<Float> {
    val month = YearMonth.from(data.today)
    val byDay = data.money.filter { it.kind.isExpense && YearMonth.from(Budget.dateOf(it)) == month }
        .groupBy { Budget.dateOf(it).dayOfMonth }.mapValues { (_, rows) -> rows.sumOf { it.amountCents } }
    var spent = 0L
    return (1..data.today.dayOfMonth).map { day -> spent += byDay[day] ?: 0; (1f - spent.toFloat() / total).coerceIn(0f, 1f) }
}

@Composable
private fun DayBars(bars: List<Bar>, modifier: Modifier, barWidth: Dp) {
    Row(modifier.semantics { contentDescription = "Gasto por día" }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        bars.forEach { bar ->
            Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Box(Modifier.width(barWidth).weight(1f, fill = true), contentAlignment = Alignment.BottomCenter) {
                    Box(Modifier.width(barWidth).fillMaxHeight(maxOf(bar.fraction, 0.08f)).clip(RoundedCornerShape(3.dp)).background(bar.color))
                }
                Spacer(Modifier.height(4.dp))
                Box(Modifier.size(4.dp).background(if (bar.today) White else Color.Transparent, CircleShape))
            }
        }
    }
}

/** The month's balance as a line up to today (white dot), then dashed to the month's end at the same pace. */
@Composable
private fun MonthLine(points: List<Float>, today: LocalDate, color: Color, modifier: Modifier, projection: Boolean = true) {
    val length = today.lengthOfMonth()
    Canvas(modifier.semantics { contentDescription = "Saldo del mes" }) {
        if (points.isEmpty()) return@Canvas
        val pad = 4.dp.toPx()
        fun x(day: Int) = pad + (day - 1f) / (length - 1f) * (size.width - 2 * pad)
        fun y(v: Float) = pad + (1f - v) * (size.height - 2 * pad)
        val path = Path().apply {
            moveTo(x(1), y(points[0]))
            points.forEachIndexed { i, v -> if (i > 0) lineTo(x(i + 1), y(v)) }
        }
        drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        val last = points.size
        val end = Offset(x(last), y(points.last()))
        if (projection && last < length) {
            val pace = (1f - points.last()) / last
            val final = (points.last() - pace * (length - last)).coerceIn(0f, 1f)
            drawLine(Color(0xFF3A3A3A), end, Offset(x(length), y(final)), 2.dp.toPx(), StrokeCap.Round, PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 5.dp.toPx())))
        }
        drawCircle(White, 3.5.dp.toPx(), end)
    }
}

@Composable
private fun Ring(fraction: Float, color: Color, thickness: Dp, modifier: Modifier) {
    Canvas(modifier) {
        val stroke = thickness.toPx()
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(Track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
        if (fraction > 0f) drawArc(color, -90f, 360f * fraction, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

/** A three-quarter arc opening at the bottom, for the saving goal. */
@Composable
private fun Arc(fraction: Float, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val stroke = 7.dp.toPx()
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(Track, 135f, 270f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        if (fraction > 0f) drawArc(color, 135f, 270f * fraction, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

/** Each section takes its share of the budget around the ring, lit as far as it was spent. */
@Composable
private fun Donut(sections: List<BudgetSection>, colors: List<Color>, modifier: Modifier) {
    val total = sections.sumOf { it.budgetCents.coerceAtLeast(0) }.coerceAtLeast(1).toFloat()
    Canvas(modifier.semantics { contentDescription = "Gasto por tema" }) {
        val stroke = 10.dp.toPx()
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val gap = if (sections.size > 1) 3f else 0f
        var start = -90f
        sections.forEachIndexed { i, section ->
            val sweep = 360f * section.budgetCents.coerceAtLeast(0) / total
            if (sweep > gap) {
                drawArc(inactiveFinanceColor(colors[i]).copy(alpha = 0.25f), start, sweep - gap, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                drawArc(colors[i], start, (sweep - gap) * usedFraction(section), false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            }
            start += sweep
        }
    }
}

@Composable
private fun LineBar(fraction: Float, color: Color, modifier: Modifier, tick: Float? = null, thickness: Dp = 6.dp) {
    Canvas(modifier.height(thickness + 10.dp)) {
        val h = thickness.toPx()
        val y = size.height / 2
        drawLine(Track, Offset(h / 2, y), Offset(size.width - h / 2, y), h, StrokeCap.Round)
        val f = fraction.coerceIn(0f, 1f)
        if (f > 0f) drawLine(color, Offset(h / 2, y), Offset(h / 2 + (size.width - h) * f, y), h, StrokeCap.Round)
        tick?.let { t ->
            val x = h / 2 + (size.width - h) * t.coerceIn(0f, 1f)
            drawLine(White, Offset(x, 0f), Offset(x, size.height), 2.dp.toPx(), StrokeCap.Round)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AddButton(onClick: () -> Unit, onLongClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(LauncherAccent)
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick)
            .semantics { contentDescription = "Registrar gasto" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(18.dp)) {
            val s = 2.dp.toPx()
            drawLine(White, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), s, StrokeCap.Round)
            drawLine(White, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), s, StrokeCap.Round)
        }
    }
}

@Composable
private fun Amount(cents: Long, color: Color, size: TextUnit) {
    Text(wholePesos(cents), color = color, style = MaterialTheme.typography.titleLarge.copy(fontSize = size, fontWeight = FontWeight.Medium), maxLines = 1)
}

@Composable
private fun Mono(text: String, color: Color, modifier: Modifier = Modifier, end: Boolean = false) {
    Text(
        text, modifier, color = color, maxLines = 1,
        style = MaterialTheme.typography.labelMedium,
        textAlign = if (end) androidx.compose.ui.text.style.TextAlign.End else null,
    )
}

/** Nothing set up for this part yet: its icon in gray, centered. */
@Composable
private fun Empty(icon: String) {
    Box(Modifier.fillMaxSize().padding(4.dp), contentAlignment = Alignment.Center) {
        StrokeIcon(icon, StrokeState.OFF, 32.dp)
    }
}
