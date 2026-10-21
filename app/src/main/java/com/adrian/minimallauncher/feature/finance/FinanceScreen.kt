package com.adrian.minimallauncher.feature.finance

import androidx.compose.foundation.background
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
import com.adrian.minimallauncher.feature.ModuleCard
import com.adrian.minimallauncher.feature.ModuleHeader
import com.adrian.minimallauncher.feature.ModuleTextField
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.ui.components.DotMatrixText
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
    onAdd: (Long, String) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = now.toLocalDate()
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val byDay = days.map { day -> finances.filter { it.createdAt.toLocalDate() == day } }
    val totals = byDay.map { list -> list.sumOf { it.amountCents } }
    val todayRecords = byDay.last()

    Column(modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        ModuleHeader("FINANZAS", onBack, Modifier.padding(top = 8.dp))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallLabel("GASTO HOY")
                DotMatrixText(formatPesos(totals.last()), cell = 8.dp)
                SmallLabel("${todayRecords.size} REGISTROS · SEMANA ${formatPesos(totals.sum())}", color = MaterialTheme.colorScheme.onSurface)
            }
            ModuleCard {
                SmallLabel("ÚLTIMOS 7 DÍAS")
                WeekDots(days, totals, today)
            }
            if (todayRecords.isEmpty()) {
                SmallLabel("SIN GASTOS HOY")
            } else {
                Column {
                    todayRecords.forEach { record -> ExpenseRow(record, onDelete) }
                }
            }
        }
        QuickEntry(onAdd)
    }
}

/** One column per day: the busiest day fills all eight dots, today is white and the rest gray. */
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
                        row > filled -> Color(0xFF161616)
                        isToday -> MaterialTheme.colorScheme.onSurface
                        else -> Color(0xFF5A5A5A)
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

@Composable
private fun ExpenseRow(record: FinanceRecord, onDelete: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(record.description, style = MaterialTheme.typography.bodyLarge)
            SmallLabel(Instant.ofEpochMilli(record.createdAt).atZone(ZoneId.systemDefault()).format(hhmm))
        }
        DotMatrixText(formatPesos(record.amountCents), cell = 3.dp)
        Text(
            "×",
            Modifier
                .clickable(role = Role.Button) { onDelete(record.id) }
                .semantics { contentDescription = "Borrar ${record.description}" }
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF1A1A1A)))
}

/** Amount and concept side by side; the white + (or the keyboard's done key) saves. */
@Composable
private fun QuickEntry(onAdd: (Long, String) -> Unit) {
    var amount by rememberSaveable { mutableStateOf("") }
    var concept by rememberSaveable { mutableStateOf("") }
    val save = {
        val cents = runCatching { BigDecimal(amount.replace(',', '.')).movePointRight(2).longValueExact() }.getOrNull()
        if (cents != null && cents > 0 && concept.isNotBlank()) {
            onAdd(cents, concept.trim())
            amount = ""
            concept = ""
        }
    }
    Row(
        Modifier.fillMaxWidth().padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ModuleTextField(
            amount, { value -> amount = value.filter { it.isDigit() || it == '.' || it == ',' }.take(10) }, "Monto",
            Modifier.width(104.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        )
        ModuleTextField(
            concept, { concept = it.take(60) }, "Concepto",
            Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { save() }),
        )
        Box(
            Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.onSurface, CircleShape)
                .clickable(role = Role.Button, onClick = save)
                .semantics { contentDescription = "Registrar gasto" },
            contentAlignment = Alignment.Center,
        ) {
            Text("+", style = MaterialTheme.typography.headlineSmall, color = Color.Black)
        }
    }
}

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
