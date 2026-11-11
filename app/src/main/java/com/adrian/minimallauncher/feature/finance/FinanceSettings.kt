package com.adrian.minimallauncher.feature.finance

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.drawBehind
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.adrian.minimallauncher.BankNotificationService
import com.adrian.minimallauncher.data.FinanceRecord
import com.adrian.minimallauncher.feature.CardLine
import com.adrian.minimallauncher.feature.LauncherAccent
import com.adrian.minimallauncher.feature.ModuleTextField
import com.adrian.minimallauncher.feature.SmallLabel
import com.adrian.minimallauncher.feature.bank.BankAccount
import com.adrian.minimallauncher.feature.bank.BankReceivable
import com.adrian.minimallauncher.ui.components.LocalReactions
import com.adrian.minimallauncher.ui.components.StrokeIcon
import com.adrian.minimallauncher.ui.components.StrokeState
import java.util.Locale
import kotlin.math.roundToInt

private val spanish = Locale.forLanguageTag("es")

/**
 * Presupuesto y ajustes de Finanzas con Acordeón (Accordion):
 * Todas las secciones se ven a primera vista y se despliegan en el mismo lugar al tocarlas.
 */
@Composable
internal fun FinanceSettings(
    summary: FinanceSummary,
    money: List<FinanceRecord>,
    accounts: List<BankAccount>,
    receivables: List<BankReceivable>,
    fixedLines: List<Pair<String, Long>>,
    dailyLimitCents: Long,
    savedPlan: String,
    onSavePlan: (String) -> Unit,
    savingGoal: String,
    onSaveGoal: (String) -> Unit,
    onSetBudget: (String, BudgetPeriod, Long) -> Unit,
    onSavingPct: (Int) -> Unit,
    onDailyLimit: (Long) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var activeSection by rememberSaveable { mutableStateOf<String?>(null) }
    val toggleSection = { key: String -> activeSection = if (activeSection == key) null else key }
    val simulator = remember { BudgetSim(0, summary.savingPct) }

    BackHandler(onBack = {
        if (activeSection != null) activeSection = null else onBack()
    })

    val reactions = LocalReactions.current
    val saved = remember(savedPlan) { FinancePlan.decode(savedPlan) }
    val sim = remember(savedPlan, summary.savingPct, summary.fixedIncomeCents) {
        BudgetSim(summary.fixedIncomeCents, summary.savingPct, saved.costs)
    }
    val typedIncome = parseCents(sim.income) ?: 0
    val dirty = sim.plan().costs != saved.costs || typedIncome != summary.fixedIncomeCents || sim.saving.roundToInt() != summary.savingPct
    val savePlan = {
        val income = if (typedIncome != summary.fixedIncomeCents) typedIncome else saved.incomeCents
        onSavePlan(sim.plan().copy(incomeCents = income, payments = saved.payments).encode())
        onSavingPct(sim.saving.roundToInt())
        reactions.tick()
        activeSection = null
    }

    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
            .padding(top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FinanceBackHeader("Presupuesto", onBack)
        BudgetOverview(summary)

        // Lo editable va cerrado: se abre al tocar, se guarda y se vuelve a cerrar.
        FinanceCard(spacing = 0.dp) {
        Text("Plan", style = MaterialTheme.typography.headlineMedium, color = FinanceWhite, modifier = Modifier.padding(bottom = 4.dp))

        // 1. Editar plan mensual
        FinanceCollapsible(
            title = "Ingreso, fijos y ahorro",
            icon = "ingreso",
            expanded = activeSection == "plan",
            onToggle = { toggleSection("plan") },
        ) {
            BudgetPlanner(sim, summary.date.lengthOfMonth())
            SaveButton(dirty, savePlan)
        }

        // 2. Distribuir por categoría
        FinanceCollapsible(
            title = "Categorías",
            icon = "presupuesto",
            summaryText = wholePesos(summary.topics.sumOf { it.limitCents }),
            expanded = activeSection == "topics",
            onToggle = { toggleSection("topics") },
        ) {
            var expandedTopic by rememberSaveable { mutableStateOf<String?>(null) }
            summary.topics.forEach { topic ->
                TopicRow(
                    topic = topic,
                    expanded = expandedTopic == topic.topic,
                    onToggle = { expandedTopic = if (expandedTopic == topic.topic) null else topic.topic },
                    onSetBudget = onSetBudget,
                )
            }
            var addTopicOpen by rememberSaveable { mutableStateOf(summary.topics.isEmpty()) }
            FinanceCollapsible(
                title = "Nueva categoría",
                icon = "nueva",
                expanded = addTopicOpen,
                onToggle = { addTopicOpen = !addTopicOpen },
                showRule = false,
            ) {
                var name by rememberSaveable { mutableStateOf("") }
                var amount by rememberSaveable { mutableStateOf("") }
                var period by rememberSaveable { mutableStateOf(BudgetPeriod.DAY) }
                ModuleTextField(name, { name = it.take(40) }, "Concepto", Modifier.fillMaxWidth())
                ModuleTextField(
                    amount, { amount = amountInput(it) }, "Presupuesto", Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    visualTransformation = PesosTransformation,
                )
                Frequency(period, Modifier.fillMaxWidth()) { period = it }
                Button(
                    onClick = {
                        onSetBudget(name.trim(), period, parseCents(amount)!!)
                        name = ""
                        amount = ""
                        addTopicOpen = false
                    },
                    enabled = name.isNotBlank() && (parseCents(amount) ?: 0) > 0,
                ) { StrokeIcon("listo", StrokeState.DONE, 22.dp) }
            }
        }

        // 3. Meta de ahorro
        val goal = remember(savingGoal) { SavingGoal.decode(savingGoal) }
        FinanceCollapsible(
            title = "Meta de ahorro",
            icon = "ahorro",
            summaryText = goal?.let { wholePesos(it.targetCents) },
            summaryColor = SavingGreen,
            expanded = activeSection == "goal",
            onToggle = { toggleSection("goal") },
        ) {
            SavingGoalView(savingGoal, summary.date, onSaveGoal)
        }

        // 4. Ajustes avanzados
        FinanceCollapsible(
            title = "Avanzado",
            icon = "gear",
            summaryText = if (dailyLimitCents > 0) "${wholePesos(dailyLimitCents)}/día" else "Auto",
            expanded = activeSection == "advanced",
            onToggle = { toggleSection("advanced") },
        ) {
            BankNoticesRow()
            Line()
            SmallLabel("PRESUPUESTO DIARIO MANUAL")
            var limit by remember(dailyLimitCents) { mutableStateOf(if (dailyLimitCents > 0) plainAmount(dailyLimitCents) else "") }
            ModuleTextField(
                limit, { limit = amountInput(it) }, "Límite manual (opcional)", Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                visualTransformation = PesosTransformation,
            )
            val parsed = if (limit.isBlank()) 0L else parseCents(limit)
            Button(
                onClick = { parsed?.let(onDailyLimit) },
                enabled = parsed != null && parsed >= 0 && parsed != dailyLimitCents,
            ) { StrokeIcon("listo", StrokeState.DONE, 22.dp) }
            if (parsed == null) Text("Escribe un importe válido con hasta dos decimales.", color = MoneyOver)
        }

        // 5. Simulador de presupuesto
        FinanceCollapsible(
            title = "Simular",
            icon = "presupuesto",
            expanded = activeSection == "simulator",
            onToggle = { toggleSection("simulator") },
        ) {
            BudgetPlanner(simulator, summary.date.lengthOfMonth())
        }
        }
    }
}

/** White "Guardar" with a check while there are changes; dark once everything is saved. */
@Composable
private fun SaveButton(enabled: Boolean, onSave: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(if (enabled) Color(0xFFF5F5F5) else Color(0xFF161616), shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onSave),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StrokeIcon("listo", StrokeState.DONE, 20.dp, color = if (enabled) Color.Black else FinanceMuted)
    }
}

/** A topic's limit as a compact collapsible row: tap to edit frequency, amount or remove. */
@Composable
private fun TopicRow(
    topic: TopicStatus,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSetBudget: (String, BudgetPeriod, Long) -> Unit,
) {
    var amount by remember(topic.limitCents) { mutableStateOf((topic.limitCents / 100).toString()) }
    val periodName = when (topic.period) {
        BudgetPeriod.DAY -> "al día"
        BudgetPeriod.WEEK -> "a la semana"
        BudgetPeriod.MONTH -> "al mes"
    }
    FinanceCollapsible(
        title = topic.topic.replaceFirstChar { it.uppercase(spanish) },
        subtitle = "Frecuencia: $periodName",
        icon = categoryIcon(topic.topic),
        summaryText = wholePesos(topic.limitCents),
        expanded = expanded,
        onToggle = onToggle,
    ) {
        Frequency(topic.period, Modifier.fillMaxWidth()) { period ->
            if (period != topic.period) {
                onSetBudget(topic.topic, topic.period, 0)
                onSetBudget(topic.topic, period, topic.limitCents)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                AmountRow(null, amount, { amount = it }, "Límite") { parseCents(amount)?.let { onSetBudget(topic.topic, topic.period, it) } }
            }
            IconButton("no", "Quitar ${topic.topic}", 18.dp) { onSetBudget(topic.topic, topic.period, 0) }
        }
    }
}

/** Bank notices: the bell lit with a check once allowed; red "Permitir" opens the system list while it is off. */
@Composable
internal fun BankNoticesRow() {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(BankNotificationService.enabled(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { enabled = BankNotificationService.enabled(context) }
    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp), Alignment.CenterVertically) {
        StrokeIcon("campana", if (enabled) StrokeState.DONE else StrokeState.NOW, 22.dp)
        Text("Avisos del banco", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        if (enabled) StrokeIcon("listo", StrokeState.DONE, 20.dp)
        else SmallLabel(
            "Permitir",
            Modifier.clickable(role = Role.Button) { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }.padding(vertical = 8.dp),
            color = LauncherAccent,
        )
    }
}

/** An amount field (hint inside) with a check that saves it; the keyboard's done does the same. */
@Composable
internal fun AmountRow(icon: String?, value: String, onChange: (String) -> Unit, hint: String, onSave: () -> Unit) {
    var saved by remember { mutableIntStateOf(0) }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        icon?.let { StrokeIcon(it, StrokeState.DONE, 22.dp) }
        ModuleTextField(
            value, { text -> onChange(amountInput(text)) }, hint,
            Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { saved++; onSave() }),
            visualTransformation = PesosTransformation,
        )
        Box(
            Modifier.clickable(role = Role.Button) { saved++; onSave() }.semantics { contentDescription = "Guardar" }.padding(8.dp),
        ) { StrokeIcon("listo", StrokeState.NOW, 22.dp, playKey = saved, color = LauncherAccent) }
    }
}

@Composable
internal fun IconButton(icon: String, description: String, size: Dp, onClick: () -> Unit) {
    Box(Modifier.clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = description }.padding(8.dp)) {
        StrokeIcon(icon, StrokeState.OFF, size)
    }
}

@Composable
internal fun Line() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(CardLine))
}

/**
 * Presupuesto a primera vista (diseño 2026-11-10), solo lectura: el ingreso grande, la barra
 * fijos / ahorro / libre que se toca para ver cada parte, y una sección para cada parte.
 */
@Composable
private fun BudgetOverview(summary: FinanceSummary) {
    val days = summary.date.lengthOfMonth()
    val income = summary.fixedIncomeCents
    val fixedRows = summary.fixed.map { it.topic to FinancePlan.perMonth(it.limitCents, it.period, days) }
    val fixed = fixedRows.sumOf { it.second }
    val saving = summary.savingCents
    val free = (income - fixed - saving).coerceAtLeast(0)
    val base = income.coerceAtLeast(1).toFloat()
    val amber = Color(0xFFFFCF7D)
    val green = Color(0xFF4DD402)
    val parts = listOf(Triple("Fijos", fixed, amber), Triple("Ahorro", saving, green), Triple("Libre", free, LauncherAccent))
    var picked by rememberSaveable { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
        FinanceSection(spacing = 20.dp) {
        Column(
            Modifier.fillMaxWidth()
                .drawBehind { drawRect(LauncherAccent, size = Size(3.dp.toPx(), size.height)) }
                .padding(start = 17.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Ingreso al mes", style = MaterialTheme.typography.labelLarge, color = FinanceMuted)
            FinanceAmount(income, size = 40, color = LauncherAccent)
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth().height(18.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFF0B2233))) {
                parts.forEach { (label, cents, color) ->
                    val f = cents / base
                    if (f > 0f) Box(
                        Modifier.weight(f).fillMaxHeight()
                            .background(if (picked == null || picked == label) color else color.copy(alpha = 0.3f))
                            .clickable(role = Role.Button) { picked = if (picked == label) null else label },
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                parts.forEach { (label, cents, color) ->
                    Text(
                        "$label ${if (income > 0) (cents * 100 / income) else 0}%",
                        Modifier.clickable(role = Role.Button) { picked = if (picked == label) null else label }.padding(vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (picked == null || picked == label) color else FinanceMuted,
                    )
                }
            }
            parts.firstOrNull { it.first == picked }?.let { (label, cents, color) ->
                Text("$label · ${wholePesos(cents)}", style = MaterialTheme.typography.titleLarge, color = color)
            }
        }
        }

        BudgetSection("Gastos fijos", wholePesos(fixed), amber) {
            fixedRows.forEach { (name, cents) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text(name, style = MaterialTheme.typography.titleMedium, color = FinanceWhite)
                    Text(wholePesos(cents), style = MaterialTheme.typography.titleMedium, color = FinanceWhite)
                }
            }
        }
        BudgetSection("Ahorro", wholePesos(saving), green) {
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF0B2233))) {
                    Box(Modifier.fillMaxWidth((summary.savingPct / 100f).coerceIn(0f, 1f)).fillMaxHeight().background(green))
                }
                Text("${summary.savingPct}%", style = MaterialTheme.typography.titleMedium, color = green)
            }
        }
        BudgetSection("Libre", wholePesos(free), LauncherAccent) {
            Text(
                "Semana ${wholePesos(summary.weekAllowanceCents)} · Día ${wholePesos(summary.dayShareCents)}",
                Modifier.padding(vertical = 10.dp),
                style = MaterialTheme.typography.labelLarge,
                color = FinanceMuted,
            )
        }
    }
}

/** Sección de solo lectura: título a 24 con su cifra en color, línea fina y su contenido. */
@Composable
private fun BudgetSection(title: String, figure: String, color: Color, content: @Composable () -> Unit) {
    FinanceSection(spacing = 0.dp) {
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), Arrangement.SpaceBetween, Alignment.Bottom) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = FinanceWhite)
            Text(figure, style = MaterialTheme.typography.titleLarge, color = color)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(FinanceRule))
        content()
    }
}
