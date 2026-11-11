package com.adrian.minimallauncher.feature.finance

import com.adrian.minimallauncher.ui.components.IconAction
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adrian.minimallauncher.data.FinanceRecord
import com.adrian.minimallauncher.data.MoneyKind
import com.adrian.minimallauncher.feature.CardLine
import com.adrian.minimallauncher.feature.DockItem
import com.adrian.minimallauncher.feature.LauncherAccent
import com.adrian.minimallauncher.feature.ModuleRow
import com.adrian.minimallauncher.feature.ModuleTitle
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.feature.bank.BankAccount
import com.adrian.minimallauncher.feature.bank.BankReceivable
import com.adrian.minimallauncher.feature.bank.BankView
import com.adrian.minimallauncher.feature.shortDay
import com.adrian.minimallauncher.ui.components.LocalReactions
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

private val spanish = Locale.forLanguageTag("es")
private val White = Color(0xFFF5F5F5)
private val Gray = FinanceMuted
private val EmptyDot = inactiveFinanceColor(FinanceTeal)
private const val TODAY_DOTS = 20
private const val TOPIC_DOTS = 10
private const val WEEK_ROWS = 10
private const val MOVEMENTS = 5

/** "12,345" for 12345. */
internal fun grouped(whole: Long): String = whole.toString().reversed().chunked(3).joinToString(",").reversed()

/** Pesos with thousands commas, without cents when they are round. */
fun formatPesos(cents: Long): String {
    val sign = if (cents < 0) "-" else ""
    val whole = abs(cents) / 100
    val rest = abs(cents) % 100
    return if (rest == 0L) "$sign$${grouped(whole)}" else "$sign$${grouped(whole)}.%02d".format(rest)
}

/** Whole pesos, rounded, with commas and a minus when negative: allowances split by days rarely come out round. */
internal fun wholePesos(cents: Long): String = (if (cents < 0) "-$" else "$") + grouped((abs(cents) + 50) / 100)

/** What an amount field keeps while typing: digits and one point with up to two decimals. */
internal fun amountInput(text: String): String {
    val clean = (if ('.' in text) text.replace(",", "") else text.replace(',', '.')).filter { it.isDigit() || it == '.' }
    val point = clean.indexOf('.')
    val whole = (if (point < 0) clean else clean.substring(0, point)).take(9)
    return if (point < 0) whole else whole + "." + clean.substring(point + 1).filter(Char::isDigit).take(2)
}

/** Shows an amount field as money while typing: "$12,345.5". The stored text stays plain digits. */
internal object PesosTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val point = raw.indexOf('.').let { if (it < 0) raw.length else it }
        val out = StringBuilder("$")
        // toOut[i] = where original offset i lands in the shown text.
        val toOut = IntArray(raw.length + 1)
        for (i in raw.indices) {
            if (i in 1 until point && (point - i) % 3 == 0) out.append(',')
            toOut[i] = out.length
            out.append(raw[i])
        }
        toOut[raw.length] = out.length
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = toOut[offset.coerceIn(0, raw.length)]
            override fun transformedToOriginal(offset: Int): Int {
                for (i in raw.length downTo 0) if (toOut[i] <= offset) return i
                return 0
            }
        }
        return TransformedText(AnnotatedString(out.toString()), mapping)
    }
}

/** The categories the entry sheet offers, plus any topic the user gave a budget. */
internal val BASE_CATEGORIES = listOf("comida", "transporte", "otro")

internal fun categoryIcon(category: String): String = when (category.lowercase(spanish)) {
    "comida" -> "colacion"
    "transporte" -> "traslado"
    else -> "otro"
}

internal fun periodIcon(period: BudgetPeriod): String = when (period) {
    BudgetPeriod.DAY -> "dia"
    BudgetPeriod.WEEK -> "semana"
    BudgetPeriod.MONTH -> "mes"
}

internal fun kindIcon(kind: MoneyKind): String = when (kind) {
    MoneyKind.INCOME_FIXED -> "ingreso"
    MoneyKind.TRANSFER -> "transferencia"
    MoneyKind.GIFT -> "regalo"
    MoneyKind.SERVICE -> "servicio"
    MoneyKind.FIXED -> "fijos"
    MoneyKind.EXTRA -> "extra"
    MoneyKind.SHARED -> "compartido"
    MoneyKind.MONTHLY -> "mes"
    MoneyKind.WITHDRAWAL -> "transferencia"
}

/**
 * Finanzas, three views behind the dock. Hoy: what is left today, the month and the week, budgets per topic and the
 * shared bills still owed. Semana: columns of dots (overspending lowers the days to come) and its movements. Mes:
 * where the extras bag comes from, spending by day or week, and its movements.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(
    summary: FinanceSummary,
    money: List<FinanceRecord>,
    dailyLimitCents: Long,
    financePlan: String,
    onSavePlan: (String) -> Unit,
    savingGoal: String,
    onSaveGoal: (String) -> Unit,
    onAdd: (kind: MoneyKind, cents: Long, description: String, category: String, pendingFromOthers: Long, account: String) -> Unit,
    onUpdate: (FinanceRecord) -> Unit,
    accounts: List<BankAccount>,
    receivables: List<BankReceivable>,
    onSaveAccount: (BankAccount) -> Unit,
    onDeleteAccount: (String) -> Unit,
    onSaveReceivable: (BankReceivable) -> Unit,
    onDeleteReceivable: (String) -> Unit,
    onCollectReceivable: (BankReceivable) -> Unit,
    onSettle: (id: String, paidCents: Long) -> Unit,
    onSetBudget: (topic: String, period: BudgetPeriod, cents: Long) -> Unit,
    onSavingPct: (Int) -> Unit,
    onDailyLimit: (Long) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** Opens the register as soon as Finanzas shows (the + on the Hoy widget). */
    openEntry: Boolean = false,
    onEntryShown: () -> Unit = {},
    /** The tab to open on: "hoy", "resumen", "banco" or "presupuesto". */
    startTab: String? = null,
) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(onSurfaceVariant = FinanceMuted),
    ) {
        var settingsOpen by rememberSaveable { mutableStateOf(false) }
        var goalOpen by rememberSaveable { mutableStateOf(false) }
        var paymentsOpen by rememberSaveable { mutableStateOf(false) }
        var entryOpen by rememberSaveable { mutableStateOf(false) }
        androidx.compose.runtime.LaunchedEffect(openEntry) { if (openEntry) { entryOpen = true; onEntryShown() } }
        // The movement the register is editing (from a swipe on a row), or null for a new one.
        var editing by remember { mutableStateOf<FinanceRecord?>(null) }
        var tab by rememberSaveable { mutableStateOf(startTab ?: "hoy") }
        // A route that names a tab moves there even when Finanzas is already open.
        androidx.compose.runtime.LaunchedEffect(startTab) { startTab?.let { tab = it } }
        var range by rememberSaveable { mutableStateOf("semana") }
        // Resumen's period: the day it is anchored on, and the picked range (empty when none).
        var anchorStr by rememberSaveable { mutableStateOf("") }
        var rangeFrom by rememberSaveable { mutableStateOf("") }
        var rangeTo by rememberSaveable { mutableStateOf("") }
        // The plan's fixed costs as a month each: they take from what the bank holds.
        val fixedLines = summary.fixed.map { cost ->
            val paid = money.filter { it.kind == MoneyKind.FIXED && Budget.categoryOf(it).equals(cost.topic.trim(), ignoreCase = true) && java.time.YearMonth.from(Budget.dateOf(it)) == java.time.YearMonth.from(summary.date) }.sumOf { it.amountCents }
            cost.topic to (FinancePlan.perMonth(cost.limitCents, cost.period, summary.date.lengthOfMonth()) - paid).coerceAtLeast(0)
        }
        val anchor = anchorStr.takeIf { it.isNotBlank() }?.let(LocalDate::parse) ?: summary.date
        // Real, planned and caps, in one place for every tab (see FinanceMoney).
        val plan = remember(financePlan) { FinancePlan.decode(financePlan) }
        val moneyView = remember(accounts, plan, summary, money) { FinanceMoney.of(accounts, plan, summary, money, summary.date) }
        // A scheduled payment marked done is recorded as that payment, on its account.
        val markDone: (PlanItem) -> Unit = { item ->
            item.payment?.let { p -> onAdd(MoneyKind.FIXED, p.cents, p.name, p.category, 0, p.accountId) }
        }
        // Bumped to draw an icon once: the day after an extra, the shared bills after one is settled.
        var dayPlay by remember { mutableIntStateOf(0) }
        var sharedPlay by remember { mutableIntStateOf(0) }
        val reactions = LocalReactions.current

        if (goalOpen || paymentsOpen) {
            val close = { goalOpen = false; paymentsOpen = false }
            BackHandler(onBack = close)
            Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                FinanceBackHeader(if (goalOpen) "Meta de ahorro" else "Pagos vinculados", close)
                if (goalOpen) SavingGoalView(savingGoal, summary.date, onSaveGoal)
                else ScheduledPaymentsView(FinancePlan.decode(financePlan), accounts, money, summary.date,
                    onSave = { onSavePlan(it.encode()) },
                    onRecord = { payment -> onAdd(MoneyKind.FIXED, payment.cents, payment.name, payment.category, 0, payment.accountId) })
            }
            return@MaterialTheme
        }
        if (settingsOpen) {
            FinanceGear(savingGoal, onSaveGoal, summary.date, { settingsOpen = false }, modifier)
            return@MaterialTheme
        }
        BackHandler(onBack = onBack)
        val movementActions = MovementActions(onEdit = { record -> editing = record; entryOpen = true }, onDelete = onDelete)
        CompositionLocalProvider(LocalMovementActions provides movementActions) {
        Box(modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).padding(horizontal = 24.dp).padding(bottom = if (tab == "banco") 0.dp else 76.dp)) {
                    val title = when (tab) {
                        "resumen" -> "Resumen"
                        "banco" -> "Banco"
                        "presupuesto" -> "Presupuesto"
                        else -> "Finanzas"
                    }
                    ModuleTitle(title, { settingsOpen = true })

                    key(tab) {
                    Column(
                        Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 12.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        when (tab) {
                            "resumen" -> {
                                PastAndComing(moneyView, money)
                                PeriodView(
                                summary, money, accounts, range, { range = it },
                                anchor, { anchorStr = it.toString() },
                                rangeFrom.takeIf { it.isNotBlank() }?.let(LocalDate::parse), rangeTo.takeIf { it.isNotBlank() }?.let(LocalDate::parse),
                                { from, to -> rangeFrom = from?.toString().orEmpty(); rangeTo = to?.toString().orEmpty() },
                               )
                            }
                            "banco" -> {
                                BankView(
                                    money = money,
                                    today = summary.date,
                                    accounts = accounts,
                                    receivables = receivables,
                                    fixedLines = fixedLines,
                                    onSaveAccount = onSaveAccount,
                                    onDeleteAccount = onDeleteAccount,
                                    onSaveReceivable = onSaveReceivable,
                                    onDeleteReceivable = onDeleteReceivable,
                                    onCollect = onCollectReceivable,
                                    onOpenScheduledPayments = { paymentsOpen = true },
                                )
                            }
                            "presupuesto" -> FinancePlanView(
                                view = moneyView,
                                plan = plan,
                                accounts = accounts,
                                onSavePlan = { onSavePlan(it.encode()); reactions.tick() },
                                onSetBudget = { topic, period, cents -> onSetBudget(topic, period, cents); reactions.tick() },
                                onMarkDone = markDone,
                                onOpenBank = { tab = "banco" },
                                savingGoal = savingGoal,
                                onSaveGoal = { onSaveGoal(it); reactions.tick() },
                                onSaveAccount = onSaveAccount,
                            )
                            else -> {
                                FinanceHoy(
                                    view = moneyView,
                                    money = money,
                                    onMarkDone = markDone,
                                    onOpenBank = { tab = "banco" },
                                    onOpenPlan = { tab = "presupuesto" },
                                )
                                if (money.isNotEmpty()) FinanceSection(spacing = 4.dp) {
                                    SectionLabel("Movimientos")
                                    Movements(money, limit = 5)
                                }
                                if (summary.sharedOpen.isNotEmpty()) SharedCard(summary, sharedPlay) { record ->
                                    onSettle(record.id, record.pendingFromOthersCents)
                                    sharedPlay++
                                    reactions.tick()
                                }
                            }
                        }
                    }
                }
                }
                FinanceDock(
                    listOf(
                        DockItem("hoy", "Hoy", "hoy", badge = summary.sharedOpen.size.takeIf { it > 0 }?.toString()),
                        DockItem("resumen", "Resumen", "semana"),
                        DockItem("banco", "Banco", "banco"),
                        DockItem("presupuesto", "Presupuesto", "presupuesto"),
                    ),
                    tab,
                    onOpen = { key -> tab = key },
                )
            }
            if (!entryOpen && tab != "presupuesto" && tab != "banco") {
                AddButton(Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 92.dp)) { editing = null; entryOpen = true }
            }
            if (entryOpen) ModalBottomSheet(
                onDismissRequest = { entryOpen = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = com.adrian.minimallauncher.feature.CardFill,
            ) {
                EntrySheet(summary, accounts, initial = editing, payments = FinancePlan.decode(financePlan).payments, onClose = { entryOpen = false }) { kind, cents, description, category, pending, account ->
                    val edit = editing
                    if (edit != null) {
                        // An edit keeps the id, the date and the meal linked to it.
                        onUpdate(edit.copy(kind = kind, amountCents = cents, description = description, category = category, pendingFromOthersCents = pending, account = account))
                        reactions.tick()
                    } else {
                        val fixedLine = summary.fixed.firstOrNull { kind == MoneyKind.FIXED && it.topic == category }
                        val over = ((kind == MoneyKind.EXTRA || kind == MoneyKind.SHARED) && Budget.wouldExceed(summary, category, cents)) ||
                            (fixedLine != null && Budget.fixedLeft(fixedLine, cents) < 0)
                        onAdd(kind, cents, description, category, pending, account)
                        if (kind == MoneyKind.EXTRA || kind == MoneyKind.SHARED) dayPlay++
                        if (kind == MoneyKind.SHARED && pending > 0) sharedPlay++
                        if (over) reactions.reject() else reactions.tick()
                    }
                    entryOpen = false
                }
            }
        }
        }
    }
}

private fun dayTitle(summary: FinanceSummary): String {
    val date = summary.date
    val day = date.dayOfWeek.getDisplayName(TextStyle.FULL, spanish).replaceFirstChar { it.uppercase(spanish) }
    return "$day, ${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.FULL, spanish)}"
}

private fun monthTitle(summary: FinanceSummary) = summary.date.month.getDisplayName(TextStyle.FULL, spanish).replaceFirstChar { it.uppercase(spanish) }

private val spentKinds = setOf(MoneyKind.EXTRA, MoneyKind.SHARED, MoneyKind.FIXED, MoneyKind.MONTHLY)

/** Each record with its kind's icon, newest first; money in gets a plus. "Ver más" / "Mostrar menos" unfolds progressively. */
@Composable
internal fun Movements(records: List<FinanceRecord>, limit: Int = MOVEMENTS, onMore: (() -> Unit)? = null) {
    var expandedAll by rememberSaveable(records.size, limit) { mutableStateOf(false) }
    val sorted = remember(records) { records.sortedByDescending { it.createdAt } }
    val effectiveLimit = if (expandedAll) Int.MAX_VALUE else limit
    val latest = sorted.take(effectiveLimit)
    if (latest.isEmpty()) {
        Text("Sin movimientos", Modifier.padding(vertical = 12.dp), style = MaterialTheme.typography.bodyMedium, color = FinanceWhite)
        return
    }
    val actions = LocalMovementActions.current
    Column {
        latest.forEach { record ->
            val out = record.kind in spentKinds
            val row: @Composable () -> Unit = {
                ModuleRow(
                    if (Budget.isExtra(record)) categoryIcon(record.category) else kindIcon(record.kind),
                    record.description.ifBlank { record.category.replaceFirstChar { it.uppercase(spanish) } },
                    shortDay(Budget.dateOf(record)) + if (record.kind == MoneyKind.WITHDRAWAL) " · Débito → efectivo" else "",
                    (if (record.kind == MoneyKind.WITHDRAWAL) "↔ " else if (out) "−" else "+") + wholePesos(record.amountCents),
                    valueColor = FinanceWhite,
                )
            }
            if (actions == null) row() else SwipeableMovement(record, actions, row)
        }
        if (onMore != null && records.size > latest.size) {
            MoreChevron(false, Modifier.clickable(role = Role.Button, onClickLabel = "Ver más", onClick = onMore).padding(vertical = 10.dp))
        } else if (onMore == null && records.size > limit) {
            MoreChevron(expandedAll, Modifier.clickable(role = Role.Button, onClickLabel = if (expandedAll) "Mostrar menos" else "Ver más") { expandedAll = !expandedAll }.padding(vertical = 10.dp))
        }
    }
}

/** What a swiped movement can do: edit it, or delete it once the user confirms. Null where rows are only read. */
internal class MovementActions(val onEdit: (FinanceRecord) -> Unit, val onDelete: (String) -> Unit)

internal val LocalMovementActions = compositionLocalOf<MovementActions?> { null }

/**
 * A movement row that swipes: to the right opens it in the register to edit; to the left asks, then deletes. The
 * row always snaps back, so nothing leaves the list until a delete is confirmed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SwipeableMovement(record: FinanceRecord, actions: MovementActions, row: @Composable () -> Unit) {
    // The swipe state outlives an edit of the row, so the callbacks read the latest record.
    val current by rememberUpdatedState(record)
    var askDelete by remember { mutableStateOf(false) }
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                // Derecha: eliminar (con confirmación). Izquierda: editar o categorizar.
                SwipeToDismissBoxValue.StartToEnd -> askDelete = true
                SwipeToDismissBoxValue.EndToStart -> actions.onEdit(current)
                SwipeToDismissBoxValue.Settled -> Unit
            }
            false
        },
    )
    SwipeToDismissBox(
        state = state,
        backgroundContent = { SwipeBackground(state.dismissDirection) },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
    ) {
        Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) { row() }
    }
    if (askDelete) {
        AlertDialog(
            onDismissRequest = { askDelete = false },
            confirmButton = {
                IconAction("papelera", "Borrar") { actions.onDelete(current.id); askDelete = false }
            },
            dismissButton = {
                IconAction("no", "Cancelar") { askDelete = false }
            },
            text = { Text(current.description.ifBlank { current.category }.ifBlank { "Movimiento" }, style = MaterialTheme.typography.bodyMedium, color = White) },
            containerColor = Color(0xFF111111),
        )
    }
}

/** Lo que aparece detrás de la fila deslizada: tinte sutil con el ícono; rosa para eliminar, azul para editar. */
@Composable
private fun SwipeBackground(direction: SwipeToDismissBoxValue) {
    val delete = direction == SwipeToDismissBoxValue.StartToEnd
    val edit = direction == SwipeToDismissBoxValue.EndToStart
    Box(
        Modifier.fillMaxSize()
            .background(if (delete) Color(0xFF2A0A12) else if (edit) Color(0xFF0B2233) else Color.Transparent)
            .padding(horizontal = 20.dp),
        contentAlignment = if (delete) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        if (delete) StrokeIcon("papelera", StrokeState.NOW, 24.dp, color = Color(0xFFFF2962))
        if (edit) StrokeIcon("lapiz", StrokeState.NOW, 24.dp, color = LauncherAccent)
    }
}

/**
 * One day as Hoy shows it: collapsible category availability and collapsible movements list with progressive disclosure.
 */
@Composable
internal fun DayBudget(summary: FinanceSummary, money: List<FinanceRecord>, day: LocalDate) {
    val rows = money.filter { Budget.dateOf(it) == day }
    val sections = BudgetSections.of(summary, money, Span.DAY, day)
        .filter { it.kind == SectionKind.FREE || it.kind == SectionKind.TOPIC }
    var categoriesExpanded by rememberSaveable(day.toString()) { mutableStateOf(false) }
    var movementsExpanded by rememberSaveable(day.toString()) { mutableStateOf(true) }
    val totalLeft = sections.sumOf { it.leftCents }

    FinanceCollapsible(
        title = "Disponible por categoría (${sections.size})",
        subtitle = sections.joinToString(" · ") { "${it.label}: ${wholePesos(it.leftCents)}" },
        summaryText = wholePesos(totalLeft),
        summaryColor = if (totalLeft < 0) MoneyOver else FinanceTeal,
        expanded = categoriesExpanded,
        onToggle = { categoriesExpanded = !categoriesExpanded },
    ) {
        sections.forEach { section ->
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(section.label, Modifier.weight(1f), color = Gray, style = MaterialTheme.typography.bodyMedium)
                Text(wholePesos(section.leftCents), color = if (section.over) MoneyOver else White, style = MaterialTheme.typography.labelLarge)
            }
        }
        val monthly = rows.filter { it.kind == MoneyKind.FIXED || it.kind == MoneyKind.MONTHLY }.sumOf { it.amountCents }
        if (monthly > 0) {
            FinanceReadMore("${wholePesos(monthly)} en pagos fijos o del mes · fuera del gasto diario")
        }
    }

    FinanceCollapsible(
        title = if (day == summary.date) "Movimientos de hoy (${rows.size})" else "Movimientos del día (${rows.size})",
        subtitle = if (rows.isEmpty()) "Sin registros hoy" else "Desliza una fila para editar o borrar",
        expanded = movementsExpanded,
        onToggle = { movementsExpanded = !movementsExpanded },
    ) {
        Movements(rows, limit = 3)
    }
}

/** [count] dots: lit as [spent] fills [allowance], in the money's state color (red once past it). */
@Composable
private fun DotBar(spent: Long, allowance: Long, count: Int, modifier: Modifier = Modifier) {
    val state = usageColor(spent, allowance)
    val lit = when {
        spent <= 0 -> 0
        allowance <= 0 -> count
        else -> ((spent * count + allowance - 1) / allowance).toInt().coerceAtMost(count)
    }
    Row(modifier, Arrangement.spacedBy(4.dp)) {
        for (i in 0 until count) {
            val color = when {
                i >= lit -> EmptyDot
                else -> state
            }
            Box(Modifier.size(7.dp).background(color, CircleShape))
        }
    }
}

/** The month in three: the saving, everything spent, and what is left of the income after both and the fixed costs. */
@Composable
private fun PeriodStats(summary: FinanceSummary) {
    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
        Stat("ahorro", "Ahorro", StrokeState.DONE, summary.savingCents, "Ahorro del mes", SavingGreen, Modifier.weight(1f))
        Stat("pago", "Gastado", StrokeState.DONE, summary.spentMonthCents, "Gastado en el mes", White, Modifier.weight(1f))
        Stat("bolsa", "Te queda", StrokeState.DONE, summary.moneyLeftCents, "Te queda del mes", leftColor(summary.moneyLeftCents, (summary.fixedIncomeCents - summary.savingCents).coerceAtLeast(1)), Modifier.weight(1f))
    }
}

@Composable
private fun Stat(icon: String, label: String, state: StrokeState, cents: Long, description: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.semantics(mergeDescendants = true) { contentDescription = "$description ${wholePesos(cents)}" },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FinanceAmount(cents, size = 20, color = color)
        SmallLabel(label)
    }
}

/**
 * The fixed income as one bar: steel blue fixed costs, hatched sage savings, teal extras spent.
 * Transfers, gifts and services sit apart on the right: received, never budgeted.
 */
@Composable
internal fun IncomeBar(summary: FinanceSummary) {
    val total = maxOf(summary.fixedIncomeCents, summary.fixedCostsCents + summary.savingCents + summary.bagCents).coerceAtLeast(1)
    val spentBag = minOf(summary.bagCents, summary.extrasMonthCents).coerceAtLeast(0)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Canvas(Modifier.fillMaxWidth().height(10.dp)) {
            val w = size.width
            val h = size.height
            val gap = 2.dp.toPx()
            drawRoundRect(EmptyDot, size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2))
            var x = 0f
            fun seg(cents: Long, draw: (Float, Float) -> Unit) {
                if (cents <= 0) return
                val width = w * cents / total
                draw(x, (width - gap).coerceAtLeast(1f))
                x += width
            }
            seg(summary.fixedCostsCents) { at, width -> drawRect(FinanceBlue, Offset(at, 0f), Size(width, h)) }
            seg(summary.savingCents) { at, width ->
                clipRect(at, 0f, at + width, h) {
                    var line = at - h
                    while (line < at + width) {
                        drawLine(SavingGreen, Offset(line, h), Offset(line + h, 0f), strokeWidth = 2.dp.toPx())
                        line += 5.dp.toPx()
                    }
                }
            }
            seg(spentBag) { at, width -> drawRect(FinanceTeal, Offset(at, 0f), Size(width, h)) }
        }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
            Leg("ingreso", "Ingreso", StrokeState.DONE, summary.fixedIncomeCents, "Ingreso fijo")
            Leg("fijos", "Fijos", StrokeState.OFF, summary.fixedCostsCents, "Gastos fijos")
            Leg("ahorro", "Ahorro", StrokeState.DONE, summary.savingCents, "Ahorro")
            Leg("bolsa", "Bolsa", StrokeState.NOW, summary.bagCents, "Bolsa de extras")
            Box(Modifier.width(1.dp).height(40.dp).background(CardLine))
            Leg("transferencia", "Aparte", StrokeState.OFF, summary.otherIncomeCents, "Ingresos aparte")
        }
    }
}

@Composable
private fun Leg(icon: String, label: String, state: StrokeState, cents: Long, description: String) {
    Column(
        Modifier.semantics(mergeDescendants = true) { contentDescription = "$description ${wholePesos(cents)}" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StrokeIcon(icon, state, 20.dp, color = when (icon) {
            "fijos" -> FinanceBlue
            "ahorro" -> SavingGreen
            "bolsa" -> FinanceTeal
            else -> FinanceMist
        })
        Text(
            ((cents + 50) / 100).toString(),
            style = MaterialTheme.typography.labelSmall,
            color = if (state == StrokeState.OFF) Gray else MaterialTheme.colorScheme.onSurface,
        )
        SmallLabel(label)
    }
}

/** The red + over the dock that opens the register; it sinks a little while pressed. */
@Composable
private fun AddButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val source = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(if (pressed) 0.97f else 1f, label = "add")
    // Solo el signo +, en círculo azul (diseño 2026-11-09): sin texto.
    Box(
        modifier
            .size(56.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(LauncherAccent, CircleShape)
            .clickable(source, null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Registrar movimiento" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(24.dp)) {
            val w = 2.4.dp.toPx()
            drawLine(Color.White, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), w, androidx.compose.ui.graphics.StrokeCap.Round)
            drawLine(Color.White, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), w, androidx.compose.ui.graphics.StrokeCap.Round)
        }
    }
}

/** Chevron que abre (abajo) o cierra (arriba) una lista; sin texto. */
@Composable
internal fun MoreChevron(expanded: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier.size(22.dp)) {
        val y = size.height * if (expanded) 0.62f else 0.38f
        val mid = size.height - y
        drawLine(Color.White, Offset(size.width * 0.22f, y), Offset(size.width * 0.5f, mid), 2.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(Color.White, Offset(size.width * 0.78f, y), Offset(size.width * 0.5f, mid), 2.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

/**
 * The plan's fixed costs: dots of what went in (today's share for daily and weekly ones, the month for monthly),
 * what is left, and its period. A monthly one fully paid shows a check.
 */
@Composable
private fun FixedLines(fixed: List<TopicStatus>) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        fixed.forEach { line ->
            val left = Budget.fixedLeft(line)
            val monthly = line.period == BudgetPeriod.MONTH
            val paid = monthly && line.periodSpentCents >= line.periodTotalCents
            Row(
                Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "${line.topic}: quedan ${wholePesos(left)}" },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StrokeIcon("fijos", if (left < 0) StrokeState.NOW else StrokeState.DONE, 24.dp)
                SmallLabel(line.topic, Modifier.width(72.dp), maxLines = 1)
                if (monthly) DotBar(line.periodSpentCents, line.periodTotalCents, TOPIC_DOTS, Modifier.weight(1f))
                else DotBar(line.todaySpentCents, line.todayAllowanceCents, TOPIC_DOTS, Modifier.weight(1f))
                if (paid && left >= 0) StrokeIcon("listo", StrokeState.DONE, 18.dp, color = MoneyPlenty)
                else Text(
                    wholePesos(left), style = MaterialTheme.typography.labelLarge,
                    color = if (monthly) usageColor(line.periodSpentCents, line.periodTotalCents) else usageColor(line.todaySpentCents, line.todayAllowanceCents),
                )
                StrokeIcon(periodIcon(line.period), StrokeState.OFF, 16.dp)
            }
        }
    }
}

/** Por cobrar: collapsible section for shared bills; tapping a bill marks it paid back. */
@Composable
private fun SharedCard(summary: FinanceSummary, play: Int, onSettle: (FinanceRecord) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    FinanceCollapsible(
        title = "Por cobrar (${summary.sharedOpen.size})",
        subtitle = "Cuentas compartidas pendientes · toca una para marcar cobrada",
        icon = "compartido",
        summaryText = wholePesos(summary.pendingFromOthersCents),
        summaryColor = FinanceMist,
        expanded = expanded,
        onToggle = { expanded = !expanded },
    ) {
        summary.sharedOpen.forEach { record ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { onSettle(record) }
                    .semantics(mergeDescendants = true) { contentDescription = "Marcar cobrada ${record.description}" }
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StrokeIcon(categoryIcon(record.category), StrokeState.OFF, 18.dp)
                SmallLabel(record.description.ifBlank { record.category }, Modifier.weight(1f), maxLines = 1)
                Text(wholePesos(record.amountCents), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(wholePesos(record.pendingFromOthersCents), Modifier.width(60.dp), style = MaterialTheme.typography.labelMedium, color = FinanceMist, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                StrokeIcon("listo", StrokeState.OFF, 16.dp)
            }
        }
    }
}
