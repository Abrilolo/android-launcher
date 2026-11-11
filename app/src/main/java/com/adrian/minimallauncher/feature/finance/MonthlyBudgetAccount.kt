package com.adrian.minimallauncher.feature.finance

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Reconciles posted expenses with unpaid commitments without counting fixed payments twice. */
internal data class MonthlyBudgetAccount(
    val income: Long,
    val spent: Long,
    val reserved: Long,
    val saving: Long,
) {
    val remaining: Long get() = income - spent - reserved - saving
}

internal fun monthlyBudgetAccount(summary: FinanceSummary) = MonthlyBudgetAccount(
    summary.fixedIncomeCents,
    summary.spentMonthCents,
    (summary.fixedCommittedCents - summary.fixedPaidCents).coerceAtLeast(0),
    summary.savingCents,
)

@Composable
internal fun MonthlyBudgetAccount(summary: FinanceSummary) {
    val account = monthlyBudgetAccount(summary)
    var breakdownOpen by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            summary.date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es"))).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodySmall,
            color = FinanceMuted,
        )
        Text(
            if (account.remaining < 0) "Falta para cubrir el mes" else "Te queda para gastar este mes",
            style = MaterialTheme.typography.titleMedium,
            color = FinanceWhite,
        )
        FinanceAmount(kotlin.math.abs(account.remaining), size = 36, color = if (account.remaining < 0) MoneyOver else FinanceTeal)
        FinanceReadMore("Después de tus registros, los fijos pendientes y el ahorro. Incluye lo que distribuyas por categoría; no es tu saldo bancario.")

        FinanceCollapsible(
            title = "Desglose del cálculo mensual",
            subtitle = "Ingreso ${wholePesos(account.income)} · Gastado ${wholePesos(account.spent)}",
            expanded = breakdownOpen,
            onToggle = { breakdownOpen = !breakdownOpen },
        ) {
            BudgetAccountLine(
                "1. Ingreso para el mes", account.income,
                "Base de tu presupuesto: ingreso habitual registrado o previsto, el mayor de los dos.",
            )
            BudgetAccountLine(
                "2. Gastos ya registrados", account.spent,
                "Incluye gastos diarios, compartidos, fijos y compras del mes.", subtract = true,
            )
            BudgetAccountLine(
                "3. Fijos pendientes de cubrir", account.reserved,
                "Dinero reservado que aún no has pagado. Un fijo pagado ya está en el paso anterior.", subtract = true,
            )
            BudgetAccountLine(
                "4. Reserva de ahorro (${summary.savingPct}%)", account.saving,
                "${summary.savingPct}% del ingreso después de los fijos previstos. Es una reserva del plan, no un depósito confirmado.", subtract = true,
            )
            if (summary.otherIncomeCents > 0) {
                FinanceReadMore("Ingresos ocasionales: ${wholePesos(summary.otherIncomeCents)}. Se registran aparte y no se suman a esta base.")
            }
        }
    }
}

@Composable
private fun BudgetAccountLine(label: String, cents: Long, detail: String, subtract: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = FinanceWhite)
            Text((if (subtract) "− " else "") + wholePesos(cents), style = MaterialTheme.typography.titleSmall, color = FinanceWhite)
        }
        FinanceReadMore(detail)
    }
}
