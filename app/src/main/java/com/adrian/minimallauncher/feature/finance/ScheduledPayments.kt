package com.adrian.minimallauncher.feature.finance

import com.adrian.minimallauncher.ui.components.StrokeState
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.IconAction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.FinanceRecord
import com.adrian.minimallauncher.data.MoneyKind
import com.adrian.minimallauncher.feature.ModuleTextField
import com.adrian.minimallauncher.feature.bank.BankAccount
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** A monthly obligation, stored apart from budget estimates. Creating it never changes a balance. */
data class ScheduledPayment(val id: String, val name: String, val cents: Long, val accountId: String, val day: Int) {
    val category: String get() = "recurring:$id"
    fun paid(records: List<FinanceRecord>, date: LocalDate): Boolean = records.any {
        it.kind == MoneyKind.FIXED && it.category == category && YearMonth.from(Budget.dateOf(it)) == YearMonth.from(date)
    }
    fun due(records: List<FinanceRecord>, date: LocalDate): LocalDate {
        val month = YearMonth.from(date).let { if (paid(records, date)) it.plusMonths(1) else it }
        return month.atDay(day.coerceIn(1, month.lengthOfMonth()))
    }
    fun json(): JSONObject = JSONObject().put("id", id).put("name", name).put("cents", cents).put("account", accountId).put("day", day)
    companion object {
        fun decode(rows: JSONArray?): List<ScheduledPayment> = (0 until (rows?.length() ?: 0)).mapNotNull { i ->
            runCatching {
                val row = rows!!.getJSONObject(i)
                ScheduledPayment(row.getString("id"), row.getString("name"), row.getLong("cents"), row.getString("account"), row.getInt("day"))
            }.getOrNull()?.takeIf { it.id.isNotBlank() && it.name.isNotBlank() && it.cents > 0 && it.day in 1..31 }
        }
    }
}

/** Shared by the page and launcher widget; overdue obligations stay visible until recorded. */
fun nextScheduledPayment(payments: List<ScheduledPayment>, records: List<FinanceRecord>, date: LocalDate): ScheduledPayment? =
    payments.minByOrNull { it.due(records, date) }

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ScheduledPaymentsView(
    plan: FinancePlan, accounts: List<BankAccount>, records: List<FinanceRecord>, today: LocalDate,
    onSave: (FinancePlan) -> Unit, onRecord: (ScheduledPayment) -> Unit,
) {
    var expandedPaymentId by rememberSaveable { mutableStateOf<String?>(null) }
    var formOpen by rememberSaveable { mutableStateOf(plan.payments.isEmpty()) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var account by rememberSaveable { mutableStateOf("") }
    var day by rememberSaveable { mutableStateOf("") }
    var deleting by remember { mutableStateOf<ScheduledPayment?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FinanceReadMore("Suscripciones y pagos mensuales ligados a tus cuentas. Se guardan por separado de las previsiones del presupuesto.")

        plan.payments.sortedBy { it.due(records, today) }.forEach { payment ->
            val paid = payment.paid(records, today)
            val linked = accounts.firstOrNull { it.id == payment.accountId }
            val isExpanded = expandedPaymentId == payment.id
            val accountLabel = linked?.name ?: "Cuenta eliminada"
            val dueText = "${if (paid) "Próximo" else "Vence"}: ${payment.due(records, today)}"
            FinanceCollapsible(
                title = payment.name,
                subtitle = "$accountLabel · $dueText",
                icon = "fijos",
                summaryText = wholePesos(payment.cents),
                summaryColor = if (paid) MoneyPlenty else FinanceLavender,
                expanded = isExpanded,
                onToggle = { expandedPaymentId = if (isExpanded) null else payment.id },
            ) {
                if (linked == null) {
                    Text("Cuenta eliminada · edita este pago para vincular otra cuenta.", color = MoneyOver, style = MaterialTheme.typography.bodySmall)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (paid) IconAction("listo", "Registrado este mes", enabled = false) {} else IconAction("nueva", "Registrar cargo", enabled = linked != null) { onRecord(payment) }
                    IconAction("lapiz", "Editar pago") {
                        editingId = payment.id
                        name = payment.name
                        amount = plainAmount(payment.cents)
                        account = payment.accountId
                        day = payment.day.toString()
                        formOpen = true
                    }
                    IconAction("papelera", "Eliminar pago") { deleting = payment }
                }
            }
        }

        FinanceCollapsible(
            title = if (editingId == null) "+ Agregar pago mensual" else "Editar pago mensual",
            subtitle = "Nombre, importe, día de cobro y cuenta vinculada",
            expanded = formOpen || editingId != null,
            onToggle = {
                if (editingId != null) {
                    editingId = null; name = ""; amount = ""; day = ""; account = ""
                    formOpen = false
                } else {
                    formOpen = !formOpen
                }
            },
            showRule = false,
        ) {
            ModuleTextField(name, { name = it.take(60) }, "Nombre del pago", Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModuleTextField(
                    amount, { amount = amountInput(it) }, "Importe", Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    visualTransformation = PesosTransformation,
                )
                ModuleTextField(
                    day, { day = it.filter(Char::isDigit).take(2) }, "Día (1–31)", Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
            Text("Tarjeta o cuenta", color = FinanceMuted, style = MaterialTheme.typography.labelMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                accounts.forEach { row ->
                    FilterChip(selected = account == row.id, onClick = { account = row.id }, label = { Text(row.name) })
                }
            }
            if (accounts.isEmpty()) Text("Agrega una cuenta en Banco para vincular el pago.", color = FinanceMuted, style = MaterialTheme.typography.bodySmall)
            val cents = parseCents(amount) ?: 0L
            val valid = name.isNotBlank() && cents > 0 && day.toIntOrNull() in 1..31 && accounts.any { it.id == account }
            Button(
                onClick = {
                    val payment = ScheduledPayment(editingId ?: UUID.randomUUID().toString(), name.trim(), cents, account, day.toInt())
                    onSave(plan.copy(payments = plan.payments.filterNot { it.id == payment.id } + payment))
                    editingId = null; name = ""; amount = ""; day = ""; account = ""
                    formOpen = false
                },
                enabled = valid,
                modifier = Modifier.fillMaxWidth(),
            ) { StrokeIcon(if (editingId == null) "nueva" else "listo", StrokeState.DONE, 22.dp) }
            if (editingId != null) {
                IconAction("no", "Cancelar edición") { editingId = null; name = ""; amount = ""; day = ""; account = ""; formOpen = false }
            }
        }
    }
    deleting?.let { payment ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Eliminar ${payment.name}") },
            text = { Text("Se elimina la programación. Los cargos ya registrados se conservan.") },
            confirmButton = {
                IconAction("papelera", "Eliminar") { onSave(plan.copy(payments = plan.payments.filterNot { it.id == payment.id })); deleting = null }
            },
            dismissButton = { IconAction("no", "Cancelar") { deleting = null } },
        )
    }
}
