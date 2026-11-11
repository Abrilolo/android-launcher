package com.adrian.minimallauncher.feature.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.FinanceRecord
import com.adrian.minimallauncher.feature.LauncherAccent
import com.adrian.minimallauncher.feature.LauncherDanger
import com.adrian.minimallauncher.feature.shortDay
import com.adrian.minimallauncher.ui.components.ColumnChart
import java.time.LocalDate

private val WeekInitials = listOf("L", "M", "X", "J", "V", "S", "D")
private val spanish = java.util.Locale.forLanguageTag("es")

/** What each kind of money is tinted with across Finanzas: spent blue, to pay amber, coming in green. */
internal val SpentTint = TextBlue
internal val ToPayTint = MoneyLow
internal val IncomeTint = MoneyPlenty

/**
 * Hoy (concept "Finanzas: planeado vs. real", 2026-11-11), in reading order:
 * 1. Dinero disponible: what is in the accounts less what is still to pay this month. A tap shows the sum.
 * 2. Por pagar (amber, dashed: planned) and Gastado hoy (blue: real); each opens its list.
 * 3. Viene: what is planned, as cards to swipe; a tap gives its date and quick actions.
 * 4. Topes and the week, two pages to swipe.
 */
@Composable
internal fun FinanceHoy(
    view: MoneyView,
    money: List<FinanceRecord>,
    onMarkDone: (PlanItem) -> Unit,
    onOpenBank: () -> Unit,
    onOpenPlan: () -> Unit,
) {
    val today = view.today
    Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
        // 1. Dinero disponible
        val available = view.availableCents
        val color = availableColor(view)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Dinero disponible")
                InfoIcon("Lo que tienes en tus cuentas menos lo que te toca pagar este mes (tarjetas y pagos programados). Lo planeado no cuenta como gastado hasta que pasa.")
            }
            Pop(description = "Ver cómo se calcula", pop = {
                PopRow("En cuentas", short(view.haveCents))
                PopRow("Por pagar este mes", "-" + short(view.toPayCents), ToPayTint)
                PopRule()
                PopRow("Disponible", short(available), color)
                PopRow("Al día · ${view.daysLeft} días", short(view.perDayCents))
            }) {
                Column {
                    FinanceAmount(available, Modifier.fillMaxWidth(), size = 54, color = color)
                    Text("${short(view.perDayCents)} al día", style = MaterialTheme.typography.titleMedium, color = FinanceWhite)
                }
            }
        }

        // 2. Por pagar · Gastado hoy
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Pop(Modifier.weight(1f), "Ver lo que falta pagar", pop = {
                if (view.toPay.isEmpty()) PopRow("Nada pendiente", "")
                view.toPay.forEach { item -> PopRow("${item.name} · ${shortDay(item.due)}", short(item.cents), dot = stateColor(item.state)) }
            }) {
                Figure("Por pagar", view.toPayCents, ToPayTint, dashed = true)
            }
            val todayRecords = money.filter { it.kind.isExpense && Budget.dateOf(it) == today }
            Pop(Modifier.weight(1f), "Ver lo gastado hoy", pop = {
                if (todayRecords.isEmpty()) PopRow("Nada hoy", "")
                todayRecords.take(6).forEach { r -> PopRow(r.description.ifBlank { Budget.categoryOf(r).replaceFirstChar { it.uppercase() } }, short(r.amountCents), dot = financeCategoryColor(Budget.categoryOf(r))) }
            }) {
                Figure("Gastado hoy", view.spentTodayCents, SpentTint)
            }
        }

        // 3. Viene: swipe sideways through what is planned.
        val coming = view.plan.filter { it.open } + view.later + view.plan.filter { !it.open }
        if (coming.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionHead("Viene", "Lo que tiene fecha pero todavía no pasa. Se marca hecho cuando llega el cobro o cuando tú lo marcas.") {
                FinIcon("lapiz", "Editar lo planeado", onClick = onOpenPlan)
            }
            val list = rememberLazyListState()
            LazyRow(state = list, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(coming, key = { _, it -> it.key }) { i, item ->
                    Box(Modifier.edgeBlur(list, i)) { PlanCard(item, today, onMarkDone, onOpenBank) }
                }
            }
        }

        // 4. Topes and the week, to swipe.
        val over = view.topesMonthCents > available && view.topes.isNotEmpty()
        SwipePages(
            titles = listOf("Topes", "Semana"),
            info = listOf(
                "Hasta cuánto quieres gastar en cada cosa. Es una guía: no se aparta de tu dinero." +
                    if (over) " Tus topes del mes suman ${short(view.topesMonthCents)} y tienes ${short(available)} disponible." else "",
                null,
            ),
        ) { page ->
            when (page) {
                0 -> Topes(view, onOpenPlan)
                else -> Week(money, today)
            }
        }
    }
}

/** Disponible's color: green with room, blue in use, amber when little is left, pink below zero. */
internal fun availableColor(view: MoneyView): Color =
    if (view.availableCents < 0) LauncherDanger else leftColor(view.availableCents, view.haveCents.coerceAtLeast(1))

/** A figure in a card tinted by what it is: its name in that color, the amount in white. */
@Composable
internal fun Figure(label: String, cents: Long, tint: Color, dashed: Boolean = false, amountColor: Color = FinanceWhite, bar: Pair<Float, Color>? = null) {
    Column(
        Modifier.fillMaxWidth().tinted(tint, dashed = dashed).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = tint)
        FinanceAmount(cents, Modifier.fillMaxWidth(), size = 22, color = amountColor)
        // How full something is (a card's limit), in its state color.
        bar?.let { (fraction, color) ->
            Box(Modifier.fillMaxWidth().height(4.dp).background(GlassFill, RoundedCornerShape(2.dp))) {
                Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().background(color, RoundedCornerShape(2.dp)))
            }
        }
    }
}

internal fun stateColor(state: PlanState): Color = when (state) {
    PlanState.PLANNED -> FinanceWhite.copy(alpha = 0.6f)
    PlanState.TODAY -> LauncherAccent
    PlanState.DONE -> MoneyPlenty
    PlanState.LATE -> LauncherDanger
}

/** The tint of a planned thing by what it is: a card to pay amber, income green, a scheduled payment white. */
internal fun kindTint(kind: PlanKind): Color = when (kind) {
    PlanKind.CARD -> ToPayTint
    PlanKind.INCOME -> IncomeTint
    PlanKind.SCHEDULED -> FinanceWhite
}

/** One planned thing as a card: its state, date, name and amount. A tap gives what can be done with it. */
@Composable
private fun PlanCard(item: PlanItem, today: LocalDate, onMarkDone: (PlanItem) -> Unit, onOpenBank: () -> Unit) {
    val days = java.time.temporal.ChronoUnit.DAYS.between(today, item.due)
    val tint = when (item.state) {
        PlanState.TODAY -> LauncherAccent
        PlanState.LATE -> LauncherDanger
        else -> kindTint(item.kind)
    }
    Pop(description = item.name, pop = { close ->
        PopRow(item.name, (if (item.kind == PlanKind.INCOME) "+" else "") + short(item.cents), dot = tint)
        PopRow(
            when {
                item.state == PlanState.DONE -> "Hecho"
                days == 0L -> "Hoy"
                days == 1L -> "Mañana"
                days > 1 -> "En $days días · ${shortDay(item.due)}"
                else -> "Atrasado · ${shortDay(item.due)}"
            },
            "",
            dot = stateColor(item.state),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (item.kind == PlanKind.SCHEDULED && item.open) PopAction("listo", "Marcar hecho", ActionColor.Done) { close(); onMarkDone(item) }
            if (item.kind == PlanKind.CARD) PopAction("banco", "Abrir en Banco") { close(); onOpenBank() }
        }
    }) {
        Column(
            Modifier.width(150.dp).heightIn(min = 104.dp).alpha(if (item.open) 1f else 0.5f)
                .tinted(tint, dashed = item.state == PlanState.PLANNED).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StateDot(item.state)
                Text(shortDay(item.due), style = MaterialTheme.typography.labelLarge, color = if (item.state == PlanState.LATE) LauncherDanger else LabelCream)
            }
            Text(item.name, style = MaterialTheme.typography.bodyMedium, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text((if (item.kind == PlanKind.INCOME) "+" else "") + short(item.cents), style = MaterialTheme.typography.titleMedium, color = FinanceWhite)
        }
    }
}

/** Each cap: its dot, name and a bar; what is left on the right. A tap gives the detail. */
@Composable
private fun Topes(view: MoneyView, onOpenPlan: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (view.topes.isEmpty()) {
            Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Sin topes", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = FinanceWhite)
                FinIcon("nueva", "Agregar tope", ActionColor.Add, onClick = onOpenPlan)
            }
        }
        view.topes.forEach { tope ->
            val dot = financeCategoryColor(tope.topic)
            val used = if (tope.limitCents <= 0) 1f else (tope.spentCents.toFloat() / tope.limitCents).coerceIn(0f, 1f)
            val state = usageColor(tope.spentCents, tope.limitCents)
            val name = tope.topic.replaceFirstChar { it.uppercase() }
            Pop(Modifier.fillMaxWidth(), name, pop = {
                PopRow(name, periodName(tope.period), dot = dot)
                PopRow("Gastado", short(tope.spentCents), SpentTint)
                PopRow("Tope", short(tope.limitCents))
                PopRule()
                PopRow(if (tope.leftCents >= 0) "Quedan" else "Te pasaste", short(kotlin.math.abs(tope.leftCents)), state)
            }) {
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Dot(dot)
                        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = FinanceWhite)
                        Text(short(tope.leftCents), style = MaterialTheme.typography.bodyMedium, color = state)
                    }
                    Box(Modifier.fillMaxWidth().height(6.dp).background(GlassFill, RoundedCornerShape(3.dp))) {
                        Box(Modifier.fillMaxWidth(used).fillMaxHeight().background(state, RoundedCornerShape(3.dp)))
                    }
                }
            }
        }
    }
}

internal fun periodName(period: BudgetPeriod) = when (period) {
    BudgetPeriod.DAY -> "Al día"
    BudgetPeriod.WEEK -> "A la semana"
    BudgetPeriod.MONTH -> "Al mes"
}

/** The week's spending, centered on today, with its interactive columns; faint bars while there is nothing yet. */
@Composable
private fun Week(money: List<FinanceRecord>, today: LocalDate) {
    val week = (-3..3).map { today.plusDays(it.toLong()) }
    val daily = week.map { d -> money.filter { it.kind.isExpense && Budget.dateOf(it) == d }.sumOf { it.amountCents }.toFloat() }
    var selected by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(3) }
    Column(Modifier.fillMaxWidth().glass().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (daily.all { it == 0f }) {
            EmptyChart("Sin gastos esta semana", height = 110.dp)
            return@Column
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                week[selected].dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, spanish).replaceFirstChar { it.uppercase(spanish) },
                Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = LabelCream,
            )
            Text(short(daily[selected].toLong()), style = MaterialTheme.typography.titleMedium, color = SpentTint)
        }
        ColumnChart(daily, week.map { WeekInitials[it.dayOfWeek.value - 1] }, height = 100.dp, selected = selected, onSelect = { selected = it })
    }
}

/** Resumen's top: what happened this month (blue, real) and what is still to come (amber, planned). */
@Composable
internal fun PastAndComing(view: MoneyView, money: List<FinanceRecord>) {
    val month = java.time.YearMonth.from(view.today)
    val past = money.filter { it.kind.isExpense && java.time.YearMonth.from(Budget.dateOf(it)) == month }.sortedByDescending { it.createdAt }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Pop(Modifier.weight(1f), "Ver lo que pasó", pop = {
            if (past.isEmpty()) PopRow("Nada este mes", "")
            past.groupBy { Budget.categoryOf(it).ifBlank { "otros" } }.mapValues { (_, rows) -> rows.sumOf { it.amountCents } }
                .entries.sortedByDescending { it.value }.take(6)
                .forEach { (topic, cents) -> PopRow(topic.replaceFirstChar { it.uppercase() }, short(cents), dot = financeCategoryColor(topic)) }
        }) { Figure("Pasó", view.spentMonthCents, SpentTint) }
        Pop(Modifier.weight(1f), "Ver lo que viene", pop = {
            if (view.toPay.isEmpty()) PopRow("Nada pendiente", "")
            view.toPay.forEach { item -> PopRow("${item.name} · ${shortDay(item.due)}", short(item.cents), dot = stateColor(item.state)) }
        }) { Figure("Viene", view.toPayCents, ToPayTint, dashed = true) }
    }
}
