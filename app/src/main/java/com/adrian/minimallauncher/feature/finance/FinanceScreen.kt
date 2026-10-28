package com.adrian.minimallauncher.feature.finance

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.adrian.minimallauncher.feature.LauncherRed
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.FinanceRecord
import com.adrian.minimallauncher.feature.ModuleTextField
import com.adrian.minimallauncher.feature.SettingsGear
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.ui.components.DotIcon
import com.adrian.minimallauncher.ui.components.DotIcons
import com.adrian.minimallauncher.ui.components.DotMatrixText
import com.adrian.minimallauncher.ui.components.LocalReactions
import com.adrian.minimallauncher.ui.components.animationsOff
import com.adrian.minimallauncher.ui.components.popOnChange
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val spanish = Locale.forLanguageTag("es")
private val hhmm = DateTimeFormatter.ofPattern("HH:mm")
private const val DOT_ROWS = 8
private const val BUDGET_DOTS = 20
private const val UNDO_MS = 4000L

/** Pesos without cents when they are round. No thousands comma: the dot-matrix glyphs have none. */
fun formatPesos(cents: Long): String {
    val whole = cents / 100
    return if (cents % 100 == 0L) "$$whole" else "$$whole.%02d".format(cents % 100)
}

/** Today's spending, the last seven days as columns of dots, today's entries and a quick entry row. */
@Composable
fun FinanceScreen(
    finances: List<FinanceRecord>,
    now: LocalDateTime,
    budgetCents: Long,
    onAdd: (Long, String) -> Unit,
    onDelete: (String) -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = now.toLocalDate()
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    // A deleted expense waits a few seconds, hidden, so it can be brought back; then it is really deleted.
    var pendingDelete by remember { mutableStateOf<String?>(null) }
    val latestDelete by rememberUpdatedState(onDelete)
    LaunchedEffect(pendingDelete) {
        val id = pendingDelete ?: return@LaunchedEffect
        delay(UNDO_MS)
        latestDelete(id)
        pendingDelete = null
    }
    DisposableEffect(Unit) { onDispose { pendingDelete?.let(latestDelete) } }
    val byDay = days.map { day -> finances.filter { it.createdAt.toLocalDate() == day && it.id != pendingDelete } }
    val totals = byDay.map { list -> list.sumOf { it.amountCents } }
    val todayRecords = byDay.last()
    val spentToday = todayRecords.sumOf { it.amountCents }
    val over = budgetCents > 0 && spentToday > budgetCents
    val reactions = LocalReactions.current
    // When the page opened: rows added after this slide in.
    val openedAt = remember { System.currentTimeMillis() }

    // No header: going back is the phone's back gesture.
    BackHandler(onBack = onBack)
    Column(modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 24.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    SmallLabel("GASTO HOY", color = LauncherRed)
                    SettingsGear(onSettings)
                }
                // The total redraws left to right when it changes; past the budget it turns red and bounces once.
                DotMatrixText(
                    formatPesos(spentToday), Modifier.popOnChange(over),
                    cell = 8.dp,
                    color = if (over) LauncherRed else MaterialTheme.colorScheme.onSurface,
                    accents = if (over) emptyMap() else mapOf('.' to LauncherRed),
                    reveal = spentToday,
                )
                if (budgetCents > 0) BudgetDots(spentToday, budgetCents)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    DotIcon(DotIcons.cartera, done = true, contentDescription = null, size = 16.dp)
                    SmallLabel("SEMANA ${formatPesos(totals.sum())}", color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Column(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF222222)))
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SmallLabel("ÚLTIMOS 7 DÍAS", color = LauncherRed)
                    WeekDots(days, totals, today)
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF222222)))
            }
            if (todayRecords.isEmpty()) {
                SmallLabel("SIN GASTOS HOY")
            } else {
                Column {
                    todayRecords.forEach { record ->
                        key(record.id) {
                            ExpenseRow(record, slideIn = record.createdAt > openedAt) {
                                pendingDelete?.let(latestDelete)
                                pendingDelete = record.id
                            }
                        }
                    }
                }
            }
        }
        val pending = pendingDelete?.let { id -> finances.firstOrNull { it.id == id } }
        if (pending != null) {
            UndoBar(pending.description) { pendingDelete = null }
        } else {
            QuickEntry { cents, concept ->
                onAdd(cents, concept)
                // Past the budget with this one: a "no" instead of the usual tick.
                if (budgetCents > 0 && spentToday <= budgetCents && spentToday + cents > budgetCents) reactions.reject() else reactions.tick()
            }
        }
    }
}

/** One column per day: the busiest day fills all eight dots, today is white, other days light gray and empty dots dark gray. */
@Composable
private fun WeekDots(days: List<LocalDate>, totals: List<Long>, today: LocalDate) {
    val max = totals.maxOrNull()?.takeIf { it > 0 } ?: 1L
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        days.forEachIndexed { index, day ->
            val isToday = day == today
            val filled = if (totals[index] == 0L) 0 else ((totals[index] * DOT_ROWS + max - 1) / max).toInt()
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (row in DOT_ROWS downTo 1) {
                    val color = when {
                        row > filled -> Color(0xFF4A4A4A)
                        isToday -> MaterialTheme.colorScheme.onSurface
                        else -> Color(0xFFA8A8A8)
                    }
                    Box(Modifier.size(9.dp).background(color, CircleShape))
                }
                SmallLabel(
                    day.dayOfWeek.getDisplayName(TextStyle.NARROW, spanish).uppercase(spanish),
                    Modifier.padding(top = 6.dp),
                    color = if (isToday) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Today against the daily budget: one dot per twentieth of it, lit white as it is spent and lighting one after another
 * when a new expense comes in. Past the budget every dot turns red.
 */
@Composable
private fun BudgetDots(spent: Long, budget: Long) {
    val over = spent > budget
    val target = if (spent <= 0) 0 else ((spent * BUDGET_DOTS + budget - 1) / budget).toInt().coerceAtMost(BUDGET_DOTS)
    val lit = remember { Animatable(target.toFloat()) }
    val off = animationsOff()
    LaunchedEffect(target) {
        if (off) lit.snapTo(target.toFloat())
        else lit.animateTo(target.toFloat(), tween((kotlin.math.abs(target - lit.value) * 35).toInt().coerceAtLeast(1), easing = LinearEasing))
    }
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in 0 until BUDGET_DOTS) {
                val color = when {
                    i >= lit.value -> Color(0xFF4A4A4A)
                    over -> LauncherRed
                    else -> MaterialTheme.colorScheme.onSurface
                }
                Box(Modifier.size(7.dp).background(color, CircleShape))
            }
        }
        SmallLabel("DE ${formatPesos(budget)}", color = if (over) LauncherRed else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Shown for a few seconds after deleting an expense, in place of the + button. */
@Composable
private fun UndoBar(description: String, onUndo: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(48.dp).padding(bottom = 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SmallLabel("BORRADO · ${description.uppercase(spanish)}", Modifier.weight(1f), maxLines = 1)
        SmallLabel(
            "DESHACER",
            Modifier.clickable(role = Role.Button, onClick = onUndo).padding(horizontal = 4.dp, vertical = 14.dp),
            color = LauncherRed,
        )
    }
    Box(Modifier.height(16.dp))
}

@Composable
private fun ExpenseRow(record: FinanceRecord, slideIn: Boolean, onDelete: () -> Unit) {
    // A just-saved expense slides in from the right once.
    val shift = remember { Animatable(if (slideIn) 1f else 0f) }
    val off = animationsOff()
    LaunchedEffect(Unit) { if (off) shift.snapTo(0f) else shift.animateTo(0f, tween(260)) }
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { translationX = shift.value * size.width * 0.3f; alpha = 1f - shift.value }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(record.description, style = MaterialTheme.typography.bodyLarge)
            SmallLabel(Instant.ofEpochMilli(record.createdAt).atZone(ZoneId.systemDefault()).format(hhmm))
        }
        Text(formatPesos(record.amountCents), style = MaterialTheme.typography.bodyLarge)
        Text(
            "×",
            Modifier
                .clickable(role = Role.Button, onClick = onDelete)
                .semantics { contentDescription = "Borrar ${record.description}" }
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF1A1A1A)))
}

/**
 * Only the white + until it is tapped; then Monto and Concepto appear (Monto focused) and the
 * + (or the keyboard's done key) saves and folds the row away again. Back folds it without saving.
 */
@Composable
private fun QuickEntry(onAdd: (Long, String) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    var amount by rememberSaveable { mutableStateOf("") }
    var concept by rememberSaveable { mutableStateOf("") }
    val amountFocus = remember { FocusRequester() }
    val close = {
        open = false
        amount = ""
        concept = ""
    }
    val save = {
        val cents = runCatching { BigDecimal(amount.replace(',', '.')).movePointRight(2).longValueExact() }.getOrNull()
        if (cents != null && cents > 0 && concept.isNotBlank()) {
            onAdd(cents, concept.trim())
            close()
        }
    }
    BackHandler(enabled = open, onBack = close)
    LaunchedEffect(open) { if (open) amountFocus.requestFocus() }
    Row(
        Modifier.fillMaxWidth().padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (open) {
            ModuleTextField(
                amount, { value -> amount = value.filter { it.isDigit() || it == '.' || it == ',' }.take(10) }, "Monto",
                Modifier.width(104.dp).focusRequester(amountFocus),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            )
            ModuleTextField(
                concept, { concept = it.take(60) }, "Concepto",
                Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { save() }),
            )
        }
        Box(
            Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.onSurface, CircleShape)
                .clickable(role = Role.Button) { if (open) save() else open = true }
                .semantics { contentDescription = if (open) "Registrar gasto" else "Agregar gasto" },
            contentAlignment = Alignment.Center,
        ) {
            Text("+", style = MaterialTheme.typography.headlineSmall, color = Color.Black)
        }
    }
}

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
