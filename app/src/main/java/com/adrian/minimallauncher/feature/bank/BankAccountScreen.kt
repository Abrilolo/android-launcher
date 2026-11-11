package com.adrian.minimallauncher.feature.bank

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.FinanceRecord
import com.adrian.minimallauncher.feature.LauncherAccent
import com.adrian.minimallauncher.feature.LauncherDanger
import com.adrian.minimallauncher.feature.finance.Budget
import com.adrian.minimallauncher.feature.finance.ChartPalette
import com.adrian.minimallauncher.feature.finance.FinanceAmount
import com.adrian.minimallauncher.feature.finance.FinanceCard
import com.adrian.minimallauncher.feature.finance.FinanceMuted
import com.adrian.minimallauncher.feature.finance.FinanceSection
import com.adrian.minimallauncher.feature.finance.FinanceWhite
import com.adrian.minimallauncher.feature.finance.Movements
import com.adrian.minimallauncher.feature.finance.wholePesos
import com.adrian.minimallauncher.ui.components.ChartPart
import com.adrian.minimallauncher.ui.components.ColumnChart
import com.adrian.minimallauncher.ui.components.HorizontalBarChart
import com.adrian.minimallauncher.ui.components.IconAction
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

private val Amber = Color(0xFFFFCF7D)
private val Green = Color(0xFF4DD402)
private val Cream = Color(0xFFF2F0C4)
private val Cell = Color.White.copy(alpha = 0.05f)
private val CellLine = Color.White.copy(alpha = 0.09f)

/**
 * Un banco o tarjeta por separado (diseño 2026-11-10), en orden de lectura:
 * 1. Nombre y tipo, con el lápiz para editar.  2. El saldo (o lo que se debe) en grande.
 * 3. En tarjetas, corte y pago con los días que faltan.  4. Este mes: entradas contra salidas.
 * 5. Las últimas semanas en columnas que se tocan.  6. En qué se gastó.  7. Sus movimientos (con gestos).
 */
@Composable
internal fun BankAccountScreen(
    account: BankAccount,
    records: List<FinanceRecord>,
    today: LocalDate,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val card = account.kind == BankKind.CARD
    val month = YearMonth.from(today)
    val monthRecords = remember(records, month) { records.filter { YearMonth.from(Budget.dateOf(it)) == month } }
    val outMonth = monthRecords.filter { it.kind.isExpense }.sumOf { it.amountCents }
    val inMonth = monthRecords.filter { it.kind.isIncome }.sumOf { it.amountCents }

    val weekStart = Budget.weekStart(today)
    val weeks = (5 downTo 0).map { weekStart.minusWeeks(it.toLong()) }
    val weekly = weeks.map { start ->
        val end = start.plusDays(6)
        records.filter { it.kind.isExpense && Budget.dateOf(it).let { d -> !d.isBefore(start) && !d.isAfter(end) } }.sumOf { it.amountCents }.toFloat()
    }
    var week by rememberSaveable(account.id) { mutableIntStateOf(-1) }

    val byCategory = monthRecords.filter { it.kind.isExpense }
        .groupBy { Budget.categoryOf(it).ifBlank { "Otros" }.replaceFirstChar { c -> c.uppercase() } }
        .mapValues { (_, list) -> list.sumOf { it.amountCents } }
        .entries.sortedByDescending { it.value }.take(5)
    var category by rememberSaveable(account.id) { mutableIntStateOf(-1) }

    Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
        // 1. Nombre y tipo
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.headlineMedium, color = kindColor(account.kind), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    when (account.kind) {
                        BankKind.CARD -> "Tarjeta de crédito"
                        BankKind.DEBIT -> "Débito"
                        BankKind.CASH -> "Efectivo"
                    },
                    style = MaterialTheme.typography.labelLarge, color = com.adrian.minimallauncher.feature.finance.LabelCream,
                )
            }
            IconAction("lapiz", "Editar cuenta", onClick = onEdit)
        }

        // 2. El saldo: lo disponible en débito y efectivo; lo que se debe en tarjeta.
        val heroColor = when {
            card -> cardState(account)
            account.balanceCents < 0 -> LauncherDanger
            else -> LauncherAccent
        }
        FinanceSection(
            Modifier.drawBehind { drawRect(heroColor, size = Size(3.dp.toPx(), size.height)) }.padding(start = 17.dp),
            spacing = 4.dp,
        ) {
            Text(if (card) "Por pagar" else "Disponible", style = MaterialTheme.typography.labelLarge, color = FinanceMuted)
            FinanceAmount(account.balanceCents, size = 40, color = heroColor)
            if (card && account.owedByOthersCents > 0) {
                Text(
                    "Te deben ${wholePesos(account.owedByOthersCents)} · tuyo ${wholePesos((account.balanceCents - account.owedByOthersCents).coerceAtLeast(0))}",
                    style = MaterialTheme.typography.labelLarge, color = Green,
                )
            }
        }

        // Tarjetas ligadas, límite y lo que toca pagar ahora y después.
        if (account.cardDigits.isNotEmpty()) {
            Text(account.cardDigits.joinToString("  ") { "•• $it" }, style = MaterialTheme.typography.labelLarge, color = FinanceMuted)
        }
        if (card) {
            val now = Bank.rolled(account, today)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AmountCell("A pagar", now.dueCents, now.due?.let { "${it.dayOfMonth}/${it.monthValue}" }, if (now.dueCents > 0 && now.due?.isBefore(today) == true) LauncherDanger else FinanceWhite, Modifier.weight(1f))
                AmountCell("Siguiente", now.nextDueCents, null, FinanceWhite, Modifier.weight(1f))
                if (account.limitCents > 0) {
                    AmountCell("Disponible", (account.limitCents - account.balanceCents).coerceAtLeast(0), "de ${wholePesos(account.limitCents)}", Green, Modifier.weight(1f))
                }
            }
        }

        // 3. Fechas de la tarjeta: corte y pago, con cuánto falta.
        if (card) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateCell("Corte", account.cutDay, today, Modifier.weight(1f))
                DateCell("Pago", account.payDay, today, Modifier.weight(1f))
            }
        }

        // 4. Este mes: lo que entró contra lo que salió.
        FinanceSection(spacing = 14.dp) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Este mes", style = MaterialTheme.typography.headlineMedium, color = FinanceWhite)
                val net = inMonth - outMonth
                Text((if (net >= 0) "+" else "") + wholePesos(net), style = MaterialTheme.typography.titleLarge, color = if (net >= 0) Green else LauncherDanger)
            }
            FlowBar(if (card) "Abonos" else "Entró", inMonth, maxOf(inMonth, outMonth), Green)
            FlowBar(if (card) "Cargos" else "Salió", outMonth, maxOf(inMonth, outMonth), if (card) Amber else LauncherAccent)
        }

        // 5. Las últimas seis semanas: tocar una muestra su monto arriba.
        FinanceCard {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Últimas semanas", style = MaterialTheme.typography.headlineMedium, color = FinanceWhite)
                Text(
                    wholePesos((if (week in weekly.indices) weekly[week] else weekly.sum()).toLong()),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (week in weekly.indices) FinanceWhite else ChartPalette[0],
                )
            }
            ColumnChart(
                weekly, weeks.map { "${it.dayOfMonth}" }, height = 130.dp, selected = week,
                onSelect = { i -> week = if (week == i) -1 else i },
            )
        }

        // 6. En qué se gastó este mes.
        if (byCategory.isNotEmpty()) {
            FinanceSection(spacing = 14.dp) {
                Text("En qué se fue", style = MaterialTheme.typography.headlineMedium, color = FinanceWhite)
                HorizontalBarChart(
                    parts = byCategory.mapIndexed { i, (name, cents) -> ChartPart(name, cents.toFloat(), ChartPalette[i % ChartPalette.size]) },
                    amountOf = { wholePesos(it.value.toLong()) },
                    selected = category,
                    onSelect = { i -> category = if (category == i) -1 else i },
                )
                if (category in byCategory.indices && outMonth > 0) {
                    Text(
                        "${byCategory[category].key} es el ${byCategory[category].value * 100 / outMonth}% de lo que salió este mes",
                        style = MaterialTheme.typography.labelLarge,
                        color = ChartPalette[category % ChartPalette.size],
                    )
                }
            }
        }

        // 7. Sus movimientos: deslizar a la derecha elimina, a la izquierda edita.
        FinanceSection(spacing = 4.dp) {
            Text("Movimientos", style = MaterialTheme.typography.headlineMedium, color = FinanceWhite)
            if (records.isEmpty()) Text("Sin movimientos", style = MaterialTheme.typography.bodySmall, color = FinanceMuted)
            else Movements(records, limit = 5)
        }
    }
}

/** Fecha de corte o pago: el día del mes y cuánto falta; rosa si faltan 3 días o menos para pagar. */
@Composable
private fun DateCell(label: String, day: Int, today: LocalDate, modifier: Modifier = Modifier) {
    val next = if (day in 1..31) Bank.nextDate(day, today) else null
    val left = next?.let { ChronoUnit.DAYS.between(today, it) }
    val urgent = label == "Pago" && left != null && left <= 3
    Column(
        modifier
            .background(Cell, RoundedCornerShape(12.dp))
            .drawBehind { drawRect(CellLine, size = Size(size.width, 1.dp.toPx())) }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = FinanceMuted)
        Text(if (day in 1..31) "Día $day" else "Sin fecha", style = MaterialTheme.typography.titleLarge, color = FinanceWhite)
        if (left != null) {
            Text(
                when (left) {
                    0L -> "Hoy"
                    1L -> "Mañana"
                    else -> "En $left días"
                },
                style = MaterialTheme.typography.labelLarge,
                color = if (urgent) LauncherDanger else Cream,
            )
        }
    }
}

/** Un monto de la tarjeta (a pagar, siguiente, disponible) con su nota abajo. */
@Composable
private fun AmountCell(label: String, cents: Long, note: String?, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(Cell, RoundedCornerShape(12.dp))
            .drawBehind { drawRect(CellLine, size = Size(size.width, 1.dp.toPx())) }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = FinanceMuted, maxLines = 1)
        Text(wholePesos(cents), style = MaterialTheme.typography.titleMedium, color = color, maxLines = 1)
        note?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = Cream, maxLines = 1) }
    }
}

/** Una barra de entrada o salida del mes, a escala de la mayor de las dos. */
@Composable
private fun FlowBar(label: String, cents: Long, top: Long, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = FinanceWhite)
            Text(wholePesos(cents), style = MaterialTheme.typography.titleSmall, color = color)
        }
        Box(Modifier.fillMaxWidth().height(12.dp).background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(6.dp))) {
            if (top > 0 && cents > 0) {
                Box(Modifier.fillMaxWidth(cents.toFloat() / top).fillMaxHeight().background(color, RoundedCornerShape(6.dp)))
            }
        }
    }
}
