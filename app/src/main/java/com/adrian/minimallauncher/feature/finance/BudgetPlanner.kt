package com.adrian.minimallauncher.feature.finance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.feature.CardFill
import com.adrian.minimallauncher.feature.CardLine
import com.adrian.minimallauncher.feature.LauncherAccent
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
import kotlin.math.roundToInt

private val White = Color(0xFFF5F5F5)
private val Gray = FinanceMuted
private val Faint = FinanceMuted
private val EmptyBar = inactiveFinanceColor(FinanceBlue)
private val CellFill = Color(0xFF161616)
private val FooterFill = Color(0xFF121212)
private val FREQUENCIES = listOf(BudgetPeriod.DAY to "Día", BudgetPeriod.WEEK to "Semana", BudgetPeriod.MONTH to "Mes")
private val TableShape = RoundedCornerShape(20.dp)

/** One fixed cost row of the simulation: what it is, how much, and how often it is paid. */
@Stable
class SimCost(name: String = "", amount: String = "", period: BudgetPeriod = BudgetPeriod.MONTH) {
    var name by mutableStateOf(name)
    var amount by mutableStateOf(amount)
    var period by mutableStateOf(period)
}

/**
 * The board's state: the monthly income, the fixed costs, the saving %. In Presupuesto it is a what-if that lives
 * while Finanzas is open; in Ajustes it starts from the saved plan and is stored with Guardar.
 */
@Stable
class BudgetSim(incomeCents: Long, savingPct: Int, costs: List<PlanCost> = emptyList()) {
    var income by mutableStateOf(if (incomeCents > 0) plainAmount(incomeCents) else "")
    var saving by mutableFloatStateOf(savingPct.toFloat())
    val costs = mutableStateListOf<SimCost>().apply {
        if (costs.isEmpty()) add(SimCost()) else costs.forEach { add(SimCost(it.name, plainAmount(it.cents), it.period)) }
    }

    /** What Guardar stores: rows without an amount are left out. */
    fun plan() = FinancePlan(
        parseCents(income) ?: 0,
        costs.mapNotNull { row -> parseCents(row.amount)?.takeIf { it > 0 }?.let { PlanCost(row.name.trim().ifBlank { "Fijo" }, it, row.period) } },
    )
}

/** Cents as an amount field holds them: "1500" or "1500.5". */
internal fun plainAmount(cents: Long): String = if (cents % 100 == 0L) (cents / 100).toString() else "%d.%02d".format(cents / 100, cents % 100).trimEnd('0')

private fun monthly(cents: Long, period: BudgetPeriod, days: Int) = FinancePlan.perMonth(cents, period, days)

/** Presupuesto: the board with the income typed right in its row. */
@Composable
internal fun BudgetPlanner(sim: BudgetSim, daysInMonth: Int) {
    PlanBoard(sim, daysInMonth) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            StrokeIcon("ingreso", StrokeState.NOW, 24.dp)
            Text("1. Ingreso mensual", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = White)
            IncomeCell(sim)
        }
    }
}

/** The income as a small money field, right-aligned. */
@Composable
internal fun IncomeCell(sim: BudgetSim, modifier: Modifier = Modifier) {
    Cell(
        sim.income, { sim.income = amountInput(it) }, "$0",
        modifier.width(140.dp),
        KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        PesosTransformation, TextAlign.End,
    )
}

/**
 * 1) the income row ([incomeRow]), 2) the fixed costs as one compact table (each row can expand its frequency),
 * 3) the saving slider, and the result.
 */
@Composable
internal fun PlanBoard(sim: BudgetSim, daysInMonth: Int, incomeRow: @Composable () -> Unit) {
    val income = parseCents(sim.income) ?: 0
    val fixed = sim.costs.sumOf { monthly(parseCents(it.amount) ?: 0, it.period, daysInMonth) }
    val free = income - fixed
    val pct = sim.saving.roundToInt()
    val saving = if (free > 0) free * pct / 100 else 0
    val left = free - saving

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        // 1. Income, always monthly.
        incomeRow()

        // 2. Fixed costs.
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                StrokeIcon("fijos", StrokeState.NOW, 24.dp)
                Text("2. Gastos fijos previstos", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = White)
                Text(wholePesos(fixed), style = MaterialTheme.typography.titleSmall, color = FinanceBlue)
            }
            FixedTable(sim, daysInMonth, fixed, income)
        }

        // 3. Saving.
        Column {
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp), Alignment.CenterVertically) {
                StrokeIcon("ahorro", StrokeState.NOW, 24.dp)
                Text("3. Reserva de ahorro", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = White)
                Text("$pct % · ${wholePesos(saving)}", style = MaterialTheme.typography.titleMedium, color = SavingGreen)
            }
            FinanceReadMore("Se calcula sobre el ingreso después de los gastos fijos.")
            Slider(
                value = sim.saving,
                onValueChange = { sim.saving = (it / 5).roundToInt() * 5f },
                valueRange = 0f..60f,
                colors = SliderDefaults.colors(thumbColor = SavingGreen, activeTrackColor = SavingGreen, inactiveTrackColor = inactiveFinanceColor(SavingGreen)),
            )
        }

        // Result.
        Column(
            Modifier.fillMaxWidth().background(CardFill, TableShape).border(1.dp, if (left < 0) MoneyOver else CardLine, TableShape).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Bottom) {
                BigAmount(left, 36.dp, if (left < 0) MoneyOver else White)
                StrokeIcon("bolsa", if (left < 0) StrokeState.NOW else StrokeState.DONE, 28.dp)
            }
            Text("Previsión mensual antes de registrar gastos", color = FinanceMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * All fixed costs in one compact table: each row shows concept, amount, and a frequency disclosure toggle.
 */
@Composable
private fun FixedTable(sim: BudgetSim, daysInMonth: Int, fixed: Long, income: Long) {
    Column(Modifier.fillMaxWidth().border(1.dp, CardLine, TableShape)) {
        sim.costs.forEachIndexed { index, cost ->
            if (index > 0) Divider()
            CostRow(cost, daysInMonth) { sim.costs.remove(cost) }
        }
        Divider()
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button) { sim.costs.add(SimCost()) }
                .semantics(mergeDescendants = true) { contentDescription = "Agregar gasto fijo" }.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StrokeIcon("nueva", StrokeState.NOW, 18.dp)
        }
        // Foot: the total, and a bar of how much of the income it takes (red past all of it).
        val share = if (income > 0) (fixed * 100 / income).toInt() else 0
        val over = income in 1 until fixed
        Column(
            Modifier.fillMaxWidth()
                .background(FooterFill, RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Total al mes", style = MaterialTheme.typography.titleSmall, color = White)
                Text(wholePesos(fixed), style = MaterialTheme.typography.titleMedium, color = if (over) MoneyOver else White)
            }
            if (income > 0) {
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp), Alignment.CenterVertically) {
                    Canvas(Modifier.weight(1f).height(6.dp)) {
                        val r = CornerRadius(size.height / 2)
                        drawRoundRect(EmptyBar, size = size, cornerRadius = r)
                        val w = size.width * (share.coerceAtMost(100) / 100f)
                        if (w > 0f) drawRoundRect(if (over) MoneyOver else FinanceBlue, size = Size(w, size.height), cornerRadius = r)
                    }
                    Text("$share % del ingreso", style = MaterialTheme.typography.labelSmall, color = if (over) MoneyOver else Faint)
                }
            }
        }
    }
}

@Composable
private fun CostRow(cost: SimCost, daysInMonth: Int, onRemove: () -> Unit) {
    var freqExpanded by remember { mutableStateOf(false) }
    val periodLabel = when (cost.period) {
        BudgetPeriod.DAY -> "Día"
        BudgetPeriod.WEEK -> "Sem"
        BudgetPeriod.MONTH -> "Mes"
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Cell(cost.name, { cost.name = it.take(30) }, "Concepto", Modifier.weight(1f), KeyboardOptions(imeAction = ImeAction.Next))
            Cell(
                cost.amount, { cost.amount = amountInput(it) }, "$0",
                Modifier.width(96.dp),
                KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                PesosTransformation, TextAlign.End,
            )
            Text(
                "$periodLabel ▾",
                Modifier
                    .background(CellFill, RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button) { freqExpanded = !freqExpanded }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                style = MaterialTheme.typography.labelMedium,
                color = FinanceTeal,
            )
            IconButton("no", "Quitar", 16.dp, onClick = onRemove)
        }
        if (freqExpanded) {
            Frequency(cost.period, Modifier.fillMaxWidth()) {
                cost.period = it
                freqExpanded = false
            }
            if (cost.period != BudgetPeriod.MONTH) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    SmallLabel("Equivalente al mes")
                    Text(
                        wholePesos(monthly(parseCents(cost.amount) ?: 0, cost.period, daysInMonth)),
                        style = MaterialTheme.typography.labelMedium,
                        color = White,
                    )
                }
            }
        }
    }
}

/** A table cell you can type in: a dark fill, the hint inside. */
@Composable
private fun Cell(
    value: String,
    onChange: (String) -> Unit,
    hint: String,
    modifier: Modifier,
    keyboardOptions: KeyboardOptions,
    transformation: VisualTransformation = VisualTransformation.None,
    align: TextAlign = TextAlign.Start,
) {
    val style = MaterialTheme.typography.bodyMedium.merge(TextStyle(color = White, textAlign = align))
    BasicTextField(
        value, onChange, modifier,
        textStyle = style,
        singleLine = true,
        cursorBrush = SolidColor(White),
        keyboardOptions = keyboardOptions,
        visualTransformation = transformation,
        decorationBox = { inner ->
            Box(Modifier.fillMaxWidth().background(CellFill, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 10.dp)) {
                if (value.isEmpty()) Text(hint, Modifier.fillMaxWidth(), style = style.copy(color = FinanceMuted))
                inner()
            }
        },
    )
}

/** Día · Semana · Mes as one segmented control; the picked one filled white. */
@Composable
internal fun Frequency(selected: BudgetPeriod, modifier: Modifier = Modifier, onPick: (BudgetPeriod) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(modifier.heightIn(min = 48.dp).border(1.dp, Color(0xFF333333), shape).padding(3.dp)) {
        FREQUENCIES.forEach { (period, label) ->
            val on = period == selected
            Box(
                Modifier.weight(1f).heightIn(min = 42.dp)
                    .background(if (on) White else Color.Transparent, RoundedCornerShape(9.dp))
                    .clickable(role = Role.RadioButton) { onPick(period) }
                    .semantics { contentDescription = label },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = if (on) Color.Black else Faint, maxLines = 1)
            }
        }
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(CardLine))
}
