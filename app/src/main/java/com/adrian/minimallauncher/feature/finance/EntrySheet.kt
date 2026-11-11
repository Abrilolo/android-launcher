package com.adrian.minimallauncher.feature.finance

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.FinanceRecord
import com.adrian.minimallauncher.data.MoneyKind
import com.adrian.minimallauncher.feature.CardLine
import com.adrian.minimallauncher.feature.ModuleTextField
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.feature.bank.Bank
import com.adrian.minimallauncher.feature.bank.BankAccount
import com.adrian.minimallauncher.feature.bank.BankKind
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
import java.math.BigDecimal
import java.util.Locale

private val spanish = Locale.forLanguageTag("es")

/** What the + records; an income then asks whether it is for stability. */
private enum class Entry(val icon: String, val label: String) {
    EXTRA("extra", "EXTRA"), SHARED("compartido", "COMPARTIDO"), FIXED("fijos", "FIJO"), MONTHLY("mes", "MENSUAL"), INCOME("ingreso", "INGRESO"), WITHDRAWAL("transferencia", "RETIRO EN EFECTIVO")
}

private val APART = listOf(MoneyKind.TRANSFER to "TRANSFER.", MoneyKind.GIFT to "REGALO", MoneyKind.SERVICE to "SERVICIO")

internal fun parseCents(text: String): Long? =
    if (text.isBlank()) null else runCatching { BigDecimal(text.replace(',', '.')).movePointRight(2).longValueExact() }.getOrNull()

/** Separates movement direction, budget, description and category with progressive disclosure for secondary options. */
@Composable
internal fun EntrySheet(
    summary: FinanceSummary,
    accounts: List<BankAccount>,
    initial: FinanceRecord? = null,
    payments: List<ScheduledPayment> = emptyList(),
    onClose: () -> Unit,
    onSave: (kind: MoneyKind, cents: Long, description: String, category: String, pendingFromOthers: Long, account: String) -> Unit,
) {
    var entry by rememberSaveable { mutableStateOf(initial?.let(::entryOf) ?: Entry.EXTRA) }
    var lastExpense by rememberSaveable { mutableStateOf(initial?.let(::entryOf)?.takeIf { it != Entry.INCOME && it != Entry.WITHDRAWAL } ?: Entry.EXTRA) }
    var amount by rememberSaveable { mutableStateOf(initial?.let { centsInput(it.amountCents) } ?: "") }
    var concept by rememberSaveable { mutableStateOf(initial?.let(::conceptOf) ?: "") }
    var category by rememberSaveable { mutableStateOf(initial?.category?.takeIf { it.isNotBlank() } ?: BASE_CATEGORIES.first()) }
    var others by rememberSaveable { mutableStateOf(initial?.takeIf { it.pendingFromOthersCents > 0 }?.let { centsInput(it.pendingFromOthersCents) } ?: "") }
    var stable by rememberSaveable { mutableStateOf(initial?.let { if (it.kind == MoneyKind.INCOME_FIXED) true else if (it.kind.isIncome) false else null }) }
    var apart by rememberSaveable { mutableStateOf(initial?.kind?.takeIf { it.isIncome && it != MoneyKind.INCOME_FIXED }) }
    var fixedName by rememberSaveable { mutableStateOf(initial?.takeIf { it.kind == MoneyKind.FIXED }?.category) }
    var account by rememberSaveable { mutableStateOf(initial?.account.orEmpty()) }
    val amountFocus = remember { FocusRequester() }
    BackHandler(onBack = onClose)

    val cents = parseCents(amount)
    val spending = entry == Entry.EXTRA || entry == Entry.SHARED
    val validAccount = if (entry == Entry.WITHDRAWAL) accounts.any { it.id == account && it.kind == BankKind.DEBIT } else true
    val canSave = validAccount && cents != null && cents > 0 && (entry != Entry.INCOME || stable == true || (stable == false && apart != null))
    val categories = (BASE_CATEGORIES + summary.topics.map { it.topic.lowercase(spanish) } + listOfNotNull(initial?.category?.lowercase(spanish)?.takeIf { it.isNotBlank() })).distinct()
    val save = save@{
        if (!canSave) return@save
        val value = cents?.takeIf { it > 0 } ?: return@save
        val kind = when (entry) {
            Entry.EXTRA -> MoneyKind.EXTRA
            Entry.SHARED -> MoneyKind.SHARED
            Entry.FIXED -> MoneyKind.FIXED
            Entry.MONTHLY -> MoneyKind.MONTHLY
            Entry.WITHDRAWAL -> MoneyKind.WITHDRAWAL
            Entry.INCOME -> when (stable) {
                true -> MoneyKind.INCOME_FIXED
                false -> apart ?: return@save
                null -> return@save
            }
        }
        val owed = if (kind == MoneyKind.SHARED) (parseCents(others) ?: 0L).coerceIn(0, value) else 0L
        val description = concept.trim().ifBlank { if (spending) category else entry.label.lowercase(spanish) }
        val tag = when {
            spending -> category
            kind == MoneyKind.FIXED -> fixedName.orEmpty()
            else -> ""
        }
        val payment = payments.firstOrNull { entry == Entry.FIXED && it.category == fixedName }
        val text = payment?.name ?: if (kind == MoneyKind.FIXED && concept.isBlank() && fixedName != null) fixedName!! else description
        onSave(kind, value, text, tag, owed, if (entry == Entry.INCOME && accounts.none { it.id == account && it.kind != BankKind.CARD } && account != Bank.CASH_ACCOUNT_ID) "" else account)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            FinanceSectionHeading(if (initial != null) "Editar movimiento" else "Registrar movimiento")
            SmallLabel("✕", Modifier.clickable(role = Role.Button, onClick = onClose).semantics { contentDescription = "Cerrar" }.padding(8.dp))
        }
        FlowRow(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Entry.EXTRA to "Gasto", Entry.INCOME to "Ingreso", Entry.WITHDRAWAL to "Retiro").forEach { (type, label) ->
                val chosen = if (type == Entry.EXTRA) entry != Entry.INCOME && entry != Entry.WITHDRAWAL else entry == type
                FilterChip(
                    selected = chosen,
                    onClick = {
                        if (entry != Entry.INCOME && entry != Entry.WITHDRAWAL) lastExpense = entry
                        entry = if (type == Entry.EXTRA) lastExpense else type
                    },
                    label = { Text(label) },
                    modifier = Modifier.heightIn(min = 44.dp),
                )
            }
        }

        // Primary inputs: Amount and Concept
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ModuleTextField(
                amount, { value -> amount = amountInput(value) }, "Monto",
                Modifier.weight(1f).focusRequester(amountFocus),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                visualTransformation = PesosTransformation,
            )
            ModuleTextField(
                concept, { concept = it.take(60) }, "Descripción (opc.)",
                Modifier.weight(1.2f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
        }

        // Category pills for daily expenses
        if (spending) {
            FlowRow(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { name ->
                    FilterChip(
                        selected = category == name,
                        onClick = { category = name },
                        label = { Text(name.replaceFirstChar { it.uppercase(spanish) }) },
                        modifier = Modifier.heightIn(min = 40.dp),
                    )
                }
            }
            val topic = summary.topics.firstOrNull { it.topic.equals(category, ignoreCase = true) }
            val refund = initial?.takeIf { Budget.isExtra(it) && Budget.categoryOf(it).equals(category, ignoreCase = true) && Budget.dateOf(it) == summary.date }?.amountCents ?: 0L
            val available = (topic?.todayLeftCents ?: summary.todayLeftCents) + refund
            val remaining = available - (cents ?: 0L)
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(inactiveFinanceColor(financeCategoryColor(category)).copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (topic != null) "De ${topic.topic}" else "De Libre",
                    color = financeCategoryColor(category),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    "Hoy: ${wholePesos(available)} → ${wholePesos(remaining)}",
                    color = if (remaining < 0) MoneyOver else FinanceWhite,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        // Progressive disclosure for Budget Scope & Shared Expense when recording an expense
        if (entry != Entry.INCOME && entry != Entry.WITHDRAWAL) {
            var scopeExpanded by rememberSaveable { mutableStateOf(entry == Entry.FIXED || entry == Entry.SHARED) }
            val budgetLabel = when (entry) {
                Entry.FIXED -> "Gasto fijo"
                Entry.MONTHLY -> "Gasto del mes"
                Entry.SHARED -> "Gasto compartido"
                else -> "Gasto del día"
            }
            FinanceCollapsible(
                title = "Presupuesto y gasto compartido",
                subtitle = "¿De qué presupuesto sale o si te deben una parte?",
                summaryText = budgetLabel,
                summaryColor = FinanceTeal,
                expanded = scopeExpanded,
                onToggle = { scopeExpanded = !scopeExpanded },
            ) {
                Column(Modifier.selectableGroup()) {
                    EntryOption("Gasto del día", "Comida, transporte y compras cotidianas", spending) { entry = Entry.EXTRA }
                    EntryOption("Gasto fijo", "Pago de una previsión o cargo programado", entry == Entry.FIXED) { entry = Entry.FIXED }
                    EntryOption("Gasto del mes", "Se registra en el mes, fuera del gasto diario", entry == Entry.MONTHLY) { entry = Entry.MONTHLY }
                }
                if (spending) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Compartí este gasto", color = FinanceWhite, style = MaterialTheme.typography.bodyMedium)
                            Text("Pagaste por otras personas y te deben una parte", color = FinanceMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = entry == Entry.SHARED,
                            onCheckedChange = { entry = if (it) Entry.SHARED else Entry.EXTRA },
                            modifier = Modifier.semantics { contentDescription = "Compartí este gasto" },
                        )
                    }
                    if (entry == Entry.SHARED) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            StrokeIcon("compartido", StrokeState.NOW, 24.dp)
                            ModuleTextField(
                                others, { value -> others = amountInput(value) }, "Te deben",
                                Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                visualTransformation = PesosTransformation,
                            )
                        }
                    }
                }
            }
        } else if (entry == Entry.INCOME) {
            Text("¿Qué ingreso recibiste?", color = FinanceMuted, style = MaterialTheme.typography.labelLarge)
            Column(Modifier.selectableGroup()) {
                EntryOption("Ingreso habitual", "Cuenta para tu presupuesto, como el sueldo", stable == true) { stable = true; apart = null }
                APART.forEach { (kind, _) ->
                    val label = when (kind) {
                        MoneyKind.TRANSFER -> "Transferencia recibida"
                        MoneyKind.GIFT -> "Regalo"
                        else -> "Pago por un servicio"
                    }
                    EntryOption(label, null, stable == false && apart == kind) { stable = false; apart = kind }
                }
            }
            FinanceReadMore("Los ingresos ocasionales se registran aparte del ingreso habitual.")
        }

        if (entry == Entry.FIXED && payments.isNotEmpty()) {
            Text("Cargo vinculado a una cuenta", color = FinanceMuted, style = MaterialTheme.typography.labelMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                payments.forEach { payment ->
                    FilterChip(
                        selected = fixedName == payment.category,
                        onClick = {
                            if (fixedName == payment.category) {
                                fixedName = null
                            } else {
                                fixedName = payment.category; account = payment.accountId; concept = payment.name; amount = plainAmount(payment.cents)
                            }
                        },
                        label = { Text(payment.name) },
                    )
                }
            }
        }
        if (entry == Entry.FIXED && summary.fixed.isNotEmpty()) {
            Text("Aplicar a una previsión de gasto", color = FinanceMuted, style = MaterialTheme.typography.labelMedium)
            FixedPicker(summary.fixed, fixedName) { status ->
                fixedName = if (fixedName == status.topic) null else status.topic
                if (fixedName != null && amount.isBlank()) amount = centsInput(status.limitCents)
            }
        }

        // Progressive disclosure for Account / Payment method
        val availableAccounts = if (entry == Entry.WITHDRAWAL) accounts.filter { it.kind == BankKind.DEBIT }
        else (accounts + listOfNotNull(Bank.cashAccount().takeIf { accounts.none { it.id == Bank.CASH_ACCOUNT_ID } }))
            .filter { entry != Entry.INCOME || it.kind != BankKind.CARD }
        val selectedAccountObj = availableAccounts.firstOrNull { it.id == account }
        val selectedAccountName = selectedAccountObj?.name ?: "Sin especificar"
        var accountExpanded by rememberSaveable(entry.name) { mutableStateOf(entry == Entry.WITHDRAWAL || account.isNotBlank()) }

        FinanceCollapsible(
            title = if (entry == Entry.WITHDRAWAL) "Retirar de débito" else if (entry == Entry.INCOME) "Recibir en cuenta" else "¿Cómo pagaste?",
            summaryText = selectedAccountName,
            summaryColor = if (account.isBlank()) FinanceMuted else FinanceWhite,
            expanded = accountExpanded,
            onToggle = { accountExpanded = !accountExpanded },
            showRule = false,
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (entry != Entry.WITHDRAWAL) {
                    FilterChip(selected = account.isBlank(), onClick = { account = "" }, label = { Text("Sin especificar") })
                }
                availableAccounts.forEach { option ->
                    val method = when (option.kind) { BankKind.DEBIT -> "Débito"; BankKind.CARD -> "Crédito"; BankKind.CASH -> "Efectivo" }
                    FilterChip(
                        selected = account == option.id,
                        onClick = { account = option.id },
                        label = { Text(if (option.kind == BankKind.CASH) option.name else "$method · ${option.name}") },
                        modifier = Modifier.heightIn(min = 44.dp),
                    )
                }
            }
            FinanceReadMore(
                when {
                    entry == Entry.WITHDRAWAL -> if (availableAccounts.isEmpty()) "Agrega una cuenta de débito en Banco para registrar un retiro." else "Pasa de débito a efectivo. No aumenta la deuda de crédito ni cuenta como gasto."
                    account.isBlank() -> "El movimiento se registra sin modificar saldos de cuentas."
                    selectedAccountObj?.kind == BankKind.CARD -> "Este gasto aumenta la deuda de la tarjeta de crédito."
                    entry == Entry.INCOME -> "El ingreso aumenta el saldo de la cuenta seleccionada."
                    else -> "Este gasto reduce el saldo seleccionado."
                }
            )
        }

        if (spending && cents != null && cents > summary.todayLeftCents) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("¿Es fijo o compra del mes?", color = FinanceMuted, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    androidx.compose.material3.TextButton(onClick = { entry = Entry.FIXED }) { Text("Fijo") }
                    androidx.compose.material3.TextButton(onClick = { entry = Entry.MONTHLY }) { Text("Del mes") }
                }
            }
        }
        val fixedStatus = summary.fixed.firstOrNull { it.topic == fixedName }.takeIf { entry == Entry.FIXED }
        if (fixedStatus != null) FixedOutlook(fixedStatus, cents ?: 0)
        val refund = initial?.takeIf { Budget.isExtra(it) && Budget.categoryOf(it).equals(category, ignoreCase = true) && Budget.dateOf(it) == summary.date }?.amountCents ?: 0L
        val left = (summary.topics.firstOrNull { it.topic.equals(category, ignoreCase = true) }?.todayLeftCents ?: summary.todayLeftCents) + refund
        val warn = spending && cents != null && cents > 0 && cents > left
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (warn) {
                Row(
                    Modifier.weight(1f).border(1.dp, MoneyOver, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 8.dp)
                        .semantics(mergeDescendants = true) { contentDescription = "Pasa lo de hoy: quedaría ${wholePesos(left - cents!!)}" },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StrokeIcon("aviso", StrokeState.NOW, 20.dp)
                    Text(wholePesos(left - cents!!), style = MaterialTheme.typography.labelLarge, color = MoneyOver)
                    Spacer(Modifier.weight(1f))
                    StrokeIcon("bolsa", StrokeState.DONE, 18.dp)
                }
                Spacer(Modifier.size(12.dp))
            } else Spacer(Modifier.weight(1f))
            androidx.compose.material3.Button(
                onClick = { save() }, enabled = canSave,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFF5F5F5), contentColor = Color.Black),
                modifier = Modifier.heightIn(min = 48.dp),
            ) { StrokeIcon("listo", StrokeState.DONE, 22.dp, color = Color.Black) }
        }
    }
}

/** A labeled choice with a whole-row touch target and native selection semantics. */
@Composable
private fun EntryOption(label: String, detail: String?, picked: Boolean, onPick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp).selectable(picked, role = Role.RadioButton, onClick = onPick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RadioButton(selected = picked, onClick = null)
        Column(Modifier.weight(1f)) {
            Text(label, color = FinanceWhite, style = MaterialTheme.typography.bodyMedium)
            if (detail != null) Text(detail, color = FinanceMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** The plan's fixed costs as pills; the picked one white. */
@Composable
private fun FixedPicker(fixed: List<TopicStatus>, picked: String?, onPick: (TopicStatus) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        fixed.forEach { status ->
            val on = status.topic == picked
            val shape = RoundedCornerShape(18.dp)
            Row(
                Modifier.background(if (on) Color(0xFFF5F5F5) else Color.Transparent, shape)
                    .border(1.dp, if (on) Color(0xFFF5F5F5) else Color(0xFF333333), shape)
                    .heightIn(min = 44.dp).selectable(selected = on, role = Role.RadioButton) { onPick(status) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StrokeIcon("fijos", StrokeState.DONE, 16.dp, color = if (on) Color.Black else null)
                Text(status.topic, style = MaterialTheme.typography.labelLarge, color = if (on) Color.Black else Color(0xFFF5F5F5), maxLines = 1)
            }
        }
    }
}

/**
 * How the picked fixed cost stands after this payment: what is left (today's share, or the month's rest), red
 * once past it, and the new share for the days still to come.
 */
@Composable
private fun FixedOutlook(status: TopicStatus, cents: Long) {
    val left = Budget.fixedLeft(status, cents)
    val next = Budget.nextDaysShare(status, cents)
    val over = left < 0
    Row(
        Modifier.fillMaxWidth()
            .border(1.dp, if (over) MoneyOver else CardLine, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Quedan ${wholePesos(left)}" + (next?.let { ", próximos días ${wholePesos(it)}" } ?: "")
            },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StrokeIcon(if (over) "aviso" else periodIcon(status.period), if (over) StrokeState.NOW else StrokeState.DONE, 20.dp)
        Text(wholePesos(left), style = MaterialTheme.typography.labelLarge, color = if (over) MoneyOver else Color(0xFFF5F5F5))
        Spacer(Modifier.weight(1f))
        if (next != null) {
            SmallLabel("Próximos días")
            Text(wholePesos(next), style = MaterialTheme.typography.labelLarge, color = if (over) MoneyOver else Color(0xFFF5F5F5))
        }
    }
}

/** The entry a saved movement was registered with, so an edit opens on the same one. */
private fun entryOf(record: FinanceRecord): Entry = when (record.kind) {
    MoneyKind.EXTRA -> Entry.EXTRA
    MoneyKind.SHARED -> Entry.SHARED
    MoneyKind.FIXED -> Entry.FIXED
    MoneyKind.MONTHLY -> Entry.MONTHLY
    MoneyKind.WITHDRAWAL -> Entry.WITHDRAWAL
    else -> Entry.INCOME
}

/** The concept the user typed, or nothing when the saved text is only the default (the category or the entry's name). */
private fun conceptOf(record: FinanceRecord): String {
    val default = listOf(record.category, entryOf(record).label, record.category.replaceFirstChar { it.uppercase(spanish) })
    return if (default.any { it.equals(record.description, ignoreCase = true) } || record.description.isBlank()) "" else record.description
}

/** An amount as the field takes it: "9504.82", "300", or "" for none. */
private fun centsInput(cents: Long): String = if (cents % 100 == 0L) (cents / 100).toString() else "${cents / 100}." + "%02d".format(cents % 100)

/** The cards and accounts as pills, the picked one white; tapping it again picks none. */
@Composable
private fun AccountPicker(accounts: List<BankAccount>, picked: String, onPick: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        accounts.forEach { account ->
            val on = account.id == picked
            val shape = RoundedCornerShape(18.dp)
            Row(
                Modifier.background(if (on) Color(0xFFF5F5F5) else Color.Transparent, shape)
                    .border(1.dp, if (on) Color(0xFFF5F5F5) else Color(0xFF333333), shape)
                    .clickable(role = Role.RadioButton) { onPick(account.id) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(account.name, style = MaterialTheme.typography.labelLarge, color = if (on) Color.Black else Color(0xFFF5F5F5), maxLines = 1)
            }
        }
    }
}

/** An icon with its caption: red detail when picked, drawn once on the tap. */
@Composable
private fun Choice(icon: String, label: String, selected: Boolean, onClick: () -> Unit) {
    var taps by remember { mutableIntStateOf(0) }
    Column(
        Modifier.widthIn(min = 64.dp).heightIn(min = 64.dp).clickable(role = Role.RadioButton) { taps++; onClick() }.semantics(mergeDescendants = true) { contentDescription = label; this.selected = selected }.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StrokeIcon(icon, if (selected) StrokeState.NOW else StrokeState.OFF, 24.dp, playKey = taps)
        SmallLabel(label, color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
