package com.adrian.minimallauncher.feature.bank

import com.adrian.minimallauncher.feature.finance.Dot
import com.adrian.minimallauncher.feature.finance.GlassFill
import com.adrian.minimallauncher.feature.finance.Pop
import com.adrian.minimallauncher.feature.finance.PopRow
import com.adrian.minimallauncher.feature.finance.PopRule
import com.adrian.minimallauncher.feature.finance.glass
import com.adrian.minimallauncher.feature.finance.tinted
import com.adrian.minimallauncher.feature.finance.usageColor
import com.adrian.minimallauncher.feature.finance.MoneyPlenty
import com.adrian.minimallauncher.feature.finance.MoneyLow
import com.adrian.minimallauncher.feature.finance.FinanceAmount
import androidx.compose.foundation.layout.fillMaxHeight
import com.adrian.minimallauncher.feature.finance.FinanceSection
import com.adrian.minimallauncher.feature.finance.FinanceCard
import com.adrian.minimallauncher.feature.finance.MoneyLow
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.FinanceRecord
import com.adrian.minimallauncher.data.MoneyKind
import com.adrian.minimallauncher.feature.CardFill
import com.adrian.minimallauncher.feature.CardLine
import com.adrian.minimallauncher.feature.LauncherAccent
import com.adrian.minimallauncher.feature.ModuleTextField
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.feature.finance.Budget
import com.adrian.minimallauncher.feature.finance.FinanceAmount
import com.adrian.minimallauncher.feature.finance.FinanceBlue
import com.adrian.minimallauncher.feature.finance.FinanceCollapsible
import com.adrian.minimallauncher.feature.finance.FinanceDropdown
import com.adrian.minimallauncher.feature.finance.FinanceLavender
import com.adrian.minimallauncher.feature.finance.FinanceMist
import com.adrian.minimallauncher.feature.finance.FinanceMuted
import com.adrian.minimallauncher.feature.finance.FinanceReadMore
import com.adrian.minimallauncher.feature.finance.FinanceRule
import com.adrian.minimallauncher.feature.finance.FinanceTeal
import com.adrian.minimallauncher.feature.finance.MoneyGoing
import com.adrian.minimallauncher.feature.finance.MoneyOver
import com.adrian.minimallauncher.feature.finance.MoneyPlenty
import com.adrian.minimallauncher.feature.finance.Movements
import com.adrian.minimallauncher.feature.finance.PesosTransformation
import com.adrian.minimallauncher.feature.finance.amountInput
import com.adrian.minimallauncher.feature.finance.parseCents
import com.adrian.minimallauncher.feature.finance.wholePesos
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
import java.text.Normalizer
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID

private val White = Color(0xFFF5F5F5)
private val Dim = FinanceBlue.copy(alpha = 0.32f)

/** What the editor is changing: an account or card (null when new) or a money still to come in. */
private sealed interface BankEdit {
    data class Account(val account: BankAccount?, val kind: BankKind) : BankEdit
    data class Receivable(val receivable: BankReceivable?) : BankEdit
}

private enum class AddAction { CARD, DEBIT, CASH, RECEIVABLE }

/**
 * Banco con divulgación progresiva (progressive disclosure):
 * - Resumen superior compacto con "Mi dinero" y "Libre" a primera vista, y desglose + formas de pago colapsables.
 * - Secciones claras a primera vista (Cuentas y efectivo, Tarjetas de crédito, Por cobrar) donde cada elemento
 *   se despliega en acordeón (inline) en lugar de abrir una vista aparte.
 * - Menú desplegable "+ Agregar" para crear cuentas, tarjetas o cobros sin saturar la vista con botones fijos.
 */
@Composable
internal fun BankView(
    money: List<FinanceRecord>,
    today: LocalDate,
    accounts: List<BankAccount>,
    receivables: List<BankReceivable>,
    fixedLines: List<Pair<String, Long>>,
    onSaveAccount: (BankAccount) -> Unit,
    onDeleteAccount: (String) -> Unit,
    onSaveReceivable: (BankReceivable) -> Unit,
    onDeleteReceivable: (String) -> Unit,
    onCollect: (BankReceivable) -> Unit,
    onOpenScheduledPayments: (() -> Unit)? = null,
) {
    var editing by remember { mutableStateOf<BankEdit?>(null) }
    // Accordion state: only one account/card expanded at a time so most stay visible at first glance.
    var expandedAccountId by rememberSaveable { mutableStateOf<String?>(null) }
    // La cuenta abierta en su propia vista (al tocar su cuadro en la cuadrícula).
    var openAccountId by rememberSaveable { mutableStateOf<String?>(null) }
    var breakdownOpen by rememberSaveable { mutableStateOf(false) }
    var receivablesOpen by rememberSaveable { mutableStateOf(false) }
    var orphansOpen by rememberSaveable { mutableStateOf(false) }

    val debitAndCash = accounts.filter { it.kind == BankKind.DEBIT || it.kind == BankKind.CASH }
    val cards = accounts.filter { it.kind == BankKind.CARD }
    val hasCash = accounts.any { it.id == Bank.CASH_ACCOUNT_ID || it.kind == BankKind.CASH }

    val addOptions = buildList {
        add(AddAction.DEBIT to "Cuenta de débito")
        add(AddAction.CARD to "Tarjeta de crédito")
        if (!hasCash) add(AddAction.CASH to "Efectivo")
        add(AddAction.RECEIVABLE to "Por cobrar")
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val openAccount = accounts.firstOrNull { it.id == openAccountId }
        if (openAccount != null) {
            BackHandler { openAccountId = null }
            BankAccountScreen(
                account = openAccount,
                records = money.filter { it.account == openAccount.id || (openAccount.id == Bank.CASH_ACCOUNT_ID && it.kind == MoneyKind.WITHDRAWAL) },
                today = today,
                onBack = { openAccountId = null },
                onEdit = { editing = BankEdit.Account(openAccount, openAccount.kind) },
            )
        }
        if (openAccount == null) {
        // 1. Lo que hay en todas las cuentas y lo que se debe en todas las tarjetas; un toque da el detalle.
        BankTotals(accounts, today)

        }

        // Inline collapsible editor when adding or editing (does not wipe out the whole view)
        // The editor comes out as a box over the page, where the pencil (or +) was tapped; no scrolling to find it.
        editing?.let { edit ->
            androidx.compose.ui.window.Dialog(onDismissRequest = { editing = null }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF141414), RoundedCornerShape(22.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
                    .padding(18.dp),
            ) {
                BankEditor(
                    edit = edit,
                    takenIds = accounts.map { it.id }.toSet(),
                    canDelete = (edit as? BankEdit.Account)?.account?.let { account ->
                        money.none { it.account == account.id || (account.id == Bank.CASH_ACCOUNT_ID && it.kind == MoneyKind.WITHDRAWAL) }
                    } ?: true,
                    onSaveAccount = { saved -> onSaveAccount(saved); editing = null },
                    onSaveReceivable = { saved -> onSaveReceivable(saved); editing = null },
                    onDelete = { id ->
                        if (edit is BankEdit.Account) onDeleteAccount(id) else onDeleteReceivable(id)
                        editing = null
                    },
                    onCollect = { receivable -> onCollect(receivable); editing = null },
                    onClose = { editing = null },
                )
            }
            }
        }

        if (openAccount == null) {
        // 2. Section: Cuentas y efectivo (Débito + Efectivo) — compact accordion rows
        val totalLiquid = debitAndCash.sumOf { it.balanceCents }
        FinanceSection(spacing = 10.dp) {
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                com.adrian.minimallauncher.feature.finance.SectionLabel("Cuentas", Modifier.weight(1f))
                Pop(description = "Agregar", pop = { close ->
                    addOptions.forEach { (action, label) ->
                        Box(
                            Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(role = Role.Button) {
                                close()
                                editing = when (action) {
                                    AddAction.CARD -> BankEdit.Account(null, BankKind.CARD)
                                    AddAction.DEBIT -> BankEdit.Account(null, BankKind.DEBIT)
                                    AddAction.CASH -> BankEdit.Account(Bank.cashAccount(), BankKind.CASH)
                                    AddAction.RECEIVABLE -> BankEdit.Receivable(null)
                                }
                            },
                            contentAlignment = Alignment.CenterStart,
                        ) { Text(label, style = MaterialTheme.typography.bodyMedium, color = White) }
                    }
                }) {
                    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { StrokeIcon("nueva", StrokeState.DONE, 22.dp) }
                }
            }
            if (debitAndCash.isEmpty()) {
                Text(
                    "Sin cuentas",
                    style = MaterialTheme.typography.bodySmall,
                    color = FinanceMuted,
                )
            }
            debitAndCash.sortedByDescending { it.balanceCents }.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { account ->
                        AccountTile(
                            account = account,
                            selected = expandedAccountId == account.id,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            balanceColor = White,
                        ) { openAccountId = account.id }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            debitAndCash.firstOrNull { it.id == expandedAccountId }?.let { account ->
                val accountRecords = remember(money, account.id) {
                    money.filter { it.account == account.id || (account.id == Bank.CASH_ACCOUNT_ID && it.kind == MoneyKind.WITHDRAWAL) }
                }
                AccountAccordionDetail(
                    account = account,
                    records = accountRecords,
                    today = today,
                    onEdit = { editing = BankEdit.Account(account, account.kind) },
                )
            }
        }

        // 3. Section: Tarjetas de crédito — compact accordion rows visible at first glance
        val totalOwed = cards.sumOf { it.balanceCents }
        FinanceSection(spacing = 10.dp) {
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                com.adrian.minimallauncher.feature.finance.SectionLabel("Tarjetas", Modifier.weight(1f))
            }
            if (cards.isEmpty()) {
                Text(
                    "Sin tarjetas registradas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = FinanceMuted,
                )
            }
            cards.sortedByDescending { it.balanceCents }.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { card ->
                        AccountTile(
                            account = card,
                            selected = expandedAccountId == card.id,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            balanceColor = White,
                            hint = Bank.rolled(card, today).let { c ->
                                c.due?.takeIf { c.dueCents > 0 }?.let { "${wholePesos(c.dueCents)} · ${com.adrian.minimallauncher.feature.shortDay(it)}" }
                            },
                        ) { openAccountId = card.id }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            cards.firstOrNull { it.id == expandedAccountId }?.let { card ->
                val cardRecords = remember(money, card.id) { money.filter { it.account == card.id } }
                AccountAccordionDetail(
                    account = card,
                    records = cardRecords,
                    today = today,
                    onEdit = { editing = BankEdit.Account(card, card.kind) },
                )
            }
        }

        // 4. Section: Por cobrar (Collapsible)
        val totalReceivables = receivables.sumOf { it.amountCents }
        FinanceSection(spacing = 0.dp) {
        FinanceCollapsible(
            title = "Por cobrar (${receivables.size})",
            icon = "ingreso",
            summaryText = wholePesos(totalReceivables),
            summaryColor = if (totalReceivables > 0) MoneyGoing else FinanceMuted,
            expanded = receivablesOpen,
            onToggle = { receivablesOpen = !receivablesOpen },
        ) {
            if (receivables.isEmpty()) {
                Text("No tienes cobros pendientes.", style = MaterialTheme.typography.bodySmall, color = FinanceMuted)
            }
            receivables.forEach { receivable ->
                ReceivableRow(
                    receivable = receivable,
                    onCollect = { onCollect(receivable) },
                    onEdit = { editing = BankEdit.Receivable(receivable) },
                )
            }
        }
        }

        // 5. Section: Pendientes de vincular (only if orphan movements exist, collapsible)
        val known = accounts.map { it.id }.toSet()
        val orphans = money.filter { it.account.isNotBlank() && it.account !in known }
        if (orphans.isNotEmpty()) {
            FinanceCollapsible(
                title = "Pendientes de vincular (${orphans.size})",
                icon = "aviso",
                summaryText = wholePesos(orphans.sumOf { it.amountCents }),
                summaryColor = FinanceMist,
                expanded = orphansOpen,
                onToggle = { orphansOpen = !orphansOpen },
            ) {
                Movements(orphans.sortedByDescending { it.createdAt }, limit = 5)
            }
        }
        }
    }
}

private fun cardDatesHint(card: BankAccount, today: LocalDate): String {
    val parts = mutableListOf<String>()
    if (card.payDay in 1..31) {
        val left = Bank.nextDate(card.payDay, today)?.let { ChronoUnit.DAYS.between(today, it) }
        parts += when (left) {
            0L -> "Pago hoy"
            1L -> "Pago mañana"
            in 2L..5L -> "Pago en $left d"
            else -> "Pago día ${card.payDay}"
        }
    }
    return if (parts.isEmpty()) "Crédito" else parts.joinToString(" · ")
}

/**
 * Inline accordion detail for a single account or credit card: shows dates, monthly activity, edit action,
 * and a collapsible movements list so the user never leaves the main Bank view.
 */
@Composable
private fun AccountAccordionDetail(
    account: BankAccount,
    records: List<FinanceRecord>,
    today: LocalDate,
    onEdit: () -> Unit,
) {
    val month = YearMonth.from(today)
    val monthRecords = remember(records, month) { records.filter { YearMonth.from(Budget.dateOf(it)) == month } }
    val spentMonth = monthRecords.filter { it.kind in spentKinds }.sumOf { it.amountCents }
    val incomeMonth = monthRecords.filter { it.kind.isIncome }.sumOf { it.amountCents }
    var showMovements by rememberSaveable(account.id) { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF101010), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (account.kind == BankKind.CARD) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.weight(1f)) { DateLine("mes", account.cutDay, today) }
                Box(Modifier.weight(1f)) { DateLine("pago", account.payDay, today) }
            }
            if (account.owedByOthersCents > 0) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    SmallLabel("Te deben de esta tarjeta")
                    Text(wholePesos(account.owedByOthersCents), style = MaterialTheme.typography.labelLarge, color = MoneyPlenty)
                }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    SmallLabel("Deuda propia")
                    Text(wholePesos((account.balanceCents - account.owedByOthersCents).coerceAtLeast(0)), style = MaterialTheme.typography.labelLarge, color = White)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            SmallLabel("Gastado este mes")
            Text(wholePesos(spentMonth), style = MaterialTheme.typography.labelLarge, color = White)
        }
        if (account.kind != BankKind.CARD && incomeMonth > 0) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                SmallLabel("Ingresos este mes")
                Text(wholePesos(incomeMonth), style = MaterialTheme.typography.labelLarge, color = MoneyPlenty)
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (records.isNotEmpty()) {
                Text(
                    if (showMovements) "Ocultar movimientos (${records.size})" else "Ver movimientos (${records.size})",
                    Modifier.clickable(role = Role.Button) { showMovements = !showMovements }.padding(vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = FinanceTeal,
                )
            } else {
                SmallLabel("Sin movimientos registrados")
            }
            Row(
                Modifier
                    .clickable(role = Role.Button, onClick = onEdit)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StrokeIcon("lapiz", StrokeState.OFF, 16.dp)
                Text("Editar", style = MaterialTheme.typography.labelLarge, color = White)
            }
        }
        if (showMovements && records.isNotEmpty()) {
            Movements(records.sortedByDescending { it.createdAt }, limit = 10)
        }
    }
}

/** One piece of the bar: what it stands for (the key), how much, and its color. */
private data class Piece(val key: String, val cents: Long, val color: Color)

/**
 * Mi dinero: compact side-by-side summary at first glance ("Mi dinero" and "Libre tras deudas y fijos"),
 * plus the allocation bar and a collapsible section for the detailed breakdown and monthly payment methods.
 */
@Composable
internal fun MyMoneyPanel(
    accounts: List<BankAccount>,
    receivables: List<BankReceivable>,
    fixedLines: List<Pair<String, Long>>,
    money: List<FinanceRecord> = emptyList(),
    today: LocalDate = LocalDate.now(),
    expanded: Boolean = false,
    onToggleExpanded: (() -> Unit)? = null,
) {
    val gross = Bank.myMoney(accounts, receivables)
    val debit = Bank.debit(accounts) + Bank.cash(accounts)
    val toCome = Bank.toCome(accounts, receivables)
    val owe = accounts.filter { it.kind == BankKind.CARD }.sumOf { it.balanceCents }
    val fixed = fixedLines.sumOf { it.second }
    val difference = gross - owe - fixed
    val leftover = (debit - owe - fixed).coerceAtLeast(0)
    val total = maxOf(gross, owe + toCome + fixed, 1L)
    var picked by remember { mutableStateOf<String?>(null) }
    var localExpanded by rememberSaveable { mutableStateOf(expanded) }
    val isExpanded = if (onToggleExpanded != null) expanded else localExpanded
    val toggle = onToggleExpanded ?: { localExpanded = !localExpanded }
    val pick = { key: String ->
        picked = if (picked == key) null else key
        if (!isExpanded) toggle()
    }
    val segments = listOf(
        Piece("cards", owe.coerceAtLeast(0), FinanceLavender),
        Piece("come", toCome.coerceAtLeast(0), FinanceMist),
        Piece("fixed", fixed.coerceAtLeast(0), FinanceBlue),
        Piece("left", leftover, FinanceTeal),
    ).filter { it.cents > 0 }

    Column(
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Mi dinero ${wholePesos(gross)}, diferencia ${wholePesos(difference)}"
            },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SmallLabel("Mi dinero")
                FinanceAmount(gross, size = 28)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SmallLabel("Tras deudas y fijos")
                FinanceAmount(difference, size = 28, color = if (difference < 0) MoneyOver else FinanceTeal)
            }
        }
        FinanceCollapsible(
            title = "Desglose del saldo",
            expanded = isExpanded,
            onToggle = toggle,
        ) {
            SingleBar(segments, total, picked, pick)
            val labels = mapOf(
                "cards" to "Tarjetas (${wholePesos(owe)})",
                "come" to "Por cobrar (${wholePesos(toCome)})",
                "fixed" to "Previsión (${wholePesos(fixed)})",
                "left" to "Disponible (${wholePesos(leftover)})",
            )
            segments.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { piece ->
                        Row(
                            Modifier
                                .weight(1f)
                                .clickable(role = Role.Button) { picked = if (picked == piece.key) null else piece.key }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(7.dp).background(piece.color, CircleShape))
                            Text(labels.getValue(piece.key), style = MaterialTheme.typography.bodySmall, color = White)
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            PieceDetail(picked, accounts, receivables, fixedLines)
            if (money.isNotEmpty() || accounts.isNotEmpty()) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(FinanceRule))
                SmallLabel("FORMAS DE PAGO · ESTE MES")
                val current = money.filter { it.kind.isExpense && YearMonth.from(Budget.dateOf(it)) == YearMonth.from(today) }
                listOf(
                    BankKind.DEBIT to "Pagado con débito",
                    BankKind.CARD to "Cargado a crédito",
                    BankKind.CASH to "Pagado en efectivo",
                ).forEach { (kind, label) ->
                    val ids = accounts.filter { it.kind == kind }.map { it.id }.toSet()
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text(label, Modifier.weight(1f), color = FinanceMuted, style = MaterialTheme.typography.bodySmall)
                        Text(wholePesos(current.filter { it.account in ids }.sumOf { it.amountCents }), color = White, style = MaterialTheme.typography.bodySmall)
                    }
                }
                val unknown = current.filter { row -> accounts.none { it.id == row.account } }.sumOf { it.amountCents }
                if (unknown > 0) {
                    Text("Sin cuenta especificada: ${wholePesos(unknown)}", color = FinanceMuted, style = MaterialTheme.typography.bodySmall)
                }
                FinanceReadMore("Los retiros trasladan dinero a efectivo; no se suman a estos gastos.")
            }
        }
    }
}

/** One rounded bar split into its pieces, in order; tapping a piece calls [onPick]. The pieces not picked fade. */
@Composable
private fun SingleBar(pieces: List<Piece>, total: Long, picked: String?, onPick: (String) -> Unit) {
    Canvas(
        Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Dim).pointerInput(pieces, total) {
            detectTapGestures { tap ->
                var x = 0f
                for (p in pieces) {
                    val w = size.width.toFloat() * p.cents / total
                    if (tap.x >= x && tap.x < x + w) { onPick(p.key); return@detectTapGestures }
                    x += w
                }
            }
        },
    ) {
        var x = 0f
        pieces.forEach { p ->
            val w = size.width * p.cents / total
            val fade = picked != null && picked != p.key
            drawRect(p.color.copy(alpha = if (fade) 0.3f else 1f), topLeft = Offset(x, 0f), size = Size(w, size.height))
            x += w
        }
    }
}

/** What the picked piece is: its name and amount, and what makes it up. Nothing when no piece is picked. */
@Composable
private fun PieceDetail(key: String?, accounts: List<BankAccount>, receivables: List<BankReceivable>, fixedLines: List<Pair<String, Long>>) {
    if (key == null) return
    val lines = mutableListOf<Pair<String, Long>>()
    val title: String
    val amount: Long
    when (key) {
        "cards" -> {
            title = "Por pagar"
            amount = accounts.filter { it.kind == BankKind.CARD }.sumOf { it.balanceCents }
            accounts.filter { it.kind == BankKind.CARD }.forEach { lines += it.name to it.balanceCents }
        }
        "come" -> {
            title = "Por cobrar"
            amount = Bank.toCome(accounts, receivables)
            receivables.forEach { lines += it.concept to it.amountCents }
            accounts.filter { it.kind == BankKind.CARD && it.owedByOthersCents > 0 }.forEach { lines += "${it.name} te debe" to it.owedByOthersCents }
        }
        "fixed" -> {
            title = "Gastos fijos"
            amount = fixedLines.sumOf { it.second }
            lines += fixedLines
        }
        else -> {
            title = "Sobra"
            amount = (Bank.debit(accounts) + Bank.cash(accounts) - accounts.filter { it.kind == BankKind.CARD }.sumOf { it.balanceCents } - fixedLines.sumOf { it.second }).coerceAtLeast(0)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SmallLabel(title.uppercase())
        Text(wholePesos(amount), style = MaterialTheme.typography.titleSmall, color = White)
        lines.forEach { (name, cents) -> SmallLabel("$name ${wholePesos(cents)}") }
    }
}

/** A day of the month for the statement or the payment, with how many days are left to it. */
@Composable
private fun DateLine(icon: String, day: Int, today: LocalDate) {
    val next = Bank.nextDate(day, today)
    val left = next?.let { ChronoUnit.DAYS.between(today, it) }
    val soon = left != null && left <= 3
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        StrokeIcon(icon, StrokeState.OFF, 18.dp)
        Text(
            when (left) {
                null -> "Sin fecha"
                0L -> "Día $day · hoy"
                1L -> "Día $day · mañana"
                else -> "Día $day · en $left d"
            },
            style = MaterialTheme.typography.labelMedium,
            color = if (soon) MoneyOver else White,
        )
    }
}

@Composable
private fun ReceivableRow(receivable: BankReceivable, onCollect: () -> Unit, onEdit: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onEdit)
            .semantics(mergeDescendants = true) { contentDescription = "${receivable.concept}: ${wholePesos(receivable.amountCents)}" }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StrokeIcon("ingreso", StrokeState.OFF, 20.dp)
        Text(receivable.concept, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = White, maxLines = 1)
        Text(wholePesos(receivable.amountCents), style = MaterialTheme.typography.labelLarge, color = MoneyGoing)
        Text(
            "Cobrar",
            Modifier
                .clickable(role = Role.Button, onClick = onCollect)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MoneyPlenty,
        )
    }
}

/**
 * The editor for one row. Saving needs a name (or a concept) and keeps the amounts as typed; for a card, what others
 * owe is kept between 0 and the balance, and the cut and payment days are a day of the month (empty for none).
 * Collecting a receivable adds its amount to the debit account and removes it.
 */
@Composable
private fun BankEditor(
    edit: BankEdit,
    takenIds: Set<String>,
    canDelete: Boolean,
    onSaveAccount: (BankAccount) -> Unit,
    onSaveReceivable: (BankReceivable) -> Unit,
    onDelete: (String) -> Unit,
    onCollect: (BankReceivable) -> Unit,
    onClose: () -> Unit,
) {
    val isAccount = edit is BankEdit.Account
    val account = (edit as? BankEdit.Account)?.account
    val receivable = (edit as? BankEdit.Receivable)?.receivable
    val isCard = (edit as? BankEdit.Account)?.kind == BankKind.CARD
    val title = when {
        isAccount && isCard -> "TARJETA"
        isAccount -> "CUENTA"
        else -> "POR COBRAR"
    }
    var name by remember(edit) { mutableStateOf(account?.name ?: receivable?.concept ?: "") }
    var amount by remember(edit) { mutableStateOf(centsText(account?.balanceCents ?: receivable?.amountCents ?: 0L)) }
    var owed by remember(edit) { mutableStateOf(centsText(account?.owedByOthersCents ?: 0L)) }
    var cut by remember(edit) { mutableStateOf(account?.cutDay?.takeIf { it > 0 }?.toString() ?: "") }
    var pay by remember(edit) { mutableStateOf(account?.payDay?.takeIf { it > 0 }?.toString() ?: "") }
    var cards by remember(edit) { mutableStateOf(account?.cardDigits?.joinToString(", ") ?: "") }
    var limit by remember(edit) { mutableStateOf(centsText(account?.limitCents ?: 0L)) }
    var due by remember(edit) { mutableStateOf(centsText(account?.dueCents ?: 0L)) }
    var next by remember(edit) { mutableStateOf(centsText(account?.nextDueCents ?: 0L)) }
    var moreCardFields by rememberSaveable(edit.toString()) {
        mutableStateOf((account?.owedByOthersCents ?: 0L) > 0L || (account?.cutDay ?: 0) > 0 || (account?.payDay ?: 0) > 0)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            SmallLabel(title, color = LauncherAccent)
            SmallLabel("✕", Modifier.clickable(role = Role.Button, onClick = onClose).semantics { contentDescription = "Cerrar" }.padding(6.dp))
        }
        ModuleTextField(
            name, { name = it.take(40) }, if (isAccount) "Nombre" else "Concepto", Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        ModuleTextField(
            amount, { amount = amountInput(it) }, if (isAccount && !isCard) "Saldo" else if (isCard) "Debes" else "Monto",
            Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            visualTransformation = PesosTransformation,
        )
        if (isAccount && edit.let { (it as BankEdit.Account).kind } != BankKind.CASH) {
            // The cards' last 4 digits: a bank notice that names them goes to this account.
            ModuleTextField(
                cards, { cards = it.filter { c -> c.isDigit() || c == ',' || c == ' ' }.take(40) }, "Tarjetas · últimos 4 (1234, 5678)",
                Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
            )
        }
        if (isCard) {
            ModuleTextField(
                limit, { limit = amountInput(it) }, "Límite", Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                visualTransformation = PesosTransformation,
            )
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                ModuleTextField(
                    due, { due = amountInput(it) }, "A pagar", Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    visualTransformation = PesosTransformation,
                )
                ModuleTextField(
                    next, { next = amountInput(it) }, "Siguiente", Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    visualTransformation = PesosTransformation,
                )
            }
            FinanceCollapsible(
                title = "Fechas de corte, pago y deuda compartida",
                expanded = moreCardFields,
                onToggle = { moreCardFields = !moreCardFields },
                showRule = false,
            ) {
                ModuleTextField(
                    owed, { owed = amountInput(it) }, "Te deben de esta tarjeta",
                    Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    visualTransformation = PesosTransformation,
                )
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                    ModuleTextField(
                        cut, { cut = it.filter(Char::isDigit).take(2) }, "Día de corte", Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    )
                    ModuleTextField(
                        pay, { pay = it.filter(Char::isDigit).take(2) }, "Día de pago", Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    )
                }
            }
        }

        if (!canDelete) {
            FinanceReadMore("Esta cuenta conserva movimientos vinculados. Puedes editar su saldo, pero no eliminarla.")
        }
        val cents = parseCents(amount) ?: 0L
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (canDelete && (account != null || receivable != null)) {
                Text(
                    "Borrar",
                    Modifier.clickable(role = Role.Button) { onDelete(account?.id ?: receivable!!.id) }.padding(vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = LauncherAccent,
                )
                Spacer(Modifier.width(20.dp))
            }
            if (receivable != null) {
                Text(
                    "Cobrado",
                    Modifier.clickable(role = Role.Button) { onCollect(receivable) }.padding(vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MoneyPlenty,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                "Guardar",
                Modifier
                    .background(White, RoundedCornerShape(18.dp))
                    .clickable(role = Role.Button) {
                        if (name.isBlank()) return@clickable
                        when (edit) {
                            is BankEdit.Account -> onSaveAccount(
                                BankAccount(
                                    id = account?.id ?: newAccountId(name, takenIds),
                                    name = name.trim(),
                                    kind = edit.kind,
                                    balanceCents = cents,
                                    owedByOthersCents = if (edit.kind == BankKind.CARD) (parseCents(owed) ?: 0L).coerceIn(0, cents) else 0L,
                                    cutDay = if (edit.kind == BankKind.CARD) cut.toIntOrNull()?.takeIf { it in 1..31 } ?: 0 else 0,
                                    payDay = if (edit.kind == BankKind.CARD) pay.toIntOrNull()?.takeIf { it in 1..31 } ?: 0 else 0,
                                    cards = Regex("\\d{4}").findAll(cards).map { it.value }.distinct().joinToString(","),
                                    limitCents = if (edit.kind == BankKind.CARD) parseCents(limit) ?: 0L else 0L,
                                    dueCents = if (edit.kind == BankKind.CARD) parseCents(due) ?: 0L else 0L,
                                    nextDueCents = if (edit.kind == BankKind.CARD) parseCents(next) ?: 0L else 0L,
                                    // The coming payment is due on the next payment day.
                                    dueDate = if (edit.kind == BankKind.CARD) {
                                        pay.toIntOrNull()?.let { Bank.nextDate(it, LocalDate.now()) }?.toString() ?: account?.dueDate.orEmpty()
                                    } else "",
                                ),
                            )
                            is BankEdit.Receivable -> if (cents > 0) onSaveReceivable(
                                BankReceivable(receivable?.id ?: UUID.randomUUID().toString(), name.trim(), cents, receivable?.createdAt ?: System.currentTimeMillis()),
                            )
                        }
                    }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                style = MaterialTheme.typography.labelLarge,
                color = Color.Black,
            )
        }
    }
}

/** The kinds that take money out of an account. */
private val spentKinds = setOf(MoneyKind.EXTRA, MoneyKind.SHARED, MoneyKind.FIXED, MoneyKind.MONTHLY)

/**
 * An id from the name ("Nu" → "nu"), so a bank's movements find their account by it (see BankApps). A name already
 * taken gets a short suffix.
 */
private fun newAccountId(name: String, taken: Set<String>): String {
    val base = Normalizer.normalize(name.trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "cuenta" }
    return if (base !in taken) base else "$base-${UUID.randomUUID().toString().take(4)}"
}

/** A cents amount as the field shows it while typing: "9504.82", "300", or "" for none. */
private fun centsText(cents: Long): String = if (cents <= 0) "" else (cents / 100).toString() + if (cents % 100 == 0L) "" else "." + "%02d".format(cents % 100)

/**
 * Cuenta en cuadrícula (2026-11-11): tarjeta translúcida con el nombre y un punto por tipo (blanco débito, ámbar
 * crédito, verde efectivo), el saldo o lo que se debe, y en tarjetas lo que toca pagar y la barra del límite.
 */
@Composable
private fun AccountTile(
    account: BankAccount,
    selected: Boolean,
    modifier: Modifier = Modifier,
    balanceColor: Color = LauncherAccent,
    hint: String? = null,
    onClick: () -> Unit,
) {
    val state = cardState(account)
    val kind = kindColor(account.kind)
    Column(
        modifier
            .then(if (account.kind == BankKind.CARD) Modifier.tinted(state) else Modifier.glass())
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Dot(kind)
            Text(account.name, style = MaterialTheme.typography.bodyMedium, color = kind, maxLines = 1)
        }
        Text(wholePesos(account.balanceCents), style = MaterialTheme.typography.titleMedium, color = White, maxLines = 1)
        hint?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = com.adrian.minimallauncher.feature.finance.LabelCream, maxLines = 1) }
        if (account.kind == BankKind.CARD && account.limitCents > 0) {
            val used = (account.balanceCents.toFloat() / account.limitCents).coerceIn(0f, 1f)
            Box(Modifier.fillMaxWidth().height(4.dp).background(GlassFill, RoundedCornerShape(2.dp))) {
                Box(Modifier.fillMaxWidth(used).fillMaxHeight().background(state, RoundedCornerShape(2.dp)))
            }
        }
    }
}

/** Color de cada tipo de cuenta: débito azul, crédito ámbar, efectivo verde. */
internal fun kindColor(kind: BankKind): Color = when (kind) {
    BankKind.CARD -> MoneyLow
    BankKind.CASH -> MoneyPlenty
    BankKind.DEBIT -> com.adrian.minimallauncher.feature.finance.TextBlue
}

/** Cómo va una tarjeta contra su límite (verde, azul, ámbar, rosa); sin límite, ámbar. */
internal fun cardState(account: BankAccount): Color =
    if (account.kind != BankKind.CARD) White else if (account.limitCents > 0) usageColor(account.balanceCents, account.limitCents) else MoneyLow

/** Arriba de Banco: lo que tienes en todas las cuentas y lo que debes en todas las tarjetas. */
@Composable
private fun BankTotals(accounts: List<BankAccount>, today: LocalDate) {
    val have = accounts.filter { it.kind != BankKind.CARD }
    val cards = accounts.filter { it.kind == BankKind.CARD }.map { Bank.rolled(it, today) }
    val haveCents = have.sumOf { it.balanceCents }
    val dueMonth = cards.filter { c -> c.due?.let { java.time.YearMonth.from(it) == java.time.YearMonth.from(today) } == true }.sumOf { it.dueCents }
    val owed = cards.sumOf { it.balanceCents }
    val limit = cards.sumOf { it.limitCents }
    // Green when the accounts cover this month's card payments with room; amber when tight; pink when they do not.
    val haveTint = when {
        haveCents >= dueMonth * 2 -> MoneyPlenty
        haveCents >= dueMonth -> MoneyLow
        else -> com.adrian.minimallauncher.feature.LauncherDanger
    }
    val owedTint = if (limit > 0) usageColor(owed, limit) else MoneyLow
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Pop(Modifier.weight(1f), "Ver tus cuentas", pop = {
            have.sortedByDescending { it.balanceCents }.forEach { PopRow(it.name, wholePesos(it.balanceCents), dot = if (it.kind == BankKind.CASH) MoneyPlenty else White) }
        }) {
            com.adrian.minimallauncher.feature.finance.Figure("En cuentas", haveCents, haveTint)
        }
        Pop(Modifier.weight(1f), "Ver lo que debes", pop = {
            cards.sortedByDescending { it.balanceCents }.forEach { PopRow(it.name, wholePesos(it.balanceCents), dot = MoneyLow) }
            PopRule()
            PopRow("Toca este mes", wholePesos(dueMonth), MoneyLow)
            if (limit > 0) PopRow("Límite total", wholePesos(limit))
        }) {
            com.adrian.minimallauncher.feature.finance.Figure("Por pagar", owed, MoneyLow, bar = if (limit > 0) (owed.toFloat() / limit) to owedTint else null)
        }
    }
}
