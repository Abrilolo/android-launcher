package com.adrian.minimallauncher.feature.finance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.FinanceRecord
import com.adrian.minimallauncher.data.MoneyKind
import java.time.LocalDate

/** One projection for the daily page and widget; current balances never replace budget allocations. */
data class DailyFinance(val spentCents: Long, val remainingCents: Long, val latest: FinanceRecord?)
fun dailyFinance(summary: FinanceSummary, records: List<FinanceRecord>, date: LocalDate): DailyFinance {
    val expenses = records.filter { it.kind.isExpense }
    val sections = BudgetSections.of(summary, records, Span.DAY, date)
        .filter { it.kind == SectionKind.FREE || it.kind == SectionKind.TOPIC }
    return DailyFinance(expenses.filter { Budget.dateOf(it) == date }.sumOf { it.amountCents },
        sections.sumOf { it.leftCents }, expenses.maxByOrNull { it.createdAt })
}

@Composable
internal fun DailyMoneyOverview(summary: FinanceSummary, records: List<FinanceRecord>, date: LocalDate) {
    val day = dailyFinance(summary, records, date)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Pagos de hoy", color = FinanceMuted, style = MaterialTheme.typography.bodySmall)
                FinanceAmount(day.spentCents, size = 30, color = FinanceBlue)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (day.remainingCents < 0) "Exceso de hoy" else "Te queda hoy", color = FinanceMuted, style = MaterialTheme.typography.bodySmall)
                FinanceAmount(kotlin.math.abs(day.remainingCents), size = 30, color = if (day.remainingCents < 0) MoneyOver else FinanceTeal)
            }
        }
        FinanceReadMore("Lo disponible corresponde a Libre y a tus presupuestos por concepto. Los cargos fijos se muestran aparte.")
    }
}

@Composable
internal fun SavingGoalCard(text: String, date: LocalDate, onOpen: () -> Unit) {
    val goal = remember(text) { SavingGoal.decode(text) }
    val fraction = goal?.let { if (it.targetCents > 0) (it.savedCents.toFloat() / it.targetCents).coerceIn(0f, 1f) else 0f } ?: 0f
    Row(
        Modifier
            .fillMaxWidth()
            .background(SavingGreen.copy(alpha = 0.09f), RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(40.dp)) {
            val width = 4.5.dp.toPx()
            drawCircle(inactiveFinanceColor(SavingGreen), radius = size.minDimension / 2 - width / 2, style = Stroke(width))
            if (fraction > 0) drawArc(
                SavingGreen, -90f, fraction * 360f, false,
                topLeft = androidx.compose.ui.geometry.Offset(width / 2, width / 2),
                size = androidx.compose.ui.geometry.Size(size.width - width, size.height - width), style = Stroke(width),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Meta de ahorro", color = SavingGreen, style = MaterialTheme.typography.titleSmall)
            Text(
                goal?.let { "${wholePesos(it.savedCents)} de ${wholePesos(it.targetCents)}" } ?: "Crear una meta",
                color = FinanceWhite,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (goal != null) {
            Text(
                if (goal.reached) "Cumplida" else "${wholePesos(goal.perMonthCents(date))}/mes",
                color = FinanceMuted,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Text("›", color = SavingGreen)
    }
}

internal fun expenseName(kind: MoneyKind): String = when (kind) {
    MoneyKind.FIXED -> "Cargos fijos"
    MoneyKind.SHARED -> "Compartidos"
    MoneyKind.MONTHLY -> "Pagos del mes"
    else -> "Por concepto"
}
internal fun expenseColor(kind: MoneyKind): Color = when (kind) {
    MoneyKind.FIXED -> FinanceLavender
    MoneyKind.SHARED -> FinanceTeal
    MoneyKind.MONTHLY -> FinanceMist
    else -> FinanceBlue
}

/**
 * Gastos por tipo como Acordeón (Accordion): cada tipo muestra su total y, al tocarlo,
 * despliega hacia abajo sus conceptos sin recargar la vista.
 */
@Composable
internal fun ExpenseBreakdown(records: List<FinanceRecord>) {
    var expandedKind by rememberSaveable { mutableStateOf<String?>(null) }
    val kinds = listOf(MoneyKind.EXTRA, MoneyKind.FIXED, MoneyKind.SHARED, MoneyKind.MONTHLY)
    val groups = Budget.breakdown(records)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        kinds.forEach { item ->
            val group = groups.firstOrNull { it.kind == item }
            val cents = group?.cents ?: 0L
            val parts = group?.parts.orEmpty()
            val color = expenseColor(item)
            val isExpanded = expandedKind == item.name
            FinanceCollapsible(
                title = expenseName(item),
                subtitle = if (parts.isEmpty()) "Sin movimientos" else "${parts.size} concepto(s)",
                summaryText = wholePesos(cents),
                summaryColor = if (cents > 0) color else color.copy(alpha = 0.65f),
                expanded = isExpanded,
                onToggle = { expandedKind = if (isExpanded) null else item.name },
            ) {
                if (parts.isEmpty()) {
                    Text("Sin movimientos en este tipo.", style = MaterialTheme.typography.bodySmall, color = FinanceMuted)
                } else {
                    parts.forEachIndexed { i, (partName, partCents) ->
                        val partColor = FinanceChartColors[(i + 1) % FinanceChartColors.size]
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(Modifier.size(7.dp).background(partColor, CircleShape))
                            Text(
                                partName.replaceFirstChar { it.uppercase() },
                                Modifier.weight(1f),
                                color = FinanceWhite,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(wholePesos(partCents), color = partColor, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}
