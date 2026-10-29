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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.adrian.minimallauncher.feature.CardLine
import com.adrian.minimallauncher.feature.LauncherRed
import com.adrian.minimallauncher.feature.ModuleHeader
import com.adrian.minimallauncher.feature.ModuleTextField
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val spanish = Locale.forLanguageTag("es")
private val dayMonth = DateTimeFormatter.ofPattern("dd/MM")
private val PERIOD_LABELS = listOf(BudgetPeriod.DAY to "DÍA", BudgetPeriod.WEEK to "SEM", BudgetPeriod.MONTH to "MES")

/**
 * Finanzas settings, behind the gear (names allowed here): the month's income and fixed costs, the saving % that
 * sizes the extras bag, the total daily limit (empty = from the bag), budgets per topic, and this month's
 * movements with a delete for mistakes.
 */
@Composable
internal fun FinanceSettings(
    summary: FinanceSummary,
    money: List<FinanceRecord>,
    dailyLimitCents: Long,
    onSetBudget: (String, BudgetPeriod, Long) -> Unit,
    onSavingPct: (Int) -> Unit,
    onDailyLimit: (Long) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        ModuleHeader("FINANZAS", onBack, Modifier.padding(top = 8.dp))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ValueRow("INGRESO FIJO DEL MES", formatPesos(summary.fixedIncomeCents))
            ValueRow("GASTOS FIJOS DEL MES", formatPesos(summary.fixedCostsCents))
            var pct by remember(summary.savingPct) { mutableFloatStateOf(summary.savingPct.toFloat()) }
            Column {
                ValueRow("AHORRO", "${pct.roundToInt()} % · ${wholePesos((summary.fixedIncomeCents - summary.fixedCostsCents).coerceAtLeast(0) * pct.roundToInt() / 100)}", LauncherRed)
                Slider(
                    value = pct,
                    onValueChange = { pct = (it / 5).roundToInt() * 5f },
                    onValueChangeFinished = { onSavingPct(pct.roundToInt()) },
                    valueRange = 0f..60f,
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = LauncherRed, inactiveTrackColor = Color(0xFF3A3A3A)),
                )
            }
            ValueRow("BOLSA DE EXTRAS", formatPesos(summary.bagCents))
            ValueRow("APARTE (TRANSFERENCIAS, REGALOS, SERVICIOS)", formatPesos(summary.otherIncomeCents))
            Line()

            SmallLabel("LÍMITE DIARIO")
            var limit by remember(dailyLimitCents) { mutableStateOf(if (dailyLimitCents > 0) (dailyLimitCents / 100).toString() else "") }
            val auto = summary.bagCents / summary.date.lengthOfMonth()
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ModuleTextField(
                    limit, { value -> limit = value.filter { it.isDigit() || it == '.' }.take(8) }, "Auto ${wholePesos(auto)}",
                    Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onDailyLimit(parseCents(limit) ?: 0L) }),
                )
                Action("GUARDAR") { onDailyLimit(parseCents(limit) ?: 0L) }
            }
            SmallLabel("VACÍO = SALE DE LA BOLSA ENTRE LOS DÍAS DEL MES")
            Line()

            SmallLabel("PRESUPUESTOS POR TEMA", color = LauncherRed)
            summary.topics.forEach { topic -> TopicRow(topic, onSetBudget) }
            val free = BASE_CATEGORIES.filter { name -> summary.topics.none { it.topic.equals(name, ignoreCase = true) } }
            if (free.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    free.forEach { name -> Action("+ ${name.uppercase(spanish)}") { onSetBudget(name, BudgetPeriod.DAY, 5000) } }
                }
            }
            Line()

            SmallLabel("MOVIMIENTOS DEL MES", color = LauncherRed)
            val month = YearMonth.from(summary.date)
            val rows = money.filter { YearMonth.from(Budget.dateOf(it)) == month }.sortedByDescending { it.createdAt }
            if (rows.isEmpty()) SmallLabel("SIN MOVIMIENTOS")
            rows.forEach { record -> MovementRow(record) { onDelete(record.id) } }
        }
    }
}

/** A topic's limit: its period (day, week or month) and amount; ✕ removes it. */
@Composable
private fun TopicRow(topic: TopicStatus, onSetBudget: (String, BudgetPeriod, Long) -> Unit) {
    var amount by remember(topic.limitCents) { mutableStateOf((topic.limitCents / 100).toString()) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            StrokeIcon(categoryIcon(topic.topic), StrokeState.DONE, 22.dp)
            SmallLabel(topic.topic.uppercase(spanish), Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
            PERIOD_LABELS.forEach { (period, label) ->
                SmallLabel(
                    label,
                    Modifier.clickable(role = Role.Button) {
                        if (period != topic.period) {
                            onSetBudget(topic.topic, topic.period, 0)
                            onSetBudget(topic.topic, period, topic.limitCents)
                        }
                    }.padding(4.dp),
                    color = if (period == topic.period) MaterialTheme.colorScheme.onSurface else Color(0xFF5C5C5C),
                )
            }
            SmallLabel(
                "✕",
                Modifier.clickable(role = Role.Button) { onSetBudget(topic.topic, topic.period, 0) }
                    .semantics { contentDescription = "Quitar ${topic.topic}" }.padding(4.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ModuleTextField(
                amount, { value -> amount = value.filter { it.isDigit() || it == '.' }.take(8) }, "Límite",
                Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { parseCents(amount)?.let { onSetBudget(topic.topic, topic.period, it) } }),
            )
            Action("GUARDAR") { parseCents(amount)?.let { onSetBudget(topic.topic, topic.period, it) } }
        }
    }
}

@Composable
private fun MovementRow(record: FinanceRecord, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        StrokeIcon(kindIcon(record.kind), if (record.pendingFromOthersCents > 0) StrokeState.NOW else StrokeState.DONE, 20.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(record.description, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            SmallLabel(Budget.dateOf(record).format(dayMonth) + if (record.category.isNotBlank()) " · ${record.category.uppercase(spanish)}" else "")
        }
        Text(formatPesos(record.amountCents), style = MaterialTheme.typography.bodyLarge, color = if (record.kind.isIncome) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "×",
            Modifier.clickable(role = Role.Button, onClick = onDelete).semantics { contentDescription = "Borrar ${record.description}" }.padding(start = 4.dp, top = 8.dp, bottom = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ValueRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        SmallLabel(label, Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge, color = valueColor)
    }
}

@Composable
private fun Action(text: String, onClick: () -> Unit) {
    SmallLabel(text, Modifier.clickable(role = Role.Button, onClick = onClick).padding(vertical = 8.dp), color = LauncherRed)
}

@Composable
private fun Line() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(CardLine))
}
