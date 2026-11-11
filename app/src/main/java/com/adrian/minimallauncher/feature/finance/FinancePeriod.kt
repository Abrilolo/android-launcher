package com.adrian.minimallauncher.feature.finance

import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.FinanceRecord
import com.adrian.minimallauncher.data.MoneyKind
import com.adrian.minimallauncher.feature.CardFill
import com.adrian.minimallauncher.feature.CardLine
import com.adrian.minimallauncher.feature.ChartColumn
import com.adrian.minimallauncher.feature.ChartRow
import com.adrian.minimallauncher.feature.ChartStack
import com.adrian.minimallauncher.feature.LauncherAccent
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.feature.StackChart
import com.adrian.minimallauncher.feature.bank.BankAccount
import com.adrian.minimallauncher.feature.bank.BankKind
import com.adrian.minimallauncher.feature.shortDay
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val spanish = Locale.forLanguageTag("es")
private val White = Color(0xFFF5F5F5)
private val Muted = FinanceMuted
private val Future = inactiveFinanceColor(FinanceBlue)
private const val INITIALS = "LMMJVSD"

/** The days a card closes its statement or is paid, marked on the calendar. */
internal data class DayMarks(val cut: Boolean = false, val pay: Boolean = false)

private val spentPeriodKinds = setOf(MoneyKind.EXTRA, MoneyKind.SHARED, MoneyKind.FIXED, MoneyKind.MONTHLY)

/**
 * Resumen con divulgación progresiva: calendario y cifras principales a primera vista;
 * evolución diaria, desglose por tipo y movimientos del período en secciones colapsables.
 */
@Composable
internal fun PeriodView(
    summary: FinanceSummary,
    money: List<FinanceRecord>,
    accounts: List<BankAccount>,
    range: String,
    onRange: (String) -> Unit,
    anchor: LocalDate,
    onAnchor: (LocalDate) -> Unit,
    rangeFrom: LocalDate?,
    rangeTo: LocalDate?,
    onRangeDays: (LocalDate?, LocalDate?) -> Unit,
) {
    val today = summary.date
    val week = range == "semana"
    val ranged = range == "rango"
    var picked by rememberSaveable(range, anchor.toString()) { mutableStateOf<String?>(null) }
    var breakdownExpanded by rememberSaveable { mutableStateOf(false) }

    val (start, end) = when (range) {
        "semana" -> Budget.weekStart(anchor).let { it to it.plusDays(6) }
        "mes" -> anchor.withDayOfMonth(1).let { it to it.withDayOfMonth(it.lengthOfMonth()) }
        else -> (rangeFrom ?: anchor).let { a -> val b = rangeTo ?: a; minOf(a, b) to maxOf(a, b) }
    }
    val monthDays = remember(anchor) { YearMonth.from(anchor).let { m -> (1..m.lengthOfMonth()).map { m.atDay(it) } } }
    val days = remember(start, end) { generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.toList() }
    val inRange = remember(money, start, end) { money.filter { val d = Budget.dateOf(it); !d.isBefore(start) && !d.isAfter(end) } }
    val extras = remember(inRange) { inRange.filter(Budget::isExtra) }
    val spent = inRange.filter { it.kind in spentPeriodKinds }.sumOf { it.amountCents }
    val came = inRange.filter { it.kind.isIncome }.sumOf { it.amountCents }
    val current = today in start..end
    val dayShare = summary.weekAllowanceCents / 7
    val spentByDay = remember(extras, days) { days.associateWith { d -> extras.filter { Budget.dateOf(it) == d }.sumOf { it.amountCents } } }
    val pickedDay = picked?.takeIf { !ranged }?.let(LocalDate::parse)
    val left = if (!current) came - spent else if (week) summary.weekLeftCents else summary.bagLeftCents
    val cutDays = accounts.filter { it.kind == BankKind.CARD }.map { it.cutDay }.filter { it > 0 }.toSet()
    val payDays = accounts.filter { it.kind == BankKind.CARD }.map { it.payDay }.filter { it > 0 }.toSet()
    fun marksOf(day: LocalDate) = DayMarks(cut = day.dayOfMonth in cutDays, pay = day.dayOfMonth in payDays)
    val step = { n: Long -> onAnchor(if (week) anchor.plusWeeks(n) else anchor.plusMonths(n)) }

    Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
        FinanceRangeSwitch(listOf("semana" to "Semana", "mes" to "Mes", "rango" to "Rango"), range, onRange)

        // 1. Días arriba, con el nombre del período: deslizar a los lados cambia de semana o mes;
        // tocar un día lo abre. En rango, aquí mismo se eligen los días.
        val pick = { day: LocalDate -> picked = if (picked == day.toString()) null else day.toString() }
        val pickRange = { day: LocalDate ->
            val from = rangeFrom
            when {
                from == null || rangeTo != null -> onRangeDays(day, null)
                day.isBefore(from) -> onRangeDays(day, from)
                else -> onRangeDays(from, day)
            }
        }
        val latestStep by rememberUpdatedState(step)
        FinanceSection(
            Modifier.pointerInput(range) {
                var drag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { drag = 0f },
                    onDragEnd = {
                        if (drag > 120f) latestStep(-1) else if (drag < -120f) latestStep(1)
                        drag = 0f
                    },
                    onHorizontalDrag = { change, amount -> drag += amount; change.consume() },
                )
            },
        ) {
            PeriodHeader(range, start, end, onBack = { step(-1) }, onForward = { step(1) }, onToday = { onAnchor(today) })
            when {
                week -> Row(Modifier.fillMaxWidth()) {
                    days.forEachIndexed { i, day -> DayChip(day, INITIALS[i], today, pickedDay, spentByDay[day] ?: 0, dayShare, marksOf(day), false, compact = pickedDay == null) { pick(day) } }
                }
                ranged -> {
                    MonthGrid(monthDays, today, null, rangeFrom?.let { start..end }, spentByDay, dayShare, ::marksOf, pickRange, allowFuture = true, compact = pickedDay == null)
                    MarkLegend(marksIn(days, accounts))
                }
                else -> MonthGrid(monthDays, today, pickedDay, null, spentByDay, dayShare, ::marksOf, pick, compact = pickedDay == null)
            }
        }

        // 2. Lo gastado en grande, con el color de cómo va, y lo que queda.
        // Lo gastado es real; no hay una bolsa de la que se reste (los topes son guías aparte, 2026-11-11).
        val budget = 0L
        val shownSpent = if (pickedDay != null) inRange.filter { Budget.dateOf(it) == pickedDay && it.kind.isExpense }.sumOf { it.amountCents } else spent
        val heroColor = if (budget > 0) usageColor(spent, budget) else FinanceWhite
        FinanceSection {
        Column(
            Modifier.fillMaxWidth()
                .drawBehind { drawRect(heroColor, size = Size(3.dp.toPx(), size.height)) }
                .padding(start = 17.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                when {
                    pickedDay != null -> shortDay(pickedDay)
                    week -> "Gastado esta semana"
                    range == "mes" -> "Gastado este mes"
                    else -> "Gastado"
                },
                style = MaterialTheme.typography.labelLarge, color = FinanceMuted,
            )
            FinanceAmount(shownSpent, size = 40, color = heroColor)
        }
        }

        // Un día elegido se ve como Hoy con los datos de ese día.
        if (pickedDay != null) {
            CardDayPanel(pickedDay, accounts, money) { picked = null }
            DayBudget(summary, money, pickedDay)
            return@Column
        }

        // 3. Gráficas, una a la vez: se cambian deslizando.
        FinanceCard {
        FinanceChartsPager(
            spent = spent,
            groups = Budget.breakdown(inRange).filter { it.kind in spentPeriodKinds && it.cents > 0 },
            records = inRange.filter { it.kind in spentPeriodKinds },
            days = days,
            spentByDay = spentByDay,
            allowance = if (days.size <= 10) dayShare else dayShare * 7,
        )
        }

        // 4. Movimientos: deslizar a la derecha elimina, a la izquierda edita.
        FinanceSection(spacing = 4.dp) {
            SectionLabel("Movimientos")
            Movements(inRange, limit = 5)
        }

        // 5. El detalle por tipo, cerrado hasta que se toca.
        if (!ranged) FinanceSection(spacing = 0.dp) {
            FinanceCollapsible(
                title = "Gastos por tipo",
                showRule = false,
                summaryText = wholePesos(spent),
                summaryColor = LauncherAccent,
                expanded = breakdownExpanded,
                onToggle = { breakdownExpanded = !breakdownExpanded },
            ) {
                ExpenseBreakdown(inRange)
            }
        }
    }
}

/** The period's name with arrows to the ones before and after; the name goes back to today. */
@Composable
private fun PeriodHeader(range: String, start: LocalDate, end: LocalDate, onBack: () -> Unit, onForward: () -> Unit, onToday: () -> Unit) {
    val title = if (range == "mes") start.month.getDisplayName(TextStyle.FULL, spanish).replaceFirstChar { it.uppercase(spanish) } + " ${start.year}"
    else "${start.dayOfMonth} ${start.month.getDisplayName(TextStyle.SHORT, spanish)} – ${end.dayOfMonth} ${end.month.getDisplayName(TextStyle.SHORT, spanish)}"
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text("‹", Modifier.clickable(role = Role.Button, onClick = onBack).padding(horizontal = 14.dp, vertical = 4.dp), style = MaterialTheme.typography.titleLarge, color = White)
        Text(
            title,
            Modifier.clickable(role = Role.Button, onClick = onToday).padding(vertical = 4.dp),
            style = MaterialTheme.typography.titleMedium,
            color = White,
        )
        Text("›", Modifier.clickable(role = Role.Button, onClick = onForward).padding(horizontal = 14.dp, vertical = 4.dp), style = MaterialTheme.typography.titleLarge, color = White)
    }
}

/** Tapping a day with a card's cut or payment: a quick panel with that card's numbers. */
@Composable
internal fun CardDayPanel(day: LocalDate, accounts: List<BankAccount>, money: List<FinanceRecord>, onClose: () -> Unit) {
    val cards = accounts.filter { it.kind == BankKind.CARD && (it.cutDay == day.dayOfMonth || it.payDay == day.dayOfMonth) }
    if (cards.isEmpty()) return
    val month = YearMonth.from(day)
    Column(
        Modifier.fillMaxWidth().background(CardFill, RoundedCornerShape(16.dp)).border(1.dp, CardLine, RoundedCornerShape(16.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            SmallLabel(shortDay(day).uppercase())
            Box(Modifier.clickable(role = Role.Button, onClick = onClose).semantics { contentDescription = "Cerrar" }.padding(6.dp)) {
                StrokeIcon("listo", StrokeState.OFF, 18.dp)
            }
        }
        cards.forEach { card ->
            val spent = money.filter { it.account == card.id && it.kind in spentPeriodKinds && YearMonth.from(Budget.dateOf(it)) == month }.sumOf { it.amountCents }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text(card.name, style = MaterialTheme.typography.labelLarge, color = White)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (card.cutDay == day.dayOfMonth) StrokeIcon("mes", StrokeState.NOW, 18.dp)
                        if (card.payDay == day.dayOfMonth) StrokeIcon("pago", StrokeState.NOW, 18.dp)
                    }
                }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    SmallLabel("Debes")
                    Text(wholePesos(card.balanceCents), style = MaterialTheme.typography.labelLarge, color = FinanceLavender)
                }
                if (card.owedByOthersCents > 0) Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    SmallLabel("Te deben")
                    Text(wholePesos(card.owedByOthersCents), style = MaterialTheme.typography.labelLarge, color = MoneyPlenty)
                }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    SmallLabel("Gastado este mes")
                    Text(wholePesos(spent), style = MaterialTheme.typography.labelLarge, color = White)
                }
            }
        }
    }
}

/** The calendar's marks in words: which card closes or is paid on each day. */
@Composable
internal fun MarkLegend(entries: List<Pair<LocalDate, String>>) {
    if (entries.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        entries.forEach { (day, text) -> SmallLabel("${shortDay(day)} · $text") }
    }
}

/** Which card closes (corte) or is paid (pago) on each of [days]. */
internal fun marksIn(days: List<LocalDate>, accounts: List<BankAccount>): List<Pair<LocalDate, String>> = days.flatMap { day ->
    accounts.filter { it.kind == BankKind.CARD }.flatMap { card ->
        listOfNotNull(
            if (card.cutDay == day.dayOfMonth) day to "corte · ${card.name}" else null,
            if (card.payDay == day.dayOfMonth) day to "pago · ${card.name}" else null,
        )
    }
}

/** The month as a calendar: the initials on top, then the weeks. [band] lights the days of a range. */
@Composable
private fun MonthGrid(
    days: List<LocalDate>,
    today: LocalDate,
    picked: LocalDate?,
    band: ClosedRange<LocalDate>?,
    spent: Map<LocalDate, Long>,
    dayShare: Long,
    marks: (LocalDate) -> DayMarks,
    onPick: (LocalDate) -> Unit,
    allowFuture: Boolean = false,
    compact: Boolean = false,
) {
    val lead = days.first().dayOfWeek.value - 1
    val cells: List<LocalDate?> = List(lead) { null } + days
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth()) {
            INITIALS.forEach { c -> Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { SmallLabel(c.toString()) } }
        }
        cells.chunked(7).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { day ->
                    if (day == null) Spacer(Modifier.weight(1f))
                    else DayChip(day, null, today, picked, spent[day] ?: 0, dayShare, marks(day), band?.contains(day) == true, allowFuture, compact) { onPick(day) }
                }
                repeat(7 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * One day: its initial (week strip only) over the number in a circle. Today has a red ring, the picked day is filled
 * white, days in a range are dim. Under it: a dot if something was spent (white, red past the day's share), a ring for
 * a card's cut day and a yellow dot for a payment day.
 */
@Composable
private fun RowScope.DayChip(
    day: LocalDate,
    initial: Char?,
    today: LocalDate,
    picked: LocalDate?,
    spent: Long,
    dayShare: Long,
    marks: DayMarks,
    inBand: Boolean,
    allowFuture: Boolean = false,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    val circle = if (compact) 28.dp else 34.dp
    val isPicked = day == picked
    val future = day.isAfter(today)
    Column(
        Modifier.weight(1f).clickable(enabled = allowFuture || !future, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "${day.dayOfWeek.getDisplayName(TextStyle.FULL, spanish)} ${day.dayOfMonth}: ${wholePesos(spent)}"
            }.padding(vertical = if (compact) 2.dp else 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp),
    ) {
        initial?.let { SmallLabel(it.toString(), color = if (isPicked || day == today) White else Muted) }
        Box(
            Modifier.size(circle)
                .background(
                    when {
                        isPicked -> White
                        inBand -> Color(0xFF2A2A2A)
                        else -> Color.Transparent
                    },
                    CircleShape,
                )
                .border(1.dp, if (day == today && !isPicked) LauncherAccent else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                day.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = when {
                    isPicked -> Color.Black
                    future -> Future
                    else -> White
                },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(5.dp).background(
                    if (spent <= 0) Color.Transparent else if (dayShare > 0) usageColor(spent, dayShare) else White,
                    CircleShape,
                ),
            )
            if (marks.cut) Box(Modifier.size(6.dp).border(1.dp, White, CircleShape))
            if (marks.pay) Box(Modifier.size(5.dp).background(MoneyGoing, CircleShape))
        }
    }
}

/** Cool category stacks per day, with a dusty rose extension for spending over the day's share. */
private fun dayColumns(extras: List<FinanceRecord>, days: List<LocalDate>, dayShare: Long): List<ChartColumn> {
    val dated = extras.map { it to Budget.dateOf(it) }
    val categories = (BASE_CATEGORIES + extras.map { it.category.lowercase(spanish) }).distinct()
    return days.map { day ->
        val rows = dated.filter { it.second == day }.map { it.first }
        val parts = categories.map { category ->
            Triple(category, financeCategoryColor(category), rows.filter { it.category.lowercase(spanish) == category }.sumOf { it.amountCents })
        }.filter { it.third > 0 }
        val total = rows.sumOf { it.amountCents }
        val over = if (dayShare > 0) (total - dayShare).coerceAtLeast(0) else 0
        ChartColumn(
            shortDay(day), shortDay(day), wholePesos(total),
            parts.map { ChartStack(it.third.toFloat(), it.second) }, over.toFloat(),
            parts.map { ChartRow(it.second, it.first.replaceFirstChar { c -> c.uppercase(spanish) }, wholePesos(it.third)) } +
                if (over > 0) listOf(ChartRow(MoneyOver, "De más", wholePesos(over))) else emptyList(),
        )
    }
}
