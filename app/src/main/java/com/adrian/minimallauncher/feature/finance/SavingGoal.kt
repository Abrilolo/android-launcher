package com.adrian.minimallauncher.feature.finance

import com.adrian.minimallauncher.ui.components.IconAction
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.feature.CardLine
import com.adrian.minimallauncher.feature.ModuleTextField
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.ui.components.LocalReactions
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import org.json.JSONObject

private val spanish = Locale.forLanguageTag("es")
private val White = Color(0xFFF5F5F5)
private val Muted = FinanceMuted

/** A saving goal: how much, from when until when, and what is already in the piggy bank. Amounts in cents. */
data class SavingGoal(val targetCents: Long, val start: LocalDate, val end: LocalDate, val savedCents: Long = 0) {
    val reached: Boolean get() = targetCents > 0 && savedCents >= targetCents

    /** What should be saved by [today] to reach the goal on time, at an even pace. */
    fun expectedCents(today: LocalDate): Long {
        val total = ChronoUnit.DAYS.between(start, end).coerceAtLeast(1)
        val gone = ChronoUnit.DAYS.between(start, today).coerceIn(0, total)
        return targetCents * gone / total
    }

    fun behind(today: LocalDate): Boolean = !reached && savedCents < expectedCents(today)

    /** Months still to go until the end date, the current one included; at least one. */
    fun monthsLeft(today: LocalDate): Long = ChronoUnit.MONTHS.between(today.withDayOfMonth(1), end.withDayOfMonth(1)).coerceAtLeast(1)

    /** What each remaining month needs to put in. */
    fun perMonthCents(today: LocalDate): Long {
        val left = (targetCents - savedCents).coerceAtLeast(0)
        val months = monthsLeft(today)
        return (left + months - 1) / months
    }

    fun encode(): String = JSONObject()
        .put("target", targetCents).put("start", start.toString()).put("end", end.toString()).put("saved", savedCents)
        .toString()

    companion object {
        fun decode(text: String): SavingGoal? = if (text.isBlank()) null else runCatching {
            val json = JSONObject(text)
            SavingGoal(json.getLong("target"), LocalDate.parse(json.getString("start")), LocalDate.parse(json.getString("end")), json.optLong("saved"))
        }.getOrNull()
    }
}

private val PLAZOS = listOf(1, 3, 6, 12, 24)

/**
 * Meta: a goal of its own, apart from the month's budget. Without one, pick how much and in how long. With one, the
 * piggy bank fills with what was put in (red on pace, amber behind the time, green once reached), what it needs per
 * month to arrive on time, and a field to put money in.
 */
@Composable
internal fun SavingGoalView(goalText: String, today: LocalDate, onSave: (String) -> Unit) {
    val goal = remember(goalText) { SavingGoal.decode(goalText) }
    var editing by rememberSaveable(goalText) { mutableStateOf(goal == null) }
    if (editing || goal == null) GoalForm(goal, today, onCancel = if (goal != null) ({ editing = false }) else null) { onSave(it.encode()) }
    else GoalProgress(goal, today, onEdit = { editing = true }, onDelete = { onSave("") }) { onSave(it.encode()) }
}

@Composable
private fun GoalForm(goal: SavingGoal?, today: LocalDate, onCancel: (() -> Unit)?, onSave: (SavingGoal) -> Unit) {
    var amount by rememberSaveable { mutableStateOf(goal?.let { plainAmount(it.targetCents) } ?: "") }
    var months by rememberSaveable { mutableIntStateOf(goal?.let { ChronoUnit.MONTHS.between(today, it.end).toInt().coerceAtLeast(1) } ?: 6) }
    var saved by rememberSaveable { mutableStateOf(goal?.let { plainAmount(it.savedCents) } ?: "0") }
    val savedCents = parseCents(saved)
    val target = parseCents(amount) ?: 0
    val originalMonths = goal?.let { ChronoUnit.MONTHS.between(today, it.end).toInt().coerceAtLeast(1) }
    val deadline = if (goal != null && months == originalMonths) goal.end else today.plusMonths(months.toLong())
    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            StrokeIcon("meta", StrokeState.NOW, 24.dp)
            ModuleTextField(
                amount, { amount = amountInput(it) }, "Importe objetivo", Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(),
                visualTransformation = PesosTransformation,
            )
        }
        ModuleTextField(saved, { saved = amountInput(it) }, "Ya ahorrado", Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), visualTransformation = PesosTransformation)
        Text("Plazo desde hoy", color = White, style = MaterialTheme.typography.titleSmall)
        Text("La meta registra tu avance; no mueve dinero entre cuentas.", color = Muted, style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            StrokeIcon("reloj", StrokeState.DONE, 24.dp)
            Row(Modifier.weight(1f).border(1.dp, CardLine, RoundedCornerShape(10.dp))) {
                PLAZOS.forEach { m ->
                    val on = m == months
                    Box(
                        Modifier.weight(1f).height(48.dp).background(if (on) White else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable(role = Role.Button) { months = m }.semantics { contentDescription = "$m meses" },
                        contentAlignment = Alignment.Center,
                    ) { Text(if (m < 12) "$m mes" else "${m / 12} año", style = MaterialTheme.typography.labelLarge, color = if (on) Color.Black else White) }
                }
            }
        }
        if (target > 0) {
            val draft = SavingGoal(target, goal?.start ?: today, deadline, savedCents ?: 0)
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                SmallLabel("Aportación mensual")
                Text(wholePesos(draft.perMonthCents(today)), style = MaterialTheme.typography.titleMedium, color = White)
            }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                SmallLabel("Hasta")
                Text(monthYear(draft.end), style = MaterialTheme.typography.titleMedium, color = White)
            }
        }
        val reactions = LocalReactions.current
        WideButton(if (goal == null) "Crear meta" else "Guardar", enabled = target > 0 && savedCents != null && savedCents >= 0) {
            onSave(SavingGoal(target, goal?.start ?: today, deadline, savedCents ?: 0))
            reactions.tick()
        }
        onCancel?.let { cancel -> IconAction("no", "Cancelar", onClick = cancel) }
    }
}

@Composable
private fun GoalProgress(goal: SavingGoal, today: LocalDate, onEdit: () -> Unit, onDelete: () -> Unit, onSave: (SavingGoal) -> Unit) {
    val reactions = LocalReactions.current
    var amount by rememberSaveable { mutableStateOf("") }
    val fraction = if (goal.targetCents > 0) goal.savedCents.toFloat() / goal.targetCents else 0f
    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            PiggyBank(fraction, goalColor(goal, today), Modifier.size(width = 216.dp, height = 180.dp))
        }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Bottom) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SmallLabel("Ahorrado")
                BigAmount(goal.savedCents, 30.dp, goalColor(goal, today))
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    StrokeIcon("meta", StrokeState.OFF, 14.dp)
                    SmallLabel("Meta")
                }
                Text(wholePesos(goal.targetCents), style = MaterialTheme.typography.titleLarge, color = White)
            }
        }
        if (!goal.reached) Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Fact("mes", "Por mes", wholePesos(goal.perMonthCents(today)), goalColor(goal, today))
            Fact("reloj", "Hasta", monthYear(goal.end), White)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            StrokeIcon("ahorro", StrokeState.NOW, 22.dp)
            ModuleTextField(
                amount, { amount = amountInput(it) }, "Abonar", Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(),
                visualTransformation = PesosTransformation,
            )
            val add = {
                parseCents(amount)?.takeIf { it > 0 }?.let { cents ->
                    onSave(goal.copy(savedCents = goal.savedCents + cents)); amount = ""; reactions.tick()
                }
                Unit
            }
            val take = {
                parseCents(amount)?.takeIf { it > 0 }?.let { cents ->
                    onSave(goal.copy(savedCents = (goal.savedCents - cents).coerceAtLeast(0))); amount = ""; reactions.reject()
                }
                Unit
            }
            IconButton("borrar", "Sacar", 20.dp, onClick = take)
            Box(Modifier.clickable(role = Role.Button, onClick = add).semantics { contentDescription = "Abonar" }.padding(8.dp)) {
                StrokeIcon("nueva", StrokeState.NOW, 24.dp, color = SavingGreen)
            }
        }
        var deleting by remember { mutableStateOf(false) }
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp, Alignment.End)) {
            IconAction("lapiz", "Editar meta", onClick = onEdit)
            IconAction("papelera", "Eliminar meta") { deleting = true }
        }
        if (deleting) androidx.compose.material3.AlertDialog(onDismissRequest = { deleting = false },
            title = { Text("¿Eliminar la meta?") }, text = { Text("Se borrará el seguimiento de ahorro de esta meta. Tus movimientos no cambian.") },
            confirmButton = { IconAction("papelera", "Eliminar meta") { deleting = false; onDelete() } },
            dismissButton = { IconAction("no", "Cancelar") { deleting = false } })
    }
}

@Composable
private fun Fact(icon: String, label: String, value: String, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            StrokeIcon(icon, StrokeState.OFF, 14.dp)
            SmallLabel(label)
        }
        Text(value, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

@Composable
private fun WideButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        Modifier.fillMaxWidth().height(54.dp).background(if (enabled) White else Color(0xFF161616), shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = MaterialTheme.typography.titleMedium, color = if (enabled) Color.Black else Color(0xFF5C5C5C)) }
}

private fun monthYear(date: LocalDate) = date.month.getDisplayName(TextStyle.SHORT, spanish).trimEnd('.') + " " + date.year
