package com.adrian.minimallauncher.feature.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.feature.CardFill
import com.adrian.minimallauncher.feature.LauncherAccent
import com.adrian.minimallauncher.feature.LauncherDanger
import com.adrian.minimallauncher.feature.ModuleTextField
import com.adrian.minimallauncher.feature.bank.BankAccount
import com.adrian.minimallauncher.feature.bank.BankKind
import java.util.UUID

/** What the editor sheet is open for. */
private sealed interface PlanEdit {
    data class Payment(val payment: ScheduledPayment?) : PlanEdit
    data object Income : PlanEdit
    data class Cap(val tope: Tope?) : PlanEdit
}

/**
 * Presupuesto (2026-11-11, "repartir lo disponible"): how this month's money is split.
 * 1. El reparto: en cuentas − planeado (+ lo que está por entrar) = disponible; − lo que tus topes aún van a gastar =
 *    sin asignar, and what is left over can go to savings. Each line opens its detail.
 * 2. Planeado · con fecha: the income, scheduled payments and the cards' payments.
 * 3. Topes · hasta cuánto: the caps.
 * A row opens its sheet to edit (deleting is there too); the blue + adds.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FinancePlanView(
    view: MoneyView,
    plan: FinancePlan,
    accounts: List<BankAccount>,
    onSavePlan: (FinancePlan) -> Unit,
    onSetBudget: (String, BudgetPeriod, Long) -> Unit,
    onMarkDone: (PlanItem) -> Unit,
    onOpenBank: () -> Unit,
    savingGoal: String = "",
    onSaveGoal: (String) -> Unit = {},
    onSaveAccount: (BankAccount) -> Unit = {},
) {
    var editing by remember { mutableStateOf<PlanEdit?>(null) }
    var saving by remember { mutableStateOf(false) }
    val goal = remember(savingGoal) { SavingGoal.decode(savingGoal) }
    Column(verticalArrangement = Arrangement.spacedBy(26.dp)) {
        // This month (what is real and what is still planned) and next month (only what is already planned).
        SwipePages(
            titles = listOf("Este mes", "Siguiente mes"),
            info = listOf(
                "Se reparte así: lo que tienes en cuentas, menos lo planeado con fecha, más lo que está por entrar, es tu dinero disponible. De ahí salen tus topes; lo que sobra puede ir a ahorro. Toca \"Para ahorro\" para apartarlo ya.",
                "Lo que ya sabes del mes que viene: tu ingreso planeado, lo que toca pagar de tus tarjetas y tus pagos fijos, y tus topes. Lo que te quedaría sale abajo.",
            ),
        ) { page ->
            if (page == 0) Split(view, goal?.savedCents ?: 0) { saving = true }
            else NextMonth(view, plan)
        }

        // Planeado · con fecha
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHead("Planeado · con fecha", "Lo que tiene fecha: tu ingreso, tus pagos fijos y lo que toca pagar de tus tarjetas. No cuenta como gastado hasta que llega el cobro.") {
                FinIcon("nueva", "Agregar pago", ActionColor.Add) { editing = PlanEdit.Payment(null) }
            }
            Column(Modifier.fillMaxWidth().tinted(FinanceWhite, dashed = true).padding(horizontal = 16.dp, vertical = 4.dp)) {
                val income = view.plan.firstOrNull { it.kind == PlanKind.INCOME }
                Line(
                    dot = income?.let { stateColor(it.state) } ?: IncomeTint,
                    name = "Ingreso",
                    note = plan.incomeDay.takeIf { it > 0 }?.let { "día $it" } ?: "sin día",
                    amount = if (plan.incomeCents > 0) "+" + short(plan.incomeCents) else "+ agregar",
                    amountColor = IncomeTint,
                ) { editing = PlanEdit.Income }
                plan.payments.sortedBy { it.day }.forEach { payment ->
                    val item = view.plan.firstOrNull { it.payment?.id == payment.id }
                    Line(
                        dot = item?.let { stateColor(it.state) } ?: FinanceWhite,
                        name = payment.name,
                        note = "día ${payment.day}",
                        amount = short(payment.cents),
                    ) { editing = PlanEdit.Payment(payment) }
                }
                val cards = (view.plan + view.later).filter { it.kind == PlanKind.CARD }
                if (cards.isNotEmpty()) Pop(Modifier.fillMaxWidth(), "Tarjetas", pop = { close ->
                    cards.forEach { PopRow("${it.name} · ${com.adrian.minimallauncher.feature.shortDay(it.due)}", short(it.cents), dot = ToPayTint) }
                    PopAction("banco", "Abrir en Banco") { close(); onOpenBank() }
                }) {
                    LineBody(ToPayTint, "Tarjetas", "de Banco", short(view.plan.filter { it.kind == PlanKind.CARD && it.open }.sumOf { it.cents }), FinanceWhite, last = true)
                }
            }
        }

        // Topes · hasta cuánto
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHead("Topes · hasta cuánto", "Lo máximo que quieres gastar en algo. No se aparta de tu dinero: cuando registras un gasto de ese tema, se suma a su tope.") {
                FinIcon("nueva", "Agregar tope", ActionColor.Add) { editing = PlanEdit.Cap(null) }
            }
            Column(Modifier.fillMaxWidth().glass().padding(horizontal = 16.dp, vertical = 4.dp)) {
                if (view.topes.isEmpty()) LineBody(FinanceWhite.copy(alpha = 0.3f), "Agregar tope", null, "", FinanceWhite, last = true, Modifier.clickable(role = Role.Button) { editing = PlanEdit.Cap(null) })
                view.topes.forEachIndexed { i, tope ->
                    val used = if (tope.monthCents <= 0) 0f else (tope.spentMonthCents.toFloat() / tope.monthCents).coerceIn(0f, 1f)
                    Column(Modifier.fillMaxWidth().clickable(role = Role.Button) { editing = PlanEdit.Cap(tope) }.semantics { contentDescription = "Editar ${tope.topic}" }) {
                        LineBody(financeCategoryColor(tope.topic), tope.topic.replaceFirstChar { it.uppercase() }, periodName(tope.period).lowercase(), short(tope.limitCents), FinanceWhite, last = true)
                        Box(Modifier.fillMaxWidth().padding(start = 20.dp).height(3.dp).background(GlassFill, RoundedCornerShape(2.dp))) {
                            Box(Modifier.fillMaxWidth(used).fillMaxHeight().background(usageColor(tope.spentMonthCents, tope.monthCents), RoundedCornerShape(2.dp)))
                        }
                        Box(Modifier.height(10.dp))
                        if (i < view.topes.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(GlassLine))
                    }
                }
            }
        }
    }

    if (saving) {
        ModalBottomSheet(
                scrimColor = SheetScrim,
            onDismissRequest = { saving = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = CardFill,
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SaveNowEditor(view.unassignedCents, accounts) { cents, accountId ->
                    val today = view.today
                    val current = goal ?: SavingGoal(0, today, today.plusYears(1))
                    onSaveGoal(current.copy(savedCents = current.savedCents + cents).encode())
                    accounts.firstOrNull { it.id == accountId }?.let { onSaveAccount(it.copy(balanceCents = it.balanceCents - cents)) }
                    saving = false
                }
            }
        }
    }

    editing?.let { edit ->
        ModalBottomSheet(
                scrimColor = SheetScrim,
            onDismissRequest = { editing = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = CardFill,
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when (edit) {
                    is PlanEdit.Payment -> PaymentEditor(
                        edit.payment, accounts,
                        onDone = edit.payment?.let { p -> view.plan.firstOrNull { it.payment?.id == p.id && it.open } }?.let { item -> { onMarkDone(item); editing = null } },
                        onDelete = edit.payment?.let { p -> { onSavePlan(plan.copy(payments = plan.payments.filter { it.id != p.id })); editing = null } },
                    ) { saved ->
                        onSavePlan(plan.copy(payments = plan.payments.filter { it.id != saved.id } + saved))
                        editing = null
                    }
                    PlanEdit.Income -> IncomeEditor(plan, onDelete = if (plan.incomeCents > 0) ({ onSavePlan(plan.copy(incomeCents = 0, incomeDay = 0)); editing = null }) else null) { cents, day ->
                        onSavePlan(plan.copy(incomeCents = cents, incomeDay = day))
                        editing = null
                    }
                    is PlanEdit.Cap -> CapEditor(edit.tope, onDelete = edit.tope?.let { t -> { onSetBudget(t.topic, t.period, 0); editing = null } }) { topic, period, cents ->
                        edit.tope?.let { old -> if (old.topic != topic || old.period != period) onSetBudget(old.topic, old.period, 0) }
                        onSetBudget(topic, period, cents)
                        editing = null
                    }
                }
            }
        }
    }
}

/**
 * El reparto del mes, line by line: where the money is (white), what is already promised (amber), what comes in
 * (green), what is free (its state color), what the caps will still take (blue) and what has no destination yet.
 */
@Composable
private fun Split(view: MoneyView, savedCents: Long, onSave: () -> Unit) {
    val free = view.unassignedCents
    Column(Modifier.fillMaxWidth().glass().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PopRow("En cuentas", short(view.haveCents))
        Pop(Modifier.fillMaxWidth(), "Ver lo planeado", pop = {
            view.toPay.forEach { PopRow("${it.name} · ${com.adrian.minimallauncher.feature.shortDay(it.due)}", short(it.cents), dot = stateColor(it.state)) }
            if (view.toPay.isEmpty()) PopRow("Nada pendiente", "")
        }) { PopRow("Planeado", "−" + short(view.toPayCents), ToPayTint) }
        if (view.incomingCents > 0) PopRow("Por entrar", "+" + short(view.incomingCents), IncomeTint)
        PopRule()
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Disponible", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = FinanceWhite)
            Text(short(view.availableCents + view.incomingCents), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium), color = availableColor(view))
        }
        Pop(Modifier.fillMaxWidth(), "Ver tus topes", pop = {
            view.topes.forEach { PopRow(it.topic.replaceFirstChar { c -> c.uppercase() }, short(it.leftMonthCents), dot = financeCategoryColor(it.topic)) }
            if (view.topes.isEmpty()) PopRow("Sin topes", "")
        }) { PopRow("Topes por gastar", if (view.topesLeftCents > 0) "−" + short(view.topesLeftCents) else short(0), SpentTint) }
        PopRule()
        // What has no destination: it can go to savings, or it says by how much the caps go over.
        Row(
            Modifier.fillMaxWidth().then(if (free > 0) Modifier.clickable(role = Role.Button, onClick = onSave) else Modifier),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (free < 0) "Te pasas" else "Para ahorro", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = FinanceWhite)
            Text(short(kotlin.math.abs(free)), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium), color = if (free < 0) LauncherDanger else MoneyPlenty)
            // Set it aside now: a real move into savings.
            if (free > 0) StrokeIconBox("ahorro", ActionColor.Done)
        }
        if (savedCents > 0) PopRow("Ya apartado", short(savedCents), MoneyPlenty)
    }
}

/** An action's icon without a button of its own (the row around it is the button). */
@Composable
private fun StrokeIconBox(icon: String, color: Color) {
    Box(Modifier.padding(start = 10.dp), contentAlignment = Alignment.Center) {
        com.adrian.minimallauncher.ui.components.StrokeIcon(icon, com.adrian.minimallauncher.ui.components.StrokeState.DONE, 22.dp, color = color)
    }
}

/**
 * Next month, from what is already known: the planned income, the cards' payments that fall in it, the scheduled
 * payments and the caps. What would be left is green, or pink when it does not reach.
 */
@Composable
private fun NextMonth(view: MoneyView, plan: FinancePlan) {
    val next = java.time.YearMonth.from(view.today).plusMonths(1)
    val cards = view.later.filter { java.time.YearMonth.from(it.due) == next && it.kind == PlanKind.CARD }
    val scheduled = plan.payments.map { p -> PlanItem("next:${p.id}", p.name, p.cents, next.atDay(p.day.coerceIn(1, next.lengthOfMonth())), PlanKind.SCHEDULED, PlanState.PLANNED, p.accountId, p) }
    val items = (cards + scheduled).sortedBy { it.due }
    val income = plan.incomeCents
    val toPay = items.sumOf { it.cents }
    val topes = view.topes.sumOf { FinancePlan.perMonth(it.limitCents, it.period, next.lengthOfMonth()) }
    val left = income - toPay - topes
    Column(Modifier.fillMaxWidth().glass().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PopRow("Ingreso planeado", if (income > 0) "+" + short(income) else "—", IncomeTint)
        Pop(Modifier.fillMaxWidth(), "Ver lo que toca pagar", pop = {
            items.forEach { PopRow("${it.name} · ${com.adrian.minimallauncher.feature.shortDay(it.due)}", short(it.cents), dot = kindTint(it.kind)) }
            if (items.isEmpty()) PopRow("Nada todavía", "")
        }) { PopRow("Por pagar", "−" + short(toPay), ToPayTint) }
        PopRow("Topes", if (topes > 0) "−" + short(topes) else short(0), SpentTint)
        PopRule()
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(if (left < 0) "Te faltarían" else "Te quedaría", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = FinanceWhite)
            Text(short(kotlin.math.abs(left)), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium), color = if (left < 0) LauncherDanger else MoneyPlenty)
        }
        if (items.isNotEmpty()) {
            PopRule()
            items.forEach { item ->
                Row(Modifier.fillMaxWidth().heightIn(min = 36.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Dot(kindTint(item.kind))
                    Text(item.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = FinanceWhite, maxLines = 1)
                    Text(com.adrian.minimallauncher.feature.shortDay(item.due), style = MaterialTheme.typography.labelLarge, color = LabelCream)
                    Text(short(item.cents), Modifier.width(88.dp), style = MaterialTheme.typography.bodyMedium, color = FinanceWhite, textAlign = TextAlign.End)
                }
            }
        }
    }
}

/** Setting money aside now: how much (what has no destination, to start) and from which account it leaves. */
@Composable
private fun SaveNowEditor(free: Long, accounts: List<BankAccount>, onSave: (Long, String) -> Unit) {
    var amount by remember { mutableStateOf(if (free > 0) centsField(free) else "") }
    val debit = accounts.filter { it.kind == BankKind.DEBIT }.sortedByDescending { it.balanceCents }
    var account by remember { mutableStateOf(debit.firstOrNull()?.id ?: "") }
    SheetHead("Apartar para ahorro", onDelete = null)
    ModuleTextField(amount, { amount = amountInput(it) }, "Cuánto", Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), visualTransformation = PesosTransformation)
    SectionLabel("Sale de")
    Chips(debit.map { it.id to "${it.name} · ${short(it.balanceCents)}" }, account) { account = it }
    val cents = parseCents(amount) ?: 0
    SaveButton(cents > 0 && account.isNotBlank()) { onSave(cents, account) }
}

/** A row of a grouped list: dot, name, its day or period in cream, amount. A tap edits it. */
@Composable
private fun Line(dot: Color, name: String, note: String?, amount: String, amountColor: Color = FinanceWhite, onClick: () -> Unit) {
    LineBody(dot, name, note, amount, amountColor, last = false, Modifier.clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = "Editar $name" })
}

@Composable
private fun LineBody(dot: Color, name: String, note: String?, amount: String, amountColor: Color, last: Boolean, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Dot(dot)
            Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = FinanceWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
            note?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = LabelCream) }
            Text(amount, Modifier.width(96.dp), style = MaterialTheme.typography.bodyLarge, color = amountColor, textAlign = TextAlign.End, maxLines = 1)
        }
        if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(GlassLine))
    }
}

/** The sheet's head: its title, and the icons to mark done (green) and delete (pink). */
@Composable
private fun SheetHead(title: String, onDone: (() -> Unit)? = null, onDelete: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = FinanceWhite)
        onDone?.let { FinIcon("listo", "Marcar hecho", ActionColor.Done, onClick = it) }
        onDelete?.let { FinIcon("papelera", "Quitar", ActionColor.Delete, onClick = it) }
    }
}

@Composable
private fun SaveButton(enabled: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Box(
            Modifier.height(48.dp).width(120.dp)
                .background(if (enabled) LauncherAccent else GlassFill, RoundedCornerShape(24.dp))
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { Text("Guardar", style = MaterialTheme.typography.labelLarge, color = FinanceWhite) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Chips(options: List<Pair<String, String>>, selected: String?, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (key, label) ->
            val on = key == selected
            Text(
                label,
                Modifier
                    .background(if (on) LauncherAccent.copy(alpha = 0.25f) else GlassFill, RoundedCornerShape(14.dp))
                    .clickable(role = Role.Button) { onSelect(key) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.labelLarge,
                color = if (on) FinanceWhite else LabelCream,
            )
        }
    }
}

@Composable
private fun PaymentEditor(
    payment: ScheduledPayment?,
    accounts: List<BankAccount>,
    onDone: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onSave: (ScheduledPayment) -> Unit,
) {
    var name by remember { mutableStateOf(payment?.name ?: "") }
    var amount by remember { mutableStateOf(payment?.cents?.let { centsField(it) } ?: "") }
    var day by remember { mutableStateOf(payment?.day?.toString() ?: "") }
    var account by remember { mutableStateOf(payment?.accountId ?: "") }
    SheetHead(if (payment == null) "Nuevo pago" else payment.name, onDone, onDelete)
    ModuleTextField(name, { name = it.take(32) }, "Qué es (Spotify, renta…)", Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ModuleTextField(amount, { amount = amountInput(it) }, "Monto", Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), visualTransformation = PesosTransformation)
        ModuleTextField(day, { day = it.filter(Char::isDigit).take(2) }, "Día", Modifier.width(96.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
    }
    SectionLabel("Se paga con")
    Chips(accounts.filter { it.kind != BankKind.CASH }.map { it.id to it.name + if (it.kind == BankKind.CARD) " · crédito" else " · débito" }, account) { account = it }
    val cents = parseCents(amount) ?: 0
    val d = day.toIntOrNull()?.takeIf { it in 1..31 }
    SaveButton(name.isNotBlank() && cents > 0 && d != null) {
        onSave(ScheduledPayment(payment?.id ?: UUID.randomUUID().toString(), name.trim(), cents, account, d!!))
    }
}

@Composable
private fun IncomeEditor(plan: FinancePlan, onDelete: (() -> Unit)?, onSave: (Long, Int) -> Unit) {
    var amount by remember { mutableStateOf(plan.incomeCents.takeIf { it > 0 }?.let { centsField(it) } ?: "") }
    var day by remember { mutableStateOf(plan.incomeDay.takeIf { it > 0 }?.toString() ?: "") }
    SheetHead("Ingreso", onDelete = onDelete)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ModuleTextField(amount, { amount = amountInput(it) }, "Monto", Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), visualTransformation = PesosTransformation)
        ModuleTextField(day, { day = it.filter(Char::isDigit).take(2) }, "Día", Modifier.width(96.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
    }
    val cents = parseCents(amount) ?: 0
    SaveButton(cents > 0) { onSave(cents, day.toIntOrNull()?.takeIf { it in 1..31 } ?: 0) }
}

private val Topics = listOf("comida", "transporte", "café", "salidas", "compras", "salud")

@Composable
private fun CapEditor(tope: Tope?, onDelete: (() -> Unit)?, onSave: (String, BudgetPeriod, Long) -> Unit) {
    var topic by remember { mutableStateOf(tope?.topic ?: "") }
    var amount by remember { mutableStateOf(tope?.limitCents?.let { centsField(it) } ?: "") }
    var period by remember { mutableStateOf(tope?.period ?: BudgetPeriod.MONTH) }
    SheetHead(if (tope == null) "Nuevo tope" else tope.topic.replaceFirstChar { it.uppercase() }, onDelete = onDelete)
    SectionLabel("Tema")
    Chips(Topics.map { it to it.replaceFirstChar { c -> c.uppercase() } }, topic.lowercase()) { topic = it }
    ModuleTextField(topic, { topic = it.take(24) }, "Otro tema", Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
    SectionLabel("Hasta cuánto")
    ModuleTextField(amount, { amount = amountInput(it) }, "Monto", Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), visualTransformation = PesosTransformation)
    Chips(BudgetPeriod.entries.map { it.name to periodName(it) }, period.name) { period = BudgetPeriod.valueOf(it) }
    val cents = parseCents(amount) ?: 0
    SaveButton(topic.isNotBlank() && cents > 0) { onSave(topic.trim().lowercase(), period, cents) }
}

/** A cents amount as a field shows it: "199", "9888.19". */
private fun centsField(cents: Long): String = (cents / 100).toString() + if (cents % 100 == 0L) "" else "." + "%02d".format(cents % 100)
